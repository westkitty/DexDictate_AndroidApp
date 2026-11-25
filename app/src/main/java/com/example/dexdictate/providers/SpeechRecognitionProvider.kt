package com.example.dexdictate.providers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.dexdictate.model.SpeechRecognitionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class SpeechCapabilities(
    val isOnDeviceAvailable: Boolean,
    val isSystemRecognitionAvailable: Boolean,
    val description: String
)

/**
 * Replaceable speech recognition interface.
 * Round 1 wraps Android's SpeechRecognizer (preferring on-device when available and disclosing
 * network dependency honestly). Round 2+ can swap in local Whisper or Parakeet models.
 */
interface SpeechRecognitionProvider {
    val providerName: String
    val state: StateFlow<SpeechRecognitionState>
    fun inspectCapabilities(): SpeechCapabilities
    fun startListening(allowNetworkBacked: Boolean)
    fun stopListening()
    fun cancel()
    fun release()
}

class AndroidSpeechRecognitionProvider(
    context: Context
) : SpeechRecognitionProvider {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    override val providerName: String = "Android SpeechRecognizer (Replaceable Adapter)"

    private var recognizer: SpeechRecognizer? = null
    private var activeIsOnDevice: Boolean = false
    private var latestPartialText: String = ""

    override fun inspectCapabilities(): SpeechCapabilities {
        val onDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext) }.getOrDefault(false)
        } else {
            false
        }
        val standard = runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }.getOrDefault(false)

        val description = when {
            onDevice -> "On-device Android speech recognition available (audio stays on device)."
            standard -> "Android speech recognition available via system/network speech service (may send audio off-device)."
            else -> "No Android speech recognition service installed on this device/emulator. Typed-text fallback is available."
        }
        return SpeechCapabilities(
            isOnDeviceAvailable = onDevice,
            isSystemRecognitionAvailable = standard,
            description = description
        )
    }

    override fun startListening(allowNetworkBacked: Boolean) {
        val capabilities = inspectCapabilities()
        if (!capabilities.isOnDeviceAvailable && !capabilities.isSystemRecognitionAvailable) {
            _state.value = SpeechRecognitionState.TypedFallback(
                reason = "No Android speech recognizer is available on this device or emulator. You can type or edit your reply directly below."
            )
            return
        }

        if (!capabilities.isOnDeviceAvailable && !allowNetworkBacked) {
            _state.value = SpeechRecognitionState.NetworkDisclosureRequired(
                message = "On-device speech recognition is not available on this device. Continuing uses Android's system speech service, which may transmit audio over the network."
            )
            return
        }

        mainHandler.post {
            destroyRecognizerLocked()
            latestPartialText = ""
            activeIsOnDevice = capabilities.isOnDeviceAvailable

            val created = runCatching {
                if (activeIsOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(appContext)
                }
            }.getOrNull()

            if (created == null) {
                _state.value = SpeechRecognitionState.TypedFallback(
                    reason = "Could not initialize Android SpeechRecognizer. Use typed fallback below to draft and insert your reply."
                )
                return@post
            }

            recognizer = created
            created.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = SpeechRecognitionState.Listening(
                        isOnDevice = activeIsOnDevice,
                        partialText = ""
                    )
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    // Wait for onResults or onError
                }

                override fun onError(error: Int) {
                    val fallbackText = latestPartialText.trim()
                    if (fallbackText.isNotEmpty()) {
                        _state.value = SpeechRecognitionState.Recognized(
                            rawText = fallbackText,
                            isOnDevice = activeIsOnDevice
                        )
                        return
                    }
                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            _state.value = SpeechRecognitionState.PermissionRequired(
                                message = "Microphone permission is required for dictation.",
                                recoveryHint = "Grant Microphone access or switch to Typed Reply fallback."
                            )
                        }
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            _state.value = SpeechRecognitionState.Error(
                                message = "No speech was captured before timeout.",
                                recoveryHint = "Tap Dictate again and speak clearly, or use Typed Fallback."
                            )
                        }
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            _state.value = SpeechRecognitionState.Error(
                                message = "Speech service could not reach the network.",
                                recoveryHint = "Check connectivity or use Typed Fallback to draft your reply offline."
                            )
                        }
                        else -> {
                            _state.value = SpeechRecognitionState.TypedFallback(
                                reason = "Speech recognition unavailable (code $error). Draft your reply with Typed Fallback below."
                            )
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    if (best.isNotEmpty()) {
                        _state.value = SpeechRecognitionState.Recognized(
                            rawText = best,
                            isOnDevice = activeIsOnDevice
                        )
                    } else if (latestPartialText.isNotBlank()) {
                        _state.value = SpeechRecognitionState.Recognized(
                            rawText = latestPartialText.trim(),
                            isOnDevice = activeIsOnDevice
                        )
                    } else {
                        _state.value = SpeechRecognitionState.Error(
                            message = "No words were recognized.",
                            recoveryHint = "Try dictating again or switch to Typed Fallback."
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim().orEmpty()
                    if (partial.isNotEmpty()) {
                        latestPartialText = partial
                        _state.value = SpeechRecognitionState.Listening(
                            isOnDevice = activeIsOnDevice,
                            partialText = partial
                        )
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                if (activeIsOnDevice) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Preserve user's natural informal or explicit wording without automatic censoring
                    putExtra(RecognizerIntent.EXTRA_MASK_OFFENSIVE_WORDS, false)
                }
            }

            _state.value = SpeechRecognitionState.Listening(
                isOnDevice = activeIsOnDevice,
                partialText = ""
            )

            runCatching { created.startListening(intent) }.onFailure {
                _state.value = SpeechRecognitionState.TypedFallback(
                    reason = "Speech recognizer could not start on this device. Use Typed Fallback below."
                )
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            runCatching { recognizer?.stopListening() }
            val current = _state.value
            if (current is SpeechRecognitionState.Listening) {
                val partial = current.partialText.trim()
                if (partial.isNotEmpty()) {
                    _state.value = SpeechRecognitionState.Recognized(
                        rawText = partial,
                        isOnDevice = current.isOnDevice
                    )
                }
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            runCatching { recognizer?.cancel() }
            destroyRecognizerLocked()
            _state.value = SpeechRecognitionState.Idle
        }
    }

    override fun release() {
        mainHandler.post {
            destroyRecognizerLocked()
        }
    }

    private fun destroyRecognizerLocked() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}

package com.example.dexdictate.providers

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.dexdictate.model.SpeechPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Replaceable speech playback interface.
 *
 * Intentionally decoupled from Google's default TTS voice so Round 2+ can plug in
 * offline Kokoro/Pocket TTS or a user-authorized custom voice provider cleanly.
 */
interface SpeechPlaybackProvider {
    val providerName: String
    val playbackState: StateFlow<SpeechPlaybackState>
    fun speak(originalText: String)
    fun stop()
    fun replay()
    fun release()
}

/**
 * Initial working Android TextToSpeech adapter.
 * Speaks the exact original message text without rewriting or simplifying it.
 */
class AndroidTextToSpeechProvider(
    context: Context
) : SpeechPlaybackProvider {

    private val appContext = context.applicationContext
    private val _playbackState = MutableStateFlow<SpeechPlaybackState>(SpeechPlaybackState.Idle)
    override val playbackState: StateFlow<SpeechPlaybackState> = _playbackState.asStateFlow()

    override var providerName: String = "Android System TTS (Replaceable Adapter)"
        private set

    private var tts: TextToSpeech? = null
    private var isInitialized: Boolean = false
    private var initFailed: Boolean = false
    private var pendingText: String? = null
    private var lastSpokenText: String? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    initFailed = false
                    val engine = tts?.defaultEngine?.substringAfterLast('.') ?: "System TTS"
                    providerName = "Android TTS ($engine — replaceable)"
                    tts?.language = Locale.getDefault()
                    tts?.setSpeechRate(0.95f) // Slightly clearer pace for dyslexia comfort
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            val current = lastSpokenText ?: return
                            _playbackState.value = SpeechPlaybackState.Speaking(current, providerName)
                        }

                        override fun onDone(utteranceId: String?) {
                            val current = lastSpokenText.orEmpty()
                            _playbackState.value = SpeechPlaybackState.Completed(current)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _playbackState.value = SpeechPlaybackState.Error(
                                message = "Audio playback failed on this device.",
                                recoveryHint = "Check device volume and Android Text-to-Speech settings, or tap Replay."
                            )
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _playbackState.value = SpeechPlaybackState.Error(
                                message = "Text-to-Speech reported error code $errorCode.",
                                recoveryHint = "Verify a voice engine is installed in Android Settings > Accessibility > Text-to-speech."
                            )
                        }
                    })

                    pendingText?.let { queued ->
                        pendingText = null
                        speakNow(queued)
                    }
                } else {
                    isInitialized = false
                    initFailed = true
                    _playbackState.value = SpeechPlaybackState.Error(
                        message = "No Android Text-to-Speech engine could be initialized.",
                        recoveryHint = "Install or enable a TTS voice in Android Settings, or read the original text card on screen."
                    )
                }
            }
        } catch (t: Throwable) {
            isInitialized = false
            initFailed = true
            _playbackState.value = SpeechPlaybackState.Error(
                message = "Unable to start Text-to-Speech: ${t.message ?: "unavailable"}.",
                recoveryHint = "Check Android Accessibility Text-to-Speech configuration."
            )
        }
    }

    override fun speak(originalText: String) {
        val text = originalText.trim()
        if (text.isEmpty()) {
            _playbackState.value = SpeechPlaybackState.Error(
                message = "There is no text to read aloud.",
                recoveryHint = "Select a message bubble first and tap Read aloud again."
            )
            return
        }
        lastSpokenText = text

        if (initFailed) {
            _playbackState.value = SpeechPlaybackState.Error(
                message = "Text-to-Speech engine is unavailable in this environment.",
                recoveryHint = "On a phone or emulator with a TTS engine installed, Dexter will read the exact message aloud."
            )
            return
        }

        if (!isInitialized) {
            pendingText = text
            _playbackState.value = SpeechPlaybackState.Speaking(text, providerName)
            return
        }

        speakNow(text)
    }

    private fun speakNow(text: String) {
        val engine = tts ?: return
        val utteranceId = "dexdictate-${UUID.randomUUID()}"
        val params = Bundle()
        _playbackState.value = SpeechPlaybackState.Speaking(text, providerName)
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _playbackState.value = SpeechPlaybackState.Error(
                message = "Android TTS could not queue playback.",
                recoveryHint = "Tap Replay or verify your device's TTS voice package."
            )
        }
    }

    override fun stop() {
        pendingText = null
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
        val previous = lastSpokenText
        _playbackState.value = if (previous != null) {
            SpeechPlaybackState.Completed(previous)
        } else {
            SpeechPlaybackState.Idle
        }
    }

    override fun replay() {
        val text = lastSpokenText
        if (text.isNullOrBlank()) {
            _playbackState.value = SpeechPlaybackState.Error(
                message = "Nothing to replay yet.",
                recoveryHint = "Tap Read aloud on a selected message first."
            )
        } else {
            speak(text)
        }
    }

    override fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
        isInitialized = false
    }
}

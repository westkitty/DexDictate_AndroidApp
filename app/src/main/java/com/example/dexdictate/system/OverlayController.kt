package com.example.dexdictate.system

import android.content.Context
import com.example.dexdictate.model.AppDestination
import com.example.dexdictate.model.DemoChatMessage
import com.example.dexdictate.model.DemoChatScenario
import com.example.dexdictate.model.DemoChatTheme
import com.example.dexdictate.model.InsertionResult
import com.example.dexdictate.model.OverlayUiState
import com.example.dexdictate.model.PermissionStatusSnapshot
import com.example.dexdictate.model.ScreenTextReadResult
import com.example.dexdictate.model.SpeechPlaybackState
import com.example.dexdictate.model.SpeechRecognitionState
import com.example.dexdictate.providers.AccessibilityScreenTextReader
import com.example.dexdictate.providers.AccessibilityTextInsertionController
import com.example.dexdictate.providers.AndroidClipboardHelper
import com.example.dexdictate.providers.AndroidSpeechRecognitionProvider
import com.example.dexdictate.providers.AndroidTextToSpeechProvider
import com.example.dexdictate.providers.CompositeScreenTextReader
import com.example.dexdictate.providers.CompositeTextInsertionController
import com.example.dexdictate.providers.DemoChatScreenTextReader
import com.example.dexdictate.providers.DemoComposerInsertionController
import com.example.dexdictate.providers.MessageExplanationProvider
import com.example.dexdictate.providers.OfflineDeterministicExplanationProvider
import com.example.dexdictate.providers.ScreenTextReader
import com.example.dexdictate.providers.SpeechPlaybackProvider
import com.example.dexdictate.providers.SpeechRecognitionProvider
import com.example.dexdictate.providers.TextInsertionController
import com.example.dexdictate.state.OverlayStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates the Floating Dexter Overlay state machine, modular providers,
 * permissions status, and the two local fictional chat test fixtures.
 */
class OverlayController(
    val stateMachine: OverlayStateMachine,
    private val screenTextReader: ScreenTextReader,
    private val explanationProvider: MessageExplanationProvider,
    val playbackProvider: SpeechPlaybackProvider,
    val recognitionProvider: SpeechRecognitionProvider,
    private val insertionController: TextInsertionController,
    val permissionsController: PermissionsController,
    private val demoStore: DemoPlaygroundStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    val overlayState: StateFlow<OverlayUiState> = stateMachine.state
    val activeDestination: StateFlow<AppDestination> = demoStore.activeDestination
    val scenarios: StateFlow<Map<DemoChatTheme, DemoChatScenario>> = demoStore.scenarios

    private val _permissions = MutableStateFlow(permissionsController.snapshot())
    val permissions: StateFlow<PermissionStatusSnapshot> = _permissions.asStateFlow()

    init {
        scope.launch {
            playbackProvider.playbackState.collect { playback ->
                stateMachine.onPlaybackStateChanged(playback)
            }
        }
        scope.launch {
            recognitionProvider.state.collect { recognition ->
                stateMachine.onRecognitionStateChanged(recognition)
            }
        }
    }

    fun refreshPermissions() {
        _permissions.value = permissionsController.snapshot()
    }

    fun selectDestination(destination: AppDestination) {
        demoStore.selectDestination(destination)
        if (destination == AppDestination.SYSTEM_HUB) {
            refreshPermissions()
        }
    }

    fun selectDemoMessage(theme: DemoChatTheme, messageId: String) {
        demoStore.selectMessage(theme, messageId)
    }

    fun updateDemoComposer(theme: DemoChatTheme, text: String) {
        demoStore.updateComposerText(theme, text)
    }

    /**
     * Explicit manual Send action inside the fictional playground only.
     * DexDictate's Insert flow NEVER calls this automatically.
     */
    fun sendDemoMessageManually(theme: DemoChatTheme) {
        demoStore.sendManualMessage(theme)
    }

    fun onExpandOverlay(fromLiveSystemOverlay: Boolean = false) {
        stateMachine.setLiveSystemOverlayFlag(fromLiveSystemOverlay)
        stateMachine.expand()
    }

    fun onCollapseOverlay() {
        playbackProvider.stop()
        recognitionProvider.cancel()
        stateMachine.collapseAndClearTemporaryData()
    }

    fun onHideOverlay() {
        playbackProvider.stop()
        recognitionProvider.cancel()
        stateMachine.hideAndClearTemporaryData()
    }

    fun onRestoreOverlay() {
        stateMachine.showCollapsed()
    }

    fun onClearTemporaryData() {
        playbackProvider.stop()
        recognitionProvider.cancel()
        stateMachine.clearTemporaryDataKeepVisibility()
    }

    fun onReadAloudClicked(fromLiveSystemOverlay: Boolean = overlayState.value.isInLiveSystemOverlay) {
        stateMachine.setLiveSystemOverlayFlag(fromLiveSystemOverlay)
        recognitionProvider.cancel()
        stateMachine.beginReadAloud()

        scope.launch {
            when (val result = screenTextReader.readActiveMessage()) {
                is ScreenTextReadResult.Success -> {
                    stateMachine.onReadMessageCaptured(result.message)
                    playbackProvider.speak(result.message.text)
                }
                is ScreenTextReadResult.Unavailable -> {
                    stateMachine.onReadFailed(result.reason, result.recoveryHint)
                }
                is ScreenTextReadResult.RestrictedSurface -> {
                    stateMachine.onReadFailed(result.reason, result.recoveryHint)
                }
            }
        }
    }

    fun onStopPlayback() {
        playbackProvider.stop()
    }

    fun onReplayPlayback() {
        val text = overlayState.value.capturedMessage?.text
        if (!text.isNullOrBlank()) {
            playbackProvider.speak(text)
        } else {
            playbackProvider.replay()
        }
    }

    fun onPlayReviewDraft() {
        val draft = overlayState.value.reviewDraftText.trim()
        if (draft.isNotEmpty()) {
            playbackProvider.speak(draft)
        }
    }

    fun onExplainMessageClicked(fromLiveSystemOverlay: Boolean = overlayState.value.isInLiveSystemOverlay) {
        stateMachine.setLiveSystemOverlayFlag(fromLiveSystemOverlay)
        playbackProvider.stop()
        recognitionProvider.cancel()
        stateMachine.beginExplainMessage()

        scope.launch {
            when (val result = screenTextReader.readActiveMessage()) {
                is ScreenTextReadResult.Success -> {
                    val explanation = explanationProvider.explain(result.message.text)
                    stateMachine.onExplanationReady(result.message, explanation)
                }
                is ScreenTextReadResult.Unavailable -> {
                    stateMachine.onExplainFailed(result.reason, result.recoveryHint)
                }
                is ScreenTextReadResult.RestrictedSurface -> {
                    stateMachine.onExplainFailed(result.reason, result.recoveryHint)
                }
            }
        }
    }

    fun onDictateClicked(fromLiveSystemOverlay: Boolean = overlayState.value.isInLiveSystemOverlay) {
        stateMachine.setLiveSystemOverlayFlag(fromLiveSystemOverlay)
        playbackProvider.stop()
        stateMachine.beginDictation()

        refreshPermissions()
        if (!permissionsController.hasRecordAudioPermission()) {
            stateMachine.onRecognitionStateChanged(
                SpeechRecognitionState.PermissionRequired(
                    message = "Microphone permission is needed before Dexter can listen.",
                    recoveryHint = permissionsController.recoveryHintForMicrophoneDenial()
                )
            )
            return
        }

        val allowNetwork = overlayState.value.networkDisclosureAccepted
        recognitionProvider.startListening(allowNetworkBacked = allowNetwork)
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        refreshPermissions()
        if (granted) {
            onDictateClicked(overlayState.value.isInLiveSystemOverlay)
        } else {
            stateMachine.onRecognitionStateChanged(
                SpeechRecognitionState.PermissionRequired(
                    message = "Microphone access was denied.",
                    recoveryHint = permissionsController.recoveryHintForMicrophoneDenial()
                )
            )
        }
    }

    fun onAcceptNetworkRecognitionDisclosure() {
        stateMachine.acceptNetworkDisclosure()
        recognitionProvider.startListening(allowNetworkBacked = true)
    }

    fun onStopDictation() {
        recognitionProvider.stopListening()
    }

    fun onUseTypedFallback(reason: String = "Draft or edit your reply directly without microphone recognition.") {
        recognitionProvider.cancel()
        stateMachine.openTypedFallback(reason)
    }

    fun onReviewDraftChanged(updated: String) {
        stateMachine.updateReviewDraft(updated)
    }

    fun onInsertReviewedReply() {
        val text = overlayState.value.reviewDraftText
        val result = insertionController.insertText(text)
        stateMachine.onInsertionResult(result)
    }

    fun onCopyReviewedReplyFallback() {
        val text = overlayState.value.reviewDraftText.trim()
        if (text.isEmpty()) {
            stateMachine.onInsertionResult(
                InsertionResult.Failed(
                    reason = "Nothing to copy yet.",
                    recoveryHint = "Dictate or type your reply first."
                )
            )
            return
        }
        val result = insertionController.copyToClipboardFallback(
            text = text,
            reason = "Copied your reviewed wording to the clipboard. Paste it into your chat box and press Send yourself."
        )
        stateMachine.onInsertionResult(result)
    }

    fun onSelectDexterVariant(variantNumber: Int) {
        stateMachine.setDexterVariant(variantNumber)
    }
}

/**
 * Holds the two fictional local chat playgrounds (Demo 1: Grindr-inspired, Demo 2: Sniffies-inspired).
 * Explicitly local-only test fixtures, never connected to external services.
 */
class DemoPlaygroundStore {

    private val _activeDestination = MutableStateFlow(AppDestination.DEMO_GRINDR)
    val activeDestination: StateFlow<AppDestination> = _activeDestination.asStateFlow()

    private val _scenarios = MutableStateFlow(createSeededScenarios())
    val scenarios: StateFlow<Map<DemoChatTheme, DemoChatScenario>> = _scenarios.asStateFlow()

    fun selectDestination(destination: AppDestination) {
        _activeDestination.value = destination
    }

    fun activeDemoScenario(): DemoChatScenario? {
        return when (_activeDestination.value) {
            AppDestination.DEMO_GRINDR -> _scenarios.value[DemoChatTheme.GRINDR_INSPIRED]
            AppDestination.DEMO_SNIFFIES -> _scenarios.value[DemoChatTheme.SNIFFIES_INSPIRED]
            AppDestination.SYSTEM_HUB -> _scenarios.value[DemoChatTheme.GRINDR_INSPIRED]
        }
    }

    fun selectMessage(theme: DemoChatTheme, messageId: String) {
        _scenarios.update { current ->
            val scenario = current[theme] ?: return@update current
            current + (theme to scenario.copy(selectedMessageId = messageId))
        }
    }

    fun updateComposerText(theme: DemoChatTheme, text: String) {
        _scenarios.update { current ->
            val scenario = current[theme] ?: return@update current
            current + (theme to scenario.copy(composerText = text))
        }
    }

    fun insertIntoActiveComposer(text: String): Pair<Boolean, String> {
        val targetTheme = when (_activeDestination.value) {
            AppDestination.DEMO_GRINDR -> DemoChatTheme.GRINDR_INSPIRED
            AppDestination.DEMO_SNIFFIES -> DemoChatTheme.SNIFFIES_INSPIRED
            AppDestination.SYSTEM_HUB -> return false to ""
        }
        var verified = false
        var label = ""
        _scenarios.update { current ->
            val scenario = current[targetTheme] ?: return@update current
            val combined = if (scenario.composerText.isBlank()) {
                text
            } else {
                "${scenario.composerText.trimEnd()} $text"
            }
            verified = true
            label = "${scenario.title} composer"
            current + (targetTheme to scenario.copy(composerText = combined))
        }
        return verified to label
    }

    fun sendManualMessage(theme: DemoChatTheme) {
        _scenarios.update { current ->
            val scenario = current[theme] ?: return@update current
            val draft = scenario.composerText.trim()
            if (draft.isEmpty()) return@update current
            val newMsg = DemoChatMessage(
                id = "out-${System.currentTimeMillis()}",
                sender = "You",
                text = draft,
                timestamp = "Just now",
                isIncoming = false
            )
            current + (theme to scenario.copy(
                messages = scenario.messages + newMsg,
                composerText = ""
            ))
        }
    }

    companion object {
        fun createSeededScenarios(): Map<DemoChatTheme, DemoChatScenario> {
            val grindrMessages = listOf(
                DemoChatMessage(
                    id = "g1",
                    sender = "Alex · 400 ft",
                    text = "u hostin rn? tryna link b4 11",
                    timestamp = "9:41 PM",
                    isIncoming = true
                ),
                DemoChatMessage(
                    id = "g2",
                    sender = "You",
                    text = "maybe in a bit, finishing dinner",
                    timestamp = "9:42 PM",
                    isIncoming = false
                ),
                DemoChatMessage(
                    id = "g3",
                    sender = "Alex · 400 ft",
                    text = "yo want sum dik?",
                    timestamp = "9:44 PM",
                    isIncoming = true
                ),
                DemoChatMessage(
                    id = "g4",
                    sender = "Alex · 400 ft",
                    text = "top/vers? no flaking pls, dl ok",
                    timestamp = "9:45 PM",
                    isIncoming = true
                )
            )

            val sniffiesMessages = listOf(
                DemoChatMessage(
                    id = "s1",
                    sender = "Cruiser_08",
                    text = "wyd tn? down to chill n see where it goes",
                    timestamp = "10:12 PM",
                    isIncoming = true
                ),
                DemoChatMessage(
                    id = "s2",
                    sender = "You",
                    text = "just got home, nearby?",
                    timestamp = "10:13 PM",
                    isIncoming = false
                ),
                DemoChatMessage(
                    id = "s3",
                    sender = "Cruiser_08",
                    text = "yo want sum dik?",
                    timestamp = "10:14 PM",
                    isIncoming = true
                ),
                DemoChatMessage(
                    id = "s4",
                    sender = "Cruiser_08",
                    text = "npk? pics ok, just lookin 4 nsa fun nearby",
                    timestamp = "10:15 PM",
                    isIncoming = true
                )
            )

            return mapOf(
                DemoChatTheme.GRINDR_INSPIRED to DemoChatScenario(
                    id = "demo_grindr",
                    theme = DemoChatTheme.GRINDR_INSPIRED,
                    title = "Alex · 400 ft away",
                    subtitle = "Online now · Fictional Demo 1",
                    profileMeta = listOf("29", "400 ft", "Host / Travel"),
                    messages = grindrMessages,
                    selectedMessageId = "g3"
                ),
                DemoChatTheme.SNIFFIES_INSPIRED to DemoChatScenario(
                    id = "demo_sniffies",
                    theme = DemoChatTheme.SNIFFIES_INSPIRED,
                    title = "Cruiser_08 · Private Chat",
                    subtitle = "0.4 mi · Fictional Demo 2",
                    profileMeta = listOf("32", "5'11\"", "Vers", "0.4 mi", "Can Host"),
                    messages = sniffiesMessages,
                    selectedMessageId = "s3"
                )
            )
        }
    }
}

/**
 * Process-scoped runtime holder so MainActivity and DexDictateOverlayService share the exact same
 * OverlayController instance cleanly without duplicating TTS or speech engines.
 */
object OverlayRuntimeBridge {
    @Volatile
    private var controller: OverlayController? = null

    fun getOrCreate(context: Context): OverlayController {
        return controller ?: synchronized(this) {
            controller ?: buildController(context.applicationContext).also { controller = it }
        }
    }

    private fun buildController(appContext: Context): OverlayController {
        val stateMachine = OverlayStateMachine()
        val demoStore = DemoPlaygroundStore()
        val playbackProvider = AndroidTextToSpeechProvider(appContext)
        val recognitionProvider = AndroidSpeechRecognitionProvider(appContext)
        val permissionsController = PermissionsController(appContext, recognitionProvider)
        val clipboardHelper = AndroidClipboardHelper(appContext)

        val screenTextReader = CompositeScreenTextReader(
            demoReader = DemoChatScreenTextReader { demoStore.activeDemoScenario() },
            accessibilityReader = AccessibilityScreenTextReader(),
            useLiveSystemReader = { stateMachine.state.value.isInLiveSystemOverlay }
        )

        val insertionController = CompositeTextInsertionController(
            demoController = DemoComposerInsertionController(
                clipboardHelper = clipboardHelper,
                insertIntoActiveDemoComposer = { text -> demoStore.insertIntoActiveComposer(text) }
            ),
            accessibilityController = AccessibilityTextInsertionController(clipboardHelper),
            useLiveSystemTarget = { stateMachine.state.value.isInLiveSystemOverlay }
        )

        return OverlayController(
            stateMachine = stateMachine,
            screenTextReader = screenTextReader,
            explanationProvider = OfflineDeterministicExplanationProvider(),
            playbackProvider = playbackProvider,
            recognitionProvider = recognitionProvider,
            insertionController = insertionController,
            permissionsController = permissionsController,
            demoStore = demoStore
        )
    }
}

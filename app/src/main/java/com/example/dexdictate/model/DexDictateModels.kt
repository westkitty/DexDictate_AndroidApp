package com.example.dexdictate.model

enum class OverlayVisibility {
    HIDDEN,
    COLLAPSED,
    EXPANDED
}

enum class OverlayActionTab {
    NONE,
    READ_ALOUD,
    EXPLAIN_MESSAGE,
    DICTATE_REPLY,
    REVIEW_AND_INSERT
}

enum class TextSourceKind {
    DEMO_PLAYGROUND,
    ACCESSIBILITY_SERVICE,
    MANUAL_SELECTION
}

data class CapturedMessage(
    val id: String,
    val text: String,
    val senderLabel: String,
    val source: TextSourceKind
)

sealed interface ScreenTextReadResult {
    data class Success(val message: CapturedMessage) : ScreenTextReadResult
    data class Unavailable(val reason: String, val recoveryHint: String) : ScreenTextReadResult
    data class RestrictedSurface(val reason: String, val recoveryHint: String) : ScreenTextReadResult
}

data class MessageExplanation(
    val originalText: String,
    val plainLanguage: String,
    val slangBreakdown: List<String>,
    val ambiguityNote: String?,
    val providerLabel: String,
    val isOfflineDeterministic: Boolean
)

sealed interface SpeechPlaybackState {
    data object Idle : SpeechPlaybackState
    data class Speaking(val text: String, val engineName: String) : SpeechPlaybackState
    data class Completed(val lastText: String) : SpeechPlaybackState
    data class Error(val message: String, val recoveryHint: String) : SpeechPlaybackState
}

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data class NetworkDisclosureRequired(val message: String) : SpeechRecognitionState
    data class Listening(val isOnDevice: Boolean, val partialText: String = "") : SpeechRecognitionState
    data class Recognized(val rawText: String, val isOnDevice: Boolean) : SpeechRecognitionState
    data class TypedFallback(val reason: String) : SpeechRecognitionState
    data class PermissionRequired(val message: String, val recoveryHint: String) : SpeechRecognitionState
    data class Error(val message: String, val recoveryHint: String) : SpeechRecognitionState
}

sealed interface InsertionResult {
    data object Idle : InsertionResult
    data class Inserted(val targetDescription: String, val insertedText: String) : InsertionResult
    data class CopiedFallback(val reason: String, val copiedText: String) : InsertionResult
    data class Failed(val reason: String, val recoveryHint: String) : InsertionResult
}

enum class DemoChatTheme {
    GRINDR_INSPIRED,
    SNIFFIES_INSPIRED
}

data class DemoChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: String,
    val isIncoming: Boolean
)

data class DemoChatScenario(
    val id: String,
    val theme: DemoChatTheme,
    val title: String,
    val subtitle: String,
    val profileMeta: List<String>,
    val messages: List<DemoChatMessage>,
    val selectedMessageId: String,
    val composerText: String = ""
)

data class PermissionStatusSnapshot(
    val canDrawOverlays: Boolean,
    val hasRecordAudioPermission: Boolean,
    val hasNotificationsPermission: Boolean,
    val isAccessibilityServiceEnabled: Boolean,
    val isLiveSystemOverlayRunning: Boolean,
    val isOnDeviceSpeechAvailable: Boolean
)

enum class AppDestination {
    DEMO_GRINDR,
    DEMO_SNIFFIES,
    SYSTEM_HUB
}

data class OverlayUiState(
    val visibility: OverlayVisibility = OverlayVisibility.COLLAPSED,
    val activeAction: OverlayActionTab = OverlayActionTab.NONE,
    val isLoading: Boolean = false,
    val capturedMessage: CapturedMessage? = null,
    val readError: String? = null,
    val readRecoveryHint: String? = null,
    val playbackState: SpeechPlaybackState = SpeechPlaybackState.Idle,
    val explanation: MessageExplanation? = null,
    val recognitionState: SpeechRecognitionState = SpeechRecognitionState.Idle,
    val reviewDraftText: String = "",
    val reviewWasOnDevice: Boolean? = null,
    val insertionResult: InsertionResult = InsertionResult.Idle,
    val selectedDexterVariant: Int = 5,
    val networkDisclosureAccepted: Boolean = false,
    val isInLiveSystemOverlay: Boolean = false
)

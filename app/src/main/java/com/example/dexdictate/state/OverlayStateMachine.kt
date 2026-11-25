package com.example.dexdictate.state

import com.example.dexdictate.brand.DexterExpression
import com.example.dexdictate.model.CapturedMessage
import com.example.dexdictate.model.InsertionResult
import com.example.dexdictate.model.MessageExplanation
import com.example.dexdictate.model.OverlayActionTab
import com.example.dexdictate.model.OverlayUiState
import com.example.dexdictate.model.OverlayVisibility
import com.example.dexdictate.model.SpeechPlaybackState
import com.example.dexdictate.model.SpeechRecognitionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pure, deterministic state machine governing the floating Dexter assistant.
 *
 * Privacy & safety invariants enforced by construction:
 * - Collapsed and Hidden states never hold active listening/capture sessions.
 * - Collapsing or hiding wipes temporary captured text, explanations, and dictation drafts by default.
 * - Expanding never starts audio capture or text reading automatically.
 */
class OverlayStateMachine(
    initialState: OverlayUiState = OverlayUiState()
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OverlayUiState> = _state.asStateFlow()

    fun expand() {
        _state.update { current ->
            current.copy(
                visibility = OverlayVisibility.EXPANDED,
                activeAction = OverlayActionTab.NONE,
                isLoading = false,
                readError = null,
                readRecoveryHint = null
            )
        }
    }

    fun collapseAndClearTemporaryData() {
        _state.update { current ->
            current.copy(
                visibility = OverlayVisibility.COLLAPSED,
                activeAction = OverlayActionTab.NONE,
                isLoading = false,
                capturedMessage = null,
                readError = null,
                readRecoveryHint = null,
                playbackState = SpeechPlaybackState.Idle,
                explanation = null,
                recognitionState = SpeechRecognitionState.Idle,
                reviewDraftText = "",
                reviewWasOnDevice = null,
                insertionResult = InsertionResult.Idle
            )
        }
    }

    fun hideAndClearTemporaryData() {
        _state.update { current ->
            current.copy(
                visibility = OverlayVisibility.HIDDEN,
                activeAction = OverlayActionTab.NONE,
                isLoading = false,
                capturedMessage = null,
                readError = null,
                readRecoveryHint = null,
                playbackState = SpeechPlaybackState.Idle,
                explanation = null,
                recognitionState = SpeechRecognitionState.Idle,
                reviewDraftText = "",
                reviewWasOnDevice = null,
                insertionResult = InsertionResult.Idle
            )
        }
    }

    fun showCollapsed() {
        _state.update { current ->
            current.copy(
                visibility = OverlayVisibility.COLLAPSED,
                activeAction = OverlayActionTab.NONE,
                isLoading = false
            )
        }
    }

    fun clearTemporaryDataKeepVisibility() {
        _state.update { current ->
            current.copy(
                activeAction = OverlayActionTab.NONE,
                isLoading = false,
                capturedMessage = null,
                readError = null,
                readRecoveryHint = null,
                playbackState = SpeechPlaybackState.Idle,
                explanation = null,
                recognitionState = SpeechRecognitionState.Idle,
                reviewDraftText = "",
                reviewWasOnDevice = null,
                insertionResult = InsertionResult.Idle
            )
        }
    }

    fun beginReadAloud() {
        _state.update {
            it.copy(
                visibility = OverlayVisibility.EXPANDED,
                activeAction = OverlayActionTab.READ_ALOUD,
                isLoading = true,
                readError = null,
                readRecoveryHint = null,
                insertionResult = InsertionResult.Idle
            )
        }
    }

    fun onReadMessageCaptured(message: CapturedMessage) {
        _state.update {
            it.copy(
                capturedMessage = message,
                isLoading = false,
                readError = null,
                readRecoveryHint = null
            )
        }
    }

    fun onReadFailed(error: String, recoveryHint: String) {
        _state.update {
            it.copy(
                isLoading = false,
                readError = error,
                readRecoveryHint = recoveryHint,
                playbackState = SpeechPlaybackState.Error(error, recoveryHint)
            )
        }
    }

    fun onPlaybackStateChanged(playbackState: SpeechPlaybackState) {
        _state.update {
            it.copy(
                isLoading = false,
                playbackState = playbackState
            )
        }
    }

    fun beginExplainMessage() {
        _state.update {
            it.copy(
                visibility = OverlayVisibility.EXPANDED,
                activeAction = OverlayActionTab.EXPLAIN_MESSAGE,
                isLoading = true,
                readError = null,
                readRecoveryHint = null,
                explanation = null,
                insertionResult = InsertionResult.Idle
            )
        }
    }

    fun onExplanationReady(message: CapturedMessage, explanation: MessageExplanation) {
        _state.update {
            it.copy(
                capturedMessage = message,
                explanation = explanation,
                isLoading = false,
                readError = null,
                readRecoveryHint = null
            )
        }
    }

    fun onExplainFailed(error: String, recoveryHint: String) {
        _state.update {
            it.copy(
                isLoading = false,
                explanation = null,
                readError = error,
                readRecoveryHint = recoveryHint
            )
        }
    }

    fun beginDictation() {
        _state.update {
            it.copy(
                visibility = OverlayVisibility.EXPANDED,
                activeAction = OverlayActionTab.DICTATE_REPLY,
                isLoading = false,
                readError = null,
                readRecoveryHint = null,
                insertionResult = InsertionResult.Idle
            )
        }
    }

    fun onRecognitionStateChanged(recognitionState: SpeechRecognitionState) {
        _state.update { current ->
            when (recognitionState) {
                is SpeechRecognitionState.Recognized -> current.copy(
                    activeAction = OverlayActionTab.REVIEW_AND_INSERT,
                    recognitionState = recognitionState,
                    reviewDraftText = recognitionState.rawText,
                    reviewWasOnDevice = recognitionState.isOnDevice,
                    insertionResult = InsertionResult.Idle
                )
                is SpeechRecognitionState.TypedFallback -> current.copy(
                    activeAction = OverlayActionTab.REVIEW_AND_INSERT,
                    recognitionState = recognitionState,
                    reviewWasOnDevice = null,
                    insertionResult = InsertionResult.Idle
                )
                else -> current.copy(
                    recognitionState = recognitionState
                )
            }
        }
    }

    fun acceptNetworkDisclosure() {
        _state.update { it.copy(networkDisclosureAccepted = true) }
    }

    fun openTypedFallback(reason: String) {
        _state.update {
            it.copy(
                visibility = OverlayVisibility.EXPANDED,
                activeAction = OverlayActionTab.REVIEW_AND_INSERT,
                recognitionState = SpeechRecognitionState.TypedFallback(reason),
                reviewWasOnDevice = null,
                insertionResult = InsertionResult.Idle
            )
        }
    }

    fun updateReviewDraft(newText: String) {
        _state.update {
            it.copy(
                reviewDraftText = newText,
                insertionResult = InsertionResult.Idle
            )
        }
    }

    fun onInsertionResult(result: InsertionResult) {
        _state.update { it.copy(insertionResult = result) }
    }

    fun setDexterVariant(variantNumber: Int) {
        _state.update { it.copy(selectedDexterVariant = variantNumber.coerceIn(1, 11)) }
    }

    fun setLiveSystemOverlayFlag(inLiveOverlay: Boolean) {
        _state.update { it.copy(isInLiveSystemOverlay = inLiveOverlay) }
    }

    fun currentExpression(): DexterExpression = deriveExpression(_state.value)

    companion object {
        fun deriveExpression(state: OverlayUiState): DexterExpression {
            if (state.visibility == OverlayVisibility.COLLAPSED) {
                return DexterExpression.IDLE
            }
            if (state.readError != null ||
                state.playbackState is SpeechPlaybackState.Error ||
                state.recognitionState is SpeechRecognitionState.Error ||
                state.recognitionState is SpeechRecognitionState.PermissionRequired
            ) {
                return DexterExpression.ERROR
            }
            if (state.playbackState is SpeechPlaybackState.Speaking) {
                return DexterExpression.SPEAKING
            }
            if (state.recognitionState is SpeechRecognitionState.Listening) {
                return DexterExpression.LISTENING
            }
            if (state.activeAction == OverlayActionTab.EXPLAIN_MESSAGE && state.explanation != null) {
                return DexterExpression.EXPLAINING
            }
            return DexterExpression.EXPANDED
        }
    }
}

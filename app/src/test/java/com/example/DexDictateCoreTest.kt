package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dexdictate.brand.BrandAssetManager
import com.example.dexdictate.brand.DexterExpression
import com.example.dexdictate.model.DemoChatTheme
import com.example.dexdictate.model.InsertionResult
import com.example.dexdictate.model.OverlayVisibility
import com.example.dexdictate.model.ScreenTextReadResult
import com.example.dexdictate.model.SpeechPlaybackState
import com.example.dexdictate.model.SpeechRecognitionState
import com.example.dexdictate.providers.AccessibilityScreenTextReader
import com.example.dexdictate.providers.AndroidClipboardHelper
import com.example.dexdictate.providers.DemoChatScreenTextReader
import com.example.dexdictate.providers.DemoComposerInsertionController
import com.example.dexdictate.providers.OfflineDeterministicExplanationProvider
import com.example.dexdictate.providers.SpeechCapabilities
import com.example.dexdictate.providers.SpeechPlaybackProvider
import com.example.dexdictate.providers.SpeechRecognitionProvider
import com.example.dexdictate.state.OverlayStateMachine
import com.example.dexdictate.system.DemoPlaygroundStore
import com.example.dexdictate.system.OverlayController
import com.example.dexdictate.system.PermissionsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DexDictateCoreTest {

    private class FakePlaybackProvider : SpeechPlaybackProvider {
        override val providerName: String = "Fake Replaceable Voice"
        private val _state = MutableStateFlow<SpeechPlaybackState>(SpeechPlaybackState.Idle)
        override val playbackState: StateFlow<SpeechPlaybackState> = _state
        val spokenTexts = mutableListOf<String>()

        override fun speak(originalText: String) {
            spokenTexts.add(originalText)
            _state.value = SpeechPlaybackState.Speaking(originalText, providerName)
        }

        override fun stop() {
            _state.value = SpeechPlaybackState.Idle
        }

        override fun replay() {
            spokenTexts.lastOrNull()?.let { speak(it) }
        }

        override fun release() = Unit
    }

    private class FakeRecognitionProvider(
        private val onDevice: Boolean = true
    ) : SpeechRecognitionProvider {
        override val providerName: String = "Fake SpeechRecognizer"
        private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
        override val state: StateFlow<SpeechRecognitionState> = _state

        override fun inspectCapabilities(): SpeechCapabilities = SpeechCapabilities(
            isOnDeviceAvailable = onDevice,
            isSystemRecognitionAvailable = true,
            description = "Fake capability"
        )

        override fun startListening(allowNetworkBacked: Boolean) {
            if (!onDevice && !allowNetworkBacked) {
                _state.value = SpeechRecognitionState.NetworkDisclosureRequired("Network required")
            } else {
                _state.value = SpeechRecognitionState.Listening(isOnDevice = onDevice)
            }
        }

        fun emitRawResult(rawText: String) {
            _state.value = SpeechRecognitionState.Recognized(rawText = rawText, isOnDevice = onDevice)
        }

        override fun stopListening() = Unit
        override fun cancel() {
            _state.value = SpeechRecognitionState.Idle
        }
        override fun release() = Unit
    }

    @Test
    fun `explain provider translates explicit slang accurately without assuming specific act`() = runTest {
        val provider = OfflineDeterministicExplanationProvider()
        val result = provider.explain("yo want sum dik?")

        assertEquals("yo want sum dik?", result.originalText)
        assertEquals(
            "They're asking whether you're interested in a sexual encounter.",
            result.plainLanguage
        )
        assertNotEquals(result.originalText, result.plainLanguage)
        assertTrue(result.slangBreakdown.any { it.contains("dik") })
        assertTrue(result.ambiguityNote?.contains("does not specify a particular sexual act") == true)
        assertTrue(result.isOfflineDeterministic)
    }

    @Test
    fun `state machine starts collapsed with no active capture and clears temp data on collapse`() {
        val machine = OverlayStateMachine()
        assertEquals(OverlayVisibility.COLLAPSED, machine.state.value.visibility)
        assertNull(machine.state.value.capturedMessage)
        assertEquals("", machine.state.value.reviewDraftText)

        machine.expand()
        assertEquals(OverlayVisibility.EXPANDED, machine.state.value.visibility)
        machine.updateReviewDraft("yeah come over rn")
        assertEquals("yeah come over rn", machine.state.value.reviewDraftText)

        machine.collapseAndClearTemporaryData()
        assertEquals(OverlayVisibility.COLLAPSED, machine.state.value.visibility)
        assertEquals("", machine.state.value.reviewDraftText)
        assertNull(machine.state.value.capturedMessage)
        assertNull(machine.state.value.explanation)
    }

    @Test
    fun `read aloud speaks original message verbatim and never simplifies`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = DemoPlaygroundStore()
        val playback = FakePlaybackProvider()
        val recognition = FakeRecognitionProvider()
        val controller = OverlayController(
            stateMachine = OverlayStateMachine(),
            screenTextReader = DemoChatScreenTextReader { store.activeDemoScenario() },
            explanationProvider = OfflineDeterministicExplanationProvider(),
            playbackProvider = playback,
            recognitionProvider = recognition,
            insertionController = DemoComposerInsertionController(
                AndroidClipboardHelper(context)
            ) { store.insertIntoActiveComposer(it) },
            permissionsController = PermissionsController(context, recognition),
            demoStore = store
        )

        controller.onExpandOverlay()
        controller.onReadAloudClicked()

        assertEquals(listOf("yo want sum dik?"), playback.spokenTexts)
        assertEquals("yo want sum dik?", controller.overlayState.value.capturedMessage?.text)
    }

    @Test
    fun `dictation denial shows recovery hint and review insert never auto-sends`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = DemoPlaygroundStore()
        val playback = FakePlaybackProvider()
        val recognition = FakeRecognitionProvider()
        val controller = OverlayController(
            stateMachine = OverlayStateMachine(),
            screenTextReader = DemoChatScreenTextReader { store.activeDemoScenario() },
            explanationProvider = OfflineDeterministicExplanationProvider(),
            playbackProvider = playback,
            recognitionProvider = recognition,
            insertionController = DemoComposerInsertionController(
                AndroidClipboardHelper(context)
            ) { store.insertIntoActiveComposer(it) },
            permissionsController = PermissionsController(context, recognition),
            demoStore = store
        )

        // Robolectric starts without RECORD_AUDIO granted
        controller.onExpandOverlay()
        controller.onDictateClicked()
        val permState = controller.overlayState.value.recognitionState
        assertTrue(permState is SpeechRecognitionState.PermissionRequired)
        assertTrue((permState as SpeechRecognitionState.PermissionRequired).recoveryHint.contains("Settings"))

        // Raw wording preserved without tone alteration and inserted without sending
        val initialMessageCount = store.scenarios.value[DemoChatTheme.GRINDR_INSPIRED]!!.messages.size
        recognition.emitRawResult("hell yeah come thru in 10")
        assertEquals("hell yeah come thru in 10", controller.overlayState.value.reviewDraftText)

        controller.onInsertReviewedReply()
        assertTrue(controller.overlayState.value.insertionResult is InsertionResult.Inserted)
        val scenarioAfterInsert = store.scenarios.value[DemoChatTheme.GRINDR_INSPIRED]!!
        assertEquals("hell yeah come thru in 10", scenarioAfterInsert.composerText)
        assertEquals(initialMessageCount, scenarioAfterInsert.messages.size) // NEVER auto-sends!
    }

    @Test
    fun `unsupported external accessibility reading fails safely with recovery guidance`() = runTest {
        val reader = AccessibilityScreenTextReader(serviceProvider = { null })
        val result = reader.readActiveMessage()
        assertTrue(result is ScreenTextReadResult.Unavailable)
        val unavailable = result as ScreenTextReadResult.Unavailable
        assertTrue(unavailable.recoveryHint.contains("Accessibility"))
    }

    @Test
    fun `brand asset manager explicitly marks temporary placeholder when official PNGs are absent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val descriptor = BrandAssetManager.resolveAsset(context, DexterExpression.IDLE, 5)
        assertTrue(descriptor.isPlaceholder)
        assertNull(descriptor.resolvedDrawableRes)
        assertEquals("dexdictate_icon_standard_05", descriptor.expectedResourceName)
    }
}

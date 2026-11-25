package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.dexdictate.model.DemoChatTheme
import com.example.dexdictate.model.SpeechPlaybackState
import com.example.dexdictate.model.SpeechRecognitionState
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
import com.example.dexdictate.ui.DexDictateApp
import com.example.ui.theme.DexDictateTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestController(): Pair<OverlayController, DemoPlaygroundStore> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = DemoPlaygroundStore()
        val fakePlayback = object : SpeechPlaybackProvider {
            override val providerName: String = "Test TTS"
            private val _state = MutableStateFlow<SpeechPlaybackState>(SpeechPlaybackState.Idle)
            override val playbackState: StateFlow<SpeechPlaybackState> = _state
            override fun speak(originalText: String) {
                _state.value = SpeechPlaybackState.Speaking(originalText, providerName)
            }
            override fun stop() {
                _state.value = SpeechPlaybackState.Idle
            }
            override fun replay() = Unit
            override fun release() = Unit
        }
        val fakeRecognition = object : SpeechRecognitionProvider {
            override val providerName: String = "Test STT"
            private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
            override val state: StateFlow<SpeechRecognitionState> = _state
            override fun inspectCapabilities() = SpeechCapabilities(true, true, "Test on-device")
            override fun startListening(allowNetworkBacked: Boolean) {
                _state.value = SpeechRecognitionState.Listening(isOnDevice = true)
            }
            override fun stopListening() = Unit
            override fun cancel() {
                _state.value = SpeechRecognitionState.Idle
            }
            override fun release() = Unit
        }
        val controller = OverlayController(
            stateMachine = OverlayStateMachine(),
            screenTextReader = DemoChatScreenTextReader { store.activeDemoScenario() },
            explanationProvider = OfflineDeterministicExplanationProvider(),
            playbackProvider = fakePlayback,
            recognitionProvider = fakeRecognition,
            insertionController = DemoComposerInsertionController(
                AndroidClipboardHelper(context)
            ) { store.insertIntoActiveComposer(it) },
            permissionsController = PermissionsController(context, fakeRecognition),
            demoStore = store
        )
        return controller to store
    }

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DexDictate", appName)
    }

    @Test
    fun `collapsed state hides expanded menu and expanded state renders centered heading and three icon-only accessible controls`() {
        val (controller, _) = createTestController()

        composeTestRule.setContent {
            DexDictateTheme {
                DexDictateApp(controller = controller)
            }
        }
        composeTestRule.waitForIdle()

        // Demo 1 renders with seeded test message and collapsed Dexter bubble
        composeTestRule.onNodeWithTag("demo_screen_grindr").assertIsDisplayed()
        composeTestRule.onNodeWithText("yo want sum dik?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dexter_collapsed_bubble").assertIsDisplayed()

        // Expanded heading must NOT appear before invocation
        composeTestRule.onAllNodesWithText("DEXTER IS HERE").assertCountEquals(0)

        // Tap collapsed Dexter -> expands with DEXTER IS HERE + three icon-only buttons with TalkBack labels
        composeTestRule.onNodeWithTag("dexter_collapsed_bubble").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dexter_expanded_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("DEXTER IS HERE").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Read aloud").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Explain message").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dictate reply").assertIsDisplayed()

        // Primary controls must NOT show READ, EXPLAIN, or REPLY as visible word labels
        composeTestRule.onAllNodesWithText("READ").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("EXPLAIN").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("REPLY").assertCountEquals(0)

        // Explain displays original wording and distinct plain-language explanation
        composeTestRule.onNodeWithContentDescription("Explain message").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("explain_plain_text")
            .assertTextContains("interested in a sexual encounter", substring = true)

        // Closing panel restores collapsed bubble and hides DEXTER IS HERE
        composeTestRule.onNodeWithTag("action_collapse_overlay").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dexter_collapsed_bubble").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("DEXTER IS HERE").assertCountEquals(0)
    }

    @Test
    fun `demo 2 renders and review insert puts reply in composer without auto-sending`() {
        val (controller, store) = createTestController()

        composeTestRule.setContent {
            DexDictateTheme {
                DexDictateApp(controller = controller)
            }
        }
        composeTestRule.waitForIdle()

        // Switch to Demo 2 (Sniffies-inspired navy private chat)
        composeTestRule.onNodeWithTag("nav_demo_sniffies").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("demo_screen_sniffies").assertIsDisplayed()
        composeTestRule.onNodeWithText("yo want sum dik?").assertIsDisplayed()

        val initialCount = store.scenarios.value[DemoChatTheme.SNIFFIES_INSPIRED]!!.messages.size

        // Expand Dexter -> Dictate -> Typed Fallback -> type reply -> Insert
        composeTestRule.onNodeWithTag("dexter_collapsed_bubble").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Dictate reply").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_typed_fallback").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("review_draft_input").performTextInput("yeah down, host?")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_insert_reply").performClick()
        composeTestRule.waitForIdle()

        // Composer has inserted text, verified banner shown, message count unchanged (never auto-sends!)
        composeTestRule.onNodeWithTag("insertion_verified_banner").assertIsDisplayed()
        val updatedScenario = store.scenarios.value[DemoChatTheme.SNIFFIES_INSPIRED]!!
        assertEquals("yeah down, host?", updatedScenario.composerText)
        assertEquals(initialCount, updatedScenario.messages.size)
    }
}

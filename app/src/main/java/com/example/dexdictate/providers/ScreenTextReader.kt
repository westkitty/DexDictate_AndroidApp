package com.example.dexdictate.providers

import com.example.dexdictate.model.CapturedMessage
import com.example.dexdictate.model.DemoChatScenario
import com.example.dexdictate.model.ScreenTextReadResult
import com.example.dexdictate.model.TextSourceKind
import com.example.dexdictate.system.DexDictateAccessibilityService

/**
 * Modular interface for reading the active incoming message.
 * Keeps in-app playground fixtures strictly separated from real Android AccessibilityService access.
 */
interface ScreenTextReader {
    suspend fun readActiveMessage(): ScreenTextReadResult
}

/**
 * Reads the currently selected (or latest incoming) message from the local fictional demo playground.
 */
class DemoChatScreenTextReader(
    private val activeScenarioProvider: () -> DemoChatScenario?
) : ScreenTextReader {

    override suspend fun readActiveMessage(): ScreenTextReadResult {
        val scenario = activeScenarioProvider()
            ?: return ScreenTextReadResult.Unavailable(
                reason = "No active demo conversation is open.",
                recoveryHint = "Switch to Demo 1 or Demo 2 and tap any incoming message bubble to select it."
            )

        val selected = scenario.messages.firstOrNull { it.id == scenario.selectedMessageId }
            ?: scenario.messages.lastOrNull { it.isIncoming }
            ?: return ScreenTextReadResult.Unavailable(
                reason = "No readable message found in this demo thread.",
                recoveryHint = "Tap any incoming message in the chat list to target it."
            )

        return ScreenTextReadResult.Success(
            CapturedMessage(
                id = selected.id,
                text = selected.text,
                senderLabel = "${selected.sender} (${scenario.title})",
                source = TextSourceKind.DEMO_PLAYGROUND
            )
        )
    }
}

/**
 * Reads visible message text on demand through the user-enabled DexDictateAccessibilityService.
 * Never runs passively in the background and never bypasses secure or protected surfaces.
 */
class AccessibilityScreenTextReader(
    private val serviceProvider: () -> DexDictateAccessibilityService? = {
        DexDictateAccessibilityService.activeInstance()
    }
) : ScreenTextReader {

    override suspend fun readActiveMessage(): ScreenTextReadResult {
        val service = serviceProvider()
            ?: return ScreenTextReadResult.Unavailable(
                reason = "DexDictate Accessibility Reader is not enabled.",
                recoveryHint = "Open System & Setup to enable DexDictate On-Demand Reader in Android Accessibility Settings, or copy the message text."
            )

        return service.readLatestVisibleMessageOnDemand()
    }
}

/**
 * Routes reading requests based on whether Dexter was triggered from the internal playground
 * or the live system-wide WindowManager overlay.
 */
class CompositeScreenTextReader(
    private val demoReader: ScreenTextReader,
    private val accessibilityReader: ScreenTextReader,
    private val useLiveSystemReader: () -> Boolean
) : ScreenTextReader {

    override suspend fun readActiveMessage(): ScreenTextReadResult {
        return if (useLiveSystemReader()) {
            accessibilityReader.readActiveMessage()
        } else {
            demoReader.readActiveMessage()
        }
    }
}

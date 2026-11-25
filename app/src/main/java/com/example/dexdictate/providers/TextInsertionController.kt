package com.example.dexdictate.providers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.dexdictate.model.InsertionResult
import com.example.dexdictate.system.DexDictateAccessibilityService

/**
 * Safely inserts user-reviewed text into an identified editable target.
 *
 * Non-negotiable safety rules:
 * - NEVER presses Send automatically.
 * - NEVER modifies unrelated messages or fields.
 * - NEVER claims insertion succeeded without verifying the target accepted the text.
 * - Always supports an explicit Clipboard copy fallback.
 */
interface TextInsertionController {
    fun insertText(text: String): InsertionResult
    fun copyToClipboardFallback(text: String, reason: String): InsertionResult.CopiedFallback
}

class AndroidClipboardHelper(context: Context) {
    private val appContext = context.applicationContext

    fun copy(text: String): Boolean {
        return runCatching {
            val manager = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            val clip = ClipData.newPlainText("DexDictate reply", text)
            manager.setPrimaryClip(clip)
            true
        }.getOrDefault(false)
    }
}

class DemoComposerInsertionController(
    private val clipboardHelper: AndroidClipboardHelper,
    private val insertIntoActiveDemoComposer: (String) -> Pair<Boolean, String>
) : TextInsertionController {

    override fun insertText(text: String): InsertionResult {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) {
            return InsertionResult.Failed(
                reason = "Reply text is empty.",
                recoveryHint = "Dictate or type your reply before tapping Insert."
            )
        }
        val (verified, targetLabel) = insertIntoActiveDemoComposer(cleaned)
        return if (verified) {
            InsertionResult.Inserted(
                targetDescription = "$targetLabel (manual Send required)",
                insertedText = cleaned
            )
        } else {
            copyToClipboardFallback(
                text = cleaned,
                reason = "No active demo composer field was open — copied to clipboard so you can paste manually."
            )
        }
    }

    override fun copyToClipboardFallback(text: String, reason: String): InsertionResult.CopiedFallback {
        clipboardHelper.copy(text)
        return InsertionResult.CopiedFallback(reason = reason, copiedText = text)
    }
}

class AccessibilityTextInsertionController(
    private val clipboardHelper: AndroidClipboardHelper,
    private val serviceProvider: () -> DexDictateAccessibilityService? = {
        DexDictateAccessibilityService.activeInstance()
    }
) : TextInsertionController {

    override fun insertText(text: String): InsertionResult {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) {
            return InsertionResult.Failed(
                reason = "Reply text is empty.",
                recoveryHint = "Dictate or type your reply first."
            )
        }
        val service = serviceProvider()
            ?: return copyToClipboardFallback(
                text = cleaned,
                reason = "DexDictate Accessibility Service is not enabled — copied to clipboard so you can paste safely."
            )

        return when (val result = service.insertTextIntoActiveEditableField(cleaned)) {
            is InsertionResult.Inserted -> result
            is InsertionResult.CopiedFallback -> {
                clipboardHelper.copy(cleaned)
                result
            }
            is InsertionResult.Failed -> {
                copyToClipboardFallback(
                    text = cleaned,
                    reason = "${result.reason} Copied to clipboard instead — paste into your chat field."
                )
            }
            InsertionResult.Idle -> copyToClipboardFallback(
                text = cleaned,
                reason = "Could not verify target input — copied to clipboard."
            )
        }
    }

    override fun copyToClipboardFallback(text: String, reason: String): InsertionResult.CopiedFallback {
        clipboardHelper.copy(text)
        return InsertionResult.CopiedFallback(reason = reason, copiedText = text)
    }
}

class CompositeTextInsertionController(
    private val demoController: TextInsertionController,
    private val accessibilityController: TextInsertionController,
    private val useLiveSystemTarget: () -> Boolean
) : TextInsertionController {

    override fun insertText(text: String): InsertionResult {
        return if (useLiveSystemTarget()) {
            accessibilityController.insertText(text)
        } else {
            demoController.insertText(text)
        }
    }

    override fun copyToClipboardFallback(text: String, reason: String): InsertionResult.CopiedFallback {
        return if (useLiveSystemTarget()) {
            accessibilityController.copyToClipboardFallback(text, reason)
        } else {
            demoController.copyToClipboardFallback(text, reason)
        }
    }
}

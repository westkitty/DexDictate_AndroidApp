package com.example.dexdictate.system

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.dexdictate.model.CapturedMessage
import com.example.dexdictate.model.InsertionResult
import com.example.dexdictate.model.ScreenTextReadResult
import com.example.dexdictate.model.TextSourceKind
import java.lang.ref.WeakReference

/**
 * Optional, explicitly user-enabled AccessibilityService for on-demand screen reading and safe text insertion.
 *
 * Strict privacy & platform rules:
 * - Does NOT continuously monitor, log, store, or transmit screen content in onAccessibilityEvent().
 * - Only inspects the active window hierarchy when the user explicitly taps Read Aloud, Explain, or Insert.
 * - Never bypasses FLAG_SECURE, password fields, or apps that restrict accessibility text exposure.
 * - Never presses Send buttons automatically.
 */
class DexDictateAccessibilityService : AccessibilityService() {

    @Volatile
    private var lastExternalPackageName: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Deliberately minimal: only remember the active external package name so we can skip our own window.
        // Zero passive text scraping or storage happens here.
        val pkg = event?.packageName?.toString() ?: return
        if (pkg != packageName) {
            lastExternalPackageName = pkg
        }
    }

    override fun onInterrupt() {
        // No background jobs to interrupt.
    }

    override fun onDestroy() {
        if (instanceRef?.get() === this) {
            instanceRef = null
        }
        super.onDestroy()
    }

    /**
     * Explicitly invoked only after user taps Read Aloud or Explain Message in the live system overlay.
     */
    fun readLatestVisibleMessageOnDemand(): ScreenTextReadResult {
        val root = findActiveExternalRootNode()
            ?: return ScreenTextReadResult.RestrictedSurface(
                reason = "The current app did not expose accessible text to Android Accessibility APIs.",
                recoveryHint = "Some apps use custom rendering or protected views. Copy the message manually or test inside DexDictate's demo playground."
            )

        val candidates = mutableListOf<String>()
        collectReadableTextNodes(root, candidates, maxDepth = 18)

        val candidate = candidates.lastOrNull()
            ?: return ScreenTextReadResult.RestrictedSurface(
                reason = "No readable message text was exposed on the current screen.",
                recoveryHint = "Tap into a visible chat thread or copy the message text manually — DexDictate never bypasses protected windows."
            )

        val sourceLabel = lastExternalPackageName?.substringAfterLast('.') ?: "Active app"
        return ScreenTextReadResult.Success(
            CapturedMessage(
                id = "a11y-${System.currentTimeMillis()}",
                text = candidate,
                senderLabel = "Visible message ($sourceLabel)",
                source = TextSourceKind.ACCESSIBILITY_SERVICE
            )
        )
    }

    /**
     * Explicitly invoked only after user taps Insert in Review & Insert.
     * Targets ONLY a verified, non-password editable node and NEVER triggers Send.
     */
    fun insertTextIntoActiveEditableField(text: String): InsertionResult {
        val root = findActiveExternalRootNode()
            ?: return InsertionResult.Failed(
                reason = "No accessible editable message box was found in the active app.",
                recoveryHint = "Tap into the chat message field first, or use Copy fallback to paste manually."
            )

        val focusedEditable = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.takeIf { it.isEditable && !it.isPassword }
            ?: findFirstEditableNode(root, maxDepth = 18)

        if (focusedEditable == null) {
            return InsertionResult.Failed(
                reason = "Could not verify an editable message composer in the active app.",
                recoveryHint = "Tap the chat input field or use Copy to paste your reply safely."
            )
        }

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val accepted = runCatching {
            focusedEditable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }.getOrDefault(false)

        return if (accepted) {
            val targetName = lastExternalPackageName?.substringAfterLast('.') ?: "active chat composer"
            InsertionResult.Inserted(
                targetDescription = "$targetName input field (manual Send required)",
                insertedText = text
            )
        } else {
            InsertionResult.Failed(
                reason = "The active app declined ACTION_SET_TEXT on its composer.",
                recoveryHint = "Use Copy fallback and paste into the message field."
            )
        }
    }

    private fun findActiveExternalRootNode(): AccessibilityNodeInfo? {
        val activeRoot = rootInActiveWindow
        if (activeRoot != null && activeRoot.packageName?.toString() != packageName) {
            return activeRoot
        }
        val externalWindow = windows.firstOrNull { window ->
            val root = window.root
            root != null && root.packageName?.toString() != packageName
        }
        return externalWindow?.root
    }

    private fun collectReadableTextNodes(
        node: AccessibilityNodeInfo?,
        out: MutableList<String>,
        maxDepth: Int
    ) {
        if (node == null || maxDepth <= 0) return
        if (node.isPassword || !node.isVisibleToUser) return

        val text = node.text?.toString()?.trim().orEmpty()
        // Exclude editable input fields and ultra-short chrome labels
        if (!node.isEditable && text.length >= 2 && text.length <= 500) {
            if (out.lastOrNull() != text) {
                out.add(text)
            }
        }
        for (i in 0 until node.childCount) {
            collectReadableTextNodes(node.getChild(i), out, maxDepth - 1)
        }
    }

    private fun findFirstEditableNode(
        node: AccessibilityNodeInfo?,
        maxDepth: Int
    ): AccessibilityNodeInfo? {
        if (node == null || maxDepth <= 0) return null
        if (node.isEditable && !node.isPassword && node.isVisibleToUser) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findFirstEditableNode(node.getChild(i), maxDepth - 1)
            if (found != null) return found
        }
        return null
    }

    companion object {
        @Volatile
        private var instanceRef: WeakReference<DexDictateAccessibilityService>? = null

        fun activeInstance(): DexDictateAccessibilityService? = instanceRef?.get()
    }
}

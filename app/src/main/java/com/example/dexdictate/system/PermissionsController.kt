package com.example.dexdictate.system

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.dexdictate.model.PermissionStatusSnapshot
import com.example.dexdictate.providers.SpeechRecognitionProvider

/**
 * Checks runtime & system permission states and constructs safe Android Settings intents
 * with plain-language recovery guidance whenever a permission is missing or denied.
 */
class PermissionsController(
    private val context: Context,
    private val speechRecognitionProvider: SpeechRecognitionProvider? = null
) {
    private val appContext = context.applicationContext

    fun canDrawOverlays(): Boolean {
        return runCatching { Settings.canDrawOverlays(appContext) }.getOrDefault(false)
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        if (DexDictateAccessibilityService.activeInstance() != null) return true
        return runCatching {
            val expectedComponent = ComponentName(
                appContext,
                DexDictateAccessibilityService::class.java
            ).flattenToString()
            val enabledServices = Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            enabledServices.split(':').any { entry ->
                entry.equals(expectedComponent, ignoreCase = true) ||
                    entry.endsWith(DexDictateAccessibilityService::class.java.simpleName)
            }
        }.getOrDefault(false)
    }

    fun snapshot(): PermissionStatusSnapshot {
        val caps = speechRecognitionProvider?.inspectCapabilities()
        return PermissionStatusSnapshot(
            canDrawOverlays = canDrawOverlays(),
            hasRecordAudioPermission = hasRecordAudioPermission(),
            hasNotificationsPermission = hasNotificationsPermission(),
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(),
            isLiveSystemOverlayRunning = DexDictateOverlayService.isRunning.value,
            isOnDeviceSpeechAvailable = caps?.isOnDeviceAvailable == true
        )
    }

    fun createOverlaySettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createAppSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun recoveryHintForMicrophoneDenial(): String {
        return "Microphone permission was denied. Tap 'Open App Settings' -> Permissions -> Microphone -> Allow while using the app, or use Typed Fallback right now."
    }

    fun recoveryHintForOverlayDenial(): String {
        return "Overlay permission is off. Tap 'Enable Overlay Permission', toggle 'Allow display over other apps' for DexDictate, then return here. The in-app demo works without this permission."
    }

    fun recoveryHintForAccessibilityOff(): String {
        return "DexDictate On-Demand Reader is off. Enable it in Android Settings -> Accessibility -> Installed apps -> DexDictate if you want Dexter to read or insert text in other apps."
    }
}

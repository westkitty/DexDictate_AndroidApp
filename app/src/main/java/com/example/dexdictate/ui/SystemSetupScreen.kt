package com.example.dexdictate.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.dexdictate.brand.BrandAssetManager
import com.example.dexdictate.brand.DexterAvatarBadge
import com.example.dexdictate.brand.DexterExpression
import com.example.dexdictate.model.OverlayUiState
import com.example.dexdictate.model.PermissionStatusSnapshot
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedNavySurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedSlate

/**
 * System & Setup Hub:
 * - Launches/stops the real native WindowManager overlay over other apps
 * - Displays live permission readiness and recovery actions
 * - Surfaces honest boundaries between internal playground demos and real cross-app Accessibility behavior
 * - Inspects BrandAssetManager placeholder slots (01..11) ready for official Dexter PNG drop-in
 */
@Composable
fun SystemSetupScreen(
    overlayState: OverlayUiState,
    permissions: PermissionStatusSnapshot,
    speechCapabilitySummary: String,
    ttsProviderName: String,
    onToggleLiveSystemOverlay: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationsPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSelectDexterVariant: (Int) -> Unit,
    onClearTemporaryData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assetDescriptors = BrandAssetManager.resolveAllVariants(context)
    val anyOfficialLoaded = assetDescriptors.any { !it.isPlaceholder }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .navigationBarsPadding()
            .testTag("system_setup_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "DexDictate System & Setup",
                style = MaterialTheme.typography.headlineSmall,
                color = IceWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Manage native Android overlay permissions, on-demand Accessibility reading, Dexter brand assets, and privacy controls.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate
            )
        }

        // Real WindowManager floating overlay control card
        item {
            Surface(
                color = DeepNavySurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.25.dp, ElectricCyan.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Layers, contentDescription = null, tint = ElectricCyan)
                        Text(
                            text = "Native WindowManager Overlay",
                            style = MaterialTheme.typography.titleMedium,
                            color = IceWhite
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (permissions.isLiveSystemOverlayRunning) {
                            "Live floating Dexter is active over other apps using non-focusable WindowManager flags so your Android keyboard stays open."
                        } else if (permissions.canDrawOverlays) {
                            "Overlay permission is granted. Start the real system overlay to float Dexter above external messaging apps."
                        } else {
                            "Grant 'Display over other apps' permission to test the real system-level WindowManager overlay. The internal demo playground works without it."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onToggleLiveSystemOverlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_live_overlay_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (permissions.isLiveSystemOverlayRunning) CoralError else ElectricCyan,
                            contentColor = MidnightNavy
                        )
                    ) {
                        Text(
                            text = when {
                                permissions.isLiveSystemOverlayRunning -> "Stop Live System Overlay"
                                permissions.canDrawOverlays -> "Start Live Floating Dexter"
                                else -> "Enable Overlay Permission in Settings"
                            }
                        )
                    }
                }
            }
        }

        // Permissions inventory
        item {
            Text(
                text = "Permissions & System Services",
                style = MaterialTheme.typography.titleMedium,
                color = IceWhite
            )
        }

        item {
            PermissionStatusCard(
                icon = Icons.Filled.Layers,
                title = "Display Over Other Apps",
                permissionKey = "SYSTEM_ALERT_WINDOW",
                granted = permissions.canDrawOverlays,
                description = "Required only for floating Dexter over external apps via WindowManager.",
                actionLabel = if (permissions.canDrawOverlays) "Review Setting" else "Open Overlay Settings",
                onAction = onOpenOverlaySettings,
                testTag = "perm_card_overlay"
            )
        }

        item {
            PermissionStatusCard(
                icon = Icons.Filled.Mic,
                title = "Microphone (Dictate Only)",
                permissionKey = "RECORD_AUDIO",
                granted = permissions.hasRecordAudioPermission,
                description = "Used strictly after you tap the Microphone button. Zero background recording. $speechCapabilitySummary",
                actionLabel = if (permissions.hasRecordAudioPermission) "Granted" else "Grant Microphone",
                onAction = onRequestMicPermission,
                testTag = "perm_card_mic"
            )
        }

        item {
            PermissionStatusCard(
                icon = Icons.Filled.AccessibilityNew,
                title = "DexDictate On-Demand Reader",
                permissionKey = "BIND_ACCESSIBILITY_SERVICE (Opt-In)",
                granted = permissions.isAccessibilityServiceEnabled,
                description = "Optional cross-app reader & safe composer inserter. Queries visible text ONLY when you tap a button and never presses Send or bypasses secure windows.",
                actionLabel = if (permissions.isAccessibilityServiceEnabled) "Manage Accessibility" else "Open Accessibility Settings",
                onAction = onOpenAccessibilitySettings,
                testTag = "perm_card_accessibility"
            )
        }

        item {
            PermissionStatusCard(
                icon = Icons.Filled.Notifications,
                title = "Active Overlay Notification",
                permissionKey = "POST_NOTIFICATIONS",
                granted = permissions.hasNotificationsPermission,
                description = "Shows a persistent notification with a one-tap Hide button while the live WindowManager overlay is active.",
                actionLabel = if (permissions.hasNotificationsPermission) "Granted" else "Allow Notifications",
                onAction = onRequestNotificationsPermission,
                testTag = "perm_card_notifications"
            )
        }

        // BrandAssetManager status & Dexter expression slots
        item {
            Surface(
                color = DeepNavySurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (anyOfficialLoaded) EmeraldVerified else AmberAlert.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BrandAssetManager — Dexter Assets",
                        style = MaterialTheme.typography.titleMedium,
                        color = IceWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (anyOfficialLoaded) {
                            "Official DexDictate assets detected."
                        } else {
                            "TEMPORARY PLACEHOLDER MODE: Official Dexter PNGs (dexdictate_icon_standard_01..11.png) were not attached to this workspace. Dexter's illustrated face is NOT fabricated; drop the official PNGs into app/src/main/res/drawable/ to activate authentic artwork automatically."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (anyOfficialLoaded) EmeraldVerified else AmberAlert
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DexterAvatarBadge(
                                expression = DexterExpression.IDLE,
                                diameter = 52.dp,
                                artworkAlpha = 0.56f,
                                preferredVariant = overlayState.selectedDexterVariant,
                                showExpandedWordmark = false
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Collapsed (55%)", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DexterAvatarBadge(
                                expression = DexterExpression.EXPANDED,
                                diameter = 84.dp,
                                artworkAlpha = 1.0f,
                                preferredVariant = overlayState.selectedDexterVariant,
                                showExpandedWordmark = true
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Expanded (100%)", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Selectable Standard Variant Slot (01–11)",
                        style = MaterialTheme.typography.labelMedium,
                        color = IceWhite
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BrandAssetManager.SUPPORTED_STANDARD_VARIANTS.toList()) { variant ->
                            val isSelected = overlayState.selectedDexterVariant == variant
                            Surface(
                                color = if (isSelected) ElectricCyan else ElevatedNavySurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) IceWhite else ElectricCyan.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable { onSelectDexterVariant(variant) }
                                    .testTag("variant_slot_$variant")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${variant.toString().padStart(2, '0')}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MidnightNavy else IceWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Privacy & modular provider boundary summary
        item {
            Surface(
                color = DeepNavySurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldVerified)
                        Text(
                            text = "Privacy & Modular Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            color = IceWhite
                        )
                    }
                    Text(
                        text = "• Never sends messages automatically\n• Zero background microphone listening or continuous screen monitoring\n• No analytics, cloud database, accounts, or message history persistence\n• Captured text & transcripts clear automatically when collapsing Dexter\n• Voice adapter: $ttsProviderName\n• Explanation adapter: Offline Deterministic Provider (Round 1)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onClearTemporaryData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("system_clear_temp_data")
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Temporary Session Data Now", color = IceWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    icon: ImageVector,
    title: String,
    permissionKey: String,
    granted: Boolean,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    Surface(
        color = DeepNavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (granted) EmeraldVerified.copy(alpha = 0.6f) else MutedSlate.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(icon, contentDescription = null, tint = if (granted) EmeraldVerified else ElectricCyan)
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = IceWhite)
                        Text(permissionKey, style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = if (granted) EmeraldVerified else AmberAlert,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (granted) "Ready" else "Off",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (granted) EmeraldVerified else AmberAlert
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(actionLabel, color = IceWhite)
            }
        }
    }
}

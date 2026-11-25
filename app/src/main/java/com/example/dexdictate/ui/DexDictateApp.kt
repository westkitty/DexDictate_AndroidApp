package com.example.dexdictate.ui

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.dexdictate.model.AppDestination
import com.example.dexdictate.model.DemoChatTheme
import com.example.dexdictate.model.OverlayVisibility
import com.example.dexdictate.system.DexDictateOverlayService
import com.example.dexdictate.system.OverlayController
import com.example.dexdictate.system.OverlayRuntimeBridge
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedNavySurface
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedSlate

class DexDictateViewModel(application: Application) : AndroidViewModel(application) {
    val controller: OverlayController = OverlayRuntimeBridge.getOrCreate(application)
}

@Composable
fun DexDictateApp(
    controller: OverlayController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val overlayState by controller.overlayState.collectAsStateWithLifecycle()
    val destination by controller.activeDestination.collectAsStateWithLifecycle()
    val scenarios by controller.scenarios.collectAsStateWithLifecycle()
    val permissions by controller.permissions.collectAsStateWithLifecycle()

    // Refresh permissions whenever returning from Android System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                controller.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        controller.onMicrophonePermissionResult(granted)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        controller.refreshPermissions()
    }

    // Back button collapses expanded Dexter first, then returns from System Hub to Demo 1
    BackHandler(enabled = overlayState.visibility == OverlayVisibility.EXPANDED) {
        controller.onCollapseOverlay()
    }
    BackHandler(enabled = overlayState.visibility != OverlayVisibility.EXPANDED && destination != AppDestination.DEMO_GRINDR) {
        controller.selectDestination(AppDestination.DEMO_GRINDR)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Compact edge-to-edge top switcher separating Demo 1, Demo 2, and System Setup
            DexDictateTopModeBar(
                activeDestination = destination,
                isOverlayHidden = overlayState.visibility == OverlayVisibility.HIDDEN,
                onSelectDestination = { controller.selectDestination(it) },
                onRestoreOverlay = { controller.onRestoreOverlay() }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (destination) {
                    AppDestination.DEMO_GRINDR -> {
                        val scenario = scenarios[DemoChatTheme.GRINDR_INSPIRED]
                        if (scenario != null) {
                            DemoChatPlaygroundScreen(
                                scenario = scenario,
                                onSelectMessage = { msgId ->
                                    controller.selectDemoMessage(DemoChatTheme.GRINDR_INSPIRED, msgId)
                                },
                                onComposerChanged = { text ->
                                    controller.updateDemoComposer(DemoChatTheme.GRINDR_INSPIRED, text)
                                },
                                onSendManualMessage = {
                                    controller.sendDemoMessageManually(DemoChatTheme.GRINDR_INSPIRED)
                                }
                            )
                        }
                    }

                    AppDestination.DEMO_SNIFFIES -> {
                        val scenario = scenarios[DemoChatTheme.SNIFFIES_INSPIRED]
                        if (scenario != null) {
                            DemoChatPlaygroundScreen(
                                scenario = scenario,
                                onSelectMessage = { msgId ->
                                    controller.selectDemoMessage(DemoChatTheme.SNIFFIES_INSPIRED, msgId)
                                },
                                onComposerChanged = { text ->
                                    controller.updateDemoComposer(DemoChatTheme.SNIFFIES_INSPIRED, text)
                                },
                                onSendManualMessage = {
                                    controller.sendDemoMessageManually(DemoChatTheme.SNIFFIES_INSPIRED)
                                }
                            )
                        }
                    }

                    AppDestination.SYSTEM_HUB -> {
                        SystemSetupScreen(
                            overlayState = overlayState,
                            permissions = permissions,
                            speechCapabilitySummary = controller.recognitionProvider.inspectCapabilities().description,
                            ttsProviderName = controller.playbackProvider.providerName,
                            onToggleLiveSystemOverlay = {
                                if (permissions.isLiveSystemOverlayRunning) {
                                    DexDictateOverlayService.stop(context)
                                } else if (permissions.canDrawOverlays) {
                                    DexDictateOverlayService.start(context)
                                } else {
                                    runCatching {
                                        context.startActivity(controller.permissionsController.createOverlaySettingsIntent())
                                    }
                                }
                                controller.refreshPermissions()
                            },
                            onOpenOverlaySettings = {
                                runCatching {
                                    context.startActivity(controller.permissionsController.createOverlaySettingsIntent())
                                }
                            },
                            onRequestMicPermission = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onRequestNotificationsPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onOpenAccessibilitySettings = {
                                runCatching {
                                    context.startActivity(controller.permissionsController.createAccessibilitySettingsIntent())
                                }
                            },
                            onSelectDexterVariant = { controller.onSelectDexterVariant(it) },
                            onClearTemporaryData = { controller.onClearTemporaryData() }
                        )
                    }
                }
            }
        }

        // In-app floating Dexter assistant layer (active across Demo 1 and Demo 2 when not running live system overlay)
        if (destination != AppDestination.SYSTEM_HUB && !permissions.isLiveSystemOverlayRunning) {
            InAppFloatingDexterHost(
                state = overlayState,
                onExpand = { controller.onExpandOverlay(fromLiveSystemOverlay = false) },
                onCollapse = { controller.onCollapseOverlay() },
                onHide = { controller.onHideOverlay() },
                onReadAloud = { controller.onReadAloudClicked(fromLiveSystemOverlay = false) },
                onStopPlayback = { controller.onStopPlayback() },
                onReplayPlayback = { controller.onReplayPlayback() },
                onExplainMessage = { controller.onExplainMessageClicked(fromLiveSystemOverlay = false) },
                onDictateReply = { controller.onDictateClicked(fromLiveSystemOverlay = false) },
                onAcceptNetworkDisclosure = { controller.onAcceptNetworkRecognitionDisclosure() },
                onStopDictation = { controller.onStopDictation() },
                onOpenTypedFallback = { controller.onUseTypedFallback() },
                onRequestMicPermission = {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onOpenAppSettings = {
                    runCatching {
                        context.startActivity(controller.permissionsController.createAppSettingsIntent())
                    }
                },
                onReviewDraftChanged = { controller.onReviewDraftChanged(it) },
                onPlayReviewDraft = { controller.onPlayReviewDraft() },
                onInsertReviewedReply = { controller.onInsertReviewedReply() },
                onCopyReviewedReply = { controller.onCopyReviewedReplyFallback() },
                onClearTempData = { controller.onClearTemporaryData() }
            )
        }
    }
}

@Composable
private fun DexDictateTopModeBar(
    activeDestination: AppDestination,
    isOverlayHidden: Boolean,
    onSelectDestination: (AppDestination) -> Unit,
    onRestoreOverlay: () -> Unit
) {
    Surface(
        color = DeepNavySurface,
        border = BorderStroke(0.75.dp, ElectricCyan.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModePillButton(
                    icon = Icons.Filled.ChatBubble,
                    label = "Demo 1",
                    selected = activeDestination == AppDestination.DEMO_GRINDR,
                    testTag = "nav_demo_grindr",
                    onClick = { onSelectDestination(AppDestination.DEMO_GRINDR) },
                    modifier = Modifier.weight(1f)
                )
                ModePillButton(
                    icon = Icons.Filled.Forum,
                    label = "Demo 2",
                    selected = activeDestination == AppDestination.DEMO_SNIFFIES,
                    testTag = "nav_demo_sniffies",
                    onClick = { onSelectDestination(AppDestination.DEMO_SNIFFIES) },
                    modifier = Modifier.weight(1f)
                )
                ModePillButton(
                    icon = Icons.Filled.Settings,
                    label = "System",
                    selected = activeDestination == AppDestination.SYSTEM_HUB,
                    testTag = "nav_system_hub",
                    onClick = { onSelectDestination(AppDestination.SYSTEM_HUB) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isOverlayHidden && activeDestination != AppDestination.SYSTEM_HUB) {
                Button(
                    onClick = onRestoreOverlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("action_restore_overlay"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = MidnightNavy
                    )
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cd_action_restore_overlay))
                }
            }
        }
    }
}

@Composable
private fun ModePillButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) ElectricCyan else ElevatedNavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) IceWhite else ElectricCyan.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MidnightNavy else ElectricCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MidnightNavy else IceWhite,
                maxLines = 1
            )
        }
    }
}

package com.example.dexdictate.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dexdictate.brand.DexterAvatarBadge
import com.example.dexdictate.brand.DexterExpression
import com.example.dexdictate.model.InsertionResult
import com.example.dexdictate.model.OverlayActionTab
import com.example.dexdictate.model.OverlayUiState
import com.example.dexdictate.model.OverlayVisibility
import com.example.dexdictate.model.SpeechPlaybackState
import com.example.dexdictate.model.SpeechRecognitionState
import com.example.dexdictate.model.TextSourceKind
import com.example.dexdictate.state.OverlayStateMachine
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedNavySurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OverlayPanelNavy
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.roundToInt

private enum class PrimaryActionVisualState {
    DEFAULT,
    SELECTED,
    LOADING,
    DISABLED,
    ERROR
}

private fun isReducedMotionEnabled(context: Context): Boolean {
    return runCatching {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        scale == 0f
    }.getOrDefault(false)
}

/**
 * In-app host for the floating Dexter assistant (used inside the Demo Playground).
 * Handles safe drag bounds and animated horizontal centering without blocking the composer or keyboard.
 */
@Composable
fun InAppFloatingDexterHost(
    state: OverlayUiState,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onHide: () -> Unit,
    onReadAloud: () -> Unit,
    onStopPlayback: () -> Unit,
    onReplayPlayback: () -> Unit,
    onExplainMessage: () -> Unit,
    onDictateReply: () -> Unit,
    onAcceptNetworkDisclosure: () -> Unit,
    onStopDictation: () -> Unit,
    onOpenTypedFallback: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onReviewDraftChanged: (String) -> Unit,
    onPlayReviewDraft: () -> Unit,
    onInsertReviewedReply: () -> Unit,
    onCopyReviewedReply: () -> Unit,
    onClearTempData: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.visibility == OverlayVisibility.HIDDEN) return

    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val animDuration = if (reducedMotion) 0 else 250

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val bubblePx = with(density) { 54.dp.toPx() }
        val marginPx = with(density) { 14.dp.toPx() }

        // Default collapsed position: upper-right edge so incoming bubbles and composer remain clear
        var offsetX by remember(maxWidthPx) {
            mutableFloatStateOf((maxWidthPx - bubblePx - marginPx).coerceAtLeast(marginPx))
        }
        var offsetY by remember(maxHeightPx) {
            mutableFloatStateOf(with(density) { 92.dp.toPx() }.coerceAtMost((maxHeightPx - bubblePx - marginPx).coerceAtLeast(marginPx)))
        }

        val isExpanded = state.visibility == OverlayVisibility.EXPANDED

        val animatedCollapsedX by animateFloatAsState(
            targetValue = if (isExpanded) (maxWidthPx - bubblePx) / 2f else offsetX,
            animationSpec = tween(durationMillis = animDuration),
            label = "dexter_x"
        )
        val animatedCollapsedY by animateFloatAsState(
            targetValue = if (isExpanded) with(density) { 64.dp.toPx() } else offsetY,
            animationSpec = tween(durationMillis = animDuration),
            label = "dexter_y"
        )

        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedCollapsedX.roundToInt(), animatedCollapsedY.roundToInt()) }
                    .pointerInput(maxWidthPx, maxHeightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x)
                                .coerceIn(marginPx, (maxWidthPx - bubblePx - marginPx).coerceAtLeast(marginPx))
                            offsetY = (offsetY + dragAmount.y)
                                .coerceIn(marginPx, (maxHeightPx - bubblePx - marginPx).coerceAtLeast(marginPx))
                        }
                    }
            ) {
                CollapsedDexterBubble(
                    variantNumber = state.selectedDexterVariant,
                    onClick = onExpand
                )
            }
        } else {
            // Expanded state centers horizontally near top so the chat composer and Android keyboard stay visible below
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, start = 14.dp, end = 14.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                ExpandedDexterPanel(
                    state = state,
                    animDurationMs = animDuration,
                    onCollapse = onCollapse,
                    onHide = onHide,
                    onReadAloud = onReadAloud,
                    onStopPlayback = onStopPlayback,
                    onReplayPlayback = onReplayPlayback,
                    onExplainMessage = onExplainMessage,
                    onDictateReply = onDictateReply,
                    onAcceptNetworkDisclosure = onAcceptNetworkDisclosure,
                    onStopDictation = onStopDictation,
                    onOpenTypedFallback = onOpenTypedFallback,
                    onRequestMicPermission = onRequestMicPermission,
                    onOpenAppSettings = onOpenAppSettings,
                    onReviewDraftChanged = onReviewDraftChanged,
                    onPlayReviewDraft = onPlayReviewDraft,
                    onInsertReviewedReply = onInsertReviewedReply,
                    onCopyReviewedReply = onCopyReviewedReply,
                    onClearTempData = onClearTempData
                )
            }
        }
    }
}

/**
 * Shared overlay content renderer used inside the native WindowManager foreground service.
 */
@Composable
fun FloatingDexterOverlayContent(
    state: OverlayUiState,
    isSystemOverlayWindow: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onHide: () -> Unit,
    onDragCollapsedBy: (Float, Float) -> Unit,
    onReadAloud: () -> Unit,
    onStopPlayback: () -> Unit,
    onReplayPlayback: () -> Unit,
    onExplainMessage: () -> Unit,
    onDictateReply: () -> Unit,
    onAcceptNetworkDisclosure: () -> Unit,
    onStopDictation: () -> Unit,
    onOpenTypedFallback: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onReviewDraftChanged: (String) -> Unit,
    onPlayReviewDraft: () -> Unit,
    onInsertReviewedReply: () -> Unit,
    onCopyReviewedReply: () -> Unit,
    onClearTempData: () -> Unit
) {
    if (state.visibility == OverlayVisibility.HIDDEN) return

    val context = LocalContext.current
    val animDuration = remember(context) { if (isReducedMotionEnabled(context)) 0 else 250 }

    when (state.visibility) {
        OverlayVisibility.COLLAPSED -> {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragCollapsedBy(dragAmount.x, dragAmount.y)
                        }
                    }
            ) {
                CollapsedDexterBubble(
                    variantNumber = state.selectedDexterVariant,
                    onClick = onExpand
                )
            }
        }
        OverlayVisibility.EXPANDED -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isSystemOverlayWindow) 14.dp else 0.dp, vertical = 4.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                ExpandedDexterPanel(
                    state = state,
                    animDurationMs = animDuration,
                    onCollapse = onCollapse,
                    onHide = onHide,
                    onReadAloud = onReadAloud,
                    onStopPlayback = onStopPlayback,
                    onReplayPlayback = onReplayPlayback,
                    onExplainMessage = onExplainMessage,
                    onDictateReply = onDictateReply,
                    onAcceptNetworkDisclosure = onAcceptNetworkDisclosure,
                    onStopDictation = onStopDictation,
                    onOpenTypedFallback = onOpenTypedFallback,
                    onRequestMicPermission = onRequestMicPermission,
                    onOpenAppSettings = onOpenAppSettings,
                    onReviewDraftChanged = onReviewDraftChanged,
                    onPlayReviewDraft = onPlayReviewDraft,
                    onInsertReviewedReply = onInsertReviewedReply,
                    onCopyReviewedReply = onCopyReviewedReply,
                    onClearTempData = onClearTempData
                )
            }
        }
        OverlayVisibility.HIDDEN -> Unit
    }
}

/**
 * STATE A — COLLAPSED:
 * - 52 dp visible diameter (>= 48 dp accessible touch target)
 * - ~55% opacity artwork with subtle cyan border
 * - No secondary menu, no "DEXTER IS HERE" heading
 * - Non-focusable so tapping never steals IME keyboard focus
 */
@Composable
fun CollapsedDexterBubble(
    variantNumber: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openLabel = stringResource(R.string.cd_dexter_collapsed)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .focusProperties { canFocus = false }
            .testTag("dexter_collapsed_bubble")
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = openLabel
                role = Role.Button
                stateDescription = "Collapsed"
            },
        contentAlignment = Alignment.Center
    ) {
        DexterAvatarBadge(
            expression = DexterExpression.IDLE,
            diameter = 52.dp,
            artworkAlpha = 0.56f,
            preferredVariant = variantNumber,
            showExpandedWordmark = false,
            borderWidth = 1.5.dp,
            accessibilityLabel = openLabel
        )
    }
}

/**
 * STATE B — EXPANDED:
 * - 88 dp fully opaque Dexter circle centered above & overlapping the upper edge of the panel
 * - Centered heading "DEXTER IS HERE" visually balanced between the circle and controls
 * - Exactly three icon-only primary buttons: Speaker ("Read aloud"), Question bubble ("Explain message"), Microphone ("Dictate reply")
 * - Compact dark navy translucent panel with thin cyan border, rounded corners, and restrained glow
 */
@Composable
fun ExpandedDexterPanel(
    state: OverlayUiState,
    animDurationMs: Int,
    onCollapse: () -> Unit,
    onHide: () -> Unit,
    onReadAloud: () -> Unit,
    onStopPlayback: () -> Unit,
    onReplayPlayback: () -> Unit,
    onExplainMessage: () -> Unit,
    onDictateReply: () -> Unit,
    onAcceptNetworkDisclosure: () -> Unit,
    onStopDictation: () -> Unit,
    onOpenTypedFallback: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onReviewDraftChanged: (String) -> Unit,
    onPlayReviewDraft: () -> Unit,
    onInsertReviewedReply: () -> Unit,
    onCopyReviewedReply: () -> Unit,
    onClearTempData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expression = OverlayStateMachine.deriveExpression(state)
    val badgeSize by animateDpAsState(
        targetValue = 88.dp,
        animationSpec = tween(durationMillis = animDurationMs),
        label = "expanded_badge_size"
    )
    val badgeAlpha by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = tween(durationMillis = animDurationMs),
        label = "expanded_badge_alpha"
    )

    val speakerState = when {
        state.activeAction == OverlayActionTab.READ_ALOUD && state.isLoading -> PrimaryActionVisualState.LOADING
        state.activeAction == OverlayActionTab.READ_ALOUD &&
            (state.readError != null || state.playbackState is SpeechPlaybackState.Error) -> PrimaryActionVisualState.ERROR
        state.activeAction == OverlayActionTab.READ_ALOUD ||
            state.playbackState is SpeechPlaybackState.Speaking -> PrimaryActionVisualState.SELECTED
        else -> PrimaryActionVisualState.DEFAULT
    }

    val explainState = when {
        state.activeAction == OverlayActionTab.EXPLAIN_MESSAGE && state.isLoading -> PrimaryActionVisualState.LOADING
        state.activeAction == OverlayActionTab.EXPLAIN_MESSAGE && state.readError != null -> PrimaryActionVisualState.ERROR
        state.activeAction == OverlayActionTab.EXPLAIN_MESSAGE -> PrimaryActionVisualState.SELECTED
        else -> PrimaryActionVisualState.DEFAULT
    }

    val dictateState = when {
        state.recognitionState is SpeechRecognitionState.PermissionRequired ||
            state.recognitionState is SpeechRecognitionState.Error -> PrimaryActionVisualState.ERROR
        state.activeAction == OverlayActionTab.DICTATE_REPLY ||
            state.activeAction == OverlayActionTab.REVIEW_AND_INSERT -> PrimaryActionVisualState.SELECTED
        else -> PrimaryActionVisualState.DEFAULT
    }

    Box(
        modifier = modifier
            .widthIn(max = 396.dp)
            .fillMaxWidth()
            .testTag("dexter_expanded_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Translucent dark-navy panel positioned so the 88dp Dexter circle overlaps its top boundary by 44dp
        Surface(
            modifier = Modifier
                .padding(top = 44.dp)
                .fillMaxWidth()
                .drawBehind {
                    // Restrained cyan ambient glow behind the panel edge
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricCyan.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.width * 0.72f
                        )
                    )
                },
            shape = RoundedCornerShape(24.dp),
            color = OverlayPanelNavy,
            border = BorderStroke(1.25.dp, ElectricCyan.copy(alpha = 0.72f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top corner Hide (left) and Collapse/Close (right) controls flanking the overlapping Dexter badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onHide,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                            .testTag("action_hide_overlay")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = stringResource(R.string.cd_action_hide_overlay),
                            tint = MutedSlate
                        )
                    }

                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                            .testTag("action_collapse_overlay")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_action_collapse_panel),
                            tint = IceWhite
                        )
                    }
                }

                // Balanced vertical spacing: Dexter circle -> DEXTER IS HERE -> [Speaker] [Question] [Mic]
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.dexter_heading),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        fontSize = 15.sp
                    ),
                    color = IceWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dexter_heading_text")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exactly three icon-only primary buttons (zero visible text labels inside)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DexterPrimaryIconButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        accessibleLabel = stringResource(R.string.cd_action_read_aloud),
                        visualState = speakerState,
                        testTag = "action_read_aloud",
                        onClick = onReadAloud
                    )
                    DexterPrimaryIconButton(
                        icon = Icons.AutoMirrored.Filled.Help,
                        accessibleLabel = stringResource(R.string.cd_action_explain_message),
                        visualState = explainState,
                        testTag = "action_explain_message",
                        onClick = onExplainMessage
                    )
                    DexterPrimaryIconButton(
                        icon = Icons.Filled.Mic,
                        accessibleLabel = stringResource(R.string.cd_action_dictate_reply),
                        visualState = dictateState,
                        testTag = "action_dictate_reply",
                        onClick = onDictateReply
                    )
                }

                // Contextual compact tray expands only when one of the three actions is active
                AnimatedVisibility(
                    visible = state.activeAction != OverlayActionTab.NONE,
                    enter = fadeIn(tween(animDurationMs)) + expandVertically(tween(animDurationMs)),
                    exit = fadeOut(tween(animDurationMs)) + shrinkVertically(tween(animDurationMs))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        HorizontalDivider(color = ElectricCyan.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(10.dp))

                        when (state.activeAction) {
                            OverlayActionTab.READ_ALOUD -> ReadAloudTray(
                                state = state,
                                onStop = onStopPlayback,
                                onReplay = onReplayPlayback,
                                onClear = onClearTempData
                            )
                            OverlayActionTab.EXPLAIN_MESSAGE -> ExplainMessageTray(
                                state = state,
                                onReadAloud = onReadAloud,
                                onClear = onClearTempData
                            )
                            OverlayActionTab.DICTATE_REPLY -> DictateTray(
                                state = state,
                                onAcceptNetworkDisclosure = onAcceptNetworkDisclosure,
                                onStopDictation = onStopDictation,
                                onRetryDictate = onDictateReply,
                                onOpenTypedFallback = onOpenTypedFallback,
                                onRequestMicPermission = onRequestMicPermission,
                                onOpenAppSettings = onOpenAppSettings
                            )
                            OverlayActionTab.REVIEW_AND_INSERT -> ReviewAndInsertTray(
                                state = state,
                                onDraftChanged = onReviewDraftChanged,
                                onPlayDraft = onPlayReviewDraft,
                                onInsert = onInsertReviewedReply,
                                onCopy = onCopyReviewedReply,
                                onClear = onClearTempData
                            )
                            OverlayActionTab.NONE -> Unit
                        }
                    }
                }
            }
        }

        // Fully opaque 88dp Dexter circle centered above & overlapping the upper edge of the panel
        DexterAvatarBadge(
            expression = expression,
            diameter = badgeSize,
            artworkAlpha = badgeAlpha,
            preferredVariant = state.selectedDexterVariant,
            showExpandedWordmark = true,
            borderWidth = 2.5.dp,
            accessibilityLabel = stringResource(R.string.cd_dexter_expanded_logo),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .focusProperties { canFocus = false }
                .testTag("dexter_expanded_badge")
        )
    }
}

@Composable
private fun DexterPrimaryIconButton(
    icon: ImageVector,
    accessibleLabel: String,
    visualState: PrimaryActionVisualState,
    testTag: String,
    onClick: () -> Unit
) {
    val containerColor = when (visualState) {
        PrimaryActionVisualState.SELECTED -> ElectricCyan
        PrimaryActionVisualState.ERROR -> CoralError.copy(alpha = 0.22f)
        PrimaryActionVisualState.DISABLED -> ElevatedNavySurface.copy(alpha = 0.4f)
        PrimaryActionVisualState.DEFAULT, PrimaryActionVisualState.LOADING -> ElevatedNavySurface
    }
    val contentColor = when (visualState) {
        PrimaryActionVisualState.SELECTED -> MidnightNavy
        PrimaryActionVisualState.ERROR -> CoralError
        PrimaryActionVisualState.DISABLED -> MutedSlate.copy(alpha = 0.5f)
        PrimaryActionVisualState.DEFAULT, PrimaryActionVisualState.LOADING -> IceWhite
    }
    val borderColor = when (visualState) {
        PrimaryActionVisualState.SELECTED -> IceWhite
        PrimaryActionVisualState.ERROR -> CoralError
        PrimaryActionVisualState.DISABLED -> MutedSlate.copy(alpha = 0.3f)
        PrimaryActionVisualState.DEFAULT, PrimaryActionVisualState.LOADING -> ElectricCyan.copy(alpha = 0.75f)
    }
    val borderWidth = when (visualState) {
        PrimaryActionVisualState.SELECTED, PrimaryActionVisualState.ERROR -> 2.5.dp
        else -> 1.5.dp
    }
    val semanticState = when (visualState) {
        PrimaryActionVisualState.SELECTED -> "Selected"
        PrimaryActionVisualState.LOADING -> "Loading"
        PrimaryActionVisualState.DISABLED -> "Disabled"
        PrimaryActionVisualState.ERROR -> "Needs attention"
        PrimaryActionVisualState.DEFAULT -> "Ready"
    }

    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(58.dp)
                .minimumInteractiveComponentSize()
                .focusProperties { canFocus = false }
                .testTag(testTag)
                .semantics {
                    contentDescription = accessibleLabel
                    role = Role.Button
                    selected = visualState == PrimaryActionVisualState.SELECTED
                    stateDescription = semanticState
                },
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(borderWidth, borderColor),
            onClick = onClick,
            enabled = visualState != PrimaryActionVisualState.DISABLED
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (visualState == PrimaryActionVisualState.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ElectricCyan,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Non-color-only state badge (active checkmark or error indicator)
        if (visualState == PrimaryActionVisualState.SELECTED || visualState == PrimaryActionVisualState.ERROR) {
            Icon(
                imageVector = if (visualState == PrimaryActionVisualState.SELECTED) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.ErrorOutline
                },
                contentDescription = null,
                tint = if (visualState == PrimaryActionVisualState.SELECTED) EmeraldVerified else CoralError,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(MidnightNavy, CircleShape)
            )
        }
    }
}

@Composable
private fun ReadAloudTray(
    state: OverlayUiState,
    onStop: () -> Unit,
    onReplay: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tray_read_aloud"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.readError != null) {
            ErrorBannerCard(
                title = state.readError,
                recoveryHint = state.readRecoveryHint.orEmpty()
            )
            return
        }

        val captured = state.capturedMessage
        if (captured != null) {
            SourceStatusChip(
                label = when (captured.source) {
                    TextSourceKind.DEMO_PLAYGROUND -> "Demo Playground Message"
                    TextSourceKind.ACCESSIBILITY_SERVICE -> "Accessibility Reader (On-Demand)"
                    TextSourceKind.MANUAL_SELECTION -> "Manual Text"
                }
            )
            Surface(
                color = DeepNavySurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Original message (read verbatim)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = captured.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = IceWhite,
                        modifier = Modifier.testTag("read_aloud_original_text")
                    )
                }
            }
        }

        when (val playback = state.playbackState) {
            is SpeechPlaybackState.Speaking -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = EmeraldVerified,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Speaking via ${playback.engineName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldVerified
                    )
                }
            }
            is SpeechPlaybackState.Error -> {
                ErrorBannerCard(title = playback.message, recoveryHint = playback.recoveryHint)
            }
            is SpeechPlaybackState.Completed -> {
                Text(
                    text = "Finished reading original wording.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }
            SpeechPlaybackState.Idle -> Unit
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .focusProperties { canFocus = false }
                    .testTag("action_stop_playback"),
                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, tint = CoralError, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Stop", color = IceWhite)
            }
            Button(
                onClick = onReplay,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .focusProperties { canFocus = false }
                    .testTag("action_replay_playback"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = MidnightNavy
                )
            ) {
                Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Replay")
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .focusProperties { canFocus = false }
                    .testTag("action_clear_temp_data")
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear temporary text", tint = MutedSlate)
            }
        }
    }
}

@Composable
private fun ExplainMessageTray(
    state: OverlayUiState,
    onReadAloud: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tray_explain_message"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state.readError != null) {
            ErrorBannerCard(
                title = state.readError,
                recoveryHint = state.readRecoveryHint.orEmpty()
            )
            return
        }

        val explanation = state.explanation ?: return

        SourceStatusChip(label = explanation.providerLabel)

        // Original wording card (kept separate from plain-language explanation)
        Surface(
            color = DeepNavySurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ORIGINAL MESSAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = explanation.originalText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IceWhite,
                    modifier = Modifier.testTag("explain_original_text")
                )
            }
        }

        // Plain-language explanation card
        Surface(
            color = ElevatedNavySurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "PLAIN-LANGUAGE EXPLANATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = explanation.plainLanguage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IceWhite,
                    modifier = Modifier.testTag("explain_plain_text")
                )

                if (explanation.slangBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SLANG & SPELLING NOTES",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    explanation.slangBreakdown.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodySmall,
                            color = IceWhite.copy(alpha = 0.92f)
                        )
                    }
                }

                if (!explanation.ambiguityNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: ${explanation.ambiguityNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberAlert
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReadAloud,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .focusProperties { canFocus = false }
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Hear Original", color = IceWhite)
            }
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .focusProperties { canFocus = false }
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear", color = MutedSlate)
            }
        }
    }
}

@Composable
private fun DictateTray(
    state: OverlayUiState,
    onAcceptNetworkDisclosure: () -> Unit,
    onStopDictation: () -> Unit,
    onRetryDictate: () -> Unit,
    onOpenTypedFallback: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tray_dictate"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (val rec = state.recognitionState) {
            is SpeechRecognitionState.PermissionRequired -> {
                ErrorBannerCard(title = rec.message, recoveryHint = rec.recoveryHint)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRequestMicPermission,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                            .testTag("action_grant_mic"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = MidnightNavy
                        )
                    ) {
                        Text("Allow Mic")
                    }
                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Settings", color = IceWhite)
                    }
                }
                OutlinedButton(
                    onClick = onOpenTypedFallback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .focusProperties { canFocus = false }
                        .testTag("action_typed_fallback")
                ) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Use Typed Reply Fallback", color = IceWhite)
                }
            }

            is SpeechRecognitionState.NetworkDisclosureRequired -> {
                Surface(
                    color = DeepNavySurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, AmberAlert)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "NETWORK SPEECH DISCLOSURE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberAlert
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rec.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = IceWhite
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAcceptNetworkDisclosure,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                            .testTag("action_accept_network_speech"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = MidnightNavy
                        )
                    ) {
                        Text("Continue Dictation")
                    }
                    OutlinedButton(
                        onClick = onOpenTypedFallback,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                            .testTag("action_typed_fallback")
                    ) {
                        Text("Type Instead", color = IceWhite)
                    }
                }
            }

            is SpeechRecognitionState.Listening -> {
                Surface(
                    color = ElevatedNavySurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, AmberAlert)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = AmberAlert
                            )
                            Text(
                                text = if (rec.isOnDevice) {
                                    "LISTENING (On-Device)"
                                } else {
                                    "LISTENING (Android System Speech)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberAlert,
                                modifier = Modifier.testTag("dictation_listening_label")
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = rec.partialText.ifBlank { "Speak naturally — Dexter keeps your exact words without rewriting tone." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = IceWhite
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStopDictation,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                            .testTag("action_stop_dictation"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralError,
                            contentColor = MidnightNavy
                        )
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stop Listening")
                    }
                    OutlinedButton(
                        onClick = onOpenTypedFallback,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                            .testTag("action_typed_fallback")
                    ) {
                        Text("Type", color = IceWhite)
                    }
                }
            }

            is SpeechRecognitionState.Error -> {
                ErrorBannerCard(title = rec.message, recoveryHint = rec.recoveryHint)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRetryDictate,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = MidnightNavy
                        )
                    ) {
                        Text("Try Again")
                    }
                    OutlinedButton(
                        onClick = onOpenTypedFallback,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .focusProperties { canFocus = false }
                            .testTag("action_typed_fallback")
                    ) {
                        Text("Typed Fallback", color = IceWhite)
                    }
                }
            }

            else -> {
                OutlinedButton(
                    onClick = onOpenTypedFallback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .focusProperties { canFocus = false }
                        .testTag("action_typed_fallback")
                ) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Typed Reply Fallback", color = IceWhite)
                }
            }
        }
    }
}

@Composable
private fun ReviewAndInsertTray(
    state: OverlayUiState,
    onDraftChanged: (String) -> Unit,
    onPlayDraft: () -> Unit,
    onInsert: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tray_review_insert"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val rec = state.recognitionState
        val statusLabel = when {
            rec is SpeechRecognitionState.TypedFallback -> "Typed Fallback (Offline)"
            state.reviewWasOnDevice == true -> "Raw On-Device Transcript"
            state.reviewWasOnDevice == false -> "Raw System Speech Transcript"
            else -> "Review Reply Before Insert"
        }
        SourceStatusChip(label = statusLabel)

        if (rec is SpeechRecognitionState.TypedFallback) {
            Text(
                text = rec.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MutedSlate
            )
        }

        when (val result = state.insertionResult) {
            is InsertionResult.Inserted -> {
                Surface(
                    color = EmeraldVerified.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldVerified)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .testTag("insertion_verified_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldVerified, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Inserted into ${result.targetDescription}. Press Send in the app when ready.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IceWhite
                        )
                    }
                }
            }
            is InsertionResult.CopiedFallback -> {
                Surface(
                    color = AmberAlert.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AmberAlert)
                ) {
                    Text(
                        text = result.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = IceWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .testTag("insertion_copied_banner")
                    )
                }
            }
            is InsertionResult.Failed -> {
                ErrorBannerCard(title = result.reason, recoveryHint = result.recoveryHint)
            }
            InsertionResult.Idle -> Unit
        }

        OutlinedTextField(
            value = state.reviewDraftText,
            onValueChange = onDraftChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("review_draft_input"),
            label = { Text("Your reply (unedited wording)") },
            placeholder = { Text("Dictated or typed reply appears here...") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = IceWhite),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = MutedSlate.copy(alpha = 0.5f),
                focusedLabelColor = ElectricCyan,
                unfocusedLabelColor = MutedSlate,
                cursorColor = ElectricCyan,
                focusedContainerColor = DeepNavySurface,
                unfocusedContainerColor = DeepNavySurface
            ),
            minLines = 2,
            maxLines = 4
        )

        Text(
            text = "DexDictate inserts into the composer only — it NEVER presses Send.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onPlayDraft,
                enabled = state.reviewDraftText.isNotBlank(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("action_play_review")
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear your reply", modifier = Modifier.size(18.dp))
            }

            Button(
                onClick = onInsert,
                enabled = state.reviewDraftText.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("action_insert_reply"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = MidnightNavy
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Input, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Insert")
            }

            OutlinedButton(
                onClick = onCopy,
                enabled = state.reviewDraftText.isNotBlank(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("action_copy_reply")
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy", color = IceWhite)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear Draft", color = MutedSlate)
            }
        }
    }
}

@Composable
private fun SourceStatusChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = ElectricCyan,
        modifier = Modifier
            .background(ElectricCyan.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun ErrorBannerCard(
    title: String,
    recoveryHint: String
) {
    Surface(
        color = CoralError.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, CoralError)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("overlay_error_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = CoralError, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = CoralError
                )
            }
            if (recoveryHint.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recoveryHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = IceWhite
                )
            }
        }
    }
}

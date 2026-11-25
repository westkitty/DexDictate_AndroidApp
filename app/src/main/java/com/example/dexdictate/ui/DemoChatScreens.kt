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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dexdictate.model.DemoChatMessage
import com.example.dexdictate.model.DemoChatScenario
import com.example.dexdictate.model.DemoChatTheme
import com.example.ui.theme.DemoNavyAccent
import com.example.ui.theme.DemoNavyBg
import com.example.ui.theme.DemoNavyHeader
import com.example.ui.theme.DemoNavyLightBubble
import com.example.ui.theme.DemoNavyOnLightBubble
import com.example.ui.theme.DemoNavyProfileChip
import com.example.ui.theme.DemoYellowAccent
import com.example.ui.theme.DemoYellowBg
import com.example.ui.theme.DemoYellowHeader
import com.example.ui.theme.DemoYellowIncoming
import com.example.ui.theme.DemoYellowOnAccent
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSlate

/**
 * Renders either Demo 1 (Grindr-inspired yellow/dark chat) or Demo 2 (Sniffies-inspired navy/blue private chat).
 * Clearly marked as local fictional test fixtures — never connected to any external service.
 */
@Composable
fun DemoChatPlaygroundScreen(
    scenario: DemoChatScenario,
    onSelectMessage: (String) -> Unit,
    onComposerChanged: (String) -> Unit,
    onSendManualMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isYellowDemo = scenario.theme == DemoChatTheme.GRINDR_INSPIRED
    val bgColor = if (isYellowDemo) DemoYellowBg else DemoNavyBg
    val headerColor = if (isYellowDemo) DemoYellowHeader else DemoNavyHeader
    val accentColor = if (isYellowDemo) DemoYellowAccent else DemoNavyAccent
    val onAccentColor = if (isYellowDemo) DemoYellowOnAccent else IceWhite

    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottomPx > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .imePadding()
            .navigationBarsPadding()
            .testTag(if (isYellowDemo) "demo_screen_grindr" else "demo_screen_sniffies")
    ) {
        // Explicit local test fixture disclosure strip (zero implication of external service affiliation)
        Surface(
            color = headerColor.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.demo_fixture_banner),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            if (isKeyboardVisible) EmeraldVerified.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("keyboard_status_indicator")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = null,
                        tint = if (isKeyboardVisible) EmeraldVerified else MutedSlate,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isKeyboardVisible) "IME Open" else "IME Ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isKeyboardVisible) EmeraldVerified else MutedSlate
                    )
                }
            }
        }

        // Chat partner header (compact profile metadata for Demo 2 Sniffies-inspired & clean avatar bar for Demo 1)
        Surface(
            color = headerColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = scenario.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = IceWhite
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = scenario.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                    }
                }

                // Compact profile info chips (prominently styled for Demo 2 Sniffies-inspired private chat)
                if (scenario.profileMeta.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        scenario.profileMeta.forEach { tag ->
                            Surface(
                                color = if (isYellowDemo) Color(0xFF282930) else DemoNavyProfileChip,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isYellowDemo) DemoYellowAccent else IceWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Helper hint explaining how to target any incoming bubble in the playground
        Text(
            text = "Tap any incoming bubble to target it for Dexter, or tap the floating Dexter circle.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Messages list (shows seeded conversation cleanly and auto-scrolls when new messages are sent)
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        androidx.compose.runtime.LaunchedEffect(scenario.messages.size) {
            if (scenario.messages.size > 4) {
                listState.animateScrollToItem(scenario.messages.lastIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(scenario.messages, key = { it.id }) { msg ->
                DemoMessageBubble(
                    message = msg,
                    isYellowDemo = isYellowDemo,
                    isTargetedForDexter = msg.id == scenario.selectedMessageId,
                    accentColor = accentColor,
                    onAccentColor = onAccentColor,
                    onClick = {
                        if (msg.isIncoming) {
                            onSelectMessage(msg.id)
                        }
                    }
                )
            }
        }

        // Visible composer bar (stays visible with real Android keyboard; Send is strictly manual)
        Surface(
            color = headerColor,
            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = scenario.composerText,
                    onValueChange = onComposerChanged,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("demo_composer_input"),
                    placeholder = {
                        Text(
                            text = "Type or insert Dexter reply...",
                            color = MutedSlate
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = IceWhite),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                        cursorColor = accentColor,
                        focusedContainerColor = bgColor,
                        unfocusedContainerColor = bgColor
                    )
                )

                IconButton(
                    onClick = onSendManualMessage,
                    enabled = scenario.composerText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(
                            if (scenario.composerText.isNotBlank()) accentColor else Color.White.copy(alpha = 0.1f)
                        )
                        .testTag("demo_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_action_send_message),
                        tint = if (scenario.composerText.isNotBlank()) onAccentColor else MutedSlate
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoMessageBubble(
    message: DemoChatMessage,
    isYellowDemo: Boolean,
    isTargetedForDexter: Boolean,
    accentColor: Color,
    onAccentColor: Color,
    onClick: () -> Unit
) {
    val bubbleColor = when {
        !message.isIncoming -> accentColor
        isYellowDemo -> DemoYellowIncoming
        else -> DemoNavyLightBubble
    }
    val textColor = when {
        !message.isIncoming -> onAccentColor
        isYellowDemo -> IceWhite
        else -> DemoNavyOnLightBubble
    }
    val bubbleShape = if (message.isIncoming) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isIncoming) Alignment.Start else Alignment.End
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            border = if (isTargetedForDexter) {
                BorderStroke(2.dp, ElectricCyan)
            } else {
                null
            },
            modifier = Modifier
                .clip(bubbleShape)
                .clickable(enabled = message.isIncoming, onClick = onClick)
                .semantics { selected = isTargetedForDexter }
                .testTag("demo_message_${message.id}")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = if (message.isIncoming) FontWeight.Normal else FontWeight.Medium
                    ),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.68f)
                    )
                    if (isTargetedForDexter && message.isIncoming) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .background(ElectricCyan.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Dexter target",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isYellowDemo) ElectricCyan else Color(0xFF0369A1)
                            )
                        }
                    }
                }
            }
        }
    }
}

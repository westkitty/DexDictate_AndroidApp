package com.example.dexdictate.brand

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CoralError
import com.example.ui.theme.DexterBadgeBlue
import com.example.ui.theme.DexterBadgeDeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.SpaceGroteskFontFamily

/**
 * Selectable Dexter visual expression states.
 * Supports authentic official artwork slots (variants 01..11) when dropped into res/drawable/
 * while explicitly flagging temporary placeholders in Round 1 (never reconstructing Dexter's face).
 */
enum class DexterExpression(
    val displayName: String,
    val expectedDrawableName: String,
    val fallbackVariantNumber: Int
) {
    IDLE("Collapsed Idle", "dexdictate_icon_standard_05", 5),
    EXPANDED("Expanded Ready", "dexdictate_icon_standard_01", 1),
    SPEAKING("Reading Aloud", "dexdictate_icon_standard_03", 3),
    EXPLAINING("Explaining Message", "dexdictate_icon_standard_07", 7),
    LISTENING("Listening / Dictating", "dexdictate_icon_standard_09", 9),
    ERROR("Needs Attention", "dexdictate_icon_standard_11", 11)
}

data class DexterAssetDescriptor(
    val expression: DexterExpression,
    val variantNumber: Int,
    val expectedResourceName: String,
    @DrawableRes val resolvedDrawableRes: Int?,
    val isPlaceholder: Boolean,
    val provenanceNote: String
)

/**
 * BrandAssetManager centralizes DexDictate visual asset resolution.
 *
 * Official DexDictate assets (dexdictate_icon_standard_01..11.png) can be placed in
 * app/src/main/res/drawable/ and will automatically replace the labeled Round 1 placeholders
 * without modifying any UI code.
 */
object BrandAssetManager {

    const val BRAND_BADGE_WORDMARK = "DEXDICTATE"
    val SUPPORTED_STANDARD_VARIANTS: IntRange = 1..11

    fun resolveAsset(
        context: Context,
        expression: DexterExpression,
        preferredVariant: Int = expression.fallbackVariantNumber
    ): DexterAssetDescriptor {
        val clampedVariant = preferredVariant.coerceIn(SUPPORTED_STANDARD_VARIANTS)
        val formattedVariant = clampedVariant.toString().padStart(2, '0')
        val variantResName = "dexdictate_icon_standard_$formattedVariant"

        val variantId = lookupDrawableId(context, variantResName)
        val expressionId = if (variantId != 0) variantId else lookupDrawableId(context, expression.expectedDrawableName)

        return if (expressionId != 0) {
            DexterAssetDescriptor(
                expression = expression,
                variantNumber = clampedVariant,
                expectedResourceName = variantResName,
                resolvedDrawableRes = expressionId,
                isPlaceholder = false,
                provenanceNote = "Official DexDictate drawable loaded ($variantResName)"
            )
        } else {
            DexterAssetDescriptor(
                expression = expression,
                variantNumber = clampedVariant,
                expectedResourceName = variantResName,
                resolvedDrawableRes = null,
                isPlaceholder = true,
                provenanceNote = "Temporary placeholder — drop official $variantResName.png into res/drawable/ to complete branding."
            )
        }
    }

    fun resolveAllVariants(context: Context): List<DexterAssetDescriptor> {
        return SUPPORTED_STANDARD_VARIANTS.map { num ->
            val matchingExpression = DexterExpression.entries.firstOrNull {
                it.fallbackVariantNumber == num
            } ?: DexterExpression.IDLE
            resolveAsset(context, matchingExpression, num)
        }
    }

    @Suppress("DiscouragedApi")
    private fun lookupDrawableId(context: Context, resourceName: String): Int {
        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
}

/**
 * Renders the circular Dexter badge in either authentic asset mode (when PNGs are provided)
 * or clearly labeled temporary placeholder mode (preserving the blue circular DEXDICTATE badge
 * identity and cyan ring without fabricating Dexter's face).
 */
@Composable
fun DexterAvatarBadge(
    expression: DexterExpression,
    diameter: Dp,
    artworkAlpha: Float,
    preferredVariant: Int = expression.fallbackVariantNumber,
    showExpandedWordmark: Boolean = diameter >= 72.dp,
    borderWidth: Dp = if (diameter >= 72.dp) 2.5.dp else 1.5.dp,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = stringResource(R.string.cd_dexter_expanded_logo)
) {
    val context = LocalContext.current
    val descriptor = BrandAssetManager.resolveAsset(context, expression, preferredVariant)

    val borderColor = when (expression) {
        DexterExpression.ERROR -> CoralError
        DexterExpression.LISTENING -> AmberAlert
        else -> ElectricCyan
    }

    val badgeGradient = Brush.radialGradient(
        colors = listOf(
            DexterBadgeBlue,
            DexterBadgeDeepBlue,
            Color(0xFF0C1C3D)
        )
    )

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(badgeGradient)
            .border(width = borderWidth, color = borderColor, shape = CircleShape)
            .semantics { contentDescription = accessibilityLabel },
        contentAlignment = Alignment.Center
    ) {
        if (descriptor.resolvedDrawableRes != null) {
            Image(
                painter = painterResource(id = descriptor.resolvedDrawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(artworkAlpha)
            )
        } else {
            // Explicitly marked temporary placeholder preserving DEXDICTATE badge identity
            // (never attempts to fake Dexter's illustrated Phalène face).
            val iconVector = when (expression) {
                DexterExpression.IDLE, DexterExpression.EXPANDED -> Icons.Filled.Mic
                DexterExpression.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                DexterExpression.EXPLAINING -> Icons.Filled.QuestionAnswer
                DexterExpression.LISTENING -> Icons.Filled.GraphicEq
                DexterExpression.ERROR -> Icons.Filled.ErrorOutline
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(artworkAlpha)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = if (expression == DexterExpression.ERROR) CoralError else ElectricCyan,
                    modifier = Modifier.size(if (showExpandedWordmark) 28.dp else 20.dp)
                )

                if (showExpandedWordmark) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = BrandAssetManager.BRAND_BADGE_WORDMARK,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.8.sp,
                        color = IceWhite,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "TEMP #${descriptor.variantNumber.toString().padStart(2, '0')}",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 7.sp,
                        letterSpacing = 0.5.sp,
                        color = ElectricCyan,
                        modifier = Modifier
                            .background(Color(0x6608101E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "DEX",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                        letterSpacing = 0.5.sp,
                        color = IceWhite.copy(alpha = 0.92f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

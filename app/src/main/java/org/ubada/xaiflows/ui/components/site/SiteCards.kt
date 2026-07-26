/**
 * SiteCards.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Small, reusable card primitives shared by every screen in ui/screens/site/.
 * Each frontend site page (streamlit-frontend's src/features/site/routes directory)
 * repeats one of a handful of card shapes — a badge + title + subtitle row,
 * a numbered text section, an expandable Q&A row, or a tappable contact
 * channel. Four composables here cover all nine screens instead of each
 * screen hand-rolling its own card markup.
 */
package org.ubada.xaiflows.ui.components.site

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CARD_SHAPE = RoundedCornerShape(16.dp)
private val CARD_BACKGROUND = Color.White.copy(alpha = 0.85f)

/**
 * A badge + title + subtitle (+ optional description + trailing action)
 * card. Covers Team members, Careers positions, Media press items, Events,
 * and Webinars — each screen just maps its own fields onto these slots.
 */
@Composable
fun BadgedInfoCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFDBEAFE),
    badgeTextColor: Color = Color(0xFF1D4ED8),
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CARD_BACKGROUND, CARD_SHAPE)
            .padding(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(text = badgeText, color = badgeTextColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(top = 2.dp))
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    trailing()
                }
            }
        }
    }
}

/** A numbered heading + body paragraph. Privacy Policy and Terms of
 *  Service are both just an ordered list of these. */
@Composable
fun SectionCard(heading: String, body: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CARD_BACKGROUND, CARD_SHAPE)
            .padding(16.dp)
    ) {
        Column {
            Text(text = heading, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = body, fontSize = 13.sp, color = Color(0xFF4B5563), lineHeight = 19.sp)
        }
    }
}

/** Tap-to-expand question/answer row, used by FaqScreen. Mirrors the
 *  frontend's single-open-at-a-time accordion via the [expanded] +
 *  [onToggle] pair the caller owns (see FaqScreen.kt). */
@Composable
fun ExpandableFaqCard(
    question: String,
    answer: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "faqChevronRotation")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CARD_BACKGROUND, CARD_SHAPE)
            .animateContentSize()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color(0xFF60A5FA))
                    ) { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp).rotate(rotation)
                )
            }
            if (expanded) {
                Text(
                    text = answer,
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563),
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

/** Tappable contact-method row — email / phone / other channel. Used by
 *  SupportScreen (Contact Support), mirrors ContactInfoComponent's
 *  IntentUtils-driven tap behaviour but with a title+description layout
 *  matching the frontend's support-page.tsx channel cards. **/
@Composable
fun ContactChannelCard(
    icon: ImageVector,
    title: String,
    value: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CARD_BACKGROUND, CARD_SHAPE)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color(0xFF60A5FA))
            ) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF3B82F6).copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text(text = value, fontSize = 13.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 1.dp))
            Text(text = description, fontSize = 11.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

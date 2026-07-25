/**
 * FaqScreen.kt — mirrors frontend's faq-page.tsx natively.
 * Content lives in ui/models/SiteContent.kt (FAQ_ITEMS) — the final entry
 * ("Who do I contact for technical issues?") is appended here so its
 * answer always uses the live COMPANY_INFO.emailSupport value instead of
 * a string frozen at edit time in the config file.
 */
package com.example.xai_flows.ui.screens.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.xai_flows.ui.components.site.ExpandableFaqCard
import com.example.xai_flows.ui.components.site.SitePageScaffold
import com.example.xai_flows.ui.models.COMPANY_INFO
import com.example.xai_flows.ui.models.FAQ_ITEMS
import com.example.xai_flows.ui.models.FaqItem

@Composable
fun FaqScreen(onBack: () -> Unit) {
    // Single-open-at-a-time accordion, same UX as the frontend's FaqPage.
    var openIndex by remember { mutableStateOf<Int?>(null) }

    val allFaqs = remember {
        FAQ_ITEMS + FaqItem(
            question = "Who do I contact for technical issues?",
            answer = "Please email ${COMPANY_INFO.emailSupport} with a description of your issue " +
                "and any relevant screenshots or error logs."
        )
    }

    SitePageScaffold(
        title = "FAQ",
        icon = Icons.Filled.HelpOutline,
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            allFaqs.forEachIndexed { index, faq ->
                ExpandableFaqCard(
                    question = faq.question,
                    answer = faq.answer,
                    expanded = openIndex == index,
                    onToggle = { openIndex = if (openIndex == index) null else index }
                )
            }
        }
    }
}

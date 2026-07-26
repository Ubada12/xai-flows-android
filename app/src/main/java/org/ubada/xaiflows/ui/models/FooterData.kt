/**
 * FooterData.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Immutable data models and constant data for the app footer.
 *
 * Contact details, social links, and section links all live here so designers
 * can update them in one place without touching composables.
 */
package org.ubada.xaiflows.ui.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.ubada.xaiflows.core.navigation.AppRoute

/** [href] is always an [AppRoute].path value — see FooterSectionComponent.kt,
 *  which resolves it back to an [AppRoute] via [AppRoute.fromPath] and
 *  navigates in-app. There is no longer a "leaves the app" case for a
 *  footer nav link (see AppRoute.kt's doc comment for why that used to be
 *  a real, confirmed bug: these hrefs and MainActivity's navigation
 *  whitelist were two independently hand-maintained lists that had
 *  drifted out of sync). */
data class FooterLink(val name: String, val href: String, val icon: ImageVector)
data class FooterSection(val title: String, val links: List<FooterLink>)
data class SocialLink(val name: String, val href: String, val icon: ImageVector, val color: Color)
data class CompanyInfo(
    val name: String,
    val description: String,
    /** General enquiries — matches the frontend's APP_META.email exactly
     *  (streamlit-frontend/src/config/app.config.ts). Was previously
     *  "contact@xai-flows.com" here, a typo'd domain that doesn't match
     *  the frontend's "contact@xaiflows.in" — fixed so both apps point
     *  users at the same real inbox. */
    val email: String,
    /** Careers/HR enquiries — CareersScreen's "Apply Now" mailto target. */
    val emailCareers: String,
    /** Press/media enquiries — MediaScreen's contact line. */
    val emailMedia: String,
    /** Support-ticket enquiries — SupportScreen's "Email Support" channel
     *  and the last FAQ entry's answer. */
    val emailSupport: String,
    val phone: String,
    val address: String
)

val COMPANY_INFO = CompanyInfo(
    name         = "XAI-FLOWS",
    description  = "AI-powered flood risk detection and real-time drainage monitoring for Mumbai.",
    email        = "contact@xaiflows.in",
    emailCareers = "careers@xaiflows.in",
    emailMedia   = "media@xaiflows.in",
    emailSupport = "support@xaiflows.in",
    phone        = "+91 98765 43210",               // Mumbai contact number
    address      = "IIT Bombay, Powai, Mumbai — 400076"
)

// Every href below is an AppRoute.path — four groups, matching the
// frontend's own FOOTER_NAV_GROUPS (Company/Resources/Support, see
// streamlit-frontend's config/app.config.ts) plus one mobile-only
// "Platform" group for in-app quick nav that the frontend doesn't need
// (it already has Home/Predictions/Analytics in its top navbar).
//
// Previously these hrefs were made-up paths ("/about", "/careers",
// "/docs", "/api-docs", "/cookies", ...) that don't exist anywhere on the
// frontend, so nearly every link here silently 404'd when opened in the
// browser. Every link below now points at a real AppRoute backed by a
// real native screen (ui/screens/site/) — see AppRoute.kt.
val FOOTER_SECTIONS = listOf(
    FooterSection(
        "Platform",
        listOf(
            FooterLink("Dashboard",   AppRoute.HOME.path,        Icons.Filled.Home),
            FooterLink("Predictions", AppRoute.PREDICTIONS.path, Icons.Filled.BarChart),
            FooterLink("Analytics",   AppRoute.ANALYTICS.path,   Icons.Filled.BarChart)
        )
    ),
    FooterSection(
        "Company",
        listOf(
            FooterLink("Team",    AppRoute.TEAM.path,    Icons.Filled.People),
            FooterLink("Careers", AppRoute.CAREERS.path, Icons.Filled.Work),
            FooterLink("Media",   AppRoute.MEDIA.path,   Icons.Filled.Newspaper)
        )
    ),
    FooterSection(
        "Resources",
        listOf(
            FooterLink("Events",   AppRoute.EVENTS.path,   Icons.Filled.Event),
            FooterLink("Webinars", AppRoute.WEBINARS.path, Icons.Filled.Videocam),
            FooterLink("FAQ",      AppRoute.FAQ.path,      Icons.Filled.Help)
        )
    ),
    FooterSection(
        "Support",
        listOf(
            FooterLink("Contact Support",  AppRoute.SUPPORT.path,          Icons.Filled.Headset),
            FooterLink("Privacy Policy",   AppRoute.PRIVACY_POLICY.path,   Icons.Filled.Security),
            FooterLink("Terms of Service", AppRoute.TERMS_OF_SERVICE.path, Icons.Filled.Description)
        )
    )
)

// Matches the frontend's SOCIAL_LINKS exactly (app.config.ts) — same 4
// profiles, same URLs. Facebook was previously listed here with no
// frontend counterpart (the platform has no Facebook presence to link
// to), so it's dropped rather than advertising a profile that doesn't
// exist. Material Icons Extended has no official brand marks for these
// platforms, so each uses the closest semantic stand-in (documented
// per-entry below) rather than pulling in a whole new icon library for
// four logos.
val SOCIAL_LINKS = listOf(
    SocialLink("Twitter",   "https://twitter.com/xaiflows",            Icons.Filled.Share,     Color(0xFF1DA1F2)), // bird logo unavailable — "share" stands in
    SocialLink("LinkedIn",  "https://linkedin.com/company/xaiflows",   Icons.Filled.Work,      Color(0xFF0077B5)), // professional-network stand-in
    SocialLink("Instagram", "https://instagram.com/xaiflows",          Icons.Filled.Favorite,  Color(0xFFE1306C)), // camera logo unavailable — heart stands in
    SocialLink("GitHub",    "https://github.com/xaiflows",             Icons.Filled.Code,      Color(0xFF6e5494))  // octocat unavailable — code glyph stands in
)

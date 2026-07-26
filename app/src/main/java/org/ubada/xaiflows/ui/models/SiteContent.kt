/**
 * SiteContent.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Config-driven content for every "site" screen (ui/screens/site/) — Team,
 * Careers, Media, Events, Webinars, FAQ, Support, Privacy Policy, Terms of
 * Service. One file, one place to edit: adding a team member, a press
 * mention, or an FAQ entry never touches a composable, exactly the same
 * philosophy as FooterData.kt's COMPANY_INFO/FOOTER_SECTIONS.
 *
 * Ported 1:1 from the frontend's real content — streamlit-frontend's
 * src/features/site/routes directory — so both apps say the same thing. Since
 * mobile (Kotlin) and web (TypeScript) are separate codebases with no
 * shared package, this file has to be kept in sync BY HAND when the
 * frontend's content changes; there's no automated link between them.
 * Search each frontend route file's name in a comment above the matching
 * section below to find its counterpart quickly.
 */
package org.ubada.xaiflows.ui.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// ─── Team (frontend: team-page.tsx) ────────────────────────────────────────

data class TeamMember(val name: String, val role: String, val description: String, val icon: ImageVector)

val TEAM_MEMBERS = listOf(
    TeamMember(
        name = "Dr. Ananya Sharma",
        role = "Lead Data Scientist",
        description = "Specialises in hydrological modelling and machine learning for flood prediction systems.",
        icon = Icons.Filled.Psychology
    ),
    TeamMember(
        name = "Rajesh Kumar",
        role = "Full-Stack Developer",
        description = "Builds and maintains the web platform, ensuring a seamless experience across all devices.",
        icon = Icons.Filled.Public
    ),
    TeamMember(
        name = "Priya Patel",
        role = "GIS & Remote Sensing Analyst",
        description = "Handles geospatial data processing, drainage mapping, and satellite imagery analysis.",
        icon = Icons.Filled.Storage
    ),
    TeamMember(
        name = "Amit Verma",
        role = "Project Manager",
        description = "Coordinates cross-functional teams and ensures timely delivery of project milestones.",
        icon = Icons.Filled.Group
    ),
)

// ─── Careers (frontend: careers-page.tsx) ──────────────────────────────────

data class CareerPosition(val title: String, val department: String, val type: String, val location: String)

val CAREER_POSITIONS = listOf(
    CareerPosition("Flood Risk Data Scientist", "Research", "Full-time", "Mumbai, India"),
    CareerPosition("Frontend Engineer (React)", "Engineering", "Full-time", "Remote"),
    CareerPosition("Geospatial Analyst", "GIS", "Full-time", "Mumbai, India"),
    CareerPosition("ML Ops Engineer", "Engineering", "Contract", "Remote"),
)

// ─── Media (frontend: media-page.tsx) ──────────────────────────────────────

data class PressItem(val title: String, val outlet: String, val date: String)

val PRESS_ITEMS = listOf(
    PressItem(
        title = "XAI-FLOWS Wins Best Smart-City Innovation at IIT Bombay Techfest",
        outlet = "TechCircle India",
        date = "March 2024"
    ),
    PressItem(
        title = "AI-Powered Flood Warning Brings Hope to Mumbai's Flood-Prone Wards",
        outlet = "Hindustan Times",
        date = "August 2023"
    ),
    PressItem(
        title = "From Lab to City: How Explainable AI Is Reinventing Disaster Management",
        outlet = "Analytics India Magazine",
        date = "June 2023"
    ),
)

// ─── Events (frontend: events-page.tsx) ────────────────────────────────────

data class SiteEvent(val title: String, val date: String, val type: String, val location: String, val description: String)

val SITE_EVENTS = listOf(
    SiteEvent(
        title = "XAI-FLOWS Demo Day — Greater Mumbai Municipalities",
        date = "15 May 2026",
        type = "In-person",
        location = "MCGM Headquarters, Mumbai",
        description = "Live demonstration of the real-time flood prediction dashboard for civic engineers."
    ),
    SiteEvent(
        title = "AI in Disaster Management — Panel Discussion",
        date = "2 June 2026",
        type = "Virtual",
        location = "Online (Zoom)",
        description = "XAI-FLOWS team participates in a national panel on AI-assisted emergency response."
    ),
    SiteEvent(
        title = "Monsoon Preparedness Workshop",
        date = "20 June 2026",
        type = "Hybrid",
        location = "IIT Bombay & Online",
        description = "Practical workshop for NDRF personnel on interpreting flood-risk outputs."
    ),
)

// ─── Webinars (frontend: webinars-page.tsx) ────────────────────────────────

data class Webinar(
    val title: String,
    val date: String,
    val duration: String,
    /** true once the recording is ready; false = not yet held. */
    val available: Boolean,
    /** Full URL to the recording, opened via IntentUtils.openUrl. Null when
     *  not yet uploaded — mirrors the frontend's "Coming Soon" placeholder. */
    val recordingUrl: String?
)

val WEBINARS = listOf(
    Webinar(
        title = "Introduction to XAI-FLOWS: Architecture & Use Cases",
        date = "30 April 2026",
        duration = "60 min",
        available = true,
        recordingUrl = null // TODO: replace with actual recording URL when uploaded (matches frontend's own TODO)
    ),
    Webinar(
        title = "SHAP-Based Explainability in Flood Prediction Models",
        date = "18 May 2026",
        duration = "45 min",
        available = false,
        recordingUrl = null
    ),
    Webinar(
        title = "Integrating Real-Time Camera Feeds with AI Pipelines",
        date = "8 June 2026",
        duration = "50 min",
        available = false,
        recordingUrl = null
    ),
)

// ─── FAQ (frontend: faq-page.tsx) ──────────────────────────────────────────

data class FaqItem(val question: String, val answer: String)

/** [COMPANY_INFO].emailSupport is substituted at screen-render time for the
 *  last entry, mirroring the frontend's template-string interpolation —
 *  see FaqScreen.kt. */
val FAQ_ITEMS = listOf(
    FaqItem(
        question = "How accurate is the flood prediction model?",
        answer = "Our current model achieves approximately 87% accuracy on validation data, with an F1 score of 0.80 and an AUC-ROC of 0.90."
    ),
    FaqItem(
        question = "What camera systems does XAI-FLOWS support?",
        answer = "XAI-FLOWS is designed to work with standard CCTV drainage cameras deployed across Mumbai. It accepts JPEG, PNG, and WebP image formats."
    ),
    FaqItem(
        question = "How often does the real-time dashboard update?",
        answer = "The real-time mode polls the backend every 20 seconds by default. This interval is configured in AppConfig.Monitoring.INTERVAL_MS."
    ),
    FaqItem(
        question = "What does the SHAP chart represent?",
        answer = "SHAP (SHapley Additive exPlanations) values quantify the contribution of each weather feature (temperature, precipitation, humidity, etc.) to the final flood risk prediction, making the model transparent and explainable."
    ),
    FaqItem(
        question = "Can I use XAI-FLOWS for other Indian cities?",
        answer = "Yes — the system supports any location by entering custom GPS coordinates. However, the pre-loaded drainage locations are currently specific to Greater Mumbai."
    ),
    // "Who do I contact for technical issues?" is appended at render time in
    // FaqScreen.kt so its answer always uses the live COMPANY_INFO.emailSupport
    // value instead of a string frozen at edit time.
)

// ─── Privacy Policy (frontend: privacy-policy-page.tsx) ───────────────────

data class LegalSection(val heading: String, val body: String)

/** Last-updated label shown under the Privacy Policy title — bump this by
 *  hand whenever SITE_PRIVACY_SECTIONS changes, mirrors the frontend's own
 *  hand-maintained "Last updated: April 2026" text. */
const val PRIVACY_POLICY_LAST_UPDATED = "Last updated: April 2026"

val SITE_PRIVACY_SECTIONS = listOf(
    LegalSection(
        "1. Information We Collect",
        "We may collect image data submitted through the prediction interface, device type, IP address, and usage analytics. No personally identifiable information is required to use the platform."
    ),
    LegalSection(
        "2. How We Use Your Information",
        "Submitted images are processed in real time for flood risk prediction and are not stored persistently. Aggregate usage data helps us improve system performance and reliability."
    ),
    LegalSection(
        "3. Data Security",
        "All data is transmitted over HTTPS. Camera feeds and uploaded images are processed transiently in memory and are not written to disk or shared with third parties."
    ),
    LegalSection(
        "4. Local Storage",
        "The app stores your session's refresh token in Android's encrypted storage (EncryptedSharedPreferences) to keep you signed in. No tracking or advertising identifiers are stored."
    ),
    LegalSection(
        "5. Third-Party Services",
        "The platform may integrate with public weather APIs, map tile providers, and Cloudflare Turnstile for bot protection. Please refer to their respective privacy policies for information on how they handle data."
    ),
    LegalSection(
        "6. Your Rights",
        "You may request deletion of any data associated with your account by contacting us at the email address below."
    ),
    // "7. Contact" is appended at render time in PrivacyPolicyScreen.kt so it
    // always uses the live COMPANY_INFO.email value.
)

// ─── Terms of Service (frontend: terms-of-service-page.tsx) ───────────────

const val TERMS_OF_SERVICE_LAST_UPDATED = "Last updated: April 2026"

val SITE_TERMS_SECTIONS = listOf(
    LegalSection(
        "1. Acceptance of Terms",
        "By accessing and using the XAI-FLOWS platform (\"Service\"), you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the Service."
    ),
    LegalSection(
        "2. Description of Service",
        "XAI-FLOWS provides an AI-powered flood warning and prediction system for the Greater Mumbai region. The Service is intended for informational and emergency management purposes only."
    ),
    LegalSection(
        "3. Acceptable Use",
        "You agree not to misuse the Service, attempt to reverse-engineer the prediction model, upload malicious content, or use the Service in any way that violates applicable law."
    ),
    LegalSection(
        "4. Disclaimer of Warranties",
        "The Service is provided 'as is' without warranty of any kind. Flood predictions are probabilistic estimates and should not be used as the sole basis for life-safety decisions. Always follow guidance from local emergency authorities."
    ),
    LegalSection(
        "5. Account & Security",
        "You are responsible for keeping your login credentials confidential. Notify us immediately at the support email below if you suspect unauthorised access to your account."
    ),
    LegalSection(
        "6. Intellectual Property",
        "All content, models, and software are the intellectual property of XAI-FLOWS Flood Warning System unless otherwise stated. You may not reproduce or distribute any part of the Service without written permission."
    ),
    LegalSection(
        "7. Changes to Terms",
        "We reserve the right to modify these Terms at any time. Continued use of the Service after changes constitutes acceptance of the revised Terms."
    ),
    LegalSection(
        "8. Governing Law",
        "These Terms shall be governed by and construed in accordance with the laws of India. Any disputes shall be subject to the exclusive jurisdiction of courts in Mumbai, Maharashtra."
    ),
    // "9. Contact" is appended at render time in TermsOfServiceScreen.kt so
    // it always uses the live COMPANY_INFO.email value.
)

/**
 * AppRoute.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Single source of truth for every in-app route path. Every path here
 * mirrors the frontend's real router paths exactly (see
 * streamlit-frontend/src/app/router.tsx and src/config/app.config.ts) so
 * the two apps agree on one contract, the same "mirrors X field-for-field"
 * discipline used throughout the auth feature (see AppConfig.Auth's doc
 * comment for the same rationale applied there).
 *
 * WHY THIS FILE EXISTS: before it did, FooterData.kt's hrefs and
 * MainActivity's handleNavigate whitelist were two independently
 * hand-maintained lists with zero compile-time link between them. They
 * had drifted apart — FooterData.kt linked to made-up paths like "/about",
 * "/careers", "/docs", "/cookies" that don't exist anywhere on the
 * frontend, and MainActivity only recognised "/", "/predictions",
 * "/analytics" as in-app — so nearly every footer link fell through to
 * FooterSectionComponent's "open the website" branch and opened a browser
 * to a 404. Routing both sides through this one enum makes that class of
 * bug structurally impossible: there is exactly one place a route's path
 * is spelled out, and Kotlin's exhaustive `when` over an enum means a
 * screen can't be wired into the footer without also being wired into
 * MainActivity's router (or vice versa) without a compile error.
 *
 * Usage:
 *   FooterData.kt      : FooterLink("Our Team", AppRoute.TEAM.path, ...)
 *   MainActivity.kt     : AppRoute.fromPath(route)?.let { page.value = it }
 *                          and `when (page.value) { AppRoute.TEAM -> TeamScreen(...) ... }`
 */
package com.example.xai_flows.core.navigation

enum class AppRoute(val path: String) {
    // ─── Core app screens ───────────────────────────────────────────────────
    HOME("/"),
    PREDICTIONS("/predictions"),
    ANALYTICS("/analytics"),

    // ─── Company (mirrors frontend FOOTER_NAV_GROUPS "Company") ────────────
    TEAM("/company/team"),
    CAREERS("/company/careers"),
    MEDIA("/company/media"),

    // ─── Resources (mirrors frontend FOOTER_NAV_GROUPS "Resources") ────────
    EVENTS("/resources/events"),
    WEBINARS("/resources/webinars"),
    FAQ("/resources/faq"),

    // ─── Support (mirrors frontend FOOTER_NAV_GROUPS "Support") ────────────
    SUPPORT("/support"),
    PRIVACY_POLICY("/other/privacy-policy"),
    TERMS_OF_SERVICE("/other/terms-of-service");

    companion object {
        /**
         * Resolves a route path string (e.g. from a FooterLink.href) back to
         * its [AppRoute]. Returns null for anything unrecognised — callers
         * should treat that as "stay on the current screen", never crash or
         * fall back to opening a browser (there is no longer a legitimate
         * reason for a footer link to leave the app).
         */
        fun fromPath(path: String): AppRoute? = entries.find { it.path == path }
    }
}

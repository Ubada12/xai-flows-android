/**
 * IntentUtils.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Convenience functions for launching common Android intents from Compose.
 * All functions silently no-op if no app can handle the intent (e.g., no
 * browser installed), so callers don't need try/catch.
 *
 * Usage:
 *   IntentUtils.openUrl(context, "https://example.com")
 *   IntentUtils.openEmail(context, "hello@example.com")
 *   IntentUtils.openPhone(context, "+919876543210")
 *   IntentUtils.openMaps(context, "Mumbai, Maharashtra, India")
 */
package com.example.xai_flows.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object IntentUtils {

    /**
     * Opens [url] in the device's default browser.
     * Prepends "https://" if the URL has no scheme.
     */
    fun openUrl(context: Context, url: String) {
        val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) url
                      else "https://$url"
        launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
    }

    /**
     * Opens the mail app with [email] pre-filled in the To: field.
     */
    fun openEmail(context: Context, email: String) {
        launchIntent(
            context,
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
            }
        )
    }

    /**
     * Opens the dialer app with [phone] pre-filled (does not auto-call).
     */
    fun openPhone(context: Context, phone: String) {
        // Strip spaces/dashes for a clean tel: URI
        val digits = phone.filter { it.isDigit() || it == '+' }
        launchIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
    }

    /**
     * Opens Google Maps (or any maps app) searching for [address].
     */
    fun openMaps(context: Context, address: String) {
        val encoded = Uri.encode(address)
        launchIntent(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
        )
    }

    // ─── Internal helper ──────────────────────────────────────────────────────

    private fun launchIntent(context: Context, intent: Intent) {
        // Guard: no-op if nothing can handle the intent
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}

/**
 * TurnstileWebView.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Renders Cloudflare's Turnstile CAPTCHA widget inside a small embedded
 * WebView. There is no native Android Turnstile SDK, so this is the
 * standard workaround — the same pattern most apps use for this exact
 * problem, matching the backend's requirement on /auth/register and
 * /auth/login (see backend/app/api/v1/endpoints/auth.py) that the web
 * frontend already satisfies with a visible widget.
 *
 * Config-driven, nothing hardcoded: the site key and the widget page URL
 * both come from AppConfig.Turnstile (itself sourced from local.properties
 * via BuildConfig — see AppConfig.kt). The site key is appended as a
 * ?sitekey=... query param on WIDGET_URL; the app-level watchdog timeout
 * below is also an AppConfig.Turnstile constant.
 *
 * IMPORTANT — this loads a REAL page over the network, not a bundled
 * asset (see AppConfig.Turnstile.WIDGET_URL's doc comment for the full
 * "why" — short version: Cloudflare's mobile-implementation docs require
 * a real hostname you control, not a synthetic local one, or challenges
 * fail with error 110200 "domain not authorized"). That hostname must be
 * in the Turnstile widget's allowed-hostnames list in the Cloudflare
 * dashboard, alongside a real (non-dummy) site key, before this ships to
 * real users.
 *
 * Also follows Cloudflare's documented WebView setup requirements
 * (developers.cloudflare.com/turnstile/get-started/mobile-implementation/):
 * cookies (incl. third-party, since the challenge iframe is served from
 * challenges.cloudflare.com, a different origin than WIDGET_URL) must be
 * accepted or the challenge can fail to complete.
 *
 * @param onToken   called with a fresh challenge token on success. Pass it
 *                  straight into RegisterRequest.turnstile_token /
 *                  LoginRequest.turnstile_token.
 * @param onError   called with a short error code on failure — either a
 *                  Cloudflare error code, "timeout" from this composable's
 *                  own watchdog, or "load_failed" if the widget page
 *                  itself couldn't be fetched (see below).
 * @param onExpired called when a previously-issued token expired before
 *                  being used. Treat the same as an error and let the user
 *                  retry — remount this composable (e.g. wrap the call
 *                  site in `key(resetCounter) { TurnstileWebView(...) }`
 *                  and bump resetCounter) to force a fresh challenge,
 *                  since the widget itself does not auto-renew silently
 *                  once it has expired.
 */
package org.ubada.xaiflows.ui.auth

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.ubada.xaiflows.core.config.AppConfig
import kotlinx.coroutines.delay
import java.net.URLEncoder

/**
 * JavascriptInterface callbacks fire on a WebView-internal thread, never
 * the main thread — this bridge hops back onto [mainHandler] before
 * invoking the (Compose-state-backed) callbacks, so LoginScreen/SignupScreen
 * can safely update Compose state or launch coroutines directly from them
 * without their own thread-safety plumbing. [markResolved] runs in that
 * same posted block so the watchdog timeout below never fires after a
 * real outcome has already arrived.
 *
 * Deliberately NOT `private`: WebView.addJavascriptInterface() binds this
 * object into the page's JS context via reflection, which requires the
 * class itself to be public — Android's own docs are explicit that a
 * non-public class here silently fails to expose any @JavascriptInterface
 * methods. Kept out of the public API surface simply by not being exported
 * anywhere outside this file, which is all the encapsulation it needs.
 */
class TurnstileJsBridge(
    private val mainHandler: Handler,
    private val onTokenState: State<(String) -> Unit>,
    private val onErrorState: State<(String) -> Unit>,
    private val onExpiredState: State<() -> Unit>,
    private val markResolved: () -> Unit
) {
    @JavascriptInterface
    fun onToken(token: String) {
        mainHandler.post { markResolved(); onTokenState.value(token) }
    }

    @JavascriptInterface
    fun onError(code: String) {
        mainHandler.post { markResolved(); onErrorState.value(code) }
    }

    @JavascriptInterface
    fun onExpired() {
        mainHandler.post { markResolved(); onExpiredState.value() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TurnstileWebView(
    modifier: Modifier = Modifier,
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
    onExpired: () -> Unit
) {
    var resolved by remember { mutableStateOf(false) }

    // rememberUpdatedState so the WebView (created once by AndroidView's
    // factory) always invokes the *current* lambdas even after the
    // composable recomposes with new ones — avoids a long-lived Android
    // View capturing a stale closure.
    val currentOnToken = rememberUpdatedState(onToken)
    val currentOnError = rememberUpdatedState(onError)
    val currentOnExpired = rememberUpdatedState(onExpired)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val bridge = remember {
        TurnstileJsBridge(
            mainHandler = mainHandler,
            onTokenState = currentOnToken,
            onErrorState = currentOnError,
            onExpiredState = currentOnExpired,
            markResolved = { resolved = true }
        )
    }

    fun markResolvedAndReport(code: String) {
        if (!resolved) {
            resolved = true
            currentOnError.value(code)
        }
    }

    // App-level watchdog: covers the widget never calling back at all
    // (fails to load over a poor connection, script blocked, etc.) —
    // Cloudflare's own timeout-callback only fires for an *interactive
    // challenge* timing out, not a widget that never renders in the first
    // place. AppConfig.Turnstile.CHALLENGE_TIMEOUT_MS is the single
    // config-driven source for this, never a magic number inline here.
    LaunchedEffect(Unit) {
        delay(AppConfig.Turnstile.CHALLENGE_TIMEOUT_MS)
        markResolvedAndReport("timeout")
    }

    AndroidView(
        modifier = modifier.height(80.dp),
        factory = { ctx ->
            // Cloudflare's mobile-implementation docs require cookies —
            // including third-party ones, since the actual challenge
            // iframe is served from challenges.cloudflare.com, a
            // different origin than WIDGET_URL — to be explicitly
            // accepted, or the widget can silently fail to verify.
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)

            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    // Surfaces a failure to even fetch WIDGET_URL (offline,
                    // DNS failure, 404 from a misconfigured deployment,
                    // etc.) immediately instead of making the user wait out
                    // the full CHALLENGE_TIMEOUT_MS watchdog for something
                    // that was never going to succeed.
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            markResolvedAndReport("load_failed")
                        }
                    }
                }
                addJavascriptInterface(bridge, "AndroidTurnstile")

                // Site key travels as a query param rather than being
                // baked into the hosted page — AppConfig.Turnstile.SITE_KEY
                // (BuildConfig-driven) stays the single source of truth for
                // which key is active, exactly like the local-asset
                // approach this replaced did via {{SITE_KEY}} substitution.
                val encodedSiteKey = URLEncoder.encode(AppConfig.Turnstile.SITE_KEY, "UTF-8")
                loadUrl("${AppConfig.Turnstile.WIDGET_URL}?sitekey=$encodedSiteKey")
            }
        }
    )
}

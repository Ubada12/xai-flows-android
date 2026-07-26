<h1 align="center">XAI-FLOWS MOBILE (ANDROID)</h1>

<p align="center">
Native Android Client for Real-Time, Explainable Flood-Risk Monitoring
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white">
  <img src="https://img.shields.io/badge/minSdk-23-3DDC84?logo=android&logoColor=white">
  <img src="https://img.shields.io/badge/compileSdk-35-3DDC84?logo=android&logoColor=white">
</p>

---

Native Android client for XAI-FLOWS — a real-time, explainable flood-risk monitoring system for Mumbai's drainage network. Runs the same prediction pipeline as the web frontend, plus a background monitoring service the browser can't offer.

**Important** — *this app is a **client only** — it has no ML models, no database, and does nothing without the backend. See **[XAI-FLOWS Backend](../streamlit-backend)** for the FastAPI service, model pipeline, and API reference this app talks to.*

---

# Quick Start

```bash
git clone <this-repo>
cd XAIFLOWS
cp local.properties.example local.properties   # see Environment Variables below
```

Open the project root in **Android Studio** (Koala or newer), let Gradle sync, then Run ▶ on a device or emulator running **API 23+**.

No `local.properties` values are strictly required to build — both are optional and fall back to working defaults (see below) — but a real Turnstile site key is needed for login/signup to actually pass CAPTCHA verification.

---

# Overview

The mobile app mirrors the web frontend's core loop — submit a drain image, get back a flood-risk verdict with a SHAP explanation — plus one thing the browser can't do: a **persistent background monitoring service** that keeps checking even when the app isn't open.

It's a single-activity Compose app. Navigation is a plain state variable, not a back stack — except for the three-screen auth flow (Signup → Verify OTP → Login), which genuinely needs one.

The architecture mirrors the frontend field-for-field — same auth flow, same request/response shapes, same config-driven approach — so the two clients never drift into disagreeing about how the backend works.

---

# What It Solves

A browser tab can't run reliably in the background, can't hold a "silent, always-on" monitoring loop across app switches, and can't push a device-level alert. This app exists specifically for the field/monitoring use case the web dashboard isn't built for: leave it running, get a notification the moment a drain camera shows high flood risk.

---

# Features

* **Live monitoring mode** — a foreground service polls the latest S3 drain image every 20 seconds, runs it through the full prediction pipeline, and pushes a high-priority alert notification when risk is High or Moderate.
* **Manual mode** — pick an image from the gallery, enter coordinates, get an on-demand prediction.
* **SHAP explainability chart** — per-feature contribution breakdown for every prediction, same values the backend computes for the frontend.
* **Full account system** — register → email OTP verification → login → silent session restore on app relaunch → logout, matching the web app's flow.
* **Encrypted session storage** — the refresh-token cookie is persisted via `EncryptedSharedPreferences` (AES-256), never in plain storage.
* **9 native site screens** — Team, Careers, Media, Events, Webinars, FAQ, Support, Privacy Policy, Terms of Service — reached from the footer, content ported 1:1 from the frontend.
* **Analytics screen** — model performance metrics, confusion matrix, ROC/precision-recall curves, SHAP feature importance, all as native Compose UI.
* **Response caching** — the last prediction and image are cached locally, so the app shows something useful immediately on relaunch instead of a blank screen.

---

# Architecture

```text
┌─────────────────────────────┐
│   Android App (this repo)   │
│                              │
│  Compose UI  →  ViewModel    │
│       ↓             ↓        │
│  AuthRepository  FloodRepository
│       ↓             ↓        │
│         Retrofit + OkHttp    │
│  (AuthInterceptor, TokenAuthenticator,
│   AuthCookieJar → EncryptedSharedPreferences)
└──────────────┬───────────────┘
               │ HTTPS / JSON + multipart
               ▼
┌─────────────────────────────┐
│   XAI-FLOWS Backend          │
│   (separate repo — FastAPI,  │
│   VGG16, XGBoost, SHAP)      │
└─────────────────────────────┘
```

Full backend pipeline (VGG16 blockage classification → Weatherbit weather → XGBoost + SHAP → heuristic verdict): **[backend README](../streamlit-backend)**.

---

# How It Works

This section walks through what actually happens, in order, for the four things this app does: start up, authenticate, monitor in real time, and predict manually. It's written at the level of "which function calls which," not just a feature list — that's the point of it.

### App Launch & Session Bootstrap

`MainActivity.onCreate()` runs in a fixed order before anything else happens:

▸ `installSplashScreen()` fires first.
▸ `CacheManager.init(this)` and `TokenStore.init(this)` run next — everything else that touches them assumes this already happened.
▸ Only after that does `setContent { XAIFLOWSTheme { MainScreen() } }` mount the actual Compose tree.
▸ Only after *that* does `PermissionManager.validateAllPermissions()` run (location + notifications), which can redirect out to system Settings if anything's missing.

A `LaunchedEffect(Unit)` in `MainScreen()` then calls `AuthRepository.restoreSession()` — the mobile equivalent of the frontend's `bootstrapSession()`. It silently calls `POST /auth/refresh` using the stored cookie. While it's in flight, a spinner shows instead of the login gate, so a genuinely logged-in user never sees a flash of "please log in" on reopen.

`restoreSession()`'s two failure branches are deliberately different:

* **`HttpException`** (401/403/404 — the server said no) → the session really is over: clears the session, the persisted cookie, and the cache, so a handed-off device never leaks the previous user's data.
* **Any other exception** (no connectivity, timeout) → only the in-memory session clears. The cookie and cache stay, since the session might still be valid once connectivity returns.

### Auth Flow

```text
Register (name, email, password, Turnstile token)
    → backend emails a 6-digit OTP
    → Verify OTP (activates the account, issues no token)
    → Login (email, password, Turnstile token)
    → access token returned in the response body
    → refresh token arrives as a Set-Cookie header, captured by AuthCookieJar
    → AuthSession.setAccessToken(...) — every screen observing AuthSession.state updates immediately
```

`AuthNavHost` is a three-route `NavHost` (Login, Signup, Verify OTP) shown only when a gated action needs it while logged out.

▸ Each screen has its own `viewModel()` — no shared `AuthViewModel` across all three.
▸ Only the Signup email needs to cross a screen boundary, and it travels as a nav argument, so it survives process death.
▸ After OTP verification, the flow returns to Login instead of auto-logging in — a real login call still mints the token.

`AuthErrorParser` converts failed calls into readable messages — a line-for-line Kotlin port of the frontend's error parser, so both clients show identical wording for the same failure.

### Token Lifecycle In Depth

The access token and refresh token are treated completely differently, and understanding why explains most of the auth code:

* **Access token — memory only.** Lives in `AuthSession`'s `StateFlow`, never written to disk. Process death means a silent re-login on next launch — the same tradeoff the frontend makes with a JS variable instead of `localStorage`.
* **Refresh token — persisted, but encrypted.** `AuthCookieJar` captures the `Set-Cookie` header OkHttp would otherwise ignore; `TokenStore` saves it via `EncryptedSharedPreferences` (AES-256, Keystore-backed). It never appears as a plain field on any model class.
* **Two refresh paths, one function.** `AuthInterceptor` refreshes proactively (just before the token expires); `TokenAuthenticator` refreshes reactively (on a `401`). Both call the same `TokenRefresher.refreshAccessTokenBlocking(...)`, so neither path can drift out of sync.
* **No duplicate refreshes.** Concurrent 401s are serialized behind a lock — only the first request actually calls `/auth/refresh`; the rest just retry with the new token.
* **`JwtUtils` decodes, never verifies.** It reads claims like the email for display only, and fails safely (`null`) on bad input. Only the backend can trust a token's signature.

### Real-Time Monitoring — Full Service Lifecycle

Tapping "Start Real-time Monitoring" does two things: a connectivity + `POST_NOTIFICATIONS` permission guard runs first (requesting the permission inline if it's missing on Android 13+), then `ContextCompat.startForegroundService(...)` launches `RealTimeMonitoringService` with the current lat/lon as Intent extras.

The service's lifecycle, in order:

1. **`onCreate()`** — registers three notification channels up front: `Monitoring` (ongoing status), `Monitoring Errors`, and `Flood Alert` (urgent — vibration pattern, alarm sound, full-screen intent capable).
2. **`onStartCommand()`** — reads the lat/lon extras (falling back to `AppConfig.Monitoring.DEFAULT_LATITUDE/LONGITUDE`, which are Mumbai coordinates, not the old default-to-London bug this replaced), calls `startForeground(...)` immediately with a "Starting…" notification, launches `monitorLoop()` on a `SupervisorJob() + Dispatchers.IO` scope, and returns **`START_STICKY`** — meaning if Android kills the process under memory pressure, the OS restarts the service (with a `null` Intent) rather than letting monitoring silently stop.
3. **`monitorLoop()`** — a `while (isActive)` loop that, each cycle:
   ◦ checks internet connectivity via `NetworkCapabilities` (not the deprecated `activeNetworkInfo`);
   ◦ checks notification permission;
   ◦ fetches the latest S3 image and submits it for prediction;
   ◦ caches the result and broadcasts both results back to the UI;
   ◦ posts a flood-alert notification — optionally as a full-screen intent for the highest severity level — only if `flood_risk` is in `AppConfig.Notifications.ALERT_RISK_LEVELS` (High/Moderate).

   Either connectivity or permission failing stops the loop cleanly (`stopSelf()`) with an explanatory notification, rather than looping forever against a guaranteed failure.
4. **`onDestroy()`** — cancels the `serviceScope`, which cancels the in-flight loop coroutine.

The service and the UI never call each other directly — they communicate through `sendBroadcast`/`BroadcastReceiver` (`RealTimeMonitoringContract`). `PredictionScreen` registers its receiver in a `DisposableEffect(Unit)` and unregisters in `onDispose`, so it only listens while actually on screen.

### Manual Mode

Same underlying call as Real-Time mode (`FloodViewModel.predictFlood(...)`), just triggered by a button press with a gallery-picked image instead of the service's 20-second S3 loop. Switching modes resets prediction state and clears the cache — Live and Manual are treated as separate sessions, not two views onto the same data.

### Navigation Model

A single `AppRoute` enum is the only place any in-app route path is spelled out — the router and every footer link both resolve through `AppRoute.fromPath(...)`, so the two can't drift apart without a compile error. This replaced a real bug: the footer's hrefs and the router's old hand-maintained whitelist used to be two separately maintained lists that had silently gone out of sync, so several footer links pointed at made-up paths and fell through to a browser 404.

---

# Key Design Decisions

### ▶ Two refresh paths, one implementation

See "Token Lifecycle In Depth" above — `AuthInterceptor` (proactive) and `TokenAuthenticator` (reactive) both call the same `TokenRefresher.refreshAccessTokenBlocking`, and concurrent 401s are coalesced behind a lock rather than each firing their own refresh call.

### ▶ No native browser cookie jar, so the app builds its own

`AuthCookieJar` is a minimal single-cookie `CookieJar` that captures the `refresh_token` `Set-Cookie` header OkHttp would otherwise discard, and persists it via `TokenStore` (`EncryptedSharedPreferences`) so it survives process death — the mobile equivalent of what a browser does automatically for the frontend.

### ▶ Access token never touches disk

It lives only in `AuthSession`'s in-memory `StateFlow`, exactly like the frontend keeps it in a JS variable rather than `localStorage`.

### ▶ Turnstile has no native Android SDK

`TurnstileWebView` embeds Cloudflare's real widget page and bridges its JS callback into Compose via `@JavascriptInterface`. It loads an actual hosted page, not a bundled asset — Cloudflare requires a real, allow-listed hostname for challenges to pass. The callback fires on a WebView-internal thread, so `TurnstileJsBridge` hops back onto the main thread before touching Compose state.

### ▶ Config-driven secrets

`TURNSTILE_SITE_KEY`/`TURNSTILE_WIDGET_URL` come from a git-ignored `local.properties` → `BuildConfig` field, never hardcoded — the same single-source-of-truth approach `AppConfig.kt` applies to every other tunable.

### ▶ AppRoute as the single routing source of truth

See "Navigation Model" above — this was a real, fixed bug, not a hypothetical one.

### ▶ The broadcast receiver's lifetime is tied to composition, not to the service

Registered in `DisposableEffect(Unit)`, unregistered in `onDispose` — the service keeps running in the background either way, but nothing listens for its broadcasts unless `PredictionScreen` is actually on screen.

### ▶ Logout and a failed silent-refresh both fully clear local state

Not just the in-memory session — `AuthSession`, `TokenStore`, and `CacheManager` all clear together, so a shared or handed-off device never surfaces the previous user's cached prediction or drain image.

---

# Project Structure

```text
app/src/main/java/org/ubada/xaiflows/
├── MainActivity.kt              # Single-activity entry point, top-level Compose scaffold
├── core/
│   ├── auth/                    # AuthSession (in-memory), AuthRepository (orchestration), TokenStore (encrypted persistence)
│   ├── config/AppConfig.kt      # Single source of truth for every tunable constant
│   ├── data/
│   │   ├── api/                 # ApiClient, ApiService, AuthApiService, AuthCookieJar, AuthInterceptor, TokenAuthenticator, TokenRefresher
│   │   ├── models/               # Retrofit request/response data classes, field-for-field with the backend's pydantic models
│   │   └── repository/           # FloodRepository — thin wrapper over ApiService
│   ├── cache/CacheManager.kt    # SharedPreferences-backed response cache
│   ├── navigation/AppRoute.kt   # Single source of truth for every route path
│   ├── permissions/              # Runtime permission handling (location, notifications)
│   ├── service/                  # RealTimeMonitoringService — the 20s foreground monitoring loop
│   └── utils/                    # JwtUtils (decode-only), AuthErrorParser, IntentUtils
└── ui/
    ├── auth/                     # AuthNavHost + Login/Signup/VerifyOtp screens + TurnstileWebView
    ├── components/                # analytics/, common/, footer/, home/, prediction/, site/
    ├── models/                    # UI-layer data classes + static content (FooterData, SiteContent)
    ├── screens/                   # HomePage, PredictionScreen, AnalyticsScreen, site/ (9 screens)
    ├── theme/                     # Compose theme, colors, type
    └── viewmodel/FloodViewModel.kt
```

---

# Tech Stack

| Layer | Choice | Why |
|---|---|---|
| UI | Jetpack Compose, Material 3 | Declarative UI matching the frontend's component-driven mental model, without maintaining a parallel XML layout system. |
| Navigation | Navigation Compose 2.7.7 (auth flow only) + a single in-memory `AppRoute` enum (main app) | The main app's navigation is flat and doesn't need a real back stack; the auth flow does (Signup → Verify OTP → Login), so it's the one place a real `NavHost` is worth the overhead. |
| Networking | Retrofit 2.9.0 + Gson, OkHttp (custom `CookieJar`/`Interceptor`/`Authenticator`) | Retrofit's suspend-function support fits Compose's coroutine-first world directly; OkHttp's interceptor/authenticator hooks are what make the shared proactive+reactive refresh design possible at all. |
| Images | Coil | Compose-native image loading, avoids threading the request lifecycle through Activities/Fragments by hand. |
| Video | Media3 ExoPlayer | Looping landing-page video on the home screen. |
| Encrypted storage | `androidx.security:security-crypto` | AES-256 `EncryptedSharedPreferences`, backed by Android Keystore — the reason `minSdk` is 23, not a lower default. |
| CAPTCHA | Cloudflare Turnstile via embedded WebView | No native Android SDK exists for Turnstile; the WebView + `@JavascriptInterface` bridge is the standard workaround, matching what the backend already requires from every client. |
| Background work | Android foreground `Service` (`dataSync` type) | The only Android primitive that can reliably keep a polling loop alive while the app isn't in the foreground. |
| Misc UI | Accompanist (flow layout, system UI controller) | Fills two small gaps Compose's stable APIs didn't cover at the time this was built (wrapping layouts, status-bar tinting). |

---

# Setup

Requirements: Android Studio (Koala+), JDK 11, an Android device/emulator on **API 23+**.

1. Clone and open the project root in Android Studio.
2. Copy `local.properties.example` → `local.properties` (git-ignored — never commit this file) and fill in a real Turnstile site key if you need working login/signup.
3. Confirm `AppConfig.Network.BASE_URL` in `core/config/AppConfig.kt` points at your backend instance.
4. Run ▶.

**Important** — *the backend must be reachable from the device/emulator (its own CORS/host config isn't relevant here since this is a native client, but the URL must be resolvable — use **`10.0.2.2`** instead of `localhost` when targeting a local backend from the Android emulator).*

---

# Environment Variables (`local.properties`)

| Key | Required | Purpose |
|---|---|---|
| `TURNSTILE_SITE_KEY` | Optional — falls back to an empty string | Cloudflare Turnstile public site key for the login/signup CAPTCHA widget. Login/register will fail CAPTCHA verification without a real one. |
| `TURNSTILE_WIDGET_URL` | Optional — falls back to the frontend's deployed `mobile-turnstile.html` | Real hosted page the Turnstile WebView loads (Cloudflare requires a real, dashboard-allow-listed hostname — a bundled local asset doesn't satisfy this). |

Neither key is committed anywhere — `local.properties` is git-ignored and both flow into the app only via generated `BuildConfig` fields.

---

# Runtime Permissions

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | All API calls; connectivity check before starting monitoring |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | Required for `RealTimeMonitoringService` to run reliably in the background |
| `POST_NOTIFICATIONS` (Android 13+) | Flood-alert and monitoring-status notifications |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` | Coordinates for weather lookup during monitoring; requested with a rationale prompt and the app exits gracefully if permanently denied |

---

# Build & Distribution

Standard Gradle build — no custom CI/signing pipeline exists yet:

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release build (unsigned — no signing config committed)
```

There is no Play Store listing or signed release pipeline at this stage — this is a research build, distributed manually. Signing, Play Console readiness, and CI are planned for a later phase — see the project's documentation roadmap.

---

# Known Limitations

**Warning** — *a few real, unresolved items — flagging rather than silently living with them.*

### ➤ No signing config or CI pipeline

Release builds are unsigned; see Build & Distribution above.

### ➤ No automated tests beyond the default Android Studio scaffolding

Only `ExampleUnitTest` and `ExampleInstrumentedTest` exist at this stage.

### ➤ FloodViewModel is not shared with RealTimeMonitoringService

The service runs its own `FloodRepository` instance and talks to the UI purely through broadcasts, rather than a shared, observable state holder. This works because there's only ever one `PredictionScreen` listening, but it does mean prediction state technically exists in two places (the service's local variables mid-loop, and the ViewModel's `StateFlow` once a broadcast lands) rather than one.

### ➤ The in-memory access token means every cold start pays a network round trip

`restoreSession()` runs before gated screens are usable, even for an already-logged-in user — a deliberate tradeoff for keeping the access token off disk, not an oversight, but worth knowing if that round trip is ever a perceived launch-latency issue.

---

# Part of XAI-FLOWS

This is one of four repos in the XAI-FLOWS PhD research project:

* **[Backend](../streamlit-backend)** — FastAPI service, VGG16 + XGBoost + SHAP pipeline
* **[Frontend](../streamlit-frontend)** — React/Vite web dashboard
* **Mobile (this repo)** — Android/Kotlin client
* Hub repo — architecture overview and cross-repo documentation (in progress)

---

# License

**All rights reserved.** This is a PhD research project — the source is public for review and academic purposes only, not licensed for reuse, modification, or redistribution without permission.
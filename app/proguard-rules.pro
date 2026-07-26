# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# TurnstileWebView's JS bridge (ui/auth/TurnstileWebView.kt) is only ever
# invoked via WebView reflection from JavaScript, never from any Kotlin/Java
# call site R8 can trace — without this it would be a live candidate for
# removal/renaming the moment isMinifyEnabled is ever flipped to true,
# silently breaking the Cloudflare Turnstile CAPTCHA in release builds.
-keepclassmembers class org.ubada.xaiflows.ui.auth.TurnstileJsBridge {
    public *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
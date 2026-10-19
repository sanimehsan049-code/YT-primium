package com.example.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.data.ShieldsPreferences
import java.io.File

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class BraveTubeWebView(
    context: Context,
    val preferences: ShieldsPreferences,
    private val onFullscreenRequested: (View?, WebChromeClient.CustomViewCallback?) -> Unit,
    private val onFullscreenExitRequested: () -> Unit,
    private val onProgressUpdate: (Int) -> Unit,
    private val onHistoryUpdated: (Boolean) -> Unit,
    private val onPlaybackUpdated: (isPlaying: Boolean, title: String, channel: String) -> Unit,
    private val onSettingsClick: () -> Unit
) : WebView(context) {

    companion object {
        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.200 Mobile Safari/537.36"
        // Desktop Chrome user agent used specifically for accounts.google.com to allow sign-in without "Couldn't sign you in" block
        const val GOOGLE_AUTH_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    var isCurrentlyPlaying: Boolean = false
        private set
    var currentVideoTitle: String = ""
        private set
    var currentChannel: String = ""
        private set

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        cleanCorruptedCache()
        setupSettings()
        setupClients()
        addJavascriptInterface(BraveTubeBridge(), "BraveTubeBridge")
    }

    private fun cleanCorruptedCache() {
        try {
            val baseCache = context.cacheDir ?: return
            val badCodeCache = File(baseCache, "WebView/Default/HTTP Cache/Code Cache")
            if (badCodeCache.exists()) {
                badCodeCache.deleteRecursively()
            }
            val httpCache = File(baseCache, "WebView/Default/HTTP Cache")
            val indexFile = File(httpCache, "index-dir/the-real-index")
            if (httpCache.exists() && !indexFile.exists()) {
                httpCache.deleteRecursively()
            }
        } catch (_: Throwable) {
        }
    }

    private fun setupSettings() {
        val s = settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportMultipleWindows(false)

        // D-pad / Android TV remote focus support
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()

        // Strip X-Requested-With header to prevent Google from identifying embedded WebView
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(s, emptySet())
            }
        } catch (_: Throwable) {
        }

        // Configure CookieManager to allow YouTube sign-in and session cookies
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@BraveTubeWebView, true)
        }

        applyUserAgent(preferences.state.value.desktopMode)
    }

    fun applyUserAgent(isDesktop: Boolean) {
        settings.userAgentString = if (isDesktop) DESKTOP_UA else MOBILE_UA
    }

    private fun setupClients() {
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // Seamless Google Sign-In Fix:
                // When navigating to Google Accounts, switch user agent to Desktop Chrome.
                // Google allows desktop Chrome login freely without "Couldn't sign you in" WebView block.
                if (url.contains("accounts.google.com") || url.contains("accounts.youtube.com")) {
                    if (settings.userAgentString != GOOGLE_AUTH_UA) {
                        settings.userAgentString = GOOGLE_AUTH_UA
                        view?.loadUrl(url)
                        return true
                    }
                } else if (url.contains("youtube.com") && !url.contains("accounts.google.com")) {
                    val targetUa = if (preferences.state.value.desktopMode) DESKTOP_UA else MOBILE_UA
                    if (settings.userAgentString == GOOGLE_AUTH_UA && !preferences.state.value.desktopMode) {
                        settings.userAgentString = targetUa
                    }
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val uri = request?.url ?: return super.shouldInterceptRequest(view, request)

                if (preferences.state.value.shieldsEnabled && BraveShieldsFilter.isBlocked(uri)) {
                    val path = uri.path?.lowercase() ?: ""
                    if (path.contains("stats") || path.contains("tracking") || path.contains("analytics")) {
                        preferences.recordTrackerBlocked()
                    } else {
                        preferences.recordAdBlocked()
                    }
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        204,
                        "No Content",
                        mapOf("Access-Control-Allow-Origin" to "*"),
                        BraveShieldsFilter.EMPTY_INPUT_STREAM
                    )
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Don't inject shields on Google Sign-In pages
                if (url?.contains("accounts.google.com") != true && url?.contains("myaccount.google.com") != true) {
                    injectShieldsScripts()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()

                if (url?.contains("accounts.google.com") != true && url?.contains("myaccount.google.com") != true) {
                    injectShieldsScripts()
                }

                onHistoryUpdated(canGoBack())

                // When returning to YouTube after successful sign-in, restore standard user agent
                if (url?.contains("youtube.com") == true && !url.contains("accounts.google.com")) {
                    if (settings.userAgentString == GOOGLE_AUTH_UA && !preferences.state.value.desktopMode) {
                        settings.userAgentString = MOBILE_UA
                    }
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                onHistoryUpdated(canGoBack())
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: android.webkit.RenderProcessGoneDetail?
            ): Boolean {
                mainHandler.post {
                    try {
                        view?.destroy()
                    } catch (_: Throwable) {}
                }
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressUpdate(newProgress)
                onHistoryUpdated(canGoBack())
                if (newProgress > 60) {
                    val currentUrl = url ?: ""
                    if (!currentUrl.contains("accounts.google.com")) {
                        injectShieldsScripts()
                    }
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customViewCallback = callback
                onFullscreenRequested(view, callback)
            }

            override fun onHideCustomView() {
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                onFullscreenExitRequested()
            }
        }
    }

    fun injectShieldsScripts() {
        val currentUrl = url ?: ""
        if (currentUrl.contains("accounts.google.com") || currentUrl.contains("myaccount.google.com")) {
            return
        }
        if (!preferences.state.value.shieldsEnabled) return
        evaluateJavascript(BraveShieldsFilter.SHIELDS_INJECTION_SCRIPT, null)
    }

    fun playVideo() {
        evaluateJavascript("var v=document.querySelector('video'); if(v) v.play();", null)
    }

    fun pauseVideo() {
        evaluateJavascript("var v=document.querySelector('video'); if(v) v.pause();", null)
    }

    fun togglePlayPause() {
        evaluateJavascript(
            "var v=document.querySelector('video'); if(v) { if(v.paused) v.play(); else v.pause(); }",
            null
        )
    }

    fun seekRelative(seconds: Int) {
        evaluateJavascript(
            "var v=document.querySelector('video'); if(v) { v.currentTime = Math.max(0, v.currentTime + ($seconds)); }",
            null
        )
    }

    fun navigateTo(url: String) {
        loadUrl(url)
    }

    inner class BraveTubeBridge {
        @JavascriptInterface
        fun onPlaybackStateChanged(
            isPlaying: Boolean,
            title: String,
            channel: String,
            duration: Double,
            currentTime: Double
        ) {
            mainHandler.post {
                isCurrentlyPlaying = isPlaying
                currentVideoTitle = title
                currentChannel = channel
                onPlaybackUpdated(isPlaying, title, channel)
            }
        }

        @JavascriptInterface
        fun onAdSkipped() {
            mainHandler.post {
                preferences.recordAdBlocked()
            }
        }

        @JavascriptInterface
        fun openSettings() {
            mainHandler.post {
                onSettingsClick()
            }
        }
    }
}

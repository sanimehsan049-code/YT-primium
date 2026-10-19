package com.example.web

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.InputStream

object BraveShieldsFilter {

    private val AD_AND_TRACKER_HOSTS = hashSetOf(
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "pubads.g.doubleclick.net",
        "ad.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "adservice.google.com",
        "stats.g.doubleclick.net",
        "static.doubleclick.net",
        "www.google-analytics.com",
        "google-analytics.com",
        "analytics.google.com",
        "googletagservices.com",
        "scorecardresearch.com",
        "ade.googlesyndication.com",
        "adclick.g.doubleclick.net",
        "ad-delivery.net",
        "2mdn.net",
        "invitemedia.com",
        "adm.youtube.com"
    )

    private val BLOCKED_PATH_PATTERNS = listOf(
        "/api/stats/ads",
        "/pagead/",
        "/ptracking",
        "/api/stats/qoe",
        "/api/stats/playback",
        "/api/stats/watchtime",
        "/get_midroll_info",
        "/youtubei/v1/att/"
    )

    fun isBlocked(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        val path = uri.path?.lowercase() ?: ""

        // NEVER block Google authentication, account login, security or captcha domains
        if (host.contains("accounts.google.com") ||
            host.contains("accounts.youtube.com") ||
            host.contains("myaccount.google.com") ||
            host.contains("ssl.gstatic.com") ||
            host.contains("recaptcha") ||
            host.contains("apis.google.com") ||
            host.contains("play.google.com") ||
            host.contains("gstatic.com")
        ) {
            return false
        }

        // Check host match or subdomain
        for (adHost in AD_AND_TRACKER_HOSTS) {
            if (host == adHost || host.endsWith(".$adHost")) {
                return true
            }
        }

        // Check YouTube-specific ad / tracking endpoints
        if (host.contains("youtube.com")) {
            for (blockedPath in BLOCKED_PATH_PATTERNS) {
                if (path.contains(blockedPath)) {
                    return true
                }
            }
            if (uri.getQueryParameter("adformat") != null || uri.getQueryParameter("ad_type") != null) {
                return true
            }
        }

        // Generic ad keywords in host
        if (host.startsWith("ad.") || host.startsWith("ads.") || host.contains("doubleclick") || host.contains("adservice")) {
            return true
        }

        return false
    }

    val EMPTY_INPUT_STREAM: InputStream
        get() = ByteArrayInputStream(ByteArray(0))

    val COSMETIC_FILTERING_CSS: String = """
        /* Hide YouTube ads and sponsored banners across Mobile & Desktop */
        ytd-promoted-video-renderer,
        ytd-display-ad-renderer,
        ytd-banner-promo-renderer,
        ytd-statement-banner-renderer,
        ytd-in-feed-ad-layout-renderer,
        ytm-promoted-sparkles-web-renderer,
        ytm-promoted-sparkles-text-search-web-renderer,
        ytm-companion-ad-renderer,
        ytm-promoted-video-renderer,
        ytm-ad-slot-renderer,
        ytm-statement-banner-renderer,
        ytm-paid-content-overlay-renderer,
        .ad-showing,
        .ad-interrupting,
        .video-ads,
        .ytp-ad-module,
        .ytp-ad-overlay-container,
        .ytp-ad-text,
        .ytp-ad-message-container,
        .ytp-ad-action-interstitial,
        .ytp-ad-progress-list,
        .ytd-action-companion-ad-renderer,
        yt-mealbar-promo-renderer,
        ytm-mealbar-promo-renderer,
        /* Hide intrusive install app nags and promo banners */
        #app-install-banner,
        ytm-upsell-dialog-renderer,
        .upsell-dialog-renderer,
        ytm-pivot-bar-item-renderer[aria-label*="Get the app"],
        ytm-mobile-topbar-renderer [aria-label*="Get the app"],
        button[aria-label*="Get the app"],
        .open-in-app,
        ytm-app-banner-renderer {
            display: none !important;
            visibility: hidden !important;
            height: 0px !important;
            min-height: 0px !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }

        /* Clean up mobile YouTube spacing for app-like presentation */
        body {
            -webkit-user-select: none;
            -webkit-tap-highlight-color: transparent;
        }

        /* Ensure bottom pivot bar allows injected settings tab to fit without clipping */
        ytm-pivot-bar-renderer {
            overflow: visible !important;
        }

        /* TV Remote D-pad focus indicator: provides clear visual feedback on Android TV */
        :focus, :focus-visible, a:focus, button:focus, [role="tab"]:focus, [tabindex]:focus {
            outline: 3.5px solid #ff5500 !important;
            outline-offset: 2px !important;
            box-shadow: 0 0 12px rgba(255, 85, 0, 0.85) !important;
        }
    """.trimIndent()

    val SHIELDS_INJECTION_SCRIPT: String = """
        (function() {
            if (window.__brave_tube_injected__) return;
            window.__brave_tube_injected__ = true;

            // 1. Ensure window.chrome exists so Google login bot-detection passes
            try {
                if (!window.chrome) {
                    window.chrome = {
                        app: { isInstalled: false },
                        runtime: {}
                    };
                }
            } catch(e) {}

            // 2. Inject Cosmetic Ad-blocking Stylesheet
            try {
                var style = document.createElement('style');
                style.id = 'brave-shields-style';
                style.textContent = ${sanitizeForJs(COSMETIC_FILTERING_CSS)};
                (document.head || document.documentElement).appendChild(style);
            } catch(e) {}

            // 3. Prevent YouTube from pausing background playback on visibility change
            try {
                Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                window.addEventListener('visibilitychange', function(e) {
                    e.stopImmediatePropagation();
                }, true);
            } catch(e) {}

            // 4. Automated Ad Skipping & Video Telemetry Loop
            var lastTitle = '';
            var lastChannel = '';
            var lastState = false;

            function checkAndSkipAds() {
                try {
                    // Check if ad is active
                    var adContainer = document.querySelector('.ad-showing, .ad-interrupting, .video-ads, .ytp-ad-player-overlay, ytd-player-legacy-desktop-watch-ads-renderer');
                    var video = document.querySelector('video');

                    if (video) {
                        if (adContainer) {
                            video.muted = true;
                            if (isFinite(video.duration) && video.duration > 0) {
                                video.currentTime = video.duration + 1;
                            }
                            if (window.BraveTubeBridge && window.BraveTubeBridge.onAdSkipped) {
                                window.BraveTubeBridge.onAdSkipped();
                            }
                        }

                        // Click skip buttons
                        var skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .videoAdUiSkipButton, [class*="skip-button"], .ytp-skip-ad-button, button.ytp-ad-skip-button-text');
                        for (var i = 0; i < skipButtons.length; i++) {
                            skipButtons[i].click();
                        }

                        // Auto-dismiss "Are you still watching?" and "Try Premium" popups
                        var dismissBtn = document.querySelector('yt-confirm-dialog-renderer #confirm-button, ytm-upsell-dialog-renderer #dismiss-button');
                        if (dismissBtn) dismissBtn.click();

                        // Report metadata to Native MediaSession
                        var isPlaying = !video.paused && !video.ended && video.readyState > 2;
                        var titleElem = document.querySelector('.slim-video-metadata-title, h1.title, meta[name="title"]');
                        var title = titleElem ? (titleElem.content || titleElem.textContent || document.title) : document.title;
                        title = (title || 'YouTube Video').replace(/ - YouTube$/, '').trim();

                        var channelElem = document.querySelector('.slim-owner-channel-name, .ytd-channel-name');
                        var channel = channelElem ? (channelElem.textContent || '').trim() : 'YouTube';

                        var duration = isFinite(video.duration) ? video.duration : 0;
                        var current = isFinite(video.currentTime) ? video.currentTime : 0;

                        if (window.BraveTubeBridge && (isPlaying !== lastState || title !== lastTitle)) {
                            lastState = isPlaying;
                            lastTitle = title;
                            lastChannel = channel;
                            window.BraveTubeBridge.onPlaybackStateChanged(isPlaying, title, channel, duration, current);
                        }
                    }

                    // 5. Inject into DOM pivot bar with equal flex sizing
                    var pivotBar = document.querySelector('ytm-pivot-bar-renderer, ytm-navigation-bar, .pivot-bar, [role="tablist"]');
                    if (pivotBar && !document.getElementById('brave-settings-gear')) {
                        pivotBar.style.overflow = 'visible';
                        var tabs = pivotBar.querySelectorAll('ytm-pivot-bar-item-renderer, [role="tab"]');
                        for (var t = 0; t < tabs.length; t++) {
                            tabs[t].style.flex = '1';
                            tabs[t].style.minWidth = '0';
                        }

                        var gearBtn = document.createElement('div');
                        gearBtn.id = 'brave-settings-gear';
                        gearBtn.setAttribute('role', 'tab');
                        gearBtn.setAttribute('aria-label', 'Settings');
                        gearBtn.style.cssText = 'display:flex; flex-direction:column; align-items:center; justify-content:center; flex:1; height:48px; min-width:38px; cursor:pointer; user-select:none; -webkit-tap-highlight-color:transparent; padding:2px 0; z-index:9999;';
                        
                        gearBtn.innerHTML = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#ff5500" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg><span style="font-size:9.5px; font-weight:600; color:#ff5500; margin-top:2px; font-family:Roboto, Arial, sans-serif;">Settings</span>';

                        gearBtn.onclick = function(e) {
                            e.preventDefault();
                            e.stopPropagation();
                            if (window.BraveTubeBridge && window.BraveTubeBridge.openSettings) {
                                window.BraveTubeBridge.openSettings();
                            }
                        };

                        pivotBar.appendChild(gearBtn);
                    }
                } catch(err) {
                    console.error('Brave Shields check error:', err);
                }
            }

            setInterval(checkAndSkipAds, 500);

            // Hook HTML5 video elements immediately when added to DOM
            var observer = new MutationObserver(function() {
                checkAndSkipAds();
            });
            observer.observe(document.documentElement || document.body, { childList: true, subtree: true });
        })();
    """.trimIndent()

    private fun sanitizeForJs(str: String): String {
        return "`" + str.replace("`", "\\`") + "`"
    }
}

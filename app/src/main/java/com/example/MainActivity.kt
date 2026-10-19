package com.example

import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.ShieldsPreferences
import com.example.service.BackgroundMediaService
import com.example.ui.components.BraveFloatingControl
import com.example.ui.components.BraveShieldsSheet
import com.example.ui.components.CompatibilityMatrixDialog
import com.example.ui.theme.BraveOrange
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.web.BraveTubeWebView

class MainActivity : ComponentActivity() {

    private lateinit var preferences: ShieldsPreferences
    private var webView: BraveTubeWebView? = null
    private var backgroundService: BackgroundMediaService? = null
    private var isServiceBound = false

    private var customView: View? = null
    private var customViewContainer: FrameLayout? = null

    private var isInPipModeState by mutableStateOf(false)
    private var isFullscreenVideoState by mutableStateOf(false)
    private var isShieldsSheetOpenState by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BackgroundMediaService.LocalBinder
            backgroundService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            backgroundService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = ShieldsPreferences(this)
        BackgroundMediaService.mediaListener = mediaControlListener

        // Bind background media playback service
        val serviceIntent = Intent(this, BackgroundMediaService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            MyApplicationTheme {
                MainScreen(
                    preferences = preferences,
                    isInPipMode = isInPipModeState,
                    isFullscreenVideo = isFullscreenVideoState,
                    onEnterPip = { enterPip() },
                    onCustomViewShow = { view -> showCustomFullscreenView(view) },
                    onCustomViewHide = { hideCustomFullscreenView() }
                )
            }
        }

        handleIntentUrl(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentUrl(intent)
    }

    private fun handleIntentUrl(intent: Intent?) {
        val data = intent?.dataString
        if (!data.isNullOrBlank() && (data.contains("youtube.com") || data.contains("youtu.be"))) {
            webView?.navigateTo(data)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (preferences.state.value.pictureInPictureEnabled && webView?.isCurrentlyPlaying == true) {
            enterPip()
        }
    }

    /**
     * Checks if current runtime is an Android TV / Google TV device
     */
    fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    /**
     * Comprehensive Android TV Remote and Hardware Media Control keys handling
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_INFO,
            KeyEvent.KEYCODE_PROG_RED -> {
                isShieldsSheetOpenState = !isShieldsSheetOpenState
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (!isShieldsSheetOpenState && webView != null && event != null) {
                    webView?.dispatchKeyEvent(event)
                    return true
                }
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                webView?.togglePlayPause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                webView?.playVideo()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                webView?.pauseVideo()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> {
                webView?.seekRelative(10)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> {
                webView?.seekRelative(-10)
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (isFullscreenVideoState) {
                    hideCustomFullscreenView()
                    return true
                }
                if (isShieldsSheetOpenState) {
                    isShieldsSheetOpenState = false
                    return true
                }
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val aspectRatio = Rational(16, 9)
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Fallback if aspect ratio not accepted
                enterPictureInPictureMode()
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
    }

    private fun showCustomFullscreenView(view: View?) {
        if (view == null) return
        customView = view
        isFullscreenVideoState = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        hideSystemBars()
    }

    private fun hideCustomFullscreenView() {
        customView = null
        isFullscreenVideoState = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        showSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private val mediaControlListener = object : BackgroundMediaService.MediaControlListener {
        override fun onMediaPlay() {
            runOnUiThread { webView?.playVideo() }
        }

        override fun onMediaPause() {
            runOnUiThread { webView?.pauseVideo() }
        }

        override fun onMediaSeekRelative(seconds: Int) {
            runOnUiThread { webView?.seekRelative(seconds) }
        }

        override fun onMediaStop() {
            runOnUiThread {
                webView?.pauseVideo()
                backgroundService?.stopForegroundPlayback()
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    fun onPlaybackUpdated(isPlaying: Boolean, title: String, channel: String) {
        if (preferences.state.value.backgroundAudioEnabled) {
            if (isPlaying) {
                val serviceIntent = Intent(this, BackgroundMediaService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
            backgroundService?.updatePlaybackState(isPlaying, title, channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        BackgroundMediaService.mediaListener = null
        webView?.destroy()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(
        preferences: ShieldsPreferences,
        isInPipMode: Boolean,
        isFullscreenVideo: Boolean,
        onEnterPip: () -> Unit,
        onCustomViewShow: (View?) -> Unit,
        onCustomViewHide: () -> Unit
    ) {
        val shieldsState by preferences.state.collectAsState()
        var pageProgress by remember { mutableIntStateOf(0) }
        var canGoBack by remember { mutableStateOf(false) }
        var isCompatibilityDialogOpen by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Request notification permission on Android 13+
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied */ }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Handle Back button
        BackHandler(enabled = isFullscreenVideo || isShieldsSheetOpenState || canGoBack) {
            when {
                isFullscreenVideo -> onCustomViewHide()
                isShieldsSheetOpenState -> isShieldsSheetOpenState = false
                canGoBack -> webView?.goBack()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isFullscreenVideo) Modifier
                        else Modifier.statusBarsPadding()
                    )
                    .background(DarkBackground)
            ) {
                // 1. Full Page Genuine YouTube (100% full immersion, native WebView layer)
                AndroidView(
                    factory = { context ->
                        BraveTubeWebView(
                            context = context,
                            preferences = preferences,
                            onFullscreenRequested = { view, _ ->
                                onCustomViewShow(view)
                            },
                            onFullscreenExitRequested = {
                                onCustomViewHide()
                            },
                            onProgressUpdate = { progress ->
                                pageProgress = progress
                            },
                            onHistoryUpdated = { hasHistory ->
                                canGoBack = hasHistory
                            },
                            onPlaybackUpdated = { playing, title, channel ->
                                this@MainActivity.onPlaybackUpdated(playing, title, channel)
                            },
                            onSettingsClick = {
                                isShieldsSheetOpenState = true
                            }
                        ).also {
                            webView = it
                            it.loadUrl("https://m.youtube.com")
                        }
                    },
                    update = { /* handled via callbacks */ },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("brave_youtube_webview")
                )

                // 2. Ultra-Thin Progress Line at top edge during page load
                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.TopCenter)
                            .zIndex(10f),
                        color = BraveOrange,
                        trackColor = Color.Transparent
                    )
                }

                // 3. Fullscreen Custom View Container (when YouTube enters HTML5 full screen)
                if (isFullscreenVideo && customView != null) {
                    AndroidView(
                        factory = { context ->
                            FrameLayout(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                customViewContainer = this
                                (customView?.parent as? ViewGroup)?.removeView(customView)
                                addView(customView)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground)
                            .zIndex(20f)
                            .testTag("fullscreen_custom_view_container")
                    )
                }

                // 4. Prominent Brave Shields Settings Gear Button (CRITICAL: zIndex 100f and navigationBarsPadding)
                // Guaranteed to be drawn on top of the native WebView layer, inset above system navigation bar
                // Hides automatically during HTML5 fullscreen video or PiP
                if (!isFullscreenVideo && !isInPipMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 16.dp, bottom = 64.dp)
                            .zIndex(100f)
                    ) {
                        BraveFloatingControl(
                            shieldsState = shieldsState,
                            onClick = { isShieldsSheetOpenState = true }
                        )
                    }
                }
            }
        }

        // Brave Shields Control Sheet (controls all Shields, PiP, Desktop, Refresh, Matrix)
        if (isShieldsSheetOpenState) {
            BraveShieldsSheet(
                sheetState = sheetState,
                shieldsState = shieldsState,
                preferences = preferences,
                onDismiss = { isShieldsSheetOpenState = false },
                onOpenCompatibilityMatrix = {
                    isShieldsSheetOpenState = false
                    isCompatibilityDialogOpen = true
                },
                onReloadRequested = {
                    webView?.applyUserAgent(shieldsState.desktopMode)
                    webView?.reload()
                },
                onEnterPip = onEnterPip,
                onToggleDesktop = {
                    val newDesktop = !shieldsState.desktopMode
                    preferences.setDesktopMode(newDesktop)
                    webView?.applyUserAgent(newDesktop)
                    webView?.reload()
                }
            )
        }

        // Cross-Platform Compatibility Dialog
        if (isCompatibilityDialogOpen) {
            CompatibilityMatrixDialog(
                onDismiss = { isCompatibilityDialogOpen = false }
            )
        }
    }
}

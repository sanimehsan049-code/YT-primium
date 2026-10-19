package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShieldsState(
    val shieldsEnabled: Boolean = true,
    val aggressiveAdBlock: Boolean = true,
    val backgroundAudioEnabled: Boolean = true,
    val pictureInPictureEnabled: Boolean = true,
    val blockCookieNags: Boolean = true,
    val desktopMode: Boolean = false,
    val adsBlockedSession: Int = 0,
    val trackersBlockedSession: Int = 0,
    val adsBlockedLifetime: Int = 0,
    val trackersBlockedLifetime: Int = 0
) {
    val totalBlockedSession: Int get() = adsBlockedSession + trackersBlockedSession
    val totalBlockedLifetime: Int get() = adsBlockedLifetime + trackersBlockedLifetime
    // Rough estimate: average video ad + banner tracker savings ~1.8 MB
    val dataSavedMbEstimated: Float get() = (totalBlockedLifetime * 1.8f)
    // Rough estimate: ~15 seconds per video ad saved
    val timeSavedSecondsEstimated: Int get() = (adsBlockedLifetime * 15)
}

class ShieldsPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("brave_shields_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        ShieldsState(
            shieldsEnabled = prefs.getBoolean("shields_enabled", true),
            aggressiveAdBlock = prefs.getBoolean("aggressive_ad_block", true),
            backgroundAudioEnabled = prefs.getBoolean("bg_audio_enabled", true),
            pictureInPictureEnabled = prefs.getBoolean("pip_enabled", true),
            blockCookieNags = prefs.getBoolean("block_cookie_nags", true),
            desktopMode = prefs.getBoolean("desktop_mode", false),
            adsBlockedSession = 0,
            trackersBlockedSession = 0,
            adsBlockedLifetime = prefs.getInt("ads_blocked_lifetime", 42),
            trackersBlockedLifetime = prefs.getInt("trackers_blocked_lifetime", 108)
        )
    )
    val state: StateFlow<ShieldsState> = _state.asStateFlow()

    fun setShieldsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("shields_enabled", enabled).apply()
        _state.value = _state.value.copy(shieldsEnabled = enabled)
    }

    fun setAggressiveAdBlock(enabled: Boolean) {
        prefs.edit().putBoolean("aggressive_ad_block", enabled).apply()
        _state.value = _state.value.copy(aggressiveAdBlock = enabled)
    }

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("bg_audio_enabled", enabled).apply()
        _state.value = _state.value.copy(backgroundAudioEnabled = enabled)
    }

    fun setPictureInPictureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pip_enabled", enabled).apply()
        _state.value = _state.value.copy(pictureInPictureEnabled = enabled)
    }

    fun setBlockCookieNags(enabled: Boolean) {
        prefs.edit().putBoolean("block_cookie_nags", enabled).apply()
        _state.value = _state.value.copy(blockCookieNags = enabled)
    }

    fun setDesktopMode(enabled: Boolean) {
        prefs.edit().putBoolean("desktop_mode", enabled).apply()
        _state.value = _state.value.copy(desktopMode = enabled)
    }

    @Synchronized
    fun recordAdBlocked() {
        val current = _state.value
        val newSession = current.adsBlockedSession + 1
        val newLifetime = current.adsBlockedLifetime + 1
        prefs.edit().putInt("ads_blocked_lifetime", newLifetime).apply()
        _state.value = current.copy(
            adsBlockedSession = newSession,
            adsBlockedLifetime = newLifetime
        )
    }

    @Synchronized
    fun recordTrackerBlocked() {
        val current = _state.value
        val newSession = current.trackersBlockedSession + 1
        val newLifetime = current.trackersBlockedLifetime + 1
        prefs.edit().putInt("trackers_blocked_lifetime", newLifetime).apply()
        _state.value = current.copy(
            trackersBlockedSession = newSession,
            trackersBlockedLifetime = newLifetime
        )
    }

    fun resetStats() {
        prefs.edit()
            .putInt("ads_blocked_lifetime", 0)
            .putInt("trackers_blocked_lifetime", 0)
            .apply()
        _state.value = _state.value.copy(
            adsBlockedSession = 0,
            trackersBlockedSession = 0,
            adsBlockedLifetime = 0,
            trackersBlockedLifetime = 0
        )
    }
}

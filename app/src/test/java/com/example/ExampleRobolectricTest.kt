package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.ShieldsPreferences
import com.example.web.BraveShieldsFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BraveTube", appName)
    }

    @Test
    fun `shields filter blocks ad endpoints`() {
        val adUri = Uri.parse("https://googleads.g.doubleclick.net/pagead/ads?client=ca-video-pub")
        assertTrue(BraveShieldsFilter.isBlocked(adUri))

        val telemetryUri = Uri.parse("https://www.youtube.com/api/stats/ads?adformat=1_2_3")
        assertTrue(BraveShieldsFilter.isBlocked(telemetryUri))
    }

    @Test
    fun `shields filter allows genuine video stream`() {
        val videoStreamUri = Uri.parse("https://rr1---sn-ab5sznzl.googlevideo.com/videoplayback?expire=123")
        assertFalse(BraveShieldsFilter.isBlocked(videoStreamUri))

        val regularPageUri = Uri.parse("https://m.youtube.com/watch?v=dQw4w9WgXcQ")
        assertFalse(BraveShieldsFilter.isBlocked(regularPageUri))
    }

    @Test
    fun `shields filter never blocks google sign in or auth endpoints`() {
        val authUri = Uri.parse("https://accounts.google.com/ServiceLogin?service=youtube")
        assertFalse(BraveShieldsFilter.isBlocked(authUri))

        val gstaticUri = Uri.parse("https://ssl.gstatic.com/accounts/static/css/login.css")
        assertFalse(BraveShieldsFilter.isBlocked(gstaticUri))

        val recaptchaUri = Uri.parse("https://www.google.com/recaptcha/api.js")
        assertFalse(BraveShieldsFilter.isBlocked(recaptchaUri))
    }

    @Test
    fun `shields preferences records blocked stats and toggles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ShieldsPreferences(context)
        prefs.resetStats()

        assertEquals(0, prefs.state.value.adsBlockedSession)
        assertEquals(0, prefs.state.value.totalBlockedSession)

        prefs.recordAdBlocked()
        prefs.recordTrackerBlocked()

        assertEquals(1, prefs.state.value.adsBlockedSession)
        assertEquals(1, prefs.state.value.trackersBlockedSession)
        assertEquals(2, prefs.state.value.totalBlockedSession)

        prefs.setShieldsEnabled(false)
        assertFalse(prefs.state.value.shieldsEnabled)
        prefs.setShieldsEnabled(true)
        assertTrue(prefs.state.value.shieldsEnabled)

        prefs.setDesktopMode(true)
        assertTrue(prefs.state.value.desktopMode)
        prefs.setDesktopMode(false)
        assertFalse(prefs.state.value.desktopMode)
    }
}

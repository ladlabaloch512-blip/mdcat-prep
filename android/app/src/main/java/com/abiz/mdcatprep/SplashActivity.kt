package com.abiz.mdcatprep

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * Splash: themed background (gradient for Classic, wave-tint for Soft,
 * plain themed colour otherwise), the pulse app mark, app name + tagline
 * from the settings endpoint (cached; fallback constants when offline),
 * then routes to Auth (no token) or Main (token / guest).
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        const val FALLBACK_APP_NAME = "MDCAT Prep"
        const val FALLBACK_TAGLINE = "Prepare. Practice. Pass."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        val t = Themes.current()

        // Splash always carries the CLIENT's branding (their reference
        // screen 1): deep green gradient, their caduceus emblem, the
        // app name and tagline, the pulse line and a loading bar.
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(Ui.dp(this, 32f), 0, Ui.dp(this, 32f), 0)
        root.setBackgroundColor(Color.parseColor("#448A47"))

        val mark = android.widget.ImageView(this)
        mark.setImageResource(R.drawable.logo_full)
        mark.layoutParams = LinearLayout.LayoutParams(Ui.dp(this, 216f), Ui.dp(this, 216f))
        root.addView(mark)
        Ui.popIn(mark)

        val settings = Prefs.settings()
        val appName = settings?.optString("app_name", "")?.takeIf { it.isNotEmpty() } ?: FALLBACK_APP_NAME
        val tagline = settings?.optString("tagline", "")?.takeIf { it.isNotEmpty() } ?: FALLBACK_TAGLINE

        // The lockup already carries the MDCAT Pakistan name, so no
        // separate name line (appName is still read for other screens).
        val tag = Ui.txt(this, t, tagline, 15f, Color.parseColor("#FFFFFF"), gravity = Gravity.CENTER)
        tag.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 0))
        root.addView(tag)
        Ui.enter(tag, 1)

        val ecg = EcgLineView(this)
        ecg.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 54f), intArrayOf(0, 30, 0, 0))
        root.addView(ecg)

        // Loading bar: track + fill that grows to full over ~1.3s.
        val track = android.widget.FrameLayout(this)
        val trackBg = GradientDrawable()
        trackBg.cornerRadius = Ui.dp(this, 5f).toFloat()
        trackBg.setColor(0x40FFFFFF)
        track.background = trackBg
        track.layoutParams = Ui.lp(this, Ui.dp(this, 220f), Ui.dp(this, 9f), intArrayOf(0, 26, 0, 0))
        val fill = android.view.View(this)
        val fillBg = GradientDrawable()
        fillBg.cornerRadius = Ui.dp(this, 5f).toFloat()
        fillBg.setColor(Color.parseColor("#FFD94D"))
        fill.background = fillBg
        fill.layoutParams = android.widget.FrameLayout.LayoutParams(0, Ui.MATCH)
        track.addView(fill)
        val trackWrap = LinearLayout(this)
        trackWrap.gravity = Gravity.CENTER
        trackWrap.addView(track)
        root.addView(trackWrap)
        val loading = Ui.txt(this, t, "Loading  0%", 13f, Color.parseColor("#FFFFFF"), bold = true, gravity = Gravity.CENTER)
        loading.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 0))
        root.addView(loading)
        setContentView(root)

        // Gentle pulse on the logo while loading.
        val pulse = android.animation.ObjectAnimator.ofFloat(mark, "scaleX", 1f, 1.045f)
        pulse.duration = 850
        pulse.repeatCount = android.animation.ValueAnimator.INFINITE
        pulse.repeatMode = android.animation.ValueAnimator.REVERSE
        pulse.start()
        val pulseY = android.animation.ObjectAnimator.ofFloat(mark, "scaleY", 1f, 1.045f)
        pulseY.duration = 850
        pulseY.repeatCount = android.animation.ValueAnimator.INFINITE
        pulseY.repeatMode = android.animation.ValueAnimator.REVERSE
        pulseY.start()

        // Loading bar + percentage count up together over ~1.9 seconds,
        // and the splash stays at least that long so the loading is
        // actually seen (previously it could flash past on fast networks).
        val fullWidth = Ui.dp(this, 220f)
        val anim = android.animation.ValueAnimator.ofInt(0, fullWidth)
        anim.duration = 1900
        anim.addUpdateListener { a ->
            val wNow = a.animatedValue as Int
            val lp = fill.layoutParams
            lp.width = wNow
            fill.layoutParams = lp
            loading.text = "Loading  " + (wNow * 100 / fullWidth) + "%"
        }
        anim.start()
        Handler(Looper.getMainLooper()).postDelayed({
            minTimeDone = true
            route()
        }, 1950)

        // Refresh settings in the background; routing waits for both the
        // fetch and the minimum splash time.
        ApiClient.get("settings", token = Prefs.token,
            onOk = { data: JSONObject ->
                Prefs.settingsJson = data.toString()
                settingsDone = true
                route()
            },
            onErr = { settingsDone = true; route() })
        Handler(Looper.getMainLooper()).postDelayed({ route(force = true) }, 4500)
    }

    private var routed = false
    private var settingsDone = false
    private var minTimeDone = false
    private fun route(force: Boolean = false) {
        if (routed || isFinishing) return
        if (!force && !(settingsDone && minTimeDone)) return
        routed = true
        val dest = if (Prefs.token != null || Prefs.guest) MainActivity::class.java else AuthActivity::class.java
        startActivity(Intent(this, dest))
        finish()
    }
}

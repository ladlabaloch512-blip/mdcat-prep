package com.abiz.mdcatprep

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import org.json.JSONObject

/**
 * Analytics tab: the student's own numbers - attempts, accuracy, study
 * streak, coins, per-subject accuracy and the last 7 days of activity.
 * Requires sign-in; guests get a clear sign-in prompt.
 */
class AnalyticsFragment : Fragment() {

    private lateinit var t: ThemeSpec
    private lateinit var container: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        t = (activity as MainActivity).theme
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(t.bg)
        container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f))
        container.addView(Ui.txt(requireContext(), t, "Analytics", 22f, t.text, bold = true))
        container.addView(Ui.txt(requireContext(), t, "Your preparation in numbers", 13f, t.muted))
        scroll.addView(container)
        load()
        return scroll
    }

    private fun load() {
        val ctx = requireContext()
        val loading = Ui.skeleton(ctx, t, 3)
        loading.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 16, 0, 0))
        container.addView(loading)
        if (Prefs.token.isNullOrEmpty()) {
            container.removeView(loading)
            val msg = Ui.txt(ctx, t, "Sign in to see your analytics.\nYour progress, accuracy and streak live on your account.", 14f, t.muted, gravity = Gravity.CENTER)
            msg.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 20, 0, 0))
            container.addView(msg)
            val btn = Ui.txt(ctx, t, "Go to Profile to sign in", 14f, t.primary, bold = true, gravity = Gravity.CENTER)
            btn.setOnClickListener { (activity as MainActivity).selectTab("profile") }
            container.addView(btn)
            return
        }
        ApiClient.get("analytics", token = Prefs.token,
            onCache = { data ->
                if (!isAdded) return@get
                container.removeView(loading)
                render(data, offline = true)
            },
            onOk = { data ->
                if (!isAdded) return@get
                container.removeView(loading)
                render(data, offline = false)
            },
            onErr = { e ->
                if (!isAdded) return@get
                container.removeView(loading)
                val err = Ui.txt(ctx, t, (e.message ?: "Could not load analytics.") + "\nTap to retry.", 14f, t.muted, gravity = Gravity.CENTER)
                err.setOnClickListener { container.removeView(err); load() }
                container.addView(err)
            })
    }

    private fun render(data: JSONObject, offline: Boolean) {
        val ctx = requireContext()
        if (offline) {
            val note = Ui.txt(ctx, t, "Offline mode. Showing your saved data.", 12f, t.muted, gravity = Gravity.CENTER)
            note.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 0))
            container.addView(note)
        }
        val accuracy = data.optInt("accuracy", 0)

        // Accuracy ring card.
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.background = Ui.cardBg(ctx, t)
        card.setPadding(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f))
        card.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 16, 0, 0))
        val ring = RingView(ctx)
        ring.percent = 0f
        ring.ringColor = t.primary
        ring.trackColor = if (t.dark) 0xFF26302A.toInt() else 0xFFE4EAE5.toInt()
        ring.centerText = "$accuracy%"
        ring.centerColor = t.text
        ring.layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 84f), Ui.dp(ctx, 84f))
        card.addView(ring)
        ring.post { Ui.sweepRing(ring, accuracy.toFloat()) }
        val texts = LinearLayout(ctx)
        texts.orientation = LinearLayout.VERTICAL
        texts.setPadding(Ui.dp(ctx, 16f), 0, 0, 0)
        texts.addView(Ui.txt(ctx, t, "Overall accuracy", 16f, t.text, bold = true))
        texts.addView(Ui.txt(ctx, t, "${data.optInt("correct")} correct out of ${data.optInt("answered")} answered", 13f, t.muted))
        card.addView(texts)
        container.addView(card)

        // Stat tiles row.
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 0))
        row.addView(tile("Attempts", data.optInt("attempts").toString()))
        row.addView(tile("Study streak", data.optInt("study_streak_days").toString() + " days"))
        val last = tile("Coins", data.optInt("coins").toString())
        (last.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
        row.addView(last)
        container.addView(row)
        row.post { Ui.enterChildren(row) }

        // Per-subject accuracy.
        val subTitle = Ui.txt(ctx, t, "Accuracy by subject", 16f, t.text, bold = true)
        subTitle.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 20, 0, 8))
        container.addView(subTitle)
        val subjects = data.optJSONArray("subjects").toList().map { Subject.from(it) }
        for (s in subjects) {
            val sc = LinearLayout(ctx)
            sc.orientation = LinearLayout.VERTICAL
            sc.background = Ui.cardBg(ctx, t)
            sc.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
            sc.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 10))
            sc.addView(Ui.txt(ctx, t, "${s.name}  -  ${s.percent}%", 14f, t.text, bold = true))
            sc.addView(Ui.txt(ctx, t, "${s.attempted} questions attempted", 12f, t.muted))
            val bar = Ui.progressBar(ctx, t, s.percent)
            bar.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 8, 0, 0))
            sc.addView(bar)
            container.addView(sc)
        }

        // Last 7 days activity bars.
        val weekTitle = Ui.txt(ctx, t, "Last 7 days", 16f, t.text, bold = true)
        weekTitle.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 8))
        container.addView(weekTitle)
        val days = data.optJSONArray("last_7_days").toList()
        var maxV = 1
        for (d in days) maxV = maxOf(maxV, d.optInt("answered"))
        val chart = LinearLayout(ctx)
        chart.orientation = LinearLayout.HORIZONTAL
        chart.gravity = Gravity.BOTTOM
        chart.background = Ui.cardBg(ctx, t)
        chart.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
        chart.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 150f), intArrayOf(0, 0, 0, 4))
        for (d in days) {
            val col = LinearLayout(ctx)
            col.orientation = LinearLayout.VERTICAL
            col.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            col.layoutParams = LinearLayout.LayoutParams(0, Ui.MATCH, 1f)
            val v = d.optInt("answered")
            col.addView(Ui.txt(ctx, t, v.toString(), 11f, t.muted, gravity = Gravity.CENTER))
            val barV = View(ctx)
            val h = Ui.dp(ctx, 8f) + (Ui.dp(ctx, 76f) * v / maxV)
            barV.layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 22f), h)
            barV.background = Ui.pillBg(t.primary, 4f, ctx)
            col.addView(barV)
            val lab = Ui.txt(ctx, t, d.optString("label", ""), 11f, t.muted, gravity = Gravity.CENTER)
            lab.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 0))
            col.addView(lab)
            chart.addView(col)
            // Bars grow upward into place.
            barV.post {
                barV.pivotY = barV.height.toFloat()
                barV.scaleY = 0f
                barV.animate().scaleY(1f).setDuration(700L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction { barV.scaleY = 1f }
                    .start()
            }
        }
        container.addView(chart)
        Ui.enterChildren(container, 2)
    }

    private fun tile(label: String, value: String): LinearLayout {
        val ctx = requireContext()
        val tile = LinearLayout(ctx)
        tile.orientation = LinearLayout.VERTICAL
        tile.background = Ui.cardBg(ctx, t)
        tile.gravity = Gravity.CENTER
        tile.setPadding(Ui.dp(ctx, 10f), Ui.dp(ctx, 12f), Ui.dp(ctx, 10f), Ui.dp(ctx, 12f))
        val lp = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        lp.marginEnd = Ui.dp(ctx, 8f)
        tile.layoutParams = lp
        tile.addView(Ui.txt(ctx, t, value, 18f, t.primary, bold = true, gravity = Gravity.CENTER))
        tile.addView(Ui.txt(ctx, t, label, 11f, t.muted, gravity = Gravity.CENTER))
        return tile
    }
}

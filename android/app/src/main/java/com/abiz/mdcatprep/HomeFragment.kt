package com.abiz.mdcatprep

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import org.json.JSONObject

/**
 * Home dashboard. Renders the daily progress and the subject list in the
 * active design's dashboard layout: TILES grid, BENTO mixed spans, LIST
 * rows, or CHIP_GRID (stat chips + compact tiles). All content comes from
 * GET home; nothing is hard-coded.
 */
class HomeFragment : Fragment() {

    private lateinit var t: ThemeSpec
    private lateinit var main: MainActivity
    private lateinit var container: LinearLayout
    private lateinit var statusView: View

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        main = activity as MainActivity
        t = main.theme
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(t.bg)
        container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f))
        statusView = Ui.skeleton(requireContext(), t, 3)
        container.addView(statusView)
        scroll.addView(container)
        Ui.pullToRefresh(scroll) { load() }
        load()
        return scroll
    }

    private fun load() {
        ApiClient.get("home", token = Prefs.token,
            onCache = { data -> if (isAdded) render(data, offline = true) },
            onOk = { data -> if (isAdded) render(data) },
            onErr = { e ->
                if (!isAdded) return@get
                container.removeView(statusView)
                val err = Ui.txt(requireContext(), t, (e.message ?: "Could not load the dashboard.") + "\nTap to retry.", 14f, t.muted, gravity = Gravity.CENTER)
                err.setOnClickListener {
                    container.removeView(err)
                    statusView = Ui.skeleton(requireContext(), t, 3)
                    container.addView(statusView)
                    load()
                }
                container.addView(err)
            })
    }

    private fun render(data: JSONObject, offline: Boolean = false) {
        val ctx = requireContext()
        container.removeAllViews()
        if (offline) {
            val note = Ui.txt(ctx, t, "Offline mode. Showing your last saved data.", 12f, t.muted, gravity = Gravity.CENTER)
            note.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 10))
            container.addView(note)
        }
        val user = if (data.isNull("user")) null else data.optJSONObject("user")
        val userName = user?.optString("name", "") ?: ""
        val appName = data.optString("app_name", SplashActivity.FALLBACK_APP_NAME)
        val goal = data.optInt("daily_goal", 50)
        val completed = data.optInt("daily_completed", 0)
        val subjects = data.optJSONArray("subjects").toList().map { Subject.from(it) }
        val bannersTop = data.optJSONArray("banners_dashboard").toList().map { Banner.from(it) }
        val bannersAfter = data.optJSONArray("banners_after_subjects").toList().map { Banner.from(it) }

        // Header row: greeting + notifications bell.
        val header = LinearLayout(ctx)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val greeting = LinearLayout(ctx)
        greeting.orientation = LinearLayout.VERTICAL
        greeting.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        greeting.addView(Ui.txt(ctx, t, if (userName.isNotEmpty()) "Assalam o Alaikum, $userName" else "Assalam o Alaikum", 20f, t.text, bold = true))
        greeting.addView(Ui.txt(ctx, t, appName + " - " + data.optString("tagline", ""), 13f, t.muted))
        val initial = (userName.trim().firstOrNull()?.toString() ?: "G").uppercase()
        val avatar = Ui.txt(ctx, t, initial, 18f, t.onPrimary, bold = true, gravity = Gravity.CENTER)
        avatar.background = Ui.pillBg(t.primary, 23f, ctx)
        avatar.layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 46f), Ui.dp(ctx, 46f)).apply { marginEnd = Ui.dp(ctx, 10f) }
        header.addView(avatar)
        header.addView(greeting)

        // Icon cluster (reference dashboard): notifications bell with an
        // unread badge, WhatsApp channel shortcut, settings gear. The
        // old text pill read as "no bell icon" in the client's review.
        fun roundBtn(): FrameLayout {
            val f = FrameLayout(ctx)
            f.background = Ui.pillBg(t.surface, 20f, ctx)
            f.layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 40f), Ui.dp(ctx, 40f)).apply { marginStart = Ui.dp(ctx, 8f) }
            return f
        }
        fun centerIcon(parent: FrameLayout, iv: View) {
            iv.layoutParams = FrameLayout.LayoutParams(Ui.dp(ctx, 22f), Ui.dp(ctx, 22f), Gravity.CENTER)
            parent.addView(iv)
        }
        val bellBox = roundBtn()
        centerIcon(bellBox, Ui.icon(ctx, R.drawable.ic_bell, 22f, t.text))
        val unread = data.optInt("unread_notifications", 0)
        if (unread > 0 && Prefs.token != null) {
            val badge = Ui.txt(ctx, t, if (unread > 9) "9+" else unread.toString(), 10f, Color.WHITE, bold = true, gravity = Gravity.CENTER)
            badge.background = Ui.pillBg(Color.parseColor("#D32F2F"), 9f, ctx)
            badge.layoutParams = FrameLayout.LayoutParams(Ui.dp(ctx, 18f), Ui.dp(ctx, 18f), Gravity.END or Gravity.TOP)
            bellBox.addView(badge)
        }
        Ui.pressable(bellBox)
        bellBox.setOnClickListener {
            startActivity(Intent(ctx, NotificationsActivity::class.java))
            requireActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
        header.addView(bellBox)
        if (data.optString("whatsapp_channel_url", "").isNotEmpty()) {
            val waBox = roundBtn()
            val waIv = ImageView(ctx)
            waIv.setImageResource(R.drawable.whatsapp_logo)
            centerIcon(waBox, waIv)
            Ui.pressable(waBox)
            waBox.setOnClickListener { startActivity(Intent(ctx, ChannelActivity::class.java)) }
            header.addView(waBox)
        }
        val gearBox = roundBtn()
        centerIcon(gearBox, Ui.icon(ctx, R.drawable.ic_gear, 22f, t.text))
        Ui.pressable(gearBox)
        gearBox.setOnClickListener { startActivity(Intent(ctx, SettingsActivity::class.java)) }
        header.addView(gearBox)
        container.addView(header)

        // Daily progress, rendered per design.
        container.addView(progressCard(completed, goal))

        // Banner slot: dashboard (after progress).
        val stripTop = Components.bannerStrip(ctx, t, bannersTop)
        stripTop.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        container.addView(stripTop)

        // Daily Reward + WhatsApp Channel cards.
        val extrasRow = LinearLayout(ctx)
        extrasRow.orientation = LinearLayout.HORIZONTAL
        extrasRow.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        val coins = data.optInt("coins", 0)
        val claimed = data.optBoolean("reward_claimed_today", false)
        val rewardCard = LinearLayout(ctx)
        rewardCard.orientation = LinearLayout.VERTICAL
        rewardCard.background = Ui.cardBg(ctx, t, fill = t.accent)
        rewardCard.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
        val rlp = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        rewardCard.layoutParams = rlp
        rewardCard.addView(Ui.txt(ctx, t, "DAILY REWARD", 11f, t.onPrimary, bold = true))
        val coinsView = Ui.txt(ctx, t, "$coins coins", 18f, t.onPrimary, bold = true)
        rewardCard.addView(coinsView)
        Ui.countUp(coinsView, coins, { v -> "$v coins" })
        rewardCard.addView(Ui.txt(ctx, t, if (claimed) "Claimed today" else "Tap to claim ${data.optInt("daily_reward_coins", 0)} coins", 12f, t.onPrimary))
        Ui.pressable(rewardCard)
        rewardCard.setOnClickListener {
            startActivity(Intent(ctx, RewardsActivity::class.java))
            requireActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
        extrasRow.addView(rewardCard)
        val channelUrl = data.optString("whatsapp_channel_url", "")
        if (channelUrl.isNotEmpty()) {
            val chCard = LinearLayout(ctx)
            chCard.orientation = LinearLayout.VERTICAL
            chCard.background = Ui.cardBg(ctx, t, fill = 0xFF128C4B.toInt())
            chCard.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
            val clp = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
            clp.marginStart = Ui.dp(ctx, 10f)
            chCard.layoutParams = clp
            chCard.addView(Ui.txt(ctx, t, "WHATSAPP CHANNEL", 11f, 0xFFD9F2E2.toInt(), bold = true))
            chCard.addView(Ui.txt(ctx, t, "Join for daily MCQs", 15f, android.graphics.Color.WHITE, bold = true))
            chCard.addView(Ui.txt(ctx, t, "Tap to open", 12f, 0xFFD9F2E2.toInt()))
            Ui.pressable(chCard)
            chCard.setOnClickListener {
                startActivity(Intent(ctx, ChannelActivity::class.java))
                requireActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
            extrasRow.addView(chCard)
        }
        container.addView(extrasRow)

        // Subjects heading.
        val heading = Ui.txt(ctx, t, if (t.id == "editorial") "SUBJECTS" else "Subjects", 17f, t.text, bold = true)
        heading.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 20, 0, 10))
        container.addView(heading)

        if (subjects.isEmpty()) {
            container.addView(Ui.txt(ctx, t, "No subjects are available yet. Please check again later.", 14f, t.muted))
        } else {
            container.addView(subjectsView(subjects))
        }

        // Banner slot: after subjects.
        val stripAfter = Components.bannerStrip(ctx, t, bannersAfter)
        stripAfter.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        container.addView(stripAfter)

        // Full mock test CTA.
        val mock = Button(ctx)
        mock.text = "Start Full Mock Test"
        mock.setTextColor(t.onPrimary)
        mock.background = Ui.pillBg(t.primary, t.radiusDp, ctx)
        mock.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 54f), intArrayOf(0, 20, 0, 8))
        mock.setOnClickListener { main.startMock() }
        container.addView(mock)
        Ui.enterChildren(container)
    }

    private fun progressCard(completed: Int, goal: Int): View {
        val ctx = requireContext()
        val pct = if (goal > 0) (completed * 100 / goal).coerceIn(0, 100) else 0
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(Ui.dp(ctx, 18f), Ui.dp(ctx, 18f), Ui.dp(ctx, 18f), Ui.dp(ctx, 18f))
        card.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 16, 0, 0))
        when (t.header) {
            HeaderStyle.BLACK_TILE -> card.background = Ui.cardBg(ctx, t, fill = Color.parseColor("#141414"))
            HeaderStyle.GRADIENT_BAND -> card.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#0A8A4D"), Color.parseColor("#0B5ED7")))
            else -> card.background = Ui.cardBg(ctx, t)
        }
        val onCard = if (t.header == HeaderStyle.BLACK_TILE || t.header == HeaderStyle.GRADIENT_BAND || t.dark) Color.WHITE else t.text
        val mutedOnCard = if (onCard == Color.WHITE) 0xFFCFE9DA.toInt() else t.muted

        when (t.id) {
            "focus", "editorial" -> {
                val row = LinearLayout(ctx); row.orientation = LinearLayout.HORIZONTAL; row.gravity = Gravity.BOTTOM
                val big = Ui.txt(ctx, t, completed.toString(), 44f, onCard, bold = true)
                row.addView(big)
                Ui.countUp(big, completed)
                val of = Ui.txt(ctx, t, " / $goal today", 16f, mutedOnCard)
                of.setPadding(0, 0, 0, Ui.dp(ctx, 8f)); row.addView(of)
                card.addView(row)
                card.addView(Ui.txt(ctx, t, "Daily progress", 13f, mutedOnCard))
                val bar = Ui.progressBar(ctx, t, pct); bar.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 8f), intArrayOf(0, 10, 0, 0)); card.addView(bar)
            }
            "soft" -> {
                card.addView(Ui.txt(ctx, t, "Daily progress", 15f, onCard, bold = true, gravity = Gravity.CENTER))
                val gauge = GaugeView(ctx)
                gauge.percent = 0f; gauge.gaugeColor = t.primary; gauge.trackColor = 0xFFF3DFE8.toInt()
                gauge.centerText = "$completed / $goal"; gauge.centerColor = onCard
                gauge.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 8, 0, 0))
                card.addView(gauge)
                gauge.post { Ui.sweepGauge(gauge, pct.toFloat()) }
            }
            "teal" -> {
                card.addView(Ui.txt(ctx, t, "Daily progress", 15f, onCard, bold = true))
                val nums = Ui.txt(ctx, t, "$completed of $goal questions today", 13f, mutedOnCard)
                nums.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 4, 0, 8)); card.addView(nums)
                card.addView(Ui.segmentedBar(ctx, t, pct))
            }
            "classic", "darkfocus" -> {
                val row = LinearLayout(ctx); row.orientation = LinearLayout.HORIZONTAL; row.gravity = Gravity.CENTER_VERTICAL
                val ring = RingView(ctx)
                ring.percent = 0f; ring.ringColor = if (t.dark) t.primary else Color.WHITE
                ring.trackColor = if (t.dark) 0xFF26302A.toInt() else 0x55FFFFFF
                ring.centerText = "$pct%"; ring.centerColor = onCard
                ring.layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 84f), Ui.dp(ctx, 84f))
                row.addView(ring)
                ring.post { Ui.sweepRing(ring, pct.toFloat()) }
                val texts = LinearLayout(ctx); texts.orientation = LinearLayout.VERTICAL
                texts.setPadding(Ui.dp(ctx, 16f), 0, 0, 0)
                texts.addView(Ui.txt(ctx, t, "Daily progress", 16f, onCard, bold = true))
                texts.addView(Ui.txt(ctx, t, "$completed of $goal questions done today", 13f, mutedOnCard))
                row.addView(texts)
                card.addView(row)
            }
            else -> {
                card.addView(Ui.txt(ctx, t, "Daily progress", 16f, onCard, bold = true))
                val nums = Ui.txt(ctx, t, "$completed / $goal questions today  -  $pct%", 13f, mutedOnCard)
                nums.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 4, 0, 8)); card.addView(nums)
                card.addView(Ui.progressBar(ctx, t, pct, fill = if (onCard == Color.WHITE) Color.WHITE else t.primary))
            }
        }
        return card
    }

    private fun subjectsView(subjects: List<Subject>): View {
        val ctx = requireContext()
        return when (t.dashboard) {
            DashboardLayout.LIST -> {
                val col = LinearLayout(ctx); col.orientation = LinearLayout.VERTICAL
                subjects.forEachIndexed { i, s -> col.addView(subjectRow(s, i)) }
                col
            }
            DashboardLayout.CHIP_GRID -> {
                val col = LinearLayout(ctx); col.orientation = LinearLayout.VERTICAL
                val grid = GridLayout(ctx); grid.columnCount = 2
                subjects.forEachIndexed { i, s -> grid.addView(subjectTile(s, i, half = true)) }
                col.addView(grid)
                col
            }
            DashboardLayout.BENTO -> {
                val grid = GridLayout(ctx); grid.columnCount = 2
                subjects.forEachIndexed { i, s -> grid.addView(subjectTile(s, i, half = true)) }
                grid
            }
            DashboardLayout.TILES -> {
                val grid = GridLayout(ctx); grid.columnCount = 2
                subjects.forEachIndexed { i, s -> grid.addView(subjectTile(s, i, half = true)) }
                grid
            }
        }
    }

    private fun subjectTile(s: Subject, index: Int, half: Boolean): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f))
        val tint = if (t.id == "poster" && index % 2 == 0) t.primary else Ui.subjectColor(s.color, t, index).let {
            if (t.id == "bento" || t.id == "classic") t.tileTints[index % t.tileTints.size] else it
        }
        val fill = when (t.id) {
            "poster" -> if (index % 2 == 0) t.primary else Color.WHITE
            "bento", "classic" -> tint
            else -> t.surface
        }
        card.background = Ui.cardBg(ctx, t, fill = fill)
        val textOn = if (t.id == "poster" && index % 2 == 0) Color.WHITE else t.text
        val mutedOn = if (t.id == "poster" && index % 2 == 0) 0xFFD8F3E3.toInt() else t.muted
        val lp = GridLayout.LayoutParams()
        lp.width = 0; lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        lp.setMargins(Ui.dp(ctx, 4f), Ui.dp(ctx, 4f), Ui.dp(ctx, 4f), Ui.dp(ctx, 4f))
        card.layoutParams = lp
        val indexLabel = if (t.id == "editorial") String.format("%02d  ", index + 1) else ""
        card.addView(Ui.txt(ctx, t, indexLabel + s.name, 16f, textOn, bold = true))
        if (s.tagline.isNotEmpty()) card.addView(Ui.txt(ctx, t, s.tagline, 12f, mutedOn))
        card.addView(Ui.txt(ctx, t, "${s.chapterCount} chapters - ${s.questionCount} MCQs", 12f, mutedOn))
        val bar = Ui.progressBar(ctx, t, s.percent, 6f, fill = if (t.id == "poster" && index % 2 == 0) Color.WHITE else t.primary)
        bar.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 6f), intArrayOf(0, 10, 0, 0))
        card.addView(bar)
        card.addView(Ui.txt(ctx, t, "${s.percent}% complete", 11f, mutedOn))
        Ui.pressable(card)
        card.setOnClickListener { main.openChapters(s) }
        return card
    }

    private fun subjectRow(s: Subject, index: Int): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(Ui.dp(ctx, 4f), Ui.dp(ctx, 14f), Ui.dp(ctx, 4f), Ui.dp(ctx, 14f))
        if (t.shape == CardShape.HAIRLINE) {
            row.background = Ui.cardBg(ctx, t, fill = t.bg)
        }
        row.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 6))
        val top = LinearLayout(ctx); top.orientation = LinearLayout.HORIZONTAL
        val name = Ui.txt(ctx, t, (if (t.id == "editorial") String.format("%02d   ", index + 1) else "") + s.name, 17f, t.text, bold = true)
        name.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        top.addView(name)
        top.addView(Ui.txt(ctx, t, "${s.percent}%", 15f, t.primary, bold = true))
        row.addView(top)
        row.addView(Ui.txt(ctx, t, "${s.chapterCount} chapters - ${s.questionCount} MCQs", 12f, t.muted))
        val bar = Ui.progressBar(ctx, t, s.percent, 5f)
        bar.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 5f), intArrayOf(0, 8, 0, 0))
        row.addView(bar)
        Ui.pressable(row)
        row.setOnClickListener { main.openChapters(s) }
        return row
    }
}

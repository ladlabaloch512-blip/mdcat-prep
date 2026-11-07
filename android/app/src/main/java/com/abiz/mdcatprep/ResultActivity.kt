package com.abiz.mdcatprep

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * Result screen. Score presentation follows the active design's result
 * style (ring, big number + tiles, segment bar, gauge, poster giant,
 * split panel, skew band, stat grid). Shows correct / incorrect / skipped,
 * a per-question performance strip, the result banner slot, Review
 * Mistakes with correct answers + explanations + save toggles, and Retake.
 *
 * Score display rule from the API contract: mock results are shown as
 * score / out_of (200 when full content is loaded); chapter quizzes as
 * score / total.
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var t: ThemeSpec

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        t = Themes.current()
        val data = QuizSession.lastResult
        if (data == null) {
            finish()
            return
        }
        val quizType = data.optString("quiz_type", QuizSession.lastQuizType)
        val score = data.optInt("score")
        val total = data.optInt("total")
        val outOf = data.optInt("out_of", total)
        val displayTotal = if (quizType == "mock") outOf else total
        val correct = data.optInt("correct")
        val incorrect = data.optInt("incorrect")
        val skipped = data.optInt("skipped")
        val percent = data.optInt("percent")

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(t.bg)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 24f))
        scroll.addView(root)
        setContentView(root)

        root.addView(Ui.txt(this, t, if (quizType == "mock") "Mock Test Result" else "Quiz Result", 22f, t.text, bold = true))

        // Score presentation per design.
        root.addView(scoreView(score, displayTotal, percent, correct, incorrect, skipped))

        // Banner slot: result, below the score area, never inside the quiz.
        val banners = data.optJSONArray("banners").toList().map { Banner.from(it) }
        val strip = Components.bannerStrip(this, t, banners)
        strip.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        root.addView(strip)

        // Per-question performance strip (custom-drawn row of blocks).
        val review = data.optJSONArray("review").toList()
        val stripTitle = Ui.txt(this, t, "Performance by question", 15f, t.text, bold = true)
        stripTitle.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 20, 0, 8))
        root.addView(stripTitle)
        val stripRow = LinearLayout(this)
        stripRow.orientation = LinearLayout.HORIZONTAL
        for (r in review) {
            val cell = android.view.View(this)
            val lp = LinearLayout.LayoutParams(0, Ui.dp(this, 14f), 1f)
            lp.marginEnd = Ui.dp(this, 2f)
            cell.layoutParams = lp
            val sel = r.optString("selected_option", "")
            cell.background = Ui.pillBg(
                when {
                    sel.isEmpty() -> if (t.dark) 0xFF3A453E.toInt() else 0xFFD8D8D8.toInt()
                    r.optBoolean("is_correct") -> t.primary
                    else -> 0xFFC0392B.toInt()
                }, 3f, this)
            stripRow.addView(cell)
        }
        root.addView(stripRow)
        val legend = Ui.txt(this, t, "Green = correct, red = wrong, grey = skipped", 11f, t.muted)
        legend.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 0))
        root.addView(legend)

        // Performance by subject (reference screen 6 chart), when the
        // quiz spans more than one subject.
        val bySubject = LinkedHashMap<String, IntArray>()
        for (rev in review) {
            val s = rev.optString("subject_title", "").ifEmpty { "General" }
            val a = bySubject.getOrPut(s) { intArrayOf(0, 0) }
            a[1]++
            if (rev.optBoolean("is_correct")) a[0]++
        }
        if (bySubject.size > 1) {
            val chartTitle = Ui.txt(this, t, "Performance by Subject", 17f, t.text, bold = true)
            chartTitle.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 22, 0, 8))
            root.addView(chartTitle)
            val chart = LinearLayout(this)
            chart.orientation = LinearLayout.VERTICAL
            chart.background = Ui.cardBg(this, t)
            chart.setPadding(Ui.dp(this, 16f), Ui.dp(this, 14f), Ui.dp(this, 16f), Ui.dp(this, 6f))
            for ((subject, a) in bySubject) {
                val pct = if (a[1] > 0) a[0] * 100 / a[1] else 0
                chart.addView(Ui.txt(this, t, "$subject: ${a[0]}/${a[1]} correct ($pct%)", 13f, t.text, bold = true))
                val bar = Ui.progressBar(this, t, pct, 8f)
                bar.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 8f), intArrayOf(0, 4, 0, 12))
                chart.addView(bar)
            }
            root.addView(chart)
        }

        // Retake + Home.
        val retake = Button(this)
        retake.text = "Retake Quiz"
        retake.setTextColor(t.onPrimary)
        retake.background = Ui.pillBg(t.primary, t.radiusDp, this)
        retake.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 20, 0, 0))
        retake.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("type", quizType)
            if (quizType != "mock") intent.putExtra("chapter_id", QuizSession.lastChapterId)
            startActivity(intent)
            finish()
        }
        root.addView(retake)
        val nextMock = Button(this)
        nextMock.text = "Next Mock Test"
        nextMock.setTextColor(t.onPrimary)
        nextMock.background = Ui.pillBg(t.accent, t.radiusDp, this)
        nextMock.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 12, 0, 0))
        nextMock.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("type", "mock")
            startActivity(intent)
            finish()
        }
        root.addView(nextMock)
        val home = Ui.txt(this, t, "Back to Home", 14f, t.primary, bold = true, gravity = Gravity.CENTER)
        home.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        home.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish()
        }
        root.addView(home)

        // Review Mistakes.
        val mistakes = review.filter { !it.optBoolean("is_correct") }
        val reviewTitle = Ui.txt(this, t, "Review Mistakes (${mistakes.size})", 17f, t.text, bold = true)
        reviewTitle.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 24, 0, 8))
        root.addView(reviewTitle)
        if (mistakes.isEmpty()) {
            root.addView(Ui.txt(this, t, "No mistakes. Every answered question was correct.", 14f, t.muted))
        } else {
            for (m in mistakes) root.addView(reviewCard(m))
        }

        // Celebration: sections cascade in; a high score gets confetti.
        Ui.enterChildren(root)
        if (percent >= 50) root.postDelayed({ celebrate() }, 420L)
    }

    /** Lightweight confetti burst over the result screen. */
    private fun celebrate() {
        val decor = window.decorView as android.view.ViewGroup
        val overlay = android.widget.FrameLayout(this)
        decor.addView(overlay, android.view.ViewGroup.LayoutParams(Ui.MATCH, Ui.MATCH))
        val colors = intArrayOf(t.primary, t.accent, 0xFFF5B301.toInt(), 0xFF0B5ED7.toInt(), 0xFFE56B9F.toInt())
        val rnd = java.util.Random()
        val w = resources.displayMetrics.widthPixels
        val h = resources.displayMetrics.heightPixels
        for (i in 0 until 42) {
            val p = android.view.View(this)
            val size = Ui.dp(this, 6f + rnd.nextInt(8))
            p.layoutParams = android.widget.FrameLayout.LayoutParams(size, size + Ui.dp(this, 4f))
            p.background = Ui.pillBg(colors[i % colors.size], 2f, this)
            p.x = rnd.nextInt(w).toFloat()
            p.y = -Ui.dp(this, 30f).toFloat()
            overlay.addView(p)
            val drift = (rnd.nextInt(121) - 60).toFloat()
            p.animate().translationY(h + Ui.dp(this, 60f).toFloat())
                .translationX(drift)
                .rotation((rnd.nextInt(541) - 270).toFloat())
                .setDuration(1500L + rnd.nextInt(900))
                .setStartDelay((rnd.nextInt(6) * 70).toLong())
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { overlay.removeView(p) }
                .start()
        }
        overlay.postDelayed({ decor.removeView(overlay) }, 3200L)
    }

    private fun statTile(label: String, value: String, color: Int = t.text): LinearLayout {
        val tile = LinearLayout(this)
        tile.orientation = LinearLayout.VERTICAL
        tile.background = Ui.cardBg(this, t)
        tile.setPadding(Ui.dp(this, 12f), Ui.dp(this, 12f), Ui.dp(this, 12f), Ui.dp(this, 12f))
        tile.gravity = Gravity.CENTER
        val lp = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        lp.marginEnd = Ui.dp(this, 8f)
        tile.layoutParams = lp
        tile.addView(Ui.txt(this, t, value, 20f, color, bold = true, gravity = Gravity.CENTER))
        tile.addView(Ui.txt(this, t, label, 12f, t.muted, gravity = Gravity.CENTER))
        return tile
    }

    private fun statsRow(correct: Int, incorrect: Int, skipped: Int): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        row.addView(statTile("Correct", correct.toString(), t.primary))
        row.addView(statTile("Wrong", incorrect.toString(), 0xFFC0392B.toInt()))
        val last = statTile("Skipped", skipped.toString())
        (last.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
        row.addView(last)
        row.post { Ui.enterChildren(row) }
        return row
    }

    private fun scoreView(score: Int, displayTotal: Int, percent: Int, correct: Int, incorrect: Int, skipped: Int): android.view.View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        when (t.result) {
            ResultStyle.RING -> {
                val card = LinearLayout(this); card.orientation = LinearLayout.VERTICAL; card.gravity = Gravity.CENTER
                card.background = Ui.cardBg(this, t)
                card.setPadding(Ui.dp(this, 18f), Ui.dp(this, 22f), Ui.dp(this, 18f), Ui.dp(this, 18f))
                val ring = RingView(this)
                ring.percent = 0f; ring.ringColor = t.primary
                ring.trackColor = if (t.dark) 0xFF26302A.toInt() else 0xFFE4EAE5.toInt()
                ring.centerText = "$percent%"; ring.centerColor = t.text
                ring.strokeDp = 12f
                ring.layoutParams = LinearLayout.LayoutParams(Ui.dp(this, 140f), Ui.dp(this, 140f))
                card.addView(ring)
                val s = Ui.txt(this, t, "Score: $score / $displayTotal", 16f, t.text, bold = true, gravity = Gravity.CENTER)
                s.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 0)); card.addView(s)
                ring.post { Ui.sweepRing(ring, percent.toFloat()) }
                Ui.countUp(s, score, { v -> "Score: $v / $displayTotal" }, 1100L)
                wrap.addView(card)
                wrap.addView(statsRow(correct, incorrect, skipped))
            }
            ResultStyle.GAUGE -> {
                val card = LinearLayout(this); card.orientation = LinearLayout.VERTICAL
                card.background = Ui.cardBg(this, t)
                card.setPadding(Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f))
                val gauge = GaugeView(this)
                gauge.percent = 0f; gauge.gaugeColor = t.primary
                gauge.trackColor = 0xFFF3DFE8.toInt(); gauge.centerText = "$score / $displayTotal"; gauge.centerColor = t.text
                card.addView(gauge)
                gauge.post { Ui.sweepGauge(gauge, percent.toFloat()) }
                wrap.addView(card)
                wrap.addView(statsRow(correct, incorrect, skipped))
            }
            ResultStyle.POSTER_GIANT -> {
                val block = LinearLayout(this); block.orientation = LinearLayout.VERTICAL; block.gravity = Gravity.CENTER
                block.background = Ui.pillBg(t.primary, 0f, this)
                block.setPadding(Ui.dp(this, 16f), Ui.dp(this, 24f), Ui.dp(this, 16f), Ui.dp(this, 24f))
                val bigScore = Ui.txt(this, t, "$score", 72f, Color.WHITE, bold = true, gravity = Gravity.CENTER)
                block.addView(bigScore)
                Ui.countUp(bigScore, score, durationMs = 1200L)
                block.addView(Ui.txt(this, t, "out of $displayTotal  -  $percent%", 16f, 0xFFD8F3E3.toInt(), bold = true, gravity = Gravity.CENTER))
                wrap.addView(block)
                wrap.addView(statsRow(correct, incorrect, skipped))
            }
            ResultStyle.SEGMENT_BAR -> {
                val card = LinearLayout(this); card.orientation = LinearLayout.VERTICAL
                card.background = Ui.cardBg(this, t)
                card.setPadding(Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f))
                val pctBig = Ui.txt(this, t, "$percent%", 40f, t.text, bold = true)
                card.addView(pctBig)
                Ui.countUp(pctBig, percent, { v -> "$v%" }, 1100L)
                val scoreLine = Ui.txt(this, t, "Score: $score / $displayTotal", 14f, t.muted)
                card.addView(scoreLine)
                Ui.countUp(scoreLine, score, { v -> "Score: $v / $displayTotal" }, 1100L)
                // Stacked segment bar: correct / wrong / skipped proportions.
                val totalQ = (correct + incorrect + skipped).coerceAtLeast(1)
                val bar = LinearLayout(this); bar.orientation = LinearLayout.HORIZONTAL
                bar.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 16f), intArrayOf(0, 12, 0, 8))
                fun seg(n: Int, color: Int) {
                    if (n <= 0) return
                    val v = android.view.View(this)
                    v.layoutParams = LinearLayout.LayoutParams(0, Ui.MATCH, n.toFloat())
                    v.setBackgroundColor(color)
                    bar.addView(v)
                }
                seg(correct, t.primary); seg(incorrect, 0xFFC0392B.toInt()); seg(skipped, 0xFFB9B9B9.toInt())
                card.addView(bar)
                card.addView(Ui.txt(this, t, "Correct $correct  -  Wrong $incorrect  -  Skipped $skipped", 12f, t.muted))
                wrap.addView(card)
            }
            ResultStyle.SPLIT_PANEL -> {
                val row = LinearLayout(this); row.orientation = LinearLayout.HORIZONTAL
                val left = LinearLayout(this); left.orientation = LinearLayout.VERTICAL; left.gravity = Gravity.CENTER
                left.background = Ui.pillBg(t.primary, t.radiusDp, this)
                left.setPadding(Ui.dp(this, 14f), Ui.dp(this, 24f), Ui.dp(this, 14f), Ui.dp(this, 24f))
                left.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f).apply { marginEnd = Ui.dp(this@ResultActivity, 10f) }
                left.addView(Ui.txt(this, t, "$score / $displayTotal", 26f, Color.WHITE, bold = true, gravity = Gravity.CENTER))
                left.addView(Ui.txt(this, t, "$percent%", 15f, 0xFFD8F3E3.toInt(), gravity = Gravity.CENTER))
                row.addView(left)
                val right = LinearLayout(this); right.orientation = LinearLayout.VERTICAL
                right.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
                right.addView(statTile("Correct", correct.toString(), t.primary).apply { layoutParams = Ui.lp(this@ResultActivity, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 8)) })
                right.addView(statTile("Wrong", incorrect.toString(), 0xFFC0392B.toInt()).apply { layoutParams = Ui.lp(this@ResultActivity, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 8)) })
                right.addView(statTile("Skipped", skipped.toString()).apply { layoutParams = Ui.lp(this@ResultActivity, Ui.MATCH, Ui.WRAP, null) })
                row.addView(right)
                wrap.addView(row)
            }
            ResultStyle.SKEW_BAND -> {
                val band = LinearLayout(this); band.orientation = LinearLayout.VERTICAL; band.gravity = Gravity.CENTER
                band.background = Ui.pillBg(t.primary, 10f, this)
                band.rotation = 0f
                band.setPadding(Ui.dp(this, 16f), Ui.dp(this, 20f), Ui.dp(this, 16f), Ui.dp(this, 20f))
                band.addView(Ui.txt(this, t, "$score / $displayTotal", 34f, Color.WHITE, bold = true, italic = true, gravity = Gravity.CENTER))
                band.addView(Ui.txt(this, t, "$percent% - keep pushing", 14f, 0xFFD8F3E3.toInt(), italic = true, gravity = Gravity.CENTER))
                wrap.addView(band)
                // Slanted bar chart of the three counts.
                val chart = LinearLayout(this); chart.orientation = LinearLayout.HORIZONTAL; chart.gravity = Gravity.BOTTOM
                chart.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 90f), intArrayOf(0, 16, 0, 0))
                val maxV = maxOf(correct, incorrect, skipped, 1)
                for ((label, v, color) in listOf(
                    Triple("Correct", correct, t.primary),
                    Triple("Wrong", incorrect, 0xFFC0392B.toInt()),
                    Triple("Skipped", skipped, 0xFF9AA5A0.toInt())
                )) {
                    val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    col.layoutParams = LinearLayout.LayoutParams(0, Ui.MATCH, 1f)
                    col.addView(Ui.txt(this, t, v.toString(), 14f, t.text, bold = true, gravity = Gravity.CENTER))
                    val barV = android.view.View(this)
                    barV.layoutParams = LinearLayout.LayoutParams(Ui.dp(this, 34f), Ui.dp(this, (54f * v / maxV).coerceAtLeast(4f)))
                    barV.background = Ui.pillBg(color, 4f, this)
                    col.addView(barV)
                    col.addView(Ui.txt(this, t, label, 11f, t.muted, gravity = Gravity.CENTER))
                    chart.addView(col)
                }
                wrap.addView(chart)
            }
            ResultStyle.STAT_GRID, ResultStyle.BIG_NUMBER_TILES -> {
                val big = Ui.txt(this, t, "$percent%", 52f, t.text, bold = true, gravity = Gravity.CENTER)
                wrap.addView(big)
                Ui.countUp(big, percent, { v -> "$v%" }, 1100L)
                val s = Ui.txt(this, t, "Score: $score / $displayTotal", 15f, t.muted, gravity = Gravity.CENTER)
                Ui.countUp(s, score, { v -> "Score: $v / $displayTotal" }, 1100L)
                s.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 2, 0, 4)); wrap.addView(s)
                wrap.addView(statsRow(correct, incorrect, skipped))
                if (t.result == ResultStyle.STAT_GRID) {
                    val seg = Ui.segmentedBar(this, t, percent)
                    seg.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 0))
                    wrap.addView(seg)
                }
            }
        }
        return wrap
    }

    private fun reviewCard(r: JSONObject): android.view.View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.cardBg(this, t)
        card.setPadding(Ui.dp(this, 16f), Ui.dp(this, 14f), Ui.dp(this, 16f), Ui.dp(this, 14f))
        card.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 10))
        card.addView(Ui.txt(this, t, r.optString("question", ""), 15f, t.text, bold = true))
        val opts = r.optJSONObject("options")
        val correct = r.optString("correct_option", "")
        val selected = r.optString("selected_option", "")
        for (letter in listOf("A", "B", "C", "D")) {
            val label = when {
                letter == correct -> "$letter.  " + (opts?.optString(letter, "") ?: "") + "   (correct answer)"
                letter == selected -> "$letter.  " + (opts?.optString(letter, "") ?: "") + "   (your answer)"
                else -> "$letter.  " + (opts?.optString(letter, "") ?: "")
            }
            val color = when {
                letter == correct -> t.primary
                letter == selected -> 0xFFC0392B.toInt()
                else -> t.muted
            }
            val line = Ui.txt(this, t, label, 14f, color, bold = letter == correct || letter == selected)
            line.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 5, 0, 0))
            card.addView(line)
        }
        if (selected.isEmpty()) {
            val sk = Ui.txt(this, t, "You skipped this question.", 12f, t.muted)
            sk.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 0)); card.addView(sk)
        }
        // Working Show Explanation toggle in the review.
        val expl = r.optString("explanation", "")
        if (expl.isNotEmpty()) {
            val body = Ui.txt(this, t, expl, 13f, t.muted)
            body.visibility = android.view.View.GONE
            body.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 0))
            val toggle = Ui.txt(this, t, "Show Explanation", 13f, t.primary, bold = true)
            toggle.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 0))
            toggle.setOnClickListener {
                val show = body.visibility != android.view.View.VISIBLE
                body.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                toggle.text = if (show) "Hide Explanation" else "Show Explanation"
            }
            card.addView(toggle)
            card.addView(body)
        }
        // Save toggle.
        var saved = r.optBoolean("is_saved")
        val save = Ui.txt(this, t, if (saved) "Saved (tap to remove)" else "Save this MCQ", 13f, t.primary, bold = true)
        save.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 0))
        save.setOnClickListener {
            val b = JSONObject().put("question_id", r.optInt("id"))
            ApiClient.post("saved/toggle", b, token = Prefs.token,
                onOk = { d ->
                    saved = d.optBoolean("saved", saved)
                    save.text = if (saved) "Saved (tap to remove)" else "Save this MCQ"
                },
                onErr = { e -> Toast.makeText(this, e.message ?: "Please sign in to save questions.", Toast.LENGTH_LONG).show() })
        }
        card.addView(save)
        return card
    }
}

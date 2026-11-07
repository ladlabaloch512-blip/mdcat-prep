package com.abiz.mdcatprep

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Chapter selection for one subject: banner slot (chapters) at top, filter
 * chips All / Weak Topics only, chapter rows with MCQ count, subtopics,
 * progress and a Weak badge. Tap a chapter to start its quiz.
 */
class ChaptersActivity : AppCompatActivity() {

    private lateinit var t: ThemeSpec
    private lateinit var container: LinearLayout
    private lateinit var listBox: LinearLayout
    private var chapters: List<Chapter> = emptyList()
    private var weakOnly = false
    private var solvedOnly = false
    private lateinit var allChip: TextView
    private lateinit var weakChip: TextView
    private lateinit var solvedChip: TextView
    private lateinit var chipsBar: LinearLayout
    private var searchText = ""
    private var subjectName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        t = Themes.current()
        val subjectId = intent.getIntExtra("subject_id", 0)
        val subjectName = intent.getStringExtra("subject_name") ?: "Subject"
        this.subjectName = subjectName

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(t.bg)
        container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 24f))

        val back = Ui.txt(this, t, "< Back", 14f, t.primary, bold = true)
        back.setOnClickListener { finish() }
        container.addView(back)
        val titleRow = LinearLayout(this)
        titleRow.orientation = LinearLayout.HORIZONTAL
        titleRow.gravity = Gravity.CENTER_VERTICAL
        titleRow.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 4))
        val title = Ui.txt(this, t, subjectName, 24f, t.text, bold = true)
        title.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        titleRow.addView(title)
        val searchBox = LinearLayout(this)
        searchBox.orientation = LinearLayout.HORIZONTAL
        searchBox.gravity = Gravity.CENTER_VERTICAL
        searchBox.visibility = android.view.View.GONE
        val searchBtn = Ui.icon(this, R.drawable.ic_search, 24f, t.primary)
        searchBtn.setOnClickListener {
            searchBox.visibility = if (searchBox.visibility == android.view.View.VISIBLE) android.view.View.GONE else android.view.View.VISIBLE
        }
        titleRow.addView(searchBtn)
        container.addView(titleRow)
        container.addView(Ui.txt(this, t, "Pick a chapter to start its MCQ quiz.", 13f, t.muted))
        searchBox.background = Ui.cardBg(this, t)
        searchBox.setPadding(Ui.dp(this, 12f), 0, Ui.dp(this, 12f), 0)
        searchBox.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 48f), intArrayOf(0, 10, 0, 0))
        searchBox.addView(Ui.icon(this, R.drawable.ic_search, 19f, t.muted))
        val searchInput = EditText(this)
        searchInput.hint = "Search chapters"
        searchInput.setTextColor(t.text)
        searchInput.setHintTextColor(t.muted)
        searchInput.textSize = 14f
        searchInput.setSingleLine(true)
        searchInput.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        searchInput.setPadding(Ui.dp(this, 8f), 0, 0, 0)
        searchInput.layoutParams = LinearLayout.LayoutParams(0, Ui.MATCH, 1f)
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                searchText = s?.toString()?.trim() ?: ""
                if (chapters.isNotEmpty()) renderList()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, n: Int) { }
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, n: Int) { }
        })
        searchBox.addView(searchInput)
        container.addView(searchBox)

        // Filter chips live in a fixed bottom bar (reference screen 4).
        chipsBar = LinearLayout(this)
        chipsBar.orientation = LinearLayout.HORIZONTAL
        chipsBar.gravity = Gravity.CENTER
        chipsBar.background = android.graphics.drawable.ColorDrawable(t.surface)
        chipsBar.setPadding(Ui.dp(this, 14f), Ui.dp(this, 10f), Ui.dp(this, 14f), Ui.dp(this, 12f))
        allChip = chip("All chapters") { weakOnly = false; solvedOnly = false; styleChips(); renderList() }
        weakChip = chip("Weak Topics only") { weakOnly = true; solvedOnly = false; styleChips(); renderList() }
        solvedChip = chip("Solved") { weakOnly = false; solvedOnly = true; styleChips(); renderList() }
        chipsBar.addView(allChip)
        chipsBar.addView(weakChip)
        chipsBar.addView(solvedChip)
        styleChips()

        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        container.addView(listBox)
        scroll.addView(container)
        val rootCol = LinearLayout(this)
        rootCol.orientation = LinearLayout.VERTICAL
        rootCol.setBackgroundColor(t.bg)
        scroll.layoutParams = LinearLayout.LayoutParams(Ui.MATCH, 0, 1f)
        rootCol.addView(scroll)
        rootCol.addView(chipsBar)
        setContentView(rootCol)

        val loading = Ui.skeleton(this, t, 3)
        listBox.addView(loading)
        ApiClient.get("chapters", mapOf("subject_id" to subjectId.toString()), token = Prefs.token,
            onCache = { data ->
                android.widget.Toast.makeText(this, "No internet. Showing your saved data.", android.widget.Toast.LENGTH_LONG).show()
                val banners = data.optJSONArray("banners").toList().map { Banner.from(it) }
                if (banners.isNotEmpty()) {
                    val strip = Components.bannerStrip(this, t, banners)
                    strip.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 6))
                    container.addView(strip, 4)
                }
                chapters = data.optJSONArray("chapters").toList().map { Chapter.from(it) }
                renderList()
            },
            onOk = { data ->
                // Banner slot: chapters (top).
                val banners = data.optJSONArray("banners").toList().map { Banner.from(it) }
                if (banners.isNotEmpty()) {
                    val strip = Components.bannerStrip(this, t, banners)
                    strip.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 6))
                    container.addView(strip, 4)
                }
                chapters = data.optJSONArray("chapters").toList().map { Chapter.from(it) }
                renderList()
            },
            onErr = { e ->
                listBox.removeAllViews()
                val err = Ui.txt(this, t, (e.message ?: "Could not load chapters.") + "\nTap to retry.", 14f, t.muted, gravity = Gravity.CENTER)
                err.setOnClickListener { recreate() }
                listBox.addView(err)
            })
    }

    private fun chip(label: String, action: () -> Unit): TextView {
        val tv = Ui.txt(this, t, label, 13f, t.text, bold = true, gravity = Gravity.CENTER)
        tv.setPadding(Ui.dp(this, 14f), Ui.dp(this, 8f), Ui.dp(this, 14f), Ui.dp(this, 8f))
        val lp = LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP)
        lp.marginEnd = Ui.dp(this, 8f)
        tv.layoutParams = lp
        tv.setOnClickListener { action() }
        return tv
    }

    private fun styleChips() {
        fun style(tv: TextView, active: Boolean) {
            tv.background = Ui.pillBg(if (active) t.primary else t.surface, 20f, this)
            tv.setTextColor(if (active) t.onPrimary else t.muted)
        }
        style(allChip, !weakOnly && !solvedOnly)
        style(weakChip, weakOnly)
        style(solvedChip, solvedOnly)
    }

    private fun renderList() {
        listBox.removeAllViews()
        // Filter change motion: the list slides in from the side the
        // filter lives on, rows then cascade (see enterChildren below).
        listBox.alpha = 0f
        listBox.translationX = Ui.dp(this, if (weakOnly || solvedOnly) 26f else -26f).toFloat()
        listBox.animate().alpha(1f).translationX(0f).setDuration(280L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction { listBox.alpha = 1f; listBox.translationX = 0f }
            .start()
        val searched = if (searchText.isEmpty()) chapters
            else chapters.filter { it.title.contains(searchText, ignoreCase = true) }
        val shown = when {
            weakOnly -> searched.filter { it.isWeak }
            solvedOnly -> searched.filter { it.questionCount > 0 && it.attempted >= it.questionCount }
            else -> searched
        }
        if (shown.isEmpty()) {
            listBox.addView(Ui.txt(this, t,
                when {
                    searchText.isNotEmpty() -> "No chapters match your search."
                    weakOnly -> "No weak topics yet. Weak topics appear after you attempt a chapter and score below the threshold."
                    solvedOnly -> "No solved chapters yet. A chapter counts as solved when you have attempted every question in it."
                    else -> "No chapters are available for this subject yet."
                },
                14f, t.muted, gravity = Gravity.CENTER))
            return
        }
        shown.forEachIndexed { i, ch -> listBox.addView(chapterRow(ch, i)) }
        Ui.enterChildren(listBox)
    }

    private fun chapterRow(ch: Chapter, index: Int): android.view.View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.background = Ui.cardBg(this, t)
        row.setPadding(Ui.dp(this, 16f), Ui.dp(this, 14f), Ui.dp(this, 16f), Ui.dp(this, 14f))
        row.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 0))
        Ui.pressable(row)
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val chIcon = Ui.icon(this, Ui.subjectIcon(subjectName), 22f, t.primary)
        chIcon.layoutParams = LinearLayout.LayoutParams(Ui.dp(this, 22f), Ui.dp(this, 22f)).apply { marginEnd = Ui.dp(this@ChaptersActivity, 10f) }
        top.addView(chIcon)
        val title = Ui.txt(this, t, (if (t.id == "editorial") String.format("%02d   ", index + 1) else "") + ch.title, 16f, t.text, bold = true)
        title.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        top.addView(title)
        if (ch.isWeak) {
            val badge = Ui.txt(this, t, "Weak", 11f, Color.WHITE, bold = true, gravity = Gravity.CENTER)
            badge.background = Ui.pillBg(0xFFC0392B.toInt(), 12f, this)
            badge.setPadding(Ui.dp(this, 10f), Ui.dp(this, 4f), Ui.dp(this, 10f), Ui.dp(this, 4f))
            top.addView(badge)
        }
        if (ch.questionCount > 0 && ch.attempted >= ch.questionCount) {
            val badge = Ui.txt(this, t, "Solved", 11f, Color.WHITE, bold = true, gravity = Gravity.CENTER)
            badge.background = Ui.pillBg(t.primary, 12f, this)
            badge.setPadding(Ui.dp(this, 10f), Ui.dp(this, 4f), Ui.dp(this, 10f), Ui.dp(this, 4f))
            top.addView(badge)
        }
        row.addView(top)
        if (ch.subtopics.isNotEmpty()) row.addView(Ui.txt(this, t, ch.subtopics, 12f, t.muted))
        row.addView(Ui.txt(this, t,
            if (ch.attempted > 0) "${ch.attempted}/${ch.questionCount} MCQs attempted"
            else "${ch.questionCount} MCQs", 12f, t.muted))
        val bar = Ui.progressBar(this, t, ch.percent, 6f)
        bar.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 6f), intArrayOf(0, 8, 0, 0))
        row.addView(bar)
        row.addView(Ui.txt(this, t, "${ch.percent}% correct so far", 11f, t.muted))
        row.setOnClickListener {
            if (ch.questionCount <= 0) {
                Toast.makeText(this, "This chapter has no MCQs yet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("type", "chapter")
            intent.putExtra("chapter_id", ch.id)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
        return row
    }
}

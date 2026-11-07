package com.abiz.mdcatprep

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

/**
 * MCQ quiz: countdown from the server-provided time limit (auto-submit on
 * finish), free answer changes, bookmark toggle, Previous/Next, Submit on
 * the last question. Explanations are only returned by the server after
 * submit, so the in-quiz explanation toggle truthfully shows that note;
 * the working explanation toggle lives in the Result review.
 */
class QuizActivity : AppCompatActivity() {

    private lateinit var t: ThemeSpec
    private var quizType: String = "chapter"
    private var chapterId: Int = 0
    private var questions: List<Question> = emptyList()
    private val answers = HashMap<Int, String>()
    private var index = 0
    private var navDirection = 1
    private var lastPickedId = -1
    private var lastPickedLetter = ""
    private var timeLimitSeconds = 0
    private var secondsPerQuestion = 60
    private var startedAt = System.currentTimeMillis()
    private var timer: CountDownTimer? = null
    private var submitted = false

    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var timerView: TextView
    private lateinit var countView: TextView
    private lateinit var progressHost: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        t = Themes.current()
        quizType = intent.getStringExtra("type") ?: "chapter"
        chapterId = intent.getIntExtra("chapter_id", 0)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(t.bg)
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 24f))
        scroll.addView(root)
        setContentView(scroll)

        val loading = Ui.skeleton(this, t, 2)
        root.addView(loading)
        val params = if (quizType == "mock") emptyMap() else mapOf("type" to "chapter", "chapter_id" to chapterId.toString())
        val fullParams = if (quizType == "mock") mapOf("type" to "mock") else params
        ApiClient.get("quiz/start", fullParams, token = Prefs.token,
            onOk = { data ->
                root.removeView(loading)
                quizType = data.optString("quiz_type", quizType)
                chapterId = data.optInt("chapter_id", chapterId)
                timeLimitSeconds = data.optInt("time_limit_seconds", 0)
                secondsPerQuestion = data.optInt("seconds_per_question", 60)
                questions = data.optJSONArray("questions").toList().map { Question.from(it) }
                if (questions.isEmpty()) {
                    root.addView(Ui.txt(this, t, "No questions are available for this quiz yet.", 15f, t.muted, gravity = Gravity.CENTER))
                    val backBtn = Button(this)
                    backBtn.text = "Go Back"
                    backBtn.setOnClickListener { finish() }
                    root.addView(backBtn)
                } else {
                    buildShell(data.optString("title", if (quizType == "mock") "Full Mock Test" else "Chapter Quiz"), data.optInt("total", questions.size))
                    startedAt = System.currentTimeMillis()
                    startTimer()
                    renderQuestion()
                }
            },
            onErr = { e ->
                root.removeView(loading)
                val err = Ui.txt(this, t, (e.message ?: "Could not load the quiz.") + "\nTap to retry.", 15f, t.muted, gravity = Gravity.CENTER)
                err.setOnClickListener { recreate() }
                root.addView(err)
            })
    }

    private fun buildShell(title: String, total: Int) {
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val titleView = Ui.txt(this, t, title, 18f, t.text, bold = true)
        titleView.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        header.addView(titleView)
        timerView = Ui.txt(this, t, formatTime(timeLimitSeconds), 16f, t.primary, bold = true)
        header.addView(timerView)
        root.addView(header)

        progressHost = LinearLayout(this)
        progressHost.orientation = LinearLayout.VERTICAL
        progressHost.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 4))
        root.addView(progressHost)

        countView = Ui.txt(this, t, "", 13f, t.muted)
        countView.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 4, 0, 10))
        root.addView(countView)

        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        root.addView(body)
    }

    private fun startTimer() {
        if (timeLimitSeconds <= 0) {
            timerView.text = "No time limit"
            return
        }
        timer = object : CountDownTimer(timeLimitSeconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                timerView.text = formatTime((ms / 1000).toInt())
                if (ms < 30000) timerView.setTextColor(0xFFC0392B.toInt())
                if (ms <= 10000) {
                    // Final-seconds pulse: the timer throbs once a second.
                    timerView.animate().scaleX(1.22f).scaleY(1.22f).setDuration(160L)
                        .withEndAction {
                            timerView.animate().scaleX(1f).scaleY(1f).setDuration(300L)
                                .withEndAction { timerView.scaleX = 1f; timerView.scaleY = 1f }
                                .start()
                        }
                        .start()
                }
            }
            override fun onFinish() {
                if (!submitted) {
                    Toast.makeText(this@QuizActivity, "Time is up. Submitting your answers.", Toast.LENGTH_LONG).show()
                    submit()
                }
            }
        }.start()
    }

    private fun formatTime(sec: Int): String = String.format("%02d:%02d", sec / 60, sec % 60)

    private fun renderQuestion() {
        val q = questions[index]
        countView.text = "Question ${index + 1} of ${questions.size}"
        progressHost.removeAllViews()
        if (t.id == "teal") {
            progressHost.addView(Ui.segmentedBar(this, t, (index + 1) * 100 / questions.size))
        } else {
            progressHost.addView(Ui.progressBar(this, t, (index + 1) * 100 / questions.size, 6f))
        }
        body.removeAllViews()

        if (q.subtopic.isNotEmpty()) body.addView(Ui.txt(this, t, q.subtopic, 12f, t.primary, bold = true))
        val qText = Ui.txt(this, t, q.text, 17f, t.text, bold = true)
        qText.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 14))
        body.addView(qText)

        // Options, rendered in the design's option style.
        val selected = answers[q.id] ?: ""
        when (t.options) {
            OptionStyle.GRID_2X2 -> {
                val grid = GridLayout(this)
                grid.columnCount = 2
                for (letter in listOf("A", "B", "C", "D")) {
                    val opt = optionView(q, letter, selected == letter)
                    val lp = GridLayout.LayoutParams()
                    lp.width = 0
                    lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    lp.setMargins(Ui.dp(this, 4f), Ui.dp(this, 4f), Ui.dp(this, 4f), Ui.dp(this, 4f))
                    opt.layoutParams = lp
                    grid.addView(opt)
                }
                body.addView(grid)
            }
            OptionStyle.DIVIDED_CARD, OptionStyle.EXAM_SHEET -> {
                val card = LinearLayout(this)
                card.orientation = LinearLayout.VERTICAL
                card.background = Ui.cardBg(this, t)
                card.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 6))
                listOf("A", "B", "C", "D").forEachIndexed { i, letter ->
                    if (i > 0) {
                        val divider = android.view.View(this)
                        divider.setBackgroundColor(if (t.dark) 0xFF2A332D.toInt() else 0xFFE6E2D8.toInt())
                        divider.layoutParams = LinearLayout.LayoutParams(Ui.MATCH, Ui.dp(this, 1f))
                        card.addView(divider)
                    }
                    card.addView(optionView(q, letter, selected == letter, inCard = true))
                }
                body.addView(card)
            }
            else -> {
                for (letter in listOf("A", "B", "C", "D")) {
                    body.addView(optionView(q, letter, selected == letter))
                }
            }
        }

        // Bookmark + explanation note row.
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        val bmIcon = Ui.icon(this, R.drawable.ic_bookmark, 18f, if (q.isSaved) t.primary else t.muted)
        row.addView(bmIcon)
        val bookmark = Ui.txt(this, t, if (q.isSaved) "Saved (tap to remove)" else "Bookmark this question", 13f, t.primary, bold = true)
        bookmark.setPadding(Ui.dp(this, 6f), 0, 0, 0)
        bookmark.setOnClickListener { toggleSaved(q, bookmark, bmIcon) }
        row.addView(bookmark)
        body.addView(row)

        val explToggle = Ui.txt(this, t, "Show Explanation", 13f, t.muted, bold = true)
        explToggle.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 0))
        val explNote = Ui.txt(this, t, "Explanation will be shown in the review after you submit.", 12f, t.muted)
        explNote.visibility = android.view.View.GONE
        explNote.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 4, 0, 0))
        explToggle.setOnClickListener {
            val showing = explNote.visibility == android.view.View.VISIBLE
            if (showing) {
                explNote.animate().alpha(0f).translationY(-Ui.dp(this, 6f).toFloat()).setDuration(160L)
                    .withEndAction {
                        explNote.visibility = android.view.View.GONE
                        explNote.alpha = 1f; explNote.translationY = 0f
                    }.start()
                explToggle.text = "Show Explanation"
            } else {
                explNote.visibility = android.view.View.VISIBLE
                explNote.alpha = 0f
                explNote.translationY = -Ui.dp(this, 8f).toFloat()
                explNote.animate().alpha(1f).translationY(0f).setDuration(280L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction { explNote.alpha = 1f; explNote.translationY = 0f }
                    .start()
                explToggle.text = "Hide Explanation"
            }
        }
        body.addView(explToggle)
        body.addView(explNote)

        // Previous / Next / Submit.
        val nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 20, 0, 0))
        val prev = Button(this)
        prev.text = "Previous"
        prev.isEnabled = index > 0
        prev.layoutParams = LinearLayout.LayoutParams(0, Ui.dp(this, 50f), 1f).apply { marginEnd = Ui.dp(this@QuizActivity, 8f) }
        prev.setOnClickListener { if (index > 0) { index--; navDirection = -1; renderQuestion() } }
        nav.addView(prev)
        val next = Button(this)
        val last = index == questions.size - 1
        next.text = if (last) "Submit" else "Next"
        next.setTextColor(t.onPrimary)
        next.background = Ui.pillBg(t.primary, t.radiusDp, this)
        next.layoutParams = LinearLayout.LayoutParams(0, Ui.dp(this, 50f), 1f)
        next.setOnClickListener {
            if (last) confirmSubmit() else { index++; navDirection = 1; renderQuestion() }
        }
        nav.addView(next)
        body.addView(nav)
        Ui.slideIn(body, navDirection)
    }

    private fun optionView(q: Question, letter: String, isSelected: Boolean, inCard: Boolean = false): android.view.View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(Ui.dp(this, 14f), Ui.dp(this, 14f), Ui.dp(this, 14f), Ui.dp(this, 14f))
        if (!inCard) {
            row.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 10))
            val fill = when {
                isSelected -> t.primary
                t.options == OptionStyle.BOLD_BLOCKS -> t.surface
                else -> t.surface
            }
            row.background = Ui.cardBg(this, t, fill = fill)
            if (t.options == OptionStyle.SIDE_STRIP && !isSelected) {
                // Split Panel Pro: green side strip via a leading bar child.
            }
        }
        if (t.options == OptionStyle.SIDE_STRIP) {
            val strip = android.view.View(this)
            strip.setBackgroundColor(if (isSelected) Color.WHITE else t.primary)
            strip.layoutParams = LinearLayout.LayoutParams(Ui.dp(this, 5f), Ui.dp(this, 34f))
            row.addView(strip)
        }
        val chip = Ui.txt(this, t, letter, if (t.options == OptionStyle.BOLD_BLOCKS) 18f else 14f,
            if (isSelected) t.onPrimary else t.primary, bold = true, gravity = Gravity.CENTER)
        val chipBg = android.graphics.drawable.GradientDrawable()
        chipBg.shape = if (t.options == OptionStyle.EXAM_SHEET) android.graphics.drawable.GradientDrawable.RECTANGLE else android.graphics.drawable.GradientDrawable.OVAL
        chipBg.setColor(if (isSelected) (if (t.dark) 0xFF0B3D28.toInt() else 0x33000000) else (if (t.dark) 0xFF223028.toInt() else 0xFFE7F6EE.toInt()))
        if (t.options == OptionStyle.EXAM_SHEET) chipBg.setStroke(Ui.dp(this, 1.5f), if (isSelected) Color.WHITE else t.text)
        chip.background = chipBg
        chip.layoutParams = LinearLayout.LayoutParams(Ui.dp(this, 36f), Ui.dp(this, 36f))
        row.addView(chip)
        val label = Ui.txt(this, t, q.options[letter] ?: "", 15f, if (isSelected && !inCard) t.onPrimary else t.text)
        label.setPadding(Ui.dp(this, 12f), 0, 0, 0)
        label.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        row.addView(label)
        Ui.pressable(row)
        if (isSelected && q.id == lastPickedId && letter == lastPickedLetter) {
            // The tap rebuilt this question; pop the freshly rendered
            // selected option so the student sees the pick land.
            lastPickedId = -1
            row.post { Ui.tapPop(row) }
        }
        row.setOnClickListener {
            answers[q.id] = letter
            lastPickedId = q.id
            lastPickedLetter = letter
            renderQuestion()
        }
        return row
    }

    private fun toggleSaved(q: Question, label: TextView, icon: ImageView? = null) {
        val bodyJson = JSONObject().put("question_id", q.id)
        ApiClient.post("saved/toggle", bodyJson, token = Prefs.token,
            onOk = { d ->
                q.isSaved = d.optBoolean("saved", q.isSaved)
                label.text = if (q.isSaved) "Saved (tap to remove)" else "Bookmark this question"
                icon?.setColorFilter(if (q.isSaved) t.primary else t.muted)
                Ui.tapPop(label)
                Toast.makeText(this, if (q.isSaved) "Saved." else "Removed from saved.", Toast.LENGTH_SHORT).show()
            },
            onErr = { e ->
                // Guests receive the server's sign-in prompt (401 auth_required).
                Toast.makeText(this, e.message ?: "Please sign in to save questions.", Toast.LENGTH_LONG).show()
            })
    }

    private fun confirmSubmit() {
        val unanswered = questions.count { answers[it.id].isNullOrEmpty() }
        val msg = if (unanswered > 0) "You have $unanswered unanswered question(s). Submit anyway?" else "Submit your answers?"
        AlertDialog.Builder(this)
            .setTitle("Submit quiz")
            .setMessage(msg)
            .setNegativeButton("Keep answering", null)
            .setPositiveButton("Submit") { _, _ -> submit() }
            .show()
    }

    private fun submit() {
        if (submitted) return
        submitted = true
        timer?.cancel()
        val arr = JSONArray()
        for (q in questions) {
            arr.put(JSONObject().put("question_id", q.id).put("selected_option", answers[q.id] ?: ""))
        }
        val timeTaken = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
        val body = JSONObject()
            .put("type", quizType)
            .put("chapter_id", chapterId)
            .put("answers", arr)
            .put("time_taken_seconds", timeTaken)
        ApiClient.post("quiz/submit", body, token = Prefs.token,
            onOk = { data ->
                QuizSession.lastResult = data
                QuizSession.lastQuizType = quizType
                QuizSession.lastChapterId = chapterId
                startActivity(Intent(this, ResultActivity::class.java))
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                finish()
            },
            onErr = { e ->
                submitted = false
                // Guests cannot submit (401 auth_required): show the server
                // message and offer sign-in.
                if (e.code == "auth_required" || e.httpStatus == 401) {
                    AlertDialog.Builder(this)
                        .setTitle("Sign in required")
                        .setMessage(e.message ?: "Please sign in to submit your quiz and save your result.")
                        .setNegativeButton("Stay as guest", null)
                        .setPositiveButton("Sign In") { _, _ ->
                            startActivity(Intent(this, AuthActivity::class.java))
                            finish()
                        }
                        .show()
                } else {
                    Toast.makeText(this, e.message ?: "Could not submit the quiz.", Toast.LENGTH_LONG).show()
                }
            })
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}

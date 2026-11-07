package com.abiz.mdcatprep

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONObject

/** Saved MCQs tab: bookmarked questions with the correct answer, the
 *  explanation and a remove action. Requires sign-in (guests get the
 *  server's auth_required message with a retry). */
class SavedFragment : Fragment() {

    private lateinit var t: ThemeSpec
    private lateinit var container: LinearLayout
    private lateinit var statusView: View
    private lateinit var listBox: LinearLayout
    private var allQuestions: List<JSONObject> = emptyList()
    private var subjectFilter = ""

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        t = (activity as MainActivity).theme
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(t.bg)
        container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f))
        container.addView(Ui.txt(requireContext(), t, "Saved MCQs", 22f, t.text, bold = true))
        statusView = Ui.skeleton(requireContext(), t, 3)
        statusView.layoutParams = Ui.lp(requireContext(), Ui.MATCH, Ui.WRAP, intArrayOf(0, 16, 0, 0))
        container.addView(statusView)
        listBox = LinearLayout(requireContext())
        listBox.orientation = LinearLayout.VERTICAL
        container.addView(listBox)
        scroll.addView(container)
        Ui.pullToRefresh(scroll) { load() }
        load()
        return scroll
    }

    private fun load() {
        ApiClient.get("saved", token = Prefs.token,
            onCache = { data ->
                if (!isAdded) return@get
                android.widget.Toast.makeText(requireContext(), "No internet. Showing your saved data.", android.widget.Toast.LENGTH_LONG).show()
                render(data)
            },
            onOk = { data ->
                if (!isAdded) return@get
                render(data)
            },
            onErr = { e ->
                if (!isAdded) return@get
                container.removeView(statusView)
                val err = Ui.txt(requireContext(), t, (e.message ?: "Could not load saved questions.") + "\nTap to retry.", 14f, t.muted, gravity = Gravity.CENTER)
                err.setOnClickListener {
                    container.removeView(err)
                    statusView = Ui.skeleton(requireContext(), t, 3)
                    container.addView(statusView)
                    load()
                }
                container.addView(err)
            })
    }

    private fun render(data: JSONObject) {
        val ctx = requireContext()
        container.removeView(statusView)
        listBox.removeAllViews()
        // Banner slot: saved (top).
        val banners = data.optJSONArray("banners").toList().map { Banner.from(it) }
        val strip = Components.bannerStrip(ctx, t, banners)
        strip.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 0))
        listBox.addView(strip)

        allQuestions = data.optJSONArray("questions").toList()
        if (allQuestions.isEmpty()) {
            val empty = Ui.txt(ctx, t, "No saved MCQs yet.\nBookmark a question during a quiz and it will appear here.", 14f, t.muted, gravity = Gravity.CENTER)
            empty.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 24, 0, 0))
            listBox.addView(empty)
            return
        }
        // Subject filter chips (reference screen 10): All + one per subject.
        val subjects = allQuestions.map { it.optString("subject_title", "") }.filter { it.isNotEmpty() }.distinct()
        if (subjects.isNotEmpty()) {
            val chips = LinearLayout(ctx)
            chips.orientation = LinearLayout.HORIZONTAL
            chips.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 2))
            fun chip(label: String, value: String): android.widget.TextView {
                val tv = Ui.txt(ctx, t, label, 13f, if (subjectFilter == value) t.onPrimary else t.text, bold = true, gravity = Gravity.CENTER)
                tv.background = Ui.pillBg(if (subjectFilter == value) t.primary else t.surface, 16f, ctx)
                tv.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 8f), Ui.dp(ctx, 14f), Ui.dp(ctx, 8f))
                tv.layoutParams = LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP).apply { marginEnd = Ui.dp(ctx, 8f) }
                tv.setOnClickListener { subjectFilter = value; renderList() }
                Ui.pressable(tv)
                return tv
            }
            chips.addView(chip("All", ""))
            for (sub in subjects) chips.addView(chip(sub, sub))
            listBox.addView(chips)
        }
        renderList()
    }

    private fun renderList() {
        val ctx = requireContext()
        // Remove previously rendered cards (everything after strip+chips).
        while (listBox.childCount > 2) listBox.removeViewAt(2)
        val shown = if (subjectFilter.isEmpty()) allQuestions
            else allQuestions.filter { it.optString("subject_title", "") == subjectFilter }
        if (shown.isEmpty()) {
            val empty = Ui.txt(ctx, t, "No saved MCQs in this subject yet.", 14f, t.muted, gravity = Gravity.CENTER)
            empty.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 24, 0, 0))
            listBox.addView(empty)
            return
        }
        for (q in shown) listBox.addView(questionCard(q))
        Ui.enterChildren(listBox, 2)
    }

    private fun questionCard(q: JSONObject): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.cardBg(ctx, t)
        card.setPadding(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f))
        card.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 0))

        val chapterTitle = q.optString("chapter_title", "")
        if (chapterTitle.isNotEmpty()) card.addView(Ui.txt(ctx, t, chapterTitle, 12f, t.primary, bold = true))
        card.addView(Ui.txt(ctx, t, q.optString("question", ""), 15f, t.text, bold = true))
        val opts = q.optJSONObject("options")
        for (letter in listOf("A", "B", "C", "D")) {
            val isCorrect = q.optString("correct_option", "") == letter
            val line = Ui.txt(ctx, t, "$letter.  " + (opts?.optString(letter, "") ?: ""), 14f,
                if (isCorrect) t.primary else t.muted, bold = isCorrect)
            line.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 0))
            card.addView(line)
        }
        val explanation = q.optString("explanation", "")
        if (explanation.isNotEmpty()) {
            val ex = Ui.txt(ctx, t, "Explanation: $explanation", 13f, t.muted)
            ex.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 0))
            card.addView(ex)
        }
        val remove = Ui.txt(ctx, t, "Remove from saved", 13f, 0xFFC0392B.toInt(), bold = true)
        remove.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 0))
        remove.setOnClickListener {
            val body = JSONObject().put("question_id", q.optInt("id"))
            ApiClient.post("saved/toggle", body, token = Prefs.token,
                onOk = {
                    Toast.makeText(ctx, "Removed from saved.", Toast.LENGTH_SHORT).show()
                    card.visibility = View.GONE
                },
                onErr = { e -> Toast.makeText(ctx, e.message ?: "Could not update.", Toast.LENGTH_LONG).show() })
        }
        card.addView(remove)
        return card
    }
}

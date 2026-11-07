package com.abiz.mdcatprep

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment

/** Chapters tab: the subjects list, each leading to that subject's chapters. */
class SubjectsFragment : Fragment() {

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
        container.addView(Ui.txt(requireContext(), t, "Chapters", 22f, t.text, bold = true))
        val sub = Ui.txt(requireContext(), t, "Pick a subject to see its chapters.", 13f, t.muted)
        sub.layoutParams = Ui.lp(requireContext(), Ui.MATCH, Ui.WRAP, intArrayOf(0, 4, 0, 14))
        container.addView(sub)
        statusView = Ui.skeleton(requireContext(), t, 3)
        container.addView(statusView)
        scroll.addView(container)
        Ui.pullToRefresh(scroll) { load() }
        load()
        return scroll
    }

    private fun load() {
        ApiClient.get("subjects", token = Prefs.token,
            onCache = { data ->
                if (!isAdded) return@get
                android.widget.Toast.makeText(requireContext(), "No internet. Showing your saved data.", android.widget.Toast.LENGTH_LONG).show()
                val subjects = data.optJSONArray("items").toList().map { Subject.from(it) }
                render(subjects)
            },
            onOk = { data ->
                if (!isAdded) return@get
                val subjects = data.optJSONArray("items").toList().map { Subject.from(it) }
                render(subjects)
            },
            onErr = { e ->
                if (!isAdded) return@get
                container.removeView(statusView)
                val err = Ui.txt(requireContext(), t, (e.message ?: "Could not load subjects.") + "\nTap to retry.", 14f, t.muted, gravity = Gravity.CENTER)
                err.setOnClickListener {
                    container.removeView(err)
                    statusView = Ui.skeleton(requireContext(), t, 3)
                    container.addView(statusView)
                    load()
                }
                container.addView(err)
            })
    }

    private fun render(subjects: List<Subject>) {
        val ctx = requireContext()
        container.removeView(statusView)
        if (subjects.isEmpty()) {
            container.addView(Ui.txt(ctx, t, "No subjects are available yet.", 14f, t.muted))
            return
        }
        subjects.forEachIndexed { i, s ->
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.VERTICAL
            row.background = Ui.cardBg(ctx, t)
            row.setPadding(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f))
            row.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 10))
            row.addView(Ui.txt(ctx, t, (if (t.id == "editorial") String.format("%02d   ", i + 1) else "") + s.name, 17f, t.text, bold = true))
            if (s.tagline.isNotEmpty()) row.addView(Ui.txt(ctx, t, s.tagline, 13f, t.muted))
            row.addView(Ui.txt(ctx, t, "${s.chapterCount} chapters - ${s.questionCount} MCQs - ${s.percent}% complete", 12f, t.muted))
            val bar = Ui.progressBar(ctx, t, s.percent, 6f)
            bar.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 6f), intArrayOf(0, 10, 0, 0))
            row.addView(bar)
            Ui.pressable(row)
            row.setOnClickListener { main.openChapters(s) }
            container.addView(row)
        }
        Ui.enterChildren(container, 2)
    }
}

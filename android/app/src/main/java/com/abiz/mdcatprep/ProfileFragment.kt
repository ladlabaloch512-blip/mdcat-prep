package com.abiz.mdcatprep

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.AlertDialog
import android.widget.Button
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import org.json.JSONObject

/**
 * Profile tab: account details (or Guest), attempt history, the 10-design
 * switcher (applies instantly and persists), support/social links from
 * settings, and Logout / Sign In.
 */
class ProfileFragment : Fragment() {

    private lateinit var t: ThemeSpec
    private lateinit var main: MainActivity
    private lateinit var container: LinearLayout
    private var rootScroll: ScrollView? = null
    private var goalBoxRef: LinearLayout? = null
    private var histBoxRef: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        main = activity as MainActivity
        t = main.theme
        val scroll = ScrollView(requireContext())
        scroll.setBackgroundColor(t.bg)
        container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 18f), Ui.dp(requireContext(), 24f))
        scroll.addView(container)
        rootScroll = scroll
        build()
        return scroll
    }

    private fun build() {
        val ctx = requireContext()
        container.removeAllViews()
        val headRow = LinearLayout(ctx)
        headRow.orientation = LinearLayout.HORIZONTAL
        headRow.gravity = Gravity.CENTER_VERTICAL
        val pTitle = Ui.txt(ctx, t, "Profile", 22f, t.text, bold = true)
        pTitle.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        headRow.addView(pTitle)
        val gear = Ui.icon(ctx, R.drawable.ic_gear, 24f, t.text)
        Ui.pressable(gear)
        gear.setOnClickListener { startActivity(Intent(ctx, SettingsActivity::class.java)) }
        headRow.addView(gear)
        container.addView(headRow)

        val user = Prefs.user()
        val isGuest = Prefs.token == null
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.cardBg(ctx, t)
        card.setPadding(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f), Ui.dp(ctx, 16f))
        card.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        if (isGuest) {
            card.addView(Ui.txt(ctx, t, "Guest", 18f, t.text, bold = true))
            card.addView(Ui.txt(ctx, t, "You are browsing as a guest. Sign in to save MCQs and record your results.", 13f, t.muted))
        } else {
            card.addView(Ui.txt(ctx, t, user?.optString("name", "") ?: "", 18f, t.text, bold = true))
            card.addView(Ui.txt(ctx, t, user?.optString("email", "") ?: "", 13f, t.muted))
            val phone = user?.optString("phone", "") ?: ""
            if (phone.isNotEmpty()) card.addView(Ui.txt(ctx, t, phone, 13f, t.muted))
        }
        container.addView(card)

        // Menu rows (reference screen 11). Chat Support is excluded by
        // scope, so it never appears here.
        val menu = LinearLayout(ctx)
        menu.orientation = LinearLayout.VERTICAL
        menu.background = Ui.cardBg(ctx, t)
        menu.setPadding(Ui.dp(ctx, 6f), Ui.dp(ctx, 6f), Ui.dp(ctx, 6f), Ui.dp(ctx, 6f))
        menu.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        fun menuRow(icon: Int, label: String, action: () -> Unit) {
            val r = LinearLayout(ctx)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER_VERTICAL
            r.setPadding(Ui.dp(ctx, 10f), Ui.dp(ctx, 12f), Ui.dp(ctx, 10f), Ui.dp(ctx, 12f))
            r.addView(Ui.icon(ctx, icon, 20f, t.primary))
            val tv = Ui.txt(ctx, t, label, 15f, t.text, bold = true)
            tv.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
            tv.setPadding(Ui.dp(ctx, 12f), 0, 0, 0)
            r.addView(tv)
            r.addView(Ui.txt(ctx, t, ">", 15f, t.muted, bold = true))
            Ui.pressable(r)
            r.setOnClickListener { action() }
            menu.addView(r)
        }
        menuRow(R.drawable.ic_chart, "My Progress") { main.selectTab("analytics") }
        menuRow(R.drawable.ic_check, "My Test History") {
            rootScroll?.post { rootScroll?.smoothScrollTo(0, histBoxRef?.top ?: 0) }
        }
        menuRow(R.drawable.ic_bookmark, "Saved MCQs") { main.selectTab("saved") }
        menuRow(R.drawable.ic_edit, "Daily Study Goals") {
            rootScroll?.post { rootScroll?.smoothScrollTo(0, goalBoxRef?.top ?: 0) }
        }
        menuRow(R.drawable.ic_gear, "Settings & Security") { startActivity(Intent(ctx, SettingsActivity::class.java)) }
        container.addView(menu)

        // Daily Study Goals + Question of the Day cards.
        val goalBox = LinearLayout(ctx)
        goalBox.orientation = LinearLayout.VERTICAL
        container.addView(goalBox)
        goalBoxRef = goalBox
        buildGoalCard(goalBox, isGuest)
        val qotdBox = LinearLayout(ctx)
        qotdBox.orientation = LinearLayout.VERTICAL
        container.addView(qotdBox)
        buildQotdCard(qotdBox)

        // Attempt history (signed-in users).
        val histTitle = Ui.txt(ctx, t, "Attempt history", 17f, t.text, bold = true)
        histTitle.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 22, 0, 8))
        container.addView(histTitle)
        val histBox = LinearLayout(ctx)
        histBox.orientation = LinearLayout.VERTICAL
        container.addView(histBox)
        histBoxRef = histBox
        if (isGuest) {
            histBox.addView(Ui.txt(ctx, t, "Sign in to see your attempt history.", 13f, t.muted))
        } else {
            val loading = Ui.skeleton(ctx, t, 1)
            histBox.addView(loading)
            ApiClient.get("attempts", token = Prefs.token,
                onCache = { data ->
                    if (!isAdded) return@get
                    histBox.removeAllViews()
                    histBox.addView(Ui.txt(ctx, t, "Offline mode. Showing your last saved data.", 12f, t.muted))
                    val attempts = data.optJSONArray("items").toList()
                    if (attempts.isEmpty()) {
                        histBox.addView(Ui.txt(ctx, t, "No attempts yet. Take a quiz to get started.", 13f, t.muted))
                    } else {
                        for (a in attempts) histBox.addView(attemptRow(a))
                    }
                },
                onOk = { data ->
                    if (!isAdded) return@get
                    histBox.removeAllViews()
                    val attempts = data.optJSONArray("items").toList()
                    if (attempts.isEmpty()) {
                        histBox.addView(Ui.txt(ctx, t, "No attempts yet. Take a quiz to get started.", 13f, t.muted))
                    } else {
                        for (a in attempts) histBox.addView(attemptRow(a))
                    }
                },
                onErr = { e ->
                    if (!isAdded) return@get
                    histBox.removeAllViews()
                    histBox.addView(Ui.txt(ctx, t, e.message ?: "Could not load attempts.", 13f, t.muted))
                })
        }

        // Design switcher: the 10 designs.
        val designTitle = Ui.txt(ctx, t, "App design", 17f, t.text, bold = true)
        designTitle.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 22, 0, 4))
        container.addView(designTitle)
        val designNote = Ui.txt(ctx, t, "Tap a design to apply it instantly across the whole app.", 12f, t.muted)
        designNote.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 8))
        container.addView(designNote)
        Themes.ALL.forEachIndexed { i, spec ->
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val active = spec.id == t.id
            row.background = Ui.cardBg(ctx, t, fill = if (active) spec.primary else t.surface)
            row.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
            row.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 8))
            val swatch = View(ctx)
            val sw = android.graphics.drawable.GradientDrawable()
            sw.shape = android.graphics.drawable.GradientDrawable.OVAL
            sw.setColor(spec.primary)
            sw.setStroke(Ui.dp(ctx, 2f), if (active) android.graphics.Color.WHITE else spec.accent)
            swatch.background = sw
            swatch.layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 30f), Ui.dp(ctx, 30f))
            row.addView(swatch)
            val label = Ui.txt(ctx, t, "${i + 1}.  ${spec.name}", 15f, if (active) spec.onPrimary else t.text, bold = active)
            label.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
            label.setPadding(Ui.dp(ctx, 12f), 0, 0, 0)
            row.addView(label)
            if (active) row.addView(Ui.txt(ctx, t, "Active", 12f, spec.onPrimary, bold = true))
            row.setOnClickListener {
                Prefs.designId = spec.id
                main.recreate()
            }
            container.addView(row)
        }

        // Colour palettes for the active design. Every design has its
        // own set, and the choice is remembered per design.
        val baseSpec = Themes.byId(t.id)
        val pals = Themes.palettesFor(baseSpec)
        val activePal = Prefs.paletteIndex(t.id)
        val palTitle = Ui.txt(ctx, t, "Colours for this design", 17f, t.text, bold = true)
        palTitle.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 22, 0, 4))
        container.addView(palTitle)
        val palNote = Ui.txt(ctx, t, "Design " + (Themes.ALL.indexOfFirst { it.id == t.id } + 1) + " (" + baseSpec.name + ") in different colours. Each design keeps its own colour.", 12f, t.muted)
        palNote.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 8))
        container.addView(palNote)
        val palRow = LinearLayout(ctx)
        palRow.orientation = LinearLayout.HORIZONTAL
        pals.forEachIndexed { pi, pal ->
            val col = LinearLayout(ctx)
            col.orientation = LinearLayout.VERTICAL
            col.gravity = Gravity.CENTER
            col.layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 72f), Ui.WRAP)
            val dot = View(ctx)
            val gd = android.graphics.drawable.GradientDrawable()
            gd.shape = android.graphics.drawable.GradientDrawable.OVAL
            gd.setColor(pal.primary)
            gd.setStroke(Ui.dp(ctx, if (pi == activePal) 4f else 2f), if (pi == activePal) t.text else pal.accent)
            dot.background = gd
            dot.layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 40f), Ui.dp(ctx, 40f))
            col.addView(dot)
            val nm = Ui.txt(ctx, t, pal.name, 11f, if (pi == activePal) t.text else t.muted, bold = pi == activePal, gravity = Gravity.CENTER)
            nm.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 5, 0, 0))
            col.addView(nm)
            Ui.pressable(col)
            col.setOnClickListener {
                Prefs.setPaletteIndex(t.id, pi)
                main.recreate()
            }
            palRow.addView(col)
        }
        container.addView(palRow)

        // Support + socials from settings.
        val settings = Prefs.settings()
        val supportEmail = settings?.optString("support_email", "") ?: ""
        val linkTitle = Ui.txt(ctx, t, "Support and social", 17f, t.text, bold = true)
        linkTitle.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 22, 0, 8))
        container.addView(linkTitle)
        if (supportEmail.isNotEmpty()) {
            container.addView(linkRow("Email support: $supportEmail") {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail")))
            })
        }
        val socials = listOf(
            "Facebook" to (settings?.optString("social_facebook", "") ?: ""),
            "YouTube" to (settings?.optString("social_youtube", "") ?: ""),
            "Instagram" to (settings?.optString("social_instagram", "") ?: "")
        )
        for ((label, url) in socials) {
            if (url.isNotEmpty()) {
                container.addView(linkRow(label) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
                })
            }
        }
        val privacy = settings?.optString("privacy_url", "") ?: ""
        if (privacy.isNotEmpty()) {
            container.addView(linkRow("Privacy Policy") {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacy))) } catch (e: Exception) { }
            })
        }

        // Account management (signed-in users only).
        if (!isGuest) {
            val accTitle = Ui.txt(ctx, t, "Account", 17f, t.text, bold = true)
            accTitle.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 22, 0, 8))
            container.addView(accTitle)

            fun accInput(hint: String, value: String, password: Boolean = false): EditText {
                val e = EditText(ctx)
                e.hint = hint
                e.setText(value)
                e.setTextColor(t.text)
                e.setHintTextColor(t.muted)
                e.textSize = 15f
                e.setSingleLine(true)
                if (password) e.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                e.background = Ui.cardBg(ctx, t)
                e.setPadding(Ui.dp(ctx, 14f), 0, Ui.dp(ctx, 14f), 0)
                e.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 50f), intArrayOf(0, 0, 0, 10))
                return e
            }
            fun accButton(label: String, primary: Boolean): Button {
                val b = Button(ctx)
                b.text = label
                b.setTextColor(if (primary) t.onPrimary else t.text)
                b.background = if (primary) Ui.pillBg(t.primary, t.radiusDp, ctx) else Ui.cardBg(ctx, t)
                b.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 48f), intArrayOf(0, 2, 0, 14))
                return b
            }

            container.addView(Ui.txt(ctx, t, "Edit profile", 14f, t.text, bold = true))
            val nameEdit = accInput("Full name", user?.optString("name", "") ?: "")
            val phoneEdit = accInput("Phone", user?.optString("phone", "") ?: "")
            container.addView(nameEdit)
            container.addView(phoneEdit)
            val saveProfile = accButton("Save Changes", true)
            saveProfile.setOnClickListener {
                val body = JSONObject()
                    .put("name", nameEdit.text.toString().trim())
                    .put("phone", phoneEdit.text.toString().trim())
                saveProfile.isEnabled = false
                ApiClient.post("profile/update", body, token = Prefs.token,
                    onOk = { updated ->
                        Prefs.userJson = updated.toString()
                        toast("Profile saved.")
                        saveProfile.isEnabled = true
                        build()
                    },
                    onErr = { e -> saveProfile.isEnabled = true; toast(e.message ?: "Could not save.") })
            }
            container.addView(saveProfile)

            container.addView(Ui.txt(ctx, t, "Change password", 14f, t.text, bold = true))
            val curPass = accInput("Current password", "", password = true)
            val newPass = accInput("New password (at least 6 characters)", "", password = true)
            container.addView(curPass)
            container.addView(newPass)
            val savePass = accButton("Change Password", false)
            savePass.setOnClickListener {
                if (newPass.text.toString().length < 6) { toast("New password must be at least 6 characters."); return@setOnClickListener }
                val body = JSONObject()
                    .put("current_password", curPass.text.toString())
                    .put("new_password", newPass.text.toString())
                savePass.isEnabled = false
                ApiClient.post("profile/password", body, token = Prefs.token,
                    onOk = { toast("Password changed."); savePass.isEnabled = true; curPass.text.clear(); newPass.text.clear() },
                    onErr = { e -> savePass.isEnabled = true; toast(e.message ?: "Could not change password.") })
            }
            container.addView(savePass)

            val delBtn = accButton("Delete My Account", false)
            delBtn.setTextColor(android.graphics.Color.rgb(192, 57, 43))
            delBtn.setOnClickListener {
                val pw = EditText(ctx)
                pw.hint = "Your password"
                pw.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                pw.setSingleLine(true)
                AlertDialog.Builder(ctx)
                    .setTitle("Delete account?")
                    .setMessage("This permanently removes your profile, results, saved MCQs and coins. This cannot be undone. Enter your password to confirm.")
                    .setView(pw)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        ApiClient.post("auth/delete", JSONObject().put("password", pw.text.toString()), token = Prefs.token,
                            onOk = {
                                Prefs.clearSession()
                                toast("Your account has been deleted.")
                                startActivity(Intent(ctx, AuthActivity::class.java))
                                main.finish()
                            },
                            onErr = { e -> toast(e.message ?: "Could not delete the account.") })
                    }
                    .show()
            }
            container.addView(delBtn)
        }

        // Logout / Sign in.
        val btn = Button(ctx)
        if (isGuest) {
            btn.text = "Sign In"
            btn.setOnClickListener {
                startActivity(Intent(ctx, AuthActivity::class.java))
                main.finish()
            }
        } else {
            btn.text = "Logout"
            btn.setOnClickListener {
                ApiClient.post("auth/logout", JSONObject(), token = Prefs.token, onOk = { }, onErr = { })
                Prefs.clearSession()
                startActivity(Intent(ctx, AuthActivity::class.java))
                main.finish()
            }
        }
        btn.setTextColor(t.onPrimary)
        btn.background = Ui.pillBg(t.primary, t.radiusDp, ctx)
        btn.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.dp(ctx, 52f), intArrayOf(0, 24, 0, 0))
        container.addView(btn)

        val ver = Ui.txt(ctx, t, "MDCAT Prep, version " + ApiConfig.APP_VERSION, 12f, t.muted, gravity = Gravity.CENTER)
        ver.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 16, 0, 8))
        container.addView(ver)
    }

    private fun toast(msg: String) =
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()

    /** Daily Study Goals: today's progress against the goal, editable. */
    private fun buildGoalCard(box: LinearLayout, isGuest: Boolean) {
        val ctx = requireContext()
        val title = Ui.txt(ctx, t, "Daily Study Goal", 17f, t.text, bold = true)
        title.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 22, 0, 8))
        box.addView(title)
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.cardBg(ctx, t)
        card.setPadding(Ui.dp(ctx, 16f), Ui.dp(ctx, 14f), Ui.dp(ctx, 16f), Ui.dp(ctx, 14f))
        box.addView(card)
        card.addView(Ui.txt(ctx, t, "Loading your goal...", 13f, t.muted))
        ApiClient.get("home", token = Prefs.token,
            onCache = { data -> if (isAdded) fillGoalCard(card, data, isGuest) },
            onOk = { data -> if (isAdded) fillGoalCard(card, data, isGuest) },
            onErr = { if (isAdded) { card.removeAllViews(); card.addView(Ui.txt(ctx, t, "Could not load your goal right now.", 13f, t.muted)) } })
    }

    private fun fillGoalCard(card: LinearLayout, data: JSONObject, isGuest: Boolean) {
        val ctx = requireContext()
        card.removeAllViews()
        var goal = data.optInt("daily_goal", 50)
        val done = data.optInt("daily_completed", 0)
        val pct = if (goal > 0) minOf(100, done * 100 / goal) else 0
        val status = Ui.txt(ctx, t, "$done of $goal questions done today ($pct%)", 14f, t.text, bold = true)
        card.addView(status)
        val bar = Ui.progressBar(ctx, t, pct)
        bar.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 8, 0, 0))
        card.addView(bar)
        if (isGuest) {
            card.addView(Ui.txt(ctx, t, "Sign in to set your own daily goal.", 12f, t.muted))
            return
        }
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 12, 0, 0))
        val goalLabel = Ui.txt(ctx, t, "Goal: $goal", 14f, t.text, bold = true)
        goalLabel.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        row.addView(goalLabel)
        fun stepper(label: String, delta: Int): android.widget.TextView {
            val b = Ui.txt(ctx, t, label, 16f, t.onPrimary, bold = true, gravity = Gravity.CENTER)
            b.background = Ui.pillBg(t.primary, 16f, ctx)
            b.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 6f), Ui.dp(ctx, 14f), Ui.dp(ctx, 6f))
            val lp = LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP)
            lp.marginStart = Ui.dp(ctx, 8f)
            b.layoutParams = lp
            b.setOnClickListener {
                goal = (goal + delta).coerceIn(5, 500)
                goalLabel.text = "Goal: $goal"
                Ui.tapPop(b)
            }
            return b
        }
        row.addView(stepper("-10", -10))
        row.addView(stepper("+10", 10))
        val save = Ui.txt(ctx, t, "Save", 14f, t.primary, bold = true, gravity = Gravity.CENTER)
        val slp = LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP)
        slp.marginStart = Ui.dp(ctx, 12f)
        save.layoutParams = slp
        save.setOnClickListener {
            ApiClient.post("profile/goal", JSONObject().put("goal", goal), token = Prefs.token,
                onOk = {
                    if (!isAdded) return@post
                    android.widget.Toast.makeText(ctx, "Daily goal saved: $goal questions.", android.widget.Toast.LENGTH_SHORT).show()
                    build()
                },
                onErr = { e ->
                    if (!isAdded) return@post
                    android.widget.Toast.makeText(ctx, e.message ?: "Could not save the goal.", android.widget.Toast.LENGTH_LONG).show()
                })
        }
        row.addView(save)
        card.addView(row)
    }

    /** Question of the Day: one practice question, same for everyone today. */
    private fun buildQotdCard(box: LinearLayout) {
        val ctx = requireContext()
        val title = Ui.txt(ctx, t, "Question of the Day", 17f, t.text, bold = true)
        title.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 22, 0, 8))
        box.addView(title)
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.cardBg(ctx, t)
        card.setPadding(Ui.dp(ctx, 16f), Ui.dp(ctx, 14f), Ui.dp(ctx, 16f), Ui.dp(ctx, 14f))
        box.addView(card)
        card.addView(Ui.skeleton(ctx, t, 1))
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        ApiClient.get("qotd", mapOf("d" to today), token = Prefs.token,
            onCache = { data -> if (isAdded) fillQotdCard(card, data) },
            onOk = { data -> if (isAdded) fillQotdCard(card, data) },
            onErr = { if (isAdded) { card.removeAllViews(); card.addView(Ui.txt(ctx, t, "Question of the Day is not available right now.", 13f, t.muted)) } })
    }

    private fun fillQotdCard(card: LinearLayout, data: JSONObject) {
        val ctx = requireContext()
        card.removeAllViews()
        val q = data.optJSONObject("question") ?: return
        val chapter = q.optString("chapter_title", "")
        if (chapter.isNotEmpty()) card.addView(Ui.txt(ctx, t, chapter, 12f, t.primary, bold = true))
        card.addView(Ui.txt(ctx, t, q.optString("question", ""), 15f, t.text, bold = true))
        val correct = q.optString("correct_option", "")
        val expl = q.optString("explanation", "")
        val explView = Ui.txt(ctx, t, expl, 13f, t.muted)
        explView.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 0))
        var answered = false
        val optionRows = mutableListOf<LinearLayout>()
        for (letter in listOf("A", "B", "C", "D")) {
            val text = q.optJSONObject("options")?.optString(letter, "") ?: ""
            if (text.isEmpty()) continue
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.background = Ui.cardBg(ctx, t, fill = t.surface)
            row.setPadding(Ui.dp(ctx, 12f), Ui.dp(ctx, 10f), Ui.dp(ctx, 12f), Ui.dp(ctx, 10f))
            row.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 8, 0, 0))
            row.addView(Ui.txt(ctx, t, letter, 13f, t.primary, bold = true))
            val lbl = Ui.txt(ctx, t, text, 14f, t.text)
            lbl.setPadding(Ui.dp(ctx, 10f), 0, 0, 0)
            row.addView(lbl)
            Ui.pressable(row)
            row.setOnClickListener {
                if (answered) return@setOnClickListener
                answered = true
                for (r in optionRows) {
                    val l = r.tag as String
                    val right = l == correct
                    r.background = Ui.cardBg(ctx, t, fill = when {
                        right -> t.primary
                        r == row -> 0xFFC0392B.toInt()
                        else -> t.surface
                    })
                    if (right || r == row) {
                        for (i in 0 until r.childCount) {
                            (r.getChildAt(i) as? android.widget.TextView)?.setTextColor(t.onPrimary)
                        }
                    }
                }
                Ui.tapPop(row)
                if (expl.isNotEmpty()) card.addView(explView)
            }
            row.tag = letter
            optionRows.add(row)
            card.addView(row)
        }
    }

    private fun attemptRow(a: JSONObject): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.VERTICAL
        row.background = Ui.cardBg(ctx, t)
        row.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
        row.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 8))
        val type = a.optString("quiz_type", "chapter")
        val title = if (type == "mock") "Full Mock Test" else a.optString("chapter_title", "Chapter quiz")
        row.addView(Ui.txt(ctx, t, title, 15f, t.text, bold = true))
        val score = a.optInt("score"); val total = a.optInt("total")
        row.addView(Ui.txt(ctx, t, "Score $score / $total  -  ${a.optInt("correct")} correct, ${a.optInt("incorrect")} wrong, ${a.optInt("skipped")} skipped", 12f, t.muted))
        row.addView(Ui.txt(ctx, t, a.optString("created_at", ""), 11f, t.muted))
        return row
    }

    private fun linkRow(label: String, action: () -> Unit): View {
        val ctx = requireContext()
        val row = Ui.txt(ctx, t, label, 14f, t.primary, bold = false)
        row.background = Ui.cardBg(ctx, t)
        row.setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
        row.layoutParams = Ui.lp(ctx, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 8))
        row.setOnClickListener { action() }
        return row
    }
}

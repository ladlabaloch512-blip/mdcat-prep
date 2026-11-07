package com.abiz.mdcatprep

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * Settings hub, reachable from the gear icons on Home and Profile:
 * sign in / logout without digging, account shortcuts, app version.
 * This answers the client's note that logout was buried in Profile.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var t: ThemeSpec

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        t = Themes.current()

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(t.bg)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 24f))

        val back = Ui.txt(this, t, "< Back", 14f, t.primary, bold = true)
        back.setOnClickListener { finish() }
        root.addView(back)
        val title = Ui.txt(this, t, "Settings", 24f, t.text, bold = true)
        title.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 6))
        root.addView(title)

        val signedIn = Prefs.token != null
        val user = Prefs.user()
        root.addView(Ui.txt(this, t,
            if (signedIn) "Signed in as " + (user?.optString("email", "") ?: "")
            else "You are browsing as a guest.", 13f, t.muted))

        if (!signedIn) {
            val signIn = Button(this)
            signIn.text = "Sign In"
            signIn.setTextColor(t.onPrimary)
            signIn.background = Ui.pillBg(t.primary, t.radiusDp, this)
            signIn.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 16, 0, 0))
            signIn.setOnClickListener {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            root.addView(signIn)
        } else {
            root.addView(row(R.drawable.ic_edit, "Edit Profile") { openMainTab("profile") })
            root.addView(row(R.drawable.ic_chart, "Daily Study Goals") { openMainTab("profile") })
            root.addView(row(R.drawable.ic_lock, "Change Password") { changePassword() })
            root.addView(row(R.drawable.ic_delete, "Delete Account", danger = true) { deleteAccount() })
        }
        root.addView(row(R.drawable.ic_book, "Privacy Policy") {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ApiConfig.BASE_URL + "privacy.php"))) }
            catch (e: Exception) { toast("Could not open the privacy policy.") }
        })

        if (signedIn) {
            val logout = Button(this)
            logout.text = "Log Out"
            logout.setTextColor(0xFFFFFFFF.toInt())
            logout.background = Ui.pillBg(0xFFC0392B.toInt(), t.radiusDp, this)
            logout.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 22, 0, 0))
            logout.setOnClickListener {
                ApiClient.post("auth/logout", JSONObject(), token = Prefs.token, onOk = { }, onErr = { })
                Prefs.clearSession()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            root.addView(logout)
        }

        val ver = Ui.txt(this, t, "MDCAT Prep, version " + ApiConfig.APP_VERSION, 12f, t.muted, gravity = Gravity.CENTER)
        ver.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 20, 0, 0))
        root.addView(ver)

        scroll.addView(root)
        setContentView(scroll)
        Ui.enterChildren(root)
    }

    private fun row(icon: Int, label: String, danger: Boolean = false, action: () -> Unit): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.background = Ui.cardBg(this, t)
        r.setPadding(Ui.dp(this, 14f), Ui.dp(this, 13f), Ui.dp(this, 14f), Ui.dp(this, 13f))
        r.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 0))
        val color = if (danger) 0xFFC0392B.toInt() else t.primary
        r.addView(Ui.icon(this, icon, 21f, color))
        val tv = Ui.txt(this, t, label, 15f, if (danger) color else t.text, bold = true)
        tv.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        tv.setPadding(Ui.dp(this, 12f), 0, 0, 0)
        r.addView(tv)
        r.addView(Ui.txt(this, t, ">", 15f, t.muted, bold = true))
        Ui.pressable(r)
        r.setOnClickListener { action() }
        return r
    }

    private fun openMainTab(tab: String) {
        val i = Intent(this, MainActivity::class.java)
        i.putExtra("tab", tab)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(i)
        finish()
    }

    private fun field(hint: String): EditText {
        val e = EditText(this)
        e.hint = hint
        e.setSingleLine(true)
        e.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        e.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 0, 0, 8))
        return e
    }

    private fun changePassword() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(Ui.dp(this, 20f), Ui.dp(this, 8f), Ui.dp(this, 20f), 0)
        val cur = field("Current password")
        val new = field("New password (at least 6 characters)")
        box.addView(cur)
        box.addView(new)
        AlertDialog.Builder(this)
            .setTitle("Change password")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Change") { _, _ ->
                if (new.text.toString().length < 6) { toast("New password must be at least 6 characters."); return@setPositiveButton }
                val body = JSONObject()
                    .put("current_password", cur.text.toString())
                    .put("new_password", new.text.toString())
                ApiClient.post("profile/password", body, token = Prefs.token,
                    onOk = { toast("Password changed.") },
                    onErr = { e -> toast(e.message ?: "Could not change password.") })
            }
            .show()
    }

    private fun deleteAccount() {
        val pw = field("Your password")
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(Ui.dp(this, 20f), Ui.dp(this, 8f), Ui.dp(this, 20f), 0)
        box.addView(pw)
        AlertDialog.Builder(this)
            .setTitle("Delete account?")
            .setMessage("This permanently removes your profile, results, saved MCQs and coins. This cannot be undone.")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                ApiClient.post("auth/delete", JSONObject().put("password", pw.text.toString()), token = Prefs.token,
                    onOk = {
                        Prefs.clearSession()
                        toast("Your account has been deleted.")
                        startActivity(Intent(this, AuthActivity::class.java))
                        finish()
                    },
                    onErr = { e -> toast(e.message ?: "Could not delete the account.") })
            }
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

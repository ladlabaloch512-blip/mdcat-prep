package com.abiz.mdcatprep

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException as GmsApiException
import org.json.JSONObject

/**
 * Login / Signup with email, Google Sign-In (web client ID read from the
 * settings endpoint at runtime) and Skip for Now (guest mode).
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var t: ThemeSpec
    private var signupMode = false
    private lateinit var nameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var nameRow: View
    private lateinit var phoneRow: View
    private lateinit var titleView: android.widget.TextView
    private lateinit var submitBtn: Button
    private lateinit var toggleView: android.widget.TextView
    private lateinit var termsBox: CheckBox

    companion object { private const val RC_GOOGLE = 9001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        t = Themes.current()

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(t.bg)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(Ui.dp(this, 24f), Ui.dp(this, 40f), Ui.dp(this, 24f), Ui.dp(this, 24f))

        val mark = ImageView(this)
        mark.setImageResource(R.drawable.emblem)
        val markLp = LinearLayout.LayoutParams(Ui.dp(this, 96f), Ui.dp(this, 96f))
        markLp.gravity = Gravity.CENTER_HORIZONTAL
        mark.layoutParams = markLp
        Ui.popIn(mark)
        root.addView(mark)
        // The logo tile already carries the MDCAT Pakistan name.

        titleView = Ui.txt(this, t, "Welcome back", 26f, t.text, bold = true)
        titleView.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 20, 0, 4))
        root.addView(titleView)
        val sub = Ui.txt(this, t, "Sign in to track your progress, save MCQs and take mock tests.", 14f, t.muted)
        sub.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 20))
        root.addView(sub)

        val namePair = iconInput("Full name", R.drawable.ic_person)
        nameInput = namePair.second
        nameRow = namePair.first
        root.addView(namePair.first)
        val emailPair = iconInput("Email address", R.drawable.ic_mail)
        val emailInput = emailPair.second
        emailInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        root.addView(emailPair.first)
        val phonePair = iconInput("Phone (optional)", R.drawable.ic_phone)
        phoneInput = phonePair.second
        phoneInput.inputType = android.text.InputType.TYPE_CLASS_PHONE
        phoneRow = phonePair.first
        root.addView(phonePair.first)
        val passPair = iconInput("Password", R.drawable.ic_lock, isPassword = true)
        val passInput = passPair.second
        root.addView(passPair.first)

        termsBox = CheckBox(this)
        termsBox.text = "I agree to Terms & Conditions"
        termsBox.setTextColor(t.text)
        termsBox.textSize = 13f
        termsBox.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 2, 0, 8))
        root.addView(termsBox)

        submitBtn = Button(this)
        submitBtn.text = "Sign In"
        submitBtn.setTextColor(t.onPrimary)
        submitBtn.background = Ui.pillBg(t.primary, t.radiusDp, this)
        submitBtn.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 10, 0, 0))
        submitBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString()
            if (email.isEmpty() || pass.isEmpty()) {
                toast("Please enter your email and password.")
                return@setOnClickListener
            }
            if (signupMode && !termsBox.isChecked) {
                toast("Please agree to the Terms & Conditions first.")
                return@setOnClickListener
            }
            submitBtn.isEnabled = false
            if (signupMode) {
                val body = JSONObject()
                    .put("name", nameInput.text.toString().trim())
                    .put("email", email)
                    .put("phone", phoneInput.text.toString().trim())
                    .put("password", pass)
                ApiClient.post("auth/register", body, onOk = { d -> onAuthSuccess(d) },
                    onErr = { e -> submitBtn.isEnabled = true; toast(e.message ?: "Sign up failed.") })
            } else {
                val body = JSONObject().put("email", email).put("password", pass)
                ApiClient.post("auth/login", body, onOk = { d -> onAuthSuccess(d) },
                    onErr = { e -> submitBtn.isEnabled = true; toast(e.message ?: "Sign in failed.") })
            }
        }
        root.addView(submitBtn)

        val googleBtn = Button(this)
        googleBtn.text = "Continue with Google"
        googleBtn.setTextColor(t.text)
        googleBtn.background = Ui.cardBg(this, t)
        googleBtn.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 12, 0, 0))
        googleBtn.setOnClickListener { startGoogle() }
        root.addView(googleBtn)

        toggleView = Ui.txt(this, t, "New here? Create an account", 14f, t.primary, bold = true, gravity = Gravity.CENTER)
        toggleView.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 18, 0, 0))
        toggleView.setOnClickListener { setMode(!signupMode) }
        root.addView(toggleView)

        val forgot = Ui.txt(this, t, "Forgot password?", 14f, t.primary, gravity = Gravity.CENTER)
        forgot.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        root.addView(forgot)

        val resetBox = LinearLayout(this)
        resetBox.orientation = LinearLayout.VERTICAL
        resetBox.visibility = View.GONE
        resetBox.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 6, 0, 0))
        val resetNote = Ui.txt(this, t, "We will email you a 6-digit reset code. It stays valid for 15 minutes.", 13f, t.muted)
        resetBox.addView(resetNote)
        val sendCodeBtn = Button(this)
        sendCodeBtn.text = "Send Reset Code"
        sendCodeBtn.setTextColor(t.text)
        sendCodeBtn.background = Ui.cardBg(this, t)
        sendCodeBtn.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 48f), intArrayOf(0, 8, 0, 4))
        sendCodeBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) { toast("Enter your email above first."); return@setOnClickListener }
            sendCodeBtn.isEnabled = false
            ApiClient.post("auth/forgot", JSONObject().put("email", email),
                onOk = { d -> sendCodeBtn.isEnabled = true; toast(d.optString("message", "If this email is registered, a reset code has been sent.")) },
                onErr = { e -> sendCodeBtn.isEnabled = true; toast(e.message ?: "Could not send the code.") })
        }
        resetBox.addView(sendCodeBtn)
        val codeInput = input("Reset code")
        codeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        resetBox.addView(codeInput)
        val newPassInput = input("New password")
        newPassInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        resetBox.addView(newPassInput)
        val doResetBtn = Button(this)
        doResetBtn.text = "Set New Password"
        doResetBtn.setTextColor(t.onPrimary)
        doResetBtn.background = Ui.pillBg(t.primary, t.radiusDp, this)
        doResetBtn.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 48f), intArrayOf(0, 4, 0, 0))
        doResetBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val code = codeInput.text.toString().trim()
            val np = newPassInput.text.toString()
            if (email.isEmpty() || code.isEmpty() || np.length < 6) {
                toast("Enter your email, the reset code and a new password of at least 6 characters.")
                return@setOnClickListener
            }
            doResetBtn.isEnabled = false
            val body = JSONObject().put("email", email).put("code", code).put("password", np)
            ApiClient.post("auth/reset", body,
                onOk = { toast("Password reset. Please sign in with your new password."); resetBox.visibility = View.GONE; doResetBtn.isEnabled = true },
                onErr = { e -> doResetBtn.isEnabled = true; toast(e.message ?: "Reset failed.") })
        }
        resetBox.addView(doResetBtn)
        root.addView(resetBox)
        forgot.setOnClickListener {
            resetBox.visibility = if (resetBox.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val skip = Ui.txt(this, t, "Skip for Now", 14f, t.muted, gravity = Gravity.CENTER)
        skip.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 14, 0, 0))
        skip.setOnClickListener {
            Prefs.guest = true
            goMain()
        }
        root.addView(skip)

        scroll.addView(root)
        setContentView(scroll)
        setMode(false)
    }

    /** Reference-style field: leading icon inside the field, eye toggle on passwords. */
    private fun iconInput(hint: String, iconRes: Int, isPassword: Boolean = false): Pair<LinearLayout, EditText> {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = Ui.cardBg(this, t)
        row.setPadding(Ui.dp(this, 14f), 0, Ui.dp(this, 10f), 0)
        row.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 54f), intArrayOf(0, 0, 0, 12))
        row.addView(Ui.icon(this, iconRes, 20f, t.muted))
        val e = EditText(this)
        e.hint = hint
        e.setTextColor(t.text)
        e.setHintTextColor(t.muted)
        e.textSize = 15f
        e.setSingleLine(true)
        e.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        e.setPadding(Ui.dp(this, 10f), 0, 0, 0)
        e.layoutParams = LinearLayout.LayoutParams(0, Ui.MATCH, 1f)
        row.addView(e)
        if (isPassword) {
            e.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            val eye = Ui.icon(this, R.drawable.ic_eye, 21f, t.muted)
            var shown = false
            eye.setOnClickListener {
                shown = !shown
                e.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    if (shown) android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    else android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                e.setSelection(e.text.length)
            }
            row.addView(eye)
        }
        return row to e
    }

    private fun input(hint: String): EditText {
        val e = EditText(this)
        e.hint = hint
        e.setTextColor(t.text)
        e.setHintTextColor(t.muted)
        e.textSize = 15f
        e.setSingleLine(true)
        if (t.id == "focus" || t.id == "editorial") {
            // Underline-style inputs for the list/editorial designs.
            e.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            e.setPadding(0, Ui.dp(this, 10f), 0, Ui.dp(this, 10f))
        } else {
            e.background = Ui.cardBg(this, t)
            e.setPadding(Ui.dp(this, 14f), 0, Ui.dp(this, 14f), 0)
        }
        e.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 54f), intArrayOf(0, 0, 0, 12))
        return e
    }

    private fun setMode(signup: Boolean) {
        signupMode = signup
        nameRow.visibility = if (signup) View.VISIBLE else View.GONE
        phoneRow.visibility = if (signup) View.VISIBLE else View.GONE
        titleView.text = if (signup) "Create your account" else "Welcome back"
        submitBtn.text = if (signup) "Sign Up" else "Sign In"
        toggleView.text = if (signup) "Already have an account? Sign in" else "New here? Create an account"
    }

    private fun startGoogle() {
        val clientId = Prefs.settings()?.optString("google_client_id", "") ?: ""
        if (clientId.isEmpty()) {
            toast("Google sign-in is not configured yet.")
            return
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        startActivityForResult(client.signInIntent, RC_GOOGLE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RC_GOOGLE) return
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(GmsApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrEmpty()) {
                toast("Google sign-in did not return a token.")
                return
            }
            val body = JSONObject().put("id_token", idToken)
            ApiClient.post("auth/google", body, onOk = { d -> onAuthSuccess(d) },
                onErr = { e -> toast(e.message ?: "Google sign-in failed.") })
        } catch (e: Exception) {
            if (resultCode != Activity.RESULT_CANCELED) toast("Google sign-in was cancelled or failed.")
        }
    }

    private fun onAuthSuccess(data: JSONObject) {
        val token = data.optString("token", "")
        val user = data.optJSONObject("user")
        if (token.isEmpty() || user == null) {
            toast("Unexpected response from the server.")
            return
        }
        Prefs.token = token
        Prefs.userJson = user.toString()
        Prefs.guest = false
        goMain()
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

package com.abiz.mdcatprep

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * Daily Reward screen: coin balance, claim streak and the once-a-day
 * claim button. All amounts come from the server settings, nothing is
 * hardcoded here.
 */
class RewardsActivity : AppCompatActivity() {

    private lateinit var t: ThemeSpec
    private lateinit var container: LinearLayout
    private lateinit var balanceView: TextView
    private lateinit var claimBtn: Button
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        t = Themes.current()

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(t.bg)
        container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 18f), Ui.dp(this, 24f))
        val back = Ui.txt(this, t, "< Back", 14f, t.primary, bold = true)
        back.setOnClickListener { finish() }
        container.addView(back)
        val title = Ui.txt(this, t, "Daily Reward", 24f, t.text, bold = true)
        title.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 6))
        container.addView(title)
        container.addView(Ui.txt(this, t, "Come back every day, claim your coins and keep your streak alive.", 13f, t.muted))
        scroll.addView(container)
        setContentView(scroll)
        buildCard()
        load()
    }

    private fun buildCard() {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.background = Ui.cardBg(this, t, fill = t.primary)
        card.setPadding(Ui.dp(this, 18f), Ui.dp(this, 22f), Ui.dp(this, 18f), Ui.dp(this, 22f))
        card.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 16, 0, 0))
        card.addView(Ui.txt(this, t, "YOUR COINS", 12f, 0xFFD8F3E3.toInt(), bold = true, gravity = Gravity.CENTER))
        balanceView = Ui.txt(this, t, "0", 48f, t.onPrimary, bold = true, gravity = Gravity.CENTER)
        card.addView(balanceView)
        statusView = Ui.txt(this, t, "Loading your reward...", 13f, 0xFFD8F3E3.toInt(), gravity = Gravity.CENTER)
        card.addView(statusView)
        container.addView(card)

        claimBtn = Button(this)
        claimBtn.text = "Claim today's reward"
        claimBtn.setTextColor(t.onPrimary)
        claimBtn.background = Ui.pillBg(t.accent, t.radiusDp, this)
        claimBtn.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 16, 0, 0))
        claimBtn.isEnabled = false
        Ui.pressable(claimBtn)
        claimBtn.setOnClickListener { claim() }
        container.addView(claimBtn)
        Ui.enterChildren(container, 2)
    }

    private fun load() {
        if (Prefs.token.isNullOrEmpty()) {
            statusView.text = "Sign in to claim your daily reward."
            claimBtn.text = "Sign in first"
            return
        }
        ApiClient.get("rewards", token = Prefs.token,
            onOk = { data -> showState(data) },
            onErr = { e ->
                statusView.text = e.message ?: "Could not load rewards."
                claimBtn.text = "Tap to retry"
                claimBtn.isEnabled = true
                claimBtn.setOnClickListener { load() }
            })
    }

    private fun showState(data: JSONObject) {
        val coins = data.optInt("coins")
        Ui.countUp(balanceView, coins, durationMs = 800L)
        val amount = data.optInt("daily_reward_coins")
        val streak = data.optInt("streak_days")
        val total = data.optInt("total_claims")
        if (!data.optBoolean("rewards_enabled", true)) {
            statusView.text = "Daily rewards are paused right now."
            claimBtn.text = "Rewards paused"
            claimBtn.isEnabled = false
            return
        }
        if (data.optBoolean("claimed_today")) {
            statusView.text = "Claimed today. Streak: $streak day(s) - Total claims: $total"
            claimBtn.text = "Come back tomorrow"
            claimBtn.isEnabled = false
        } else {
            statusView.text = "Today's reward: $amount coins - Streak: $streak day(s)"
            claimBtn.text = "Claim $amount coins"
            claimBtn.isEnabled = true
        }
    }

    private fun claim() {
        claimBtn.isEnabled = false
        claimBtn.text = "Claiming..."
        ApiClient.post("rewards/claim", JSONObject(), token = Prefs.token,
            onOk = { data ->
                val coins = data.optInt("coins")
                Ui.countUp(balanceView, coins, durationMs = 900L)
                Ui.tapPop(balanceView)
                statusView.text = "You got ${data.optInt("added")} coins. Streak: ${data.optInt("streak_days")} day(s)"
                claimBtn.text = "Come back tomorrow"
                Toast.makeText(this, "Reward claimed.", Toast.LENGTH_SHORT).show()
            },
            onErr = { e ->
                Toast.makeText(this, e.message ?: "Could not claim the reward.", Toast.LENGTH_LONG).show()
                load()
            })
    }
}

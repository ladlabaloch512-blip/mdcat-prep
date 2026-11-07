package com.abiz.mdcatprep

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * WhatsApp Channel screen: explains the channel and opens the join link
 * set by the admin in Settings (whatsapp_channel_url). This is a public
 * channel join link only; it is not a chat or support feature.
 */
class ChannelActivity : AppCompatActivity() {

    private lateinit var t: ThemeSpec
    private lateinit var container: LinearLayout

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
        val title = Ui.txt(this, t, "WhatsApp Channel", 24f, t.text, bold = true)
        title.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 12))
        container.addView(title)
        scroll.addView(container)
        setContentView(scroll)
        load()
    }

    private fun load() {
        val loading = Ui.skeleton(this, t, 2)
        container.addView(loading)
        ApiClient.get("settings", token = Prefs.token,
            onCache = { data ->
                container.removeView(loading)
                render(data.optString("whatsapp_channel_url", ""))
            },
            onOk = { data ->
                container.removeView(loading)
                render(data.optString("whatsapp_channel_url", ""))
            },
            onErr = {
                container.removeView(loading)
                render("")
            })
    }

    private fun render(url: String) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = Ui.cardBg(this, t, fill = 0xFF128C4B.toInt())
        card.setPadding(Ui.dp(this, 18f), Ui.dp(this, 20f), Ui.dp(this, 18f), Ui.dp(this, 20f))
        card.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 4, 0, 0))
        card.addView(Ui.txt(this, t, "Join our WhatsApp Channel", 18f, android.graphics.Color.WHITE, bold = true))
        card.addView(Ui.txt(this, t, "Daily practice MCQs, important dates and preparation tips, straight on WhatsApp. One-way updates from our team, no group noise.", 13f, 0xFFD9F2E2.toInt()))
        container.addView(card)

        val join = Button(this)
        join.setTextColor(android.graphics.Color.WHITE)
        join.background = Ui.pillBg(0xFF128C4B.toInt(), t.radiusDp, this)
        join.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 52f), intArrayOf(0, 16, 0, 0))
        Ui.pressable(join)
        if (url.isEmpty()) {
            join.text = "Channel link coming soon"
            join.isEnabled = false
            container.addView(join)
            val note = Ui.txt(this, t, "The join link has not been added yet. Please check again soon.", 13f, t.muted, gravity = Gravity.CENTER)
            note.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 0))
            container.addView(note)
        } else {
            join.text = "Join the channel"
            join.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open the channel link.", Toast.LENGTH_SHORT).show()
                }
            }
            container.addView(join)
        }
        Ui.enterChildren(container, 2)
    }
}

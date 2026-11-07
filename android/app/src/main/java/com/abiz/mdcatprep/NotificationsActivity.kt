package com.abiz.mdcatprep

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Button
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

/** Notifications list from GET notifications, with an empty state. */
class NotificationsActivity : AppCompatActivity() {

    private lateinit var t: ThemeSpec
    private lateinit var container: LinearLayout
    private lateinit var listBox: LinearLayout

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
        val title = Ui.txt(this, t, "Notifications", 24f, t.text, bold = true)
        title.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 10, 0, 12))
        container.addView(title)
        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        container.addView(listBox)
        if (Prefs.token != null) {
            val markBtn = Button(this)
            markBtn.text = "Mark All as Read"
            markBtn.setTextColor(t.onPrimary)
            markBtn.background = Ui.pillBg(t.primary, t.radiusDp, this)
            markBtn.layoutParams = Ui.lp(this, Ui.MATCH, Ui.dp(this, 50f), intArrayOf(0, 14, 0, 0))
            markBtn.setOnClickListener {
                markBtn.isEnabled = false
                ApiClient.post("notifications/read-all", org.json.JSONObject(), token = Prefs.token,
                    onOk = { toast("All notifications marked as read."); markBtn.isEnabled = true; load() },
                    onErr = { e -> markBtn.isEnabled = true; toast(e.message ?: "Could not mark as read.") })
            }
            container.addView(markBtn)
        }
        scroll.addView(container)
        setContentView(scroll)
        Ui.pullToRefresh(scroll) { load() }
        load()
    }

    private fun toast(msg: String) = android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()

    private fun load() {
        listBox.removeAllViews()
        val loading = Ui.skeleton(this, t, 3)
        listBox.addView(loading)
        ApiClient.get("notifications", token = Prefs.token,
            onCache = { data ->
                android.widget.Toast.makeText(this, "No internet. Showing your saved data.", android.widget.Toast.LENGTH_LONG).show()
                listBox.removeView(loading)
                val items = data.optJSONArray("items").toList()
                if (items.isEmpty()) {
                    listBox.addView(Ui.txt(this, t, "No notifications yet.", 14f, t.muted, gravity = Gravity.CENTER))
                } else {
                    for (n in items) {
                        val card = LinearLayout(this)
                        card.orientation = LinearLayout.VERTICAL
                        card.background = Ui.cardBg(this, t)
                        card.setPadding(Ui.dp(this, 16f), Ui.dp(this, 14f), Ui.dp(this, 16f), Ui.dp(this, 14f))
                        card.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 10))
                        val titleRow = LinearLayout(this)
                        titleRow.orientation = LinearLayout.HORIZONTAL
                        titleRow.gravity = Gravity.CENTER_VERTICAL
                        val nTitle = Ui.txt(this, t, n.optString("title", ""), 15f, t.text, bold = true)
                        nTitle.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
                        titleRow.addView(nTitle)
                        if (!n.optBoolean("is_read", true)) {
                            val nw = Ui.txt(this, t, "NEW", 10f, Color.WHITE, bold = true, gravity = Gravity.CENTER)
                            nw.background = Ui.pillBg(Color.parseColor("#D32F2F"), 8f, this)
                            nw.setPadding(Ui.dp(this, 7f), Ui.dp(this, 3f), Ui.dp(this, 7f), Ui.dp(this, 3f))
                            titleRow.addView(nw)
                        }
                        card.addView(titleRow)
                        card.addView(Ui.txt(this, t, n.optString("body", ""), 13f, t.muted))
                        card.addView(Ui.txt(this, t, n.optString("created_at", ""), 11f, t.muted))
                        listBox.addView(card)
                    }
                }
                Ui.enterChildren(listBox)
            },
            onOk = { data ->
                listBox.removeView(loading)
                val items = data.optJSONArray("items").toList()
                if (items.isEmpty()) {
                    listBox.addView(Ui.txt(this, t, "No notifications yet.", 14f, t.muted, gravity = Gravity.CENTER))
                } else {
                    for (n in items) {
                        val card = LinearLayout(this)
                        card.orientation = LinearLayout.VERTICAL
                        card.background = Ui.cardBg(this, t)
                        card.setPadding(Ui.dp(this, 16f), Ui.dp(this, 14f), Ui.dp(this, 16f), Ui.dp(this, 14f))
                        card.layoutParams = Ui.lp(this, Ui.MATCH, Ui.WRAP, intArrayOf(0, 0, 0, 10))
                        val titleRow = LinearLayout(this)
                        titleRow.orientation = LinearLayout.HORIZONTAL
                        titleRow.gravity = Gravity.CENTER_VERTICAL
                        val nTitle = Ui.txt(this, t, n.optString("title", ""), 15f, t.text, bold = true)
                        nTitle.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
                        titleRow.addView(nTitle)
                        if (!n.optBoolean("is_read", true)) {
                            val nw = Ui.txt(this, t, "NEW", 10f, Color.WHITE, bold = true, gravity = Gravity.CENTER)
                            nw.background = Ui.pillBg(Color.parseColor("#D32F2F"), 8f, this)
                            nw.setPadding(Ui.dp(this, 7f), Ui.dp(this, 3f), Ui.dp(this, 7f), Ui.dp(this, 3f))
                            titleRow.addView(nw)
                        }
                        card.addView(titleRow)
                        card.addView(Ui.txt(this, t, n.optString("body", ""), 13f, t.muted))
                        card.addView(Ui.txt(this, t, n.optString("created_at", ""), 11f, t.muted))
                        listBox.addView(card)
                    }
                }
                Ui.enterChildren(listBox)
            },
            onErr = { e ->
                listBox.removeView(loading)
                val err = Ui.txt(this, t, (e.message ?: "Could not load notifications.") + "\nTap to retry.", 14f, t.muted, gravity = Gravity.CENTER)
                err.setOnClickListener { recreate() }
                listBox.addView(err)
            })
    }
}

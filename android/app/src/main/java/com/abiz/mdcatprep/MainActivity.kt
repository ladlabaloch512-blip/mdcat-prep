package com.abiz.mdcatprep

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * Hosts the four tabs (Home / Chapters / Saved / Profile) above the themed
 * bottom navigation. Design switching recreates this activity so every
 * screen re-renders in the newly picked design.
 */
class MainActivity : AppCompatActivity() {

    lateinit var theme: ThemeSpec
    private lateinit var content: FrameLayout
    private var currentTab: String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        theme = Themes.current()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(theme.bg)

        content = FrameLayout(this)
        content.layoutParams = LinearLayout.LayoutParams(Ui.MATCH, 0, 1f)
        // Stable id: with a generated id the system cannot restore the
        // tab fragment after a recreate (design switch), which left the
        // content area blank until a tab was tapped again.
        content.id = 424242
        root.addView(content)
        currentTab = savedInstanceState?.getString("tab") ?: "home"

        val nav = Components.bottomNav(this, theme, currentTab,
            onSelect = { key -> selectTab(key) },
            onMock = { startMock() })
        root.addView(nav)
        setContentView(root)

        // After a recreate the previous tab fragment is not restored
        // (tabs are created in code), so attach one explicitly; without
        // this the first screen stays blank until a tab is tapped.
        if (supportFragmentManager.findFragmentById(content.id) == null) {
            selectTab(currentTab)
            intent?.getStringExtra("tab")?.let { selectTab(it) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("tab", currentTab)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("tab")?.let { selectTab(it) }
    }

    fun selectTab(key: String) {
        currentTab = key
        val fragment: Fragment = when (key) {
            "chapters" -> SubjectsFragment()
            "analytics" -> AnalyticsFragment()
            "saved" -> SavedFragment()
            "profile" -> ProfileFragment()
            else -> HomeFragment()
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .replace(content.id, fragment).commit()
        // Rebuild the nav so the active state follows the tab.
        val root = content.parent as LinearLayout
        val oldNav = root.getChildAt(1)
        val newNav = Components.bottomNav(this, theme, currentTab,
            onSelect = { k -> selectTab(k) }, onMock = { startMock() })
        root.removeView(oldNav)
        root.addView(newNav)
    }

    fun startMock() {
        val intent = Intent(this, QuizActivity::class.java)
        intent.putExtra("type", "mock")
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    fun openChapters(subject: Subject) {
        val intent = Intent(this, ChaptersActivity::class.java)
        intent.putExtra("subject_id", subject.id)
        intent.putExtra("subject_name", subject.name)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}

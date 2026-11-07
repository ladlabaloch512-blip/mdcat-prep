package com.abiz.mdcatprep

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Reusable themed components: banner strip + the themed bottom navigation. */
object Components {

    /**
     * Banner strip for one slot. Collapses completely when the slot is empty.
     * Multiple banners are shown as a horizontal snap row (each full width).
     */
    fun bannerStrip(ctx: Context, t: ThemeSpec, banners: List<Banner>): View {
        val container = LinearLayout(ctx)
        container.orientation = LinearLayout.VERTICAL
        if (banners.isEmpty()) {
            container.visibility = View.GONE
            return container
        }
        val scroll = android.widget.HorizontalScrollView(ctx)
        scroll.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        for (b in banners) {
            val card = FrameLayout(ctx)
            val lp = LinearLayout.LayoutParams(Ui.dp(ctx, 300f), Ui.dp(ctx, 120f))
            lp.marginEnd = Ui.dp(ctx, 10f)
            card.layoutParams = lp
            card.background = Ui.cardBg(ctx, t, fill = t.primary)
            val img = ImageView(ctx)
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            img.layoutParams = FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH)
            card.addView(img)
            val label = Ui.txt(ctx, t, b.title, 14f, Color.WHITE, bold = true)
            val llp = FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP)
            llp.gravity = Gravity.BOTTOM or Gravity.START
            llp.setMargins(Ui.dp(ctx, 12f), 0, Ui.dp(ctx, 12f), Ui.dp(ctx, 10f))
            label.layoutParams = llp
            if (b.imageUrl.isEmpty() || !b.imageUrl.startsWith("http")) {
                // Text banner: keep the title visible on the themed fill.
                card.addView(label)
            } else {
                ApiClient.loadBitmap(b.imageUrl, { bmp -> img.setImageBitmap(bmp) }, { card.addView(label) })
            }
            if (b.targetLink.isNotEmpty()) {
                Ui.pressable(card)
                card.setOnClickListener {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.targetLink)))
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Could not open the link.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            row.addView(card)
        }
        scroll.addView(row)
        container.addView(scroll)
        if (banners.size > 1) {
            // Auto-sliding carousel: advance one banner every 4 seconds,
            // wrapping back to the first. Runs only while on screen.
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var idx = 0
            val stepPx = Ui.dp(ctx, 310f)
            val advance = object : Runnable {
                override fun run() {
                    idx = (idx + 1) % banners.size
                    scroll.smoothScrollTo(idx * stepPx, 0)
                    handler.postDelayed(this, 4000L)
                }
            }
            container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    handler.removeCallbacks(advance)
                    handler.postDelayed(advance, 4000L)
                }
                override fun onViewDetachedFromWindow(v: View) {
                    handler.removeCallbacks(advance)
                }
            })
        }
        return container
    }

    data class NavItem(val key: String, val label: String)

    val NAV_ITEMS = listOf(
        NavItem("home", "Home"),
        NavItem("chapters", "Chapters"),
        NavItem("analytics", "Analytics"),
        NavItem("saved", "Saved"),
        NavItem("profile", "Profile")
    )

    /**
     * Builds the bottom navigation in the active design's nav style.
     * [onSelect] fires with the item key; [onMock] fires for the raised
     * centre button in the floating-dock design (Dark Mode Focus), which is
     * a shortcut to the full mock test and not a fifth tab: the four tabs
     * stay exactly Home / Chapters / Saved / Profile.
     */
    fun bottomNav(
        ctx: Context, t: ThemeSpec, selected: String,
        onSelect: (String) -> Unit, onMock: () -> Unit
    ): View {
        val outer = FrameLayout(ctx)
        val bar = LinearLayout(ctx)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER

        when (t.nav) {
            NavStyle.FLOATING_PILL, NavStyle.FLOATING_CARD -> {
                val lp = FrameLayout.LayoutParams(Ui.MATCH, Ui.dp(ctx, 64f))
                lp.setMargins(Ui.dp(ctx, 16f), Ui.dp(ctx, 8f), Ui.dp(ctx, 16f), Ui.dp(ctx, 14f))
                lp.gravity = Gravity.BOTTOM
                bar.layoutParams = lp
                bar.background = Ui.pillBg(if (t.nav == NavStyle.FLOATING_PILL) Color.parseColor("#141414") else Color.WHITE, 32f, ctx)
                bar.elevation = Ui.dp(ctx, 8f).toFloat()
            }
            NavStyle.FLOATING_DOCK -> {
                val lp = FrameLayout.LayoutParams(Ui.MATCH, Ui.dp(ctx, 68f))
                lp.setMargins(Ui.dp(ctx, 14f), Ui.dp(ctx, 10f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f))
                lp.gravity = Gravity.BOTTOM
                bar.layoutParams = lp
                bar.background = Ui.pillBg(t.surface, 24f, ctx)
                bar.elevation = Ui.dp(ctx, 8f).toFloat()
            }
            NavStyle.SLANTED_BAR -> {
                bar.layoutParams = FrameLayout.LayoutParams(Ui.MATCH, Ui.dp(ctx, 62f)).apply { gravity = Gravity.BOTTOM }
                bar.background = SlantedBarDrawable(Color.parseColor("#101613"))
            }
            NavStyle.POSTER_BAR -> {
                bar.layoutParams = FrameLayout.LayoutParams(Ui.MATCH, Ui.dp(ctx, 60f)).apply { gravity = Gravity.BOTTOM }
                bar.background = Ui.pillBg(Color.BLACK, 0f, ctx)
            }
            NavStyle.TEXT_MINIMAL -> {
                bar.layoutParams = FrameLayout.LayoutParams(Ui.MATCH, Ui.dp(ctx, 56f)).apply { gravity = Gravity.BOTTOM }
                bar.background = Ui.pillBg(t.bg, 0f, ctx)
                bar.setPadding(0, Ui.dp(ctx, 6f), 0, 0)
            }
            NavStyle.FILLED_BAR -> {
                bar.layoutParams = FrameLayout.LayoutParams(Ui.MATCH, Ui.dp(ctx, 60f)).apply { gravity = Gravity.BOTTOM }
                bar.background = Ui.pillBg(t.primary, 0f, ctx)
            }
        }

        val darkBar = t.nav == NavStyle.FLOATING_PILL || t.nav == NavStyle.SLANTED_BAR ||
            t.nav == NavStyle.POSTER_BAR || t.nav == NavStyle.FILLED_BAR
        val idleColor = when {
            t.nav == NavStyle.FLOATING_CARD -> t.muted
            t.nav == NavStyle.FLOATING_DOCK -> t.muted
            darkBar -> 0xFFBFD8CC.toInt()
            else -> t.muted
        }
        val activeColor = when {
            t.nav == NavStyle.POSTER_BAR -> t.primary
            t.nav == NavStyle.FLOATING_PILL -> Color.WHITE
            darkBar -> Color.WHITE
            else -> t.primary
        }

        fun itemView(item: NavItem): TextView {
            val tv = Ui.txt(ctx, t, item.label, 12f, if (item.key == selected) activeColor else idleColor, bold = item.key == selected, gravity = Gravity.CENTER)
            val lp = LinearLayout.LayoutParams(0, Ui.MATCH, 1f)
            tv.layoutParams = lp
            tv.setOnClickListener { Ui.tapPop(tv); onSelect(item.key) }
            Ui.pressable(tv)
            if (item.key == selected) {
                tv.post { Ui.tapPop(tv) }
                when (t.nav) {
                    NavStyle.POSTER_BAR -> tv.background = Ui.pillBg(t.primary, 0f, ctx).apply { setColor(t.primary) }
                    NavStyle.FILLED_BAR -> if (t.id == "split") tv.background = Ui.pillBg(0xFF083D27.toInt(), 0f, ctx)
                    else -> {}
                }
                if (t.nav == NavStyle.POSTER_BAR) tv.setTextColor(Color.WHITE)
            }
            return tv
        }

        if (t.nav == NavStyle.FLOATING_DOCK) {
            bar.addView(itemView(NAV_ITEMS[0]))
            bar.addView(itemView(NAV_ITEMS[1]))
            val centre = FrameLayout(ctx)
            centre.layoutParams = LinearLayout.LayoutParams(0, Ui.MATCH, 1f)
            val btn = TextView(ctx)
            btn.text = "MOCK"
            btn.gravity = Gravity.CENTER
            btn.setTextColor(0xFF06231A.toInt())
            btn.textSize = 11f
            btn.setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            val blp = FrameLayout.LayoutParams(Ui.dp(ctx, 56f), Ui.dp(ctx, 56f))
            blp.gravity = Gravity.CENTER
            blp.topMargin = -Ui.dp(ctx, 16f)
            btn.layoutParams = blp
            val circle = android.graphics.drawable.GradientDrawable()
            circle.shape = android.graphics.drawable.GradientDrawable.OVAL
            circle.setColor(t.primary)
            btn.background = circle
            btn.elevation = Ui.dp(ctx, 6f).toFloat()
            btn.setOnClickListener { onMock() }
            Ui.pressable(btn)
            centre.addView(btn)
            bar.addView(centre)
            bar.addView(itemView(NAV_ITEMS[2]))
            bar.addView(itemView(NAV_ITEMS[3]))
            bar.addView(itemView(NAV_ITEMS[4]))
        } else {
            NAV_ITEMS.forEach { bar.addView(itemView(it)) }
        }
        outer.addView(bar)
        return outer
    }
}

/** Slanted top edge drawable for the Diagonal Energy bottom bar. */
class SlantedBarDrawable(private val color: Int) : android.graphics.drawable.Drawable() {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: android.graphics.Canvas) {
        paint.color = color
        paint.style = android.graphics.Paint.Style.FILL
        val b = bounds
        val slant = b.height() * 0.28f
        val path = android.graphics.Path()
        path.moveTo(b.left.toFloat(), b.top + slant)
        path.lineTo(b.right.toFloat(), b.top.toFloat())
        path.lineTo(b.right.toFloat(), b.bottom.toFloat())
        path.lineTo(b.left.toFloat(), b.bottom.toFloat())
        path.close()
        canvas.drawPath(path, paint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(cf: android.graphics.ColorFilter?) { paint.colorFilter = cf }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.OPAQUE
}

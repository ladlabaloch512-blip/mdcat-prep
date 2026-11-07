package com.abiz.mdcatprep

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Shared view-building helpers that render through the active ThemeSpec. */
object Ui {
    fun dp(ctx: Context, v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.resources.displayMetrics).toInt()

    fun lp(ctx: Context, w: Int, h: Int, marginsDp: IntArray? = null): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        if (marginsDp != null) {
            p.setMargins(dp(ctx, marginsDp[0].toFloat()), dp(ctx, marginsDp[1].toFloat()), dp(ctx, marginsDp[2].toFloat()), dp(ctx, marginsDp[3].toFloat()))
        }
        return p
    }


    /**
     * Pull-to-refresh for the main scroll screens: pulling down past
     * the threshold while already at the top triggers [onRefresh].
     * The touch is never consumed, so normal scrolling is untouched.
     */
    /** Tinted vector icon view used across screens. */
    fun icon(ctx: Context, res: Int, sizeDp: Float, tint: Int): ImageView {
        val iv = ImageView(ctx)
        iv.setImageResource(res)
        iv.setColorFilter(tint)
        iv.layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp), dp(ctx, sizeDp))
        return iv
    }

    /** Subject icon by title: Biology leaf, Chemistry flask, Physics atom, else book. */
    fun subjectIcon(title: String): Int = when {
        title.contains("Bio", ignoreCase = true) -> R.drawable.ic_leaf
        title.contains("Chem", ignoreCase = true) -> R.drawable.ic_flask
        title.contains("Phys", ignoreCase = true) -> R.drawable.ic_atom
        else -> R.drawable.ic_book
    }

    fun pullToRefresh(scroll: ScrollView, onRefresh: () -> Unit) {
        var startY = 0f
        var armed = false
        scroll.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = ev.rawY
                    armed = scroll.scrollY == 0
                }
                MotionEvent.ACTION_MOVE -> {
                    if (armed && scroll.scrollY == 0 && ev.rawY - startY > dp(v.context, 90f)) {
                        armed = false
                        tapPop(v)
                        onRefresh()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> armed = false
            }
            false
        }
    }
    const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    /** Card background honouring the theme's shape language. */
    fun cardBg(ctx: Context, t: ThemeSpec, fill: Int = t.surface, strokeColor: Int? = null): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(fill)
        when (t.shape) {
            CardShape.SQUARE -> d.cornerRadius = 0f
            CardShape.HAIRLINE -> {
                d.cornerRadius = dp(ctx, t.radiusDp).toFloat()
                d.setStroke(dp(ctx, 0.7f).coerceAtLeast(1), strokeColor ?: if (t.dark) 0xFF2A332D.toInt() else 0xFFE2E2DC.toInt())
            }
            else -> d.cornerRadius = dp(ctx, t.radiusDp).toFloat()
        }
        if (t.id == "poster") d.setStroke(dp(ctx, 2.5f), Color.BLACK)
        return d
    }

    fun pillBg(color: Int, radiusDp: Float, ctx: Context): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(ctx, radiusDp).toFloat()
        return d
    }

    fun txt(
        ctx: Context, t: ThemeSpec, text: String, sizeSp: Float, color: Int,
        bold: Boolean = false, italic: Boolean = false, gravity: Int = Gravity.START
    ): TextView {
        val tv = TextView(ctx)
        tv.text = text
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        tv.setTextColor(color)
        var tf = Themes.typeface(t, bold)
        if (italic) tf = android.graphics.Typeface.create(tf, android.graphics.Typeface.ITALIC)
        tv.typeface = tf
        if (t.id == "diagonal" && bold) tv.setTypeface(tf, android.graphics.Typeface.BOLD_ITALIC)
        tv.gravity = gravity
        if (t.id == "editorial") tv.letterSpacing = 0.02f
        return tv
    }

    /** Thin progress bar rendered as two stacked views (track + fill). */
    /**
     * Entrance motion for cards and rows: a soft rise with a slight
     * grow and fade, staggered by position. Short and always finishing
     * at the natural state, so content is never left hidden.
     */
    fun enter(v: View, index: Int = 0) {
        // One-by-one cascade reveal: each section rises from below and
        // grows into place, in order, like a wave through the screen.
        v.alpha = 0f
        v.translationY = dp(v.context, 30f).toFloat()
        v.scaleX = 0.94f
        v.scaleY = 0.94f
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setStartDelay(minOf(index * 100L, 700L))
            .setDuration(420L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                v.alpha = 1f; v.translationY = 0f; v.scaleX = 1f; v.scaleY = 1f
            }
            .start()
    }

    /** Climbs a number from 0 to its value (scores, progress counts). */
    fun countUp(tv: android.widget.TextView, target: Int, format: (Int) -> String = { it.toString() }, durationMs: Long = 900L) {
        if (target <= 0) {
            tv.text = format(target)
            return
        }
        val anim = android.animation.ValueAnimator.ofInt(0, target)
        anim.duration = durationMs
        anim.interpolator = android.view.animation.DecelerateInterpolator()
        anim.addUpdateListener { tv.text = format(it.animatedValue as Int) }
        anim.start()
    }

    /** Small bounce confirming a tap action (bookmark, nav, option). */
    fun tapPop(v: View) {
        v.animate().scaleX(1.18f).scaleY(1.18f).setDuration(110L)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(220L)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.2f))
                    .withEndAction { v.scaleX = 1f; v.scaleY = 1f }
                    .start()
            }
            .start()
    }

    /** Physical press feedback: dips on touch down, springs back up. */
    fun pressable(v: View) {
        v.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.965f).scaleY(0.965f).setDuration(90L).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(240L)
                        .setInterpolator(android.view.animation.OvershootInterpolator(1.6f))
                        .withEndAction { view.scaleX = 1f; view.scaleY = 1f }
                        .start()
            }
            false
        }
    }

    /** Sweeps a ring or gauge from empty to its percent value. */
    fun sweepRing(ring: RingView, toPercent: Float, durationMs: Long = 950L) {
        val anim = android.animation.ValueAnimator.ofFloat(0f, toPercent)
        anim.duration = durationMs
        anim.interpolator = android.view.animation.DecelerateInterpolator()
        anim.addUpdateListener {
            ring.percent = it.animatedValue as Float
            ring.invalidate()
        }
        anim.start()
    }

    fun sweepGauge(gauge: GaugeView, toPercent: Float, durationMs: Long = 950L) {
        val anim = android.animation.ValueAnimator.ofFloat(0f, toPercent)
        anim.duration = durationMs
        anim.interpolator = android.view.animation.DecelerateInterpolator()
        anim.addUpdateListener {
            gauge.percent = it.animatedValue as Float
            gauge.invalidate()
        }
        anim.start()
    }

    /**
     * Shimmering placeholder blocks shown while a screen loads, shaped
     * like the cards that will replace them.
     */
    fun skeleton(ctx: Context, t: ThemeSpec, rows: Int = 3): View {
        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        val blockColor = if (t.dark) 0xFF232A26.toInt() else 0xFFE7EDE9.toInt()
        for (i in 0 until rows) {
            val block = View(ctx)
            block.background = pillBg(blockColor, 14f, ctx)
            val h = when (i % 3) {
                0 -> 92f
                1 -> 64f
                else -> 76f
            }
            block.layoutParams = lp(ctx, MATCH, dp(ctx, h), intArrayOf(0, if (i == 0) 6 else 12, 0, 0))
            col.addView(block)
        }
        val anim = android.animation.ValueAnimator.ofFloat(0.45f, 1f)
        anim.duration = 850L
        anim.repeatCount = android.animation.ValueAnimator.INFINITE
        anim.repeatMode = android.animation.ValueAnimator.REVERSE
        anim.addUpdateListener { col.alpha = it.animatedValue as Float }
        col.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { if (!anim.isStarted) anim.start() }
            override fun onViewDetachedFromWindow(v: View) { anim.cancel(); col.alpha = 1f }
        })
        return col
    }

    /** Applies [enter] to a group's children, optionally from an offset. */
    fun enterChildren(group: android.view.ViewGroup, from: Int = 0) {
        for (i in from until group.childCount) {
            enter(group.getChildAt(i), i - from)
        }
    }

    /** Growing pop for the splash mark and hero elements. */
    fun popIn(v: View, delayMs: Long = 0L) {
        v.alpha = 0f
        v.scaleX = 0.82f
        v.scaleY = 0.82f
        v.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setStartDelay(delayMs)
            .setDuration(480L)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.4f))
            .withEndAction { v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f }
            .start()
    }

    /** Horizontal slide for quiz question changes (+1 next, -1 previous). */
    fun slideIn(v: View, direction: Int) {
        val dx = dp(v.context, 42f).toFloat() * direction
        v.alpha = 0f
        v.translationX = dx
        v.animate().alpha(1f).translationX(0f)
            .setDuration(260L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction { v.alpha = 1f; v.translationX = 0f }
            .start()
    }

    fun progressBar(ctx: Context, t: ThemeSpec, percent: Int, heightDp: Float = 8f, fill: Int = t.primary): View {
        val track = android.widget.FrameLayout(ctx)
        track.background = pillBg(if (t.dark) 0xFF26302A.toInt() else 0xFFE4EAE5.toInt(), heightDp, ctx)
        val fillView = View(ctx)
        fillView.background = pillBg(fill, heightDp, ctx)
        val pct = percent.coerceIn(0, 100)
        val params = android.widget.FrameLayout.LayoutParams(0, dp(ctx, heightDp))
        fillView.layoutParams = params
        track.addView(fillView)
        track.post {
            val w = (track.width * pct / 100f).toInt()
            val lp2 = fillView.layoutParams as android.widget.FrameLayout.LayoutParams
            lp2.width = w
            if (t.id == "diagonal") fillView.rotation = 0f // parallelogram feel via skew layer below
            fillView.layoutParams = lp2
            // Grow the fill from the left edge instead of snapping in.
            if (w > 0) {
                fillView.pivotX = 0f
                fillView.scaleX = 0f
                fillView.animate().scaleX(1f).setDuration(650L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction { fillView.scaleX = 1f }
                    .start()
            }
        }
        return track
    }

    /** Segmented 10-block progress (Teal Clinical Grid + Focus result legend). */
    fun segmentedBar(ctx: Context, t: ThemeSpec, percent: Int): LinearLayout {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        val filled = (percent.coerceIn(0, 100) / 10f).toInt()
        for (i in 0 until 10) {
            val seg = View(ctx)
            val p = LinearLayout.LayoutParams(0, dp(ctx, 10f), 1f)
            p.marginStart = if (i == 0) 0 else dp(ctx, 3f)
            seg.layoutParams = p
            seg.background = pillBg(if (i < filled) t.primary else if (t.dark) 0xFF26302A.toInt() else 0xFFDFECEC.toInt(), 2f, ctx)
            row.addView(seg)
        }
        return row
    }

    /** Coloured subject colour from the API (hex string), with theme fallback. */
    fun subjectColor(hex: String, t: ThemeSpec, index: Int): Int {
        return try {
            if (hex.startsWith("#") && hex.length >= 7) Color.parseColor(hex) else t.tileTints[index % t.tileTints.size]
        } catch (e: Exception) {
            t.tileTints[index % t.tileTints.size]
        }
    }
}

/** Circular ring progress view (Classic / Dark result + dashboard ring). */
class RingView(ctx: Context) : View(ctx) {
    var percent: Float = 0f
    var ringColor: Int = Color.GREEN
    var trackColor: Int = 0xFFE4EAE5.toInt()
    var strokeDp: Float = 10f
    var centerText: String = ""
    var centerColor: Int = Color.BLACK
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = Ui.dp(context, strokeDp).toFloat()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        val inset = stroke / 2 + Ui.dp(context, 2f)
        val rect = RectF(inset, inset, width - inset, height - inset)
        paint.color = trackColor
        canvas.drawArc(rect, 0f, 360f, false, paint)
        paint.color = ringColor
        canvas.drawArc(rect, -90f, 360f * (percent.coerceIn(0f, 100f) / 100f), false, paint)
        if (centerText.isNotEmpty()) {
            paint.style = Paint.Style.FILL
            paint.color = centerColor
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = Ui.dp(context, 20f).toFloat()
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            val y = height / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(centerText, width / 2f, y, paint)
        }
    }
}

/** Semicircle gauge (Soft Rounded Care progress + result). */
class GaugeView(ctx: Context) : View(ctx) {
    var percent: Float = 0f
    var gaugeColor: Int = Color.MAGENTA
    var trackColor: Int = 0xFFF3DFE8.toInt()
    var centerText: String = ""
    var centerColor: Int = Color.BLACK
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w / 2 + Ui.dp(context, 14f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = Ui.dp(context, 14f).toFloat()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        val inset = stroke / 2 + Ui.dp(context, 2f)
        val rect = RectF(inset, inset, width - inset, (width - inset) * 1f)
        paint.color = trackColor
        canvas.drawArc(rect, 180f, 180f, false, paint)
        paint.color = gaugeColor
        canvas.drawArc(rect, 180f, 180f * (percent.coerceIn(0f, 100f) / 100f), false, paint)
        if (centerText.isNotEmpty()) {
            paint.style = Paint.Style.FILL
            paint.color = centerColor
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = Ui.dp(context, 22f).toFloat()
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            canvas.drawText(centerText, width / 2f, height - Ui.dp(context, 6f).toFloat(), paint)
        }
    }
}

/** App mark: a pulse line through a rounded tile (splash + headers). */
class PulseMarkView(ctx: Context) : View(ctx) {
    var tileColor: Int = 0xFF0A8A4D.toInt()
    var lineColor: Int = Color.WHITE
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = Ui.dp(context, 14f).toFloat()
        paint.style = Paint.Style.FILL
        paint.color = tileColor
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), r, r, paint)
        paint.style = Paint.Style.STROKE
        paint.color = lineColor
        paint.strokeWidth = Ui.dp(context, 4f).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        val midY = height * 0.55f
        val path = Path()
        path.moveTo(width * 0.14f, midY)
        path.lineTo(width * 0.36f, midY)
        path.lineTo(width * 0.45f, height * 0.32f)
        path.lineTo(width * 0.57f, height * 0.74f)
        path.lineTo(width * 0.65f, midY)
        path.lineTo(width * 0.86f, midY)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(width * 0.5f, height * 0.2f, Ui.dp(context, 3.5f).toFloat(), paint)
    }
}

/** The pulse / ECG line from the client's splash branding, animated:
 *  the heartbeat trace draws itself across the screen in a loop, with a
 *  bright dot riding the head of the trace, like a real monitor. */
class EcgLineView(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val faintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x59FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD94D")
        style = Paint.Style.FILL
    }
    private var sweep = 0f
    private var animator: android.animation.ValueAnimator? = null

    private fun tracePath(w: Float, h: Float): Path {
        val mid = h * 0.55f
        val p = Path()
        p.moveTo(0f, mid)
        p.lineTo(w * 0.26f, mid)
        p.lineTo(w * 0.34f, mid - h * 0.16f)
        p.lineTo(w * 0.42f, mid + h * 0.30f)
        p.lineTo(w * 0.50f, mid - h * 0.55f)
        p.lineTo(w * 0.58f, mid + h * 0.14f)
        p.lineTo(w * 0.64f, mid)
        p.lineTo(w, mid)
        return p
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1700
            repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.RESTART
            addUpdateListener { sweep = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val full = tracePath(width.toFloat(), height.toFloat())
        canvas.drawPath(full, faintPaint)
        val measure = android.graphics.PathMeasure(full, false)
        val len = measure.length
        val segment = Path()
        measure.getSegment(0f, len * sweep, segment, true)
        canvas.drawPath(segment, paint)
        val pos = FloatArray(2)
        measure.getPosTan(len * sweep, pos, null)
        canvas.drawCircle(pos[0], pos[1], 8f, dotPaint)
    }
}

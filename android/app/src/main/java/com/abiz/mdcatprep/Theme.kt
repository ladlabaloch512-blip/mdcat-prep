package com.abiz.mdcatprep

import android.graphics.Color
import android.graphics.Typeface

/**
 * Theme engine.
 *
 * A [ThemeSpec] fully describes one of the 10 approved V3 designs: colours,
 * corner radius, shape language, typeface family/weight feel, bottom-nav
 * style, dashboard layout type, quiz option style and result style.
 * Activities/fragments never hard-code a design; they ask [Themes.current]
 * and render through the shared helpers in Ui.kt, switching on the layout /
 * nav / option / result enums. Switching a design persists the id in Prefs
 * and recreates the activity, so every screen re-renders in the new design.
 */

enum class DashboardLayout { TILES, BENTO, LIST, CHIP_GRID }
enum class NavStyle { FILLED_BAR, FLOATING_PILL, FLOATING_DOCK, SLANTED_BAR, FLOATING_CARD, POSTER_BAR, TEXT_MINIMAL }
enum class OptionStyle { ROWS, GRID_2X2, DIVIDED_CARD, BOLD_BLOCKS, DARK_CHIPS, EXAM_SHEET, SOFT_PILLS, SIDE_STRIP }
enum class ResultStyle { RING, BIG_NUMBER_TILES, SEGMENT_BAR, GAUGE, POSTER_GIANT, SPLIT_PANEL, SKEW_BAND, STAT_GRID }
enum class HeaderStyle { GRADIENT_BAND, BLACK_TILE, PLAIN_NUMERAL, DARK_GLOW, DIAGONAL_CUT, HAIRLINE_INDEX, SOFT_WAVE, SPLIT_PANEL, POSTER_BLOCKS, TEAL_GRID }
enum class CardShape { NORMAL, SQUIRCLE, CUT_CORNER, SQUARE, HAIRLINE }

data class ThemeSpec(
    val id: String,
    val name: String,
    val bg: Int,
    val surface: Int,
    val primary: Int,
    val accent: Int,
    val text: Int,
    val muted: Int,
    val onPrimary: Int = Color.WHITE,
    val radiusDp: Float = 16f,
    val shape: CardShape = CardShape.NORMAL,
    val dark: Boolean = false,
    val fontFamily: String = "sans-serif",       // downloadable-font stand-ins:
    val fontWeightNote: String = "medium",        // system families + weight/spacing
    val dashboard: DashboardLayout = DashboardLayout.TILES,
    val nav: NavStyle = NavStyle.FILLED_BAR,
    val options: OptionStyle = OptionStyle.ROWS,
    val result: ResultStyle = ResultStyle.RING,
    val header: HeaderStyle = HeaderStyle.GRADIENT_BAND,
    /** Pastel tints used by tile/bento subject cards, cycled in order. */
    val tileTints: List<Int> = listOf(0xFFE7F6EE.toInt(), 0xFFE8F0FE.toInt(), 0xFFFFF3E0.toInt(), 0xFFF3E8FD.toInt())
)

private fun c(hex: String) = Color.parseColor(hex)

/** One colour choice inside a design: primary + accent override. */
data class Palette(val name: String, val primary: Int, val accent: Int)

object Themes {
    val ALL: List<ThemeSpec> = listOf(
        // 1 — Classic Green Gradient (Poppins-like)
        ThemeSpec(
            id = "classic", name = "Classic Green Gradient",
            bg = c("#F5F9F6"), surface = Color.WHITE, primary = c("#0A8A4D"), accent = c("#0B5ED7"),
            text = c("#12261A"), muted = c("#6B7D71"), radiusDp = 18f,
            dashboard = DashboardLayout.TILES, nav = NavStyle.FILLED_BAR,
            options = OptionStyle.ROWS, result = ResultStyle.RING, header = HeaderStyle.GRADIENT_BAND,
            fontFamily = "sans-serif-medium"
        ),
        // 2 — Bento Scholar (Sora-like)
        ThemeSpec(
            id = "bento", name = "Bento Scholar",
            bg = c("#F4F1EA"), surface = Color.WHITE, primary = c("#111111"), accent = c("#0A8A4D"),
            text = c("#171512"), muted = c("#8A8578"), radiusDp = 24f, shape = CardShape.SQUIRCLE,
            dashboard = DashboardLayout.BENTO, nav = NavStyle.FLOATING_PILL,
            options = OptionStyle.GRID_2X2, result = ResultStyle.BIG_NUMBER_TILES, header = HeaderStyle.BLACK_TILE,
            fontFamily = "sans-serif"
        ),
        // 3 — Focus List (Inter-like)
        ThemeSpec(
            id = "focus", name = "Focus List",
            bg = Color.WHITE, surface = Color.WHITE, primary = c("#111111"), accent = c("#0A8A4D"),
            text = c("#111111"), muted = c("#8A8A8A"), radiusDp = 10f, shape = CardShape.HAIRLINE,
            dashboard = DashboardLayout.LIST, nav = NavStyle.TEXT_MINIMAL,
            options = OptionStyle.DIVIDED_CARD, result = ResultStyle.SEGMENT_BAR, header = HeaderStyle.PLAIN_NUMERAL,
            fontFamily = "sans-serif"
        ),
        // 4 — Dark Mode Focus (Manrope-like)
        ThemeSpec(
            id = "darkfocus", name = "Dark Mode Focus",
            bg = c("#0C0F0D"), surface = c("#171B18"), primary = c("#35D99A"), accent = c("#35D99A"),
            text = c("#EDF5F0"), muted = c("#7E8C84"), onPrimary = c("#06231A"), radiusDp = 18f, dark = true,
            dashboard = DashboardLayout.TILES, nav = NavStyle.FLOATING_DOCK,
            options = OptionStyle.DARK_CHIPS, result = ResultStyle.RING, header = HeaderStyle.DARK_GLOW,
            fontFamily = "sans-serif-medium"
        ),
        // 5 — Diagonal Energy (Barlow-like, italic accents)
        ThemeSpec(
            id = "diagonal", name = "Diagonal Energy",
            bg = c("#F4F8F5"), surface = Color.WHITE, primary = c("#0A8A4D"), accent = c("#FF7A1A"),
            text = c("#132018"), muted = c("#71806F"), radiusDp = 12f, shape = CardShape.CUT_CORNER,
            dashboard = DashboardLayout.TILES, nav = NavStyle.SLANTED_BAR,
            options = OptionStyle.ROWS, result = ResultStyle.SKEW_BAND, header = HeaderStyle.DIAGONAL_CUT,
            fontFamily = "sans-serif-condensed"
        ),
        // 6 — Editorial Index (Archivo-like)
        ThemeSpec(
            id = "editorial", name = "Editorial Index",
            bg = c("#FAF7F0"), surface = c("#FAF7F0"), primary = c("#1A1A1A"), accent = c("#0A8A4D"),
            text = c("#1A1A1A"), muted = c("#7A7466"), radiusDp = 0f, shape = CardShape.HAIRLINE,
            dashboard = DashboardLayout.LIST, nav = NavStyle.TEXT_MINIMAL,
            options = OptionStyle.EXAM_SHEET, result = ResultStyle.SEGMENT_BAR, header = HeaderStyle.HAIRLINE_INDEX,
            fontFamily = "serif"
        ),
        // 7 — Soft Rounded Care (Quicksand-like)
        ThemeSpec(
            id = "soft", name = "Soft Rounded Care",
            bg = c("#FDF3F6"), surface = Color.WHITE, primary = c("#E56B9F"), accent = c("#4EA8A0"),
            text = c("#3D2C33"), muted = c("#A08B96"), radiusDp = 28f, shape = CardShape.SQUIRCLE,
            dashboard = DashboardLayout.TILES, nav = NavStyle.FLOATING_CARD,
            options = OptionStyle.SOFT_PILLS, result = ResultStyle.GAUGE, header = HeaderStyle.SOFT_WAVE,
            fontFamily = "sans-serif"
        ),
        // 8 — Split Panel Pro (Space Grotesk-like)
        ThemeSpec(
            id = "split", name = "Split Panel Pro",
            bg = c("#EEF3F0"), surface = Color.WHITE, primary = c("#0B5136"), accent = c("#0A8A4D"),
            text = c("#14231C"), muted = c("#71817A"), radiusDp = 14f,
            dashboard = DashboardLayout.TILES, nav = NavStyle.FILLED_BAR,
            options = OptionStyle.SIDE_STRIP, result = ResultStyle.SPLIT_PANEL, header = HeaderStyle.SPLIT_PANEL,
            fontFamily = "monospace"
        ),
        // 9 — Duotone Poster (Archivo Black-like headlines)
        ThemeSpec(
            id = "poster", name = "Duotone Poster",
            bg = Color.WHITE, surface = Color.WHITE, primary = c("#0A8A4D"), accent = c("#111111"),
            text = c("#111111"), muted = c("#666666"), radiusDp = 0f, shape = CardShape.SQUARE,
            dashboard = DashboardLayout.TILES, nav = NavStyle.POSTER_BAR,
            options = OptionStyle.BOLD_BLOCKS, result = ResultStyle.POSTER_GIANT, header = HeaderStyle.POSTER_BLOCKS,
            fontFamily = "sans-serif-black"
        ),
        // 10 — Teal Clinical Grid (Inter-like)
        ThemeSpec(
            id = "teal", name = "Teal Clinical Grid",
            bg = c("#F2F8F8"), surface = Color.WHITE, primary = c("#0E9594"), accent = c("#0E9594"),
            text = c("#122A2A"), muted = c("#6E8A8A"), radiusDp = 8f, shape = CardShape.HAIRLINE,
            dashboard = DashboardLayout.CHIP_GRID, nav = NavStyle.FILLED_BAR,
            options = OptionStyle.ROWS, result = ResultStyle.STAT_GRID, header = HeaderStyle.TEAL_GRID,
            fontFamily = "sans-serif"
        )
    )

    /**
     * Each design carries its OWN colour palettes (Snow's rule: design
     * bhi alag, colour picker bhi alag). Index 0 is always the design's
     * original colours; the map holds the three alternates per design.
     */
    private val ALTERNATES: Map<String, List<Palette>> = mapOf(
        "classic" to listOf(
            Palette("Ocean", c("#0B5ED7"), c("#00A3A3")),
            Palette("Royal", c("#6C3CE9"), c("#0B5ED7")),
            Palette("Berry", c("#C2255C"), c("#862E9C"))),
        "bento" to listOf(
            Palette("Forest", c("#14532D"), c("#B45309")),
            Palette("Navy", c("#14213D"), c("#D97706")),
            Palette("Plum", c("#581C87"), c("#DB2777"))),
        "focus" to listOf(
            Palette("Slate", c("#334155"), c("#0284C7")),
            Palette("Maroon", c("#7F1D1D"), c("#B45309")),
            Palette("Teal", c("#134E4A"), c("#0F766E"))),
        "darkfocus" to listOf(
            Palette("Ice", c("#7DD3FC"), c("#A78BFA")),
            Palette("Amber", c("#FBBF24"), c("#FB923C")),
            Palette("Rose", c("#FDA4AF"), c("#F472B6"))),
        "diagonal" to listOf(
            Palette("Crimson", c("#C92A2A"), c("#1864AB")),
            Palette("Indigo", c("#3B5BDB"), c("#F76707")),
            Palette("Violet", c("#7048E8"), c("#0CA678"))),
        "editorial" to listOf(
            Palette("Ink Blue", c("#1A1A1A"), c("#1864AB")),
            Palette("Rust", c("#7C2D12"), c("#B45309")),
            Palette("Olive", c("#3F6212"), c("#A16207"))),
        "soft" to listOf(
            Palette("Lavender", c("#7C6BD9"), c("#E56B9F")),
            Palette("Peach", c("#E8834E"), c("#3E9B8F")),
            Palette("Mint", c("#2F9E77"), c("#E8834E"))),
        "split" to listOf(
            Palette("Midnight", c("#0B2545"), c("#2A9D8F")),
            Palette("Burgundy", c("#5C1A24"), c("#B45309")),
            Palette("Slate Teal", c("#263238"), c("#0E9594"))),
        "poster" to listOf(
            Palette("Red", c("#D62828"), c("#111111")),
            Palette("Blue", c("#1864AB"), c("#F08C00")),
            Palette("Purple", c("#5F0F40"), c("#111111"))),
        "teal" to listOf(
            Palette("Ocean", c("#0B5ED7"), c("#00A3A3")),
            Palette("Grape", c("#7048E8"), c("#0CA678")),
            Palette("Coral", c("#E8590C"), c("#0E9594")))
    )

    /** Palettes for a design: Original first, then its alternates. */
    fun palettesFor(spec: ThemeSpec): List<Palette> =
        listOf(Palette("Original", spec.primary, spec.accent)) + (ALTERNATES[spec.id] ?: emptyList())

    fun byId(id: String?): ThemeSpec {
        if (id.isNullOrEmpty()) return ALL[0]
        ALL.firstOrNull { it.id == id }?.let { return it }
        // The admin default_design setting is the design NUMBER (1-10),
        // while saved user choices store the string id. Accept both.
        val n = id.toIntOrNull()
        if (n != null && n in 1..ALL.size) return ALL[n - 1]
        return ALL[0]
    }

    /** Active design: the user's saved choice wins, then the server default.
     *  The design's own saved colour palette is applied on top. */
    fun current(): ThemeSpec {
        val saved = try { Prefs.designId } catch (e: Exception) { null }
        val spec = if (!saved.isNullOrEmpty()) byId(saved) else {
            val serverDefault = try { Prefs.settings()?.optString("default_design", "") } catch (e: Exception) { "" }
            byId(serverDefault)
        }
        val idx = try { Prefs.paletteIndex(spec.id) } catch (e: Exception) { 0 }
        val pals = palettesFor(spec)
        if (idx <= 0 || idx >= pals.size) return spec
        val p = pals[idx]
        return spec.copy(primary = p.primary, accent = p.accent)
    }

    fun typeface(t: ThemeSpec, bold: Boolean = false): Typeface {
        val base = when (t.fontFamily) {
            "serif" -> Typeface.SERIF
            "monospace" -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
        return if (bold || t.fontFamily == "sans-serif-black") {
            Typeface.create(base, Typeface.BOLD)
        } else {
            Typeface.create(base, Typeface.NORMAL)
        }
    }
}

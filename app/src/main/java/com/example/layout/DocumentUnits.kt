package com.example.layout

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for all unit conversions across the Midad document publishing engine.
 *
 * Fundamental physical standards:
 * - 1 inch = 72 pt (PostScript / PDF Points)
 * - 1 inch = 25.4 mm (International Standard)
 * - 1 inch = 160 dp (Android Baseline Density mdpi)
 *
 * Strictly prohibits approximate magic numbers (e.g. 0.8f, 0.9f).
 */
object DocumentUnits {

    const val POINTS_PER_INCH = 72f
    const val MM_PER_INCH = 25.4f
    const val DP_PER_INCH = 160f

    // Direct mathematical factors
    const val MM_TO_PT = POINTS_PER_INCH / MM_PER_INCH // ~2.8346457f
    const val PT_TO_MM = MM_PER_INCH / POINTS_PER_INCH // ~0.3527778f
    const val PT_TO_DP = DP_PER_INCH / POINTS_PER_INCH // ~2.2222223f
    const val DP_TO_PT = POINTS_PER_INCH / DP_PER_INCH // 0.45f
    const val MM_TO_DP = DP_PER_INCH / MM_PER_INCH     // ~6.2992124f
    const val DP_TO_MM = MM_PER_INCH / DP_PER_INCH     // 0.15875f

    // Millimeters <-> Points (PDF / Print domain)
    fun mmToPt(mm: Float): Float = mm * MM_TO_PT
    fun ptToMm(pt: Float): Float = pt * PT_TO_MM
    fun inchToPt(inch: Float): Float = inch * POINTS_PER_INCH
    fun ptToInch(pt: Float): Float = pt / POINTS_PER_INCH

    // Points <-> Density-independent pixels (Screen / Compose domain)
    fun ptToDp(pt: Float): Float = pt * PT_TO_DP
    fun dpToPt(dp: Float): Float = dp * DP_TO_PT
    fun ptToDpValue(pt: Float): Dp = (pt * PT_TO_DP).dp

    // Millimeters <-> Density-independent pixels
    fun mmToDp(mm: Float): Float = mm * MM_TO_DP
    fun dpToMm(dp: Float): Float = dp * DP_TO_MM
    fun mmToDpValue(mm: Float): Dp = (mm * MM_TO_DP).dp

    // Screen Density Conversions (Pixel domain)
    fun ptToPx(pt: Float, density: Density): Float = ptToDp(pt) * density.density
    fun pxToPt(px: Float, density: Density): Float = (px / density.density) * DP_TO_PT

    fun mmToPx(mm: Float, density: Density): Float = mmToDp(mm) * density.density
    fun pxToMm(px: Float, density: Density): Float = (px / density.density) * DP_TO_MM

    fun dpToPx(dp: Float, density: Density): Float = dp * density.density
    fun pxToDp(px: Float, density: Density): Float = px / density.density

    // Standard SP to PT mapping (at 1.0 font scale, 1pt = 1sp)
    fun ptToSp(pt: Float): Float = pt
    fun spToPt(sp: Float): Float = sp
}

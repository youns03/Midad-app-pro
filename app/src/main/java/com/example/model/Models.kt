package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign

/**
 * Supported standard paper sizes for Arabic document publishing and export.
 */
enum class PageSize(
    val displayNameAr: String,
    val widthMm: Float,
    val heightMm: Float,
    val widthPt: Float,
    val heightPt: Float
) {
    A4("A4 (210 × 297 مم)", 210f, 297f, 595.28f, 841.89f),
    A5("A5 (148 × 210 مم)", 148f, 210f, 419.53f, 595.28f)
}

/**
 * Margin measurement units: Millimeters (مم) or Centimeters (سم).
 */
enum class MarginUnit(val symbolAr: String, val toMmFactor: Float) {
    MILLIMETER("ملم", 1.0f),
    CENTIMETER("سم", 10.0f)
}

/**
 * Page Margins with precision in millimeters and display unit.
 */
data class PageMargins(
    val topMm: Float = 20f,
    val bottomMm: Float = 20f,
    val rightMm: Float = 20f,
    val leftMm: Float = 20f,
    val unit: MarginUnit = MarginUnit.MILLIMETER
) {
    fun displayTop(): Float = if (unit == MarginUnit.CENTIMETER) topMm / 10f else topMm
    fun displayBottom(): Float = if (unit == MarginUnit.CENTIMETER) bottomMm / 10f else bottomMm
    fun displayRight(): Float = if (unit == MarginUnit.CENTIMETER) rightMm / 10f else rightMm
    fun displayLeft(): Float = if (unit == MarginUnit.CENTIMETER) leftMm / 10f else leftMm

    companion object {
        fun fromDisplay(
            top: Float,
            bottom: Float,
            right: Float,
            left: Float,
            unit: MarginUnit
        ): PageMargins {
            val factor = unit.toMmFactor
            return PageMargins(
                topMm = (top * factor).coerceIn(5f, 60f),
                bottomMm = (bottom * factor).coerceIn(5f, 60f),
                rightMm = (right * factor).coerceIn(5f, 60f),
                leftMm = (left * factor).coerceIn(5f, 60f),
                unit = unit
            )
        }
    }
}

/**
 * Text Alignment options.
 */
enum class TextAlignOption(val labelAr: String, val composeAlign: TextAlign) {
    RIGHT("يمين", TextAlign.Right),
    CENTER("وسط", TextAlign.Center),
    LEFT("يسار", TextAlign.Left),
    JUSTIFY("ضبط كامل", TextAlign.Justify)
}

/**
 * Preset Kashida / Tatweel levels.
 */
enum class KashidaLevel(val labelAr: String, val tatweelCount: Int) {
    OFF("معطلة", 0),
    LIGHT("خفيف (ـ)", 1),
    MEDIUM("متوسط (ــ)", 2),
    HEAVY("كبير (ـــ)", 3)
}

/**
 * Represents a font available in the app (either pre-bundled or user-imported).
 */
data class FontItem(
    val id: String,
    val nameAr: String,
    val fontFamily: FontFamily,
    val isCustom: Boolean = false,
    val filePath: String? = null
)

/**
 * Ready-made templates tailored for Arabic typography use-cases.
 */
data class DocumentTemplate(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val categoryAr: String,
    val sampleText: String,
    val fontId: String,
    val fontSizePt: Float,
    val textColor: Color,
    val pageColor: Color,
    val align: TextAlignOption,
    val kashidaEnabled: Boolean,
    val kashidaLevel: KashidaLevel,
    val margins: PageMargins
)

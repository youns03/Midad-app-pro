package com.example.layout

import android.graphics.Color
import android.graphics.Typeface
import com.example.model.KashidaLevel
import com.example.model.PageMargins
import com.example.model.PageSize
import com.example.model.TextAlignOption

/**
 * Encapsulates all environmental and typographic parameters that influence document layout.
 * Any modification to these settings triggers a recalculation by [DocumentLayoutEngine].
 */
data class LayoutSettings(
    val pageSize: PageSize = PageSize.A4,
    val margins: PageMargins = PageMargins(),
    val typeface: Typeface = Typeface.DEFAULT,
    val fontSizePt: Float = 18f,
    val lineSpacingMultiplier: Float = 1.35f,
    val lineSpacingExtraPt: Float = 0f,
    val textAlign: TextAlignOption = TextAlignOption.JUSTIFY,
    val textColor: Int = Color.BLACK,
    val pageColor: Int = Color.WHITE,
    val kashidaEnabled: Boolean = false,
    val kashidaLevel: KashidaLevel = KashidaLevel.OFF
)

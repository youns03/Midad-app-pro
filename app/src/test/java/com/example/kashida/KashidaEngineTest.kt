package com.example.kashida

import android.graphics.Typeface
import android.text.TextPaint
import com.example.model.KashidaLevel
import com.example.model.MarginUnit
import com.example.model.PageMargins
import com.example.model.PageSize
import com.example.model.TextAlignOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KashidaEngineTest {

    private val paint = TextPaint().apply {
        typeface = Typeface.DEFAULT
        textSize = 18f
    }

    @Test
    fun manual_kashida_keeps_diacritics_attached() {
        val word = "بَتَ"
        val point = KashidaEngine.findConnectionPointsInWord(word).first()

        val result = KashidaEngine.applyManualKashidaToWord(
            word,
            point.indexInWord,
            2
        )

        assertEquals("بَــتَ", result)
    }

    @Test
    fun non_connecting_letters_are_not_candidates() {
        assertTrue(KashidaEngine.findConnectionPointsInWord("ادر").isEmpty())
        assertFalse(KashidaEngine.findConnectionPointsInWord("كتب").isEmpty())
    }

    @Test
    fun shaping_is_render_only_and_never_mutates_input() {
        val source = "كتب العربية الجميلة"
        val shaped = KashidaEngine.shapeLine(
            source,
            deficitPx = 20f,
            level = KashidaLevel.MEDIUM,
            paint = paint
        )

        assertFalse(source.contains(KashidaEngine.TATWEEL))
        assertTrue(shaped.contains(source.first()))
    }

    @Test
    fun layout_returns_real_pages_without_splitting_lines() {
        val source = ("هذا سطر عربي طويل للاختبار. ".repeat(80))
        val layout = DocumentLayoutEngine.build(
            text = source,
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            textAlign = TextAlignOption.JUSTIFY,
            margins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER),
            pageSize = PageSize.A4,
            kashidaEnabled = true,
            kashidaLevel = KashidaLevel.MEDIUM
        )

        assertTrue(layout.pageCount > 1)
        assertEquals(source, layout.rawText)
        assertTrue(layout.pages.all { page ->
            page.lines.zipWithNext().all { (a, b) -> b.topPt >= a.topPt }
        })
    }

    @Test
    fun layout_preserves_blank_lines_and_uses_point_margins() {
        val layout = DocumentLayoutEngine.build(
            text = "الأول\n\nالثالث",
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            textAlign = TextAlignOption.RIGHT,
            margins = PageMargins(25.4f, 25.4f, 25.4f, 25.4f, MarginUnit.MILLIMETER),
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        assertEquals(72f, layout.pages.first().leftMarginPt, 0.01f)
        assertEquals(72f, layout.pages.first().topMarginPt, 0.01f)
        assertTrue(layout.pages.first().lines.any { it.text.isEmpty() })
        assertEquals(1, layout.pageCount)
    }
}

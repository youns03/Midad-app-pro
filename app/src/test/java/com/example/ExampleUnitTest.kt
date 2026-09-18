package com.example

import android.graphics.Typeface
import android.text.TextPaint
import com.example.kashida.KashidaEngine
import com.example.model.KashidaLevel
import com.example.model.MarginUnit
import com.example.model.PageMargins
import com.example.model.PageSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testStripKashida() {
        val withKashida = "كـتـاب جـمـيـل"
        val clean = KashidaEngine.stripKashida(withKashida)
        assertEquals("كتاب جميل", clean)
    }

    @Test
    fun testArabicConnectionRules() {
        // Ba connects to Ta
        assertTrue(KashidaEngine.canConnectForward('ب', 'ت'))
        // Seen connects to Meem
        assertTrue(KashidaEngine.canConnectForward('س', 'م'))

        // Non-connecting forward letters
        assertFalse(KashidaEngine.canConnectForward('د', 'ر'))
        assertFalse(KashidaEngine.canConnectForward('و', 'ر'))
        assertFalse(KashidaEngine.canConnectForward('ر', 'ح'))
        assertFalse(KashidaEngine.canConnectForward('ا', 'ل'))

        // Lam-Alef ligature must NOT have kashida in between
        assertFalse(KashidaEngine.canConnectForward('ل', 'ا'))
        assertFalse(KashidaEngine.canConnectForward('ل', 'أ'))
        assertFalse(KashidaEngine.canConnectForward('ل', 'إ'))
    }

    @Test
    fun testFindConnectionPointsInWord() {
        val word = "كتاب"
        val points = KashidaEngine.findConnectionPointsInWord(word)
        // 'ك' connects to 'ت', 'ت' connects to 'ا', 'ا' does NOT connect forward to 'ب'
        assertEquals(2, points.size)
        assertEquals('ك', points[0].firstChar)
        assertEquals('ت', points[0].secondChar)
        assertEquals('ت', points[1].firstChar)
        assertEquals('ا', points[1].secondChar)
    }

    @Test
    fun testSmartKashidaApplication() {
        val input = "الخط العربي فن جميل ورائع"
        val paint = TextPaint().apply {
            typeface = Typeface.DEFAULT
            textSize = 18f
        }
        val applied = KashidaEngine.shapeLine(input, 200f, KashidaLevel.MEDIUM, paint)
        assertTrue(applied.contains(KashidaEngine.TATWEEL))
        assertEquals(input, KashidaEngine.stripKashida(applied))
    }

    @Test
    fun testManualKashidaApplication() {
        val word = "كتاب"
        val extended = KashidaEngine.applyManualKashidaToWord(word, 0, 3)
        assertEquals("كـــتاب", extended)
    }

    @Test
    fun testPageMarginsConversion() {
        val marginsMm = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER)
        assertEquals(20f, marginsMm.displayTop(), 0.01f)

        val marginsCm = PageMargins(20f, 20f, 20f, 20f, MarginUnit.CENTIMETER)
        assertEquals(2.0f, marginsCm.displayTop(), 0.01f)

        val fromDisplay = PageMargins.fromDisplay(2.5f, 2.5f, 2.5f, 2.5f, MarginUnit.CENTIMETER)
        assertEquals(25f, fromDisplay.topMm, 0.01f)
    }

    @Test
    fun testPageSizes() {
        assertEquals(210f, PageSize.A4.widthMm, 0.01f)
        assertEquals(297f, PageSize.A4.heightMm, 0.01f)
        assertEquals(148f, PageSize.A5.widthMm, 0.01f)
        assertEquals(210f, PageSize.A5.heightMm, 0.01f)
    }
}

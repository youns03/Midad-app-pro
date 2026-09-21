package com.example

import android.graphics.Typeface
import com.example.layout.DocumentLayoutEngine
import com.example.layout.DocumentUnits
import com.example.layout.LayoutSettings
import com.example.model.MarginUnit
import com.example.model.PageMargins
import com.example.model.PageSize
import com.example.model.TextAlignOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentLayoutEngineTest {

    private val defaultSettings = LayoutSettings(
        pageSize = PageSize.A4,
        margins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER),
        typeface = Typeface.DEFAULT,
        fontSizePt = 16f,
        textAlign = TextAlignOption.RIGHT,
        textColor = 0xFF000000.toInt(),
        pageColor = 0xFFFFFFFF.toInt()
    )

    @Test
    fun testDocumentUnitsPrecision() {
        // 25.4 mm is exactly 1 inch = 72 pt
        val ptFromMm = DocumentUnits.mmToPt(25.4f)
        assertEquals(72.0f, ptFromMm, 0.001f)

        // 72 pt is exactly 25.4 mm
        val mmFromPt = DocumentUnits.ptToMm(72.0f)
        assertEquals(25.4f, mmFromPt, 0.001f)

        // 1 inch = 72 pt
        val ptFromInch = DocumentUnits.inchToPt(1.0f)
        assertEquals(72.0f, ptFromInch, 0.001f)

        // A4 paper is 210mm x 297mm
        val a4WidthPt = DocumentUnits.mmToPt(210f)
        val a4HeightPt = DocumentUnits.mmToPt(297f)
        assertEquals(595.2756f, a4WidthPt, 0.05f)
        assertEquals(841.8898f, a4HeightPt, 0.05f)
    }

    @Test
    fun testSinglePageDocumentLayout() {
        val text = "بسم الله الرحمن الرحيم\nهذا نص تجريبي لتطبيق مِداد لتنضيد النصوص العربية."
        val layout = DocumentLayoutEngine.calculateLayout(text, defaultSettings)

        // Must produce exactly 1 page
        assertEquals(1, layout.pages.size)
        val page = layout.pages[0]
        assertEquals(0, page.pageIndex)
        assertTrue(page.lines.isNotEmpty())

        // Page width and height match A4
        assertEquals(595.2756f, layout.pageWidthPt, 0.1f)
        assertEquals(841.8898f, layout.pageHeightPt, 0.1f)

        // Margins check
        val expectedMarginPt = DocumentUnits.mmToPt(20f)
        assertEquals(expectedMarginPt, layout.marginLeftPt, 0.01f)
        assertEquals(expectedMarginPt, layout.marginTopPt, 0.01f)
        assertEquals(layout.pageWidthPt - (expectedMarginPt * 2), layout.contentWidthPt, 0.01f)
        assertEquals(layout.pageHeightPt - (expectedMarginPt * 2), layout.contentHeightPt, 0.01f)

        // Total content height on page must not exceed available content height
        assertTrue("Page content height must fit within printable content height", page.contentUsedHeightPt <= layout.contentHeightPt)
        assertEquals(layout.pageHeightPt, page.heightPt, 0.01f)
    }

    @Test
    fun testMultiPagePaginationNoOverlap() {
        // Generate a long Arabic text that exceeds 1 A4 page
        val paragraph = "مِداد هو تطبيق متقدم لتنضيد وتحرير النصوص العربية وفق قواعد الخط الأصيل مع دعم الكشيدة الذكية وضبط الهوامش والمقاييس بدقة متناهية.\n"
        val longText = paragraph.repeat(60)

        val layout = DocumentLayoutEngine.calculateLayout(longText, defaultSettings)

        // Should span multiple pages
        assertTrue("Long text should span more than 1 page, actual: ${layout.pages.size}", layout.pages.size > 1)

        // Verify that all lines across all pages are consecutive and non-overlapping
        var totalLinesCount = 0
        var prevGlobalIndex = -1
        for (page in layout.pages) {
            assertTrue("Page ${page.pageIndex} must not be empty", page.lines.isNotEmpty())
            assertTrue("Page ${page.pageIndex} height must not exceed contentHeight", page.contentUsedHeightPt <= layout.contentHeightPt)

            for (line in page.lines) {
                totalLinesCount++
                if (prevGlobalIndex != -1) {
                    assertEquals(prevGlobalIndex + 1, line.lineIndexInDocument)
                }
                prevGlobalIndex = line.lineIndexInDocument
            }
        }
        assertTrue("Expected many lines to be laid out", totalLinesCount > 50)
    }

    @Test
    fun testBoundaryLineMovesToNextPage() {
        // Generate text that fills almost an entire page
        val paragraph = "سطر نص عربي لاختبار الحد الفاصل بين الصفحات في محرك التخطيط الموحد.\n"
        val text = paragraph.repeat(45)

        val layout = DocumentLayoutEngine.calculateLayout(text, defaultSettings)

        for (page in layout.pages) {
            // Every page's content height must strictly fit within contentHeightPt without vertical overflow
            assertTrue(
                "Page content ${page.contentUsedHeightPt} must not exceed printable height ${layout.contentHeightPt}",
                page.contentUsedHeightPt <= layout.contentHeightPt
            )
        }
    }

    @Test
    fun testSingleSourceOfTruthFields() {
        val text = "نص تجريبي للتحقق من المصدر الموحد للحقيقة"
        val layout = DocumentLayoutEngine.calculateLayout(text, defaultSettings)

        assertEquals(text, layout.rawText)
        assertEquals(defaultSettings, layout.settings)
        assertEquals(1, layout.pageCount)
        assertEquals(layout.pageWidthPt, layout.pages[0].pageWidthPt, 0.01f)
        assertEquals(layout.pageHeightPt, layout.pages[0].pageHeightPt, 0.01f)
        assertEquals(layout.contentWidthPt, layout.pages[0].contentWidthPt, 0.01f)
        assertEquals(layout.contentHeightPt, layout.pages[0].contentHeightPt, 0.01f)
    }

    @Test
    fun testKashidaIntegratedInLayout() {
        val justifiedSettings = defaultSettings.copy(
            textAlign = TextAlignOption.JUSTIFY,
            kashidaEnabled = true,
            kashidaLevel = com.example.model.KashidaLevel.HEAVY
        )
        val text = "الكتابة باللغة العربية فن جميل يستحق الإتقان والتنضيد البديع"
        val layout = DocumentLayoutEngine.calculateLayout(text, justifiedSettings)

        assertEquals(text, layout.rawText)
        // Kashida should be present in renderedText
        assertTrue("Rendered text should contain tatweel when justified", layout.renderedText.contains('\u0640'))
        assertEquals(justifiedSettings, layout.settings)
    }

    @Test
    fun testMixedArabicEnglishNumbers() {
        val bidiText = "هذا التقرير الصادر في عام 2026 يتضمن الإصدار رقم v2.5 من محرك LayoutEngine مع دعم UTF-8 الكامل."
        val layout = DocumentLayoutEngine.calculateLayout(bidiText, defaultSettings)

        assertEquals(1, layout.pageCount)
        assertTrue(layout.pages[0].lines.isNotEmpty())
        for (line in layout.pages[0].lines) {
            assertTrue("Line width must be <= contentWidth", line.widthPt <= layout.contentWidthPt + 1.0f)
        }
    }

    @Test
    fun testCanvasDrawPageMultiPage() {
        val paragraph = "فقرة اختبارية للرسم المتعدد الصفحات على الكانفاس المشترك.\n"
        val text = paragraph.repeat(50)
        val layout = DocumentLayoutEngine.calculateLayout(text, defaultSettings)

        assertTrue(layout.pageCount > 1)

        val bitmap = android.graphics.Bitmap.createBitmap(
            layout.pageWidthPt.toInt(),
            layout.pageHeightPt.toInt(),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)

        for (page in layout.pages) {
            DocumentLayoutEngine.drawPage(
                canvas = canvas,
                pageIndex = page.pageIndex,
                layout = layout,
                settings = defaultSettings,
                drawBackground = true,
                drawFooter = true
            )
        }
        bitmap.recycle()
    }

    @Test
    fun testEmptyTextLayout() {
        val layout = DocumentLayoutEngine.calculateLayout("", defaultSettings)
        assertEquals(1, layout.pages.size)
        assertEquals(0, layout.pages[0].lines.size)
        assertEquals(0f, layout.pages[0].contentUsedHeightPt, 0.001f)
        assertEquals(layout.pageHeightPt, layout.pages[0].heightPt, 0.01f)
    }
}

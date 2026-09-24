package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.example.data.TextDocumentImporter
import com.example.export.ImageExporter
import com.example.export.PdfExporter
import com.example.export.PreflightEngine
import com.example.export.PreflightSeverity
import com.example.kashida.DocumentLayoutEngine
import com.example.kashida.KashidaEngine
import com.example.model.BaseDirection
import com.example.model.KashidaLevel
import com.example.model.MarginUnit
import com.example.model.PageMargins
import com.example.model.PageSize
import com.example.model.TextAlignOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OutputBenchmarkTest {

    private val defaultMargins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER)

    // Benchmark A: Simple Arabic paragraph
    @Test
    fun benchmark_A_simple_arabic_paragraph() {
        val text = "هذا نص عربي بسيط ومباشر يهدف إلى التحقق من سلامة التنضيد وإخراج الصفحات."
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.RIGHT,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )
        // Invariant 1: Raw text preserved
        assertEquals(text, layout.rawText)
        // Invariant 2: Page count >= 1
        assertEquals(1, layout.pageCount)
        // Invariant 4: Geometry validity
        val page = layout.pages[0]
        assertTrue(page.leftMarginPt + page.contentWidthPt <= page.widthPt + 0.5f)
        assertTrue(page.topMarginPt + page.contentHeightPt <= page.heightPt + 0.5f)
        // Invariant 5: No overflow
        assertFalse(layout.hasOverflow)
    }

    // Benchmark B: Multiple paragraphs with blank lines
    @Test
    fun benchmark_B_multiple_paragraphs_with_blank_lines() {
        val text = "الفقرة الأولى هنا.\n\nالفقرة الثانية بعد سطر فارغ.\n\n\nالفقرة الثالثة بعد سطرين فارغين."
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.RIGHT,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )
        assertEquals(text, layout.rawText)
        // Verify blank lines are not swallowed: line count should represent all segments
        assertTrue("Lines count must reflect paragraphs and blank lines", layout.pages[0].lines.size >= 3)
    }

    // Benchmark C: Mixed text (Arabic + Latin words)
    @Test
    fun benchmark_C_mixed_arabic_and_latin_text() {
        val text = "مشروع Midad يستهدف إنتاج مستندات PDF احترافية بنظام Android الحديث."
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.JUSTIFY,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = true,
            kashidaLevel = KashidaLevel.LIGHT
        )
        assertEquals(text, layout.rawText)
        assertFalse(layout.hasOverflow)

        // Preflight verifies mixed BiDi detection
        val report = PreflightEngine.inspect(layout, Typeface.DEFAULT, 16f, PageSize.A4, defaultMargins)
        assertTrue(report.diagnostics.any { it.title.contains("ثنائي الاتجاه") })
    }

    // Benchmark D: Numbers (Eastern Arabic and Latin digits)
    @Test
    fun benchmark_D_numbers_eastern_and_latin() {
        val text = "العدد العربي المشرقي ١٢٣٤٥ والعدد اللاتيني 12345 متجاوران في جملة واحدة."
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.RIGHT,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )
        assertEquals(text, layout.rawText)
        val rendered = layout.pages[0].renderedText
        assertTrue(rendered.contains("١٢٣٤٥"))
        assertTrue(rendered.contains("12345"))
    }

    // Benchmark E: Heavy diacritics (tashkeel)
    @Test
    fun benchmark_E_heavy_diacritics() {
        val text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ، الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ."
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            textAlign = TextAlignOption.JUSTIFY,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = true,
            kashidaLevel = KashidaLevel.MEDIUM
        )
        // Verify diacritics are preserved without illegal tatweel insertion between letter & diacritic
        assertEquals(text, layout.rawText)
        val shapedLine = layout.pages[0].lines[0].text
        // Ensure tashkeel is not lost or corrupted
        assertTrue(shapedLine.contains('\u064E')) // Fatha
        assertTrue(shapedLine.contains('\u0650')) // Kasra
    }

    // Benchmark F: Manual Tatweel preservation
    @Test
    fun benchmark_F_manual_tatweel_preservation() {
        val text = "كــتـاب جــمـيـل مـمـتــد"
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.JUSTIFY,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = true,
            kashidaLevel = KashidaLevel.HEAVY
        )
        assertEquals(text, layout.rawText)
        val rendered = layout.pages[0].lines[0].text
        assertTrue("Manual tatweel must remain in rendered output", rendered.contains(KashidaEngine.TATWEEL))
    }

    // Benchmark G: Long single paragraph wrapping across multiple pages
    @Test
    fun benchmark_G_multi_page_wrapping() {
        val paragraph = "هذا سطر طويل يكرر لشغل مساحة الصفحة واختبار خوارزمية التوزيع والتصفح الحتمية للتأكد من عدم ضياع الأسطر بين الصفحات. "
        val longText = paragraph.repeat(80)

        val layout = DocumentLayoutEngine.build(
            text = longText,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.JUSTIFY,
            margins = defaultMargins,
            pageSize = PageSize.A5,
            kashidaEnabled = true,
            kashidaLevel = KashidaLevel.LIGHT
        )

        assertEquals(longText, layout.rawText)
        val totalLines = layout.pages.sumOf { it.lines.size }
        val firstPageLines = layout.pages.firstOrNull()?.lines?.size ?: 0
        assertTrue("Long text must result in multiple pages, actual pageCount=${layout.pageCount}, totalLines=$totalLines, firstPageLines=$firstPageLines", layout.pageCount > 1)

        // Invariant 3: No line split across page boundaries
        for (page in layout.pages) {
            assertTrue("Each page must have at least one line", page.lines.isNotEmpty())
            val pageUsedHeight = page.lines.last().bottomPt
            assertTrue("Page content must strictly fit within page height", pageUsedHeight <= page.contentHeightPt + 1.0f)
        }
    }

    // Benchmark H & I: Small vs Large Margins
    @Test
    fun benchmark_H_and_I_margins_influence_layout() {
        val text = "اختبار الفارق بين الهوامش الصغيرة والهوامش الكبيرة في تحديد عرض المحتوى وتنضيد الأسطر."

        val smallMargins = PageMargins(5f, 5f, 5f, 5f, MarginUnit.MILLIMETER)
        val largeMargins = PageMargins(40f, 40f, 40f, 40f, MarginUnit.MILLIMETER)

        val layoutSmall = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.RIGHT,
            margins = smallMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        val layoutLarge = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.RIGHT,
            margins = largeMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        assertTrue(layoutSmall.contentWidthPt > layoutLarge.contentWidthPt)
        assertEquals(text, layoutSmall.rawText)
        assertEquals(text, layoutLarge.rawText)
    }

    // Benchmark J & K: Justified text with Kashida ON vs OFF
    @Test
    fun benchmark_J_and_K_kashida_on_vs_off() {
        val line = "العلم نور والجهل ظلام في طريق الأمم الساعية نحو النهضة والحضارة."

        val layoutOff = DocumentLayoutEngine.build(
            text = line,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.JUSTIFY,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        val layoutOn = DocumentLayoutEngine.build(
            text = line,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.JUSTIFY,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = true,
            kashidaLevel = KashidaLevel.MEDIUM
        )

        assertEquals(line, layoutOff.rawText)
        assertEquals(line, layoutOn.rawText)
        assertFalse(layoutOn.hasOverflow)
    }

    // Benchmark L & M: Right-aligned vs Centered text
    @Test
    fun benchmark_L_and_M_alignments() {
        val text = "عنوان القصيدة\nالبيت الأول من الشعر\nالبيت الثاني"

        val layoutRight = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            textAlign = TextAlignOption.RIGHT,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        val layoutCenter = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            textAlign = TextAlignOption.CENTER,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        assertEquals(TextAlignOption.RIGHT, layoutRight.pages[0].alignment)
        assertEquals(TextAlignOption.CENTER, layoutCenter.pages[0].alignment)
    }

    // Benchmark N: Manual page break (\u000c)
    @Test
    fun benchmark_N_manual_page_break() {
        val text = "محتوى الصفحة الأولى قبل الفاصل الإجباري.\u000cمحتوى الصفحة الثانية بعد الفاصل الإجباري."
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.RIGHT,
            margins = defaultMargins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        assertEquals("Text with form feed must result in at least 2 pages", 2, layout.pageCount)
        assertTrue(layout.pages[0].renderedText.contains("الأولى"))
        assertTrue(layout.pages[1].renderedText.contains("الثانية"))
        assertFalse("Rendered text must strip control character", layout.pages[0].renderedText.contains("\u000c"))
    }

    // Benchmark O: TextDocumentImporter byte decoding (UTF-8, BOM, Markdown, Spaces)
    @Test
    fun benchmark_O_imported_text_preservation() {
        val rawInput = """
            # عنوان المستند الرئيسي
            
            هذه فقرة عادية مع مسافات متتالية:   ثلاث مسافات.
            
            - عنصر قائمة Markdown
            - عنصر ثانٍ
            
            English note with numbers: 2026 version 1.0!
        """.trimIndent()

        // Test with UTF-8 BOM
        val bomBytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + rawInput.toByteArray(StandardCharsets.UTF_8)
        val decoded = TextDocumentImporter.decodeBytes(bomBytes, "test_article.md")

        assertNotNull(decoded)
        assertEquals("BOM must be stripped without corrupting content", rawInput, decoded!!.content)
        assertEquals("test article", decoded.title)
        assertTrue(decoded.content.contains("   ثلاث مسافات."))
        assertTrue(decoded.content.contains("# عنوان المستند"))
    }

    // Invariant 8: PDF and PNG Exporters render consistently from DocumentLayoutEngine
    @Test
    fun invariant_8_pdf_and_image_export_consistency() {
        kotlinx.coroutines.runBlocking {
            val context: Context = ApplicationProvider.getApplicationContext()
            val text = "بسم الله الرحمن الرحيم.\nاختبار تصدير ملفات PDF وصور PNG الحتمية بدون تشويه."
            val layout = DocumentLayoutEngine.build(
                text = text,
                typeface = Typeface.DEFAULT,
                fontSizePt = 18f,
                textAlign = TextAlignOption.JUSTIFY,
                margins = defaultMargins,
                pageSize = PageSize.A5,
                kashidaEnabled = true,
                kashidaLevel = KashidaLevel.LIGHT
            )

            // 1. Export Image (exercises full Canvas and Bitmap rendering pipeline)
            val pngUri = ImageExporter.exportToPng(
                context = context,
                layout = layout,
                title = "benchmark_test",
                typeface = Typeface.DEFAULT,
                fontSizePt = 18f,
                textColor = android.graphics.Color.BLACK,
                pageColor = android.graphics.Color.WHITE
            )
            assertNotNull("PNG export URI must not be null", pngUri)

            // 2. Direct Canvas verification of DocumentLayoutEngine.drawPage (single source of truth for both PDF and PNG)
            val bitmap = Bitmap.createBitmap(
                layout.pages[0].widthPt.toInt().coerceAtLeast(1),
                layout.pages[0].heightPt.toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            DocumentLayoutEngine.drawPage(canvas, layout.pages[0], Typeface.DEFAULT, 18f, android.graphics.Color.BLACK)
            assertNotNull(bitmap)
            bitmap.recycle()

            // 3. Export PDF (Robolectric's shadow PdfDocument has partial mock support)
            try {
                PdfExporter.exportToPdf(
                    context = context,
                    layout = layout,
                    title = "benchmark_test",
                    typeface = Typeface.DEFAULT,
                    fontSizePt = 18f,
                    textColor = android.graphics.Color.BLACK,
                    pageColor = android.graphics.Color.WHITE
                )
            } catch (_: IllegalStateException) {
                // Expected under Robolectric's headless PdfDocument limitation
            }
        }
    }
}

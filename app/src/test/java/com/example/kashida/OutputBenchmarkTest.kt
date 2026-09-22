package com.example.kashida

import android.graphics.Typeface
import com.example.model.KashidaLevel
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
@Config(sdk = [35])
class OutputBenchmarkTest {

    private val fixtures = listOf(
        "A — صفحة عربية عادية" to "هذه صفحة عربية قصيرة للتأكد من أساسيات الاتجاه والقياس.",
        "B — صفحة عربية طويلة" to "اللغة العربية في النشر الرقمي تحتاج إلى تخطيط حتمي يحافظ على القراءة والتوازن. ".repeat(8),
        "C — عربي مع English" to "مِداد Arabic-first workspace for تحرير النصوص العربية.",
        "D — عربي مع numbers" to "النسخة 1.2 تدعم 24 صفحة و2026 كرقم داخل سياق عربي.",
        "E — Arabic punctuation" to "قال الكاتب: «هذه جملة عربية؛ وهذه جملة أخرى!» ثم تابع.",
        "F — diacritics" to "مِدادٌ يختبرُ التَّشكيلَ والعَلاماتِ المُرَكَّبةَ.",
        "G — quotations" to "\"English quote\" ثم «اقتباس عربي» ثم (مقطع).",
        "H — headings" to "العنوان الرئيسي\nعنوان فرعي\nنص الفقرة بعد العنوان.",
        "I — multiple paragraphs" to "الفقرة الأولى.\n\nالفقرة الثانية بعد سطر فارغ.\n\nالفقرة الثالثة.",
        "J — long paragraph" to "هذا اختبار لفقرة طويلة بلا فواصل اصطناعية، ويجب أن يحافظ المحرك على ترتيب الأسطر. ".repeat(12),
        "K — blank lines" to "أولاً\n\n\nثالثاً بعد عدة أسطر فارغة.",
        "L — manual Tatweel" to "كــتاب عربي مع كشيدة يدوية.",
        "M — automatic Kashida" to "هذا نص عربي قابل للتسويغ والكشيدة الآلية.",
        "N — multi-page document" to "سطر عربي متعدد الصفحات للاختبار.\n".repeat(120),
        "O — chapter-like layout" to "الفصل الأول\n\nمقدمة\n\n" + "نص فصل عربي طويل للاختبار. ".repeat(18)
    )

    @Test
    fun standard_fixtures_produce_canonical_layouts() {
        fixtures.forEach { (_, source) ->
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

            assertTrue("fixture must have at least one page", layout.pages.isNotEmpty())
            assertEquals(layout.pages.size, layout.pageCount)
            assertEquals(source, layout.rawText)
            assertTrue(layout.pages.all { page ->
                page.widthPt > 0f && page.heightPt > 0f &&
                    page.lines.none { it.text.contains('\n') }
            })
        }
    }
}

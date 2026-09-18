package com.example

import com.example.data.TextImportCodec
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

class TextImportCodecTest {
    @Test
    fun reads_utf8_arabic_mixed_text_without_normalizing_content() {
        val source = "مِداد عربي\n\nEnglish 123،!?"
        val decoded = TextImportCodec.readUtf8(
            ByteArrayInputStream(source.toByteArray(StandardCharsets.UTF_8))
        )
        assertEquals(source, decoded)
    }

    @Test
    fun treats_markdown_as_plain_editable_text() {
        val source = "# عنوان\n\n**عربي** و English"
        val decoded = TextImportCodec.readUtf8(
            ByteArrayInputStream(source.toByteArray(StandardCharsets.UTF_8))
        )
        assertEquals(source, decoded)
    }

    @Test
    fun reads_long_text_without_an_artificial_small_limit() {
        val source = ("سطر طويل للاختبار 123 English\n").repeat(10_000)
        val decoded = TextImportCodec.readUtf8(
            ByteArrayInputStream(source.toByteArray(StandardCharsets.UTF_8))
        )
        assertEquals(source.length, decoded.length)
        assertEquals(source, decoded)
    }

    @Test
    fun derives_document_title_without_changing_file_content() {
        assertEquals("مقال", TextImportCodec.titleFromDisplayName("مقال.md"))
        assertEquals("مستند مستورد", TextImportCodec.titleFromDisplayName(null))
    }
}

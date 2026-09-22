package com.example

import android.graphics.Typeface
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.AppDatabase
import com.example.export.PreflightDiagnostic
import com.example.export.PreflightEngine
import com.example.export.PreflightSeverity
import com.example.kashida.DocumentLayoutEngine
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
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MidadWorkflowTest {

    @Test
    fun preflight_detects_empty_document_error() {
        val layout = DocumentLayoutEngine.build(
            text = "   \n  \t  ",
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            textAlign = TextAlignOption.RIGHT,
            margins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER),
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        val report = PreflightEngine.inspect(
            layout = layout,
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            pageSize = PageSize.A4,
            margins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER)
        )

        assertFalse("Empty document should not be ready for export", report.isReadyForExport)
        assertTrue("Report must have at least one error", report.errorCount > 0)
        assertTrue(
            "Report must mention empty document",
            report.diagnostics.any { it.title.contains("فارغ") }
        )
    }

    @Test
    fun preflight_passes_for_clean_arabic_document() {
        val text = "بسم الله الرحمن الرحيم. هذا نص تجريبي لتأكيد سلامة التنضيد والصفحات."
        val margins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER)
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            textAlign = TextAlignOption.JUSTIFY,
            margins = margins,
            pageSize = PageSize.A4,
            kashidaEnabled = true,
            kashidaLevel = KashidaLevel.MEDIUM
        )

        val report = PreflightEngine.inspect(
            layout = layout,
            typeface = Typeface.DEFAULT,
            fontSizePt = 18f,
            pageSize = PageSize.A4,
            margins = margins
        )

        assertTrue("Clean document should pass preflight check", report.isReadyForExport)
        assertEquals("Should have zero preflight errors", 0, report.errorCount)
    }

    @Test
    fun preflight_detects_overflow_and_reports_page_and_line() {
        // Force overflow with ultra-wide right and left margins
        val margins = PageMargins(
            topMm = 20f,
            bottomMm = 20f,
            rightMm = 73f,
            leftMm = 73f,
            unit = MarginUnit.MILLIMETER
        )
        val text = "المستند العربي المعاصر الفائق الاتساع"
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 24f,
            textAlign = TextAlignOption.RIGHT,
            margins = margins,
            pageSize = PageSize.A5,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        val report = PreflightEngine.inspect(
            layout = layout,
            typeface = Typeface.DEFAULT,
            fontSizePt = 24f,
            pageSize = PageSize.A5,
            margins = margins
        )

        assertFalse("Overflowing document should fail preflight", report.isReadyForExport)
        assertTrue("Error count must be >= 1", report.errorCount >= 1)
        assertTrue("Diagnostics must mention horizontal overflow", report.diagnostics.any { it.title.contains("تجاوز") })
    }

    @Test
    fun preflight_flags_mixed_bidi_text_as_informational() {
        val text = "هذا التقرير يحتوي على إصدار v2.4 والكلمة الإنجليزية Android للتجربة."
        val margins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER)
        val layout = DocumentLayoutEngine.build(
            text = text,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            textAlign = TextAlignOption.RIGHT,
            margins = margins,
            pageSize = PageSize.A4,
            kashidaEnabled = false,
            kashidaLevel = KashidaLevel.OFF
        )

        val report = PreflightEngine.inspect(
            layout = layout,
            typeface = Typeface.DEFAULT,
            fontSizePt = 16f,
            pageSize = PageSize.A4,
            margins = margins
        )

        assertTrue("Mixed bidi info should not block export", report.isReadyForExport)
        assertTrue("Should contain info diagnostic for BiDi", report.infoCount > 0)
    }

    @Test
    fun room_migration_1_2_preserves_documents_and_creates_custom_fonts() {
        // Create an in-memory database configuration for testing MIGRATION_1_2
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext()
        )
            .name(null) // in-memory
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Version 1 schema with documents table only
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `documents` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `fontId` TEXT NOT NULL,
                            `fontSizePt` REAL NOT NULL,
                            `textColorLong` INTEGER NOT NULL,
                            `pageColorLong` INTEGER NOT NULL,
                            `textAlign` TEXT NOT NULL,
                            `marginTopMm` REAL NOT NULL,
                            `marginBottomMm` REAL NOT NULL,
                            `marginRightMm` REAL NOT NULL,
                            `marginLeftMm` REAL NOT NULL,
                            `marginUnit` TEXT NOT NULL,
                            `paperSize` TEXT NOT NULL,
                            `kashidaEnabled` INTEGER NOT NULL,
                            `kashidaLevel` TEXT NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    // Insert a document in v1
                    db.execSQL(
                        """
                        INSERT INTO `documents` (
                            `title`, `content`, `fontId`, `fontSizePt`, `textColorLong`,
                            `pageColorLong`, `textAlign`, `marginTopMm`, `marginBottomMm`,
                            `marginRightMm`, `marginLeftMm`, `marginUnit`, `paperSize`,
                            `kashidaEnabled`, `kashidaLevel`, `updatedAt`
                        ) VALUES (
                            'وثيقة قديمة', 'محتوى أصلي محفوظ لا يُحذف', 'amiri', 18.0, 0,
                            0, 'JUSTIFY', 20.0, 20.0, 20.0, 20.0, 'MILLIMETER', 'A4',
                            1, 'MEDIUM', 1000
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Execute MIGRATION_1_2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Verify document is preserved
        val docCursor = db.query("SELECT title, content FROM documents WHERE id = 1")
        assertTrue("Document row must exist after migration", docCursor.moveToFirst())
        assertEquals("وثيقة قديمة", docCursor.getString(0))
        assertEquals("محتوى أصلي محفوظ لا يُحذف", docCursor.getString(1))
        docCursor.close()

        // Verify custom_fonts table was created
        val fontCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='custom_fonts'")
        assertTrue("custom_fonts table must exist after migration", fontCursor.moveToFirst())
        fontCursor.close()

        db.close()
    }
}

package com.example.export

import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import com.example.kashida.DocumentLayoutEngine
import com.example.model.PageMargins
import com.example.model.PageSize

enum class PreflightSeverity {
    INFO,
    WARNING,
    ERROR
}

data class PreflightDiagnostic(
    val severity: PreflightSeverity,
    val title: String,
    val description: String,
    val pageIndex: Int? = null,
    val lineIndex: Int? = null
)

data class PreflightReport(
    val isReadyForExport: Boolean,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val diagnostics: List<PreflightDiagnostic>,
    val summaryAr: String
)

object PreflightEngine {

    fun inspect(
        layout: DocumentLayoutEngine.DocumentLayout,
        typeface: Typeface,
        fontSizePt: Float,
        pageSize: PageSize,
        margins: PageMargins
    ): PreflightReport {
        val diagnostics = mutableListOf<PreflightDiagnostic>()

        // 1. Content Emptiness Check
        if (layout.rawText.isBlank()) {
            diagnostics += PreflightDiagnostic(
                severity = PreflightSeverity.ERROR,
                title = "المستند فارغ",
                description = "لا يوجد أي نص لتصديره. يرجى كتابة نص أو استيراد مستند قبل التصدير."
            )
        }

        // 2. Page Geometry Checks
        val leftPt = margins.leftMm * 72f / 25.4f
        val rightPt = margins.rightMm * 72f / 25.4f
        val topPt = margins.topMm * 72f / 25.4f
        val bottomPt = margins.bottomMm * 72f / 25.4f

        if (leftPt + rightPt >= pageSize.widthPt - 20f) {
            diagnostics += PreflightDiagnostic(
                severity = PreflightSeverity.ERROR,
                title = "هوامش أفقية غير صالحة",
                description = "مجموع الهامشين الأيمن والأيسر يتجاوز أو يقارب عرض الصفحة بالكامل."
            )
        }

        if (topPt + bottomPt >= pageSize.heightPt - 20f) {
            diagnostics += PreflightDiagnostic(
                severity = PreflightSeverity.ERROR,
                title = "هوامش رأسية غير صالحة",
                description = "مجموع الهامشين العلوي والسفلي يتجاوز أو يقارب ارتفاع الصفحة بالكامل."
            )
        }

        // 3. Layout Issues from DocumentLayoutEngine (Single source of truth)
        for (issue in layout.issues) {
            when (issue.issueType) {
                DocumentLayoutEngine.LayoutIssue.IssueType.HORIZONTAL_OVERFLOW -> {
                    diagnostics += PreflightDiagnostic(
                        severity = PreflightSeverity.ERROR,
                        title = "تجاوز أفقي لهامش الصفحة",
                        description = issue.message,
                        pageIndex = issue.pageIndex,
                        lineIndex = issue.lineIndexOnPage
                    )
                }
                DocumentLayoutEngine.LayoutIssue.IssueType.VERTICAL_PAGE_OVERFLOW -> {
                    diagnostics += PreflightDiagnostic(
                        severity = PreflightSeverity.ERROR,
                        title = "تجاوز رأسي للصفحة",
                        description = issue.message,
                        pageIndex = issue.pageIndex,
                        lineIndex = issue.lineIndexOnPage
                    )
                }
                else -> {
                    diagnostics += PreflightDiagnostic(
                        severity = PreflightSeverity.WARNING,
                        title = "ملاحظة تنضيد",
                        description = issue.message,
                        pageIndex = issue.pageIndex,
                        lineIndex = issue.lineIndexOnPage
                    )
                }
            }
        }

        // 4. Font and Glyph Availability Check
        val paint = TextPaint().apply {
            this.typeface = typeface
            textSize = fontSizePt
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val missingGlyphs = mutableSetOf<Char>()
            val sampleChars = layout.rawText.filter { !it.isWhitespace() && it != '\n' }.take(500)
            for (char in sampleChars) {
                if (!paint.hasGlyph(char.toString())) {
                    missingGlyphs += char
                }
            }
            if (missingGlyphs.isNotEmpty()) {
                val sampleMissing = missingGlyphs.take(5).joinToString(", ") { "'$it'" }
                diagnostics += PreflightDiagnostic(
                    severity = PreflightSeverity.WARNING,
                    title = "محارف قد لا يدعمها الخط الحالي",
                    description = "الخط المحدد قد يفتقر إلى رسوم المحارف التالية: $sampleMissing. قد يُستخدم خط احتياطي من النظام."
                )
            }
        }

        // 5. Mixed BiDi & Number Check
        val hasMixedText = layout.rawText.any { it in 'a'..'z' || it in 'A'..'Z' } &&
                layout.rawText.any { it in '\u0600'..'\u06FF' }
        if (hasMixedText) {
            diagnostics += PreflightDiagnostic(
                severity = PreflightSeverity.INFO,
                title = "نص ثنائي الاتجاه (عربي ولاتيني)",
                description = "يحتوي المستند على نصوص عربية ولاتينية مدمجة؛ تم تنضيدها وفق خوارزمية الاتجاه الأول القوي (First-Strong RTL)."
            )
        }

        val errorCount = diagnostics.count { it.severity == PreflightSeverity.ERROR }
        val warningCount = diagnostics.count { it.severity == PreflightSeverity.WARNING }
        val infoCount = diagnostics.count { it.severity == PreflightSeverity.INFO }
        val isReady = errorCount == 0

        val summary = when {
            errorCount > 0 -> "توجد $errorCount مشكلات حرجة قد تؤدي إلى قص النص في الملف المصدر."
            warningCount > 0 -> "جاهز للتصدير مع $warningCount تنبيهات فحص."
            else -> "المستند سليم وجاهز للتصدير والطباعة بالكامل."
        }

        return PreflightReport(
            isReadyForExport = isReady,
            errorCount = errorCount,
            warningCount = warningCount,
            infoCount = infoCount,
            diagnostics = diagnostics,
            summaryAr = summary
        )
    }
}

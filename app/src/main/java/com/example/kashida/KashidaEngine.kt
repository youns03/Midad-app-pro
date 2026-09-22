package com.example.kashida

import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.model.KashidaLevel
import com.example.model.PageMargins
import com.example.model.PageSize
import com.example.model.TextAlignOption

/**
 * Layout-time Arabic kashida shaping.
 *
 * The stored document text is never modified by this engine. It operates on
 * the line fragments produced by [DocumentLayoutEngine] and returns render
 * text only.
 */
object KashidaEngine {

    const val TATWEEL = '\u0640'

    private val NON_CONNECTING_FORWARD = setOf(
        'ا', 'أ', 'إ', 'آ', 'ء', 'د', 'ذ', 'ر', 'ز', 'و', 'ؤ', 'ة', 'ى',
        '\u0671', '\u0672', '\u0673', '\u0675', '\u0688', '\u068c',
        '\u068d', '\u068e', '\u0698', '\u06c6', '\u06c7', '\u06c8',
        '\u06cb', '\u06cf'
    )

    fun isDiacritic(c: Char): Boolean =
        c in '\u0610'..'\u061A' ||
        c in '\u064B'..'\u065F' ||
        c == '\u0670' ||
        c in '\u06D6'..'\u06ED'

    fun isArabicLetter(c: Char): Boolean =
        (c in '\u0621'..'\u064A') || (c in '\u0671'..'\u06D3')

    fun stripKashida(text: String): String = text.replace(TATWEEL.toString(), "")

    fun canConnectForward(firstChar: Char, secondChar: Char): Boolean {
        if (!isArabicLetter(firstChar) || !isArabicLetter(secondChar)) return false
        if (firstChar in NON_CONNECTING_FORWARD) return false
        if (firstChar == 'ل' && secondChar in setOf('ا', 'أ', 'إ', 'آ')) return false
        return true
    }

    data class ConnectionPoint(
        val indexInWord: Int,
        val firstChar: Char,
        val secondChar: Char,
        val insertionIndex: Int = indexInWord + 1,
        val aestheticScore: Int = 1
    )

    /**
     * Returns connection points as insertion offsets in the original word.
     * Diacritics attached to the first letter remain before the inserted
     * tatweel, so tashkeel is never displaced.
     */
    fun findConnectionPointsInLine(text: String): List<ConnectionPoint> {
        if (text.length < 2) return emptyList()

        val points = mutableListOf<ConnectionPoint>()
        var i = 0
        while (i < text.length) {
            val first = text[i]
            if (!isArabicLetter(first) || first == TATWEEL) {
                i++
                continue
            }

            // Skip any diacritics attached to the first letter
            var scan = i + 1
            while (scan < text.length && isDiacritic(text[scan])) {
                scan++
            }

            // Skip any existing tatweels (preserving manual kashida)
            while (scan < text.length && (text[scan] == TATWEEL || isDiacritic(text[scan]))) {
                scan++
            }

            val insertionPoint = scan

            if (scan < text.length) {
                val second = text[scan]
                if (isArabicLetter(second) && second != TATWEEL) {
                    if (canConnectForward(first, second)) {
                        var score = 1
                        if (second !in NON_CONNECTING_FORWARD) score += 1
                        if (first in setOf('س', 'ش', 'ص', 'ض', 'ط', 'ظ')) score += 2
                        if (first in setOf('ب', 'ت', 'ث', 'ن', 'ي', 'ئ')) score += 1
                        if (first in setOf('ف', 'ق')) score += 1
                        points += ConnectionPoint(
                            indexInWord = i,
                            firstChar = first,
                            secondChar = second,
                            insertionIndex = insertionPoint,
                            aestheticScore = score
                        )
                    }
                }
            }
            i = scan
        }
        return points
    }

    fun findConnectionPointsInWord(word: String): List<ConnectionPoint> {
        return findConnectionPointsInLine(word)
    }

    /**
     * Shapes one already-wrapped line to consume as much of [deficitPx] as
     * possible without changing its line break. The original line remains
     * untouched; the returned value is render-only.
     *
     * Crucially: existing manual tatweels in [line] are NEVER stripped.
     */
    fun shapeLine(
        line: String,
        deficitPx: Float,
        level: KashidaLevel,
        paint: TextPaint
    ): String {
        if (level == KashidaLevel.OFF || deficitPx <= 0f || line.isBlank()) return line

        val points = findConnectionPointsInLine(line)
        if (points.isEmpty()) return line

        val insertions = IntArray(points.size)
        val maxPerPoint = when (level) {
            KashidaLevel.OFF -> 0
            KashidaLevel.LIGHT -> 8
            KashidaLevel.MEDIUM -> 16
            KashidaLevel.HEAVY -> 32
        }

        var remaining = deficitPx
        var progress = true
        while (remaining > 0.05f && progress) {
            progress = false
            val ordered = points.indices.sortedByDescending { points[it].aestheticScore }
            for (pointIndex in ordered) {
                if (insertions[pointIndex] >= maxPerPoint) continue
                val candidate = buildWithInsertions(line, points, insertions, pointIndex)
                val widthBefore = paint.measureText(buildWithInsertions(line, points, insertions))
                val widthAfter = paint.measureText(candidate)
                val delta = widthAfter - widthBefore
                if (delta > 0f && delta <= remaining + 0.01f) {
                    insertions[pointIndex]++
                    remaining -= delta
                    progress = true
                }
                if (remaining <= 0.05f) break
            }
        }

        return buildWithInsertions(line, points, insertions)
    }

    private fun buildWithInsertions(
        text: String,
        points: List<ConnectionPoint>,
        insertions: IntArray,
        extraPoint: Int? = null
    ): String {
        val counts = insertions.copyOf()
        if (extraPoint != null) counts[extraPoint]++
        val totalExtra = counts.sum()
        if (totalExtra == 0) return text

        val sb = StringBuilder(text.length + totalExtra)
        for (i in text.indices) {
            sb.append(text[i])
            val pointIndex = points.indexOfFirst { it.insertionIndex == i + 1 }
            if (pointIndex >= 0) repeat(counts[pointIndex]) { sb.append(TATWEEL) }
        }
        return sb.toString()
    }

    fun applyManualKashidaToWord(
        cleanWord: String,
        connectionIndex: Int,
        tatweelCount: Int
    ): String {
        if (connectionIndex !in cleanWord.indices) return cleanWord
        val count = tatweelCount.coerceIn(0, 10)
        val points = findConnectionPointsInLine(cleanWord)
        val point = points.firstOrNull { it.indexInWord == connectionIndex }
            ?: return cleanWord
        return cleanWord.substring(0, point.insertionIndex) +
            TATWEEL.toString().repeat(count) +
            cleanWord.substring(point.insertionIndex)
    }
}

/**
 * Canonical document layout used by preview and export.
 * Document units are points; screen dp/sp conversion belongs to UI only.
 */
object DocumentLayoutEngine {

    data class LayoutIssue(
        val pageIndex: Int,
        val lineIndexOnPage: Int,
        val issueType: IssueType,
        val message: String,
        val overflowAmountPt: Float = 0f
    ) {
        enum class IssueType {
            HORIZONTAL_OVERFLOW,
            VERTICAL_PAGE_OVERFLOW,
            SUSPICIOUS_GEOMETRY,
            INVALID_LINE
        }
    }

    data class LayoutLine(
        val rawStart: Int,
        val rawEnd: Int,
        val text: String,
        val topPt: Float,
        val bottomPt: Float,
        val baselinePt: Float,
        val widthPt: Float,
        val paragraphEnd: Boolean,
        val isOverflowing: Boolean = false,
        val overflowAmountPt: Float = 0f
    )

    data class PageLayout(
        val index: Int,
        val lines: List<LayoutLine>,
        val renderedText: String,
        val widthPt: Float,
        val heightPt: Float,
        val contentWidthPt: Float,
        val contentHeightPt: Float,
        val leftMarginPt: Float,
        val topMarginPt: Float,
        val alignment: TextAlignOption,
        val issues: List<LayoutIssue> = emptyList()
    ) {
        val hasOverflow: Boolean get() = issues.any {
            it.issueType == LayoutIssue.IssueType.HORIZONTAL_OVERFLOW ||
            it.issueType == LayoutIssue.IssueType.VERTICAL_PAGE_OVERFLOW
        }
    }

    data class DocumentLayout(
        val pages: List<PageLayout>,
        val pageWidthPt: Float,
        val pageHeightPt: Float,
        val contentWidthPt: Float,
        val contentHeightPt: Float,
        val rawText: String,
        val issues: List<LayoutIssue> = emptyList()
    ) {
        val pageCount: Int get() = pages.size
        val hasOverflow: Boolean get() = issues.any {
            it.issueType == LayoutIssue.IssueType.HORIZONTAL_OVERFLOW ||
            it.issueType == LayoutIssue.IssueType.VERTICAL_PAGE_OVERFLOW
        }
        val totalIssuesCount: Int get() = issues.size
    }

    fun build(
        text: String,
        typeface: Typeface,
        fontSizePt: Float,
        textAlign: TextAlignOption,
        margins: PageMargins,
        pageSize: PageSize,
        kashidaEnabled: Boolean,
        kashidaLevel: KashidaLevel,
        lineSpacingMultiplier: Float = 1.35f
    ): DocumentLayout {
        val pageWidth = pageSize.widthPt
        val pageHeight = pageSize.heightPt
        val left = margins.leftMm * 72f / 25.4f
        val right = margins.rightMm * 72f / 25.4f
        val top = margins.topMm * 72f / 25.4f
        val bottom = margins.bottomMm * 72f / 25.4f
        val contentWidth = (pageWidth - left - right).coerceAtLeast(1f)
        val contentHeight = (pageHeight - top - bottom).coerceAtLeast(1f)

        val paint = TextPaint().apply {
            this.typeface = typeface
            textSize = fontSizePt
            isAntiAlias = true
        }
        val baseLayout = createStaticLayout(text, paint, contentWidth.toInt(), textAlign, lineSpacingMultiplier)
        val lines = mutableListOf<LayoutLine>()

        for (i in 0 until baseLayout.lineCount) {
            val rawStart = baseLayout.getLineStart(i)
            val rawEndWithBreak = baseLayout.getLineEnd(i)
            val rawEnd = rawEndWithBreak
                .coerceAtMost(text.length)
                .let { end -> if (end > rawStart && text[end - 1] == '\n') end - 1 else end }
                .let { end -> if (end > rawStart && text[end - 1] == '\r') end - 1 else end }
            val rawLine = text.substring(rawStart, rawEnd)
            val isLastLine = i == baseLayout.lineCount - 1
            val hasParagraphBreak = rawEndWithBreak > rawEnd
            val rawWidth = paint.measureText(rawLine)
            val canJustify = textAlign == TextAlignOption.JUSTIFY && !isLastLine && !hasParagraphBreak
            val rendered = if (kashidaEnabled && canJustify) {
                KashidaEngine.shapeLine(
                    rawLine,
                    (contentWidth - rawWidth).coerceAtLeast(0f),
                    kashidaLevel,
                    paint
                )
            } else rawLine
            val width = paint.measureText(rendered)
            val overflowAmount = (width - contentWidth).coerceAtLeast(0f)
            val isOverflowing = overflowAmount > 0.5f

            lines += LayoutLine(
                rawStart = rawStart,
                rawEnd = rawEnd,
                text = rendered,
                topPt = baseLayout.getLineTop(i).toFloat(),
                bottomPt = baseLayout.getLineBottom(i).toFloat(),
                baselinePt = baseLayout.getLineBaseline(i).toFloat(),
                widthPt = width,
                paragraphEnd = hasParagraphBreak,
                isOverflowing = isOverflowing,
                overflowAmountPt = overflowAmount
            )
        }

        val pages = paginate(
            lines = lines,
            rawText = text,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            contentWidth = contentWidth,
            contentHeight = contentHeight,
            left = left,
            top = top,
            alignment = textAlign
        )

        val allIssues = pages.flatMap { it.issues }

        return DocumentLayout(
            pages = pages,
            pageWidthPt = pageWidth,
            pageHeightPt = pageHeight,
            contentWidthPt = contentWidth,
            contentHeightPt = contentHeight,
            rawText = text,
            issues = allIssues
        )
    }

    private fun paginate(
        lines: List<LayoutLine>,
        rawText: String,
        pageWidth: Float,
        pageHeight: Float,
        contentWidth: Float,
        contentHeight: Float,
        left: Float,
        top: Float,
        alignment: TextAlignOption
    ): List<PageLayout> {
        if (lines.isEmpty()) {
            return listOf(
                PageLayout(0, emptyList(), "", pageWidth, pageHeight, contentWidth, contentHeight, left, top, alignment)
            )
        }

        val pages = mutableListOf<PageLayout>()
        var pageLines = mutableListOf<LayoutLine>()
        var usedHeight = 0f
        var pageIndex = 0

        fun flush() {
            val normalized = pageLines.map {
                it.copy(
                    topPt = it.topPt - (pageLines.firstOrNull()?.topPt ?: 0f),
                    bottomPt = it.bottomPt - (pageLines.firstOrNull()?.topPt ?: 0f),
                    baselinePt = it.baselinePt - (pageLines.firstOrNull()?.topPt ?: 0f)
                )
            }
            val pageIssues = mutableListOf<LayoutIssue>()
            normalized.forEachIndexed { lineIdx, line ->
                if (line.isOverflowing) {
                    pageIssues += LayoutIssue(
                        pageIndex = pageIndex,
                        lineIndexOnPage = lineIdx,
                        issueType = LayoutIssue.IssueType.HORIZONTAL_OVERFLOW,
                        message = "السطر ${lineIdx + 1} في الصفحة ${pageIndex + 1} يتجاوز عرض المحتوى بمقدار %.1f نقطة".format(line.overflowAmountPt),
                        overflowAmountPt = line.overflowAmountPt
                    )
                }
            }
            if (usedHeight > contentHeight + 0.5f) {
                pageIssues += LayoutIssue(
                    pageIndex = pageIndex,
                    lineIndexOnPage = normalized.lastIndex.coerceAtLeast(0),
                    issueType = LayoutIssue.IssueType.VERTICAL_PAGE_OVERFLOW,
                    message = "الصفحة ${pageIndex + 1} تتجاوز الارتفاع القابل للطباعة بمقدار %.1f نقطة".format(usedHeight - contentHeight),
                    overflowAmountPt = usedHeight - contentHeight
                )
            }

            pages += PageLayout(
                index = pageIndex++,
                lines = normalized,
                renderedText = normalized.joinToString("\n") { it.text },
                widthPt = pageWidth,
                heightPt = pageHeight,
                contentWidthPt = contentWidth,
                contentHeightPt = contentHeight,
                leftMarginPt = left,
                topMarginPt = top,
                alignment = alignment,
                issues = pageIssues
            )
            pageLines = mutableListOf()
            usedHeight = 0f
        }

        for (line in lines) {
            val lineHeight = (line.bottomPt - line.topPt).coerceAtLeast(1f)
            if (pageLines.isNotEmpty() && usedHeight + lineHeight > contentHeight) {
                flush()
            }
            pageLines += line
            usedHeight += lineHeight
        }
        if (pageLines.isNotEmpty()) flush()

        return pages
    }

    fun createStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: TextAlignOption,
        lineSpacingMultiplier: Float = 1.35f
    ): StaticLayout {
        val layoutAlignment = when (alignment) {
            TextAlignOption.RIGHT, TextAlignOption.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
            TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignOption.LEFT -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val textDirection = when (alignment) {
            TextAlignOption.LEFT -> TextDirectionHeuristics.FIRSTSTRONG_LTR
            else -> TextDirectionHeuristics.FIRSTSTRONG_RTL
        }
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
                .setAlignment(layoutAlignment)
                .setTextDirection(textDirection)
                .setLineSpacing(0f, lineSpacingMultiplier)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text, paint, width.coerceAtLeast(1), layoutAlignment,
                lineSpacingMultiplier, 0f, false
            )
        }
    }

    /**
     * Draws the already-wrapped lines in [page]. Each consumer uses this
     * renderer, so rendering cannot introduce a second line break or layout.
     */
    fun drawPage(
        canvas: android.graphics.Canvas,
        page: PageLayout,
        typeface: Typeface,
        fontSizePt: Float,
        textColor: Int = android.graphics.Color.BLACK
    ) {
        val paint = TextPaint().apply {
            this.typeface = typeface
            textSize = fontSizePt
            color = textColor
            isAntiAlias = true
        }
        canvas.save()
        canvas.translate(page.leftMarginPt, page.topMarginPt)
        page.lines.forEach { line ->
            if (line.text.isEmpty()) return@forEach
            // Keep layout width strictly bounded to page.contentWidthPt.
            // Never artificially expand, which would push RTL text outside margins!
            val lineWidth = page.contentWidthPt.toInt().coerceAtLeast(1)
            val lineLayout = createStaticLayout(line.text, paint, lineWidth, page.alignment, 1f)
            canvas.save()
            canvas.translate(0f, line.topPt)
            lineLayout.draw(canvas)
            canvas.restore()
        }
        canvas.restore()
    }
}

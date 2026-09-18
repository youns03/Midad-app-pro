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
    fun findConnectionPointsInWord(word: String): List<ConnectionPoint> {
        val cleanWord = stripKashida(word)
        if (cleanWord.length < 2) return emptyList()

        val points = mutableListOf<ConnectionPoint>()
        var i = 0
        while (i < cleanWord.length) {
            val first = cleanWord[i]
            if (!isArabicLetter(first)) {
                i++
                continue
            }

            var insertion = i + 1
            while (insertion < cleanWord.length && isDiacritic(cleanWord[insertion])) {
                insertion++
            }

            if (insertion < cleanWord.length) {
                val second = cleanWord[insertion]
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
                        insertionIndex = insertion,
                        aestheticScore = score
                    )
                }
            }
            i = insertion
        }
        return points
    }

    /**
     * Shapes one already-wrapped line to consume as much of [deficitPx] as
     * possible without changing its line break. The original line remains
     * untouched; the returned value is render-only.
     */
    fun shapeLine(
        line: String,
        deficitPx: Float,
        level: KashidaLevel,
        paint: TextPaint
    ): String {
        if (level == KashidaLevel.OFF || deficitPx <= 0f || line.isBlank()) return line

        val clean = stripKashida(line)
        val points = findConnectionPointsInWord(clean)
        if (points.isEmpty()) return clean

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
                val candidate = buildWithInsertions(clean, points, insertions, pointIndex)
                val widthBefore = paint.measureText(buildWithInsertions(clean, points, insertions))
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

        return buildWithInsertions(clean, points, insertions)
    }

    private fun buildWithInsertions(
        text: String,
        points: List<ConnectionPoint>,
        insertions: IntArray,
        extraPoint: Int? = null
    ): String {
        val counts = insertions.copyOf()
        if (extraPoint != null) counts[extraPoint]++
        val sb = StringBuilder(text.length + counts.sum())
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
        val points = findConnectionPointsInWord(cleanWord)
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

    data class LayoutLine(
        val rawStart: Int,
        val rawEnd: Int,
        val text: String,
        val topPt: Float,
        val bottomPt: Float,
        val baselinePt: Float,
        val widthPt: Float,
        val paragraphEnd: Boolean
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
        val alignment: TextAlignOption
    )

    data class DocumentLayout(
        val pages: List<PageLayout>,
        val pageWidthPt: Float,
        val pageHeightPt: Float,
        val contentWidthPt: Float,
        val contentHeightPt: Float,
        val rawText: String
    ) {
        val pageCount: Int get() = pages.size
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
            lines += LayoutLine(
                rawStart = rawStart,
                rawEnd = rawEnd,
                text = rendered,
                topPt = baseLayout.getLineTop(i).toFloat(),
                bottomPt = baseLayout.getLineBottom(i).toFloat(),
                baselinePt = baseLayout.getLineBaseline(i).toFloat(),
                widthPt = width,
                paragraphEnd = hasParagraphBreak
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

        return DocumentLayout(
            pages = pages,
            pageWidthPt = pageWidth,
            pageHeightPt = pageHeight,
            contentWidthPt = contentWidth,
            contentHeightPt = contentHeight,
            rawText = text
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
                alignment = alignment
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
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
                .setAlignment(layoutAlignment)
                .setTextDirection(TextDirectionHeuristics.RTL)
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
            val lineWidth = maxOf(page.contentWidthPt, paint.measureText(line.text)).toInt()
            val lineLayout = createStaticLayout(line.text, paint, lineWidth, page.alignment, 1f)
            canvas.save()
            canvas.translate(0f, line.topPt)
            lineLayout.draw(canvas)
            canvas.restore()
        }
        canvas.restore()
    }
}

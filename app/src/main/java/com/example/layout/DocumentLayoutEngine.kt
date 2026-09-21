package com.example.layout

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.kashida.KashidaEngine
import com.example.model.KashidaLevel
import com.example.model.TextAlignOption

/**
 * The Central Layout Engine and Single Source of Truth for document layout calculation,
 * text measurement, line wrapping, and line-based pagination.
 *
 * Responsibilities:
 * 1. Takes raw Document text + [LayoutSettings].
 * 2. Formats text with Kashida (without altering raw user text) when justified.
 * 3. Uses Android's [StaticLayout] for Arabic shaping, BiDi, RTL, and precise font metrics.
 * 4. Extracts individual [TextLine]s preserving exact metrics and empty lines.
 * 5. Paginates lines cleanly without slicing lines across pages.
 * 6. Provides unified page rendering for PDF export, image export, and Compose previews.
 */
object DocumentLayoutEngine {

    /**
     * Calculates the full [DocumentLayout] with line-based pagination and unified formatting.
     */
    fun calculateLayout(
        text: String,
        settings: LayoutSettings,
        kashidaEnabled: Boolean = settings.kashidaEnabled,
        kashidaLevel: KashidaLevel = settings.kashidaLevel
    ): DocumentLayout {
        val effectiveSettings = settings.copy(
            kashidaEnabled = kashidaEnabled,
            kashidaLevel = kashidaLevel
        )

        val pageWidthPt = effectiveSettings.pageSize.widthPt
        val pageHeightPt = effectiveSettings.pageSize.heightPt

        val leftMarginPt = DocumentUnits.mmToPt(effectiveSettings.margins.leftMm)
        val rightMarginPt = DocumentUnits.mmToPt(effectiveSettings.margins.rightMm)
        val topMarginPt = DocumentUnits.mmToPt(effectiveSettings.margins.topMm)
        val bottomMarginPt = DocumentUnits.mmToPt(effectiveSettings.margins.bottomMm)

        val contentWidthPt = (pageWidthPt - (leftMarginPt + rightMarginPt)).coerceAtLeast(50f)
        val contentHeightPt = (pageHeightPt - (topMarginPt + bottomMarginPt)).coerceAtLeast(50f)

        // Single Source of Truth for Kashida formatting:
        // Raw text is preserved; renderedText is calculated once here for layout and rendering.
        val renderedText = if (kashidaEnabled && effectiveSettings.textAlign == TextAlignOption.JUSTIFY) {
            KashidaEngine.applySmartKashida(text, kashidaLevel)
        } else {
            text
        }

        // If the document is completely empty, produce a single empty page
        if (renderedText.isEmpty()) {
            return DocumentLayout(
                pageWidthPt = pageWidthPt,
                pageHeightPt = pageHeightPt,
                contentWidthPt = contentWidthPt,
                contentHeightPt = contentHeightPt,
                marginTopPt = topMarginPt,
                marginBottomPt = bottomMarginPt,
                marginRightPt = rightMarginPt,
                marginLeftPt = leftMarginPt,
                pages = listOf(
                    PageLayout(
                        pageIndex = 0,
                        widthPt = pageWidthPt,
                        heightPt = pageHeightPt,
                        marginLeftPt = leftMarginPt,
                        marginTopPt = topMarginPt,
                        marginRightPt = rightMarginPt,
                        marginBottomPt = bottomMarginPt,
                        contentWidthPt = contentWidthPt,
                        contentHeightPt = contentHeightPt,
                        lines = emptyList(),
                        contentUsedHeightPt = 0f
                    )
                ),
                rawText = text,
                renderedText = renderedText,
                settings = effectiveSettings
            )
        }

        val textPaint = TextPaint().apply {
            this.typeface = effectiveSettings.typeface
            this.textSize = effectiveSettings.fontSizePt
            this.color = effectiveSettings.textColor
            this.isAntiAlias = true
        }

        val layoutAlignment = when (effectiveSettings.textAlign) {
            TextAlignOption.RIGHT, TextAlignOption.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
            TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignOption.LEFT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val printableWidthInt = contentWidthPt.toInt().coerceAtLeast(1)

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(renderedText, 0, renderedText.length, textPaint, printableWidthInt)
                .setAlignment(layoutAlignment)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setLineSpacing(effectiveSettings.lineSpacingExtraPt, effectiveSettings.lineSpacingMultiplier)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                renderedText,
                textPaint,
                printableWidthInt,
                layoutAlignment,
                effectiveSettings.lineSpacingMultiplier,
                effectiveSettings.lineSpacingExtraPt,
                false
            )
        }

        // Line-based pagination: guaranteed never to split a line across two pages
        val pages = mutableListOf<PageLayout>()
        val currentPageLines = mutableListOf<TextLine>()
        var currentYOnPage = 0f
        var pageIndex = 0

        for (i in 0 until staticLayout.lineCount) {
            val startOffset = staticLayout.getLineStart(i)
            val endOffset = staticLayout.getLineEnd(i)
            val rawLineText = renderedText.substring(startOffset, endOffset)
            val lineText = rawLineText.trimEnd('\r', '\n')

            val layoutTop = staticLayout.getLineTop(i).toFloat()
            val layoutBottom = staticLayout.getLineBottom(i).toFloat()
            val layoutBaseline = staticLayout.getLineBaseline(i).toFloat()
            val lineHeight = layoutBottom - layoutTop
            val baselineOffset = layoutBaseline - layoutTop
            val lineWidth = staticLayout.getLineWidth(i)
            val lineLeft = staticLayout.getLineLeft(i)

            // If adding this line exceeds page content height and current page is not empty:
            // Move the entire line to the next page!
            if (currentPageLines.isNotEmpty() && (currentYOnPage + lineHeight) > contentHeightPt) {
                pages.add(
                    PageLayout(
                        pageIndex = pageIndex,
                        widthPt = pageWidthPt,
                        heightPt = pageHeightPt,
                        marginLeftPt = leftMarginPt,
                        marginTopPt = topMarginPt,
                        marginRightPt = rightMarginPt,
                        marginBottomPt = bottomMarginPt,
                        contentWidthPt = contentWidthPt,
                        contentHeightPt = contentHeightPt,
                        lines = currentPageLines.toList(),
                        contentUsedHeightPt = currentYOnPage
                    )
                )
                pageIndex++
                currentPageLines.clear()
                currentYOnPage = 0f
            }

            val lineTopOnPage = currentYOnPage
            val lineBottomOnPage = currentYOnPage + lineHeight
            val lineBaselineOnPage = currentYOnPage + baselineOffset

            currentPageLines.add(
                TextLine(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    text = lineText,
                    topPt = lineTopOnPage,
                    baselinePt = lineBaselineOnPage,
                    bottomPt = lineBottomOnPage,
                    widthPt = lineWidth,
                    leftPt = lineLeft,
                    lineIndexInDocument = i
                )
            )

            currentYOnPage += lineHeight
        }

        if (currentPageLines.isNotEmpty() || pages.isEmpty()) {
            pages.add(
                PageLayout(
                    pageIndex = pageIndex,
                    widthPt = pageWidthPt,
                    heightPt = pageHeightPt,
                    marginLeftPt = leftMarginPt,
                    marginTopPt = topMarginPt,
                    marginRightPt = rightMarginPt,
                    marginBottomPt = bottomMarginPt,
                    contentWidthPt = contentWidthPt,
                    contentHeightPt = contentHeightPt,
                    lines = currentPageLines.toList(),
                    contentUsedHeightPt = currentYOnPage
                )
            )
        }

        return DocumentLayout(
            pageWidthPt = pageWidthPt,
            pageHeightPt = pageHeightPt,
            contentWidthPt = contentWidthPt,
            contentHeightPt = contentHeightPt,
            marginTopPt = topMarginPt,
            marginBottomPt = bottomMarginPt,
            marginRightPt = rightMarginPt,
            marginLeftPt = leftMarginPt,
            pages = pages,
            rawText = text,
            renderedText = renderedText,
            settings = effectiveSettings
        )
    }

    /**
     * Unified page drawing logic executed identically by PDF export, PNG export, and Compose preview.
     * Renders directly onto an Android [Canvas] at native PDF point coordinates.
     */
    fun drawPage(
        canvas: Canvas,
        pageIndex: Int,
        layout: DocumentLayout,
        settings: LayoutSettings = layout.settings ?: LayoutSettings(),
        drawBackground: Boolean = true,
        drawFooter: Boolean = true
    ) {
        val page = layout.pages.getOrNull(pageIndex) ?: return
        val activeSettings = layout.settings ?: settings

        // 1. Page background
        if (drawBackground) {
            val bgPaint = Paint().apply {
                color = activeSettings.pageColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, layout.pageWidthPt, layout.pageHeightPt, bgPaint)
        }

        // 2. Lines rendering
        val textPaint = TextPaint().apply {
            this.typeface = activeSettings.typeface
            this.textSize = activeSettings.fontSizePt
            this.color = activeSettings.textColor
            this.isAntiAlias = true
        }

        for (line in page.lines) {
            if (line.text.isEmpty()) continue // Empty line preserves vertical advance

            val baselineY = layout.marginTopPt + line.baselinePt

            when (activeSettings.textAlign) {
                TextAlignOption.RIGHT, TextAlignOption.JUSTIFY -> {
                    textPaint.textAlign = Paint.Align.RIGHT
                    val x = layout.marginLeftPt + layout.contentWidthPt
                    canvas.drawText(line.text, x, baselineY, textPaint)
                }
                TextAlignOption.LEFT -> {
                    textPaint.textAlign = Paint.Align.LEFT
                    val x = layout.marginLeftPt
                    canvas.drawText(line.text, x, baselineY, textPaint)
                }
                TextAlignOption.CENTER -> {
                    textPaint.textAlign = Paint.Align.CENTER
                    val x = layout.marginLeftPt + (layout.contentWidthPt / 2f)
                    canvas.drawText(line.text, x, baselineY, textPaint)
                }
            }
        }

        // 3. Page numbering footer
        if (drawFooter && layout.pages.size > 1) {
            val footerPaint = TextPaint().apply {
                this.typeface = Typeface.DEFAULT
                this.textSize = 9f
                this.color = AndroidColor.argb(120, 100, 100, 100)
                this.isAntiAlias = true
                this.textAlign = Paint.Align.CENTER
            }
            val footerY = layout.pageHeightPt - (layout.marginBottomPt / 2f)
            canvas.drawText(
                "${pageIndex + 1} / ${layout.pages.size}",
                layout.pageWidthPt / 2f,
                footerY,
                footerPaint
            )
        }
    }
}

package com.example.layout

/**
 * Immutable layout model representing the completely paginated and formatted document.
 * Acts as the Single Source of Truth for:
 * - On-screen Compose Page Previews
 * - PDF Document Rendering
 * - Image (PNG) Export
 */
data class DocumentLayout(
    val pageWidthPt: Float,
    val pageHeightPt: Float,
    val contentWidthPt: Float,
    val contentHeightPt: Float,
    val marginTopPt: Float = 0f,
    val marginBottomPt: Float = 0f,
    val marginRightPt: Float = 0f,
    val marginLeftPt: Float = 0f,
    val pages: List<PageLayout> = emptyList(),
    val rawText: String = "",
    val renderedText: String = "",
    val settings: LayoutSettings? = null
) {
    val pageCount: Int get() = pages.size.coerceAtLeast(1)
    val totalLinesCount: Int get() = pages.sumOf { it.lines.size }
}

/**
 * Represents a single physical page in the layout, containing only the lines that fit on it.
 * Explicitly records exact physical dimensions, margins, and content boundaries in points (pt).
 */
data class PageLayout(
    val pageIndex: Int,
    val widthPt: Float = 0f,
    val heightPt: Float = 0f,
    val marginLeftPt: Float = 0f,
    val marginTopPt: Float = 0f,
    val marginRightPt: Float = 0f,
    val marginBottomPt: Float = 0f,
    val contentWidthPt: Float = 0f,
    val contentHeightPt: Float = 0f,
    val lines: List<TextLine> = emptyList(),
    val contentUsedHeightPt: Float = 0f
) {
    val lineCount: Int get() = lines.size
    val pageWidthPt: Float get() = widthPt
    val pageHeightPt: Float get() = heightPt
}

/**
 * Represents a single rendered line of text, with exact layout coordinates in points (pt).
 * Coordinates topPt, baselinePt, bottomPt, and leftPt are relative to the page's printable content area.
 */
data class TextLine(
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val topPt: Float,
    val baselinePt: Float,
    val bottomPt: Float,
    val widthPt: Float,
    val leftPt: Float = 0f,
    val lineIndexInDocument: Int = 0
) {
    val heightPt: Float get() = bottomPt - topPt
    val rightPt: Float get() = leftPt + widthPt
}

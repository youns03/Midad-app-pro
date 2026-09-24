package com.example.typography

import com.example.model.BaseDirection
import com.example.model.KashidaLevel
import com.example.model.TextAlignOption

/**
 * Foundation abstractions for Arabic typography and future rich styling.
 * Preserves full backward-compatibility with plain-string documents while
 * establishing safe extension points for character and paragraph styling.
 */
data class CharacterStyle(
    val fontId: String? = null,
    val fontSizePt: Float? = null,
    val textColorLong: Long? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false
)

data class ParagraphStyle(
    val alignment: TextAlignOption = TextAlignOption.JUSTIFY,
    val baseDirection: BaseDirection = BaseDirection.RTL,
    val lineSpacingMultiplier: Float = 1.35f,
    val paragraphSpacingPt: Float = 6f,
    val kashidaLevel: KashidaLevel = KashidaLevel.MEDIUM
)

data class PageSemantics(
    val manualPageBreakChar: Char = '\u000c',
    val pageNumberingEnabled: Boolean = false,
    val headerText: String = "",
    val footerText: String = ""
)

data class DocumentStyle(
    val defaultParagraphStyle: ParagraphStyle = ParagraphStyle(),
    val defaultCharacterStyle: CharacterStyle = CharacterStyle(),
    val pageSemantics: PageSemantics = PageSemantics()
)

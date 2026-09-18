package com.example.data

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Reads user-selected text files without normalizing or changing their content. */
object TextImportCodec {
    fun isSupportedFileName(displayName: String?): Boolean {
        val name = displayName?.trim()?.lowercase() ?: return false
        return name.endsWith(".txt") || name.endsWith(".md")
    }

    fun readUtf8(input: InputStream): String {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return InputStreamReader(input, decoder).buffered().use { it.readText() }
    }

    fun titleFromDisplayName(displayName: String?): String =
        displayName?.substringBeforeLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() } ?: "مستند مستورد"
}

package com.example.data

import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/** Reads user-selected text files without normalizing or changing their content. */
object TextImportCodec {
    fun readUtf8(input: InputStream): String =
        InputStreamReader(input, StandardCharsets.UTF_8).buffered().use { it.readText() }

    fun titleFromDisplayName(displayName: String?): String =
        displayName?.substringBeforeLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() } ?: "مستند مستورد"
}

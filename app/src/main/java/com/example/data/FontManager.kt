package com.example.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import com.example.model.FontItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FontManager(private val context: Context, private val customFontDao: CustomFontDao) {

    // Pre-bundled static Arabic Google Fonts installed in res/font/
    val bundledFonts: List<FontItem> = listOf(
        FontItem(
            id = "amiri",
            nameAr = "أميري (نسخ كلاسيكي/قرآني)",
            fontFamily = FontFamily(Font(R.font.amiri, FontWeight.Normal))
        ),
        FontItem(
            id = "cairo",
            nameAr = "كايرو (عصري هندسي)",
            fontFamily = FontFamily(Font(R.font.cairo, FontWeight.Normal))
        ),
        FontItem(
            id = "tajawal",
            nameAr = "تجوال (أنيق ومريح)",
            fontFamily = FontFamily(Font(R.font.tajawal, FontWeight.Normal))
        ),
        FontItem(
            id = "aref_ruqaa",
            nameAr = "رقعة (خط الرقعة الفني)",
            fontFamily = FontFamily(Font(R.font.aref_ruqaa, FontWeight.Normal))
        ),
        FontItem(
            id = "system",
            nameAr = "خط النظام الافتراضي",
            fontFamily = FontFamily.Default
        )
    )

    private val fontsDir: File
        get() {
            val dir = File(context.filesDir, "imported_fonts")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /**
     * Resolves display name from Uri.
     */
    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) it.substring(cut + 1) else it
            }
        }
        return result ?: "custom_font_${System.currentTimeMillis()}.ttf"
    }

    /**
     * Imports a user-selected font (.ttf / .otf) from device storage,
     * copies it to app's internal private storage, validates it, and saves it to Room.
     */
    suspend fun importFontFromUri(uri: Uri): FontItem? = withContext(Dispatchers.IO) {
        try {
            val originalName = getFileNameFromUri(uri)
            val extension = when {
                originalName.endsWith(".otf", ignoreCase = true) -> ".otf"
                else -> ".ttf"
            }
            val baseName = originalName.substringBeforeLast(".")
            val safeId = "font_${System.currentTimeMillis()}"
            val targetFile = File(fontsDir, "$safeId$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                return@withContext null
            }

            // Validate that the file is a readable Typeface
            val typeface = Typeface.createFromFile(targetFile)
            if (typeface == null) {
                targetFile.delete()
                return@withContext null
            }

            val fontEntity = CustomFontEntity(
                id = safeId,
                name = baseName,
                filePath = targetFile.absolutePath,
                dateAdded = System.currentTimeMillis()
            )
            customFontDao.insertCustomFont(fontEntity)

            val customFontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
            FontItem(
                id = safeId,
                nameAr = "$baseName (مستورد)",
                fontFamily = customFontFamily,
                isCustom = true,
                filePath = targetFile.absolutePath
            )
        } catch (e: Exception) {
            Log.e("FontManager", "Failed to import font from uri: $uri", e)
            null
        }
    }

    /**
     * Loads custom fonts stored in Room into FontItem models.
     */
    suspend fun loadCustomFonts(): List<FontItem> = withContext(Dispatchers.IO) {
        val entities = customFontDao.getAllCustomFontsList()
        entities.mapNotNull { entity ->
            val file = File(entity.filePath)
            if (file.exists() && file.canRead()) {
                try {
                    val typeface = Typeface.createFromFile(file)
                    val customFontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
                    FontItem(
                        id = entity.id,
                        nameAr = "${entity.name} (مستورد)",
                        fontFamily = customFontFamily,
                        isCustom = true,
                        filePath = entity.filePath
                    )
                } catch (e: Exception) {
                    Log.e("FontManager", "Error reading font file: ${entity.filePath}", e)
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Deletes an imported custom font from local database and disk.
     */
    suspend fun deleteCustomFont(id: String) = withContext(Dispatchers.IO) {
        val entities = customFontDao.getAllCustomFontsList()
        val found = entities.find { it.id == id }
        if (found != null) {
            val file = File(found.filePath)
            if (file.exists()) file.delete()
            customFontDao.deleteCustomFont(id)
        }
    }

    /**
     * Gets native Android Typeface for PDF / Canvas drawing given a font ID.
     */
    fun getNativeTypeface(fontId: String, customFilePath: String? = null): Typeface {
        return try {
            if (!customFilePath.isNullOrBlank()) {
                val file = File(customFilePath)
                if (file.exists()) return Typeface.createFromFile(file)
            }
            when (fontId) {
                "amiri" -> androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiri) ?: Typeface.SERIF
                "cairo" -> androidx.core.content.res.ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.SANS_SERIF
                "tajawal" -> androidx.core.content.res.ResourcesCompat.getFont(context, R.font.tajawal) ?: Typeface.SANS_SERIF
                "aref_ruqaa" -> androidx.core.content.res.ResourcesCompat.getFont(context, R.font.aref_ruqaa) ?: Typeface.SERIF
                else -> Typeface.DEFAULT
            }
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
}

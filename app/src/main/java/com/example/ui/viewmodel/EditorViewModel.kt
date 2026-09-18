package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DocumentEntity
import com.example.data.DocumentRepository
import com.example.data.FontManager
import com.example.export.ImageExporter
import com.example.export.PdfExporter
import com.example.kashida.DocumentLayoutEngine
import com.example.kashida.KashidaEngine
import com.example.model.DocumentTemplate
import com.example.model.FontItem
import com.example.model.KashidaLevel
import com.example.model.MarginUnit
import com.example.model.PageMargins
import com.example.model.PageSize
import com.example.model.TextAlignOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.data.TextImportCodec

enum class AppScreen {
    HOME,
    EDITOR
}

enum class HomeTab(val titleAr: String) {
    DOCUMENTS("المستندات"),
    TEMPLATES("القوالب"),
    FONTS("الخطوط"),
    SETTINGS("الإعدادات")
}

enum class ActiveSheet {
    NONE,
    FONT,
    SIZE_COLOR,
    ALIGN,
    MARGINS,
    KASHIDA
}

enum class ActiveDialog {
    NONE,
    PREVIEW
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class EditorUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedHomeTab: HomeTab = HomeTab.DOCUMENTS,
    val searchQuery: String = "",
    val documentsList: List<DocumentEntity> = emptyList(),
    val currentDocId: Long = 0,
    val title: String = "مستند عربي جديد",
    val text: String = "",
    val selectedFont: FontItem,
    val availableFonts: List<FontItem> = emptyList(),
    val fontSizePt: Float = 18f,
    val textColor: Color = Color(0xFF1E293B),
    val pageColor: Color = Color(0xFFFFFFFF),
    val textAlign: TextAlignOption = TextAlignOption.JUSTIFY,
    val margins: PageMargins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER),
    val paperSize: PageSize = PageSize.A4,
    val kashidaEnabled: Boolean = true,
    val kashidaLevel: KashidaLevel = KashidaLevel.MEDIUM,
    val manualKashidaMode: Boolean = false,
    val activeSheet: ActiveSheet = ActiveSheet.NONE,
    val activeDialog: ActiveDialog = ActiveDialog.NONE,
    val isSavedLocally: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val lastExportedPdfUri: Uri? = null,
    val lastExportedPngUri: Uri? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val wordCount: Int
        get() = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    val charCount: Int
        get() = text.length

}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val documentRepo = DocumentRepository(db.documentDao())
    val fontManager = FontManager(application, db.customFontDao())

    private val _uiState = MutableStateFlow(
        EditorUiState(
            selectedFont = fontManager.bundledFonts.first(),
            availableFonts = fontManager.bundledFonts,
            text = ""
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    init {
        observeDocuments()
        loadInitialData()
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            documentRepo.allDocuments.collect { list ->
                _uiState.update { it.copy(documentsList = list) }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val customFonts = fontManager.loadCustomFonts()
            val allFonts = fontManager.bundledFonts + customFonts
            _uiState.update { it.copy(availableFonts = allFonts) }

            // Check if there are any documents; if empty, create a welcome document
            val latest = documentRepo.getLatestDocument()
            if (latest == null) {
                val welcomeDoc = DocumentEntity(
                    title = "مرحبًا بك في تطبيق مِداد",
                    content = """بسم الله الرحمن الرحيم

مرحبًا بك في تطبيق مِداد — البديل الاحترافي المتكامل لتحرير وتنسيق النصوص العربية وفق أصول الخط والطباعة الأصيلة.

تتيح لك تقنية الكشيدة الذكية ضبط نهايات الأسطر وتمديد الحروف بتوازن بصري رائع، مع إمكانية استيراد خطوطك المفضلة وتعديل الهوامش بدقة وتصدير المستند بصيغة PDF عالية الجودة أو صورة PNG للمشاركة المباشرة.""",
                    fontId = "amiri",
                    fontSizePt = 18f,
                    textColorLong = Color(0xFF1E293B).value.toLong(),
                    pageColorLong = Color(0xFFFFFFFF).value.toLong(),
                    textAlign = TextAlignOption.JUSTIFY.name,
                    marginTopMm = 20f,
                    marginBottomMm = 20f,
                    marginRightMm = 20f,
                    marginLeftMm = 20f,
                    marginUnit = MarginUnit.MILLIMETER.name,
                    paperSize = PageSize.A4.name,
                    kashidaEnabled = true,
                    kashidaLevel = KashidaLevel.MEDIUM.name,
                    updatedAt = System.currentTimeMillis()
                )
                documentRepo.saveDocument(welcomeDoc)
            }
        }
    }

    // --- Navigation Controls ---

    fun navigateToHome() {
        _uiState.update { it.copy(currentScreen = AppScreen.HOME, activeSheet = ActiveSheet.NONE) }
    }

    fun navigateToEditor() {
        _uiState.update { it.copy(currentScreen = AppScreen.EDITOR) }
    }

    fun selectHomeTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedHomeTab = tab) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // --- Document Lifecycle ---

    fun createNewDocument() {
        viewModelScope.launch {
            val defaultFont = _uiState.value.availableFonts.firstOrNull() ?: fontManager.bundledFonts.first()
            val newDoc = DocumentEntity(
                title = "مستند عربي جديد",
                content = "",
                fontId = defaultFont.id,
                fontSizePt = 18f,
                textColorLong = Color(0xFF1E293B).value.toLong(),
                pageColorLong = Color(0xFFFFFFFF).value.toLong(),
                textAlign = TextAlignOption.JUSTIFY.name,
                marginTopMm = 20f,
                marginBottomMm = 20f,
                marginRightMm = 20f,
                marginLeftMm = 20f,
                marginUnit = MarginUnit.MILLIMETER.name,
                paperSize = PageSize.A4.name,
                kashidaEnabled = true,
                kashidaLevel = KashidaLevel.MEDIUM.name,
                updatedAt = System.currentTimeMillis()
            )
            val generatedId = documentRepo.saveDocument(newDoc)
            openDocument(generatedId)
        }
    }

    fun openDocument(docId: Long) {
        viewModelScope.launch {
            val doc = documentRepo.getDocumentSnapshot(docId) ?: return@launch
            val allFonts = _uiState.value.availableFonts
            val matchedFont = allFonts.find { it.id == doc.fontId } ?: allFonts.first()
            val matchedAlign = TextAlignOption.entries.find { it.name == doc.textAlign } ?: TextAlignOption.JUSTIFY
            val matchedUnit = MarginUnit.entries.find { it.name == doc.marginUnit } ?: MarginUnit.MILLIMETER
            val matchedPaper = PageSize.entries.find { it.name == doc.paperSize } ?: PageSize.A4
            val matchedLevel = KashidaLevel.entries.find { it.name == doc.kashidaLevel } ?: KashidaLevel.MEDIUM

            undoStack.clear()
            redoStack.clear()

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.EDITOR,
                    currentDocId = doc.id,
                    title = doc.title,
                    text = doc.content,
                    selectedFont = matchedFont,
                    fontSizePt = doc.fontSizePt,
                    textColor = Color(doc.textColorLong.toULong()),
                    pageColor = Color(doc.pageColorLong.toULong()),
                    textAlign = matchedAlign,
                    margins = PageMargins(
                        topMm = doc.marginTopMm,
                        bottomMm = doc.marginBottomMm,
                        rightMm = doc.marginRightMm,
                        leftMm = doc.marginLeftMm,
                        unit = matchedUnit
                    ),
                    paperSize = matchedPaper,
                    kashidaEnabled = doc.kashidaEnabled,
                    kashidaLevel = matchedLevel,
                    isSavedLocally = true,
                    canUndo = false,
                    canRedo = false,
                    manualKashidaMode = false,
                    activeSheet = ActiveSheet.NONE
                )
            }
        }
    }

    fun deleteDocument(docId: Long) {
        viewModelScope.launch {
            documentRepo.deleteDocument(docId)
        }
    }

    fun duplicateDocument(docId: Long) {
        viewModelScope.launch {
            documentRepo.duplicateDocument(docId)
        }
    }

    // --- Undo / Redo ---

    fun onTextChanged(newText: String) {
        val current = _uiState.value.text
        if (current != newText) {
            undoStack.add(current)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            _uiState.update {
                it.copy(
                    text = newText,
                    isSavedLocally = false,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = false
                )
            }
            triggerAutoSave()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_uiState.value.text)
            _uiState.update {
                it.copy(
                    text = prev,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    isSavedLocally = false
                )
            }
            triggerAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_uiState.value.text)
            _uiState.update {
                it.copy(
                    text = next,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    isSavedLocally = false
                )
            }
            triggerAutoSave()
        }
    }

    // --- Formatting Updates ---

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, isSavedLocally = false) }
        triggerAutoSave()
    }

    fun onFontSelected(font: FontItem) {
        _uiState.update { it.copy(selectedFont = font, isSavedLocally = false) }
        triggerAutoSave()
    }

    fun onFontSizeChange(newSizePt: Float) {
        val clamped = newSizePt.coerceIn(10f, 72f)
        _uiState.update { it.copy(fontSizePt = clamped, isSavedLocally = false) }
        triggerAutoSave()
    }

    fun incrementFontSize() {
        onFontSizeChange(_uiState.value.fontSizePt + 1f)
    }

    fun decrementFontSize() {
        onFontSizeChange(_uiState.value.fontSizePt - 1f)
    }

    fun onTextColorChanged(color: Color) {
        _uiState.update { it.copy(textColor = color, isSavedLocally = false) }
        triggerAutoSave()
    }

    fun onPageColorChanged(color: Color) {
        _uiState.update { it.copy(pageColor = color, isSavedLocally = false) }
        triggerAutoSave()
    }

    fun onTextAlignChanged(align: TextAlignOption) {
        _uiState.update { it.copy(textAlign = align, isSavedLocally = false) }
        triggerAutoSave()
    }

    fun onMarginUnitChanged(unit: MarginUnit) {
        val curMargins = _uiState.value.margins
        _uiState.update {
            it.copy(margins = curMargins.copy(unit = unit), isSavedLocally = false)
        }
        triggerAutoSave()
    }

    fun onMarginsChanged(top: Float, bottom: Float, right: Float, left: Float) {
        val curUnit = _uiState.value.margins.unit
        val newMargins = PageMargins.fromDisplay(top, bottom, right, left, curUnit)
        _uiState.update { it.copy(margins = newMargins, isSavedLocally = false) }
        triggerAutoSave()
    }

    fun onPaperSizeChanged(size: PageSize) {
        _uiState.update { it.copy(paperSize = size, isSavedLocally = false) }
        triggerAutoSave()
    }

    // --- Kashida Controls ---

    fun toggleKashida() {
        val newEnabled = !_uiState.value.kashidaEnabled
        val newLevel = if (newEnabled && _uiState.value.kashidaLevel == KashidaLevel.OFF) {
            KashidaLevel.MEDIUM
        } else {
            _uiState.value.kashidaLevel
        }
        _uiState.update {
            it.copy(kashidaEnabled = newEnabled, kashidaLevel = newLevel, isSavedLocally = false)
        }
        triggerAutoSave()
    }

    fun onKashidaLevelChanged(level: KashidaLevel) {
        _uiState.update {
            it.copy(
                kashidaLevel = level,
                kashidaEnabled = level != KashidaLevel.OFF,
                isSavedLocally = false
            )
        }
        triggerAutoSave()
    }

    /**
     * Automatic kashida is a render-time transformation and never mutates
     * the document text.
     */
    fun applySmartKashidaToDocument() {
        _uiState.update { it.copy(exportMessage = "الكشيدة التلقائية تُطبّق أثناء التنضيد دون تعديل النص الأصلي") }
    }

    fun toggleManualKashidaMode() {
        val current = _uiState.value.manualKashidaMode
        _uiState.update { it.copy(manualKashidaMode = !current, activeSheet = ActiveSheet.NONE) }
    }

    fun applyManualKashidaAtPoint(
        cleanWord: String,
        connectionIndex: Int,
        count: Int,
        wordStartIndex: Int
    ) {
        val currentText = _uiState.value.text
        if (wordStartIndex !in currentText.indices) return

        var wordEnd = wordStartIndex
        while (wordEnd < currentText.length && !currentText[wordEnd].isWhitespace()) {
            wordEnd++
        }

        val currentWord = currentText.substring(wordStartIndex, wordEnd)
        val cleanCurrentWord = KashidaEngine.stripKashida(currentWord)
        if (cleanCurrentWord != cleanWord) return

        val replacement = KashidaEngine.applyManualKashidaToWord(
            cleanCurrentWord,
            connectionIndex,
            count
        )
        onTextChanged(currentText.replaceRange(wordStartIndex, wordEnd, replacement))
    }

    fun buildDocumentLayout(state: EditorUiState = _uiState.value): DocumentLayoutEngine.DocumentLayout {
        val typeface = fontManager.getNativeTypeface(
            state.selectedFont.id,
            state.selectedFont.filePath
        )
        return DocumentLayoutEngine.build(
            text = state.text,
            typeface = typeface,
            fontSizePt = state.fontSizePt,
            textAlign = state.textAlign,
            margins = state.margins,
            pageSize = state.paperSize,
            kashidaEnabled = state.kashidaEnabled,
            kashidaLevel = state.kashidaLevel
        )
    }

    // --- Sheets & Dialogs ---

    fun openSheet(sheet: ActiveSheet) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = ActiveSheet.NONE) }
    }

    fun showDialog(dialog: ActiveDialog) {
        _uiState.update { it.copy(activeDialog = dialog) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(activeDialog = ActiveDialog.NONE) }
    }

    fun clearExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }

    // --- Templates & Font Library ---

    fun getTemplates(): List<DocumentTemplate> = documentRepo.getTemplates()

    fun applyTemplateAndOpen(template: DocumentTemplate) {
        viewModelScope.launch {
            val matchedFont = _uiState.value.availableFonts.find { it.id == template.fontId }
                ?: fontManager.bundledFonts.first()

            val newDoc = DocumentEntity(
                title = template.titleAr,
                content = template.sampleText,
                fontId = matchedFont.id,
                fontSizePt = template.fontSizePt,
                textColorLong = template.textColor.value.toLong(),
                pageColorLong = template.pageColor.value.toLong(),
                textAlign = template.align.name,
                marginTopMm = template.margins.topMm,
                marginBottomMm = template.margins.bottomMm,
                marginRightMm = template.margins.rightMm,
                marginLeftMm = template.margins.leftMm,
                marginUnit = template.margins.unit.name,
                paperSize = PageSize.A4.name,
                kashidaEnabled = template.kashidaEnabled,
                kashidaLevel = template.kashidaLevel.name,
                updatedAt = System.currentTimeMillis()
            )
            val generatedId = documentRepo.saveDocument(newDoc)
            openDocument(generatedId)
        }
    }

    fun importFont(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val imported = fontManager.importFontFromUri(uri)
            if (imported != null) {
                val updatedFonts = _uiState.value.availableFonts + imported
                _uiState.update {
                    it.copy(
                        availableFonts = updatedFonts,
                        selectedFont = imported,
                        isExporting = false,
                        exportMessage = "تم استيراد الخط وتطبيقه بنجاح: ${imported.nameAr}"
                    )
                }
                triggerAutoSave()
            } else {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = "تعذر قراءة ملف الخط. يرجى التأكد من اختيار ملف .ttf أو .otf صالح."
                    )
                }
            }
        }
    }

    /** Imports .txt/.md as editable UTF-8 text without applying Kashida. */
    fun importTextDocument(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportMessage = null) }
            val result = runCatching {
                val resolver = getApplication<Application>().contentResolver
                val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                val title = TextImportCodec.titleFromDisplayName(displayName)
                val content = resolver.openInputStream(uri)?.use { input ->
                    TextImportCodec.readUtf8(input)
                } ?: error("تعذر فتح الملف")
                title to content
            }
            result.onSuccess { (title, content) ->
                val state = _uiState.value
                val imported = DocumentEntity(
                    title = title,
                    content = content,
                    fontId = state.selectedFont.id,
                    fontSizePt = state.fontSizePt,
                    textColorLong = state.textColor.value.toLong(),
                    pageColorLong = state.pageColor.value.toLong(),
                    textAlign = state.textAlign.name,
                    marginTopMm = state.margins.topMm,
                    marginBottomMm = state.margins.bottomMm,
                    marginRightMm = state.margins.rightMm,
                    marginLeftMm = state.margins.leftMm,
                    marginUnit = state.margins.unit.name,
                    paperSize = state.paperSize.name,
                    kashidaEnabled = state.kashidaEnabled,
                    kashidaLevel = state.kashidaLevel.name,
                    updatedAt = System.currentTimeMillis()
                )
                val id = documentRepo.saveDocument(imported)
                openDocument(id)
                _uiState.update { it.copy(isExporting = false, exportMessage = "تم استيراد الملف كنص قابل للتحرير") }
            }.onFailure {
                _uiState.update { it.copy(isExporting = false, exportMessage = "تعذر قراءة الملف النصي UTF-8") }
            }
        }
    }

    fun deleteCustomFont(fontId: String) {
        viewModelScope.launch {
            fontManager.deleteCustomFont(fontId)
            val updatedFonts = _uiState.value.availableFonts.filter { it.id != fontId }
            val fallbackFont = if (_uiState.value.selectedFont.id == fontId) {
                updatedFonts.firstOrNull() ?: fontManager.bundledFonts.first()
            } else {
                _uiState.value.selectedFont
            }
            _uiState.update {
                it.copy(availableFonts = updatedFonts, selectedFont = fallbackFont)
            }
            triggerAutoSave()
        }
    }

    // --- Settings Controls ---

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun toggleAutoSave() {
        _uiState.update { it.copy(autoSaveEnabled = !it.autoSaveEnabled) }
    }

    // --- Export Actions ---

    fun exportPdf(onSuccess: (Uri) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportMessage = null) }
            val state = _uiState.value
            val layout = buildDocumentLayout(state)
            val nativeTypeface = fontManager.getNativeTypeface(state.selectedFont.id, state.selectedFont.filePath)

            val uri = PdfExporter.exportToPdf(
                context = getApplication(),
                layout = layout,
                title = state.title,
                typeface = nativeTypeface,
                fontSizePt = state.fontSizePt,
                textColor = state.textColor.toArgb(),
                pageColor = state.pageColor.toArgb()
            )

            _uiState.update {
                it.copy(
                    isExporting = false,
                    lastExportedPdfUri = uri,
                    exportMessage = if (uri != null) "تم تجهيز مستند PDF بنجاح!" else "حدث خطأ أثناء تصدير PDF"
                )
            }
            if (uri != null) onSuccess(uri)
        }
    }

    fun exportPng(onSuccess: (Uri) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportMessage = null) }
            val state = _uiState.value
            val layout = buildDocumentLayout(state)
            val nativeTypeface = fontManager.getNativeTypeface(state.selectedFont.id, state.selectedFont.filePath)

            val uri = ImageExporter.exportToPng(
                context = getApplication(),
                layout = layout,
                title = state.title,
                typeface = nativeTypeface,
                fontSizePt = state.fontSizePt,
                textColor = state.textColor.toArgb(),
                pageColor = state.pageColor.toArgb()
            )

            _uiState.update {
                it.copy(
                    isExporting = false,
                    lastExportedPngUri = uri,
                    exportMessage = if (uri != null) "تم تصدير الصورة بنجاح للمشاركة السريعة!" else "حدث خطأ أثناء تصدير الصورة"
                )
            }
            if (uri != null) onSuccess(uri)
        }
    }

    // --- Persistence ---

    fun saveDocumentNow() {
        viewModelScope.launch {
            persistCurrentDocument()
            _uiState.update { it.copy(isSavedLocally = true, exportMessage = "تم حفظ المستند بنجاح") }
        }
    }

    private fun triggerAutoSave() {
        if (!_uiState.value.autoSaveEnabled) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(600)
            persistCurrentDocument()
            _uiState.update { it.copy(isSavedLocally = true) }
        }
    }

    private suspend fun persistCurrentDocument() {
        val s = _uiState.value
        val doc = DocumentEntity(
            id = s.currentDocId,
            title = s.title.ifBlank { "مستند عربي جديد" },
            content = s.text,
            fontId = s.selectedFont.id,
            fontSizePt = s.fontSizePt,
            textColorLong = s.textColor.value.toLong(),
            pageColorLong = s.pageColor.value.toLong(),
            textAlign = s.textAlign.name,
            marginTopMm = s.margins.topMm,
            marginBottomMm = s.margins.bottomMm,
            marginRightMm = s.margins.rightMm,
            marginLeftMm = s.margins.leftMm,
            marginUnit = s.margins.unit.name,
            paperSize = s.paperSize.name,
            kashidaEnabled = s.kashidaEnabled,
            kashidaLevel = s.kashidaLevel.name,
            updatedAt = System.currentTimeMillis()
        )
        val savedId = documentRepo.saveDocument(doc)
        if (s.currentDocId == 0L) {
            _uiState.update { it.copy(currentDocId = savedId) }
        }
    }
}

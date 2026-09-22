package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentEntity
import com.example.export.PdfExporter
import com.example.model.DocumentTemplate
import com.example.model.FontItem
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.EditorUiState
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.HomeTab
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    onImportFontClick: () -> Unit,
    onImportDocumentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearchActive by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("بحث في المستندات...", fontSize = 14.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        viewModel.onSearchQueryChanged("")
                                        isSearchActive = false
                                    }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "إلغاء")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        } else {
                            Text(
                                text = when (uiState.selectedHomeTab) {
                                    HomeTab.DOCUMENTS -> "مستنداتي"
                                    HomeTab.TEMPLATES -> "القوالب الجاهزة"
                                    HomeTab.FONTS -> "مكتبة الخطوط"
                                    HomeTab.SETTINGS -> "الإعدادات"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        if (!isSearchActive && uiState.selectedHomeTab == HomeTab.DOCUMENTS) {
                            IconButton(
                                onClick = onImportDocumentClick,
                                modifier = Modifier.testTag("home_import_doc_btn")
                            ) {
                                Icon(Icons.Outlined.FileUpload, contentDescription = "استيراد مستند نصي (TXT / MD)")
                            }
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Outlined.Search, contentDescription = "بحث")
                            }
                        }
                        IconButton(onClick = { viewModel.selectHomeTab(HomeTab.TEMPLATES) }) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = "القوالب الجاهزة")
                        }
                        IconButton(onClick = { viewModel.selectHomeTab(HomeTab.SETTINGS) }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val tabs = listOf(
                        Triple(HomeTab.DOCUMENTS, "المستندات", Icons.Outlined.Description),
                        Triple(HomeTab.TEMPLATES, "القوالب", Icons.Outlined.AutoAwesome),
                        Triple(HomeTab.FONTS, "الخطوط", Icons.Outlined.FontDownload),
                        Triple(HomeTab.SETTINGS, "الإعدادات", Icons.Outlined.Settings)
                    )

                    tabs.forEach { (tab, label, icon) ->
                        val isSelected = uiState.selectedHomeTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectHomeTab(tab) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            },
            floatingActionButton = {
                if (uiState.selectedHomeTab == HomeTab.DOCUMENTS) {
                    FloatingActionButton(
                        onClick = { viewModel.createNewDocument() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("home_create_fab")
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "إنشاء مستند جديد",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.selectedHomeTab) {
                    HomeTab.DOCUMENTS -> DocumentsTabContent(
                        documents = uiState.documentsList.filter {
                            if (uiState.searchQuery.isBlank()) true
                            else it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                                 it.content.contains(uiState.searchQuery, ignoreCase = true)
                        },
                        onOpen = { viewModel.openDocument(it.id) },
                        onDuplicate = { viewModel.duplicateDocument(it.id) },
                        onDelete = { viewModel.deleteDocument(it.id) },
                        onShare = { doc ->
                            viewModel.openDocument(doc.id)
                            viewModel.exportPdf { uri ->
                                PdfExporter.shareFile(context, uri, "application/pdf", "مشاركة مستند PDF")
                            }
                        },
                        onCreateNew = { viewModel.createNewDocument() },
                        onImportDocument = onImportDocumentClick
                    )

                    HomeTab.TEMPLATES -> TemplatesTabContent(
                        templates = viewModel.getTemplates(),
                        onSelectTemplate = { viewModel.applyTemplateAndOpen(it) }
                    )

                    HomeTab.FONTS -> FontsTabContent(
                        availableFonts = uiState.availableFonts,
                        onImportFont = onImportFontClick,
                        onDeleteFont = { viewModel.deleteCustomFont(it) }
                    )

                    HomeTab.SETTINGS -> SettingsTabContent(
                        themeMode = uiState.themeMode,
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        autoSaveEnabled = uiState.autoSaveEnabled,
                        onToggleAutoSave = { viewModel.toggleAutoSave() },
                        customFontsCount = uiState.availableFonts.count { it.isCustom }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// 1. DOCUMENTS TAB CONTENT (Cards with 16dp rounded corners)
// ----------------------------------------------------------------------
@Composable
fun DocumentsTabContent(
    documents: List<DocumentEntity>,
    onOpen: (DocumentEntity) -> Unit,
    onDuplicate: (DocumentEntity) -> Unit,
    onDelete: (DocumentEntity) -> Unit,
    onShare: (DocumentEntity) -> Unit,
    onCreateNew: () -> Unit,
    onImportDocument: () -> Unit
) {
    if (documents.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Text(
                    text = "لا توجد مستندات بعد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ابدأ بإنشاء مستند جديد أو استورد ملف نصي من جهازك",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onCreateNew,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مستند جديد")
                    }
                    OutlinedButton(
                        onClick = onImportDocument,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_import_doc_btn")
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استيراد TXT/MD")
                    }
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(documents, key = { it.id }) { doc ->
                DocumentCard(
                    doc = doc,
                    onOpen = { onOpen(doc) },
                    onDuplicate = { onDuplicate(doc) },
                    onDelete = { onDelete(doc) },
                    onShare = { onShare(doc) }
                )
            }
        }
    }
}

@Composable
fun DocumentCard(
    doc: DocumentEntity,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedDate = remember(doc.updatedAt) {
        DateFormat.format("yyyy/MM/dd • hh:mm a", Date(doc.updatedAt)).toString()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("doc_card_${doc.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = doc.title.ifBlank { "مستند بدون عنوان" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "خيارات")
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("فتح المستند") },
                            leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تكرار المستند") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("مشاركة المستند (PDF)") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thumbnail Preview Container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = doc.content.ifBlank { "مستند فارغ..." },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------
// 2. TEMPLATES TAB CONTENT (2-Column Grid of Styled Cards)
// ----------------------------------------------------------------------
@Composable
fun TemplatesTabContent(
    templates: List<DocumentTemplate>,
    onSelectTemplate: (DocumentTemplate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "اختر قالباً جاهزاً لبدء مستندك فوراً:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(templates) { template ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clickable { onSelectTemplate(template) }
                        .testTag("template_grid_item_${template.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = template.categoryAr,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = template.titleAr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Mini Simulated Sheet in Template Colors
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = template.pageColor,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            ) {
                                Text(
                                    text = template.sampleText.take(80) + "...",
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = template.textColor,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        Text(
                            text = "استخدام القالب ←",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// 3. FONTS TAB CONTENT (Imported & Bundled Fonts Management)
// ----------------------------------------------------------------------
@Composable
fun FontsTabContent(
    availableFonts: List<FontItem>,
    onImportFont: () -> Unit,
    onDeleteFont: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FilledTonalButton(
            onClick = onImportFont,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.FileUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("استيراد خط جديد من الهاتف (.ttf أو .otf)")
        }

        Text(
            text = "الخطوط المتوفرة في التطبيق:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(availableFonts) { font ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = font.nameAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (font.isCustom) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "مستورد",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "أَبْجَدِيَّةُ الضَّادِ وَجَمَالُ الحَرْفِ العَرَبِيّ",
                                fontFamily = font.fontFamily,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (font.isCustom) {
                            IconButton(onClick = { onDeleteFont(font.id) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "حذف الخط",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// 4. SETTINGS TAB CONTENT
// ----------------------------------------------------------------------
@Composable
fun SettingsTabContent(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    autoSaveEnabled: Boolean,
    onToggleAutoSave: () -> Unit,
    customFontsCount: Int
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Theme Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "المظهر ونظام الألوان",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair(AppThemeMode.SYSTEM, "تلقائي (النظام)"),
                            Pair(AppThemeMode.LIGHT, "فاتح"),
                            Pair(AppThemeMode.DARK, "داكن")
                        ).forEach { (mode, title) ->
                            val isSelected = themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(title) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Auto Save Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الحفظ التلقائي المحلي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "حفظ فوري للتعديلات داخل ذاكرة الهاتف دون الحاجة لإنترنت",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Switch(
                        checked = autoSaveEnabled,
                        onCheckedChange = { onToggleAutoSave() }
                    )
                }
            }
        }

        // Language & Fonts Info
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("لغة التطبيق والواجهة", fontWeight = FontWeight.SemiBold)
                        Text("العربية (RTL كامل)", color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الخطوط المستوردة يدويًا", fontWeight = FontWeight.SemiBold)
                        Text("$customFontsCount خطوط مضافة", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // About App Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حول تطبيق مِداد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "مِداد — محرر وتنسيق نصوص عربي احترافي متكامل، صُمم ليكون بديلاً حقيقياً لـ Google Docs على الهاتف المحمول مع التركيز على تقنية الكشيدة الذكية وتنسيق الطباعة وفق أصول الخط العربي الأصيل، ويعمل بالكامل دون اتصال بالإنترنت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الإصدار: 1.0.0 • جميع الحقوق محفوظة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

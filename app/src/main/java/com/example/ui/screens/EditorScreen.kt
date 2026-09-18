package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.LineWeight
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.export.PdfExporter
import com.example.kashida.DocumentLayoutEngine
import com.example.ui.components.MainCanvasComponent
import com.example.ui.dialogs.PreviewExportDialog
import com.example.ui.sheets.EditorBottomSheetsHost
import com.example.ui.viewmodel.ActiveDialog
import com.example.ui.viewmodel.ActiveSheet
import com.example.ui.viewmodel.EditorUiState
import com.example.ui.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val documentLayout = remember(
        uiState.text,
        uiState.selectedFont.id,
        uiState.selectedFont.filePath,
        uiState.fontSizePt,
        uiState.textAlign,
        uiState.margins,
        uiState.paperSize,
        uiState.kashidaEnabled,
        uiState.kashidaLevel
    ) {
        viewModel.buildDocumentLayout(uiState)
    }
    val nativeTypeface = remember(uiState.selectedFont.id, uiState.selectedFont.filePath) {
        viewModel.fontManager.getNativeTypeface(
            uiState.selectedFont.id,
            uiState.selectedFont.filePath
        )
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFont(uri)
        }
    }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearExportMessage()
        }
    }

    BackHandler {
        viewModel.navigateToHome()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        // Return/Back button on right in RTL
                        IconButton(
                            onClick = { viewModel.navigateToHome() },
                            modifier = Modifier.testTag("editor_back_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "العودة للمستندات"
                            )
                        }
                    },
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Editable title
                            BasicTextField(
                                value = uiState.title,
                                onValueChange = { viewModel.onTitleChanged(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .testTag("editor_title_input")
                            )

                            // Status badge with save icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                if (uiState.isSavedLocally) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "محفوظ تلقائيًا",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "جاري الحفظ...",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Undo button
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = uiState.canUndo,
                            modifier = Modifier.testTag("editor_undo_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = "تراجع",
                                tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        }

                        // Redo button
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = uiState.canRedo,
                            modifier = Modifier.testTag("editor_redo_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Redo,
                                contentDescription = "إعادة",
                                tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        }

                        // Prominent "معاينة PDF" button
                        FilledTonalButton(
                            onClick = { viewModel.showDialog(ActiveDialog.PREVIEW) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("editor_preview_btn")
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("معاينة PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sleek horizontal toolbar with 32x32px tool icons and tooltips
                    HorizontalToolBar(
                        onOpenSheet = { viewModel.openSheet(it) }
                    )

                    // Exporting progress indicator
                    if (uiState.isExporting) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري معالجة وتصدير المستند بدقة عالية...", fontSize = 11.sp)
                            }
                        }
                    }

                    // Main Writing Sheet
                    MainCanvasComponent(
                        uiState = uiState,
                        onTextChanged = { viewModel.onTextChanged(it) },
                        onApplyManualKashida = { cleanWord, connectionIndex, count, wordIndex ->
                            viewModel.applyManualKashidaAtPoint(cleanWord, connectionIndex, count, wordIndex)
                        },
                        onExitManualMode = { viewModel.toggleManualKashidaMode() },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Floating Bottom Info Bar (Word/Char counter and estimated pages)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .testTag("floating_editor_counter_bar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "الكلمات: ${uiState.wordCount} | الحروف: ${uiState.charCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Text(
                            text = "صفحة ١ من ${documentLayout.pageCount} (${uiState.paperSize.name})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Sheets Host
            EditorBottomSheetsHost(
                viewModel = viewModel,
                uiState = uiState,
                onImportFontClick = {
                    fontPickerLauncher.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "*/*"))
                }
            )

            // Fullscreen Preview & Export Dialog
            if (uiState.activeDialog == ActiveDialog.PREVIEW) {
                PreviewExportDialog(
                    uiState = uiState,
                    documentLayout = documentLayout,
                    typeface = nativeTypeface,
                    onExportPdf = {
                        viewModel.exportPdf { uri ->
                            PdfExporter.shareFile(context, uri, "application/pdf", "مشاركة مستند PDF")
                        }
                    },
                    onExportPng = {
                        viewModel.exportPng { uri ->
                            PdfExporter.shareFile(context, uri, "image/png", "مشاركة صورة المستند PNG")
                        }
                    },
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
        }
    }
}

/**
 * Compact horizontal scrollable toolbar beneath the TopAppBar.
 * Contains 32x32dp tool icons with long-press tooltips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizontalToolBar(
    onOpenSheet: (ActiveSheet) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tools = listOf(
                Triple(ActiveSheet.FONT, "نوع الخط", Icons.Outlined.FontDownload),
                Triple(ActiveSheet.SIZE_COLOR, "الحجم واللون", Icons.Outlined.ColorLens),
                Triple(ActiveSheet.ALIGN, "محاذاة النص", Icons.Outlined.FormatAlignCenter),
                Triple(ActiveSheet.KASHIDA, "الكشيدة الذكية", Icons.Outlined.Gesture),
                Triple(ActiveSheet.MARGINS, "هوامش الصفحة", Icons.Outlined.LineWeight)
            )

            tools.forEach { (sheet, tooltipText, icon) ->
                ToolIconItem(
                    sheet = sheet,
                    tooltipText = tooltipText,
                    icon = icon,
                    onClick = { onOpenSheet(sheet) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolIconItem(
    sheet: ActiveSheet,
    tooltipText: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val tooltipState = rememberTooltipState()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(tooltipText, fontSize = 11.sp)
            }
        },
        state = tooltipState
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .size(38.dp)
                .testTag("tool_btn_${sheet.name.lowercase()}")
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tooltipText,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

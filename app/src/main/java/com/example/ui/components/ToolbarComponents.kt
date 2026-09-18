package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FontItem
import com.example.model.KashidaLevel
import com.example.model.MarginUnit
import com.example.model.PageMargins
import com.example.model.PageSize
import com.example.model.TextAlignOption
import com.example.ui.viewmodel.EditorUiState

enum class ToolbarTab(val titleAr: String, val icon: ImageVector) {
    FONT("الخط والحجم", Icons.Default.FontDownload),
    KASHIDA("الكشيدة", Icons.Default.Gesture),
    ALIGN_MARGINS("المحاذاة والهوامش", Icons.Default.LineWeight),
    COLORS_PAPER("الألوان والورق", Icons.Default.ColorLens),
    EXPORT("المعاينة والتصدير", Icons.Default.Share)
}

@Composable
fun EditorToolbar(
    uiState: EditorUiState,
    onFontSelect: (FontItem) -> Unit,
    onImportFontClick: () -> Unit,
    onOpenFontLibrary: () -> Unit,
    onFontSizeInc: () -> Unit,
    onFontSizeDec: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onToggleKashida: () -> Unit,
    onKashidaLevelChange: (KashidaLevel) -> Unit,
    onToggleManualKashida: () -> Unit,
    onApplySmartKashida: () -> Unit,
    onAlignChange: (TextAlignOption) -> Unit,
    onMarginsChange: (Float, Float, Float, Float) -> Unit,
    onMarginUnitChange: (MarginUnit) -> Unit,
    onTextColorChange: (Color) -> Unit,
    onPageColorChange: (Color) -> Unit,
    onPaperSizeChange: (PageSize) -> Unit,
    onOpenPreview: () -> Unit,
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = ToolbarTab.entries

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Horizontal Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        label = {
                            Text(
                                text = tab.titleAr,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.titleAr,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                when (tabs[selectedTab]) {
                    ToolbarTab.FONT -> FontControlPanel(
                        selectedFont = uiState.selectedFont,
                        availableFonts = uiState.availableFonts,
                        fontSizePt = uiState.fontSizePt,
                        onFontSelect = onFontSelect,
                        onImportFontClick = onImportFontClick,
                        onOpenFontLibrary = onOpenFontLibrary,
                        onFontSizeInc = onFontSizeInc,
                        onFontSizeDec = onFontSizeDec,
                        onFontSizeChange = onFontSizeChange
                    )
                    ToolbarTab.KASHIDA -> KashidaControlPanel(
                        kashidaEnabled = uiState.kashidaEnabled,
                        kashidaLevel = uiState.kashidaLevel,
                        manualKashidaMode = uiState.manualKashidaMode,
                        onToggleKashida = onToggleKashida,
                        onKashidaLevelChange = onKashidaLevelChange,
                        onToggleManualKashida = onToggleManualKashida,
                        onApplySmartKashida = onApplySmartKashida
                    )
                    ToolbarTab.ALIGN_MARGINS -> AlignmentAndMarginsPanel(
                        currentAlign = uiState.textAlign,
                        margins = uiState.margins,
                        onAlignChange = onAlignChange,
                        onMarginsChange = onMarginsChange,
                        onMarginUnitChange = onMarginUnitChange
                    )
                    ToolbarTab.COLORS_PAPER -> ColorsAndPaperPanel(
                        selectedTextColor = uiState.textColor,
                        selectedPageColor = uiState.pageColor,
                        selectedPaperSize = uiState.paperSize,
                        onTextColorChange = onTextColorChange,
                        onPageColorChange = onPageColorChange,
                        onPaperSizeChange = onPaperSizeChange
                    )
                    ToolbarTab.EXPORT -> ExportPanel(
                        onOpenPreview = onOpenPreview,
                        onExportPdf = onExportPdf,
                        onExportPng = onExportPng
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. FONT CONTROL PANEL
// -------------------------------------------------------------
@Composable
fun FontControlPanel(
    selectedFont: FontItem,
    availableFonts: List<FontItem>,
    fontSizePt: Float,
    onFontSelect: (FontItem) -> Unit,
    onImportFontClick: () -> Unit,
    onOpenFontLibrary: () -> Unit,
    onFontSizeInc: () -> Unit,
    onFontSizeDec: () -> Unit,
    onFontSizeChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Font Selection Chips with "استيراد خط"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نوع الخط المختار:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = onImportFontClick,
                    modifier = Modifier.testTag("import_font_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استيراد خط (.ttf/.otf)", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenFontLibrary,
                    modifier = Modifier.testTag("font_library_btn")
                ) {
                    Text("مكتبة الخطوط", fontSize = 12.sp)
                }
            }
        }

        // Horizontal list of fonts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableFonts.forEach { font ->
                val isSelected = font.id == selectedFont.id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) borderBorder(MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .clickable { onFontSelect(font) }
                        .testTag("font_chip_${font.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = font.nameAr,
                            fontFamily = font.fontFamily,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "أبجد هوز حطي",
                            fontFamily = font.fontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Font Size Controls (1pt step)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "حجم الخط:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = onFontSizeDec,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("font_size_dec_btn")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "تقليل الحجم")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = borderBorder(MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "${fontSizePt.toInt()} pt",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onFontSizeInc,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("font_size_inc_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "زيادة الحجم")
                }
            }

            Slider(
                value = fontSizePt,
                onValueChange = onFontSizeChange,
                valueRange = 10f..72f,
                steps = 61,
                modifier = Modifier
                    .width(130.dp)
                    .testTag("font_size_slider")
            )
        }
    }
}

// -------------------------------------------------------------
// 2. KASHIDA CONTROL PANEL
// -------------------------------------------------------------
@Composable
fun KashidaControlPanel(
    kashidaEnabled: Boolean,
    kashidaLevel: KashidaLevel,
    manualKashidaMode: Boolean,
    onToggleKashida: () -> Unit,
    onKashidaLevelChange: (KashidaLevel) -> Unit,
    onToggleManualKashida: () -> Unit,
    onApplySmartKashida: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Toggle Switch & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "تقنية الكشيدة الذكية (التطويل والمد)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "توزيع التمديد بذكاء على نقاط الحروف الطبيعية",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Switch(
                checked = kashidaEnabled,
                onCheckedChange = { onToggleKashida() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("kashida_toggle_switch")
            )
        }

        // Preset Levels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "المستوى الجاهز:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            listOf(KashidaLevel.LIGHT, KashidaLevel.MEDIUM, KashidaLevel.HEAVY).forEach { level ->
                val isSelected = kashidaEnabled && kashidaLevel == level
                FilterChip(
                    selected = isSelected,
                    onClick = { onKashidaLevelChange(level) },
                    label = { Text(level.labelAr) },
                    enabled = kashidaEnabled,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("kashida_level_${level.name.lowercase()}")
                )
            }
        }

        // Actions: Apply Now & Manual Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onApplySmartKashida,
                enabled = kashidaEnabled,
                modifier = Modifier
                    .weight(1f)
                    .testTag("apply_smart_kashida_btn")
            ) {
                Text("تطبيق الكشيدة التلقائية", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onToggleManualKashida,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (manualKashidaMode) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_kashida_mode_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Gesture,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (manualKashidaMode) "إنهاء التحكم اليدوي" else "وضع التحكم اليدوي",
                    fontSize = 12.sp
                )
            }
        }

        if (manualKashidaMode) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "وضع التحكم اليدوي مفعّل: يمكنك الضغط على أي نقطة اتصال بين حرفين في الورقة أدناه لتمديدها أو تقليصها بحرية تامة مع معاينة فورية!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 3. ALIGNMENT & MARGINS PANEL
// -------------------------------------------------------------
@Composable
fun AlignmentAndMarginsPanel(
    currentAlign: TextAlignOption,
    margins: PageMargins,
    onAlignChange: (TextAlignOption) -> Unit,
    onMarginsChange: (Float, Float, Float, Float) -> Unit,
    onMarginUnitChange: (MarginUnit) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Alignment Buttons (4 options: Right, Center, Left, Justify)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "محاذاة النص:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Pair(TextAlignOption.RIGHT, Icons.AutoMirrored.Filled.FormatAlignRight),
                    Pair(TextAlignOption.CENTER, Icons.Default.FormatAlignCenter),
                    Pair(TextAlignOption.LEFT, Icons.AutoMirrored.Filled.FormatAlignLeft),
                    Pair(TextAlignOption.JUSTIFY, Icons.Default.FormatAlignJustify)
                ).forEach { (align, icon) ->
                    val isSelected = currentAlign == align
                    if (isSelected) {
                        FilledTonalIconButton(
                            onClick = { onAlignChange(align) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("align_${align.name.lowercase()}")
                        ) {
                            Icon(icon, contentDescription = align.labelAr)
                        }
                    } else {
                        OutlinedIconButton(
                            onClick = { onAlignChange(align) },
                            modifier = Modifier.testTag("align_${align.name.lowercase()}")
                        ) {
                            Icon(icon, contentDescription = align.labelAr)
                        }
                    }
                }
            }
        }

        // Margins Header & Unit Toggle (ملم / سم)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "هوامش الصفحة (دقيقة وقابلة للتعديل):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MarginUnit.entries.forEach { unit ->
                    val isSelected = margins.unit == unit
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMarginUnitChange(unit) },
                        label = { Text(unit.symbolAr, fontSize = 11.sp) },
                        modifier = Modifier.testTag("margin_unit_${unit.name.lowercase()}")
                    )
                }
            }
        }

        // 4 Separate Numerical Steppers for Margins
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val step = if (margins.unit == MarginUnit.CENTIMETER) 0.2f else 2f
            val topVal = margins.displayTop()
            val bottomVal = margins.displayBottom()
            val rightVal = margins.displayRight()
            val leftVal = margins.displayLeft()

            MarginInputBox(
                label = "علوي",
                value = topVal,
                unitStr = margins.unit.symbolAr,
                onInc = { onMarginsChange(topVal + step, bottomVal, rightVal, leftVal) },
                onDec = { onMarginsChange((topVal - step).coerceAtLeast(0.5f), bottomVal, rightVal, leftVal) },
                modifier = Modifier.weight(1f)
            )

            MarginInputBox(
                label = "سفلي",
                value = bottomVal,
                unitStr = margins.unit.symbolAr,
                onInc = { onMarginsChange(topVal, bottomVal + step, rightVal, leftVal) },
                onDec = { onMarginsChange(topVal, (bottomVal - step).coerceAtLeast(0.5f), rightVal, leftVal) },
                modifier = Modifier.weight(1f)
            )

            MarginInputBox(
                label = "يمين",
                value = rightVal,
                unitStr = margins.unit.symbolAr,
                onInc = { onMarginsChange(topVal, bottomVal, rightVal + step, leftVal) },
                onDec = { onMarginsChange(topVal, bottomVal, (rightVal - step).coerceAtLeast(0.5f), leftVal) },
                modifier = Modifier.weight(1f)
            )

            MarginInputBox(
                label = "يسار",
                value = leftVal,
                unitStr = margins.unit.symbolAr,
                onInc = { onMarginsChange(topVal, bottomVal, rightVal, leftVal + step) },
                onDec = { onMarginsChange(topVal, bottomVal, rightVal, (leftVal - step).coerceAtLeast(0.5f)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MarginInputBox(
    label: String,
    value: Float,
    unitStr: String,
    onInc: () -> Unit,
    onDec: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = String.format("%.1f %s", value, unitStr),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                IconButton(
                    onClick = onDec,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "تقليل", modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = onInc,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. COLORS & PAPER PANEL
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorsAndPaperPanel(
    selectedTextColor: Color,
    selectedPageColor: Color,
    selectedPaperSize: PageSize,
    onTextColorChange: (Color) -> Unit,
    onPageColorChange: (Color) -> Unit,
    onPaperSizeChange: (PageSize) -> Unit
) {
    val textPalette = listOf(
        Pair("فحمي", Color(0xFF1E293B)),
        Pair("عنابي", Color(0xFF4A0E17)),
        Pair("كحلي", Color(0xFF0B2545)),
        Pair("زمردي", Color(0xFF134E4A)),
        Pair("بني ملكي", Color(0xFF451A03)),
        Pair("ذهبي", Color(0xFF854D0E))
    )

    val pagePalette = listOf(
        Pair("أبيض ناصع", Color(0xFFFFFFFF)),
        Pair("ورق قرطاس", Color(0xFFFAF4E8)),
        Pair("عاجي دافئ", Color(0xFFFAF8F5)),
        Pair("رمادي فاتح", Color(0xFFF1F5F9)),
        Pair("نعناعي هادئ", Color(0xFFF0FDF4))
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Paper Size (A4 vs A5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مقاس الورق:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PageSize.entries.forEach { size ->
                    val isSelected = selectedPaperSize == size
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPaperSizeChange(size) },
                        label = { Text(size.displayNameAr, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("paper_size_${size.name.lowercase()}")
                    )
                }
            }
        }

        // Text Color Palette
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "لون النص:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                textPalette.forEach { (name, color) ->
                    val isSelected = selectedTextColor == color
                    ColorSwatch(
                        name = name,
                        color = color,
                        isSelected = isSelected,
                        onClick = { onTextColorChange(color) }
                    )
                }
            }
        }

        // Page Color Palette
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "لون خلفية الصفحة:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pagePalette.forEach { (name, color) ->
                    val isSelected = selectedPageColor == color
                    ColorSwatch(
                        name = name,
                        color = color,
                        isSelected = isSelected,
                        onClick = { onPageColorChange(color) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSwatch(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) borderBorder(MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("color_swatch_${name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            Text(text = name, fontSize = 11.sp)
        }
    }
}

// -------------------------------------------------------------
// 5. EXPORT PANEL
// -------------------------------------------------------------
@Composable
fun ExportPanel(
    onOpenPreview: () -> Unit,
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "المعاينة والتصدير المباشر:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenPreview,
                modifier = Modifier
                    .weight(1f)
                    .testTag("preview_pdf_btn")
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("معاينة PDF والطباعة", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onExportPdf,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_pdf_direct_btn")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تصدير مستند PDF", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onExportPng,
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_png_direct_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة صورة PNG", fontSize = 12.sp)
            }
        }
    }
}

private fun borderBorder(color: Color) = androidx.compose.foundation.BorderStroke(2.dp, color)

// Helper extension for Color luminance
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

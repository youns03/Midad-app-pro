package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FontItem
import com.example.model.KashidaLevel
import com.example.model.MarginUnit
import com.example.model.PageMargins
import com.example.model.TextAlignOption
import com.example.ui.viewmodel.ActiveSheet
import com.example.ui.viewmodel.EditorUiState
import com.example.ui.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorBottomSheetsHost(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    onImportFontClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (uiState.activeSheet != ActiveSheet.NONE) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeSheet() },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("editor_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    when (uiState.activeSheet) {
                        ActiveSheet.FONT -> FontBottomSheet(
                            availableFonts = uiState.availableFonts,
                            selectedFont = uiState.selectedFont,
                            onFontSelect = {
                                viewModel.onFontSelected(it)
                                viewModel.closeSheet()
                            },
                            onImportFontClick = onImportFontClick
                        )

                        ActiveSheet.SIZE_COLOR -> SizeColorBottomSheet(
                            fontSizePt = uiState.fontSizePt,
                            textColor = uiState.textColor,
                            pageColor = uiState.pageColor,
                            onFontSizeChange = { viewModel.onFontSizeChange(it) },
                            onFontSizeInc = { viewModel.incrementFontSize() },
                            onFontSizeDec = { viewModel.decrementFontSize() },
                            onTextColorChange = { viewModel.onTextColorChanged(it) },
                            onPageColorChange = { viewModel.onPageColorChanged(it) }
                        )

                        ActiveSheet.ALIGN -> AlignmentBottomSheet(
                            currentAlign = uiState.textAlign,
                            onAlignSelect = {
                                viewModel.onTextAlignChanged(it)
                                viewModel.closeSheet()
                            }
                        )

                        ActiveSheet.MARGINS -> MarginsBottomSheet(
                            margins = uiState.margins,
                            onMarginsChange = { top, bottom, right, left ->
                                viewModel.onMarginsChanged(top, bottom, right, left)
                            },
                            onUnitChange = { viewModel.onMarginUnitChanged(it) }
                        )

                        ActiveSheet.KASHIDA -> KashidaBottomSheet(
                            kashidaEnabled = uiState.kashidaEnabled,
                            kashidaLevel = uiState.kashidaLevel,
                            onToggleKashida = { viewModel.toggleKashida() },
                            onLevelChange = { viewModel.onKashidaLevelChanged(it) },
                            onApplySmartKashida = {
                                viewModel.applySmartKashidaToDocument()
                                viewModel.closeSheet()
                            },
                            onOpenManualMode = {
                                viewModel.toggleManualKashidaMode()
                                viewModel.closeSheet()
                            }
                        )

                        ActiveSheet.NONE -> {}
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// 1. FONT BOTTOM SHEET
// ----------------------------------------------------------------------
@Composable
fun FontBottomSheet(
    availableFonts: List<FontItem>,
    selectedFont: FontItem,
    onFontSelect: (FontItem) -> Unit,
    onImportFontClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "اختيار نوع الخط",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FilledTonalButton(
                onClick = onImportFontClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("sheet_import_font_btn")
            ) {
                Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("استيراد خط جديد", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableFonts) { font ->
                val isSelected = font.id == selectedFont.id
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFontSelect(font) }
                        .testTag("sheet_font_item_${font.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = font.nameAr,
                                    fontFamily = font.fontFamily,
                                    fontSize = 16.sp,
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
                            Text(
                                text = "أَبْجَدِيَّةُ الضَّادِ وَجَمَالُ الحَرْفِ العَرَبِيّ",
                                fontFamily = font.fontFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// 2. SIZE & COLOR BOTTOM SHEET
// ----------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SizeColorBottomSheet(
    fontSizePt: Float,
    textColor: Color,
    pageColor: Color,
    onFontSizeChange: (Float) -> Unit,
    onFontSizeInc: () -> Unit,
    onFontSizeDec: () -> Unit,
    onTextColorChange: (Color) -> Unit,
    onPageColorChange: (Color) -> Unit
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "حجم الخط والألوان",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Font Size Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
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
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "تقليل")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "${fontSizePt.toInt()} pt",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onFontSizeInc,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "زيادة")
                }
            }

            Slider(
                value = fontSizePt,
                onValueChange = onFontSizeChange,
                valueRange = 10f..72f,
                steps = 61,
                modifier = Modifier
                    .width(130.dp)
                    .testTag("sheet_font_slider")
            )
        }

        // Text Color Palette
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "لون النص:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                textPalette.forEach { (name, color) ->
                    val isSelected = textColor == color
                    ColorItemChip(name = name, color = color, isSelected = isSelected) {
                        onTextColorChange(color)
                    }
                }
            }
        }

        // Page Color Palette
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "لون خلفية الصفحة:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pagePalette.forEach { (name, color) ->
                    val isSelected = pageColor == color
                    ColorItemChip(name = name, color = color, isSelected = isSelected) {
                        onPageColorChange(color)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorItemChip(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("sheet_color_$name")
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
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color.red + color.green + color.blue > 1.5f) Color.Black else Color.White,
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            Text(name, fontSize = 11.sp)
        }
    }
}

// ----------------------------------------------------------------------
// 3. ALIGNMENT BOTTOM SHEET
// ----------------------------------------------------------------------
@Composable
fun AlignmentBottomSheet(
    currentAlign: TextAlignOption,
    onAlignSelect: (TextAlignOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "محاذاة النص والأسطر",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val alignOptions = listOf(
                Triple(TextAlignOption.RIGHT, Icons.AutoMirrored.Outlined.FormatAlignRight, "يمين"),
                Triple(TextAlignOption.CENTER, Icons.Outlined.FormatAlignCenter, "وسط"),
                Triple(TextAlignOption.LEFT, Icons.AutoMirrored.Outlined.FormatAlignLeft, "يسار"),
                Triple(TextAlignOption.JUSTIFY, Icons.Outlined.FormatAlignJustify, "ضبط كامل")
            )

            alignOptions.forEach { (option, icon, label) ->
                val isSelected = currentAlign == option
                OutlinedCard(
                    onClick = { onAlignSelect(option) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("align_sheet_${option.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// 4. MARGINS BOTTOM SHEET (مع رسم تخطيطي لصفحة)
// ----------------------------------------------------------------------
@Composable
fun MarginsBottomSheet(
    margins: PageMargins,
    onMarginsChange: (Float, Float, Float, Float) -> Unit,
    onUnitChange: (MarginUnit) -> Unit
) {
    val step = if (margins.unit == MarginUnit.CENTIMETER) 0.2f else 2f
    val topVal = margins.displayTop()
    val bottomVal = margins.displayBottom()
    val rightVal = margins.displayRight()
    val leftVal = margins.displayLeft()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "هوامش الصفحة والطباعة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MarginUnit.entries.forEach { unit ->
                    val isSelected = margins.unit == unit
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUnitChange(unit) },
                        label = { Text(unit.symbolAr, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Mini Page Diagram with visually placed inputs
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Margin Input
                MarginStepper(
                    label = "علوي",
                    value = topVal,
                    unitStr = margins.unit.symbolAr,
                    onInc = { onMarginsChange(topVal + step, bottomVal, rightVal, leftVal) },
                    onDec = { onMarginsChange((topVal - step).coerceAtLeast(0.5f), bottomVal, rightVal, leftVal) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right Margin Input
                    MarginStepper(
                        label = "يمين",
                        value = rightVal,
                        unitStr = margins.unit.symbolAr,
                        onInc = { onMarginsChange(topVal, bottomVal, rightVal + step, leftVal) },
                        onDec = { onMarginsChange(topVal, bottomVal, (rightVal - step).coerceAtLeast(0.5f), leftVal) }
                    )

                    // Page representation inside diagram
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .width(100.dp)
                            .height(70.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "مساحة الكتابة",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Left Margin Input
                    MarginStepper(
                        label = "يسار",
                        value = leftVal,
                        unitStr = margins.unit.symbolAr,
                        onInc = { onMarginsChange(topVal, bottomVal, rightVal, leftVal + step) },
                        onDec = { onMarginsChange(topVal, bottomVal, rightVal, (leftVal - step).coerceAtLeast(0.5f)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Margin Input
                MarginStepper(
                    label = "سفلي",
                    value = bottomVal,
                    unitStr = margins.unit.symbolAr,
                    onInc = { onMarginsChange(topVal, bottomVal + step, rightVal, leftVal) },
                    onDec = { onMarginsChange(topVal, (bottomVal - step).coerceAtLeast(0.5f), rightVal, leftVal) }
                )
            }
        }
    }
}

@Composable
private fun MarginStepper(
    label: String,
    value: Float,
    unitStr: String,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onDec, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Remove, contentDescription = "تقليل", modifier = Modifier.size(12.dp))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = String.format("%.1f %s", value, unitStr),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onInc, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = "زيادة", modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ----------------------------------------------------------------------
// 5. KASHIDA BOTTOM SHEET
// ----------------------------------------------------------------------
@Composable
fun KashidaBottomSheet(
    kashidaEnabled: Boolean,
    kashidaLevel: KashidaLevel,
    onToggleKashida: () -> Unit,
    onLevelChange: (KashidaLevel) -> Unit,
    onApplySmartKashida: () -> Unit,
    onOpenManualMode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Toggle Switch Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "تقنية الكشيدة الذكية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "توزيع التطويل والمد وفق أصول الخط والطباعة العربية",
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
                modifier = Modifier.testTag("sheet_kashida_switch")
            )
        }

        // 3 Preset Level Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(KashidaLevel.LIGHT, KashidaLevel.MEDIUM, KashidaLevel.HEAVY).forEach { level ->
                val isSelected = kashidaEnabled && kashidaLevel == level
                FilterChip(
                    selected = isSelected,
                    onClick = { onLevelChange(level) },
                    label = { Text(level.labelAr, fontSize = 12.sp) },
                    enabled = kashidaEnabled,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Buttons
        FilledTonalButton(
            onClick = onApplySmartKashida,
            enabled = kashidaEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تطبيق الكشيدة التلقائية على النص")
        }

        // Advanced Manual Mode Link
        OutlinedButton(
            onClick = onOpenManualMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Gesture, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تحكم يدوي متقدم (لمس نقاط الاتصال)")
        }
    }
}

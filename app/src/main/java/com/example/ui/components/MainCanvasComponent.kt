package com.example.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kashida.KashidaEngine
import com.example.ui.viewmodel.EditorUiState

@Composable
fun MainCanvasComponent(
    uiState: EditorUiState,
    onTextChanged: (String) -> Unit,
    onApplyManualKashida: (cleanWord: String, connectionIndex: Int, count: Int, wordIndex: Int) -> Unit,
    onExitManualMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Margin padding simulation relative to screen scale
    // Normalizing 20mm to ~16-24dp for comfortable mobile viewport
    val topPaddingDp = (uiState.margins.topMm * 0.8f).coerceIn(8f, 48f).dp
    val bottomPaddingDp = (uiState.margins.bottomMm * 0.8f).coerceIn(8f, 48f).dp
    val rightPaddingDp = (uiState.margins.rightMm * 0.8f).coerceIn(8f, 48f).dp
    val leftPaddingDp = (uiState.margins.leftMm * 0.8f).coerceIn(8f, 48f).dp

    val aspectRatio = uiState.paperSize.widthMm / uiState.paperSize.heightMm

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 680.dp)
            ) {
                // Realistic Paper Sheet Container
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = uiState.pageColor,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .testTag("document_paper_sheet")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = topPaddingDp,
                                bottom = bottomPaddingDp,
                                end = leftPaddingDp,
                                start = rightPaddingDp
                            )
                    ) {
                        // Visual margin guideline (faint border showing printable area)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            if (uiState.manualKashidaMode) {
                                // Interactive Manual Kashida Mode View
                                ManualKashidaInteractiveView(
                                    text = uiState.text,
                                    font = uiState.selectedFont,
                                    fontSizePt = uiState.fontSizePt,
                                    textColor = uiState.textColor,
                                    align = uiState.textAlign,
                                    onApplyKashida = onApplyManualKashida,
                                    onExitManualMode = onExitManualMode
                                )
                            } else {
                                // Standard Interactive Typing Canvas
                                BasicTextField(
                                    value = uiState.text,
                                    onValueChange = onTextChanged,
                                    textStyle = TextStyle(
                                        fontFamily = uiState.selectedFont.fontFamily,
                                        fontSize = uiState.fontSizePt.sp,
                                        color = uiState.textColor,
                                        textAlign = uiState.textAlign.composeAlign,
                                        lineHeight = (uiState.fontSizePt * 1.55f).sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("document_text_field")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

/**
 * Interactive Manual Kashida inspection component.
 * Allows tapping on connection points between letters to stretch them with a live slider.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualKashidaInteractiveView(
    text: String,
    font: com.example.model.FontItem,
    fontSizePt: Float,
    textColor: Color,
    align: com.example.model.TextAlignOption,
    onApplyKashida: (cleanWord: String, connectionIndex: Int, count: Int, wordIndex: Int) -> Unit,
    onExitManualMode: () -> Unit
) {
    val words = remember(text) { text.split(" ") }

    var selectedWordIdx by remember { mutableStateOf<Int?>(null) }
    var selectedPointIdx by remember { mutableStateOf<Int?>(null) }
    var tatweelSliderVal by remember { mutableIntStateOf(1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Mode Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Gesture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "انقر على أي نقطة زرقاء لتمديد الكشيدة يدوياً",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            IconButton(
                onClick = onExitManualMode,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Word Flow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = when (align) {
                com.example.model.TextAlignOption.CENTER -> Arrangement.Center
                com.example.model.TextAlignOption.LEFT -> Arrangement.End
                else -> Arrangement.Start
            },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.forEachIndexed { wordIdx, word ->
                val cleanWord = KashidaEngine.stripKashida(word)
                val points = remember(cleanWord) { KashidaEngine.findConnectionPointsInWord(cleanWord) }
                val isWordSelected = selectedWordIdx == wordIdx

                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWordSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                    ),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Word Text
                        Text(
                            text = word,
                            fontFamily = font.fontFamily,
                            fontSize = fontSizePt.sp,
                            color = textColor
                        )

                        // Connection Points Dots
                        if (points.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                points.forEachIndexed { pIdx, point ->
                                    val isPointSelected = isWordSelected && selectedPointIdx == pIdx
                                    Box(
                                        modifier = Modifier
                                            .size(if (isPointSelected) 14.dp else 10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPointSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                            )
                                            .clickable {
                                                selectedWordIdx = wordIdx
                                                selectedPointIdx = pIdx
                                                tatweelSliderVal = 2
                                            }
                                            .testTag("kashida_point_${wordIdx}_$pIdx")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Inspector & Slider for Selected Point
        if (selectedWordIdx != null && selectedPointIdx != null && selectedWordIdx!! in words.indices) {
            val curWord = words[selectedWordIdx!!]
            val cleanWord = KashidaEngine.stripKashida(curWord)
            val points = KashidaEngine.findConnectionPointsInWord(cleanWord)

            if (selectedPointIdx!! in points.indices) {
                val point = points[selectedPointIdx!!]
                val previewWord = KashidaEngine.applyManualKashidaToWord(cleanWord, point.indexInWord, tatweelSliderVal)

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تمديد الاتصال بين: ( ${point.firstChar}ـ ) و ( ـ${point.secondChar} )",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    selectedWordIdx = null
                                    selectedPointIdx = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء")
                            }
                        }

                        // Magnified Word Preview
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = previewWord,
                                    fontFamily = font.fontFamily,
                                    fontSize = (fontSizePt * 1.4f).sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Slider & Steppers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    if (tatweelSliderVal > 0) {
                                        tatweelSliderVal--
                                        onApplyKashida(cleanWord, point.indexInWord, tatweelSliderVal, selectedWordIdx!!)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "تقليص")
                            }

                            Slider(
                                value = tatweelSliderVal.toFloat(),
                                onValueChange = {
                                    tatweelSliderVal = it.toInt()
                                    onApplyKashida(cleanWord, point.indexInWord, tatweelSliderVal, selectedWordIdx!!)
                                },
                                valueRange = 0f..8f,
                                steps = 7,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    if (tatweelSliderVal < 8) {
                                        tatweelSliderVal++
                                        onApplyKashida(cleanWord, point.indexInWord, tatweelSliderVal, selectedWordIdx!!)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "تمديد")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مستوى التمديد: $tatweelSliderVal كشيدة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            FilledTonalButton(
                                onClick = {
                                    onApplyKashida(cleanWord, point.indexInWord, tatweelSliderVal, selectedWordIdx!!)
                                    selectedWordIdx = null
                                    selectedPointIdx = null
                                }
                            ) {
                                Text("حفظ التمديد")
                            }
                        }
                    }
                }
            }
        }
    }
}

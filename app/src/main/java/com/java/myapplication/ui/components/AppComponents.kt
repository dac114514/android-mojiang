package com.java.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import com.java.myapplication.ui.theme.CardElevation

@Composable
fun GradientHeroCard(
    title: String,
    subtitle: String,
    footnote: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation.level2)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                )
                Text(
                    text = footnote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    caption: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation.level1)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class StatusType { SUCCESS, WARNING, ERROR, INFO }

@Composable
fun StatusBadge(text: String, type: StatusType = StatusType.INFO) {
    val (bgColor, textColor) = when (type) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        StatusType.WARNING -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onPrimary
        StatusType.ERROR -> Color(0xFFE74C3C) to Color.White
        StatusType.INFO -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = textColor
        )
    }
}

@Composable
fun LoadingOverlay(isLoading: Boolean, message: String = "处理中…") {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = CardElevation.level2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun DotBadge(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(color)
        )
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ChapterRangeFields(
    startChapter: String,
    endChapter: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    startLabel: String = "起始章节",
    endLabel: String = "结束章节"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = startChapter,
            onValueChange = onStartChange,
            modifier = Modifier.weight(1f),
            label = { Text(startLabel) },
            singleLine = true
        )
        OutlinedTextField(
            value = endChapter,
            onValueChange = onEndChange,
            modifier = Modifier.weight(1f),
            label = { Text(endLabel) },
            singleLine = true
        )
    }
}

fun buildRangeSummary(startChapter: String, endChapter: String, unit: String = "章"): String {
    val start = startChapter.trim()
    val end = endChapter.trim()
    return when {
        start.isBlank() && end.isBlank() -> "未设置范围"
        start.isNotBlank() && end.isBlank() -> "从第 $start ${unit}开始"
        start.isBlank() && end.isNotBlank() -> "到第 $end ${unit}结束"
        else -> "第 $start-$end $unit"
    }
}
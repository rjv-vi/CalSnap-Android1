package com.rjvvi.calsnap.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rjvvi.calsnap.data.db.WeightEntry
import com.rjvvi.calsnap.ui.theme.*
import com.rjvvi.calsnap.viewmodel.DaySummary
import com.rjvvi.calsnap.viewmodel.ProgressViewModel
import kotlin.math.max

@Composable
fun ProgressScreen(vm: ProgressViewModel) {
    val weeklyData by vm.weeklyData.collectAsStateWithLifecycle()
    val monthlyData by vm.monthlyData.collectAsStateWithLifecycle()
    val allWeights by vm.allWeights.collectAsStateWithLifecycle()
    val calorieGoal by vm.calorieGoal.collectAsStateWithLifecycle()

    var showLogWeight by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        // Header
        Text(
            "Прогресс",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // Period tabs
        val periods = listOf("7 дней", "30 дней")
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            periods.forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedPeriod == i) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { selectedPeriod = i }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontWeight = if (selectedPeriod == i) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Stats cards
        val data = if (selectedPeriod == 0) weeklyData else monthlyData
        StatsSummaryRow(data, calorieGoal)

        // Bar chart
        if (data.isNotEmpty()) {
            CalorieBarChart(data = data, goal = calorieGoal)
        }

        // Macros chart
        if (data.isNotEmpty()) {
            MacrosPieSection(data)
        }

        // Weight
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("⚖️ Вес", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = { showLogWeight = true }, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(36.dp)) { Text("+ Записать", fontSize = 13.sp) }
                }
                Spacer(Modifier.height(12.dp))
                if (allWeights.isNotEmpty()) {
                    WeightChart(allWeights)
                    Spacer(Modifier.height(8.dp))
                    val latest = allWeights.first()
                    val oldest = allWeights.last()
                    val diff = latest.weight - oldest.weight
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        WeightStat("Текущий", "${latest.weight} кг")
                        WeightStat("Изменение", "${if (diff >= 0) "+" else ""}${"%.1f".format(diff)} кг")
                        WeightStat("Начало", "${oldest.weight} кг")
                    }
                } else {
                    Text("Нет записей веса. Нажми «Записать» чтобы начать отслеживать.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    if (showLogWeight) {
        LogWeightDialog(onDismiss = { showLogWeight = false }, onSave = { vm.logWeight(it) })
    }
}

@Composable
private fun StatsSummaryRow(data: List<DaySummary>, goal: Int) {
    val avgCals = if (data.isEmpty()) 0 else data.filter { it.calories > 0 }.let { active -> if (active.isEmpty()) 0 else active.sumOf { it.calories } / active.size }
    val daysLogged = data.count { it.calories > 0 }
    val bestDay = data.maxByOrNull { it.calories }

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Дней", "$daysLogged", "📅", Modifier.weight(1f))
        StatCard("Среднее", "$avgCals ккал", "📊", Modifier.weight(1f))
        StatCard("Максимум", "${bestDay?.calories ?: 0} ккал", "🏆", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, emoji: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalorieBarChart(data: List<DaySummary>, goal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Калории по дням", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            val maxVal = max(data.maxOfOrNull { it.calories } ?: 0, goal).toFloat()
            val okColor = OkGreen
            val warnColor = Color(0xFFF59E0B)
            val errColor = MaterialTheme.colorScheme.error
            val goalLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val barW = size.width / (data.size * 1.5f + 0.5f)
                val gap = barW * 0.5f
                val goalY = size.height - (goal.toFloat() / maxVal) * size.height

                // Goal line
                drawLine(goalLineColor, Offset(0f, goalY), Offset(size.width, goalY), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))

                data.forEachIndexed { i, day ->
                    val x = gap + i * (barW + gap)
                    val barH = (day.calories.toFloat() / maxVal) * size.height
                    val color = when {
                        day.calories == 0 -> Color.Transparent
                        day.calories > goal -> errColor
                        day.calories > goal * 0.85 -> warnColor
                        else -> okColor
                    }
                    if (barH > 0) {
                        drawRoundRect(color = color, topLeft = Offset(x, size.height - barH), size = Size(barW, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                    }
                }
            }

            // Labels
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                data.forEach { day ->
                    Text(day.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MacrosPieSection(data: List<DaySummary>) {
    val totalP = data.sumOf { it.protein.toDouble() }.toFloat()
    val totalC = data.sumOf { it.carbs.toDouble() }.toFloat()
    val totalF = data.sumOf { it.fat.toDouble() }.toFloat()
    val total = totalP + totalC + totalF

    if (total <= 0) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Pie chart
            Canvas(Modifier.size(100.dp)) {
                val slices = listOf(
                    totalP / total to ProteinColor,
                    totalC / total to CarbsColor,
                    totalF / total to FatColor
                )
                var startAngle = -90f
                slices.forEach { (fraction, color) ->
                    val sweep = fraction * 360f
                    drawArc(color = color, startAngle = startAngle, sweepAngle = sweep, useCenter = false, style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt))
                    startAngle += sweep
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Макросы (среднее)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                MacroLegendRow("Белки", totalP / data.size, ProteinColor, total)
                MacroLegendRow("Углеводы", totalC / data.size, CarbsColor, total)
                MacroLegendRow("Жиры", totalF / data.size, FatColor, total)
            }
        }
    }
}

@Composable
private fun MacroLegendRow(label: String, avg: Float, color: Color, total: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text("$label: ${avg.toInt()}г", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WeightChart(weights: List<WeightEntry>) {
    val sorted = weights.sortedBy { it.date }
    if (sorted.size < 2) return

    val minW = sorted.minOf { it.weight }
    val maxW = sorted.maxOf { it.weight }
    val range = (maxW - minW).coerceAtLeast(1f)

    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val pts = sorted.mapIndexed { i, e ->
            Offset(i.toFloat() / (sorted.size - 1) * size.width, size.height - (e.weight - minW) / range * size.height)
        }
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = ProteinColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        pts.forEach { pt ->
            drawCircle(ProteinColor, radius = 4.dp.toPx(), center = pt)
        }
    }
}

@Composable
private fun WeightStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LogWeightDialog(onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var weight by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Записать вес") },
        text = {
            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Вес (кг)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp))
        },
        confirmButton = { Button(onClick = { weight.toFloatOrNull()?.let { onSave(it); onDismiss() } }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

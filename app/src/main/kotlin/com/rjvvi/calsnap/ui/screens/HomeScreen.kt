package com.rjvvi.calsnap.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rjvvi.calsnap.data.db.FoodEntry
import com.rjvvi.calsnap.ui.theme.*
import com.rjvvi.calsnap.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun HomeScreen(vm: HomeViewModel, onAddClick: () -> Unit) {
    val todayFood by vm.todayFood.collectAsStateWithLifecycle()
    val todayCalories by vm.todayCalories.collectAsStateWithLifecycle()
    val todayProtein by vm.todayProtein.collectAsStateWithLifecycle()
    val todayCarbs by vm.todayCarbs.collectAsStateWithLifecycle()
    val todayFat by vm.todayFat.collectAsStateWithLifecycle()
    val calorieGoal by vm.calorieGoal.collectAsStateWithLifecycle()
    val todayWater by vm.todayWater.collectAsStateWithLifecycle()
    val waterGoal by vm.waterGoal.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val userName by vm.userName.collectAsStateWithLifecycle()

    val date = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date())
            .replaceFirstChar { it.uppercase() }
    }

    val accent = MaterialTheme.colorScheme.onBackground
    val streakCol = streakColor()

    var editEntry by remember { mutableStateOf<FoodEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<FoodEntry?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (userName.isNotBlank()) "Привет, $userName! 👋" else "Дневник питания",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                    )
                    Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (streak > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = streakCol.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 16.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("$streak", fontWeight = FontWeight.Bold, color = streakCol)
                        }
                    }
                }
            }
        }

        // Calorie Ring
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Ring
                        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                            CalorieRing(consumed = todayCalories, goal = calorieGoal)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$todayCalories",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                                Text("ккал", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "из $calorieGoal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(20.dp))
                        // Macros
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MacroRow("Белки", todayProtein, ProteinColor)
                            MacroRow("Углеводы", todayCarbs, CarbsColor)
                            MacroRow("Жиры", todayFat, FatColor)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (todayCalories.toFloat() / calorieGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = when {
                            todayCalories > calorieGoal -> MaterialTheme.colorScheme.error
                            todayCalories > calorieGoal * 0.85 -> Color(0xFFF59E0B)
                            else -> OkGreen
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "${(calorieGoal - todayCalories).coerceAtLeast(0)} ккал осталось",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Water tracker
        item {
            WaterCard(todayWater = todayWater, goal = waterGoal, onAdd = { vm.addWater(it) }, onUndo = { vm.undoWater() })
        }

        // Meal groups
        val meals = listOf(
            Triple("breakfast", "🌅 Завтрак", todayFood.filter { it.mealType == "breakfast" }),
            Triple("lunch", "☀️ Обед", todayFood.filter { it.mealType == "lunch" }),
            Triple("snack", "🍎 Перекус", todayFood.filter { it.mealType == "snack" }),
            Triple("dinner", "🌙 Ужин", todayFood.filter { it.mealType == "dinner" })
        )

        meals.forEach { (_, label, entries) ->
            if (entries.isNotEmpty()) {
                item {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(entries, key = { it.id }) { entry ->
                    FoodEntryCard(
                        entry = entry,
                        onDelete = { deleteEntry = entry },
                        onEdit = { editEntry = entry }
                    )
                }
            }
        }

        if (todayFood.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🍽️", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Дневник пуст", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Нажми + чтобы добавить еду", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    }

    // Edit dialog
    editEntry?.let { entry ->
        EditFoodDialog(entry, onDismiss = { editEntry = null }, onSave = { vm.updateFood(it); editEntry = null })
    }

    // Delete confirm
    deleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteEntry = null },
            title = { Text("Удалить запись?") },
            text = { Text(entry.name) },
            confirmButton = { TextButton(onClick = { vm.deleteFood(entry); deleteEntry = null }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteEntry = null }) { Text("Отмена") } }
        )
    }
}

@Composable
fun CalorieRing(consumed: Int, goal: Int) {
    val progress = (consumed.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animProg by animateFloatAsState(progress, animationSpec = tween(800, easing = EaseOutQuart), label = "ring")
    val over = consumed > goal
    val ringColor = when {
        over -> Color(0xFFEF4444)
        progress > 0.85f -> Color(0xFFF59E0B)
        else -> OkGreen
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
        val stroke = 12.dp.toPx()
        val pad = stroke / 2
        drawArc(
            color = trackColor,
            startAngle = -90f, sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        if (animProg > 0f) {
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animProg,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun MacroRow(name: String, value: Float, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text("${value.toInt()}г", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun WaterCard(todayWater: Int, goal: Int, onAdd: (Int) -> Unit, onUndo: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("💧", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Вода", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("$todayWater / $goal мл", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (todayWater.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF3B82F6),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallIconBtn("↩️") { onUndo() }
                SmallIconBtn("+ 250") { onAdd(250) }
            }
        }
    }
}

@Composable
private fun SmallIconBtn(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FoodEntryCard(entry: FoodEntry, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(entry.portion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroChip("Б ${entry.protein.toInt()}г", ProteinColor)
                    MacroChip("У ${entry.carbs.toInt()}г", CarbsColor)
                    MacroChip("Ж ${entry.fat.toInt()}г", FatColor)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.calories}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("ккал", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun MacroChip(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EditFoodDialog(entry: FoodEntry, onDismiss: () -> Unit, onSave: (FoodEntry) -> Unit) {
    var name by remember { mutableStateOf(entry.name) }
    var calories by remember { mutableStateOf(entry.calories.toString()) }
    var protein by remember { mutableStateOf(entry.protein.toString()) }
    var carbs by remember { mutableStateOf(entry.carbs.toString()) }
    var fat by remember { mutableStateOf(entry.fat.toString()) }
    var portion by remember { mutableStateOf(entry.portion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("ккал") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    OutlinedTextField(value = portion, onValueChange = { portion = it }, label = { Text("Порция") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Белки г") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Углев. г") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Жиры г") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(entry.copy(
                    name = name,
                    calories = calories.toIntOrNull() ?: entry.calories,
                    protein = protein.toFloatOrNull() ?: entry.protein,
                    carbs = carbs.toFloatOrNull() ?: entry.carbs,
                    fat = fat.toFloatOrNull() ?: entry.fat,
                    portion = portion
                ))
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

package com.rjvvi.calsnap.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rjvvi.calsnap.data.prefs.UserPrefs
import com.rjvvi.calsnap.ui.theme.Streak
import com.rjvvi.calsnap.ui.theme.streakColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val prefs = remember { (app as com.rjvvi.calsnap.CalSnapApp).prefs }
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    // Form state
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var dob by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("170") }
    var weightKg by remember { mutableStateOf("70") }
    var goal by remember { mutableStateOf("maintain") }
    var activity by remember { mutableStateOf("moderate") }

    val accent = streakColor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Logo + progress
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("🍎", fontSize = 52.sp)
        }
        Text(
            "Cal\uD835\uDC92nap".replace("\uD835\uDC92", "S"),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        // Step indicators
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline)
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        // Step content
        AnimatedContent(targetState = step, transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
        }, label = "ob_step") { s ->
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (s) {
                    0 -> NameGenderStep(name, gender, onName = { name = it }, onGender = { gender = it })
                    1 -> DobStep(dob, onDob = { dob = it })
                    2 -> BodyStep(heightCm, weightKg, onHeight = { heightCm = it }, onWeight = { weightKg = it })
                    3 -> GoalStep(goal, onGoal = { goal = it })
                    4 -> ActivityStep(activity, onActivity = { activity = it })
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (step < totalSteps - 1) {
                    step++
                } else {
                    scope.launch {
                        prefs.saveProfile(
                            name = name,
                            gender = gender,
                            dob = dob.ifBlank { "2000-01-01" },
                            height = heightCm.toFloatOrNull() ?: 170f,
                            weight = weightKg.toFloatOrNull() ?: 70f,
                            goal = goal,
                            activity = activity
                        )
                        onFinished()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                if (step < totalSteps - 1) "Далее" else "Начать!",
                fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        if (step > 0) {
            TextButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) {
                Text("Назад", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NameGenderStep(name: String, gender: String, onName: (String) -> Unit, onGender: (String) -> Unit) {
    Text("Как тебя зовут?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Персонализируем расчёты под тебя", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
    OutlinedTextField(
        value = name, onValueChange = onName,
        label = { Text("Имя (необязательно)") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
    Spacer(Modifier.height(20.dp))
    Text("Пол", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("male" to "♂ Мужской", "female" to "♀ Женский").forEach { (key, label) ->
            val selected = gender == key
            OutlinedCard(
                onClick = { onGender(key) },
                modifier = Modifier.weight(1f),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                colors = CardDefaults.outlinedCardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun DobStep(dob: String, onDob: (String) -> Unit) {
    Text("Дата рождения", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Нужна для точного расчёта TDEE", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
    OutlinedTextField(
        value = dob,
        onValueChange = { if (it.length <= 10) onDob(it) },
        label = { Text("ГГГГ-ММ-ДД") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        placeholder = { Text("2000-01-15") }
    )
}

@Composable
private fun BodyStep(height: String, weight: String, onHeight: (String) -> Unit, onWeight: (String) -> Unit) {
    Text("Параметры тела", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Для расчёта нормы калорий", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = height, onValueChange = onHeight,
            label = { Text("Рост (см)") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            value = weight, onValueChange = onWeight,
            label = { Text("Вес (кг)") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
    }
}

@Composable
private fun GoalStep(goal: String, onGoal: (String) -> Unit) {
    Text("Твоя цель", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Выбери, чего хочешь достичь", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
    val goals = listOf(
        Triple("lose", "🔥", "Похудеть"),
        Triple("maintain", "⚖️", "Держать вес"),
        Triple("gain", "💪", "Набрать массу")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        goals.forEach { (key, emoji, label) ->
            val selected = goal == key
            OutlinedCard(
                onClick = { onGoal(key) },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                colors = CardDefaults.outlinedCardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(16.dp))
                    Text(label, fontSize = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun ActivityStep(activity: String, onActivity: (String) -> Unit) {
    Text("Уровень активности", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Сколько ты двигаешься в обычный день", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
    val levels = listOf(
        Triple("sedentary", "🛋️", "Сидячий образ жизни"),
        Triple("light", "🚶", "Лёгкая активность (1-3 дня/нед.)"),
        Triple("moderate", "🏃", "Умеренная (3-5 дней/нед.)"),
        Triple("active", "🏋️", "Высокая (6-7 дней/нед.)"),
        Triple("very_active", "⚡", "Очень высокая (2x в день)")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { (key, emoji, label) ->
            val selected = activity == key
            OutlinedCard(
                onClick = { onActivity(key) },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                colors = CardDefaults.outlinedCardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

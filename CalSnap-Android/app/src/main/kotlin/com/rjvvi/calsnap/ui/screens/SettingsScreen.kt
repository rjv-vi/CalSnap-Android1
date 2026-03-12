package com.rjvvi.calsnap.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rjvvi.calsnap.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val theme by vm.theme.collectAsStateWithLifecycle()
    val sounds by vm.soundsEnabled.collectAsStateWithLifecycle()
    val haptic by vm.hapticEnabled.collectAsStateWithLifecycle()
    val notif by vm.notifEnabled.collectAsStateWithLifecycle()
    val apiKey by vm.apiKey.collectAsStateWithLifecycle()
    val model by vm.geminiModel.collectAsStateWithLifecycle()
    val calorieGoal by vm.calorieGoal.collectAsStateWithLifecycle()
    val waterGoal by vm.waterGoal.collectAsStateWithLifecycle()
    val name by vm.name.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val models by vm.availableModels.collectAsStateWithLifecycle()
    val modelsLoading by vm.modelsLoading.collectAsStateWithLifecycle()

    var apiKeyEdit by remember(apiKey) { mutableStateOf(apiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var calorieEdit by remember(calorieGoal) { mutableStateOf(calorieGoal.toString()) }
    var waterEdit by remember(waterGoal) { mutableStateOf(waterGoal.toString()) }
    var nameEdit by remember(name) { mutableStateOf(name) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            if (streak > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("$streak дней", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // ── ПРОФИЛЬ ──
        SettingsSection("Профиль") {
            SettingsTextField(label = "Имя", value = nameEdit, onValueChange = { nameEdit = it }, onDone = { vm.setName(nameEdit) }, icon = Icons.Default.Person)
        }

        // ── ЦЕЛИ ──
        SettingsSection("Цели") {
            SettingsTextField(
                label = "Калории в день (ккал)",
                value = calorieEdit,
                onValueChange = { calorieEdit = it },
                onDone = { calorieEdit.toIntOrNull()?.let { vm.setCalorieGoal(it) } },
                icon = Icons.Default.LocalFireDepartment,
                keyboardType = KeyboardType.Number
            )
            SettingsTextField(
                label = "Вода в день (мл)",
                value = waterEdit,
                onValueChange = { waterEdit = it },
                onDone = { waterEdit.toIntOrNull()?.let { vm.setWaterGoal(it) } },
                icon = Icons.Default.WaterDrop,
                keyboardType = KeyboardType.Number
            )
        }

        // ── GEMINI AI ──
        SettingsSection("Gemini AI") {
            // API Key field
            OutlinedTextField(
                value = apiKeyEdit,
                onValueChange = { apiKeyEdit = it },
                label = { Text("API ключ Gemini") },
                placeholder = { Text("AIza...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { vm.setApiKey(apiKeyEdit) }) {
                            Icon(Icons.Default.Save, contentDescription = "Сохранить", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                supportingText = { Text("Получи бесплатно на aistudio.google.com", style = MaterialTheme.typography.labelSmall) }
            )

            SettingsRow(
                icon = Icons.Default.Psychology,
                title = "Модель Gemini",
                subtitle = model,
                onClick = {
                    showModelPicker = true
                    vm.loadModels()
                }
            )

            TextButton(
                onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")); context.startActivity(intent) },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) { Text("Получить API ключ →") }
        }

        // ── ВНЕШНИЙ ВИД ──
        SettingsSection("Внешний вид") {
            SettingsRow(
                icon = Icons.Default.DarkMode,
                title = "Тема",
                subtitle = when (theme) { "dark" -> "Тёмная"; "light" -> "Светлая"; else -> "Системная" },
                onClick = { showThemePicker = true }
            )
            SettingsSwitchRow(icon = Icons.Default.Vibration, title = "Вибрация", checked = haptic, onCheckedChange = { vm.setHaptic(it) })
        }

        // ── УВЕДОМЛЕНИЯ ──
        SettingsSection("Уведомления") {
            SettingsSwitchRow(icon = Icons.Default.Notifications, title = "Напоминания о еде", checked = notif, onCheckedChange = { vm.setNotif(it) })
        }

        // ── ДАННЫЕ ──
        SettingsSection("Данные и экспорт") {
            SettingsRow(
                icon = Icons.Default.Upload,
                title = "Экспорт данных",
                subtitle = "Сохранить все записи в JSON",
                onClick = {
                    scope.launch {
                        val file = vm.exportJson(context)
                        if (file != null) {
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Экспорт данных"))
                        }
                    }
                }
            )
            SettingsRow(
                icon = Icons.Default.Delete,
                title = "Сбросить все данные",
                subtitle = "Удалить дневник, вес и настройки",
                onClick = { showResetConfirm = true },
                tint = MaterialTheme.colorScheme.error
            )
        }

        // ── О ПРИЛОЖЕНИИ ──
        SettingsSection("О приложении") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍎", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("CalSnap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Версия 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Сделано с заботой о здоровье 🫀", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    Text("AI на базе Gemini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    // Theme picker dialog
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("Тема") },
            text = {
                Column {
                    listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная").forEach { (key, label) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { vm.setTheme(key); showThemePicker = false }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = theme == key, onClick = { vm.setTheme(key); showThemePicker = false })
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemePicker = false }) { Text("Закрыть") } }
        )
    }

    // Model picker dialog
    if (showModelPicker) {
        AlertDialog(
            onDismissRequest = { showModelPicker = false },
            title = { Text("Модель Gemini") },
            text = {
                Column {
                    if (modelsLoading) {
                        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    val displayModels = models.ifEmpty {
                        listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash", "gemini-2.0-flash-lite")
                    }
                    displayModels.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { vm.setModel(m); showModelPicker = false }.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = model == m, onClick = { vm.setModel(m); showModelPicker = false })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(m, style = MaterialTheme.typography.bodyMedium)
                                if (m.contains("flash")) Text("Быстрая, бесплатная", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (m.contains("pro")) Text("Умная, платная", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showModelPicker = false }) { Text("Закрыть") } }
        )
    }

    // Reset confirm
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Сбросить все данные?") },
            text = { Text("Это удалит весь дневник питания, записи веса, избранное и настройки. Действие необратимо.") },
            confirmButton = {
                Button(
                    onClick = { vm.resetAll(); showResetConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Сбросить") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = tint)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsSwitchRow(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = {
            IconButton(onClick = onDone) {
                Icon(Icons.Default.Check, contentDescription = "Сохранить", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

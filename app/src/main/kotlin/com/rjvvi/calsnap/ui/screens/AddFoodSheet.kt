package com.rjvvi.calsnap.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rjvvi.calsnap.data.api.FoodAnalysisResult
import com.rjvvi.calsnap.data.db.FavouriteEntry
import com.rjvvi.calsnap.ui.theme.*
import com.rjvvi.calsnap.viewmodel.AddFoodState
import com.rjvvi.calsnap.viewmodel.AddFoodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSheet(
    viewModel: AddFoodViewModel,
    onDismiss: () -> Unit,
    onAddToJournal: (FoodAnalysisResult, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var mealType by remember { mutableStateOf("snack") }

    ModalBottomSheet(
        onDismissRequest = { viewModel.reset(); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Добавить еду", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.reset(); onDismiss() }) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            val tabLabels = listOf("📷 Фото", "✍️ Текст", "📦 Штрихкод", "⭐ Избранное")
            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, edgePadding = 12.dp) {
                tabLabels.forEachIndexed { i, label ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i; viewModel.reset() },
                        text = { Text(label, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Meal selector
            val mealOptions = listOf("breakfast" to "🌅 Завтрак", "lunch" to "☀️ Обед", "snack" to "🍎 Перекус", "dinner" to "🌙 Ужин")
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mealOptions) { (key, label) ->
                    FilterChip(selected = mealType == key, onClick = { mealType = key }, label = { Text(label, fontSize = 13.sp) })
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> PhotoTabContent(state, viewModel, onAdd = { onAddToJournal(it, mealType) }, onFav = { viewModel.saveToFavourites(it) })
                    1 -> TextTabContent(state, viewModel, onAdd = { onAddToJournal(it, mealType) }, onFav = { viewModel.saveToFavourites(it) })
                    2 -> BarcodeTabContent(state, viewModel, onAdd = { onAddToJournal(it, mealType) }, onFav = { viewModel.saveToFavourites(it) })
                    3 -> FavouritesTabContent(favourites, onAdd = { fav ->
                        onAddToJournal(FoodAnalysisResult(fav.name, fav.portion, fav.calories, fav.protein, fav.carbs, fav.fat), mealType)
                    }, onDelete = { viewModel.deleteFavourite(it) })
                }
            }
        }
    }
}

@Composable
private fun PhotoTabContent(state: AddFoodState, vm: AddFoodViewModel, onAdd: (FoodAnalysisResult) -> Unit, onFav: (FoodAnalysisResult) -> Unit) {
    val context = LocalContext.current
    var hint by remember { mutableStateOf("") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { bmp = uriToBitmap(context, it) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b: Bitmap? ->
        b?.let { bmp = it }
    }

    Column(Modifier.padding(16.dp)) {
        if (bmp == null) {
            Card(modifier = Modifier.fillMaxWidth().height(160.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("📷", fontSize = 36.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { cameraLauncher.launch(null) }) { Text("Камера") }
                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) { Text("Галерея") }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp))) {
                Image(bitmap = bmp!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                TextButton(
                    onClick = { bmp = null; vm.reset() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    colors = ButtonDefaults.textButtonColors(containerColor = Color.Black.copy(0.5f))
                ) { Text("Изменить", color = Color.White, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = hint, onValueChange = { hint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Подсказка") }, placeholder = { Text("Например: борщ, большая тарелка") }, shape = RoundedCornerShape(14.dp), maxLines = 2)
        Spacer(Modifier.height(10.dp))
        AnalysisSection(state, canAnalyze = bmp != null, btnLabel = "🔍 Анализировать", onAnalyze = { bmp?.let { vm.analyzePhoto(it, hint) } }, onAdd = onAdd, onFav = onFav, onReset = { vm.reset(); bmp = null })
    }
}

@Composable
private fun TextTabContent(state: AddFoodState, vm: AddFoodViewModel, onAdd: (FoodAnalysisResult) -> Unit, onFav: (FoodAnalysisResult) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Название блюда") }, placeholder = { Text("Гречка с курицей, 200г") }, shape = RoundedCornerShape(14.dp), maxLines = 3)
        Spacer(Modifier.height(10.dp))
        AnalysisSection(state, canAnalyze = text.isNotBlank(), btnLabel = "🔍 Найти калории", onAnalyze = { vm.analyzeText(text) }, onAdd = onAdd, onFav = onFav, onReset = { vm.reset() })
    }
}

@Composable
private fun BarcodeTabContent(state: AddFoodState, vm: AddFoodViewModel, onAdd: (FoodAnalysisResult) -> Unit, onFav: (FoodAnalysisResult) -> Unit) {
    var barcode by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = barcode, onValueChange = { barcode = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Штрихкод") }, placeholder = { Text("4607001895028") }, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        Spacer(Modifier.height(10.dp))
        AnalysisSection(state, canAnalyze = barcode.isNotBlank(), btnLabel = "🔍 Найти продукт", onAnalyze = { vm.analyzeBarcode(barcode) }, onAdd = onAdd, onFav = onFav, onReset = { vm.reset() })
    }
}

@Composable
private fun AnalysisSection(state: AddFoodState, canAnalyze: Boolean, btnLabel: String, onAnalyze: () -> Unit, onAdd: (FoodAnalysisResult) -> Unit, onFav: (FoodAnalysisResult) -> Unit, onReset: () -> Unit) {
    when (state) {
        is AddFoodState.Idle -> AnalyzeButton(btnLabel, canAnalyze, onAnalyze)
        is AddFoodState.Error -> { Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp)); AnalyzeButton(btnLabel, canAnalyze, onAnalyze) }
        is AddFoodState.Loading -> Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Gemini анализирует…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        is AddFoodState.Success -> FoodResultCard(state.result, onAdd = { onAdd(state.result) }, onFav = { onFav(state.result) }, onReset = onReset)
    }
}

@Composable
private fun AnalyzeButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = enabled, shape = RoundedCornerShape(14.dp)) { Text(label) }
}

@Composable
private fun FavouritesTabContent(favs: List<FavouriteEntry>, onAdd: (FavouriteEntry) -> Unit, onDelete: (Long) -> Unit) {
    if (favs.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⭐", fontSize = 40.sp); Spacer(Modifier.height(12.dp))
            Text("Нет избранного", style = MaterialTheme.typography.titleMedium)
            Text("Добавляй часто используемые блюда", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    } else {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            favs.forEach { fav ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fav.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("${fav.calories} ккал · ${fav.portion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDelete(fav.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                        Button(onClick = { onAdd(fav) }, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(36.dp)) { Text("+ Добавить", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodResultCard(result: FoodAnalysisResult, onAdd: () -> Unit, onFav: () -> Unit, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(result.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(result.portion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${result.calories}", fontSize = 36.sp, fontWeight = FontWeight.Black)
                    Text("ккал", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroBox2("Белки", result.protein, ProteinColor, Modifier.weight(1f))
                MacroBox2("Углев.", result.carbs, CarbsColor, Modifier.weight(1f))
                MacroBox2("Жиры", result.fat, FatColor, Modifier.weight(1f))
            }
            if (result.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(result.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReset, modifier = Modifier.height(46.dp), shape = RoundedCornerShape(12.dp)) { Text("✕") }
                OutlinedButton(onClick = onFav, modifier = Modifier.height(46.dp), shape = RoundedCornerShape(12.dp)) { Text("⭐") }
                Button(onClick = onAdd, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) { Text("+ Добавить") }
            }
        }
    }
}

@Composable
private fun MacroBox2(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${value.toInt()}г", fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) { null }
}

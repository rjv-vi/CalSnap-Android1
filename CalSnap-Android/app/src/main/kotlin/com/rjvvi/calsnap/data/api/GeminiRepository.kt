package com.rjvvi.calsnap.data.api

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class FoodAnalysisResult(
    val name: String,
    val portion: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val description: String = ""
)

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String
)

class GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val BASE = "https://generativelanguage.googleapis.com/v1beta/models"

    // ── PHOTO ANALYSIS ──
    suspend fun analyzePhoto(bitmap: Bitmap, apiKey: String, model: String, hint: String = ""): Result<FoodAnalysisResult> =
        withContext(Dispatchers.IO) {
            try {
                val b64 = bitmapToBase64(bitmap)
                val hintText = if (hint.isNotBlank()) "\n\nПодсказка от пользователя: $hint" else ""

                val prompt = """Ты - эксперт по питанию. Проанализируй еду на фото и верни ТОЛЬКО JSON без markdown.
                    
Формат ответа:
{"name":"название блюда","portion":"размер порции","calories":число,"protein":число,"carbs":число,"fat":число,"description":"краткое описание"}

Поля protein, carbs, fat - граммы. calories - ккал. Числа без единиц.
Если на фото нет еды - верни: {"error":"На фото нет еды"}$hintText"""

                val body = JsonObject().apply {
                    add("contents", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("role", "user")
                            add("parts", JsonArray().apply {
                                add(JsonObject().apply {
                                    add("inlineData", JsonObject().apply {
                                        addProperty("mimeType", "image/jpeg")
                                        addProperty("data", b64)
                                    })
                                })
                                add(JsonObject().apply { addProperty("text", prompt) })
                            })
                        })
                    })
                    add("generationConfig", JsonObject().apply {
                        addProperty("temperature", 0.1)
                        addProperty("maxOutputTokens", 256)
                    })
                }

                val resp = post("$BASE/$model:generateContent?key=$apiKey", body.toString())
                val text = extractText(resp)
                val json = gson.fromJson(cleanJson(text), JsonObject::class.java)
                if (json.has("error")) return@withContext Result.failure(Exception(json["error"].asString))

                Result.success(
                    FoodAnalysisResult(
                        name = json["name"]?.asString ?: "Неизвестно",
                        portion = json["portion"]?.asString ?: "1 порция",
                        calories = json["calories"]?.asInt ?: 0,
                        protein = json["protein"]?.asFloat ?: 0f,
                        carbs = json["carbs"]?.asFloat ?: 0f,
                        fat = json["fat"]?.asFloat ?: 0f,
                        description = json["description"]?.asString ?: ""
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── TEXT ANALYSIS ──
    suspend fun analyzeText(text: String, apiKey: String, model: String): Result<FoodAnalysisResult> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """Ты - эксперт по питанию. Пользователь написал: "$text"
                
Определи еду и верни ТОЛЬКО JSON без markdown:
{"name":"название","portion":"размер порции","calories":число,"protein":число,"carbs":число,"fat":число}

Числа - без единиц. calories - ккал. protein/carbs/fat - граммы."""

                val body = buildTextBody(prompt, model)
                val resp = post("$BASE/$model:generateContent?key=$apiKey", body)
                val responseText = extractText(resp)
                val json = gson.fromJson(cleanJson(responseText), JsonObject::class.java)

                Result.success(
                    FoodAnalysisResult(
                        name = json["name"]?.asString ?: text,
                        portion = json["portion"]?.asString ?: "1 порция",
                        calories = json["calories"]?.asInt ?: 0,
                        protein = json["protein"]?.asFloat ?: 0f,
                        carbs = json["carbs"]?.asFloat ?: 0f,
                        fat = json["fat"]?.asFloat ?: 0f
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── BARCODE LOOKUP ──
    suspend fun lookupBarcode(barcode: String, apiKey: String, model: String): Result<FoodAnalysisResult> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """Штрихкод продукта: $barcode
                
Найди информацию об этом продукте и верни ТОЛЬКО JSON:
{"name":"название продукта","portion":"100 г","calories":число,"protein":число,"carbs":число,"fat":число,"description":"бренд и описание"}

Если не знаешь такой штрихкод: {"error":"Продукт не найден в базе"}"""

                val body = buildTextBody(prompt, model)
                val resp = post("$BASE/$model:generateContent?key=$apiKey", body)
                val text = extractText(resp)
                val json = gson.fromJson(cleanJson(text), JsonObject::class.java)
                if (json.has("error")) return@withContext Result.failure(Exception(json["error"].asString))

                Result.success(
                    FoodAnalysisResult(
                        name = json["name"]?.asString ?: "Продукт $barcode",
                        portion = json["portion"]?.asString ?: "100 г",
                        calories = json["calories"]?.asInt ?: 0,
                        protein = json["protein"]?.asFloat ?: 0f,
                        carbs = json["carbs"]?.asFloat ?: 0f,
                        fat = json["fat"]?.asFloat ?: 0f,
                        description = json["description"]?.asString ?: ""
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── AI CHAT ──
    suspend fun chat(
        history: List<ChatMessage>,
        userMessage: String,
        apiKey: String,
        model: String,
        userContext: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """Ты - AI-ассистент приложения CalSnap для трекинга питания.
Ты помогаешь с вопросами о питании, калориях, диетах, здоровье.
Отвечай кратко и по делу. Используй эмодзи где уместно.
$userContext"""

            val contents = JsonArray()

            // System context as first user message
            contents.add(JsonObject().apply {
                addProperty("role", "user")
                add("parts", JsonArray().apply {
                    add(JsonObject().apply { addProperty("text", systemPrompt) })
                })
            })
            contents.add(JsonObject().apply {
                addProperty("role", "model")
                add("parts", JsonArray().apply {
                    add(JsonObject().apply { addProperty("text", "Понял! Готов помочь с вопросами о питании 🍎") })
                })
            })

            // Chat history
            for (msg in history.takeLast(10)) {
                contents.add(JsonObject().apply {
                    addProperty("role", msg.role)
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", msg.text) })
                    })
                })
            }

            // New user message
            contents.add(JsonObject().apply {
                addProperty("role", "user")
                add("parts", JsonArray().apply {
                    add(JsonObject().apply { addProperty("text", userMessage) })
                })
            })

            val body = JsonObject().apply {
                add("contents", contents)
                add("generationConfig", JsonObject().apply {
                    addProperty("temperature", 0.7)
                    addProperty("maxOutputTokens", 1024)
                })
            }

            val resp = post("$BASE/$model:generateContent?key=$apiKey", body.toString())
            Result.success(extractText(resp))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── FETCH AVAILABLE MODELS ──
    suspend fun listModels(apiKey: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                    .get().build()
                val body = client.newCall(req).execute().body?.string() ?: ""
                val root = gson.fromJson(body, JsonObject::class.java)
                val models = root.getAsJsonArray("models")
                    ?.map { it.asJsonObject["name"].asString.removePrefix("models/") }
                    ?.filter { it.startsWith("gemini") }
                    ?: emptyList()
                Result.success(models)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── HELPERS ──
    private fun post(url: String, body: String): String {
        val req = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val resp = client.newCall(req).execute()
        val text = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw Exception("API Error ${resp.code}: $text")
        return text
    }

    private fun extractText(responseBody: String): String {
        val root = gson.fromJson(responseBody, JsonObject::class.java)
        return root.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString ?: ""
    }

    private fun cleanJson(text: String): String {
        return text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun buildTextBody(prompt: String, model: String): String {
        val body = JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", prompt) })
                    })
                })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.1)
                addProperty("maxOutputTokens", 256)
            })
        }
        return body.toString()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val ratio = minOf(1024f / bitmap.width, 1024f / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}

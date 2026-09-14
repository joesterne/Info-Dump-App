package com.example.data

import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key not configured. Please add it in AI Studio Secrets."
        }

        // Read file bytes and encode to base64
        val bytes = audioFile.readBytes()
        val base64Audio = Base64.encodeToString(bytes, Base64.NO_WRAP)

        // Construct JSON payload
        val root = JSONObject()
        
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        
        // Text Prompt Part
        val textPart = JSONObject()
        textPart.put("text", "Please provide a highly accurate transcription of this audio note. It's for an infodump session prep. Return just the transcribed text.")
        partsArray.put(textPart)
        
        // Audio Part
        val audioPart = JSONObject()
        val inlineDataObj = JSONObject()
        inlineDataObj.put("mimeType", "audio/mp4")
        inlineDataObj.put("data", base64Audio)
        audioPart.put("inlineData", inlineDataObj)
        partsArray.put(audioPart)
        
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        val requestBody = root.toString().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(requestBody)
            .build()
            
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No transcription generated.")
                    }
                }
                "Error: Could not parse response."
            } else {
                "Error: API call failed with code ${response.code}. $responseBody"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

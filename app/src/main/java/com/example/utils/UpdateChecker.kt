package com.example.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val updateUrl: String,
    val releaseNotes: String
)

object UpdateChecker {
    private val client = OkHttpClient()

    suspend fun checkUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/Moc196/appalone/main/update.json")
                .header("Cache-Control", "no-cache")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                
                // Manual lightweight parsing to avoid Moshi codegen/reflection dependency errors
                val versionCode = body.substringAfter("\"versionCode\":")
                    .substringBefore(",")
                    .substringBefore("}")
                    .trim()
                    .toIntOrNull() ?: 0
                
                val versionName = body.substringAfter("\"versionName\":")
                    .substringBefore(",")
                    .substringBefore("}")
                    .trim()
                    .replace("\"", "")
                
                val updateUrl = body.substringAfter("\"updateUrl\":")
                    .substringBefore(",")
                    .substringBefore("}")
                    .trim()
                    .replace("\"", "")
                    .replace("\\/", "/") // Unescape slashes if any
                
                val releaseNotes = body.substringAfter("\"releaseNotes\":")
                    .substringBefore("}")
                    .trim()
                    .replace("\"", "")
                
                return@withContext UpdateInfo(versionCode, versionName, updateUrl, releaseNotes)
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Failed to check for update", e)
            null
        }
    }
}

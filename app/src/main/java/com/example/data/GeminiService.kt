package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun generatePoeticReflection(momentsText: String): String {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "placeholder") {
            return "Unable to evoke Gemini Reflection. Add your Gemini API Key in the AI Studio Secrets panel."
        }

        val systemPrompt = """
            You are "Aura", a comforting, poetic, and highly reflective AI nostalgic memory companion.
            The user has logged some captured memories. 
            Write a single, visually comforting, highly polished brief paragraph structure highlighting the emotional atmosphere of their logged moments.
            Keep it strictly private, peaceful, personal, and cinematic. Do not use generic corporate language. 
            Do not use emojis. End with a soft elegant query or gentle aesthetic wisdom (1-2 sentences).
            Maximum length: 80 words.
        """.trimIndent()

        val fullPrompt = "$systemPrompt\n\nHere are the logged moments:\n$momentsText"

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = fullPrompt)
                    )
                )
            )
        )

        return try {
            val response = api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "The evening light shifts. Keep storing your moments here in quiet solitude."
        } catch (e: Exception) {
            "A serene hush falls over are memories. Configure your Gemini key, or capture more moments to mirror your aura."
        }
    }
}

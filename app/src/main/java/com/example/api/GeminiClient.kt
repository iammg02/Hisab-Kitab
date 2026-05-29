package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiAdvisor {
    /**
     * Calls Gemini API to get personalized suggestions for the user's spending data.
      */
    suspend fun getSpendingSuggestions(
        monthlyBudget: Double,
        currencySymbol: String,
        expensesSummary: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getRuleBasedHeuristics(monthlyBudget, currencySymbol, expensesSummary)
        }

        val prompt = """
            Here is my financial details and active monthly transactions. 
            My monthly budget limit is $currencySymbol${"%.2f".format(monthlyBudget)}.
            
            Current month expenses summary:
            $expensesSummary
            
            Please provide a friendly, precise, and practical expense insight report in 3 short, readable bullet points:
            1. An analysis of whether I am on track or overspending relative to my $currencySymbol$monthlyBudget budget.
            2. Identify my highest spending categories and give one creative, highly actionable tip to reduce cost in those specific fields.
            3. A supportive, custom recommendation to maximize savings this week.
            Keep formatting clean. Keep it polite, concise, and focused strictly on the provided categories. No emojis.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are a professional luxury minimalist financial advisor. Your advice is direct, incredibly helpful, high-signal, and motivating.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No insights available right now. Keep tracking your daily transactions regularly to help me build your tailored reports!"
        } catch (e: Exception) {
            // Log exception, and run fallback heuristic
            getRuleBasedHeuristics(monthlyBudget, currencySymbol, expensesSummary, isError = true)
        }
    }

    private fun getRuleBasedHeuristics(
        monthlyBudget: Double,
        currencySymbol: String,
        expensesSummary: String,
        isError: Boolean = false
    ): String {
        // Simple elegant offline backup heuristic of advisor
        val baseMessage = if (isError) {
            "NOTE: Network insight stream is currently busy. Local advisor report active:\n\n"
        } else {
            "NOTE: Configure Gemini API Key in the AI Studio secrets panel to unlock real-time Gemini AI insights! Local advisor report active:\n\n"
        }

        // Try parsing summary lines
        var totalSpent = 0.0
        val items = expensesSummary.split("\n")
        for (item in items) {
            if (item.contains("Total Spent:")) {
                val cleaned = item.substringAfter("Total Spent:").trim().replace(currencySymbol, "").replace(",", "").trim()
                totalSpent = cleaned.toDoubleOrNull() ?: 0.0
            }
        }

        val status = if (totalSpent > monthlyBudget) {
            "Status alert: Overspent. Current expenses ($currencySymbol${"%.2f".format(totalSpent)}) exceeded your monthly limit of $currencySymbol${"%.2f".format(monthlyBudget)}."
        } else if (totalSpent > monthlyBudget * 0.8) {
            "Status warning: Caution. Reached ${"%.0f".format((totalSpent / monthlyBudget) * 100)}% of your $currencySymbol${"%.2f".format(monthlyBudget)} budget. Reduce secondary purchases today."
        } else {
            "Status okay: Solid trajectory. You are comfortably below your limit. Current spending is at ${"%.0f".format((totalSpent / monthlyBudget) * 100)}%."
        }

        return """
            $baseMessage
            1. $status
            2. Primary category cost reduction tip: Check your 'Bills' and 'Food' sub-payments. Standard practices show meal prepping and unused audio/visual streaming adjustments can reduce weekly spend.
            3. Supportive tip: Set aside 5% of this week's entertainment budget into savings. Continual tracking ensures compounding interest!
        """.trimIndent()
    }
}

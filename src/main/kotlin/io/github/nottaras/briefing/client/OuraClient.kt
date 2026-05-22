package io.github.nottaras.briefing.client

import io.github.nottaras.briefing.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.LocalDate

class OuraClient(private val accessToken: String) {

    private val baseUrl = "https://api.ouraring.com/v2/usercollection"

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getSleep(date: LocalDate = LocalDate.now()): SleepData? =
        get("daily_sleep", date)

    suspend fun getReadiness(date: LocalDate = LocalDate.now()): ReadinessData? =
        get("daily_readiness", date)

    suspend fun getCardiovascular(date: LocalDate = LocalDate.now()): CardiovascularData? =
        get("daily_cardiovascular_age", date)

    private suspend inline fun <reified T> get(
        endpoint: String,
        date: LocalDate,
    ): T? = runCatching {
        val response: OuraResponse<T> = http.get("$baseUrl/$endpoint") {
            headers { append("Authorization", "Bearer $accessToken") }
            parameter("start_date", date.toString())
            parameter("end_date", date.toString())
        }.body()
        response.data.firstOrNull()
    }.getOrNull()

    fun close() = http.close()
}

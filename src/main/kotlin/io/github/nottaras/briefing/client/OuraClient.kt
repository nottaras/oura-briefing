package io.github.nottaras.briefing.client

import io.github.nottaras.briefing.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
        expectSuccess = false
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
    ): T? {
        val httpResponse = try {
            http.get("$baseUrl/$endpoint") {
                headers { append("Authorization", "Bearer $accessToken") }
                parameter("start_date", date.toString())
                parameter("end_date", date.toString())
            }
        } catch (e: Exception) {
            throw OuraApiException("Oura API $endpoint request failed: ${e.message}", e)
        }

        when (httpResponse.status) {
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                throw OuraApiException("Oura API unauthorized (${httpResponse.status}). Run `briefing auth` to re-authenticate.")
            else -> if (!httpResponse.status.isSuccess()) {
                throw OuraApiException(
                    "Oura API $endpoint failed (${httpResponse.status}): ${httpResponse.bodyAsText().take(500)}"
                )
            }
        }

        return try {
            httpResponse.body<OuraResponse<T>>().data.firstOrNull()
        } catch (e: Exception) {
            throw OuraApiException("Oura API $endpoint returned unexpected data: ${e.message}", e)
        }
    }

    fun close() = http.close()
}

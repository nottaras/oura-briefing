package io.github.nottaras.briefing.client

import io.github.nottaras.briefing.model.HealthContext
import io.github.nottaras.briefing.service.Prompts
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ClaudeClient(
    private val apiKey: String,
    private val model: String,
) {
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        expectSuccess = false
    }

    suspend fun generateBriefing(context: HealthContext): String {
        val httpResponse = http.post("https://api.anthropic.com/v1/messages") {
            headers {
                append("x-api-key", apiKey)
                append("anthropic-version", "2023-06-01")
            }
            contentType(ContentType.Application.Json)
            setBody(ClaudeRequest(
                model = model,
                maxTokens = 400,
                system = Prompts.system,
                messages = listOf(Message("user", Prompts.userMessage(context))),
            ))
        }
        if (!httpResponse.status.isSuccess()) {
            error("Claude API failed (${httpResponse.status}): ${httpResponse.bodyAsText()}")
        }
        return httpResponse.body<ClaudeResponse>().content.first().text
    }

    fun close() = http.close()
}

// ---------- Anthropic API DTOs ----------

@Serializable
private data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<Message>,
)

@Serializable
private data class Message(val role: String, val content: String)

@Serializable
private data class ClaudeResponse(val content: List<ContentBlock>)

@Serializable
private data class ContentBlock(val type: String, val text: String = "")

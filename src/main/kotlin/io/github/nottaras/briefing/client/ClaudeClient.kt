package io.github.nottaras.briefing.client

import io.github.nottaras.briefing.model.HealthContext
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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
    }

    // TODO: implement full Claude API call (issue #4)
    // TODO: add system prompt from Prompts.kt (issue #4)
    suspend fun generateBriefing(context: HealthContext): String {
        return "TODO: briefing for ${context.date}"
    }

    fun close() = http.close()
}

// ---------- Anthropic API DTOs ----------

@Serializable
private data class ClaudeRequest(
    val model: String,
    val max_tokens: Int,
    val system: String,
    val messages: List<Message>,
)

@Serializable
private data class Message(val role: String, val content: String)

@Serializable
private data class ClaudeResponse(val content: List<ContentBlock>)

@Serializable
private data class ContentBlock(val type: String, val text: String = "")

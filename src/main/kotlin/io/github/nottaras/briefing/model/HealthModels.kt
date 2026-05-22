package io.github.nottaras.briefing.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Oura API response wrappers ----------

@Serializable
data class OuraResponse<T>(
    val data: List<T>
)

// ---------- Sleep ----------

@Serializable
data class SleepData(
    val id: String,
    val day: String,
    val score: Int? = null,
    @SerialName("total_sleep_duration") val totalSleepDuration: Int? = null,
    @SerialName("deep_sleep_duration") val deepSleepDuration: Int? = null,
    @SerialName("rem_sleep_duration") val remSleepDuration: Int? = null,
    @SerialName("light_sleep_duration") val lightSleepDuration: Int? = null,
    val efficiency: Int? = null,
    val latency: Int? = null,
    val restfulness: Double? = null,
)

// ---------- Readiness ----------

@Serializable
data class ReadinessData(
    val id: String,
    val day: String,
    val score: Int? = null,
    @SerialName("temperature_deviation") val temperatureDeviation: Double? = null,
    @SerialName("hrv_balance_score") val hrvBalanceScore: Int? = null,
    @SerialName("recovery_index_score") val recoveryIndexScore: Int? = null,
    @SerialName("resting_heart_rate") val restingHeartRate: Int? = null,
    val contributors: ReadinessContributors? = null,
)

@Serializable
data class ReadinessContributors(
    @SerialName("hrv_balance") val hrvBalance: Int? = null,
    @SerialName("resting_heart_rate") val restingHeartRate: Int? = null,
    @SerialName("body_temperature") val bodyTemperature: Int? = null,
    @SerialName("recovery_index") val recoveryIndex: Int? = null,
    @SerialName("sleep_balance") val sleepBalance: Int? = null,
    @SerialName("activity_balance") val activityBalance: Int? = null,
)

// ---------- Cardiovascular / HRV ----------

@Serializable
data class CardiovascularData(
    val id: String,
    val day: String,
    @SerialName("vascular_age") val vascularAge: Int? = null,
)

// ---------- Aggregated context passed to Claude ----------

data class HealthContext(
    val date: String,
    val sleep: SleepData?,
    val readiness: ReadinessData?,
    val cardiovascular: CardiovascularData?,
)

// ---------- Trend averages from history cache ----------

data class TrendContext(
    val days: Int,
    val avgSleepScore: Double?,
    val avgReadinessScore: Double?,
)

// ---------- Result returned by BriefingService ----------

data class BriefingResult(
    val text: String,
    val context: HealthContext,
)

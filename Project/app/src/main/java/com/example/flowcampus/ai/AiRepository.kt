package com.example.flowcampus.ai

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente do servidor de IA (pasta ai-service).
 *
 * Qualquer tela pode usar:
 *
 *   val result = AiRepository().fetchForecast()
 *   result.onSuccess { forecast -> ... }
 *   result.onFailure { error -> ... }   // servidor desligado, sem rede etc.
 *
 * Não precisa de biblioteca extra: usa HttpURLConnection e org.json (do Android).
 */
class AiRepository {

    suspend fun fetchForecast(
        horizonHours: Int = 48
    ): Result<AiForecast> = withContext(Dispatchers.IO) {

        try {

            val token = currentToken()

            val url = URL(
                "${AiServiceConfig.BASE_URL}/api/forecast?horizon_hours=$horizonHours"
            )

            val connection = url.openConnection() as HttpURLConnection

            try {

                connection.requestMethod = "GET"
                connection.connectTimeout = AiServiceConfig.CONNECT_TIMEOUT_MS
                connection.readTimeout = AiServiceConfig.READ_TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/json")

                if (token != null) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }

                val code = connection.responseCode

                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val body = stream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                if (code !in 200..299) {
                    throw IllegalStateException("Servidor de IA respondeu HTTP $code: $body")
                }

                Result.success(parseForecast(JSONObject(body)))

            } finally {
                connection.disconnect()
            }

        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Token do usuário logado (o servidor só exige se REQUIRE_AUTH=true). */
    private suspend fun currentToken(): String? {

        val user = FirebaseAuth.getInstance().currentUser ?: return null

        return try {
            user.getIdToken(false).await().token
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
    }
}

private fun JSONArray?.objects(): List<JSONObject> {

    if (this == null) return emptyList()

    return (0 until length()).map { getJSONObject(it) }
}

private fun parseForecast(json: JSONObject): AiForecast {

    return AiForecast(
        bestModel = json.optString("bestModel"),
        bestModelLabel = json.optString("bestModelLabel"),
        baselineMae = json.optDouble("baselineMae"),
        trainingRows = json.optInt("trainingRows"),
        validationRows = json.optInt("validationRows"),
        trainingSeconds = json.optDouble("trainingSeconds"),

        models = json.optJSONArray("models").objects().map { model ->
            AiModelScore(
                key = model.optString("key"),
                label = model.optString("label"),
                mae = model.optDouble("mae"),
                trainSeconds = model.optDouble("trainSeconds"),
                isBest = model.optBoolean("isBest")
            )
        },

        forecasts = json.optJSONArray("forecasts").objects().map { environment ->
            AiEnvironmentForecast(
                environmentId = environment.optString("environmentId"),
                environmentName = environment.optString("environmentName"),
                capacity = environment.optInt("capacity"),

                points = environment.optJSONArray("points").objects().map { point ->
                    AiForecastPoint(
                        timestamp = point.optString("timestamp"),
                        hour = point.optInt("hour"),
                        predictedOccupancyPercent = point.optInt("predictedOccupancyPercent"),
                        predictedPeople = point.optInt("predictedPeople")
                    )
                },

                peak = environment.optJSONObject("peak")?.toMoment(),
                bestTime = environment.optJSONObject("bestTime")?.toMoment()
            )
        },

        insights = json.optJSONArray("insights").objects().map { insight ->
            AiInsight(
                text = insight.optString("text"),
                highlight = insight.optString("highlight"),
                icon = insight.optString("icon"),
                tone = insight.optString("tone")
            )
        }
    )
}

private fun JSONObject.toMoment(): AiMoment {

    return AiMoment(
        timestamp = optString("timestamp"),
        hour = optInt("hour"),
        occupancyPercent = optInt("occupancyPercent"),
        label = optString("label")
    )
}

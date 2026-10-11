package com.example.flowcampus.ai

/** MAE de um dos 3 modelos treinados na chamada. */
data class AiModelScore(
    val key: String,
    val label: String,
    val mae: Double,
    val trainSeconds: Double,
    val isBest: Boolean
)

/** Ocupação prevista (%) de um ambiente em uma hora. */
data class AiForecastPoint(
    val timestamp: String,
    val hour: Int,
    val predictedOccupancyPercent: Int,
    val predictedPeople: Int
)

/** Um momento de destaque (pico ou melhor horário). label = "hoje", "amanhã"... */
data class AiMoment(
    val timestamp: String,
    val hour: Int,
    val occupancyPercent: Int,
    val label: String
)

data class AiEnvironmentForecast(
    val environmentId: String,
    val environmentName: String,
    val capacity: Int,
    val points: List<AiForecastPoint>,
    val peak: AiMoment?,
    val bestTime: AiMoment?
)

/** Frase pronta para a seção "Insights da IA". */
data class AiInsight(
    val text: String,
    val highlight: String,
    val icon: String,
    val tone: String
)

/** Resposta completa de GET /api/forecast. */
data class AiForecast(
    val bestModel: String,
    val bestModelLabel: String,
    val baselineMae: Double,
    val trainingRows: Int,
    val validationRows: Int,
    val trainingSeconds: Double,
    val models: List<AiModelScore>,
    val forecasts: List<AiEnvironmentForecast>,
    val insights: List<AiInsight>
) {

    /** Atalho para as outras telas: previsão de um ambiente pelo nome. */
    fun forEnvironment(name: String): AiEnvironmentForecast? {

        return forecasts.firstOrNull {
            it.environmentName.equals(name, ignoreCase = true)
        }
    }
}

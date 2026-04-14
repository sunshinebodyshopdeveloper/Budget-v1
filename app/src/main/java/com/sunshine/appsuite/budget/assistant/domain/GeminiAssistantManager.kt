package com.sunshine.appsuite.budget.assistant.domain

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import com.sunshine.appsuite.BuildConfig
import com.sunshine.appsuite.budget.assistant.data.ServiceOrderResponse

class GeminiAssistantManager {
    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    // Forzamos una configuración estándar para evitar el error de versión de API
    private val config = generationConfig {
        temperature = 0.7f
        topK = 40
        topP = 0.95f
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey,
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        ),
        systemInstruction = content {
            text("Eres el asistente de Sunshine AppSuite. Responde basándote en los datos técnicos.")
        }
    )

    private var chatSession = generativeModel.startChat()

    fun provideDetailedContext(data: ServiceOrderResponse) {
        val workOrdersStr = data.workOrders?.joinToString("\n") {
            "- WO ${it.code}: ${it.status} (${it.repairStage ?: "N/A"})"
        } ?: "Sin órdenes de trabajo."

        val promptContexto = """
            CONTEXTO DE TRABAJO:
            Orden: ${data.code}
            Cliente: ${data.clientName}
            Vehículo: ${data.vehicle?.carBrand} ${data.vehicle?.carName}
            Placas: ${data.vehicle?.plates}
            Estatus: ${data.status}
            Seguro: ${data.insuranceClaim?.policyNumber ?: "N/A"}
            Tareas: $workOrdersStr
        """.trimIndent()

        // Reinicio limpio de sesión
        chatSession = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text(promptContexto) },
                content(role = "model") { text("Entendido. Datos de la orden ${data.code} cargados. ¿Qué quieres saber?") }
            )
        )
    }

    suspend fun sendMessage(userPrompt: String): String? {
        return try {
            val response = chatSession.sendMessage(userPrompt)
            response.text
        } catch (e: Exception) {
            Log.e("GeminiError", "Detalle: ${e.message}")
            "Error en la IA: ${e.localizedMessage}. Por favor, intenta de nuevo."
        }
    }
}
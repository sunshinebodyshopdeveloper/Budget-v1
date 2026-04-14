package com.sunshine.appsuite.budget.assistant.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunshine.appsuite.budget.assistant.data.AssistantApiService
import com.sunshine.appsuite.budget.assistant.domain.GeminiAssistantManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

// Definimos los estados para guiar la conversación
enum class AssistantState { NORMAL, WAITING_FOR_PLATE, WAITING_FOR_CLIENT }

class AssistantViewModel(
    private val apiService: AssistantApiService,
    private val geminiManager: GeminiAssistantManager
) : ViewModel() {

    private val _aiResponse = MutableSharedFlow<String>()
    val aiResponse = _aiResponse.asSharedFlow()

    private var currentState = AssistantState.NORMAL

    fun onChipClicked(suggestionText: String) {
        viewModelScope.launch {
            when {
                suggestionText.contains("placa", ignoreCase = true) -> {
                    currentState = AssistantState.WAITING_FOR_PLATE
                    _aiResponse.emit("Claro, dime el número de placa y la busco de inmediato.")
                }
                suggestionText.contains("cliente", ignoreCase = true) -> {
                    currentState = AssistantState.WAITING_FOR_CLIENT
                    _aiResponse.emit("Perfecto, ¿cuál es el nombre del cliente para buscar su orden?")
                }
                else -> {
                    sendMessage(suggestionText)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val query = text.trim()

            try {
                // Si estábamos esperando un dato específico, lo procesamos directo
                when (currentState) {
                    AssistantState.WAITING_FOR_PLATE -> {
                        currentState = AssistantState.NORMAL
                        processSearchQuery(query) // Buscamos la placa directamente
                        return@launch
                    }
                    AssistantState.WAITING_FOR_CLIENT -> {
                        currentState = AssistantState.NORMAL
                        processSearchQuery(query) // Buscamos el nombre directamente
                        return@launch
                    }
                    else -> {
                        // Lógica estándar: primero intentamos detectar una OT manual
                        val otRegex = Regex("OT-[A-Z0-9-]+")
                        val directCode = otRegex.find(query.uppercase())?.value

                        if (directCode != null) {
                            fetchAndProvideDetail(directCode)
                        } else {
                            // Si no es OT, dejamos que Gemini responda o busque
                            val response = geminiManager.sendMessage(query)
                            _aiResponse.emit(response ?: "Lo siento, no pude procesar tu solicitud.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AssistantVM", "Error: ${e.message}")
                _aiResponse.emit("Hubo un problema. ¿Podrías intentar de nuevo?")
            }
        }
    }

    private suspend fun processSearchQuery(query: String) {
        // Ahora searchResponse contiene la propiedad "data" que es List<ServiceOrderResponse>
        val searchResponse = apiService.searchServiceOrders(query.uppercase())

        // Tomamos el primer resultado de la lista "data"
        val foundOrder = searchResponse.data.firstOrNull()

        if (foundOrder?.code != null) {
            // Si el objeto ya viene completo de la búsqueda, podrías saltarte
            // el fetchAndProvideDetail y usar foundOrder directamente
            fetchAndProvideDetail(foundOrder.code)
        } else {
            _aiResponse.emit("No encontré ninguna orden con '$query'. ¿Deseas intentar con otra placa?")
        }
    }

    private suspend fun fetchAndProvideDetail(code: String) {
        try {
            val envelope = apiService.getServiceOrderDetail(code)
            geminiManager.provideDetailedContext(envelope.data)
            val aiMsg = geminiManager.sendMessage("Confirma de forma amable que encontraste la orden $code y resume su estatus.")
            _aiResponse.emit(aiMsg ?: "Encontré la orden $code, ¿qué deseas saber?")
        } catch (e: Exception) {
            _aiResponse.emit("Encontré la referencia $code pero no pude cargar los detalles técnicos.")
        }
    }

    fun initAssistant(orderId: Int) {
        viewModelScope.launch {
            try {
                val envelope = apiService.getServiceOrderDetail(orderId.toString())
                geminiManager.provideDetailedContext(envelope.data)
            } catch (e: Exception) {
                Log.e("AssistantVM", "Error inicial: ${e.message}")
            }
        }
    }
}
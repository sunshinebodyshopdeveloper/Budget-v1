package com.sunshine.appsuite.budget.orders.data

import com.google.gson.JsonObject

/**
 * DTO liviano para el LISTADO de Órdenes de Servicio (OS).
 * Mantiene campos seguros/nullable para que nunca truene si el backend cambia.
 */
data class ServiceOrderListItemDto(
    val id: Int? = null,
    val code: String? = null,

    val clientName: String? = null,
    val status: String? = null,
    val createdAt: String? = null,

    // Vehículo (lo mínimo útil para listar)
    val vehicleBrand: String? = null,
    val vehicleName: String? = null,
    val vehicleYear: Int? = null,
    val vehicleType: String? = null,
    val vehicleColor: String? = null,
    val vehiclePlates: String? = null,

    // Seguro (opcional en listado)
    val policyNumber: String? = null,
    val sinisterNumber: String? = null,

    // Relación OS -> OT (si el backend lo trae en el listado)
    val activeOtCount: Int? = null,
    val totalOtCount: Int? = null
) {
    companion object {

        fun from(obj: JsonObject): ServiceOrderListItemDto {

            fun str(key: String): String? =
                obj.get(key)?.takeIf { !it.isJsonNull }?.asString

            fun int(key: String): Int? =
                obj.get(key)?.takeIf { !it.isJsonNull }?.asInt

            // Cliente: client_name o client.name
            val clientName =
                str("client_name")
                    ?: obj.getAsJsonObject("client")?.get("name")?.takeIf { !it.isJsonNull }?.asString

            // Vehículo: campos flat o vehicle.*
            val vehicleObj = obj.getAsJsonObject("vehicle")

            fun vStr(key: String): String? =
                vehicleObj?.get(key)?.takeIf { !it.isJsonNull }?.asString ?: str(key)

            fun vInt(key: String): Int? =
                vehicleObj?.get(key)?.takeIf { !it.isJsonNull }?.asInt ?: int(key)

            val vehicleBrand = vStr("car_brand") ?: vStr("brand")
            val vehicleName = vStr("car_name") ?: vStr("name") ?: vStr("model")
            val vehicleYear = vInt("car_year") ?: vInt("year")
            val vehicleType = vStr("type")
            val vehicleColor = vStr("color")
            val vehiclePlates = vStr("plates")

            // Seguro: insurance_claim.*
            val insuranceObj = obj.getAsJsonObject("insurance_claim") ?: obj.getAsJsonObject("insurance")
            val policyNumber = insuranceObj?.get("policy_number")?.takeIf { !it.isJsonNull }?.asString
                ?: str("policy_number")
            val sinisterNumber = insuranceObj?.get("sinister_number")?.takeIf { !it.isJsonNull }?.asString
                ?: str("sinister_number")

            // Conteos OT: intentamos varias claves comunes sin romper si no existen
            val activeOtCount =
                int("active_ot_count")
                    ?: int("active_ots_count")
                    ?: int("ot_active_count")
                    ?: int("active_work_orders")
                    ?: int("active_work_orders_count")

            val totalOtCount =
                int("ot_count")
                    ?: int("ots_count")
                    ?: int("work_orders_count")
                    ?: int("total_work_orders")

            return ServiceOrderListItemDto(
                id = int("id"),
                code = str("code") ?: str("folio"),

                clientName = clientName,
                status = str("status"),
                createdAt = str("created_at"),

                vehicleBrand = vehicleBrand,
                vehicleName = vehicleName,
                vehicleYear = vehicleYear,
                vehicleType = vehicleType,
                vehicleColor = vehicleColor,
                vehiclePlates = vehiclePlates,

                policyNumber = policyNumber,
                sinisterNumber = sinisterNumber,

                activeOtCount = activeOtCount,
                totalOtCount = totalOtCount
            )
        }
    }
}

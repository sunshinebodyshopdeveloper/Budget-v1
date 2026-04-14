package com.sunshine.appsuite.budget.settings.system.notifications.model

enum class NotificationTopic(val key: String) {
    TOWING_NEW_INTAKE("notif_towing_new_intake"),
    OT_NEW_ORDER("notif_ot_new_order"),
    ASSIGNMENT_UNIT("notif_assignment_unit"),
    TRACKING_LOCATION_CHANGE("notif_tracking_location_change"),
    APPOINTMENT_NEW("notif_appointment_new"),
    UNIT_STATUS("notif_unit_status"),
}

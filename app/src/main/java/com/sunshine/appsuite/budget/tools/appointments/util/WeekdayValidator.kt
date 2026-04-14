package com.sunshine.appsuite.budget.tools.appointments.util

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints
import java.util.Calendar
import java.util.TimeZone

/**
 * Permite solo Lunes–Viernes.
 * MaterialDatePicker usa millis en UTC (medianoche UTC), así que validamos en UTC para evitar “cambio de día”.
 */
class WeekdayValidator : CalendarConstraints.DateValidator {

    override fun isValid(date: Long): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = date
        }
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return day != Calendar.SATURDAY && day != Calendar.SUNDAY
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) = Unit

    companion object CREATOR : Parcelable.Creator<WeekdayValidator> {
        override fun createFromParcel(source: Parcel): WeekdayValidator = WeekdayValidator()
        override fun newArray(size: Int): Array<WeekdayValidator?> = arrayOfNulls(size)
    }
}

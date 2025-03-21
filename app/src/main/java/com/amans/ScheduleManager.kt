package com.amans

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScheduleManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("KidSchedule", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveSchedule(schedule: List<ScheduleItem>) {
        val json = gson.toJson(schedule)
        sharedPreferences.edit().putString("daily_schedule", json).apply()
    }

    fun loadSchedule(): List<ScheduleItem> {
        val json = sharedPreferences.getString("daily_schedule", null)
        return if (json != null) {
            val type = object : TypeToken<List<ScheduleItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun clearSchedule() {
        sharedPreferences.edit().remove("daily_schedule").apply()
    }
}

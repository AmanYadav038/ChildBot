package com.amans

data class ScheduleItem(
    val activityName: String,
    val duration : Int,
    var refuseCount: Int = 0,
    var status : String = "pending"
)
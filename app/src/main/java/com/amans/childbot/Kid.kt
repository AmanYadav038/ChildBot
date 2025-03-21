package com.amans.childbot

data class Kid(
    val name: String = "",
    val age: Int = 0,
    val classLevel: String = "",
    val schoolStartTime: String = "",
    val schoolEndTime: String = "",
    val homeworkDuration: Int = 0,
    val preferredStudyTime: String = "",
    val hobbies: List<String> = emptyList(),
    val learningInterests: List<String> = emptyList()
)

package com.amans

data class KidInfo(
    val name: String,
    val age: Int,
    val standard : String,
    val homeworkDuration: Int =0,
    val homeworksLinks: List<String> = listOf(),
    val schoolTimings: String,  // Example: "8:00 AM - 2:00 PM"
    val hobbies: List<String>,  // Example: ["Drawing", "Cycling"]
    val learningInterests: List<String> // Example: ["Math", "Science"]
) {
    constructor() : this("", 0, "", 0, emptyList(), "",emptyList(), emptyList())
}

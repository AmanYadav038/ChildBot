package com.amans.childbot.chatbot

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amans.KidInfo
import com.amans.ScheduleItem
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _messageList = MutableLiveData<MutableList<MessageModel>>(mutableListOf())
    val messageList: LiveData<MutableList<MessageModel>> get() = _messageList

    private val _kidInfo = MutableLiveData<KidInfo?>()
    val kidInfo: LiveData<KidInfo?> get() = _kidInfo  // 🔹 Observe this in UI

    private val firestore = FirebaseFirestore.getInstance()
    private val sharedPreferences = getApplication<Application>().getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)

    val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = Constants.apiKey
    )

    init {
        fetchKidInfo()
    }

    private fun fetchKidInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("ChatViewModel", "User ID is null")
            return
        }

        firestore.collection("kids").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val kidData = document.toObject(KidInfo::class.java)

                    // ✅ Now fetch homework links from Firestore
                    firestore.collection("kids").document(userId).collection("homework")
                        .get()
                        .addOnSuccessListener { homeworkDocs ->
                            val links = homeworkDocs.mapNotNull { it.getString("link") }
                            kidData?.let {
                                _kidInfo.postValue(it.copy(homeworksLinks = links))  // ✅ Update with links
                            }
                            Log.d("ChatViewModel", "Fetched KidInfo + Homework: $kidData")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatViewModel", "Error fetching homework links", e)
                        }
                } else {
                    Log.e("ChatViewModel", "No KidInfo found in Firestore")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error fetching KidInfo", e)
            }
    }


    @RequiresApi(35)
    fun sendMessage(question: String) {
        viewModelScope.launch {
            try {
                if (kidInfo.value == null) {
                    fetchKidInfo()
                    delay(1000)
                }
                val kid = kidInfo.value ?: return@launch
                val schedule = loadSchedule()
                val scheduleText = schedule.joinToString("\n") { "${it.activityName} - ${it.duration} min" }
                val homeworkLinks = if (kid.homeworksLinks.isEmpty()) {
                    "No homework for today!"
                } else {
                    kid.homeworksLinks.joinToString("\n") { "📖 $it" }
                }

                val prompt = """
                        Hey! You are a fun and friendly assistant for kids. Speak in a cheerful, engaging way.
                        
                        Child's Name: ${kid.name}
                        Age: ${kid.age}
                        School Timings: ${kid.schoolTimings}
                        Homework and study Duration: ${kid.homeworkDuration}
                        Hobbies: ${kid.hobbies.joinToString(", ")}
                        Learning Interests: ${kid.learningInterests.joinToString(", ")}
                        
                        Here are today's homework links:
                        $homeworkLinks
                        
                        Here's today's schedule:
                        $scheduleText
                        
                        The child asked: "$question". Respond in a friendly, simple, and engaging way.Keep the track of the schedule and remind the kid about it frequently. In schedule maintain a high priority of study like activities. But keep the schedule flexible too like allow kid to refuse a given activity but only twice otherwise warn him to inform parents.
                        """.trimIndent()

                val chat = generativeModel.startChat(
                    history = messageList.value?.map {
                        content(it.role) { text(it.message) }
                    }?: listOf(
                        content("model") {
                            text("You are a helpful and friendly assistant for children. Keep responses engaging and simple.")
                        }
                    )
                )

                val updatedList = _messageList.value ?: mutableListOf()
                updatedList.add(MessageModel(question, "user"))
                updatedList.add(MessageModel("Typing...", "model")) // Show "Typing..."
                _messageList.postValue(updatedList)

                val response = chat.sendMessage(prompt)

                updatedList.removeAt(updatedList.size - 1)
                updatedList.add(MessageModel(response.text.toString(), "model"))
                _messageList.postValue(updatedList)

            } catch (e: Exception) {
                val updatedList = _messageList.value ?: mutableListOf()
                if (updatedList.isNotEmpty()) updatedList.removeAt(updatedList.size - 1)
                updatedList.add(MessageModel("Error: ${e.message}", "model"))
                _messageList.postValue(updatedList)
            }
        }
    }

    // 🔹 Store Schedule in SharedPreferences
    fun saveSchedule(schedule: List<ScheduleItem>) {
        val gson = Gson()
        val jsonString = gson.toJson(schedule)
        sharedPreferences.edit().putString("daily_schedule", jsonString).apply()
        sharedPreferences.edit().putString("lastGeneratedDate", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())).apply()
    }

    // 🔹 Load Schedule from SharedPreferences
    fun loadSchedule(): List<ScheduleItem> {
        val lastDate = sharedPreferences.getString("lastGeneratedDate", null)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (lastDate != today) {
            return emptyList() // Force schedule regeneration
        }

        val jsonString = sharedPreferences.getString("daily_schedule", null) ?: return emptyList()
        val type = object : TypeToken<List<ScheduleItem>>() {}.type
        return Gson().fromJson(jsonString, type)
    }

    // 🔹 Generate Schedule (AI-based)
    fun generateSchedule(kidInfo: KidInfo) {
        Log.d("ChatViewModel", "Generating schedule for: ${kidInfo.name}, Age: ${kidInfo.age}") // ✅ Log kid info

        val prompt = """
            Generate a structured daily schedule based on:
            - Name: ${kidInfo.name}
            - Age: ${kidInfo.age}
            - School Timings: ${kidInfo.schoolTimings}
            - Homework and Study Duration: ${kidInfo.homeworkDuration}
            - Homework Links: ${kidInfo.homeworksLinks}
            - Hobbies: ${kidInfo.hobbies.joinToString(", ")}
            - Learning Interests: ${kidInfo.learningInterests.joinToString(", ")}

            The schedule should include **study time and homework time, playtime, and hobbies** only, and should be formatted as:
            "Activity Name - Duration (minutes)"
        """.trimIndent()

        generateSingleOutput(prompt) { response ->

            val scheduleList = response.lines().mapNotNull { line ->
                val parts = line.split("-").map { it.trim() }
                if (parts.size == 2) {
                    val activity = parts[0]
                    val duration = parts[1].toIntOrNull() ?: return@mapNotNull null
                    ScheduleItem(activity, duration, refuseCount = 0)
                } else {
                    null
                }
            }
            Log.d("Schedule Generated",scheduleList.toString())
            saveSchedule(scheduleList) // ✅ Store schedule locally
        }
    }

    private fun generateSingleOutput(prompt: String, callback: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val aiResponse = response.text.toString().trim()

                if (aiResponse.isNotEmpty()) {
                    callback(aiResponse)  // ✅ Pass AI response to callback
                } else {
                    callback("Failed to generate a response.")
                }
            } catch (e: Exception) {
                callback("Error: ${e.message}")
            }
        }
    }
}





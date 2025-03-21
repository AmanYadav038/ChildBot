package com.amans.childbot.pages

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.amans.KidInfo
import com.amans.childbot.chatbot.ChatPageActivity
import com.amans.childbot.chatbot.ChatViewModel
import com.amans.childbot.databinding.ActivityKidScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class KidScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKidScheduleBinding
    private lateinit var db: FirebaseFirestore
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Initialize View Binding
        binding = ActivityKidScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // ✅ SeekBar Listener to update Homework Duration Text
        binding.homeworkDurationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.homeworkDurationText.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // ✅ Time Picker for Start & End Time
        binding.startTimeBtn.setOnClickListener {
            showTimePicker { selectedTime ->
                binding.startTimeText.text = selectedTime
                Log.d("KidSchedule", "Start Time Selected: $selectedTime")
            }
        }

        binding.endTimeBtn.setOnClickListener {
            showTimePicker { selectedTime ->
                binding.endTimeText.text = selectedTime
                Log.d("KidSchedule", "End Time Selected: $selectedTime")
            }
        }

        // ✅ Save Kid's Schedule on Button Click
        binding.scheduleDispBtn.setOnClickListener {
            saveKidSchedule()
        }

        // ✅ Placeholder for AI Feature
        binding.callAI.setOnClickListener {
            Toast.makeText(this, "AI Feature Coming Soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveKidSchedule() {
        val name = binding.kidNameEdit.text.toString().trim()
        val ageInput = binding.kidAgeEdit.text.toString().trim()
        val classLevel = binding.kidClassEdit.text.toString().trim()
        val schoolStartTime = binding.startTimeText.text.toString().trim()
        val schoolEndTime = binding.endTimeText.text.toString().trim()
        val homeworkDuration = binding.homeworkDurationText.toString().toIntOrNull() ?: 0
        val preferredStudyTime = binding.studyPreferenceSpinner.selectedItem.toString()
        val hobbies = binding.hobbies.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val learningInterests = binding.learningInterest.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }

        // ✅ Validate Required Fields
        if (name.isEmpty() || ageInput.isEmpty() || classLevel.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageInput.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(this, "Please enter a valid age!", Toast.LENGTH_SHORT).show()
            return
        }

        if (schoolStartTime.isEmpty() || schoolEndTime.isEmpty()) {
            Toast.makeText(this, "Please select school timings!", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("KidScheduleActivity", "User not logged in!")
            return
        }

        val kid = KidInfo(name, age, classLevel,homeworkDuration, emptyList(),"${schoolStartTime}-${schoolEndTime}", hobbies, learningInterests)


// ✅ Log before writing to Firestore
        Log.d("KidScheduleActivity", "Saving kid info: $kid")

        db.collection("kids").document(userId)
            .set(kid)
            .addOnSuccessListener {
                Log.d("KidScheduleActivity", "Kid info saved successfully in Firestore!")
                generateSchedule(kid)
            }
            .addOnFailureListener { e ->
                Log.e("KidScheduleActivity", "Error saving kid info: ${e.message}")
            }

    }

    private fun generateSchedule(kid : KidInfo){
        chatViewModel.generateSchedule(kid)
        val intent = Intent(this, ChatPageActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(formattedTime)
        }, hour, minute, false)

        timePickerDialog.show()
    }
}


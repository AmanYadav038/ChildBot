package com.amans.childbot.chatbot

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.amans.KidInfo
import com.amans.childbot.R
import com.amans.childbot.databinding.ActivityChatPageBinding
import com.amans.childbot.databinding.DialogLoginBinding
import com.amans.childbot.pages.HomeworkUploadActivity
import com.amans.childbot.pages.LoginActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatPageBinding
    private lateinit var chatAdapter: ChatAdapter
    private val firestore = FirebaseFirestore.getInstance()

    private val chatViewModel: ChatViewModel by viewModels() // ✅ No need for a factory!

    private val speechRecognitionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val speechResult = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!speechResult.isNullOrEmpty()) {
                    binding.queryText.setText(speechResult[0])  // ✅ Fill EditText with recognized text
                }
            }
        }

    @RequiresApi(35)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        logSharedPreferences()
        val sharedPreferences = getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)

        if (!sharedPreferences.contains("daily_schedule") || isNewDay(sharedPreferences)) {
            fetchKidInfo { kidInfo ->
                if (kidInfo != null) {
                    Log.d("ChatPageActivity", "KidInfo available, generating schedule...")
                    chatViewModel.generateSchedule(kidInfo)
                } else {
                    Log.e("ChatPageActivity", "Kid info not found in Firebase")
                }
            }
        }

        // ✅ Set up RecyclerView
        chatAdapter = ChatAdapter(this, emptyList())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatPageActivity).apply { stackFromEnd = true }
            adapter = chatAdapter
        }

        // ✅ Observe LiveData for messages
        chatViewModel.messageList.observe(this) { messages ->
            chatAdapter.updateMessages(messages)
            binding.recyclerView.scrollToPosition(messages.size - 1)
        }

        // ✅ Send message on button click
        binding.sendBtn.setOnClickListener {
            val userInput = binding.queryText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                Log.d("ChatPageActivity", "Sending message: $userInput")
                chatViewModel.sendMessage(userInput)
                binding.welcomeText.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.queryText.text.clear()
            } else {
                Log.e("ChatPageActivity", "Empty message! Not sending")
            }
        }

        // ✅ Handle Speech Recognition
        binding.micIcon.setOnClickListener { startVoiceRecognition() }

        // ✅ Logout Functionality
        binding.logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.addHomeWork.setOnClickListener {
            showLoginDialog()
        }
    }

    private fun showLoginDialog() {
        val dialogView = DialogLoginBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setTitle("Parent Login")
            .setView(dialogView.root)
            .setPositiveButton("Login") { _, _ ->
                val email = dialogView.emailEditText.text.toString().trim()
                val password = dialogView.passwordEditText.text.toString().trim()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    authenticateParent(email, password)
                } else {
                    Toast.makeText(this, "Please enter email and password!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun authenticateParent(email: String, password: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    openHomeworkPage()
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun openHomeworkPage() {
        val intent = Intent(this, HomeworkUploadActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun logSharedPreferences() {
        val sharedPreferences = getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)
        Log.d("ChatPageActivity", "---- SharedPreferences Debug Start ----")
        sharedPreferences.all.forEach { (key, value) ->
            Log.d("ChatPageActivity", "Key: $key | Value: $value")
        }
        Log.d("ChatPageActivity", "---- SharedPreferences Debug End ----")
    }

    private fun isNewDay(sharedPreferences: SharedPreferences): Boolean {
        val lastDate = sharedPreferences.getString("lastGeneratedDate", null) ?: return true
        return lastDate != getTodayDate()
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // 🎤 Speech Recognition Function
    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        try {
            speechRecognitionLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Speech recognition is not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchKidInfo(callback: (KidInfo?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return callback(null)
        firestore.collection("kids").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val kidInfo = document.toObject(KidInfo::class.java)
                    callback(kidInfo)
                } else {
                    Log.e("ChatPageActivity", "No KidInfo found in Firestore")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatPageActivity", "Error fetching kid info: ${e.message}")
                callback(null)
            }
    }
}





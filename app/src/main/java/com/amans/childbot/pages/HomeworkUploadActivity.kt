package com.amans.childbot.pages

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.amans.childbot.R
import com.amans.childbot.chatbot.ChatPageActivity
import com.amans.childbot.databinding.ActivityHomeworkUploadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeworkUploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeworkUploadBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeworkUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.uploadHomeworkBtn.setOnClickListener {
            uploadHomework()
        }
        binding.backBtn.setOnClickListener{
            val intent = Intent(this, ChatPageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun uploadHomework() {
        val title = binding.homeworkTitleEdit.text.toString().trim()
        val link = binding.homeworkLinkEdit.text.toString().trim()

        if (title.isEmpty() || link.isEmpty()) {
            Toast.makeText(this, "Please enter both title and link!", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val homeworkData = mapOf(
            "title" to title,
            "link" to link,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("kids").document(userId).collection("homework")
            .add(homeworkData)
            .addOnSuccessListener {
                Toast.makeText(this, "Homework link saved!", Toast.LENGTH_SHORT).show()
                binding.homeworkTitleEdit.text.clear()
                binding.homeworkLinkEdit.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

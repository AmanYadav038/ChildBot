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
import com.amans.childbot.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        binding.signUpText.setOnClickListener{
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }
        binding.logInBtn.setOnClickListener{
            val email = binding.emailText.text.toString().trim()
            val password = binding.passText.text.toString().trim()

            if(email.isNotEmpty() && password.isNotEmpty()){
                loginUser(email,password)
            }else{
                Toast.makeText(this, "Please enter all fields",Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loginUser(email : String, password : String){
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){task->
                if(task.isSuccessful){
                    Toast.makeText(this, "Login Successful",Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ChatPageActivity::class.java))
                    finish()
                }else{
                    Toast.makeText(this, "Failed : ${task.exception?.message}",Toast.LENGTH_SHORT).show()
                }
            }
    }
}
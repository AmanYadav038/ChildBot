package com.amans.childbot.pages

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.amans.childbot.databinding.ActivitySignUpBinding
import com.amans.childbot.R
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySignUpBinding
    private lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        binding.loginText.setOnClickListener{
            val intent = Intent(this@SignUpActivity,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.signUpBtn.setOnClickListener {
            val email = binding.emailText.text.toString().trim()
            val password = binding.passInput.text.toString().trim()

            if(email.isNotEmpty() && password.isNotEmpty()){
                registerUser(email,password)
            }else{
                Toast.makeText(this, "fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun registerUser(email : String, password : String){
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){task->
                if(task.isSuccessful){
                    Toast.makeText(this, "Registered", Toast.LENGTH_SHORT)
                    startActivity(Intent(this, KidScheduleActivity::class.java))
                    finish()
                }else{
                    Toast.makeText(this, "Incomplete due to : ${task.exception?.message}",Toast.LENGTH_SHORT).show()
                }
            }
    }
}
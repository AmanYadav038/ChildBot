package com.amans.childbot.pages

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.amans.childbot.R
import com.amans.childbot.databinding.ActivityKidScheduleBinding
import com.amans.childbot.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebViewBinding
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.webView.settings.javaScriptEnabled = true  // Enable JavaScript if needed
        binding.webView.settings.domStorageEnabled = true // Enable storage for better experience
        binding.webView.webViewClient = WebViewClient() // Open links inside WebView

        val url = intent.getStringExtra("url") // Get the URL from intent
        if (url != null) {
            binding.webView.loadUrl(url) // Load the webpage
        }
    }
}
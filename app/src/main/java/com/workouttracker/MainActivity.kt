package com.faust

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.faust.databinding.ActivityMainBinding
import com.faust.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Entry point — shows splash, then routes to Auth or Home.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            delay(1500) // Splash duration
            navigate()
        }
    }

    private fun navigate() {
        // Check if user is logged in via SessionManager (injected in ViewModel)
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}

package com.ioi.myssue.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initApp(
            onReady = {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
            onError = {
                Toast.makeText(this, "초기화 실패", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

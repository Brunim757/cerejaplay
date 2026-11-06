package com.cerejaplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cerejaplay.app.ui.CerejaPlayRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CerejaPlayApp).container
        setContent {
            CerejaPlayRoot(container = container)
        }
    }
}

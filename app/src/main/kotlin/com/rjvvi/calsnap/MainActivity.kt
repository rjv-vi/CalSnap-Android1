package com.rjvvi.calsnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rjvvi.calsnap.data.prefs.UserPrefs
import com.rjvvi.calsnap.ui.NavGraph
import com.rjvvi.calsnap.ui.theme.CalSnapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = (application as CalSnapApp).prefs

        setContent {
            val themeStr by prefs.theme.collectAsStateWithLifecycle(initialValue = "system")
            val isDark = when (themeStr) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            CalSnapTheme(darkTheme = isDark) {
                NavGraph()
            }
        }
    }
}

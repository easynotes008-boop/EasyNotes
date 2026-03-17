package uk.ac.tees.mad.easynotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import uk.ac.tees.mad.easynotes.data.preferences.UserPreferencesManager
import uk.ac.tees.mad.easynotes.presentation.navigation.EasyNotesNavGraph
import uk.ac.tees.mad.easynotes.presentation.screens.settings.ThemePreference
import uk.ac.tees.mad.easynotes.presentation.theme.EasyNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val preferencesManager = remember { UserPreferencesManager(context) }
            val themePreference by preferencesManager.themePreferenceFlow.collectAsState(
                initial = ThemePreference.SYSTEM
            )
            val systemDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> systemDarkTheme
            }

            EasyNotesTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    EasyNotesNavGraph(navController = navController)
                }
            }
        }
    }
}
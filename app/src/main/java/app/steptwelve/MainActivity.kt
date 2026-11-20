package app.steptwelve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.steptwelve.ui.home.HomeScreen
import app.steptwelve.ui.tags.TagsScreen
import app.steptwelve.ui.theme.StepTwelveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepTwelveTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        HomeScreen(onOpenTags = { navController.navigate(Routes.TAGS) })
                    }
                    composable(Routes.TAGS) {
                        TagsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val TAGS = "tags"
}

package app.steptwelve

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.steptwelve.ui.MainViewModel
import app.steptwelve.ui.common.NoteEditorSheet
import app.steptwelve.ui.contact.ContactDetailScreen
import app.steptwelve.ui.home.HomeScreen
import app.steptwelve.ui.lock.LockScreen
import app.steptwelve.ui.refreshPermissions
import app.steptwelve.ui.settings.SettingsScreen
import app.steptwelve.ui.tags.TagsScreen
import app.steptwelve.ui.theme.StepTwelveTheme
import app.steptwelve.ui.timelog.TimelogScreen

/** FragmentActivity because BiometricPrompt needs one. */
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepTwelveTheme {
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val locked by viewModel.locked.collectAsStateWithLifecycle()
                val noteTarget by viewModel.noteTarget.collectAsStateWithLifecycle()

                // FLAG_SECURE blocks screenshots and blanks the recent-apps preview.
                val secure = settings?.screenPrivacy == true || settings?.appLock == true && locked
                LaunchedEffect(secure) {
                    if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }

                when {
                    settings == null -> Unit
                    settings!!.appLock && locked -> LockScreen(
                        appName = if (settings!!.discreetIcon) "Circle Back" else "StepTwelve",
                        onUnlocked = viewModel::unlock,
                    )
                    else -> {
                        AppNavigation(viewModel)
                        noteTarget?.let { target ->
                            NoteEditorSheet(
                                target = target,
                                onDismiss = viewModel::dismissNote,
                                onSave = viewModel::saveNote,
                                onDelete = viewModel::deleteNote,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onForeground()
    }

    override fun onResume() {
        super.onResume()
        // The user may grant or revoke access in system settings while we're away.
        viewModel.refreshPermissions(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.onBackground(this)
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Contacts", Icons.Filled.People),
    TIMELOG("timelog", "Timelog", Icons.AutoMirrored.Filled.List),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

@androidx.compose.runtime.Composable
private fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            NavHost(navController = navController, startDestination = Tab.HOME.route) {
                composable(Tab.HOME.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenContact = { key -> navController.navigate("contact/${android.net.Uri.encode(key)}") },
                        onOpenTags = { navController.navigate("tags") },
                    )
                }
                composable(Tab.TIMELOG.route) { TimelogScreen(viewModel) }
                composable(Tab.SETTINGS.route) {
                    SettingsScreen(viewModel, onOpenTags = { navController.navigate("tags") })
                }
                composable("contact/{lookupKey}") { entry ->
                    ContactDetailScreen(
                        lookupKey = entry.arguments?.getString("lookupKey").orEmpty(),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("tags") { TagsScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}

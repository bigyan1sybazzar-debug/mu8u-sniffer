package com.streamsniffer.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamsniffer.app.ui.auth.LoginScreen
import com.streamsniffer.app.ui.browser.BrowserScreen
import com.streamsniffer.app.ui.history.HistoryScreen
import com.streamsniffer.app.ui.iptv.IptvScreen
import com.streamsniffer.app.ui.player.PlayerScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Browser : Screen("browser", "Browser", Icons.Default.Language)
    object History : Screen("history", "History", Icons.Default.History)
    object IPTV : Screen("iptv", "IPTV", Icons.Default.LiveTv)
    object Login : Screen("login", "Login", Icons.Default.Language) // Using Language as a placeholder
    object Player : Screen("player/{url}/{title}", "Player") {
        fun createRoute(url: String, title: String): String {
            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            return "player/$encodedUrl/$encodedTitle"
        }
    }
}

@Composable
fun StreamSnifferNavGraph(
    externalStreamUrl: String? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle deep links or shared URLs
    LaunchedEffect(externalStreamUrl) {
        externalStreamUrl?.let {
            navController.navigate(Screen.Player.createRoute(it, "External Stream"))
        }
    }

    var isAdmin by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Player.route && currentRoute != Screen.Login.route) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    val navigationItems = if (isAdmin) {
                        listOf(Screen.IPTV, Screen.Browser, Screen.History)
                    } else {
                        listOf(Screen.IPTV, Screen.Login)
                    }

                    navigationItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    
                    if (isAdmin) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Logout") },
                            label = { Text("Logout") },
                            selected = false,
                            onClick = { isAdmin = false }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.IPTV.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.IPTV.route) {
                IptvScreen(
                    onPlayChannel = { stream ->
                        navController.navigate(Screen.Player.createRoute(stream.url, stream.title))
                    }
                )
            }
            composable(Screen.Browser.route) {
                if (isAdmin) {
                    BrowserScreen(
                        onNavigateToPlayer = { url, title ->
                            navController.navigate(Screen.Player.createRoute(url, title))
                        }
                    )
                }
            }
            composable(Screen.History.route) {
                if (isAdmin) {
                    HistoryScreen(
                        onPlayStream = { stream ->
                            navController.navigate(Screen.Player.createRoute(stream.url, stream.title))
                        }
                    )
                }
            }
            composable(Screen.Login.route) {
                // I'll create this screen next
                LoginScreen(
                    onLoginSuccess = { 
                        isAdmin = true
                        navController.navigate(Screen.IPTV.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: "Stream"
                PlayerScreen(
                    url = url,
                    title = title,
                    onBackPressed = { navController.navigateUp() }
                )
            }
        }
    }
}

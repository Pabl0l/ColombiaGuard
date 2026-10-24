package com.colombiaguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.colombiaguard.ui.config.ConfigScreen
import com.colombiaguard.ui.home.HomeScreen
import com.colombiaguard.ui.whitelist.WhitelistScreen
import com.colombiaguard.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColombiaGuardTheme {
                MainScreen()
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        NavigationItem("inicio", "Inicio", Icons.Default.Home),
        NavigationItem("whitelist", "Permitidos", Icons.Default.Check),
        NavigationItem("blacklist", "Bloqueados", Icons.Default.Clear),
        NavigationItem("config", "Config", Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ColombiaGuard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                contentColor = OnSurface
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) Primary else OnSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                item.title,
                                color = if (isSelected) Primary else OnSurfaceVariant
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        },
        containerColor = Background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "inicio",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("inicio") { HomeScreen() }
            composable("whitelist") { WhitelistScreen(isWhitelist = true) }
            composable("blacklist") { WhitelistScreen(isWhitelist = false) }
            composable("config") { ConfigScreen() }
        }
    }
}

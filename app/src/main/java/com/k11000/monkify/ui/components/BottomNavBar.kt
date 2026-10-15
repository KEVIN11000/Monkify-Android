package com.k11000.monkify.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.k11000.monkify.ui.navigation.bottomNavItems
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground

@Composable
fun BottomNavBar(
    navController: NavController,
    onHomeReselect: (() -> Unit)? = null
) {
    NavigationBar(
        containerColor = MonkifyBackground,
        contentColor = MonkifyOnBackground
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        if (item.route == "home") {
                            onHomeReselect?.invoke()
                        }
                    } else {
                        if (item.route == "home") {
                            onHomeReselect?.invoke()
                        }
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MonkifyOnBackground,
                    selectedTextColor = MonkifyOnBackground,
                    indicatorColor = MonkifyBackground,
                    unselectedIconColor = MonkifyGray,
                    unselectedTextColor = MonkifyGray
                )
            )
        }
    }
}

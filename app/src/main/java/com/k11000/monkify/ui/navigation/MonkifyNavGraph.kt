package com.k11000.monkify.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.k11000.monkify.ui.screens.HomeScreen
import com.k11000.monkify.ui.screens.LibraryScreen
import com.k11000.monkify.ui.screens.SearchScreen

@Composable
fun MonkifyNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()) // Solo empujamos la parte de abajo
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen()
        }
        composable(BottomNavItem.Search.route) {
            SearchScreen()
        }
        composable(BottomNavItem.Library.route) {
            LibraryScreen()
        }
    }
}

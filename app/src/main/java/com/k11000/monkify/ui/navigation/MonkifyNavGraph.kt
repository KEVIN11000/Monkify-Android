package com.k11000.monkify.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.screens.HomeScreen
import com.k11000.monkify.ui.screens.LibraryScreen
import com.k11000.monkify.ui.screens.PlaylistDetailScreen
import com.k11000.monkify.ui.screens.SearchScreen
import com.k11000.monkify.ui.screens.SettingsScreen

@Composable
fun MonkifyNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    homeScrollTrigger: Int = 0,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()) // Solo empujamos la parte de abajo
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(
                playerViewModel = playerViewModel,
                onNavigateToSettings = { navController.navigate(Destinations.SETTINGS) },
                scrollToTopTrigger = homeScrollTrigger
            )
        }
        composable(BottomNavItem.Search.route) {
            SearchScreen(playerViewModel = playerViewModel)
        }
        composable(BottomNavItem.Library.route) {
            LibraryScreen(
                playerViewModel = playerViewModel,
                onNavigateToPlaylist = { playlistId, name ->
                    navController.navigate(Destinations.playlistDetail(playlistId, name))
                }
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Destinations.PLAYLIST_DETAIL,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            PlaylistDetailScreen(
                playlistId = playlistId,
                initialName = name,
                playerViewModel = playerViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

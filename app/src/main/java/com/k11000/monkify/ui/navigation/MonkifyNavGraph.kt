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
import com.k11000.monkify.ui.screens.AlbumDetailScreen
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
    libraryScrollTrigger: Int = 0,
    searchResetTrigger: Int = 0,
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
                onNavigateToAlbum = { album, artist, coverUrl ->
                    navController.navigate(Destinations.albumDetail(album, artist, coverUrl))
                },
                scrollToTopTrigger = homeScrollTrigger
            )
        }
        composable(BottomNavItem.Search.route) {
            SearchScreen(
                playerViewModel = playerViewModel,
                scrollToResetTrigger = searchResetTrigger
            )
        }
        composable(BottomNavItem.Library.route) {
            LibraryScreen(
                playerViewModel = playerViewModel,
                scrollToTopTrigger = libraryScrollTrigger,
                onNavigateToPlaylist = { playlistId, name ->
                    navController.navigate(Destinations.playlistDetail(playlistId, name))
                },
                onNavigateToAlbum = { album, artist, coverUrl ->
                    navController.navigate(Destinations.albumDetail(album, artist, coverUrl))
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
        composable(
            route = Destinations.ALBUM_DETAIL,
            arguments = listOf(
                navArgument("album") { type = NavType.StringType; defaultValue = "" },
                navArgument("artist") { type = NavType.StringType; defaultValue = "" },
                navArgument("coverUrl") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val album = backStackEntry.arguments?.getString("album") ?: ""
            val artist = backStackEntry.arguments?.getString("artist") ?: ""
            val coverUrl = backStackEntry.arguments?.getString("coverUrl") ?: ""
            AlbumDetailScreen(
                albumName = album,
                initialArtist = artist,
                initialCoverUrl = coverUrl,
                playerViewModel = playerViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

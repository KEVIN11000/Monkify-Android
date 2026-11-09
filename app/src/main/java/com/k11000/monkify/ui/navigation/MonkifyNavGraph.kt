package com.k11000.monkify.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
    val bottomPadding = innerPadding.calculateBottomPadding()
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = Modifier.fillMaxSize() // Altura fija e inmutable: HomeScreen y ZenBackground no se redimensionan en runtime
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding)
            ) {
                SearchScreen(
                    playerViewModel = playerViewModel,
                    scrollToResetTrigger = searchResetTrigger
                )
            }
        }
        composable(BottomNavItem.Library.route) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding)
            ) {
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
            val rawName = backStackEntry.arguments?.getString("name") ?: ""
            val name = runCatching { java.net.URLDecoder.decode(rawName, "UTF-8") }.getOrDefault(rawName)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding)
            ) {
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    initialName = name,
                    playerViewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = Destinations.ALBUM_DETAIL,
            arguments = listOf(
                navArgument("album") { type = NavType.StringType; defaultValue = "" },
                navArgument("artist") { type = NavType.StringType; defaultValue = "" },
                navArgument("coverUrl") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawAlbum = backStackEntry.arguments?.getString("album") ?: ""
            val rawArtist = backStackEntry.arguments?.getString("artist") ?: ""
            val rawCoverUrl = backStackEntry.arguments?.getString("coverUrl") ?: ""

            val album = runCatching { java.net.URLDecoder.decode(rawAlbum, "UTF-8") }.getOrDefault(rawAlbum)
            val artist = runCatching { java.net.URLDecoder.decode(rawArtist, "UTF-8") }.getOrDefault(rawArtist)
            val coverUrl = runCatching { java.net.URLDecoder.decode(rawCoverUrl, "UTF-8") }.getOrDefault(rawCoverUrl)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding)
            ) {
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
}

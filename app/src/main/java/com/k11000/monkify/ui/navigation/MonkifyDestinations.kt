package com.k11000.monkify.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Inicio")
    object Search : BottomNavItem("search", Icons.Default.Search, "Buscar")
    object Library : BottomNavItem("library", Icons.Default.LibraryMusic, "Tu Biblioteca")
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Search,
    BottomNavItem.Library
)

object Destinations {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}?name={name}"
    const val ALBUM_DETAIL = "album_detail?album={album}&artist={artist}&coverUrl={coverUrl}"

    fun playlistDetail(playlistId: String, name: String = ""): String =
        "playlist_detail/$playlistId?name=${java.net.URLEncoder.encode(name, "UTF-8")}"

    fun albumDetail(album: String, artist: String = "", coverUrl: String? = null): String {
        val encAlbum = java.net.URLEncoder.encode(album, "UTF-8")
        val encArtist = java.net.URLEncoder.encode(artist, "UTF-8")
        val encCover = java.net.URLEncoder.encode(coverUrl ?: "", "UTF-8")
        return "album_detail?album=$encAlbum&artist=$encArtist&coverUrl=$encCover"
    }
}

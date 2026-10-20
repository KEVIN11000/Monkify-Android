package com.k11000.monkify

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.components.BottomNavBar
import com.k11000.monkify.ui.components.MiniPlayer
import com.k11000.monkify.ui.navigation.MonkifyNavGraph
import com.k11000.monkify.ui.screens.FullScreenPlayer
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MyApplicationTheme
import com.k11000.monkify.domain.repository.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    private val playerViewModel: PlayerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            syncScheduler.schedulePeriodicSync()
        } catch (_: Throwable) {
            // No bloquear la inicialización de UI si el planificador de WorkManager está en entorno restringido
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            playerViewModel.playbackError.collect { errorMsg ->
                android.widget.Toast.makeText(this@MainActivity, errorMsg, android.widget.Toast.LENGTH_LONG).show()
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                var showFullScreenPlayer by rememberSaveable { mutableStateOf(false) }
                var homeScrollTrigger by rememberSaveable { mutableStateOf(0) }
                var libraryScrollTrigger by rememberSaveable { mutableStateOf(0) }
                var searchResetTrigger by rememberSaveable { mutableStateOf(0) }
                val isZenCleared by playerViewModel.isZenCleared.collectAsState()

                // Interceptar retroceso del sistema para colapsar el reproductor sin salir de la app
                BackHandler(enabled = showFullScreenPlayer) {
                    showFullScreenPlayer = false
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MonkifyBackground,
                    bottomBar = {
                        Column {
                            AnimatedVisibility(visible = !showFullScreenPlayer) {
                                MiniPlayer(
                                    viewModel = playerViewModel,
                                    isZenPeek = isZenCleared,
                                    onRestoreZen = { playerViewModel.setZenCleared(false) },
                                    onExpand = { showFullScreenPlayer = true }
                                )
                            }
                            AnimatedVisibility(
                                visible = !showFullScreenPlayer && !isZenCleared,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            ) {
                                BottomNavBar(
                                    navController = navController,
                                    onHomeReselect = {
                                        showFullScreenPlayer = false
                                        playerViewModel.setZenCleared(false)
                                        homeScrollTrigger++
                                    },
                                    onLibraryReselect = {
                                        showFullScreenPlayer = false
                                        libraryScrollTrigger++
                                    },
                                    onSearchReselect = {
                                        showFullScreenPlayer = false
                                        searchResetTrigger++
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    MonkifyNavGraph(
                        navController = navController,
                        innerPadding = innerPadding,
                        homeScrollTrigger = homeScrollTrigger,
                        libraryScrollTrigger = libraryScrollTrigger,
                        searchResetTrigger = searchResetTrigger,
                        playerViewModel = playerViewModel
                    )
                }

                // Reproductor Pantalla Completa Flotante con físicas de resorte y desvanecimiento
                AnimatedVisibility(
                    visible = showFullScreenPlayer,
                    enter = slideInVertically(
                        initialOffsetY = { (it * 0.9f).toInt() },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 180)
                    )
                ) {
                    FullScreenPlayer(
                        viewModel = playerViewModel,
                        onClose = { showFullScreenPlayer = false },
                        onNavigateToPlaylist = { playlistId, name ->
                            showFullScreenPlayer = false
                            navController.navigate(com.k11000.monkify.ui.navigation.Destinations.playlistDetail(playlistId, name))
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_8")
@Composable
fun MonkifyMainPreview() {
    MyApplicationTheme {
        val navController = rememberNavController()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MonkifyBackground,
            bottomBar = {
                Column {
                    MiniPlayer()
                    BottomNavBar(navController = navController)
                }
            }
        ) { innerPadding ->
            MonkifyNavGraph(
                navController = navController,
                innerPadding = innerPadding
            )
        }
    }
}

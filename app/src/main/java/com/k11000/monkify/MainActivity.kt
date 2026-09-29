package com.k11000.monkify

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.k11000.monkify.ui.components.BottomNavBar
import com.k11000.monkify.ui.components.MiniPlayer
import com.k11000.monkify.ui.navigation.MonkifyNavGraph
import com.k11000.monkify.ui.screens.FullScreenPlayer
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch( Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                var showFullScreenPlayer by rememberSaveable { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MonkifyBackground,
                    bottomBar = {
                        Column {
                            AnimatedVisibility(visible = !showFullScreenPlayer) {
                                MiniPlayer(
                                    onExpand = { showFullScreenPlayer = true }
                                )
                            }
                            AnimatedVisibility(visible = !showFullScreenPlayer) {
                                BottomNavBar(navController = navController)
                            }
                        }
                    }
                ) { innerPadding ->
                    MonkifyNavGraph(
                        navController = navController,
                        innerPadding = innerPadding
                    )
                }

                // Reproductor Pantalla Completa Flotante
                AnimatedVisibility(
                    visible = showFullScreenPlayer,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    FullScreenPlayer(
                        onClose = { showFullScreenPlayer = false }
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

package com.k11000.monkify

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.components.BottomNavBar
import com.k11000.monkify.ui.components.MiniPlayer
import com.k11000.monkify.ui.navigation.MonkifyNavGraph
import com.k11000.monkify.ui.screens.FullScreenPlayer
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
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
    private var isInPipMode by mutableStateOf(false)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> playerViewModel.togglePlayPause()
                ACTION_PIP_NEXT -> playerViewModel.skipToNext()
                ACTION_PIP_PREV -> playerViewModel.skipToPrevious()
            }
        }
    }

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

        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_NEXT)
            addAction(ACTION_PIP_PREV)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipReceiver, filter)
        }

        lifecycleScope.launch {
            playerViewModel.isPlaying.collect { isPlaying ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        setPictureInPictureParams(buildPipParams(isPlaying))
                    } catch (_: Exception) {}
                }
            }
        }

        lifecycleScope.launch {
            playerViewModel.playbackError.collect { errorMsg ->
                android.widget.Toast.makeText(this@MainActivity, errorMsg, android.widget.Toast.LENGTH_LONG).show()
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                if (isInPipMode) {
                    val isPlaying by playerViewModel.isPlaying.collectAsState()
                    val title by playerViewModel.currentTrackTitle.collectAsState()
                    val artist by playerViewModel.currentTrackArtist.collectAsState()
                    val coverUrl by playerViewModel.coverUrl.collectAsState()
                    val progress by playerViewModel.progress.collectAsState()

                    PipPlayerContent(
                        isPlaying = isPlaying,
                        title = title,
                        artist = artist,
                        coverUrl = coverUrl,
                        progress = progress
                    )
                } else {
                    val navController = rememberNavController()
                    var showFullScreenPlayer by rememberSaveable { mutableStateOf(false) }
                    var homeScrollTrigger by rememberSaveable { mutableStateOf(0) }

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
                                        onExpand = { showFullScreenPlayer = true }
                                    )
                                }
                                AnimatedVisibility(visible = !showFullScreenPlayer) {
                                    BottomNavBar(
                                        navController = navController,
                                        onHomeReselect = {
                                            showFullScreenPlayer = false
                                            playerViewModel.setZenCleared(false)
                                            homeScrollTrigger++
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        MonkifyNavGraph(
                            navController = navController,
                            innerPadding = innerPadding,
                            homeScrollTrigger = homeScrollTrigger
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
                            onClose = { showFullScreenPlayer = false }
                        )
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerViewModel.isPlaying.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(buildPipParams(playerViewModel.isPlaying.value))
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun buildPipParams(isPlaying: Boolean): PictureInPictureParams {
        val actions = mutableListOf<RemoteAction>()

        val prevIntent = PendingIntent.getBroadcast(
            this, 101, Intent(ACTION_PIP_PREV).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                "Anterior", "Anterior", prevIntent
            )
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pausar" else "Reproducir"
        val playPauseIntent = PendingIntent.getBroadcast(
            this, 102, Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, playPauseIcon),
                playPauseTitle, playPauseTitle, playPauseIntent
            )
        )

        val nextIntent = PendingIntent.getBroadcast(
            this, 103, Intent(ACTION_PIP_NEXT).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_next),
                "Siguiente", "Siguiente", nextIntent
            )
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
        }

        return builder.build()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.k11000.monkify.PIP_PLAY_PAUSE"
        const val ACTION_PIP_NEXT = "com.k11000.monkify.PIP_NEXT"
        const val ACTION_PIP_PREV = "com.k11000.monkify.PIP_PREV"
    }
}

@Composable
fun PipPlayerContent(
    isPlaying: Boolean,
    title: String,
    artist: String,
    coverUrl: String?,
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MonkifyBackground)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                if (!coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "Carátula",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MonkifyPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.ifEmpty { "Monkify" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MonkifyOnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artist.ifEmpty { "Reproduciendo..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MonkifyGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = MonkifyPrimary,
                    trackColor = Color(0x33FFFFFF)
                )
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

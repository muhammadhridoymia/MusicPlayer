package com.example.musicapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.musicapp.Song
import com.example.musicapp.loadSongs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var currentSongIndex by remember { mutableStateOf(-1) }
    var nowPlayingTitle by remember { mutableStateOf<String?>(null) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            songs = loadSongs(context)
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            songs = loadSongs(context)
        } else {
            permissionLauncher.launch(permission)
        }
    }

    fun playSongAt(index: Int) {
        if (index < 0 || index >= songs.size) return

        val song = songs[index]
        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(context, song.uri)
        mediaPlayer?.setOnPreparedListener {
            duration = it.duration
            isPlaying = true
            it.start()
            nowPlayingTitle = if (song.title.length > 25) song.title.substring(0, 25) + "..." else song.title
            currentSongIndex = index

            coroutineScope.launch {
                while (isPlaying && mediaPlayer?.isPlaying == true) {
                    currentPosition = mediaPlayer?.currentPosition ?: 0
                    delay(1000)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🎵 Music App") })
        },
        bottomBar = {
            // FIXED: Moved the Mini-Player cleanly inside the bottomBar parameter
            if (nowPlayingTitle != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Album Art with integrated progress border 
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4A3E4E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎵", fontSize = 20.sp)

                            CircularProgressIndicator(
                                progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                                modifier = Modifier.matchParentSize(),
                                color = Color.Yellow,
                                strokeWidth = 2.dp,
                                trackColor = Color.Transparent
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Text Column (Title & Subtitle)
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = nowPlayingTitle ?: "Unknown Track",
                                maxLines = 1,
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Control Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "⏮",
                                fontSize = 22.sp,
                                modifier = Modifier.clickable {
                                    if (currentSongIndex > 0) playSongAt(currentSongIndex - 1)
                                }
                            )

                            Text(
                                text = if (isPlaying) "❚❚" else "▶",
                                fontSize = 26.sp,
                                modifier = Modifier.clickable {
                                    if (isPlaying) {
                                        mediaPlayer?.pause()
                                    } else {
                                        mediaPlayer?.start()
                                        coroutineScope.launch {
                                            while (mediaPlayer?.isPlaying == true) {
                                                currentPosition = mediaPlayer?.currentPosition ?: 0
                                                delay(1000)
                                            }
                                        }
                                    }
                                    isPlaying = !isPlaying
                                }
                            )

                            Text(
                                text = "⏭",
                                fontSize = 22.sp,
                                modifier = Modifier.clickable {
                                    if (currentSongIndex < songs.lastIndex) playSongAt(currentSongIndex + 1)
                                }
                            )

                            Text(
                                text = "🎶",
                                fontSize = 20.sp,
                                modifier = Modifier.clickable { /* Handle queue click */ }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(songs) { song ->
                val isCurrentSong = songs.indexOf(song) == currentSongIndex

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val index = songs.indexOf(song)
                            playSongAt(index)
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.Gray, shape = RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Replaced standard song.isPlaying with clean state verification
                            Text(if (isCurrentSong && isPlaying) "▶" else "🎵")
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(
                                text = if (song.title.length > 25) song.title.substring(0, 25) + "..." else song.title,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = song.artist,
                                fontSize = 7.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                HorizontalDivider() // Clean modern substitute for deprecated Divider()
            }
        }
    }
}

fun formatTime(ms: Int): String {
    val min = (ms / 1000) / 60
    val sec = (ms / 1000) % 60
    return String.format("%02d:%02d", min, sec)
}
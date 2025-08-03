package com.example.musicapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
        if (ContextCompat.checkSelfPermission(context, permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
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
            nowPlayingTitle = song.title
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
            if (nowPlayingTitle != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    tonalElevation = 8.dp,
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = nowPlayingTitle ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // SeekBar
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            onValueChange = {
                                val newPos = (it * duration).toInt()
                                mediaPlayer?.seekTo(newPos)
                                currentPosition = newPos
                            }
                        )

                        // Time Text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(currentPosition))
                            Text(formatTime(duration))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {
                                if (currentSongIndex > 0) playSongAt(currentSongIndex - 1)
                            }) {
                                Text("⏮️")
                            }

                            Button(onClick = {
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
                            }) {
                                Text(if (isPlaying) "⏸️" else "▶️")
                            }

                            Button(onClick = {
                                if (currentSongIndex < songs.lastIndex) playSongAt(currentSongIndex + 1)
                            }) {
                                Text("⏭️")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(songs) { song ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val index = songs.indexOf(song)
                            playSongAt(index)
                        }
                        .padding(16.dp)
                ) {
                    Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = song.artist, style = MaterialTheme.typography.bodyMedium)
                }
                Divider()
            }
        }
    }
}

fun formatTime(ms: Int): String {
    val min = (ms / 1000) / 60
    val sec = (ms / 1000) % 60
    return String.format("%02d:%02d", min, sec)
}

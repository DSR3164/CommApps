package com.example.commapps

import android.Manifest
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.commapps.ui.theme.CommAppsTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CommAppsTheme(dynamicColor = false) {
                MusicPlayerScreen()
            }
        }
    }
}

@Composable
fun MusicPlayerScreen() {
    var hasPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val musicFiles by remember {
        mutableStateOf(getMusicFiles())
    }

    var currentTrackIndex by remember { mutableStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        permissionLauncher.launch(permission)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.release()
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Error releasing MediaPlayer", e)
            }
        }
    }

    if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Permission required to access music files.")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Favorite List",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(musicFiles) { index, file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            tryPlay(mediaPlayer, file)
                            currentTrackIndex = index
                            isPlaying = true
                        }
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(file.name)
                        Text(file.path, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Divider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = {
                if (currentTrackIndex > 0) {
                    currentTrackIndex--
                    tryPlay(mediaPlayer, musicFiles[currentTrackIndex])
                    isPlaying = true
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
            }

            IconButton(onClick = {
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                } else if (currentTrackIndex != -1) {
                    mediaPlayer.start()
                    isPlaying = true
                }
            }) {
                Icon(if (isPlaying) Icons.Filled.Clear else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
            }

            IconButton(onClick = {
                mediaPlayer.stop()
                isPlaying = false
            }) {
                Icon(Icons.Filled.Clear, contentDescription = "Stop")
            }

            IconButton(onClick = {
                if (currentTrackIndex < musicFiles.lastIndex) {
                    currentTrackIndex++
                    tryPlay(mediaPlayer, musicFiles[currentTrackIndex])
                    isPlaying = true
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
        }
    }
}

private fun getMusicFiles(): List<File> {
    val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    return musicDir.listFiles()?.filter { it.extension in listOf("mp3", "wav") } ?: emptyList()
}

private fun tryPlay(mediaPlayer: MediaPlayer, file: File) {
    try {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.prepare()
        mediaPlayer.start()
    } catch (e: Exception) {
        Log.e("MusicPlayer", "Error playing file", e)
    }
}
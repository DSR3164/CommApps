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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.commapps.ui.theme.CommAppsTheme
import java.io.File


class MusicActivity : ComponentActivity() {
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

    val musicFiles = remember { mutableStateOf<List<File>>(emptyList()) }

    var currentTrackIndex by remember { mutableStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }
    var currentPosition by remember { mutableStateOf(0) }
    var totalDuration by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            musicFiles.value = getMusicFiles()
        }
    }

    LaunchedEffect(currentTrackIndex) {
        if (currentTrackIndex != -1) {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(musicFiles.value[currentTrackIndex].absolutePath)
            mediaPlayer.prepare()
            totalDuration = mediaPlayer.duration
            mediaPlayer.start()
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = mediaPlayer.currentPosition
            kotlinx.coroutines.delay(1000)
        }
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

    Column(modifier = Modifier.fillMaxSize().padding(top = 50.dp)) {
        Text(
            "Favorite List",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.inversePrimary
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(musicFiles.value) { index, file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            tryPlay(mediaPlayer, file)
                            currentTrackIndex = index
                            isPlaying = true
                        }
                        .padding(5.dp)
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.inversePrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(file.name.substringBeforeLast("."), color = MaterialTheme.colorScheme.inversePrimary)
                    }
                }
            }
        }

        Text(
            text = "%02d:%02d / %02d:%02d".format(
                currentPosition / 1000 / 60,
                (currentPosition / 1000) % 60,
                totalDuration / 1000 / 60,
                (totalDuration / 1000) % 60
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(80.dp),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = {
                if (currentTrackIndex > 0) {
                    currentTrackIndex--
                    tryPlay(mediaPlayer, musicFiles.value[currentTrackIndex])
                    isPlaying = true
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, modifier = Modifier.size(32.dp), contentDescription = "Previous")
            }

            IconButton(onClick = {
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                } else if (currentTrackIndex != -1) {
                    mediaPlayer.start()
                    isPlaying = true
                }
            },
                modifier = Modifier.size(48.dp))
            {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Default.PlayArrow, modifier = Modifier.size(48.dp), contentDescription = "Play/Pause")
            }

            IconButton(onClick = {
                if (currentTrackIndex < musicFiles.value.lastIndex) {
                    currentTrackIndex++
                    tryPlay(mediaPlayer, musicFiles.value[currentTrackIndex])
                    isPlaying = true
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, modifier = Modifier.size(32.dp),contentDescription = "Next")
            }
        }
    }
}

private fun getMusicFiles(): List<File> {
    val musicDir = Environment.getExternalStoragePublicDirectory("Music")
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
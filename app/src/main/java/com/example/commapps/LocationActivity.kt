package com.example.commapps

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.commapps.ui.theme.CommAppsTheme
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

data class LocationEntry(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)

class LocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CommAppsTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationSaverScreen()
                }
            }
        }
    }
}

@Composable
fun LocationSaverScreen() {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Location not fetched") }
    var hasPermission by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var entries by remember { mutableStateOf(listOf<LocationEntry>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms[ACCESS_FINE_LOCATION] == true || perms[ACCESS_COARSE_LOCATION] == true
    }

    val file = remember {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "location_data.json"
        )
    }

    fun loadLocationsFromFile(): List<LocationEntry> {
        return try {
            val text = file.readText()
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                LocationEntry(
                    latitude  = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    timestamp = obj.getString("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
        entries = loadLocationsFromFile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Button(onClick = {

            if (!hasPermission) {
                permissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
                statusText = "Запрашиваю разрешения..."
                return@Button
            }

            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val lat = location.latitude
                            val lon = location.longitude
                            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(Date())
                            val newObj = JSONObject().apply {
                                put("latitude", lat)
                                put("longitude", lon)
                                put("timestamp", time)
                            }

                            val arr = try { JSONArray(file.readText()) } catch (_: Exception) { JSONArray() }
                            arr.put(newObj)

                            FileWriter(file).use { it.write(arr.toString(2)) }

                            statusText = "SAVED: $time"
                            entries = loadLocationsFromFile()
                        } else {
                            statusText = "Location is null"
                        }
                    }
                    .addOnFailureListener { e ->
                        statusText = "Ошибка получения локации: ${e.message}"
                    }
            } catch (e: SecurityException) {
                statusText = "Нет разрешения на получение локации"
            }
        }) {
            Text("Get Location")
        }

        Spacer(Modifier.height(16.dp))
        Text(statusText)

        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(entries) { entry ->
                LocationCard(entry)
            }
        }
    }
}

@Composable
fun LocationCard(entry: LocationEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🕑 ${entry.timestamp}", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text("📍 Широта: ${entry.latitude}",
                style = MaterialTheme.typography.bodyMedium)
            Text("📍 Долгота: ${entry.longitude}",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

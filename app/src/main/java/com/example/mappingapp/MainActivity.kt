package com.example.mappingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mappingapp.ui.theme.MappingAppTheme
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polygon
import org.ramani.compose.Polyline
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity(), LocationListener {

    val latLngViewModel : LatLngViewModel by viewModels()
    var styleBuilder = Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent {
            val navController = rememberNavController()
            MappingAppTheme {
                Row(modifier = Modifier.fillMaxWidth()){
                    Button(modifier = Modifier.weight(1.0f), onClick = {
                        navController.navigate("settingsScreen")
                    }){
                        Text("Settings")
                    }
                    Button(modifier = Modifier.weight(1.0f), onClick = {
                        navController.navigate("mapScreen")
                    }){
                        Text("Map")
                    }
                }
                NavHost(navController = navController, startDestination = "mapScreen"){
                    composable("settingsScreen"){
                        SettingsScreenComposable{
                            //go back
                        }
                    }
                    composable("mapScreen") {
                        MapScreenComposable()
                    }
                }
            }
        }
    }

    @Composable
    fun SettingsScreenComposable(onSettingsAltered: () -> Unit){
        Column{
            Text("Settings")
            Button(onClick = { onSettingsAltered() }){
                Text("Alter Settings")
            }
        }
    }

    @Composable
    fun MapScreenComposable(){
        val latLngState = remember { mutableStateOf(LatLng(0.0, 0.0)) } //make these by remember
        val latitudeInput = remember { mutableStateOf("") }
        val longitudeInput = remember { mutableStateOf("") }

        latLngViewModel.latLngLiveData.observe(this) {
            latLngState.value = it
            latitudeInput.value = it.latitude.toString()
            longitudeInput.value = it.longitude.toString()
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) box@{
            MapLibre(
                modifier = Modifier.align(Alignment.TopCenter).height(this.maxHeight - 100.dp).offset(y = 50.dp),
                styleBuilder = styleBuilder,
                cameraPosition = CameraPosition(
                    target = latLngState.value,
                    zoom = 14.0
                )
            ) {
                Circle(
                    center = latLngState.value,
                    radius = 100.0f,
                    opacity = 0.5f
                )
            }
            Row(modifier = Modifier.align(Alignment.BottomCenter).height(100.dp).background(color = Color.White)) {
                TextField(
                    modifier = Modifier.weight(2f),
                    value = latitudeInput.value,
                    onValueChange = { latitudeInput.value = it },
                    label = { Text("Latitude:") },
                    singleLine = true
                )

                TextField(
                    modifier = Modifier.weight(2f),
                    value = longitudeInput.value,
                    onValueChange = { longitudeInput.value = it },
                    label = { Text("Longitude:") },
                    singleLine = true
                )

                Button(
                    modifier = Modifier
                        .weight(1.8f)
                        .padding(8.dp),
                    onClick = {
                        val lat = latitudeInput.value.toDoubleOrNull()
                        val lng = longitudeInput.value.toDoubleOrNull()

                        if (lat != null && lng != null) {
                            latLngState.value = LatLng(lat, lng)
                        }
                    }
                ) {
                    Text("Update")
                }
            }
        }
    }

    //Checks whether the GPS Permission has been granted
    //If it has, start the GPS
    //Else, request permission from the user
    fun checkPermissions(){
        val requiredPermission = Manifest.permission.ACCESS_FINE_LOCATION

        if(checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED) {
                startGPS()
            } else{
            val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if(isGranted) {
                    startGPS() // A function to start the GPS - see below
                } else {
                    // Permission not granted
                    Toast.makeText(this, "GPS permission not granted", Toast.LENGTH_LONG).show()
                }
            }
            permissionLauncher.launch(requiredPermission)
        }
    }

    @SuppressLint("MissingPermission")
    fun startGPS(){
        //start listening for GPS updates
        val mgr = getSystemService(LOCATION_SERVICE) as LocationManager
        val lastLocation = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        lastLocation?.let {
            latLngViewModel.latLng = LatLng(it.latitude, it.longitude)
        }
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this )
    }

    // Compulsory - provide onLocationChanged() method which runs whenever
    // the location changes
    override fun onLocationChanged(location: Location) {
        Toast.makeText(this, "Latitude: ${location.latitude}, Longitude: ${location.longitude}", Toast.LENGTH_SHORT).show()
        latLngViewModel.latLng = LatLng(location.latitude, location.longitude)
    }

    // Optional - runs when the user enables the GPS
    override fun onProviderEnabled(provider: String) {
        Toast.makeText(this, "GPS enabled", Toast.LENGTH_LONG).show()
    }

    // Optional - runs when the user disables the GPS
    override fun onProviderDisabled(provider: String) {
        Toast.makeText(this, "GPS disabled", Toast.LENGTH_LONG).show()
    }
}

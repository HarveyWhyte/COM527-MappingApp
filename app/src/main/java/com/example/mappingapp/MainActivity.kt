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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), LocationListener {

    val latLngViewModel : LatLngViewModel by viewModels()
    var styleBuilder = Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        setContent {
            val navController = rememberNavController()
            MappingAppTheme {
                var latLngState by remember { mutableStateOf(LatLng(0.0, 0.0)) }
                var zoomState by remember { mutableDoubleStateOf(14.0) }
                latLngViewModel.latLngLiveData.observe(this) {
                    latLngState = it
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(modifier = Modifier.height(100.dp)) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Home, "Home") },
                                label = { Text("Map") },
                                onClick = { navController.navigate("mapScreen") },
                                selected = false
                            )

                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Settings, "Settings") },
                                label = { Text("Settings") },
                                onClick = { navController.navigate("settingsScreen") },
                                selected = false
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(modifier = Modifier.padding(innerPadding), navController = navController, startDestination = "mapScreen"){
                        composable("settingsScreen"){
                            SettingsScreenComposable(latLngState, zoomState){ latLng, zoom ->
                                latLngViewModel.latLngLiveData.value = latLng
                                zoomState = zoom
                                navController.popBackStack()
                            }
                        }
                        composable("mapScreen") {
                            MapScreenComposable(latLngState, zoomState)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SettingsScreenComposable(latLngState: LatLng, zoomState: Double, onSettingsAltered: (LatLng, Double) -> Unit){
        var updatedLatLngState by remember { mutableStateOf(latLngState) }
        var updatedZoomState by remember { mutableDoubleStateOf(zoomState) }

        var latitudeInput by remember { mutableStateOf(latLngState.latitude.toString()) }
        var longitudeInput by remember { mutableStateOf(latLngState.longitude.toString()) }
        var zoomInput by remember { mutableStateOf(zoomState.toString()) }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ){
                TextField(
                    value = latitudeInput,
                    onValueChange = { latitudeInput = it },
                    label = { Text("Latitude:") },
                    singleLine = true
                )

                TextField(
                    value = longitudeInput,
                    onValueChange = { longitudeInput = it },
                    label = { Text("Longitude:") },
                    singleLine = true
                )

                TextField(
                    value = zoomInput,
                    onValueChange = { zoomInput = it },
                    label = { Text("Zoom:") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        val lat = latitudeInput.toDoubleOrNull()
                        val lng = longitudeInput.toDoubleOrNull()
                        val zoom = zoomInput.toDoubleOrNull()

                        if (lat != null && lng != null) {
                            updatedLatLngState = LatLng(lat, lng)
                        }
                        if (zoom != null){
                            updatedZoomState = zoom
                        }
                        onSettingsAltered(updatedLatLngState, updatedZoomState)
                    }
                ) {
                    Text("Update")
                }
            }
        }
    }

    @Composable
    fun MapScreenComposable(latLngState: LatLng, zoomState: Double){
        MapLibre(
            modifier = Modifier,
            styleBuilder = styleBuilder,
            cameraPosition = CameraPosition(
                target = latLngState,
                zoom = zoomState
            )
        )
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

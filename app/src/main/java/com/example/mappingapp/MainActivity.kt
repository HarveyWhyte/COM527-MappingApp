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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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


class MainActivity : ComponentActivity(), LocationListener {

    val latLngViewModel : LatLngViewModel by viewModels()
    var styleBuilder = Style.Builder().fromUri("https://tiles.openfreemap.org/styles/bright")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent {
            MappingAppTheme {
                val latLngState = remember { mutableStateOf(LatLng(0.0, 0.0)) }
                latLngViewModel.latLngLiveData.observe(this) {
                    latLngState.value = it
                }

                Column{
                    Spacer(modifier = Modifier.height(16.dp))
                    Row{
                        TextField(
                            modifier = Modifier.weight(2.0f),
                            value = latLngState.value.latitude.toString(),
                            onValueChange={ latLngState.value.latitude = it.toDouble() },
                            label={ Text("Enter Latitude:") }
                        )
                        TextField(
                            modifier = Modifier.weight(2.0f),
                            value = latLngState.value.longitude.toString(), //change
                            onValueChange={ latLngState.value.longitude = it.toDouble() }, //change
                            label={ Text("Enter Longitude:") }
                        )
                        Button(modifier = Modifier.weight(1.0f).padding(8.dp),
                            onClick = { latLngState.value =  //temp langstate where change comments are}
                        ){
                            Text("Clear")
                        }
                    }
                    MapLibre(
                        modifier=Modifier.fillMaxSize(),
                        styleBuilder = styleBuilder,
                        cameraPosition = CameraPosition(
                            target  = latLngState.value,
                            zoom = 14.0
                        )
                    ){
                        Circle(center = latLngState.value,
                            radius = 100.0f,
                            opacity = 0.5f
                        )

                        Polyline(
                            points = listOf(
                                latLngState.value,
                                LatLng(latLngState.value.latitude - 0.001, latLngState.value.longitude - 0.003),
                            ),
                            color = "#0000ff",
                            lineWidth = 3.0f
                        )

                        Polygon(
                            vertices = listOf(
                                LatLng(latLngState.value.latitude - 0.003, latLngState.value.longitude),
                                LatLng(latLngState.value.latitude - 0.004, latLngState.value.longitude - 0.003),
                                LatLng(latLngState.value.latitude - 0.005, latLngState.value.longitude + 0.003)
                            ),
                            fillColor = "#ff0000",
                            opacity = 0.3f,
                            borderColor = "#ff0000"
                        )
                    }
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

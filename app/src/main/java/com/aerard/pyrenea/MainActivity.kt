package com.aerard.pyrenea

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aerard.pyrenea.map.ui.PyreneaMapController
import com.aerard.pyrenea.map.ui.PyreneaMapScreen
import com.aerard.pyrenea.map.vm.MapViewViewModel
import com.aerard.pyrenea.ui.theme.PyreneaTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import java.io.InputStream

class MainActivity() : ComponentActivity() {

    private val mapViewModel: MapViewViewModel by viewModels()
    val locationRequest = LocationRequest.Builder(2_500, ).setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY).setWaitForAccurateLocation(true).build()

    private  lateinit var fusedLocationService: FusedLocationProviderClient
    private var isLocationGranted =false
    val getContent = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        Log.d("Pyrenea", uri.toString())
        uri?.let {
            val fis =
                contentResolver.openInputStream(it)
            if (fis == null) {
                return@let
            }
            mapViewModel.onGpxFileChange(
                fis
            )
        }
    }

    val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Location granted
                isLocationGranted = true
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                //location granted
                isLocationGranted = true
            }
        }
        if (isLocationGranted) {
            fusedLocationService.requestLocationUpdates(locationRequest, mapViewModel.locationCallback, Looper.getMainLooper())
        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationService = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            PyreneaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    val uiState by mapViewModel.state.collectAsStateWithLifecycle()
                    if (isLocationGranted) {
                        fusedLocationService.lastLocation.addOnSuccessListener{ location : Location?->
                            if (location != null) {
                                mapViewModel.setLocation(location)
                            }
                        }
                    }

                    PyreneaMapScreen(inner, uiState, object : PyreneaMapController {
                        override fun handleZoom(value: Double) {
                           mapViewModel.setZoom(value)
                        }

                        override fun setCenter() {
                           mapViewModel.setCenter()
                        }

                        override fun onFileImportClick() {
                            openFileChooser()
                        }
                    })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    override fun onStop() {
        super.onStop()
        fusedLocationService.removeLocationUpdates(mapViewModel.locationCallback)
    }

    private fun openFileChooser() {
        getContent.launch(arrayOf("*/*"))

    }
}



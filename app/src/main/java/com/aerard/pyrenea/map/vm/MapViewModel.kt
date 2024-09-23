package com.aerard.pyrenea.map.vm

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.aerard.pyrenea.map.service.gpx.GpxLoader
import com.aerard.pyrenea.map.service.gpx.GpxParserAdapter
import com.aerard.pyrenea.map.service.gpx.PWaypoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.util.GeoPoint
import java.io.InputStream

data class MapState(
    val gpxPath: List<GeoPoint> = listOf(),
    val gpxPathColor: String = "#efc23e",
    val zoomLevel: Double = 15.0,
    val location: Location? = null,
    val isCenterSet: Boolean = false,
    val gpxWaypoints: List<PWaypoint> = listOf()
    )

class MapViewViewModel() : ViewModel() {
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            Log.d("Pyrenea", "Location updated : $location")
            setLocation(location)
        }
    }
    fun setLocation(location: Location?) {
        _state.update { currentState ->
            currentState.copy(
                location = location
            )

        }
    }

    fun setCenter() {
        _state.update { currentState ->
            currentState.copy(
                isCenterSet = true
            )
        }
    }

    fun removeCenter() {
        _state.update { currentState ->
            currentState.copy(
                isCenterSet = false
            )
        }
    }
    fun onGpxFileChange(input: InputStream) {
        Log.d("Pyrenea", "OnGpxFileChange")
        val gpxData = GpxLoader(GpxParserAdapter()).loadGpx(input)
        _state.update { currentState ->
            currentState.copy(
                gpxPath = gpxData.track,
                gpxWaypoints = gpxData.waypoints
            )
        }
    }

    fun setZoom(zoom: Double) {
        _state.update { currentState ->
            currentState.copy(
                zoomLevel = zoom
            )
        }
    }
}

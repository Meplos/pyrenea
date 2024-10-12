package com.aerard.pyrenea.feature.tracevizualization

import android.util.Log
import com.aerard.pyrenea.map.service.gpx.GpxLoader
import com.aerard.pyrenea.map.service.gpx.GpxParserAdapter
import com.aerard.pyrenea.map.service.gpx.PWaypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.util.GeoPoint
import java.io.InputStream

data class Trace (
    val path : List<GeoPoint> = listOf(),
    val wpts : List<PWaypoint> = listOf()
)

class TraceRepository {
    val data = MutableStateFlow(Trace())

    suspend fun loadTrace(input: InputStream) {
        Log.d("Pyrenea", "Load Trace")
        val gpxData = GpxLoader(GpxParserAdapter()).loadGpx(input)
        data.update { currentState ->
            currentState.copy(
                path = gpxData.track,
                wpts =  gpxData.waypoints
            )
        }
    }

    suspend fun clearTrace() {
        data.update { currentState ->
            currentState.copy(
                path =  listOf(),
                wpts = listOf(),
            )
        }
    }
}
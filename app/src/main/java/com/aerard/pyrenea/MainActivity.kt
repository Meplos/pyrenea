package com.aerard.pyrenea

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aerard.pyrenea.ui.theme.PyreneaTheme
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.InputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PyreneaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
                        val mapViewModel:MapViewViewModel by viewModels()
                        mapViewModel.onGpxFileChange(assets.open("aule.gpx"))
                        MapView(context).apply {
                            minZoomLevel = 8.0
                            setExpectedCenter(GeoPoint(43.4857, -0.775))
                            setMultiTouchControls(true)
                            setTileSource(TileSourceFactory.OpenTopo)
                            lifecycleScope.launch {
                                repeatOnLifecycle(Lifecycle.State.STARTED) {
                                        mapViewModel.state.collect {
                                            val p: Polyline = Polyline(this@apply, true)
                                            p.outlinePaint.color= Color.parseColor(it.gpxPathColor)
                                            it.gpxPath.forEach(p::addPoint)
                                            overlayManager.add(p)
                                            invalidate()
                                        }
                                }
                            }
                        }
                    })
                }
            }
        }
    }
}


data class MapState (val gpxPath : List<GeoPoint> = listOf(), val gpxPathColor: String= "#efc23e")

class MapViewViewModel() : ViewModel() {
    private val _state = MutableStateFlow(MapState())
    val state : StateFlow<MapState> = _state.asStateFlow()

    fun onGpxFileChange(input : InputStream) {
        val path = GpxLoader(GpxParserAdapter()).loadGpx(input)
        _state.update { currentState ->
            currentState.copy(
                gpxPath = path
            )
        }
    }

}

interface GpxParser {
    fun parse(fis: InputStream): Gpx
}

class  GpxParserAdapter : GpxParser {
    val parser = GPXParser()
    override fun parse(fis: InputStream): Gpx {
        return parser.parse(fis)
    }
}

class GpxLoader(parser : GpxParser) {
    fun loadGpx(fis:InputStream) : List<GeoPoint> {
        val parser = GPXParser()
        val parsed = parser.parse(fis)
         return parsed.tracks.map {
            trk -> trk.trackSegments
        }
            .reduce { acc, trackSegments -> acc.addAll(trackSegments);acc}
            .map { seg -> seg.trackPoints }
            .reduce { acc, pts -> acc.addAll(pts); acc}
            .map {
                pt -> GeoPoint(pt.latitude, pt.longitude)
            }
    }

}


fun buildPath(fis: InputStream, map: MapView): Polyline {
    val points = GpxLoader(GpxParserAdapter()).loadGpx(fis)
    val path = Polyline(map, true, true)
    points.forEach {path.addPoint(it)}
    return path
}

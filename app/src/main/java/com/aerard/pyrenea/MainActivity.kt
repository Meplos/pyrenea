package com.aerard.pyrenea

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.CustomZoomButtonsController.OnZoomListener
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.InputStream

class MainActivity() : ComponentActivity() {

    private val mapViewModel: MapViewViewModel by viewModels()

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PyreneaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    Box(modifier = Modifier
                        .padding(inner)
                        .zIndex(10f)
                        .fillMaxSize()) {
                        Box() {
                            AndroidView(
                                factory = { context ->
                                    mapViewModel.onGpxFileChange(assets.open("aule.gpx"))
                                    MapView(context).apply {
                                        addMapListener(object : MapListener {
                                            override fun onScroll(event: ScrollEvent?): Boolean {
                                                return true
                                            }

                                            override fun onZoom(event: ZoomEvent?): Boolean {
                                                if (event?.zoomLevel !== null) {
                                                    mapViewModel.setZoom(event.zoomLevel)
                                                }
                                                return true
                                            }
                                        })
                                        minZoomLevel = 8.0
                                        controller.setZoom(mapViewModel.state.value.zoomLevel)
                                        setExpectedCenter(GeoPoint(43.4857, -0.775))
                                        setMultiTouchControls(true)
                                        setTileSource(TileSourceFactory.OpenTopo)
                                        lifecycleScope.launch {
                                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                                mapViewModel.state.collect {
                                                    overlayManager.removeAll(overlayManager.overlays())
                                                    val p: Polyline = Polyline(this@apply, true)
                                                    p.outlinePaint.color =
                                                        Color.parseColor(it.gpxPathColor)
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
                    IconButton(
                        modifier = Modifier
                            .padding(inner)
                            .zIndex(10f),
                        onClick = {
                            Log.d("Pyrenea", "Click on button")
                            openFileChooser()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "",
                            modifier = Modifier.size(30.dp),
                            tint = androidx.compose.ui.graphics.Color.Black
                        )
                    }
                }
            }
        }
    }

    private fun openFileChooser() {
        getContent.launch(arrayOf("*/*"))

    }
}


data class MapState(val gpxPath: List<GeoPoint> = listOf(), val gpxPathColor: String = "#efc23e", val zoomLevel: Double = 15.0)

class MapViewViewModel() : ViewModel() {
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()


    fun onGpxFileChange(input: InputStream) {
        val path = GpxLoader(GpxParserAdapter()).loadGpx(input)
        _state.update { currentState ->
            currentState.copy(
                gpxPath = path
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

interface GpxParser {
    fun parse(fis: InputStream): Gpx
}

class GpxParserAdapter : GpxParser {
    val parser = GPXParser()
    override fun parse(fis: InputStream): Gpx {
        return parser.parse(fis)
    }
}

class GpxLoader(parser: GpxParser) {
    fun loadGpx(fis: InputStream): List<GeoPoint> {
        val parser = GPXParser()
        val parsed = parser.parse(fis)
        return parsed.tracks
            .map { trk -> trk.trackSegments }
            .reduce { acc, trackSegments -> acc.addAll(trackSegments);acc }
            .map { seg -> seg.trackPoints }
            .reduce { acc, pts -> acc.addAll(pts); acc }
            .map { pt ->
                GeoPoint(pt.latitude, pt.longitude)
            }
    }
}
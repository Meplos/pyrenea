package com.aerard.pyrenea.map.ui

import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat.getDrawable
import com.aerard.pyrenea.map.vm.MapState
import com.aerard.pyrenea.utils.toGeopoint
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.IconOverlay
import org.osmdroid.views.overlay.Polyline

interface PyreneaMapController {
    fun handleZoom(value: Double)
    fun onFileImportClick()
}

@Composable
fun PyreneaMapScreen(
    inner: PaddingValues, uiState: MapState, mapController: PyreneaMapController, ) {
    var positionIcon : IconOverlay? = null
    Box(
        modifier = Modifier
            .padding(inner)
            .zIndex(10f)
            .fillMaxSize()
    ) {
        Box() {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                return true
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                if (event?.zoomLevel !== null) {
                                    mapController.handleZoom(event.zoomLevel)
                                }
                                return true
                            }
                        })
                        minZoomLevel = 8.0
                        controller.setZoom(uiState.zoomLevel)
                        setExpectedCenter(GeoPoint(43.4857, -0.775))
                        setMultiTouchControls(true)
                        setTileSource(TileSourceFactory.OpenTopo)
                        if (uiState.location != null) {
                            setExpectedCenter(uiState.location.toGeopoint())
                        }
                }
        },
                update = {view ->
                    view.overlayManager.removeAll(view.overlayManager.overlays())
                    val p: Polyline = Polyline(view, true)
                    p.outlinePaint.color =
                        Color.parseColor(uiState.gpxPathColor)
                    uiState.gpxPath.forEach(p::addPoint)
                    view.overlayManager.add(p)
                    if (uiState.location != null) {
                        if (positionIcon != null) {
                            positionIcon!!.moveTo(uiState.location.toGeopoint(), view)
                        } else {
                            positionIcon = IconOverlay()
                            positionIcon!!.set(
                                uiState.location.toGeopoint(), getDrawable(view.context,
                                    org.osmdroid.library.R.drawable.osm_ic_follow_me
                                )
                            )
                        }
                        view.overlayManager.add(positionIcon)
                    }
                    view.invalidate()
                }
                )
    }
}
IconButton(
    modifier = Modifier
    .padding(inner)
    .zIndex(10f),
    onClick = {
        Log.d("Pyrenea", "Open file")
        mapController.onFileImportClick()
    }) {
    Icon(
        imageVector = Icons.Default.Download,
        contentDescription = "",
        modifier = Modifier.size(30.dp),
        tint = androidx.compose.ui.graphics.Color.Black
    )
    }
}
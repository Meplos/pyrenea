package com.aerard.pyrenea.map.ui

import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat.getDrawable
import com.aerard.pyrenea.R
import com.aerard.pyrenea.map.vm.PMapState
import com.aerard.pyrenea.ui.theme.HunterGreen
import com.aerard.pyrenea.ui.theme.Parchement
import com.aerard.pyrenea.utils.toGeopoint
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.IconOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import kotlin.math.abs

interface PMapController {
    fun handleZoom(value: Double)
    fun onFileImportClick()
    fun setCenter(c: GeoPoint)
    fun bind(view: MapView)
    fun enableFollowing()
    fun disableFollowing()
    fun clearGpxData()
}

@Composable
fun PMapScreen(
    inner: PaddingValues, uiState: PMapState, mapController: PMapController,
) {
    Box(
        modifier = Modifier
            .padding(inner)
            .zIndex(10f)
            .fillMaxSize()
    ) {
        Box() {
            PMap(mapController, uiState)
        }
        PMainMenu(Modifier.matchParentSize(), mapController, uiState)
    }
}

@Composable
fun PMap(mapController: PMapController, uiState: PMapState) {
    var lastPath: Polyline? = null
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        if (event == null) return false
                        if (abs(event.x) > 10 && abs(event.y) > 10)
                        mapController.disableFollowing()
                        return true
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        if (event?.zoomLevel !== null) {
                            mapController.handleZoom(event.zoomLevel)
                        }
                        return true
                    }
                })
                minZoomLevel = 2.0
                controller.setZoom(uiState.zoomLevel)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                setTileSource(TileSourceFactory.OpenTopo)
            }
        },
        update = { view ->
            mapController.bind(view)
            view.overlayManager.removeAll(view.overlayManager.overlays())
            val info = MarkerInfoWindow(R.layout.marker_info_layout, view).apply {
            }
            if (uiState.gpxPath.isNotEmpty()) {
                lastPath = Polyline(view, true)
                lastPath?.apply {
                    outlinePaint.color =
                        Color.parseColor(uiState.gpxPathColor)
                    uiState.gpxPath.forEach(this::addPoint)
                    infoWindow = null
                }
                view.overlayManager.add(lastPath)
                val start = uiState.gpxPath.first()
                val end = uiState.gpxPath.last()
                if (start.distanceToAsDouble(end) < 100) {
                    val startMarker = Marker(view).apply {
                        position = start
                        icon = getDrawable(
                            view.context,
                            R.drawable.baseline_flag_circle_24
                        )

                        title = "Départ - Arrivé"
                        setInfoWindow(info)
                    }
                    view.overlayManager.add(startMarker)
                } else {
                    val startMarker = Marker(view).apply {
                        position = start
                        icon = getDrawable(
                            view.context,
                            R.drawable.baseline_flag_24
                        )?.mutate()?.apply {
                            setTint(Color.parseColor("#8DECB4"))
                        }
                        title = "Départ"
                        setInfoWindow(info)
                    }

                    val endMarker = Marker(view).apply {
                        position = end
                        icon = getDrawable(
                            view.context,
                            R.drawable.baseline_flag_24
                        )?.mutate()?.apply { setTint(Color.parseColor("#C9484D")) }
                        title = "Arrivé"
                        setInfoWindow(info)
                    }

                    view.overlayManager.addAll(listOf(startMarker, endMarker))
                }

            }

            if (uiState.gpxWaypoints.isNotEmpty()) {
                val wpts = uiState.gpxWaypoints.mapIndexed { idx, it ->
                    val markerIcon =
                        getDrawable(
                            view.context,
                            R.drawable.baseline_location_pin_24
                        )?.mutate()
                    markerIcon?.setTint(Color.parseColor("#1679AB"))
                    Marker(view).apply {
                        position = it.location
                        icon = markerIcon
                        title = "$idx - ${it.name}"
                        subDescription = "${it.altitude}"
                        snippet = it.description
                        setInfoWindow(info)
                    }
                }
                view.overlayManager.addAll(wpts)
            }

            if (uiState.location != null ) {
                val positionIcon = IconOverlay()
                positionIcon.set(
                    uiState.location.toGeopoint(), getDrawable(
                        view.context,
                        R.drawable.baseline_navigation_24
                    )
                )
                view.overlayManager.add(positionIcon)
                if (uiState.isFollowing) {
                    view.setExpectedCenter(uiState.location.toGeopoint())
                }
            }
        }
    )
}


@Composable
fun PMainMenu(modifier: Modifier, mapController: PMapController, uiState: PMapState) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        if (uiState.gpxPath.isNotEmpty()) {
            SmallFloatingActionButton(
                modifier = Modifier.padding(2.dp),
                contentColor = Parchement,
                containerColor = HunterGreen,
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    mapController.setCenter(uiState.gpxPath.first())
                }) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Se rendre au départ",
                    modifier = Modifier.size(30.dp),
                )
            }
            SmallFloatingActionButton(
                modifier = Modifier.padding(2.dp),
                contentColor = Parchement,
                containerColor = HunterGreen,
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    mapController.clearGpxData()
                }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Effacer la trace gpx",
                    modifier = Modifier.size(30.dp),
                )
            }

        }
        SmallFloatingActionButton(
            modifier = Modifier.padding(2.dp),
            contentColor = Parchement,
            containerColor = HunterGreen,
            shape = RoundedCornerShape(10.dp),
            onClick = {
                Log.d("Pyrenea", "Open file")
                mapController.onFileImportClick()
            }) {
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = "Importer un fichier gpx",
                modifier = Modifier.size(30.dp),
            )
        }
        SmallFloatingActionButton(
            modifier = Modifier.padding(2.dp),
            contentColor = Parchement,
            containerColor = HunterGreen,
            shape = RoundedCornerShape(10.dp),
            onClick = {
                Log.d("Pyrenea", "Open file")
                mapController.enableFollowing()
            }) {
            var icon = Icons.Default.LocationSearching
            if (uiState.isFollowing) {
                icon = Icons.Default.MyLocation
            }
            Icon(
                imageVector = icon,
                contentDescription = "Recentrer",
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
package com.aerard.pyrenea.map.service.gpx

import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import io.ticofab.androidgpxparser.parser.domain.WayPoint
import org.osmdroid.util.GeoPoint
import java.io.InputStream

interface GpxParser {
    fun parse(fis: InputStream): Gpx
}

class GpxParserAdapter : GpxParser {
    val parser = GPXParser()
    override fun parse(fis: InputStream): Gpx {
        return parser.parse(fis)
    }
}

data class GpxData (
    val track: List<GeoPoint>,
    val waypoints: List<PWaypoint>
)
data class PWaypoint(
    val location: GeoPoint,
    val altitude : Double,
    val name : String,
    val description: String
)

class GpxLoader(val parser: GpxParser) {
    fun loadGpx(fis: InputStream): GpxData {
        val parsed = parser.parse(fis)
        val waypoints = parsed.wayPoints.map {
            PWaypoint(
                location = GeoPoint(it.latitude, it.longitude),
                altitude = it.elevation,
                name= it.name.orEmpty(),
                description = it.desc.orEmpty()
            )
        }
        val track = parsed.tracks
            .map { trk -> trk.trackSegments }
            .reduce { acc, trackSegments -> acc.addAll(trackSegments);acc }
            .map { seg -> seg.trackPoints }
            .reduce { acc, pts -> acc.addAll(pts); acc }
            .map { pt ->
                GeoPoint(pt.latitude, pt.longitude)
            }

        return GpxData(track, waypoints)
    }
}

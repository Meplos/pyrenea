package com.aerard.pyrenea.map.service.gpx

import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
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

class GpxLoader(val parser: GpxParser) {
    fun loadGpx(fis: InputStream): List<GeoPoint> {
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

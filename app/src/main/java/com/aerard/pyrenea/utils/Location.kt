package com.aerard.pyrenea.utils

import android.location.Location
import org.osmdroid.util.GeoPoint

fun Location.toGeopoint() : GeoPoint = GeoPoint(latitude, longitude)

package com.aerard.pyrenea

import android.app.Application
import com.aerard.pyrenea.feature.tracevizualization.TraceRepository

class PyreneaApp : Application(){

    val traceRepository = TraceRepository()
}
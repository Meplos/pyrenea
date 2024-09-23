package com.aerard.pyrenea.map.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.aerard.pyrenea.R
import com.aerard.pyrenea.ui.theme.PyreneaTheme

class MarkerInfoLayout(context: Context, attrSet: AttributeSet): LinearLayout(context, attrSet) {
    init {
        inflate(context, R.layout.marker_info_layout, this)
    }
}
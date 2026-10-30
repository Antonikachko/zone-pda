
package com.zonepda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView

private val satelliteSource = object : OnlineTileSourceBase(
    "Esri World Imagery",
    1,
    19,
    256,
    ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(tileIndex: Long): String {
        return baseUrl +
            MapTileIndex.getZoom(tileIndex) + "/" +
            MapTileIndex.getY(tileIndex) + "/" +
            MapTileIndex.getX(tileIndex)
    }
}

@Composable
fun ZoneMapPage() {
    var satellite by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val mapView = remember {
        Configuration.getInstance().userAgentValue =
            context.packageName

        MapView(context).apply {
            setTileSource(satelliteSource)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(7.0)
            controller.setCenter(GeoPoint(58.7, 25.5))
        }
    }

    LaunchedEffect(satellite) {
        mapView.setTileSource(
            if (satellite) satelliteSource
            else TileSourceFactory.MAPNIK
        )
        mapView.invalidate()
    }

    DisposableEffect(mapView) {
        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF080E09))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { satellite = true },
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (satellite)
                        Color(0xFF29442E)
                    else Color(0xFF101C13)
                ),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text("СУПУТНИК", fontSize = 9.sp)
            }

            Button(
                onClick = { satellite = false },
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!satellite)
                        Color(0xFF29442E)
                    else Color(0xFF101C13)
                ),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text("ЗВИЧАЙНА", fontSize = 9.sp)
            }
        }

        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            update = { view ->
                view.invalidate()
            }
        )

        Text(
            if (satellite) "IMAGERY: ESRI"
            else "MAP DATA: OPENSTREETMAP",
            color = Color(0xFF9BFFA3),
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

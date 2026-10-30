
package com.zonepda

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import org.osmdroid.views.overlay.Marker

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

private fun createStalkerIcon(context: android.content.Context): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(96, 120, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(155, 255, 163)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(8, 20, 11)
        style = Paint.Style.FILL
    }

    val drop = Path().apply {
        moveTo(48f, 5f)
        cubicTo(35f, 25f, 10f, 52f, 10f, 73f)
        cubicTo(10f, 96f, 27f, 113f, 48f, 113f)
        cubicTo(69f, 113f, 86f, 96f, 86f, 73f)
        cubicTo(86f, 52f, 61f, 25f, 48f, 5f)
        close()
    }

    canvas.drawPath(drop, fill)
    canvas.drawPath(drop, border)

    val figure = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(155, 255, 163)
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    // Голова
    canvas.drawCircle(43f, 48f, 7f, figure)

    // Тулуб
    canvas.drawLine(43f, 57f, 43f, 79f, figure)

    // Руки
    canvas.drawLine(43f, 62f, 29f, 72f, figure)
    canvas.drawLine(43f, 62f, 59f, 69f, figure)

    // Ноги
    canvas.drawLine(43f, 79f, 32f, 94f, figure)
    canvas.drawLine(43f, 79f, 55f, 94f, figure)

    // Зброя
    canvas.drawLine(54f, 67f, 72f, 58f, figure)
    canvas.drawLine(72f, 58f, 79f, 58f, figure)

    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun ZoneMapPage(
    latitude: Double?,
    longitude: Double?,
    gpsActive: Boolean
) {
    var satellite by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            setTileSource(satelliteSource)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(7.0)
            controller.setCenter(GeoPoint(58.7, 25.5))
        }
    }

    var userMarker by remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(satellite) {
        mapView.setTileSource(
            if (satellite) satelliteSource
            else TileSourceFactory.MAPNIK
        )
        mapView.invalidate()
    }

    // Оновлення маркера за GPS-координатами
    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            val position = GeoPoint(latitude, longitude)

            if (userMarker == null) {
                userMarker = Marker(mapView).apply {
                    setAnchor(
                        Marker.ANCHOR_CENTER,
                        Marker.ANCHOR_BOTTOM
                    )
                    icon = createStalkerIcon(context)
                    title = "МОЄ МІСЦЕ"
                    setPosition(position)
                }
                mapView.overlays.add(userMarker)
            } else {
                userMarker?.setPosition(position)
            }

            mapView.invalidate()
        }
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
                        Color(0xFF29442E) else Color(0xFF101C13)
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
                        Color(0xFF29442E) else Color(0xFF101C13)
                ),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text("ЗВИЧАЙНА", fontSize = 9.sp)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .background(Color(0xFF080E09))
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize().clipToBounds()
            )

            Button(
                onClick = {
                    if (latitude != null && longitude != null) {
                        mapView.controller.animateTo(
                            GeoPoint(latitude, longitude)
                        )
                    }
                },
                enabled = latitude != null && longitude != null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF29442E),
                    contentColor = Color(0xFF9BFFA3)
                ),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text("◎ МОЄ МІСЦЕ", fontSize = 10.sp)
            }

            if (!gpsActive) {
                Text(
                    "ОЧІКУВАННЯ GPS...",
                    color = Color(0xFFD6B66C),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                )
            }
        }

        Text(
            if (satellite) "IMAGERY: ESRI"
            else "MAP DATA: OPENSTREETMAP",
            color = Color(0xFF9BFFA3),
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}


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


private fun createStalkerIcon(
    context: android.content.Context
): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(
        128, 150, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    val green = AndroidColor.rgb(155, 255, 163)
    val dark = AndroidColor.rgb(8, 20, 11)
    val shade = AndroidColor.rgb(38, 75, 43)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dark
        style = Paint.Style.FILL
    }

    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    val detail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    // Зовнішня форма краплі
    val drop = Path().apply {
        moveTo(64f, 5f)
        cubicTo(47f, 29f, 12f, 66f, 12f, 94f)
        cubicTo(12f, 122f, 34f, 143f, 64f, 143f)
        cubicTo(94f, 143f, 116f, 122f, 116f, 94f)
        cubicTo(116f, 66f, 81f, 29f, 64f, 5f)
        close()
    }

    canvas.drawPath(drop, fill)
    canvas.drawPath(drop, outline)

    // Внутрішній контур
    canvas.drawCircle(64f, 88f, 43f, outline)

    // Рюкзак
    canvas.drawRoundRect(
        32f, 76f, 48f, 111f,
        5f, 5f, fill
    )
    canvas.drawRoundRect(
        32f, 76f, 48f, 111f,
        5f, 5f, outline
    )
    canvas.drawLine(36f, 84f, 44f, 84f, detail)
    canvas.drawLine(36f, 98f, 44f, 98f, detail)

    // Капюшон
    val hood = Path().apply {
        moveTo(43f, 64f)
        cubicTo(43f, 38f, 51f, 27f, 64f, 27f)
        cubicTo(79f, 27f, 87f, 40f, 85f, 64f)
        lineTo(78f, 78f)
        lineTo(49f, 78f)
        close()
    }

    canvas.drawPath(hood, fill)
    canvas.drawPath(hood, outline)

    // Маска протигаза
    canvas.drawRoundRect(
        49f, 49f, 79f, 76f,
        8f, 8f, fill
    )
    canvas.drawRoundRect(
        49f, 49f, 79f, 76f,
        8f, 8f, outline
    )

    // Окуляри
    canvas.drawCircle(57f, 59f, 5f, detail)
    canvas.drawCircle(71f, 59f, 5f, detail)

    // Фільтр протигаза
    canvas.drawCircle(64f, 69f, 4f, detail)
    canvas.drawLine(64f, 73f, 64f, 78f, detail)

    // Плечі та тулуб
    val body = Path().apply {
        moveTo(49f, 79f)
        lineTo(37f, 89f)
        lineTo(42f, 116f)
        lineTo(86f, 116f)
        lineTo(91f, 89f)
        lineTo(79f, 79f)
        close()
    }

    canvas.drawPath(body, fill)
    canvas.drawPath(body, outline)

    // Руки
    canvas.drawLine(43f, 88f, 57f, 101f, detail)
    canvas.drawLine(85f, 88f, 74f, 96f, detail)

    // Зброя в руках
    canvas.drawLine(67f, 95f, 91f, 82f, detail)
    canvas.drawLine(91f, 82f, 105f, 82f, detail)
    canvas.drawLine(78f, 91f, 83f, 103f, detail)
    canvas.drawLine(99f, 82f, 99f, 77f, detail)

    // Підсвічування нижньої частини
    canvas.drawLine(49f, 121f, 79f, 121f, detail)

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

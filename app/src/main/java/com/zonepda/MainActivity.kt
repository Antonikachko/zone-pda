
package com.zonepda

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.roundToInt
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun loadNotes(
    prefs: android.content.SharedPreferences
): List<String> {
    return try {
        val array = JSONArray(prefs.getString("notes", "[]"))
        List(array.length()) { index ->
            array.getString(index)
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private val bg = Color(0xFF080E09)
private val panel = Color(0xFF101C13)
private val green = Color(0xFF9BFFA3)
private val muted = Color(0xFF64836A)
private val amber = Color(0xFFD6B66C)
private val line = Color(0xFF29442E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdaApp()
        }
    }
}

private fun compassDirection(degrees: Float): String {
    val directions = listOf(
        "Пн", "ПнСх", "Сх", "ПдСх",
        "Пд", "ПдЗх", "Зх", "ПнЗх"
    )

    val index = ((degrees + 22.5f) / 45f)
        .toInt() % 8

    return directions[index]
}
@Composable
fun PdaApp() {
    val context = LocalContext.current
var hasLocationPermission by remember {
    mutableStateOf(
        context.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    )
}

var userLatitude by remember { mutableStateOf<Double?>(null) }
var userLongitude by remember { mutableStateOf<Double?>(null) }
var speedKmh by remember { mutableStateOf<Float?>(null) }
var altitude by remember { mutableStateOf<Double?>(null) }
var headingDegrees by remember { mutableStateOf<Float?>(null) }

val locationPermissionLauncher =
    rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

LaunchedEffect(Unit) {
    if (!hasLocationPermission) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

DisposableEffect(hasLocationPermission) {
    if (!hasLocationPermission) {
        onDispose { }
    } else {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                userLatitude = location.latitude
                userLongitude = location.longitude
speedKmh = if (location.hasSpeed()) {
    location.speed * 3.6f
} else {
    of
}

altitude = if (location.hasAltitude()) {
    location.altitude
} else {
    null
}
            }
        }

        try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )

            providers.forEach { provider ->
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.getLastKnownLocation(provider)?.let {
                            userLatitude = it.latitude
                            userLongitude = it.longitude
                        }

                        locationManager.requestLocationUpdates(
                            provider,
                            2000L,
                            3f,
                            listener
                        )
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: SecurityException) {
        }

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }
}
// ЕЛЕКТРОННИЙ КОМПАС
DisposableEffect(context) {
    val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val magnetometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    var gravity: FloatArray? = null
    var magnetic: FloatArray? = null

    val listener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->
                    gravity = event.values.clone()

                Sensor.TYPE_MAGNETIC_FIELD ->
                    magnetic = event.values.clone()
            }

            val g = gravity ?: return
            val m = magnetic ?: return

            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            if (
                SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    g,
                    m
                )
            ) {
                SensorManager.getOrientation(
                    rotationMatrix,
                    orientation
                )

                val degrees = (
    Math.toDegrees(
        orientation[0].toDouble()
    ).toFloat() + 360f
) % 360f

val previous = headingDegrees ?: degrees

// Найкоротша різниця між двома напрямками
val difference = (
    (degrees - previous + 540f) % 360f
) - 180f

// Плавне згладжування
headingDegrees = (
    previous + difference * 0.12f + 360f
) % 360f
            }
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int
        ) {}
    }

    if (accelerometer != null && magnetometer != null) {
        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        sensorManager.registerListener(
            listener,
            magnetometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    onDispose {
        sensorManager.unregisterListener(listener)
    }
}

    val prefs = remember {
        context.getSharedPreferences(
            "zone_pda_data",
            Context.MODE_PRIVATE
        )
    }

    var page by remember { mutableStateOf("КПК") }
    var note by remember { mutableStateOf("") }

    var notes by remember {
        mutableStateOf(loadNotes(prefs))
    }

    var currentTime by remember {
        mutableStateOf("--:--:--")
    }

    var currentDate by remember {
        mutableStateOf("--.--.----")
    }

    var battery by remember {
        mutableIntStateOf(-1)
    }

    // Збереження журналу
    LaunchedEffect(notes) {
        val array = JSONArray()
        notes.forEach { array.put(it) }

        prefs.edit()
            .putString("notes", array.toString())
            .apply()
    }

    // Годинник
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()

            currentTime = SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(now)

            currentDate = SimpleDateFormat(
                "dd.MM.yy",
                Locale.getDefault()
            ).format(now)

            delay(1000)
        }
    }

    // Реальний заряд батареї
    DisposableEffect(context) {
        val filter = IntentFilter(
            Intent.ACTION_BATTERY_CHANGED
        )

        fun updateBattery(intent: Intent?) {
            if (intent != null) {
                val level = intent.getIntExtra("level", -1)
                val scale = intent.getIntExtra("scale", -1)

                if (level >= 0 && scale > 0) {
                    battery = (level * 100) / scale
                }
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                updateBattery(intent)
            }
        }

        @Suppress("DEPRECATION")
        val initial = context.registerReceiver(receiver, filter)

        updateBattery(initial)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    MaterialTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(
                    if (page == "КАРТА") Modifier
                    else Modifier.padding(12.dp)
                )
        ) {

            // ЗВИЧАЙНА ВЕРХНЯ ПАНЕЛЬ
            if (page != "КАРТА") {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "ZONE PDA",
                            color = green,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            "PERSONAL FIELD TERMINAL",
                            color = muted,
                            fontSize = 9.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "● SYSTEM ONLINE",
                            color = green,
                            fontSize = 10.sp
                        )

                        Text(
                            "BUILD 0.3",
                            color = muted,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = line)
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            currentTime,
                            color = green,
                            fontSize = 23.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            currentDate,
                            color = muted,
                            fontSize = 10.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "ЗОНА: НЕВІДОМО",
                            color = amber,
                            fontSize = 10.sp
                        )

                        Text(
                            "GPS: STANDBY",
                            color = muted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            panel,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            line,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "ЗВ'ЯЗОК: ONLINE",
                        color = green,
                        fontSize = 9.sp
                    )

                    Text(
                        "СКАНЕР: READY",
                        color = green,
                        fontSize = 9.sp
                    )

                    Text(
                        "АКУМ: ${if (battery >= 0) "$battery%" else "--"}",
                        color = if (battery in 0..20) amber else green,
                        fontSize = 9.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

            } else {

                // КОМПАКТНА ПАНЕЛЬ КАРТИ
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(panel)
                        .border(
                            1.dp,
                            line,
                            RoundedCornerShape(2.dp)
                        )
                        .padding(horizontal = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currentTime,
                        color = green,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        currentDate,
                        color = green,
                        fontSize = 9.sp
                    )

                    Text(
                        "GPS: ${if (userLatitude != null) "ACTIVE" else "SEARCH"}",
                        color = muted,
                        fontSize = 9.sp
                    )

                    Text(
                        "NET: ONLINE",
                        color = green,
                        fontSize = 9.sp
                    )

                    Text(
                        "АКБ: ${if (battery >= 0) "$battery%" else "--"}",
                        color = if (battery in 0..20) amber else green,
                        fontSize = 9.sp
                    )
                }
Spacer(Modifier.height(4.dp))

Column(
    Modifier
        .fillMaxWidth()
        .background(panel)
        .border(1.dp, line, RoundedCornerShape(2.dp))
        .padding(7.dp)
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("КОМПАС", color = muted, fontSize = 8.sp)
            Text(
                if (headingDegrees != null)
                    "${compassDirection(headingDegrees!!)} ${headingDegrees!!.roundToInt()}°"
                else "--",
                color = green,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column {
            Text("ШВИДКІСТЬ", color = muted, fontSize = 8.sp)
            Text(
                speedKmh?.let { "%.1f км/год".format(Locale.US, it) } ?: "--",
                color = green,
                fontSize = 13.sp
            )
        }

        Column {
            Text("ВИСОТА", color = muted, fontSize = 8.sp)
            Text(
                altitude?.let { "${it.roundToInt()} м" } ?: "--",
                color = green,
                fontSize = 13.sp
            )
        }
    }

    Spacer(Modifier.height(5.dp))

    HorizontalDivider(color = line)

    Spacer(Modifier.height(5.dp))

    Text(
        if (userLatitude != null && userLongitude != null) {
            "LAT: %.5f    LON: %.5f".format(
                Locale.US,
                userLatitude!!,
                userLongitude!!
            )
        } else {
            "LAT: --    LON: --"
        },
        color = green,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace
    )
}
            }

            // ОСНОВНИЙ ВМІСТ
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (page) {

                    "КПК" -> HomePage(
                        currentTime,
                        currentDate,
                        notes.size,
                        battery
                    )

                    "КАРТА" -> ZoneMapPage(
latitude = userLatitude,
    longitude = userLongitude,
    gpsActive = userLatitude != null && userLongitude != null
)

                    "СКАНЕР" -> RadarPage()

                    "ЖУРНАЛ" -> Column {
                        Text(
                            "ПОЛЬОВИЙ ЖУРНАЛ",
                            color = green,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "PERSONAL NOTES DATABASE",
                            color = muted,
                            fontSize = 9.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        BasicTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    panel,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    line,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = green,
                                fontSize = 14.sp
                            ),
                            decorationBox = { inner ->
                                if (note.isEmpty()) {
                                    Text(
                                        "Введи польову нотатку...",
                                        color = muted
                                    )
                                }
                                inner()
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (note.isNotBlank()) {
                                    notes = notes + note.trim()
                                    note = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF29442E),
                                contentColor = green
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("ЗБЕРЕГТИ ЗАПИС")
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "ЗАПИСІВ У БАЗІ: ${notes.size}",
                            color = muted,
                            fontSize = 10.sp
                        )

                        LazyColumn {
                            items(notes) { entry ->
                                Text(
                                    "› $entry",
                                    color = green,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(panel)
                                        .padding(9.dp)
                                )
                            }
                        }
                    }

                    "ЗАВДАННЯ" -> InfoPage(
                        "АКТИВНІ ЗАВДАННЯ",
                        "Немає активних завдань.\n\nОчікування нових повідомлень."
                    )

                    "ІНВЕНТАР" -> InfoPage(
                        "ОСОБИСТИЙ ІНВЕНТАР",
                        "Список спорядження поки порожній."
                    )
                }
            }

            // НИЖНЄ МЕНЮ
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = line)
            Spacer(Modifier.height(8.dp))

            val tabs = listOf(
                "КПК",
                "КАРТА",
                "СКАНЕР",
                "ЖУРНАЛ",
                "ЗАВДАННЯ",
                "ІНВЕНТАР"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                tabs.chunked(3).forEach { tabRow ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        tabRow.forEach { tab ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .background(
                                        if (page == tab) Color(0xFF1B3520)
                                        else panel,
                                        RoundedCornerShape(3.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (page == tab) green else line,
                                        RoundedCornerShape(3.dp)
                                    )
                                    .clickable {
                                        page = tab
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    tab,
                                    color = if (page == tab) green else muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ПІДПИС ПРИХОВАНИЙ НА КАРТІ
            if (page != "КАРТА") {
                Spacer(Modifier.height(5.dp))

                Text(
                    "ZONE PDA // FIELD EQUIPMENT DIVISION",
                    color = muted,
                    fontSize = 8.sp,
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )
            }
        }
    }
}

@Composable
fun HomePage(
    time: String,
    date: String,
    noteCount: Int,
    battery: Int
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            InfoPage(
                "СТАН СИСТЕМИ",
                "Термінал працює нормально.\nЗв'язок: стабільний\nСистема: активна"
            )
        }

        item {
            InfoPage(
                "ПОЛЬОВИЙ СТАТУС",
                "Режим: дослідження\nЛокація: невідома\nСканер: готовий"
            )
        }

        item {
            InfoPage(
                "ОСТАННІЙ ЗАПИС",
                if (noteCount == 0) "Записів у журналі немає."
                else "Кількість записів: $noteCount"
            )
        }

        item {
            InfoPage(
                "АКУМУЛЯТОР",
                if (battery >= 0) "Заряд пристрою: $battery%"
                else "Дані батареї недоступні."
            )
        }

        item {
            InfoPage(
                "СИСТЕМНА ІНФОРМАЦІЯ",
                "Час: $time\nДата: $date\nВерсія термінала: 0.3"
            )
        }

        item {
            Text(
                "WARNING: UNKNOWN TERRITORY",
                color = amber,
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(panel)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun InfoPage(title: String, message: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(panel, RoundedCornerShape(4.dp))
            .border(1.dp, line, RoundedCornerShape(4.dp))
            .padding(13.dp)
    ) {
        Text(
            "■ $title",
            color = green,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(9.dp))

        Text(
            message,
            color = muted,
            fontSize = 12.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
fun RadarPage() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "АНАЛІЗ ОТОЧЕННЯ",
            color = green,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "PROXIMITY DETECTION SYSTEM",
            color = muted,
            fontSize = 9.sp
        )

        Spacer(Modifier.height(12.dp))

        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val r = minOf(size.width, size.height) * 0.43f
            val c = Offset(size.width / 2, size.height / 2)

            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                drawCircle(
                    color = muted,
                    radius = r * fraction,
                    center = c,
                    style = Stroke(1.dp.toPx())
                )
            }

            drawLine(
                muted,
                Offset(c.x - r, c.y),
                Offset(c.x + r, c.y),
                1.dp.toPx()
            )

            drawLine(
                muted,
                Offset(c.x, c.y - r),
                Offset(c.x, c.y + r),
                1.dp.toPx()
            )

            drawLine(
                green.copy(alpha = 0.6f),
                c,
                Offset(c.x + r * 0.8f, c.y - r * 0.3f),
                2.dp.toPx()
            )

            drawCircle(green, 4.dp.toPx(), c)

            drawCircle(
                amber,
                5.dp.toPx(),
                Offset(c.x + r * 0.45f, c.y - r * 0.25f)
            )

            drawCircle(
                green,
                3.dp.toPx(),
                Offset(c.x - r * 0.35f, c.y + r * 0.4f)
            )
        }

        Text(
            "СКАНУВАННЯ: ДЕМО",
            color = amber,
            fontSize = 11.sp
        )

        Text(
            "ВИЯВЛЕНІ МІТКИ НЕ Є РЕАЛЬНИМИ ДАНИМИ",
            color = muted,
            fontSize = 8.sp
        )
    }
}

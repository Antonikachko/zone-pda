
package com.zonepda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val bg = Color(0xFF07100B)
private val panel = Color(0xFF142019)
private val green = Color(0xFF91FF9B)
private val muted = Color(0xFF63816A)
private val amber = Color(0xFFD7B66C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PdaApp() }
    }
}

@Composable
fun PdaApp() {
    var page by remember { mutableStateOf("КПК") }
    var note by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(listOf<String>()) }

    MaterialTheme {
        Column(
            Modifier.fillMaxSize()
                .background(bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ZONE PDA", color = green,
                        fontSize = 23.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace)
                    Text("FIELD TERMINAL // 0.1",
                        color = muted, fontSize = 10.sp)
                }
                Text("● ONLINE", color = green, fontSize = 11.sp)
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth()
                    .background(panel, RoundedCornerShape(5.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ЗОНА: УКРАЇНА", color = amber, fontSize = 10.sp)
                Text("GPS: STANDBY", color = muted, fontSize = 10.sp)
            }

            Spacer(Modifier.height(14.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    "КПК" -> HomePage()
                    "КАРТА" -> InfoPage("КАРТА",
                        "Модуль GPS буде підключений пізніше.")
                    "СКАНЕР" -> RadarPage()
                    "ЖУРНАЛ" -> Column {
                        Text("ПОЛЬОВИЙ ЖУРНАЛ",
                            color = green, fontSize = 17.sp)
                        Spacer(Modifier.height(12.dp))
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth()
                                .background(panel, RoundedCornerShape(5.dp))
                                .padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = green, fontSize = 14.sp
                            ),
                            decorationBox = { inner ->
                                if (note.isEmpty()) {
                                    Text("Введи нотатку...",
                                        color = muted)
                                }
                                inner()
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            if (note.isNotBlank()) {
                                notes = notes + note
                                note = ""
                            }
                        }) {
                            Text("ЗБЕРЕГТИ")
                        }
                        Spacer(Modifier.height(12.dp))
                        LazyColumn {
                            items(notes.size) { i ->
                                Text("› ${notes[i]}",
                                    color = green,
                                    modifier = Modifier.padding(6.dp))
                            }
                        }
                    }
                    "ЗАВДАННЯ" -> InfoPage("ЗАВДАННЯ",
                        "Активних завдань поки немає.")
                    "ІНВЕНТАР" -> InfoPage("ІНВЕНТАР",
                        "Твій польовий інвентар.")
                }
            }

            Spacer(Modifier.height(10.dp))

            val tabs = listOf(
                "КПК", "КАРТА", "СКАНЕР",
                "ЖУРНАЛ", "ЗАВДАННЯ", "ІНВЕНТАР"
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                tabs.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { tab ->
                            Box(
                                Modifier.weight(1f)
                                    .background(
                                        if (page == tab) green.copy(alpha = 0.15f)
                                        else panel,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(1.dp, muted,
                                        RoundedCornerShape(4.dp))
                                    .clickable { page = tab }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(tab,
                                    color = if (page == tab) green else muted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomePage() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoPage("СТАН СИСТЕМИ",
            "Термінал працює нормально.\n\nЗв'язок: стабільний\nGPS: очікування")
        InfoPage("ОСТАННІЙ ЗАПИС",
            "Поки що записів немає.")
        InfoPage("ПОЛЬОВИЙ СТАТУС",
            "Режим: дослідження\nЛокація: невідома\nСканер: готовий")
    }
}

@Composable
fun InfoPage(title: String, message: String) {
    Column(
        Modifier.fillMaxWidth()
            .background(panel, RoundedCornerShape(5.dp))
            .border(1.dp, muted, RoundedCornerShape(5.dp))
            .padding(14.dp)
    ) {
        Text(title, color = green,
            fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = muted, fontSize = 13.sp)
    }
}

@Composable
fun RadarPage() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("АНАЛІЗ ОТОЧЕННЯ",
            color = green, fontSize = 15.sp)
        Spacer(Modifier.height(15.dp))
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val r = minOf(size.width, size.height) * 0.43f
            val c = Offset(size.width / 2, size.height / 2)

            listOf(0.33f, 0.66f, 1f).forEach {
                drawCircle(muted, r * it, c,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }

            drawLine(muted, Offset(c.x-r,c.y),
                Offset(c.x+r,c.y), 1.dp.toPx())
            drawLine(muted, Offset(c.x,c.y-r),
                Offset(c.x,c.y+r), 1.dp.toPx())

            drawCircle(green, 4.dp.toPx(), c)
            drawCircle(amber, 5.dp.toPx(),
                Offset(c.x + r * 0.45f, c.y - r * 0.25f))
        }
        Text("СКАНУВАННЯ: ДЕМО",
            color = amber, fontSize = 11.sp)
    }
}

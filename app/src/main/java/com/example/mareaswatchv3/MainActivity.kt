package com.example.mareaswatchv3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mareaswatchv3.data.MarineWeatherData
import com.example.mareaswatchv3.data.TidePoint
import com.example.mareaswatchv3.data.TideTrend
import com.example.mareaswatchv3.data.TideType
import com.example.mareaswatchv3.ui.MainViewModel
import com.example.mareaswatchv3.ui.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MareasV3App() }
    }
}

private val Navy = Color(0xFF020617)
private val Panel = Color(0xFF0F172A)
private val Cyan = Color(0xFF22D3EE)
private val Amber = Color(0xFFFBBF24)
private val Muted = Color(0xFF94A3B8)

@Composable
fun MareasV3App(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isWatch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
            when (val current = state) {
                UiState.WaitingGps -> WaitingScreen("Buscando GPS...", "Obteniendo ubicación actual")
                UiState.LoadingData -> WaitingScreen("Cargando datos...", "Meteorología y oleaje")
                is UiState.Error -> ErrorScreen(current.message, viewModel::refresh)
                is UiState.Ready -> if (isWatch) {
                    Dashboard(current.data, compact = true, viewModel::refresh)
                } else {
                    Dashboard(current.data, compact = false, viewModel::refresh)
                }
            }
        }
    }
}

@Composable
private fun WaitingScreen(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Cyan)
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ErrorScreen(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No se pudieron cargar los datos", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = retry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun Dashboard(data: MarineWeatherData, compact: Boolean, refresh: () -> Unit) {
    val horizontalPadding = if (compact) 14.dp else 28.dp
    val maxGraphHeight = if (compact) 130.dp else 240.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = if (compact) 18.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mareas Watch V3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (compact) 13.sp else 22.sp)
        Text(data.locationName, color = Cyan, fontSize = if (compact) 11.sp else 16.sp, textAlign = TextAlign.Center)
        Text("${"%.5f".format(data.latitude)}, ${"%.5f".format(data.longitude)}", color = Muted, fontSize = if (compact) 8.sp else 11.sp)
        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${"%.2f".format(data.tide.currentLevel)} m", color = Color.White, fontSize = if (compact) 28.sp else 44.sp, fontWeight = FontWeight.Black)
            Text(trendSymbol(data.tide.trend), color = trendColor(data.tide.trend), fontSize = if (compact) 24.sp else 36.sp)
        }
        Text("Marea estimada · ${trendText(data.tide.trend)}", color = Amber, fontSize = if (compact) 9.sp else 13.sp)

        Spacer(Modifier.height(if (compact) 5.dp else 12.dp))
        TideGraph(
            points = data.tide.curve,
            currentHour = data.tide.currentHour,
            modifier = Modifier.fillMaxWidth().height(maxGraphHeight)
        )
        GraphAxis(compact)

        data.tide.nextEvent?.let { event ->
            val eventName = if (event.type == TideType.HIGH) "Próxima pleamar estimada" else "Próxima bajamar estimada"
            Text(
                "$eventName: ${event.time} · ${"%.2f".format(event.height)} m",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 9.sp else 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
        if (compact) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Metric("TEMP", "${"%.0f".format(data.temperature)}°")
                Metric("VIENTO", "${"%.0f".format(data.windSpeed)} km/h")
                Metric("OLA", "${"%.1f".format(data.waveHeight)} m")
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailCard("Temperatura", "${"%.1f".format(data.temperature)} °C", "Sensación ${"%.1f".format(data.apparentTemperature)} °C", Modifier.weight(1f))
                DetailCard("Viento", "${"%.0f".format(data.windSpeed)} km/h", "Rachas ${"%.0f".format(data.windGusts)} km/h", Modifier.weight(1f))
                DetailCard("Oleaje", "${"%.1f".format(data.waveHeight)} m", "Periodo ${"%.0f".format(data.wavePeriod)} s", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailCard("UV", "${"%.1f".format(data.uvIndex)}", "Humedad ${data.humidity}%", Modifier.weight(1f))
                DetailCard("Presión", "${"%.0f".format(data.pressure)} hPa", "Coef. ${data.tide.coefficient}", Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("La curva de marea es orientativa y no debe usarse para navegación.", color = Color(0xFFFF8A80), fontSize = if (compact) 8.sp else 11.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Button(onClick = refresh) { Text("Actualizar GPS") }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 7.sp)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Cyan, fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GraphAxis(compact: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("00", "06", "12", "18", "24").forEach {
            Text(it, color = Muted, fontSize = if (compact) 7.sp else 10.sp)
        }
    }
}

@Composable
private fun TideGraph(points: List<TidePoint>, currentHour: Double, modifier: Modifier) {
    Canvas(modifier = modifier.background(Panel, RoundedCornerShape(18.dp)).padding(10.dp)) {
        if (points.size < 2) return@Canvas
        val minimum = points.minOf { it.height }
        val maximum = points.maxOf { it.height }
        val range = (maximum - minimum).coerceAtLeast(0.1)
        val left = 4.dp.toPx()
        val right = size.width - 4.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()

        fun x(hour: Double): Float = left + (hour / 24.0).toFloat() * (right - left)
        fun y(height: Double): Float = bottom - (((height - minimum) / range).toFloat() * (bottom - top))

        listOf(0.0, 6.0, 12.0, 18.0, 24.0).forEach { hour ->
            drawLine(Color(0xFF26364A), Offset(x(hour), top), Offset(x(hour), bottom), 1.dp.toPx())
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            val px = x(point.hour.toDouble())
            val py = y(point.height)
            if (index == 0) {
                path.moveTo(px, py)
            } else {
                val previous = points[index - 1]
                val previousX = x(previous.hour.toDouble())
                val previousY = y(previous.height)
                val middleX = (previousX + px) / 2
                path.cubicTo(middleX, previousY, middleX, py, px, py)
            }
        }
        drawPath(path, Cyan, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        val hour = currentHour.coerceIn(0.0, 24.0)
        val lowerIndex = hour.toInt().coerceIn(0, 23)
        val fraction = hour - lowerIndex
        val nowHeight = points[lowerIndex].height + (points[lowerIndex + 1].height - points[lowerIndex].height) * fraction
        val nowX = x(hour)
        val nowY = y(nowHeight)
        drawLine(Color(0x99FFFFFF), Offset(nowX, top), Offset(nowX, bottom), 1.dp.toPx())
        drawCircle(Color.White, 6.dp.toPx(), Offset(nowX, nowY))
        drawCircle(Cyan, 3.5.dp.toPx(), Offset(nowX, nowY))
    }
}

private fun trendText(trend: TideTrend) = when (trend) {
    TideTrend.RISING -> "subiendo"
    TideTrend.FALLING -> "bajando"
    TideTrend.STATIONARY -> "estable"
}

private fun trendSymbol(trend: TideTrend) = when (trend) {
    TideTrend.RISING -> "↑"
    TideTrend.FALLING -> "↓"
    TideTrend.STATIONARY -> "→"
}

private fun trendColor(trend: TideTrend) = when (trend) {
    TideTrend.RISING -> Color(0xFF34D399)
    TideTrend.FALLING -> Amber
    TideTrend.STATIONARY -> Muted
}

package com.example.mareaswatchv3.data

enum class TideType { HIGH, LOW }
enum class TideTrend { RISING, FALLING, STATIONARY }

data class TideEvent(
    val time: String,
    val type: TideType,
    val height: Double
)

data class TidePoint(
    val hour: Int,
    val height: Double
)

data class TideInfo(
    val currentLevel: Double,
    val trend: TideTrend,
    val nextEvent: TideEvent?,
    val curve: List<TidePoint>,
    val currentHour: Double,
    val coefficient: Int
)

data class MarineWeatherData(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val humidity: Int,
    val pressure: Double,
    val uvIndex: Double,
    val windSpeed: Double,
    val windDirection: Double,
    val windGusts: Double,
    val waveHeight: Double,
    val wavePeriod: Double,
    val waveDirection: Double,
    val swellHeight: Double,
    val tide: TideInfo
)

data class WeatherResponse(val current: WeatherCurrent?)
data class WeatherCurrent(
    val temperature_2m: Double?,
    val apparent_temperature: Double?,
    val relative_humidity_2m: Int?,
    val surface_pressure: Double?,
    val uv_index: Double?,
    val wind_speed_10m: Double?,
    val wind_direction_10m: Double?,
    val wind_gusts_10m: Double?
)

data class MarineResponse(val current: MarineCurrent?)
data class MarineCurrent(
    val wave_height: Double?,
    val wave_period: Double?,
    val wave_direction: Double?,
    val swell_wave_height: Double?
)

data class NominatimResponse(val address: NominatimAddress?)
data class NominatimAddress(
    val amenity: String?,
    val suburb: String?,
    val town: String?,
    val city: String?,
    val municipality: String?,
    val county: String?,
    val state: String?,
    val country: String?
)

package com.example.mareaswatchv3.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MarineRepository {
    suspend fun load(latitude: Double, longitude: Double): MarineWeatherData = coroutineScope {
        val weatherDeferred = async { ApiProvider.weather.current(latitude, longitude) }
        val marineDeferred = async { ApiProvider.marine.current(latitude, longitude) }
        val geocodeDeferred = async {
            runCatching {
                ApiProvider.geocode.reverse(latitude = latitude, longitude = longitude)
            }.getOrNull()
        }

        val weather = weatherDeferred.await().current
        val marine = marineDeferred.await().current
        val address = geocodeDeferred.await()?.address

        val place = listOfNotNull(
            address?.amenity,
            address?.suburb,
            address?.town,
            address?.city,
            address?.municipality,
            address?.county
        ).firstOrNull() ?: "Costa local"
        val region = address?.state ?: address?.country
        val locationName = if (region.isNullOrBlank()) place else "$place, $region"

        MarineWeatherData(
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            temperature = weather?.temperature_2m ?: 0.0,
            apparentTemperature = weather?.apparent_temperature ?: 0.0,
            humidity = weather?.relative_humidity_2m ?: 0,
            pressure = weather?.surface_pressure ?: 0.0,
            uvIndex = weather?.uv_index ?: 0.0,
            windSpeed = weather?.wind_speed_10m ?: 0.0,
            windDirection = weather?.wind_direction_10m ?: 0.0,
            windGusts = weather?.wind_gusts_10m ?: 0.0,
            waveHeight = marine?.wave_height ?: 0.0,
            wavePeriod = marine?.wave_period ?: 0.0,
            waveDirection = marine?.wave_direction ?: 0.0,
            swellHeight = marine?.swell_wave_height ?: 0.0,
            tide = TideCalculator.calculate(longitude)
        )
    }
}

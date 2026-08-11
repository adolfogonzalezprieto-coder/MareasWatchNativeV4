package com.example.mareaswatchv3.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,apparent_temperature,relative_humidity_2m,surface_pressure,uv_index,wind_speed_10m,wind_direction_10m,wind_gusts_10m",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}

interface MarineApi {
    @GET("v1/marine")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "wave_height,wave_period,wave_direction,swell_wave_height",
        @Query("timezone") timezone: String = "auto"
    ): MarineResponse
}

interface GeocodeApi {
    @GET("reverse")
    suspend fun reverse(
        @Query("format") format: String = "json",
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("zoom") zoom: Int = 12
    ): NominatimResponse
}

object ApiProvider {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "MareasWatchNativeV3Dual/3.0")
                .build()
            chain.proceed(request)
        }
        .build()

    private fun retrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weather: WeatherApi = retrofit("https://api.open-meteo.com/").create(WeatherApi::class.java)
    val marine: MarineApi = retrofit("https://marine-api.open-meteo.com/").create(MarineApi::class.java)
    val geocode: GeocodeApi = retrofit("https://nominatim.openstreetmap.org/").create(GeocodeApi::class.java)
}

package com.example.mareaswatchv3.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mareaswatchv3.data.MarineRepository
import com.example.mareaswatchv3.data.MarineWeatherData
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface UiState {
    data object WaitingGps : UiState
    data object LoadingData : UiState
    data class Ready(val data: MarineWeatherData) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MarineRepository()
    private val locationClient = LocationServices.getFusedLocationProviderClient(application)
    private val _state = MutableStateFlow<UiState>(UiState.WaitingGps)
    val state: StateFlow<UiState> = _state

    fun refresh() = viewModelScope.launch {
        val permissionGranted = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            _state.value = UiState.Error("Permiso de ubicación no concedido")
            return@launch
        }

        _state.value = UiState.WaitingGps
        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(25_000L)
                .setMaxUpdateAgeMillis(0L)
                .build()
            val location = locationClient.getCurrentLocation(
                request,
                CancellationTokenSource().token
            ).await()

            if (location == null) {
                _state.value = UiState.Error("GPS no disponible. Activa la ubicación e inténtalo al aire libre.")
                return@launch
            }

            _state.value = UiState.LoadingData
            _state.value = UiState.Ready(repository.load(location.latitude, location.longitude))
        } catch (error: Exception) {
            _state.value = UiState.Error(error.message ?: "No se pudieron cargar los datos")
        }
    }
}

package com.minimo.launcher.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Approximate (city-level) location for the weather widget. */
@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /** Current location (up to 10 s), falling back to the last known one; null if unavailable. */
    @SuppressLint("MissingPermission")
    suspend fun getLocation(): Location? {
        val manager = locationManager
        if (!hasPermission() || manager == null) return null
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (provider in providers) {
                val location = withTimeoutOrNull(10_000) { currentLocation(provider) }
                if (location != null) return location
            }
        }
        return lastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationSignal()
            continuation.invokeOnCancellation { cancellation.cancel() }
            try {
                locationManager!!.getCurrentLocation(
                    provider, cancellation, ContextCompat.getMainExecutor(context)
                ) { location -> if (continuation.isActive) continuation.resume(location) }
            } catch (exception: Exception) {
                Timber.e(exception)
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): Location? = try {
        locationManager?.getProviders(true).orEmpty()
            .mapNotNull { locationManager?.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
    } catch (exception: Exception) {
        Timber.e(exception)
        null
    }

    /** City name for coordinates using the system geocoder; null when it is unavailable. */
    suspend fun cityName(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(10_000) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        })
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            }
            address?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        } catch (exception: Exception) {
            Timber.e(exception)
            null
        }
    }
}

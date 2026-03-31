package com.example.saferideai.feature.ride.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.saferideai.core.permissions.hasPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class RideLocationTracker(
    private val context: Context,
    private val onLocationUpdated: (Location) -> Unit,
    private val onPermissionMissing: () -> Unit
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(onLocationUpdated)
        }
    }

    fun start() {
        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            onPermissionMissing()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).setMinUpdateIntervalMillis(2000L)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stop() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

package com.example.flutter_gps

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.flutter_gps.model.Location
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
import io.flutter.plugin.common.MethodChannel


class GpsHandler(private val activity: Activity) {

    companion object {
        const val REQUEST_GPS_PERIMSSION = 1
        const val REQUEST_CHECK_SETTINGS = 2
    }

    private var permissionGps: Boolean = false
    private var locationSettingsResponseTask: Task<LocationSettingsResponse>? = null

    lateinit var onSuccessGetLocation: (Double, Double) -> Unit
    lateinit var onFailureGetLocation: () -> Unit

    fun requestPermissionGps(requestCode: Int) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            permissionGps = ActivityCompat.checkSelfPermission(
                    activity.applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    activity.applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGps) {
                ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                        requestCode
                )
            } else {
                settingsrequest()
            }
        } else {
            permissionGps = true
            settingsrequest()
        }
    }

    fun handlePermissionResult(grantResults: IntArray) {
        permissionGps = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
        if (permissionGps) {
            settingsrequest()
        } else {
            onFailureGetLocation()
        }
    }

    fun onMethodGetCurrentLocation(result: MethodChannel.Result, requestCode: Int){
        initGetCurrentLocationResult(result)
        requestPermissionGps(requestCode)
    }

    fun initGetCurrentLocationResult(result: MethodChannel.Result) {
        onSuccessGetLocation = { lat, long -> result.success(Location(lat, long).toString()) }
        onFailureGetLocation = { result.error("ERROR", "Failed to get location", null) }
    }

    fun getCurrentLocation() {
        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        mFusedLocationClient.lastLocation.apply {
            addOnSuccessListener {
                onSuccessGetLocation(it.latitude, it.longitude)
            }
            addOnFailureListener {
                onFailureGetLocation()
            }
        }
    }

    fun settingsrequest() {
        if (locationSettingsResponseTask != null) {
            getCurrentLocation()
            return
        }

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 30 * 1000
        locationRequest.fastestInterval = 5 * 1000
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true) //this is the key ingredient

        locationSettingsResponseTask = LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build()).apply {
            addOnSuccessListener {
                if (it.locationSettingsStates.isLocationPresent) {
                    getCurrentLocation()
                } else {
                    onFailureGetLocation()
                }
            }
            addOnFailureListener {
                if (it is ResolvableApiException) {
                    try {
                        it.startResolutionForResult(activity,
                                REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        onFailureGetLocation()
                    }

                } else {
                    onFailureGetLocation()
                }
            }
        }
    }


}
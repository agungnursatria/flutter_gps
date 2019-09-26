package com.example.flutter_gps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.flutter_gps.helper.Helper
import com.example.flutter_gps.model.Location
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import io.flutter.plugin.common.MethodChannel


class GpsHandler(private val activity: Activity) {

    companion object {
        const val REQUEST_GPS_PERIMSSION = 1
        const val REQUEST_CHECK_SETTINGS = 2
    }

    private var permissionGps: Boolean = false

    lateinit var onSuccessGetLocation: (Double, Double) -> Unit
    lateinit var onFailureGetLocation: (String) -> Unit

    lateinit var locService: LocationUpdateUsingLocationService

    fun onMethodGetCurrentLocation(result: MethodChannel.Result, requestCode: Int) {
        initGetCurrentLocationResult(result)
        requestPermissionGps(requestCode)
    }

    fun handlePermissionResult(grantResults: IntArray) {
        permissionGps = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
        if (permissionGps) {
            getLocation()
        } else {
            onFailureGetLocation("Gps permission is not granted")
        }
    }

    private fun requestPermissionGps(requestCode: Int) {
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
                getLocation()
            }
        } else {
            permissionGps = true
            getLocation()
        }
    }

    private fun initGetCurrentLocationResult(result: MethodChannel.Result) {
        onSuccessGetLocation = { lat, long -> result.success(Location(lat, long).toString()) }
        onFailureGetLocation = { errorMessage -> result.error("ERROR", errorMessage, null) }
    }

    fun getLocation(){
        if (Helper.isGooglePlayServicesAvailable(activity)){
            locService = LocationUpdateUsingLocationService(activity, onSuccessGetLocation, onFailureGetLocation)
            locService.initGetLocation()
        } else {
            onFailureGetLocation("Has not been implemented")
        }
    }
}

class LocationUpdateUsingLocationService(private val activity: Activity, private val onSuccessGetLocation: (Double, Double) -> Unit, private val onFailureGetLocation: (String) -> Unit) {
    private var mFusedLocationClient: FusedLocationProviderClient
    private var locationRequest: LocationRequest
    private var builder: LocationSettingsRequest.Builder
    private var mLocationCallback: LocationCallback

    private var locationSettingsResponseTask: Task<LocationSettingsResponse>? = null

    init {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 30 * 1000
        locationRequest.fastestInterval = 5 * 1000
        builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true) //this is the key ingredient


        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    return
                }

                for (location in locationResult.locations) {
                    if (location != null) {
                        onSuccessGetLocation(location.latitude, location.longitude)
                        break
                    }
                }

                stopGetLocation()
            }
        }
    }

    fun initGetLocation() {
        // Initialize get location, check if gps is enabled, if true then start get location
        locationSettingsResponseTask = LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build()).apply {
            this.addOnSuccessListener {
                startGetLocation()
            }
            this.addOnFailureListener {
                if (it is ResolvableApiException) {
                    try {
                        it.startResolutionForResult(activity,
                                GpsHandler.REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        onFailureGetLocation(sendEx.localizedMessage)
                    }

                } else {
                    activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    Toast.makeText(activity, activity.getString(R.string.failed_turn_on_request_gps), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startGetLocation(){
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener {
                    var looper = Looper.myLooper()
                    if (looper == null) {
                        looper = Looper.getMainLooper()
                    }

                    mFusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            mLocationCallback,
                            looper)
                }
    }

    private fun stopGetLocation(){
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }
}
package com.example.flutter_gps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.flutter_gps.helper.Helper
import com.example.flutter_gps.model.Loc
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.util.Strings
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
    lateinit var locManager: LocationUpdateUsingLocationManager

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
        onSuccessGetLocation = { lat, long -> result.success(Loc(lat, long).toString()) }
        onFailureGetLocation = { errorMessage -> result.error("ERROR", errorMessage, null) }
    }

    fun getLocation() {
        if (Helper.isGooglePlayServicesAvailable(activity)) {
            locService = LocationUpdateUsingLocationService(activity, onSuccessGetLocation, onFailureGetLocation)
            locService.initGetLocation()
        } else {
            locManager = LocationUpdateUsingLocationManager(activity, permissionGps, onSuccessGetLocation, onFailureGetLocation)
            locManager.startGetLocation()
        }
    }
}

class LocationUpdateUsingLocationManager(private val activity: Activity, private val permissionGps: Boolean, private val onSuccessGetLocation: (Double, Double) -> Unit, private val onFailureGetLocation: (String) -> Unit) : LocationListener {

    private var mActiveProvider: String? = null
    private var currentLocation: Location? = null

    lateinit var locationManager: LocationManager

    @SuppressLint("MissingPermission")
    fun startGetLocation() {
        locationManager = activity.getSystemService(Activity.LOCATION_SERVICE) as LocationManager

        // Make sure we remove existing listeners before we register a new one
        locationManager.removeUpdates(this)

        // Try to get the best possible location provider for the requested accuracy
        mActiveProvider = getBestProvider(locationManager)

        if (mActiveProvider.isNullOrEmpty()) {
            onFailureGetLocation(activity.getString(R.string.warning_inadequate_location))
            return
        }

        if (!permissionGps) {
            onFailureGetLocation(activity.getString(R.string.warning_request_permission_gps))
            return
        }

        currentLocation = locationManager.getLastKnownLocation(mActiveProvider)

        // If we are listening to multiple location updates we can go ahead
        // and report back the last known location (if we have one).
        currentLocation?.let {
            onSuccessGetLocation(it.latitude, it.longitude)
            return
        }


        var looper = Looper.myLooper()
        if (looper == null) {
            looper = Looper.getMainLooper()
        }

        locationManager.requestLocationUpdates(
                mActiveProvider,
                0,
                0f,
                this,
                looper)
    }

    fun stopGetLocation() {
        locationManager.removeUpdates(this)
    }

    private fun getBestProvider(locationManager: LocationManager): String? {
        val criteria = Criteria()

        criteria.isBearingRequired = false
        criteria.isAltitudeRequired = false
        criteria.isSpeedRequired = false
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.horizontalAccuracy = Criteria.ACCURACY_HIGH
        criteria.powerRequirement = Criteria.POWER_HIGH

        var provider = locationManager.getBestProvider(criteria, true)

        if (Strings.isEmptyOrWhitespace(provider)) {
            val providers = locationManager.getProviders(true)
            if (providers != null && providers.size > 0)
                provider = providers[0]
        }

        return provider
    }

    override fun onLocationChanged(location: Location?) {
        currentLocation = location
        currentLocation?.let {
            onSuccessGetLocation(it.latitude, it.longitude)
            stopGetLocation()
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        if (status == LocationProvider.AVAILABLE) {
            onProviderEnabled(provider)
        } else if (status == LocationProvider.OUT_OF_SERVICE) {
            onProviderDisabled(provider)
        }
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
        if (provider == mActiveProvider) {
            onFailureGetLocation("Error updating location: The active location provider was disabled. Check if the location services are enabled in the device settings.")
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
                    Toast.makeText(activity, activity.getString(R.string.warning_register_failed_turn_on_request_gps), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startGetLocation() {
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

    private fun stopGetLocation() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }
}
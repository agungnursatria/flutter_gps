package com.example.flutter_gps

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity : FlutterActivity() {
    private var gpsHandler: GpsHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        MethodChannel(flutterView, "gps").setMethodCallHandler { methodCall, result ->
            when (methodCall.method) {
                "turn on gps" -> {
                    gpsHandler = GpsHandler(this,
                            onGetLocation = { lat, long ->
                                result.success(Loc(lat, long).toString())
                            },
                            onFailureGetLocation = {
                                result.error("ERROR", "Failed to get location", null)
                            }
                    )
                    gpsHandler!!.requestPermissionGps(GpsHandler.REQUEST_GPS_PERIMSSION)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            GpsHandler.REQUEST_GPS_PERIMSSION -> gpsHandler!!.handlePermissionResult(grantResults)
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode){
            GpsHandler.REQUEST_CHECK_SETTINGS -> if (resultCode == Activity.RESULT_OK) gpsHandler!!.settingsrequest()
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

data class Loc(val lat: Double, val long: Double) {
    override fun toString(): String {
        return """{"lat": "$lat", "long": "$long"}"""
    }
}
package com.example.flutter_gps

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity : FlutterActivity() {
    private var gpsHandler: GpsHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        gpsHandler = GpsHandler(this)

        MethodChannel(flutterView, "com.payfazz.Fazzcard/gps").setMethodCallHandler { methodCall, result ->
            when (methodCall.method) {
                "get_current_location" -> gpsHandler!!.onMethodGetCurrentLocation(result, GpsHandler.REQUEST_GPS_PERIMSSION)
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
            GpsHandler.REQUEST_CHECK_SETTINGS -> if (resultCode == Activity.RESULT_CANCELED) Toast.makeText(this, getString(R.string.failed_turn_on_request_gps), Toast.LENGTH_SHORT).show() else if (resultCode == Activity.RESULT_OK) gpsHandler!!.getLocation()
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
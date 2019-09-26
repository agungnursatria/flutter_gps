package com.example.flutter_gps.model


data class Location(val lat: Double, val long: Double) {
    override fun toString(): String {
        return """{"lat": "$lat", "long": "$long"}"""
    }
}
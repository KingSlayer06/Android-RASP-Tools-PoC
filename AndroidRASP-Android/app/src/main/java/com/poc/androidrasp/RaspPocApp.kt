package com.poc.androidrasp

import android.app.Application
import android.util.Log
import com.securevale.rasp.android.SecureApp

class RaspPocApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Loads libnative.so (Rust) and initialises the JNI layer. Must run once per process.
        SecureApp.init()
        Log.i(RaspRunner.TAG, "SecureApp.init() OK, native library loaded")
    }
}

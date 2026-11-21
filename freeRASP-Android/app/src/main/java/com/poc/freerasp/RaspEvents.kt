package com.poc.freerasp

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/** Records every freeRASP callback so the screen and logcat can show them. */
object RaspEvents {
    const val TAG = "freeRASP-PoC"

    private val start = SystemClock.elapsedRealtime()
    private val main = Handler(Looper.getMainLooper())
    private val events = mutableListOf<String>()
    private var listener: (() -> Unit)? = null

    @Synchronized
    fun record(name: String, detail: String = "") {
        val ms = SystemClock.elapsedRealtime() - start
        val line = "+${ms}ms  $name" + if (detail.isNotEmpty()) "  ($detail)" else ""
        events += line
        Log.i(TAG, line)
        main.post { listener?.invoke() }
    }

    @Synchronized
    fun snapshot(): List<String> = events.toList()

    fun observe(onChange: (() -> Unit)?) {
        listener = onChange
    }
}

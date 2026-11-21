package com.poc.freerasp

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

/** Shows every freeRASP callback as it fires. */
class MainActivity : Activity() {

    private lateinit var text: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        text = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setPadding(32, 96, 32, 32)
        }
        setContentView(ScrollView(this).apply { addView(text) })
    }

    override fun onResume() {
        super.onResume()
        RaspEvents.observe(::render)
        render()
    }

    override fun onPause() {
        RaspEvents.observe(null)
        super.onPause()
    }

    private fun render() {
        val events = RaspEvents.snapshot()
        text.text = buildString {
            appendLine("freeRASP-Android PoC")
            appendLine("build: ${BuildConfig.BUILD_TYPE}, prod=${RaspConfig.PROD}")
            appendLine("callbacks received: ${events.size}")
            appendLine()
            events.forEach(::appendLine)
        }
    }
}

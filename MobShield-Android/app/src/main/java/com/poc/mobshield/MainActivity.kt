package com.poc.mobshield

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.mobshield.core.MobShield
import io.mobshield.core.MobShieldListener
import io.mobshield.core.ThreatEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var output: TextView
    private val log = StringBuilder()
    private val time = SimpleDateFormat("HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        ShieldProfile.entries.forEach { profile ->
            root.addView(
                Button(this).apply {
                    text = "Run: ${profile.label}"
                    setOnClickListener { run(profile) }
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
            )
        }
        output = TextView(this).apply {
            textSize = 12f
            setTextIsSelectable(true)
        }
        root.addView(ScrollView(this).apply { addView(output) }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        append("MobShield native version=${MobShield.getVersion()} buildId=${MobShield.getBuildId()} selfCheck=${MobShield.selfCheck()}")
        run(ShieldProfile.ALL_DEFAULTS)
    }

    private fun run(profile: ShieldProfile) {
        append("\n=== Profile: ${profile.label}")
        val config = ShieldRunner.start(this, profile, listener)
        append(
            "modules=${ShieldRunner.registeredModuleNames()} detectOnly=${config.detectOnly} " +
                "termination=${config.terminationPolicy} intervalSec=${config.periodicIntervalSec} " +
                "tuning=${config.detectionTuning.keys}",
        )
    }

    private val listener = object : MobShieldListener {
        override fun onThreat(event: ThreatEvent) {
            append("THREAT ${event.type} ${event.severity} score=${event.score} signals=${event.signals}")
        }

        override fun onAllChecksFinished(events: List<ThreatEvent>) {
            val state = MobShield.getState()
            append("SCAN FINISHED: ${events.size} threat(s), risk=${state.riskLevel}, running=${state.running}")
        }
    }

    override fun onDestroy() {
        MobShield.stop()
        super.onDestroy()
    }

    private fun append(line: String) {
        Log.i(TAG, line)
        runOnUiThread {
            log.append("[").append(time.format(Date())).append("] ").append(line).append('\n')
            output.text = log
        }
    }

    companion object {
        const val TAG = "MobShieldPoC"
    }
}

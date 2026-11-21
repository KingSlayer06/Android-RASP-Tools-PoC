package com.example.rootbeerpoc

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.scottyab.rootbeer.RootBeer
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val boxes = mutableMapOf<RootCheck, CheckBox>()
    private lateinit var runner: RootBeerRunner
    private lateinit var output: TextView
    private lateinit var extraApps: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runner = RootBeerRunner(RootBeer(this))
        output = findViewById(R.id.output)
        extraApps = findViewById(R.id.extraApps)

        val list = findViewById<LinearLayout>(R.id.checkList)
        RootCheck.entries.forEach { check ->
            val box = CheckBox(this).apply {
                text = "${check.label}  (${check.method})"
                isChecked = check in RootBeerConfig.IS_ROOTED_EQUIVALENT
            }
            boxes[check] = box
            list.addView(box)
        }

        findViewById<Button>(R.id.presetDefault).setOnClickListener { select(RootBeerConfig.IS_ROOTED_EQUIVALENT) }
        findViewById<Button>(R.id.presetBusyBox).setOnClickListener { select(RootBeerConfig.WITH_BUSYBOX_EQUIVALENT) }
        findViewById<Button>(R.id.presetAll).setOnClickListener { select(RootBeerConfig.ALL) }
        findViewById<Button>(R.id.run).setOnClickListener { runSelected() }

        // Run once on launch so `adb logcat -s RootBeerPoC` shows results without tapping.
        runSelected()
    }

    private fun select(checks: Set<RootCheck>) {
        boxes.forEach { (check, box) -> box.isChecked = check in checks }
    }

    private fun runSelected() {
        val extra = extraApps.text.split(',', ' ', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        val config = RootBeerConfig(
            checks = boxes.filterValues { it.isChecked }.keys,
            extraRootManagementApps = extra,
            logging = true,
        )
        output.text = "Running ${config.checks.size} checks…"
        // RootBeer does file and process I/O, so its README recommends a background thread.
        executor.execute {
            val report = runner.run(config)
            val text = format(report)
            Log.i(TAG, text)
            main.post { output.text = text }
        }
    }

    private fun format(r: RootBeerReport): String = buildString {
        appendLine("Selected checks (${r.perCheck.size}):")
        r.perCheck.forEach { appendLine(line(it)) }
        appendLine()
        appendLine("Library presets:")
        appendLine(line(r.isRooted))
        appendLine(line(r.isRootedWithBusyBox))
        appendLine()
        append("VERDICT: ").append(if (r.anyDetected) "ROOT INDICATED by selected checks" else "no root indicated by selected checks")
    }

    private fun line(c: CheckResult) =
        "%-32s %-8s %6d µs%s".format(c.name, if (c.detected) "DETECTED" else "clean", c.micros, c.error?.let { "  ERROR $it" } ?: "")

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    companion object { const val TAG = "RootBeerPoC" }
}

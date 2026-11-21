package com.poc.androidrasp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.poc.androidrasp.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val worker = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.runButton.setOnClickListener { run() }
        binding.permissionButton.setOnClickListener {
            // Enables the advanced emulator checks (operator name, radio version, ...).
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQ_PHONE)
        }
        run() // auto-run once so a launch alone produces a Logcat report
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, grants: IntArray) {
        super.onRequestPermissionsResult(code, perms, grants)
        if (code == REQ_PHONE && grants.firstOrNull() == PackageManager.PERMISSION_GRANTED) run()
    }

    private fun run() {
        val toggles = RaspRunner.Config(
            emulator = binding.emulatorSwitch.isChecked,
            debugger = binding.debuggerSwitch.isChecked,
            root = binding.rootSwitch.isChecked
        )
        binding.output.text = getString(R.string.running)
        binding.runButton.isEnabled = false
        worker.execute {
            val report = runCatching { RaspRunner.runAll(applicationContext, toggles) }
                .getOrElse { "ERROR: ${it.stackTraceToString()}" }
            runOnUiThread {
                binding.output.text = report
                binding.runButton.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private companion object { const val REQ_PHONE = 42 }
}

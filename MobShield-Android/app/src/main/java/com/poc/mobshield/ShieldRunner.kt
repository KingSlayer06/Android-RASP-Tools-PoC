package com.poc.mobshield

import android.content.Context
import io.mobshield.core.MobShield
import io.mobshield.core.MobShieldConfig
import io.mobshield.core.MobShieldListener
import io.mobshield.core.ModuleRegistry
import io.mobshield.detect.debugger.DebugDetectionModule
import io.mobshield.detect.environment.EmulatorDetectionModule
import io.mobshield.detect.hooks.HookDetectionModule
import io.mobshield.detect.integrity.IntegrityDetectionModule
import io.mobshield.detect.root.RootDetectionModule

/**
 * Registers the profile's detector modules and starts MobShield.
 *
 * The README's `XxxDetectionRegistrar` helpers live only in the JitPack umbrella artifact; the
 * modular Maven Central artifacts ship the `XxxDetectionModule` classes, so we register those
 * directly with [ModuleRegistry] (exactly what the registrars do internally).
 */
object ShieldRunner {
    fun start(
        context: Context,
        profile: ShieldProfile,
        listener: MobShieldListener,
    ): MobShieldConfig {
        val app = context.applicationContext
        val config = profile.config(app.packageName)
        MobShield.stop()
        ModuleRegistry.clear()
        profile.detectors.forEach { detector ->
            ModuleRegistry.register(
                when (detector) {
                    Detector.ROOT -> RootDetectionModule(app, config.detectionTuning)
                    Detector.HOOKS -> HookDetectionModule(app, config.detectionTuning)
                    Detector.DEBUGGER -> DebugDetectionModule(app, config.detectionTuning)
                    Detector.ENVIRONMENT -> EmulatorDetectionModule(app, config.detectionTuning)
                    Detector.INTEGRITY -> IntegrityDetectionModule(app, config)
                },
            )
        }
        MobShield.start(app, config, listener)
        return config
    }

    fun registeredModuleNames(): List<String> = ModuleRegistry.getAll().map { it.name }
}

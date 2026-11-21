package com.poc.freerasp

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import app.talsec.rasp.security.api.ScreenProtector
import app.talsec.rasp.security.api.SuspiciousAppInfo
import app.talsec.rasp.security.api.Talsec
import app.talsec.rasp.security.api.TalsecConfig
import app.talsec.rasp.security.api.ThreatListener
import java.security.MessageDigest

class RaspApplication : Application() {

    private val threats = object : ThreatListener.ThreatDetected() {
        override fun onPrivilegedAccess() = RaspEvents.record("onPrivilegedAccess")
        override fun onDebug() = RaspEvents.record("onDebug")
        override fun onSimulator() = RaspEvents.record("onSimulator")
        override fun onAppIntegrity() = RaspEvents.record("onAppIntegrity")
        override fun onUnofficialStore() = RaspEvents.record("onUnofficialStore")
        override fun onHooks() = RaspEvents.record("onHooks")
        override fun onDeviceBinding() = RaspEvents.record("onDeviceBinding")
        override fun onObfuscationIssues() = RaspEvents.record("onObfuscationIssues")
        override fun onMalware(suspiciousApps: List<SuspiciousAppInfo>) = RaspEvents.record(
            "onMalware",
            suspiciousApps.joinToString { "${it.packageInfo.packageName}: ${it.reasons}" }
        )
        override fun onAutomation() = RaspEvents.record("onAutomation")
        override fun onScreenshot() = RaspEvents.record("onScreenshot")
        override fun onScreenRecording() = RaspEvents.record("onScreenRecording")
        override fun onMultiInstance() = RaspEvents.record("onMultiInstance")
        override fun onUnsecureWifi() = RaspEvents.record("onUnsecureWifi")
        override fun onTimeSpoofing() = RaspEvents.record("onTimeSpoofing")
        override fun onLocationSpoofing() = RaspEvents.record("onLocationSpoofing")
        override fun onBootloader() = RaspEvents.record("onBootloader")
        override fun onOngoingCall(state: ThreatListener.CallState) =
            RaspEvents.record("onOngoingCall", state.name)
    }

    private val deviceState = object : ThreatListener.DeviceState() {
        override fun onPasscode() = RaspEvents.record("onPasscode")
        override fun onSecureHardwareNotAvailable() = RaspEvents.record("onSecureHardwareNotAvailable")
        override fun onDevMode() = RaspEvents.record("onDevMode")
        override fun onAdbEnabled() = RaspEvents.record("onAdbEnabled")
        override fun onSystemVpn() = RaspEvents.record("onSystemVpn")
    }

    private val executionState = object : ThreatListener.RaspExecutionState() {
        override fun onCriticalChecksFinished() = RaspEvents.record("onCriticalChecksFinished")
        override fun onFastChecksFinished() = RaspEvents.record("onFastChecksFinished")
        override fun onPostponedChecksFinished() = RaspEvents.record("onPostponedChecksFinished")
    }

    override fun onCreate() {
        super.onCreate()

        val hashes = RaspConfig.EXPECTED_SIGNING_CERT_HASHES.ifEmpty { arrayOf(currentSigningCertHash()) }
        RaspEvents.record("Talsec.start", "prod=${RaspConfig.PROD}, certHash=${hashes.first()}")

        val config = TalsecConfig.Builder(RaspConfig.EXPECTED_PACKAGE_NAME, hashes)
            .watcherMail(RaspConfig.WATCHER_MAIL)
            .supportedAlternativeStores(RaspConfig.SUPPORTED_ALTERNATIVE_STORES)
            .prod(RaspConfig.PROD)
            .killOnBypass(RaspConfig.KILL_ON_BYPASS)
            .build()

        ThreatListener(
            threats,
            if (RaspConfig.REGISTER_DEVICE_STATE) deviceState else null,
            if (RaspConfig.REGISTER_EXECUTION_STATE) executionState else null,
        ).registerListener(this)
        Talsec.start(this, config)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                Talsec.blockScreenCapture(activity, RaspConfig.BLOCK_SCREEN_CAPTURE)
                Talsec.blockScreenControl(activity, RaspConfig.BLOCK_SCREEN_CONTROL)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Talsec.blockOverlayWindows(activity, RaspConfig.BLOCK_OVERLAY_WINDOWS)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                if (RaspConfig.DETECT_SCREEN_EVENTS) ScreenProtector.registerScreenCallbacks(activity)
                if (RaspConfig.DETECT_ONGOING_CALL) Talsec.startOngoingCallDetection(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (RaspConfig.DETECT_SCREEN_EVENTS) ScreenProtector.unregisterScreenCallbacks(activity)
                if (RaspConfig.DETECT_ONGOING_CALL) Talsec.stopOngoingCallDetection(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /** PoC helper: Base64 SHA-256 of the certificate that signed this APK. */
    @Suppress("DEPRECATION")
    private fun currentSigningCertHash(): String {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners.orEmpty()
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures.orEmpty()
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(sigs.first().toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}

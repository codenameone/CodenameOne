package com.codenameone.examples.hellocodenameone

import com.codename1.camera.Camera
import com.codename1.car.Car
import com.codename1.car.CarApplication
import com.codename1.car.CarContext
import com.codename1.car.CarListTemplate
import com.codename1.car.CarRow
import com.codename1.car.CarScreen
import com.codename1.car.CarTemplate
import com.codename1.system.Lifecycle
import com.codename1.testing.TestReporting
import com.codename1.ui.CN
import com.codename1.ui.Display
import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunner
import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunnerReporter
import com.codenameone.examples.hellocodenameone.tests.KotlinUiTest

open class HelloCodenameOne : Lifecycle() {
    override fun init(context: Any?) {
        super.init(context)
        check(!Display.getInstance().isJailbrokenDevice()) {
            "Jailbroken device detected by Display.isJailbrokenDevice()."
        }
        DefaultMethodDemo.validate()
        // Reference the low-level camera API (com.codename1.camera.*) so the
        // build's bytecode scanner flips IPhoneBuilder.usesCn1Camera /
        // AiDependencyTable: this compiles the CN1Camera AVFoundation natives
        // on iOS & Mac Catalyst and pulls in CameraX on Android. Without an app
        // exercising this API, that native code is gated out of every CI build
        // and never gets compiled. isSupported()/getCameras() never open a
        // session, so no runtime permission prompt is triggered.
        try {
            val cameraSupported = Camera.isSupported()
            val cameraCount = if (cameraSupported) Camera.getCameras().size else 0
            System.out.println("CN1SS:CAMERA_DIAG supported=$cameraSupported cameras=$cameraCount")
        } catch (t: Throwable) {
            System.out.println("CN1SS:CAMERA_DIAG:EXCEPTION " + t.javaClass.name + ": " + t.message)
        }
        // Reference the in-car API (com.codename1.car.*) so the build's bytecode scanner flips
        // usesCar: this compiles the CarPlay natives on iOS/Mac (CN1_USE_CARPLAY +
        // CarPlay.framework + the CarPlay scene/entitlement) and injects the Android Auto
        // CarAppService + androidx.car.app dependency. Without an app exercising this API that
        // code is gated out of every CI build and never compiles. Registration is inert at CI:
        // no head unit connects, so CN.isCarConnected() stays false on the simulator.
        try {
            Car.setApplication(HelloCarApplication())
            System.out.println("CN1SS:CARPLAY_DIAG connected=" + CN.isCarConnected())
        } catch (t: Throwable) {
            System.out.println("CN1SS:CARPLAY_DIAG:EXCEPTION " + t.javaClass.name + ": " + t.message)
        }
        try {
            NativeInterfaceLanguageValidator.validate()
        } catch (t: Throwable) {
            System.out.println("CN1SS:SWIFT_DIAG:VALIDATION_EXCEPTION " + t.javaClass.name + ": " + t.message)
            t.printStackTrace()
            // Keep running so DeviceRunner can emit CN1SS markers and report swift_diag_status explicitly.
        }
        Cn1ssDeviceRunner.addTest(KotlinUiTest())
        // NOTE: the in-car API is exercised via Car.setApplication(...) above (which is what makes
        // the build compile the CarPlay / Android Auto natives) and validated by the dedicated
        // car-android.yml / car-ios.yml integration workflows + the JVM CarTest. It is deliberately
        // NOT registered as a device-suite screenshot test: adding a screenshot-producing test to the
        // shared suite destabilises the screenshot baselines (the JS/iOS suites are timing-sensitive).
        TestReporting.setInstance(Cn1ssDeviceRunnerReporter())
    }

    override fun runApp() {
        // HTML5 runs inside a Web Worker whose single thread hosts the EDT —
        // starting a java.lang.Thread there would never get to execute, so
        // call the runner serially on the EDT instead.
        val runner = Runnable { Cn1ssDeviceRunner().runSuite() }
        if (Display.getInstance().platformName == "HTML5") {
            CN.callSerially(runner)
        } else {
            Thread(runner, "CN1SS-Runner").start()
        }
    }
}

/**
 * Minimal in-car experience exercised only to keep the CarPlay / Android Auto native code compiled
 * in CI (see the note in [HelloCodenameOne.init]). Builds a single browse list with the portable
 * com.codename1.car template API; never shown unless a real head unit connects.
 */
class HelloCarApplication : CarApplication() {
    override fun onCreateRootScreen(context: CarContext): CarScreen = object : CarScreen() {
        override fun onCreateTemplate(): CarTemplate =
            CarListTemplate().setTitle("Hello Car")
                .addRow(CarRow("Now Playing").setBrowsable(true))
                .addRow(CarRow("Library").setBrowsable(true))
    }
}

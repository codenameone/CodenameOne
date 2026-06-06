package com.codenameone.examples.hellocodenameone

import com.codename1.camera.Camera
import com.codename1.payment.Purchase
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
        try {
            NativeInterfaceLanguageValidator.validate()
        } catch (t: Throwable) {
            System.out.println("CN1SS:SWIFT_DIAG:VALIDATION_EXCEPTION " + t.javaClass.name + ": " + t.message)
            t.printStackTrace()
            // Keep running so DeviceRunner can emit CN1SS markers and report swift_diag_status explicitly.
        }
        // Reference the In-App-Purchase API (com.codename1.payment.*) so the
        // build's bytecode scanner flips IPhoneBuilder.usesPurchaseAPI: this
        // defines CN1_USE_STOREKIT and links StoreKit.framework on iOS so the
        // SKPaymentQueue observer is compiled in. Installing a recording
        // ReceiptStore lets the iOS StoreKitTest harness assert, from the
        // hosted XCTest, that a purchase reached the store -- the iOS-level
        // guard for issue #5186. Without an app exercising IAP this native
        // path is gated out of every CI build and never gets compiled.
        try {
            Purchase.getInAppPurchase().setReceiptStore(RecordingReceiptStore())
            // Drain any receipts already enqueued before the store was installed
            // (e.g. the Android instrumentation fake fires a purchase from the
            // activity's onCreate, which can race ahead of this init). Sensible
            // for a real app too: submit pending purchases once the store exists.
            Purchase.getInAppPurchase().synchronizeReceipts()
            System.out.println("CN1SS:IAP_DIAG installed=true")
        } catch (t: Throwable) {
            System.out.println("CN1SS:IAP_DIAG:EXCEPTION " + t.javaClass.name + ": " + t.message)
        }
        Cn1ssDeviceRunner.addTest(KotlinUiTest())
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

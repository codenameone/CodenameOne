package com.codenameone.examples.hellocodenameone

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
        try {
            NativeInterfaceLanguageValidator.validate()
        } catch (t: Throwable) {
            System.out.println("CN1SS:SWIFT_DIAG:VALIDATION_EXCEPTION " + t.javaClass.name + ": " + t.message)
            t.printStackTrace()
            // Keep running so DeviceRunner can emit CN1SS markers and report swift_diag_status explicitly.
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

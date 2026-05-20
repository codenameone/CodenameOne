package com.codenameone.examples.hellocodenameone

import com.codename1.system.Lifecycle
import com.codename1.testing.TestReporting
import com.codename1.ui.CN
import com.codename1.ui.Display
import com.codename1.ui.Graphics
import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunner
import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunnerReporter
import com.codenameone.examples.hellocodenameone.tests.KotlinUiTest

open class HelloCodenameOne : Lifecycle() {
    override fun init(context: Any?) {
        super.init(context)
        // Opt the test app into matrix-correct g.translate: under this flag
        // the container/component painting chain composes its translates onto
        // the impl-side affine instead of accumulating an integer offset that
        // gets multiplied by subsequent user g.scale calls. Required for
        // direct-to-screen rendering in the GH-3302 inscribed-triangle /
        // translate-then-scale screenshots to land on the same pixels as the
        // buffered mutable-image path.
        //
        // The default is on; CI also runs the suite with the flag off via
        // `-Dcodename1.arg.matrixTranslation=false` so regressions in the
        // legacy code path get caught alongside the matrix-mode coverage.
        val matrixFlag = Display.getInstance().getProperty("matrixTranslation", "true")
        Graphics.useMatrixTranslation = "true".equals(matrixFlag, ignoreCase = true)
        // Diagnostic: print the resolved flag value to the CI log so we
        // can confirm the build hint round-tripped to the runtime
        // correctly (e.g. validate that the build-ios-metal-legacy job
        // really runs with matrix mode off).
        System.out.println("CN1SS:INFO:matrixTranslation=" + Graphics.useMatrixTranslation + " property=" + matrixFlag)
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

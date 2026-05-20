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
        // Scoped to iOS only: this feature targets the iOS Metal backend
        // (where direct-to-screen affine drift is observable). Leaving it
        // on for the JavaScript / Android / desktop ports would change
        // pixel output for tests that already have green baselines on
        // those platforms without delivering the GH-3302 fix that motivated
        // the work. CI flips the iOS suite into matrix-off mode through
        // `-Dcodename1.arg.matrixTranslation=false` so the legacy code
        // path also gets regression coverage.
        // Exact-string matching on purpose: the Kotlin `equals(...,
        // ignoreCase = true)` extension compiles to a kotlin.text.StringsKt
        // call, and the ParparVM-translated JavaScript port doesn't bundle
        // StringsKt, so the Kotlin form blows up at class init with
        // `Unknown class kotlin_text_StringsKt` and the browser harness
        // hangs waiting for the suite to start. The platform name is
        // already lowercase (`"ios"`, see also the `"HTML5"` check below)
        // and the matrixTranslation build hint is set to lowercase
        // `true`/`false` by CI, so case folding isn't needed here.
        val platform = Display.getInstance().platformName
        val isIos = platform == "ios"
        val matrixFlag = Display.getInstance().getProperty("matrixTranslation", if (isIos) "true" else "false")
        Graphics.useMatrixTranslation = isIos && matrixFlag == "true"
        // Diagnostic: print the resolved flag value to the CI log so we
        // can confirm the build hint round-tripped to the runtime
        // correctly (e.g. validate that the build-ios-metal-legacy job
        // really runs with matrix mode off).
        System.out.println("CN1SS:INFO:matrixTranslation=" + Graphics.useMatrixTranslation + " property=" + matrixFlag + " platform=" + platform)
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

package com.codenameone.examples.hellocodenameone

import com.codename1.system.Lifecycle
import com.codename1.testing.TestReporting
import com.codename1.ui.Display
import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunner
import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunnerReporter
import com.codenameone.examples.hellocodenameone.tests.KotlinUiTest

open class HelloCodenameOne : Lifecycle() {
    override fun init(context: Any?) {
        println("CN1JS:HelloCodenameOne.init.begin")
        super.init(context)
        check(!Display.getInstance().isJailbrokenDevice()) {
            "Jailbroken device detected by Display.isJailbrokenDevice()."
        }
        DefaultMethodDemo.validate()
        Cn1ssDeviceRunner.addTest(KotlinUiTest())
        TestReporting.setInstance(Cn1ssDeviceRunnerReporter())
        println("CN1JS:HelloCodenameOne.init.end")
    }

    override fun runApp() {
        println("CN1JS:HelloCodenameOne.runApp")
        val runner = Runnable {
            println("CN1JS:HelloCodenameOne.runner.begin")
            Cn1ssDeviceRunner().runSuite()
            println("CN1JS:HelloCodenameOne.runner.end")
        }
        if (Display.getInstance().platformName == "HTML5") {
            runner.run()
        } else {
            Thread(runner, "CN1SS-Runner").start()
        }
    }
}

// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::maven-creating-cn1libs-java-001[]
package com.example;

public class HelloWorld {
    public static void helloWorld() {
        System.out.println("Hello world");
    }
}
// end::maven-creating-cn1libs-java-001[]

// tag::maven-creating-cn1libs-java-002[]
com.example.HelloWorld.helloWorld();
// end::maven-creating-cn1libs-java-002[]

// tag::maven-creating-cn1libs-java-003[]
package com.example.bt.simulator;

import com.codename1.components.ToastBar;
import com.codename1.ui.Display;

public final class Hooks {
    public static void toggleAdapter() {
        boolean next = !BluetoothSimulator.isEnabled();
        BluetoothSimulator.setEnabled(next);
        if (Display.isInitialized()) {
            ToastBar.showInfoMessage("Bluetooth adapter " + (next ? "ON" : "OFF"));
        }
    }
}
// end::maven-creating-cn1libs-java-003[]

// tag::maven-creating-cn1libs-java-004[]
import com.codename1.testing.AbstractTest;
import com.codename1.ui.CN;

public class BluetoothDemoTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        // Skip cleanly off-simulator: a real device has no hook registered
        // and CN.canExecute will not return TRUE.
        if (!Boolean.TRUE.equals(CN.canExecute("bluetooth:item2"))) {
            return true;
        }
        // Seed the simulator — same effect as clicking "Add demo peripheral".
        CN.execute("bluetooth:item2");
        // ...now drive the public Bluetooth API as usual.
        return true;
    }
}
// end::maven-creating-cn1libs-java-004[]

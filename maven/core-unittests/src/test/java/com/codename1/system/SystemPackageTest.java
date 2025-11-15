package com.codename1.system;

import com.codename1.io.Preferences;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;

import static org.junit.jupiter.api.Assertions.*;

class SystemPackageTest extends UITestBase {

    interface MockNative extends NativeInterface { }

    public static class MockNativeImpl implements MockNative {
        public boolean isSupported() {
            return true;
        }
    }

    @FormTest
    void testNativeLookupRegistersAndCreatesInstances() {
        NativeLookup.setVerbose(false);
        NativeLookup.register(MockNative.class, MockNativeImpl.class);
        MockNative nativeInstance = NativeLookup.create(MockNative.class);
        assertNotNull(nativeInstance);
        assertTrue(nativeInstance.isSupported());

        class UnsupportedNative implements NativeInterface {
            public boolean isSupported() {
                return false;
            }
        }

        NativeLookup.setVerbose(false);
        assertNull(NativeLookup.create(UnsupportedNative.class));
    }

    @FormTest
    void testLifecycleStartInvokesRunApp() {
        TestLifecycle lifecycle = new TestLifecycle();
        lifecycle.init(null);
        lifecycle.start();
        assertTrue(lifecycle.wasRunAppCalled());
        lifecycle.stop();
        lifecycle.destroy();
    }

    @FormTest
    void testDefaultCrashReporterInstallsReporter() {
        Preferences.set("$CN1_crashBlocked", false);
        Preferences.set("$CN1_pendingCrash", false);
        Preferences.set("$CN1_prompt", true);

        DefaultCrashReporter.setErrorText("Crash detected");
        assertEquals("Crash detected", DefaultCrashReporter.getErrorText());
        DefaultCrashReporter.init(false, 0);
        assertNotNull(Display.getInstance().getCrashReporter());
    }

    private static class TestLifecycle extends Lifecycle {
        private boolean runAppCalled;

        public void init(Object context) {
            // Do not call super.init() to avoid theme loading.
        }

        public void runApp() {
            runAppCalled = true;
        }

        boolean wasRunAppCalled() {
            return runAppCalled;
        }
    }
}

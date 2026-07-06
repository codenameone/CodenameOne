package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ar.AR;
import com.codename1.ar.ARCapabilities;
import com.codename1.ar.ARSessionOptions;
import com.codename1.util.SuccessCallback;

/// Exercises the `com.codename1.ar` API contract against whichever per-port
/// `ARImpl` is in use, without opening a live AR session.
///
/// A real `AR.open()` has device-specific side effects that make it unsafe in
/// the shared screenshot suite: on Android the ARCore backend calls
/// `ArCoreApk.requestInstall()`, which launches the Play Store install flow and
/// backgrounds the app when ARCore is absent - precisely the CI emulator's
/// state - freezing the whole suite. The full session round trip is therefore
/// covered where it is safe and deterministic: the JavaSE simulator integration
/// test drives `JavaSEARImpl` end to end (open, plane detection, hit test,
/// anchor), and the core unit tests exercise it against `RecordingARImpl`.
///
/// Here we only verify the parts that are safe on every port: the capability
/// query never returns null, and on platforms with no AR runtime the documented
/// unsupported contract holds (all capabilities false, `AR.open` throws before
/// touching any backend, and the permission request delivers false).
///
/// No screenshot -- this is an assertion test.
public class ARApiTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        final boolean supported = AR.isSupported();
        ARCapabilities caps = AR.getCapabilities();
        if (caps == null) {
            fail("getCapabilities() must never return null");
            return true;
        }
        com.codename1.io.Log.p("ARApiTest: supported=" + supported
                + " world=" + caps.isWorldTrackingSupported()
                + " planes=" + caps.isPlaneDetectionSupported()
                + " images=" + caps.isImageTrackingSupported()
                + " faces=" + caps.isFaceTrackingSupported()
                + " light=" + caps.isLightEstimationSupported());

        if (supported) {
            // A live session open is exercised by the JavaSE simulator
            // integration test and the unit tests, not here, to avoid the
            // Android ARCore install flow backgrounding the screenshot suite.
            done();
            return true;
        }

        // No AR runtime on this platform: the full unsupported contract. When
        // unsupported, AR.open throws IllegalStateException before it reaches
        // any backend, so there is no device side effect to worry about.
        if (caps.isWorldTrackingSupported() || caps.isPlaneDetectionSupported()
                || caps.isImageTrackingSupported() || caps.isFaceTrackingSupported()
                || caps.isLightEstimationSupported()) {
            fail("unsupported platforms must report all capabilities false");
            return true;
        }
        try {
            AR.open(new ARSessionOptions());
            fail("AR.open must throw when unsupported");
            return true;
        } catch (IllegalStateException expected) {
            // The documented unsupported behavior.
        }
        AR.requestPermissions(new SuccessCallback<Boolean>() {
            public void onSucess(Boolean granted) {
                if (Boolean.TRUE.equals(granted)) {
                    fail("permission request must deliver false when AR is unsupported");
                } else {
                    done();
                }
            }
        });
        return true;
    }
}

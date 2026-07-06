package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ar.AR;
import com.codename1.ar.ARAnchor;
import com.codename1.ar.ARCapabilities;
import com.codename1.ar.ARModel;
import com.codename1.ar.ARNode;
import com.codename1.ar.ARPose;
import com.codename1.ar.ARSession;
import com.codename1.ar.ARSessionOptions;
import com.codename1.ar.ARView;
import com.codename1.gpu.Primitives;
import com.codename1.util.SuccessCallback;

/// End-to-end exercise of the `com.codename1.ar` API contract against
/// whichever per-port `ARImpl` is in use.
///
/// None of the CI suite platforms has an AR runtime (ARKit is unavailable on
/// the iOS Simulator and Mac Catalyst, the CI Android emulator has no ARCore,
/// and the desktop/JS ports ship no backend), so what CI verifies is the
/// documented unsupported contract: capabilities all read false, `AR.open`
/// throws, and the permission request delivers false. On a real AR device (a
/// manual run) the supported branch opens a session, places content on an
/// anchor and closes cleanly. The simulated AR backend is covered separately
/// by the JavaSE simulator integration screenshot tests.
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
        if (!supported) {
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

        // Supported platform (a real AR device or the simulator): a full but
        // conservative session round trip that needs no tracking to settle.
        ARSession session = AR.open(new ARSessionOptions());
        try {
            if (session.isClosed()) {
                fail("freshly opened session reports closed");
                return true;
            }
            ARView view = session.createView();
            if (view == null || view != session.createView()) {
                fail("createView must return one cached view instance");
                return true;
            }
            if (session.getCameraPose() == null || session.getLightEstimate() == null
                    || session.getTrackingState() == null) {
                fail("poll getters must never return null");
                return true;
            }
            ARAnchor anchor = session.createAnchor(ARPose.IDENTITY);
            anchor.setNode(new ARNode(ARModel.fromMesh(
                    Primitives.sphere(0.1f, 8, 12, false), 0xffdd4433)));
            if (session.getAnchors().length != 1) {
                fail("anchor registry expected exactly one anchor");
                return true;
            }
            anchor.detach();
            if (!anchor.isDetached() || session.getAnchors().length != 0) {
                fail("detach must remove the anchor");
                return true;
            }
        } finally {
            session.close();
            session.close(); // idempotent
        }
        if (!session.isClosed()) {
            fail("close must mark the session closed");
            return true;
        }
        done();
        return true;
    }
}

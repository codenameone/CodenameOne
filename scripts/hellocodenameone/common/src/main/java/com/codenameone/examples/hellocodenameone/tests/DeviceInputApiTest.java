package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.DevicePosture;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.PointerEvent;

/**
 * Exercises the device input and form-factor APIs (rich pointer metadata, multi-button mice,
 * mouse wheel, stylus, foldable posture and multi-display detection) end-to-end against whatever
 * platform the suite runs on. It only asserts that the API surface is reachable and returns the
 * documented safe defaults, so it runs everywhere without a golden image.
 */
public class DeviceInputApiTest extends BaseTest {
    @Override
    public boolean runTest() {
        try {
            // Rich pointer metadata is always reachable and never null.
            PointerEvent current = CN.getCurrentPointerEvent();
            if (current == null) {
                fail("CN.getCurrentPointerEvent() must never return null");
                return false;
            }
            CN.getPointerButton();
            CN.getPressedButtonMask();
            CN.getPointerType();
            float pressure = CN.getPointerPressure();
            if (pressure < 0f || pressure > 4f) {
                fail("Unexpected pressure value: " + pressure);
                return false;
            }
            CN.getPointerTiltX();
            CN.getPointerTiltY();
            CN.getPointerContactSize();
            CN.isStylusPointer();

            // PointerEvent value object behaves as documented.
            PointerEvent pe = new PointerEvent(3, 4, PointerEvent.BUTTON_SECONDARY,
                    PointerEvent.MASK_SECONDARY, PointerEvent.TYPE_STYLUS, 0.5f, 10f, 20f, 0.3f,
                    PointerEvent.MODIFIER_ALT, false);
            if (!pe.isSecondaryButton() || !pe.isStylus() || !pe.isAltDown()
                    || pe.getX() != 3 || pe.getY() != 4) {
                fail("PointerEvent accessors returned unexpected values");
                return false;
            }

            // Foldable / device posture API and its safe defaults.
            DevicePosture posture = CN.getDevicePosture();
            if (posture == null) {
                fail("DevicePosture.getInstance() must never return null");
                return false;
            }
            posture.getPosture();
            posture.getHingeAngle();
            posture.getFoldOrientation();
            posture.isSeparating();
            posture.isTableTop();
            posture.getFoldBounds(null);
            boolean foldable = CN.isFoldable();
            if (!foldable && posture.getFoldBounds(null) != null) {
                fail("A non-foldable device must not report fold bounds");
                return false;
            }

            // Desktop windowing / multi-display detection.
            CN.isDesktopMode();
            if (CN.getDisplayCount() < 1) {
                fail("getDisplayCount() must be at least 1");
                return false;
            }
            if (CN.isExternalDisplayConnected() != (CN.getDisplayCount() > 1)) {
                fail("isExternalDisplayConnected() must agree with getDisplayCount()");
                return false;
            }

            // Listener registration must be safe even when nothing fires.
            ActionListener noop = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                }
            };
            CN.addPostureListener(noop);
            CN.removePostureListener(noop);
            Label probe = new Label("probe");
            probe.addContextMenuListener(noop);
            probe.removeContextMenuListener(noop);
            probe.addMouseWheelListener(noop);
            probe.removeMouseWheelListener(noop);
            probe.addStylusListener(noop);
            probe.removeStylusListener(noop);

            done();
            return true;
        } catch (Throwable t) {
            fail("Device input API invocation failed: " + t);
            return false;
        }
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}

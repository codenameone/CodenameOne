package com.codenameone.examples.hellocodenameone;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.DevicePosture;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Interactive demonstration of the device input and form-factor APIs. Show this form in the
 * simulator (or on device) and interact with the surface to watch the live pointer detail,
 * mouse wheel events, context menu requests and device posture update in real time.
 *
 * <p>This is a manual demo rather than an automated test - the APIs it shows depend on physical
 * input (mouse buttons, a stylus, a foldable hinge) that cannot be synthesized headlessly.</p>
 */
public class DeviceInputDemo {

    private final Label pointerInfo = new Label("Touch / click the surface");
    private final Label wheelInfo = new Label("Scroll the wheel here");
    private final Label contextInfo = new Label("Right click or long press for a context menu");
    private final Label postureInfo = new Label();
    private final Label displayInfo = new Label();

    /**
     * Builds the demo form. Pass the form to return to when the back command is pressed, or null.
     */
    public Form build(final Form back) {
        Form f = new Form("Device Input", BoxLayout.y());

        Container surface = new Container(BoxLayout.y());
        surface.add(new Label("Interaction surface:"));
        surface.add(pointerInfo);
        surface.add(wheelInfo);
        surface.add(contextInfo);

        // Rich pointer metadata on every press / drag / release.
        ActionListener pointerListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                PointerEvent pe = evt.getPointerEvent();
                if (pe != null) {
                    pointerInfo.setText("button=" + describeButton(pe.getButton())
                            + " type=" + describeType(pe.getPointerType())
                            + " pressure=" + round(pe.getPressure())
                            + " tiltX=" + round(pe.getTiltX())
                            + (pe.isHovering() ? " (hover)" : ""));
                    pointerInfo.getParent().revalidate();
                }
            }
        };
        surface.addPointerPressedListener(pointerListener);
        surface.addPointerDraggedListener(pointerListener);

        // Mouse wheel - control plus wheel is intercepted to show consumption.
        surface.addMouseWheelListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                WheelEvent w = (WheelEvent) evt;
                wheelInfo.setText("wheel dx=" + w.getDeltaX() + " dy=" + w.getDeltaY()
                        + (w.isPrecise() ? " precise" : "")
                        + (w.isControlDown() ? " +ctrl(consumed)" : ""));
                wheelInfo.getParent().revalidate();
                if (w.isControlDown()) {
                    w.consume();
                }
            }
        });

        // Unified context menu request: right click, stylus barrel button or long press.
        surface.addContextMenuListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                contextInfo.setText("context menu requested at " + evt.getX() + "," + evt.getY());
                contextInfo.getParent().revalidate();
                evt.consume();
            }
        });

        Button refresh = new Button("Refresh posture / display info");
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                updatePostureInfo();
                updateDisplayInfo();
            }
        });

        CN.addPostureListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                updatePostureInfo();
            }
        });

        updatePostureInfo();
        updateDisplayInfo();

        f.add(surface);
        f.add(new Label("Device posture:"));
        f.add(postureInfo);
        f.add(new Label("Displays:"));
        f.add(displayInfo);
        f.add(refresh);

        if (back != null) {
            f.getToolbar().setBackCommand("Back", new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    back.showBack();
                }
            });
        }
        return f;
    }

    private void updatePostureInfo() {
        DevicePosture p = CN.getDevicePosture();
        postureInfo.setText("foldable=" + p.isFoldable()
                + " posture=" + describePosture(p.getPosture())
                + " hinge=" + p.getHingeAngle()
                + " orientation=" + describeFoldOrientation(p.getFoldOrientation())
                + " separating=" + p.isSeparating());
        if (postureInfo.getParent() != null) {
            postureInfo.getParent().revalidate();
        }
    }

    private void updateDisplayInfo() {
        displayInfo.setText("count=" + CN.getDisplayCount()
                + " external=" + CN.isExternalDisplayConnected()
                + " desktopMode=" + CN.isDesktopMode()
                + " desktop=" + CN.isDesktop());
        if (displayInfo.getParent() != null) {
            displayInfo.getParent().revalidate();
        }
    }

    private static String round(float v) {
        return "" + (Math.round(v * 100) / 100.0);
    }

    private static String describeButton(int button) {
        switch (button) {
            case PointerEvent.BUTTON_PRIMARY: return "primary";
            case PointerEvent.BUTTON_SECONDARY: return "secondary";
            case PointerEvent.BUTTON_MIDDLE: return "middle";
            case PointerEvent.BUTTON_BACK: return "back";
            case PointerEvent.BUTTON_FORWARD: return "forward";
            default: return "none";
        }
    }

    private static String describeType(int type) {
        switch (type) {
            case PointerEvent.TYPE_TOUCH: return "touch";
            case PointerEvent.TYPE_MOUSE: return "mouse";
            case PointerEvent.TYPE_STYLUS: return "stylus";
            case PointerEvent.TYPE_ERASER: return "eraser";
            default: return "unknown";
        }
    }

    private static String describePosture(int posture) {
        switch (posture) {
            case DevicePosture.POSTURE_FLAT: return "flat";
            case DevicePosture.POSTURE_HALF_OPENED: return "half-opened";
            case DevicePosture.POSTURE_CLOSED: return "closed";
            default: return "unknown";
        }
    }

    private static String describeFoldOrientation(int orientation) {
        switch (orientation) {
            case DevicePosture.FOLD_ORIENTATION_VERTICAL: return "vertical";
            case DevicePosture.FOLD_ORIENTATION_HORIZONTAL: return "horizontal";
            default: return "none";
        }
    }
}

package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityRole;

import dart.runtime.Funcs;

/**
 * Leaf render box for {@link GestureOverlay}: a transparent CN1 component
 * (UIID "FlutterGesture", paints nothing, grabs pointer events) sized by the
 * parent {@link GestureRenderElement} to exactly the child's bounds. A tap
 * is a pointer release inside the bounds that neither activated a drag nor
 * followed a long press; long presses ride CN1's built-in long-press
 * dispatch.
 */
public class GestureOverlayRenderElement extends RenderElement {

    public GestureOverlayRenderElement(GestureOverlay widget) {
        super(widget);
    }

    private GestureDetector gesture() {
        Element p = parent();
        if (p instanceof GestureRenderElement) {
            return ((GestureRenderElement) p).gesture();
        }
        return null;
    }

    private void fire(Funcs.VoidFunc0 f) {
        if (f != null) {
            f.call();
        }
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Component c = new OverlayComponent();
        publishSemantics(c);
        return c;
    }

    @Override
    protected void updateComponent(Component c) {
        publishSemantics(c);
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        // The parent hands us tight constraints matching the child's bounds.
        return constraints.smallest();
    }

    /**
     * Publishes the tap target to the accessibility tree.
     *
     * <p>The overlay is the only component that knows a subtree is tappable —
     * the child it covers is ordinary content — so without this a
     * GestureDetector/InkWell is invisible to screen readers and to anything
     * driving the UI through semantics. Mirrors Flutter, which gives a
     * GestureDetector with an onTap the button role and a tap action.</p>
     */
    private void publishSemantics(Component c) {
        GestureDetector g = gesture();
        if (g == null || g.getOnTap() == null) {
            return;
        }
        try {
            c.getSemantics()
                    .setRole(AccessibilityRole.BUTTON)
                    .addAction(new AccessibilityAction(AccessibilityAction.ACTIVATE, null,
                            new AccessibilityAction.Handler() {
                                @Override
                                public boolean perform(Component component, Object argument) {
                                    GestureDetector target = gesture();
                                    if (target == null || target.getOnTap() == null) {
                                        return false;
                                    }
                                    fire(target.getOnTap());
                                    return true;
                                }
                            }));
        } catch (Throwable t) {
            // semantics are best-effort; never fail a build over them
        }
    }

    class OverlayComponent extends Component {

        private boolean suppressTap;

        OverlayComponent() {
            setUIID("FlutterGesture");
            setGrabsPointerEvents(true);
            setFocusable(false);
            getAllStyles().setBgTransparency(0);
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
        }

        @Override
        public void paint(Graphics g) {
            // paints nothing — pure hit area
        }

        @Override
        public void pointerPressed(int x, int y) {
            suppressTap = false;
            super.pointerPressed(x, y);
        }

        @Override
        public void longPointerPress(int x, int y) {
            super.longPointerPress(x, y);
            GestureDetector g = gesture();
            if (g != null && g.getOnLongPress() != null) {
                suppressTap = true;
                fire(g.getOnLongPress());
            }
        }

        @Override
        public void pointerReleased(int x, int y) {
            boolean wasDrag = isDragActivated();
            super.pointerReleased(x, y);
            if (!wasDrag && !suppressTap && contains(x, y)) {
                GestureDetector g = gesture();
                if (g != null) {
                    fire(g.getOnTap());
                }
            }
            suppressTap = false;
        }
    }
}

package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

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
        return new OverlayComponent();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        // The parent hands us tight constraints matching the child's bounds.
        return constraints.smallest();
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

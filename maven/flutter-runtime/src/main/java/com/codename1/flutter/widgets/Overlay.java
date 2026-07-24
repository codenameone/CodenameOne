package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * The stack of {@link OverlayEntry} objects floating above the navigator —
 * Flutter's {@code Overlay}. new_gallery reaches the ambient overlay through the
 * static {@link #of(BuildContext, boolean, Object)} to insert feature-discovery
 * entries; the {@code Overlay} widget itself is provided by the navigator and is
 * not constructed by the app, so its element holds no children at this pass.
 */
public class Overlay extends Widget {

    private static final OverlayState SHARED_STATE = new OverlayState();

    private DartList<OverlayEntry> initialEntries;
    private Object clipBehavior;

    public void initialEntries(DartList<OverlayEntry> v) {
        this.initialEntries = v;
    }

    public void clipBehavior(Object v) {
        this.clipBehavior = v;
    }

    /** Flutter's {@code Overlay.of} — the nearest ancestor overlay's state. */
    public static OverlayState of(BuildContext context, boolean rootOverlay, Object debugRequiredFor) {
        return SHARED_STATE;
    }

    /** Flutter's {@code Overlay.maybeOf}. */
    public static OverlayState maybeOf(BuildContext context, boolean rootOverlay) {
        return SHARED_STATE;
    }

    @Override
    public Element createElement() {
        return new SimpleChildrenRenderElement(this, new SimpleChildrenRenderElement.Children() {
            @Override
            public DartList<Widget> get() {
                return null;
            }
        });
    }
}

// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::device-input-and-form-factors-java-001[]
myComponent.addPointerPressedListener(e -> {
    PointerEvent pe = e.getPointerEvent();
    if (pe.isSecondaryButton()) {
        showContextMenu(pe.getX(), pe.getY());
    }
    System.out.println("pressure=" + pe.getPressure() + " type=" + pe.getPointerType());
});
// end::device-input-and-form-factors-java-001[]

// tag::device-input-and-form-factors-java-002[]
int button = CN.getPointerButton();      // PointerEvent.BUTTON_PRIMARY, BUTTON_SECONDARY, ...
float pressure = CN.getPointerPressure(); // 0.0 - 1.0, defaults to 1.0
boolean pen = CN.isStylusPointer();
PointerEvent current = CN.getCurrentPointerEvent();
// end::device-input-and-form-factors-java-002[]

// tag::device-input-and-form-factors-java-003[]
label.addContextMenuListener(e -> {
    e.consume(); // suppress the normal click/long-press handling
    showMyContextMenu(e.getX(), e.getY());
});
// end::device-input-and-form-factors-java-003[]

// tag::device-input-and-form-factors-java-004[]
canvas.addMouseWheelListener(e -> {
    WheelEvent w = (WheelEvent) e;
    if (w.isControlDown()) {
        zoom(w.getDeltaY());
        w.consume(); // do not scroll, we zoomed instead
    }
});
// end::device-input-and-form-factors-java-004[]

// tag::device-input-and-form-factors-java-005[]
Container photo = new Container() {
    protected boolean pinch(float scale) {
        zoom(scale);                 // trackpad pinch or two finger pinch
        return true;
    }
    protected boolean rotation(float radians) {
        rotate(radians);             // trackpad two finger rotate
        return true;
    }
};
// end::device-input-and-form-factors-java-005[]

// tag::device-input-and-form-factors-java-006[]
drawingArea.addStylusListener(e -> {
    PointerEvent pe = e.getPointerEvent();
    float width = 1f + pe.getPressure() * 9f; // pressure controls stroke width
    if (pe.isEraser()) {
        erase(pe.getX(), pe.getY());
    } else {
        drawPoint(pe.getX(), pe.getY(), width);
    }
});
// end::device-input-and-form-factors-java-006[]

// tag::device-input-and-form-factors-java-007[]
DevicePosture p = CN.getDevicePosture();
if (p.isFoldable() && p.isTableTop()) {
    // half-opened, hinge horizontal: put media on top, controls on the bottom half
    layoutForTableTop(p.getFoldBounds(null));
}
// end::device-input-and-form-factors-java-007[]

// tag::device-input-and-form-factors-java-008[]
CN.addPostureListener(e -> relayoutForPosture(CN.getDevicePosture()));
// end::device-input-and-form-factors-java-008[]

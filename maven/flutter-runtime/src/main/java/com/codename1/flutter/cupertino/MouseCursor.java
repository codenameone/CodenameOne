package com.codename1.flutter.cupertino;

/**
 * A pointer cursor kind — Flutter's {@code MouseCursor}. CN1 does not retarget
 * the desktop cursor per widget, so this is an opaque marker carried by
 * {@code MouseRegion(cursor:)} and never acted upon this pass.
 */
public class MouseCursor {

    /**
     * {@code MouseCursor.defer}: defers the cursor decision to the region behind
     * this one. An opaque marker in this runtime.
     */
    public static final MouseCursor defer = new MouseCursor("defer");

    private final String kind;

    public MouseCursor(String kind) {
        this.kind = kind;
    }

    public String kind() {
        return kind;
    }
}

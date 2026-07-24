package com.codename1.flutter.rendering;

import com.codename1.flutter.Offset;

/**
 * A render object laid out with the box protocol (a Cartesian size) — Flutter's
 * {@code RenderBox}. The transformations and reply studies read {@link #size()}
 * and map points through {@link #localToGlobal} / {@link #globalToLocal}. This
 * is a structural stub returning neutral geometry; a later rendering milestone
 * will back it with the live Codename One layout.
 */
public class RenderBox extends RenderObject {

    private Size size = Size.ZERO;

    /** The size of this box after layout. */
    public Size size() {
        return size;
    }

    /** Named setter used by the runtime once layout is known. */
    public void size(Size v) {
        this.size = v == null ? Size.ZERO : v;
    }

    /** Whether this box has been through layout and has a valid size. */
    public boolean hasSize() {
        return size != null;
    }

    /**
     * Converts a point from this box's local coordinate space to the global
     * (screen) space, optionally relative to {@code ancestor}. Identity in this
     * milestone.
     */
    public Offset localToGlobal(Offset point, RenderObject ancestor) {
        return point == null ? Offset.zero : point;
    }

    /**
     * Converts a point from global (screen) space to this box's local space,
     * optionally relative to {@code ancestor}. Identity in this milestone.
     */
    public Offset globalToLocal(Offset point, RenderObject ancestor) {
        return point == null ? Offset.zero : point;
    }

    /**
     * The transform mapping this object's coordinate space to {@code ancestor}
     * (a 4x4 matrix in Flutter). Returned opaque for this milestone.
     */
    public Object getTransformTo(RenderObject ancestor) {
        return null;
    }
}

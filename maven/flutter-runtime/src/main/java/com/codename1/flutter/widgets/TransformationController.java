package com.codename1.flutter.widgets;

import com.codename1.flutter.Offset;
import com.codename1.flutter.foundation.ValueNotifier;
import com.codename1.flutter.vectormath.Matrix4;

/**
 * The 4x4-matrix controller shared with an {@link InteractiveViewer} — Flutter's
 * {@code TransformationController} (a {@code ValueNotifier<Matrix4>}). The
 * transformations demo animates {@link #value()} and maps viewport points to the
 * child's coordinate space with {@link #toScene(Offset)}.
 */
public class TransformationController extends ValueNotifier<Matrix4> {

    public TransformationController() {
        super(Matrix4.identity());
    }

    public TransformationController(Matrix4 value) {
        super(value == null ? Matrix4.identity() : value);
    }

    /**
     * Maps a point from the viewport (widget) coordinate space to the child's
     * coordinate space. This pass returns the point unchanged (identity mapping)
     * until the live pan/zoom transform is applied.
     */
    public Offset toScene(Offset viewportPoint) {
        return viewportPoint;
    }
}

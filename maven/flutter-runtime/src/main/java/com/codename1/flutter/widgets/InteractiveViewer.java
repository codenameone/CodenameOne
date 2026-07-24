package com.codename1.flutter.widgets;

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * A pan/zoom viewport for its {@code child} — Flutter's {@code InteractiveViewer}.
 * Structural pass-through for this milestone: the {@code child} renders
 * unchanged; the {@link TransformationController} and interaction callbacks are
 * captured for a later render pass that applies the live matrix.
 */
public class InteractiveViewer extends Widget implements HasChild {

    private TransformationController transformationController;
    private EdgeInsets boundaryMargin;
    private double minScale;
    private double maxScale;
    private Boolean constrained;
    private Boolean panEnabled;
    private Boolean scaleEnabled;
    private double scaleFactor;
    private Object onInteractionStart;
    private Object onInteractionUpdate;
    private Object onInteractionEnd;
    private Object clipBehavior;
    private Boolean alignPanAxis;
    private Widget child;

    public void transformationController(TransformationController v) { this.transformationController = v; }
    public void boundaryMargin(EdgeInsets v) { this.boundaryMargin = v; }
    public void minScale(double v) { this.minScale = v; }
    public void maxScale(double v) { this.maxScale = v; }
    public void constrained(Boolean v) { this.constrained = v; }
    public void panEnabled(Boolean v) { this.panEnabled = v; }
    public void scaleEnabled(Boolean v) { this.scaleEnabled = v; }
    public void scaleFactor(double v) { this.scaleFactor = v; }
    public void onInteractionStart(dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.gestures.ScaleStartDetails> v) { this.onInteractionStart = v; }
    public void onInteractionUpdate(dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.gestures.ScaleUpdateDetails> v) { this.onInteractionUpdate = v; }
    public void onInteractionEnd(dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.gestures.ScaleEndDetails> v) { this.onInteractionEnd = v; }
    public void clipBehavior(Object v) { this.clipBehavior = v; }
    public void alignPanAxis(Boolean v) { this.alignPanAxis = v; }

    public void child(Widget v) {
        this.child = v;
    }

    public TransformationController getTransformationController() {
        return transformationController;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}

package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.CustomPainter;
import com.codename1.flutter.rendering.Size;

/**
 * Provides a canvas for a {@link CustomPainter} to paint on, behind and/or in
 * front of an optional {@code child} — Flutter's {@code CustomPaint}. This pass
 * renders the child (or reserves {@code size} when there is none); driving the
 * painter's {@code paint(Canvas, Size)} is deferred to the paint layer.
 */
public class CustomPaint extends StatelessWidget {

    private CustomPainter painter;
    private CustomPainter foregroundPainter;
    private Size size;
    private Widget child;

    public void painter(CustomPainter v) {
        this.painter = v;
    }

    public void foregroundPainter(CustomPainter v) {
        this.foregroundPainter = v;
    }

    public void size(Size v) {
        this.size = v;
    }

    public void isComplex(boolean v) {
    }

    public void willChange(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public CustomPainter getPainter() {
        return painter;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        if (child != null) {
            return child;
        }
        return new SizedBox();
    }
}

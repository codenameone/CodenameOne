package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.TextDirection;
import com.codename1.flutter.Widget;

/**
 * Establishes the reading direction for its subtree, mirroring Flutter's
 * {@code Directionality}. Layout-transparent in this runtime: it simply wraps
 * its child; the recorded {@link TextDirection} is available for later
 * bidi-aware rendering.
 */
public class Directionality extends Widget {

    private TextDirection textDirection;
    private Widget child;

    public void textDirection(TextDirection v) {
        this.textDirection = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public TextDirection getTextDirection() {
        return textDirection;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new DirectionalityRenderElement(this);
    }

    /**
     * Dart's {@code Directionality.of(context)}: the ambient text direction.
     * This runtime does not scope directionality through the element tree, so
     * the default LTR reading direction is reported.
     */
    public static TextDirection of(BuildContext context) {
        return TextDirection.ltr;
    }
}

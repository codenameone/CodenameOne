package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;

/**
 * Translates its {@code child} by an {@link Offset} expressed as a fraction of the child's
 * own size before painting — Flutter's {@code FractionalTranslation}.
 */
public class FractionalTranslation extends Widget
        implements FractionalTranslationRenderElement.FractionSource {

    private Offset translation;
    private boolean transformHitTests = true;
    private Widget child;

    public void translation(Offset v) {
        this.translation = v;
    }

    public void transformHitTests(boolean v) {
        this.transformHitTests = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Offset getTranslation() {
        return translation;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Offset fraction() {
        return translation;
    }

    @Override
    public Widget child() {
        return child;
    }

    @Override
    public com.codename1.flutter.foundation.Listenable driver() {
        return null;   // a static translation: nothing to follow
    }

    @Override
    public Element createElement() {
        return new FractionalTranslationRenderElement(this);
    }
}

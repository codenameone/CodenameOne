package com.codename1.flutter.animation;

import com.codename1.flutter.Element;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.FractionalTranslationRenderElement;

/**
 * Slides its child by an animated offset given as a FRACTION of the child's own size —
 * Flutter's {@code SlideTransition}.
 *
 * <p>It is its own render element rather than a wrapper around FractionalTranslation
 * because the fraction has to be read at PAINT time: the animation moves every frame, and
 * rebuilding a wrapper widget per frame to carry the new value would relayout the subtree
 * for what is only a change of where it is drawn.</p>
 */
public class SlideTransition extends AnimatedChildWidget
        implements FractionalTranslationRenderElement.FractionSource {

    private Animation<?> position;

    public void position(Animation<?> v) {
        this.position = v;
        listenable(v);
    }

    public Animation<?> getPosition() {
        return position;
    }

    @Override
    public Offset fraction() {
        if (position == null) {
            return null;
        }
        Object v = position.value();
        return v instanceof Offset ? (Offset) v : null;
    }

    @Override
    public Widget child() {
        return getChild();
    }

    @Override
    public com.codename1.flutter.foundation.Listenable driver() {
        return position;
    }

    @Override
    public Element createElement() {
        return new FractionalTranslationRenderElement(this);
    }
}

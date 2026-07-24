package com.codename1.flutter.animation;

/**
 * Slides its child by an animated fractional {@code Offset} — Flutter's
 * {@code SlideTransition}. The position animation carries an {@code Offset}
 * (opaque to this runtime); the child is hosted, the translation deferred.
 */
public class SlideTransition extends AnimatedChildWidget {

    private Animation<?> position;

    public void position(Animation<?> v) {
        this.position = v;
    }

    public Animation<?> getPosition() {
        return position;
    }
}

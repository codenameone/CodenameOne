package com.codename1.flutter.animation;

/**
 * Animates the position/size (a {@code RelativeRect}) of a child within a
 * Stack — Flutter's {@code PositionedTransition}. The child is hosted; the
 * animated placement is deferred.
 */
public class PositionedTransition extends AnimatedChildWidget {

    private Animation<?> rect;

    public void rect(Animation<?> v) {
        this.rect = v;
    }

    public Animation<?> getRect() {
        return rect;
    }
}

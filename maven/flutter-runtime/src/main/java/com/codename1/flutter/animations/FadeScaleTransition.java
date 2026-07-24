package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;

/**
 * Fades and scales its child in/out for modal reveals — the {@code animations}
 * package's {@code FadeScaleTransition}. Driven by {@code animation} (0 = hidden,
 * 1 = shown). This pass hosts the {@code child}; compositing the fade/scale is
 * deferred.
 */
public class FadeScaleTransition extends StatelessWidget {

    private Animation<Double> animation;
    private Widget child;

    public void animation(Animation<Double> v) {
        this.animation = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Animation<Double> getAnimation() {
        return animation;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}

package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;

/**
 * Cross-fades and slides between two pages along a shared axis — the
 * {@code animations} package's {@code SharedAxisTransition}. Driven by the
 * primary {@code animation} (incoming page) and {@code secondaryAnimation}
 * (outgoing page) with a direction given by {@link SharedAxisTransitionType}.
 * This pass hosts the {@code child}; compositing the fade/slide is deferred.
 */
public class SharedAxisTransition extends StatelessWidget {

    private Animation<Double> animation;
    private Animation<Double> secondaryAnimation;
    private SharedAxisTransitionType transitionType;
    private Color fillColor;
    private Widget child;

    public void animation(Animation<Double> v) {
        this.animation = v;
    }

    public void secondaryAnimation(Animation<Double> v) {
        this.secondaryAnimation = v;
    }

    public void transitionType(SharedAxisTransitionType v) {
        this.transitionType = v;
    }

    public void fillColor(Color v) {
        this.fillColor = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public SharedAxisTransitionType getTransitionType() {
        return transitionType;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}

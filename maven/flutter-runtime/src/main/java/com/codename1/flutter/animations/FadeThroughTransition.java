package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;

/**
 * Fades the outgoing child out then the incoming child in (Material shared-Z
 * motion) — the {@code animations} package's {@code FadeThroughTransition}.
 * This pass hosts the {@code child}; compositing the fade is deferred.
 */
public class FadeThroughTransition extends StatelessWidget {

    private Animation<Double> animation;
    private Animation<Double> secondaryAnimation;
    private Color fillColor;
    private Widget child;

    public void animation(Animation<Double> v) { this.animation = v; }
    public void secondaryAnimation(Animation<Double> v) { this.secondaryAnimation = v; }
    public void fillColor(Color v) { this.fillColor = v; }
    public void child(Widget v) { this.child = v; }

    public Widget getChild() { return child; }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}

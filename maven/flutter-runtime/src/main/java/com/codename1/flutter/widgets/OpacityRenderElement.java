package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Paints {@link Opacity}'s subtree at its opacity, by compositing the whole nested pane
 * through the Graphics alpha rather than tinting components individually — so overlapping
 * children fade as one layer, the way Flutter's Opacity behaves.
 */
public class OpacityRenderElement extends EffectRenderElement {

    public OpacityRenderElement(Opacity widget) {
        super(widget);
    }

    private Opacity opacity() {
        return (Opacity) widget();
    }

    @Override
    protected Widget effectChild() {
        return opacity().getChild();
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Runnable paintChildren) {
        double o = opacity().getOpacity();
        if (o >= 1.0) {
            paintChildren.run();
            return;
        }
        if (o <= 0.0) {
            return;   // fully transparent: painting anything would be wrong
        }
        int previous = g.getAlpha();
        // Compose with the alpha already in effect, so nested Opacity multiplies.
        g.setAlpha((int) Math.round(previous * o));
        try {
            paintChildren.run();
        } finally {
            g.setAlpha(previous);
        }
    }
}

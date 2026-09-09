package com.codename1.flutter.widgets;

import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

/**
 * Paints a subtree shifted by an offset expressed as a FRACTION OF ITS OWN SIZE — Flutter's
 * {@code FractionalTranslation}, and the mechanism behind {@code SlideTransition}.
 *
 * <p>Like Flutter this is a paint effect: the child is laid out where it belongs and only
 * the painting moves, so a sliding page does not disturb the layout around it. The fraction
 * is resolved against the laid-out size, which is why it must happen here rather than in the
 * widget — the size is not known until layout has run.</p>
 *
 * <p>Both widgets previously reported the translation as unimplemented and drew the child in
 * place, which turned every slide in the app into a jump.</p>
 */
public class FractionalTranslationRenderElement extends EffectRenderElement {

    /** Resolves the current fractional offset — the widget's, or an animation's. */
    public interface FractionSource {
        Offset fraction();

        Widget child();

        /**
         * The animation to repaint with, or null for a static translation. Repainting is
         * enough: the fraction is read at paint time and the child's layout never moves,
         * so a tick must not cost a layout pass.
         */
        com.codename1.flutter.foundation.Listenable driver();
    }

    private com.codename1.flutter.foundation.Listenable listened;
    private final dart.runtime.Funcs.VoidFunc0 repaint = new dart.runtime.Funcs.VoidFunc0() {
        @Override
        public void call() {
            markNeedsPaint();
        }
    };

    public FractionalTranslationRenderElement(Widget widget) {
        super(widget);
    }

    /**
     * The CURRENT configuration. Read through {@code widget()} rather than captured at
     * construction: a rebuild swaps the widget, and a captured one would keep reporting the
     * offset and the animation of a configuration that is no longer on screen.
     */
    private FractionSource source() {
        Widget w = widget();
        return w instanceof FractionSource ? (FractionSource) w : null;
    }

    @Override
    public void mount(com.codename1.flutter.Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
        unsubscribe();
        super.update(newWidget);
        subscribe();
    }

    @Override
    public void unmount() {
        unsubscribe();
        super.unmount();
    }

    private void subscribe() {
        FractionSource src = source();
        listened = src == null ? null : src.driver();
        if (listened != null) {
            listened.addListener(repaint);
        }
    }

    private void unsubscribe() {
        if (listened != null) {
            listened.removeListener(repaint);
            listened = null;
        }
    }

    @Override
    protected Widget effectChild() {
        FractionSource src = source();
        return src == null ? null : src.child();
    }

    @Override
    protected void paintWithEffect(Graphics g, Container pane, Subtree paintChildren) {
        FractionSource src = source();
        Offset f = src == null ? null : src.fraction();
        if (f == null || (f.dx() == 0 && f.dy() == 0)) {
            paintChildren.paint(g);
            return;
        }
        int dx = (int) Math.round(f.dx() * pane.getWidth());
        int dy = (int) Math.round(f.dy() * pane.getHeight());
        if (dx == 0 && dy == 0) {
            paintChildren.paint(g);
            return;
        }
        g.translate(dx, dy);
        try {
            paintChildren.paint(g);
        } finally {
            g.translate(-dx, -dy);
        }
    }
}

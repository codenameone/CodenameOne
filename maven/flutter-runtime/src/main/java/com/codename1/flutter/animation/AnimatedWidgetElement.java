package com.codename1.flutter.animation;

import com.codename1.flutter.StatelessElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.Listenable;

import dart.runtime.Funcs;

/**
 * Element for {@link AnimatedWidget}: listens to the widget's {@code listenable} and rebuilds
 * on every notification — Flutter's {@code _AnimatedState}.
 *
 * <p>Without this an AnimatedWidget is a plain StatelessWidget that happens to read an
 * animation: it samples the value once, paints a correct first frame, and never moves again.
 * The gallery's settings button is exactly that shape — it renders
 * {@code SettingsIcon(animationController.value)}, so a frozen subscription leaves the icon
 * stuck on whichever glyph it was built with while the panel behind it opens and closes.</p>
 *
 * <p>{@link AnimatedBuilderElement} already did this for the builder-callback form; the
 * subclass form went without, which is why three of the gallery's widgets — this icon,
 * Shrine's backdrop title and Rally's pie chart — were all still.</p>
 */
public class AnimatedWidgetElement extends StatelessElement {

    private Listenable listened;
    private final Funcs.VoidFunc0 handler = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            markNeedsBuild();
        }
    };

    public AnimatedWidgetElement(AnimatedWidget widget) {
        super(widget);
    }

    @Override
    public void mount(com.codename1.flutter.Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
        // Resubscribed around the swap: a rebuilt widget may carry a DIFFERENT listenable,
        // and holding the old one leaks a listener onto a controller that outlives us.
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
        Widget w = widget();
        if (!(w instanceof AnimatedWidget)) {
            return;
        }
        listened = ((AnimatedWidget) w).listenable();
        if (listened != null) {
            listened.addListener(handler);
        }
    }

    private void unsubscribe() {
        if (listened != null) {
            listened.removeListener(handler);
            listened = null;
        }
    }
}

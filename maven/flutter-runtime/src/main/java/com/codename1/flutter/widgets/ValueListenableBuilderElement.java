package com.codename1.flutter.widgets;

import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.ValueListenable;

import dart.runtime.Funcs;

/**
 * Element for {@link ValueListenableBuilder}: subscribes to the listenable on mount and
 * rebuilds on every notification, mirroring Flutter's
 * {@code _ValueListenableBuilderState}.
 *
 * <p>Without the subscription the builder still produced a correct FIRST frame, which is
 * why this looked like it worked: the widget rendered, and only stopped tracking after
 * that. Everything driven by a ValueNotifier was therefore frozen at its initial value —
 * in the gallery, the settings button toggled its notifier and nothing on screen moved,
 * so the whole settings panel was unreachable.</p>
 */
public class ValueListenableBuilderElement extends ComposedElement {

    private ValueListenable<Object> listened;

    private final Funcs.VoidFunc0 handler = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            markNeedsBuild();
        }
    };

    public ValueListenableBuilderElement(ValueListenableBuilder<?> widget) {
        super(widget);
    }

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        subscribe();
    }

    @Override
    public void update(Widget newWidget) {
        // The new configuration may name a DIFFERENT listenable; resubscribing
        // unconditionally is simpler than comparing and cannot leave a stale listener
        // attached to the old one.
        unsubscribe();
        super.update(newWidget);
        subscribe();
    }

    @Override
    public void unmount() {
        unsubscribe();
        super.unmount();
    }

    @SuppressWarnings("unchecked")
    private void subscribe() {
        Object l = ((ValueListenableBuilder<?>) widget()).getValueListenable();
        if (l instanceof ValueListenable) {
            listened = (ValueListenable<Object>) l;
            listened.addListener(handler);
        }
    }

    private void unsubscribe() {
        if (listened != null) {
            listened.removeListener(handler);
            listened = null;
        }
    }

    @Override
    protected Widget build() {
        return ((ValueListenableBuilder<?>) widget()).buildWith(this);
    }
}

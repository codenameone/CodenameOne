package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.ValueNotifier;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A ValueListenableBuilder has to LISTEN. Reading the value once at build time produces a
 * correct first frame and then freezes, which is the failure mode that hides best: the
 * widget renders, so it looks wired up, and only never updates again.
 *
 * <p>In the gallery that froze the whole settings panel — the button flipped its
 * ValueNotifier and nothing on screen reacted, so the menu appeared to do nothing.</p>
 */
class ValueListenableBuilderTest {

    private BuildOwner owner;

    private ValueListenableBuilder<Object> builderOn(final ValueNotifier<Object> notifier,
            final int[] builds) {
        ValueListenableBuilder<Object> b = new ValueListenableBuilder<Object>();
        b.valueListenable(notifier);
        b.builder(new Funcs.Func3<BuildContext, Object, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, Object value, Widget child) {
                builds[0]++;
                // Size the box from the value, so the rebuild is observable as geometry
                // and not merely as a counter.
                int v = value instanceof Number ? ((Number) value).intValue() : 0;
                return new ProbeBox(v, v);
            }
        });
        return b;
    }

    private com.codename1.flutter.Element mount(Widget root) {
        owner = new BuildOwner();
        return FlutterUI.mount(root, new RenderHost(), owner);
    }

    @Test
    void theBuilderRunsOnceForTheFirstFrame() {
        int[] builds = {0};
        ValueNotifier<Object> n = new ValueNotifier<Object>(Integer.valueOf(1));
        mount(builderOn(n, builds));

        assertEquals(1, builds[0]);
    }

    @Test
    void changingTheValueRebuilds() {
        int[] builds = {0};
        ValueNotifier<Object> n = new ValueNotifier<Object>(Integer.valueOf(1));
        mount(builderOn(n, builds));

        n.value(Integer.valueOf(2));
        owner.flushSync();

        assertEquals(2, builds[0], "a value change must re-invoke the builder");
    }

    @Test
    void theBuilderSeesTheNewValue() {
        final Object[] seen = new Object[1];
        ValueNotifier<Object> n = new ValueNotifier<Object>(Integer.valueOf(1));
        ValueListenableBuilder<Object> b = new ValueListenableBuilder<Object>();
        b.valueListenable(n);
        b.builder(new Funcs.Func3<BuildContext, Object, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, Object value, Widget child) {
                seen[0] = value;
                return new ProbeBox(1, 1);
            }
        });
        mount(b);

        n.value(Integer.valueOf(42));
        owner.flushSync();

        assertEquals(Integer.valueOf(42), seen[0]);
    }

    @Test
    void anUnmountedBuilderStopsListening() {
        int[] builds = {0};
        ValueNotifier<Object> n = new ValueNotifier<Object>(Integer.valueOf(1));
        com.codename1.flutter.Element root = mount(builderOn(n, builds));

        FlutterUI.unmountTree(root);
        int atUnmount = builds[0];

        n.value(Integer.valueOf(2));
        owner.flushSync();

        assertEquals(atUnmount, builds[0],
                "an unmounted builder must have removed its listener");
    }
}

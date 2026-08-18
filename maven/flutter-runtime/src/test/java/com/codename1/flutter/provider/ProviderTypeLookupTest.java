package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Consumer/Selector reads the model of the type it ASKED FOR, not the nearest one.
 *
 * <p>Java erases {@code Consumer<T>}, so the runtime takes the type as a token the transpiler
 * emits. Without it the lookup returned the nearest provided value of any type — right only
 * when a single model is in scope. Reply has its localizations and its EmailStore above the
 * same Selector: it got the localizations and the study came up blank — a cast error on the
 * desktop, and on iOS, where that cast is unchecked, a wrong object that flowed on until an
 * unrelated switch matched nothing.</p>
 */
class ProviderTypeLookupTest {

    static class Localizations {
    }

    static class EmailStore {
        final String name = "store";
    }

    /** Two models in scope with the WRONG one nearer — the Reply arrangement. */
    private Widget twoProviders(Widget leaf) {
        Provider inner = new Provider();
        inner.value(new Localizations());
        inner.child(leaf);
        Provider outer = new Provider();
        outer.value(new EmailStore());
        outer.child(inner);
        return outer;
    }

    @Test
    @DisplayName("a Consumer that names its type skips the nearer, wrong model")
    void consumerFindsItsOwnType() {
        final Object[] seen = new Object[1];
        Consumer<EmailStore> c = new Consumer<EmailStore>();
        c.providedType(EmailStore.class);
        c.builder(new Funcs.Func3<BuildContext, EmailStore, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, EmailStore value, Widget child) {
                seen[0] = value;
                return new ProbeBox(1, 1);
            }
        });

        FlutterUI.mount(twoProviders(c), new RenderHost(), new BuildOwner());

        assertTrue(seen[0] instanceof EmailStore, "expected the EmailStore, got " + seen[0]);
    }

    @Test
    @DisplayName("a Selector that names its type skips the nearer, wrong model")
    void selectorFindsItsOwnType() {
        final Object[] seen = new Object[1];
        Selector<EmailStore, String> s = new Selector<EmailStore, String>();
        s.providedType(EmailStore.class);
        s.selector(new Funcs.Func2<BuildContext, EmailStore, String>() {
            @Override
            public String call(BuildContext context, EmailStore store) {
                seen[0] = store;
                return store.name;
            }
        });
        s.builder(new Funcs.Func3<BuildContext, String, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, String value, Widget child) {
                return new ProbeBox(1, 1);
            }
        });

        FlutterUI.mount(twoProviders(s), new RenderHost(), new BuildOwner());

        assertTrue(seen[0] instanceof EmailStore, "expected the EmailStore, got " + seen[0]);
    }

    @Test
    @DisplayName("with no type named it still takes the nearest - the old behaviour")
    void anUnnamedTypeTakesTheNearest() {
        final Object[] seen = new Object[1];
        Consumer<Object> c = new Consumer<Object>();
        c.builder(new Funcs.Func3<BuildContext, Object, Widget, Widget>() {
            @Override
            public Widget call(BuildContext context, Object value, Widget child) {
                seen[0] = value;
                return new ProbeBox(1, 1);
            }
        });

        FlutterUI.mount(twoProviders(c), new RenderHost(), new BuildOwner());

        assertTrue(seen[0] instanceof Localizations,
                "the default is still nearest-wins, got " + seen[0]);
    }
}

package com.codename1.flutter.navigation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.provider.SingleChildWidget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A route mounts as its own element-tree root (its own Form), but in Flutter a
 * route's page builds below the app and inherits everything above it. The
 * context fallback restores that: ancestor lookups continue from the context
 * that pushed the route.
 */
class RouteInheritanceTest {

    /** A value published to a subtree, like MaterialApp's localizations scope. */
    private static class Scope extends SingleChildWidget implements InheritedValueProvider {
        private final String value;

        Scope(String value) {
            this.value = value;
        }

        @Override
        public Object providedValueFor(Class<?> type) {
            return type == String.class ? value : null;
        }
    }

    /** Records what its context could see at build time. */
    private static class Probe extends StatelessWidget {
        String seen;
        Element context;

        @Override
        public Widget build(BuildContext context) {
            this.context = (Element) context;
            seen = (String) context.providerValueOfType(String.class);
            return new ProbeBox(1, 1);
        }
    }

    private static Element mountAppWithScope(String value, Probe inApp) {
        Scope scope = new Scope(value);
        scope.child(inApp);
        return FlutterUI.mount(scope, new RenderHost(), new BuildOwner());
    }

    @Test
    void aFreshRootSeesNothingFromTheAppTree() {
        Probe inApp = new Probe();
        mountAppWithScope("app-value", inApp);
        assertSame("app-value", inApp.seen, "sanity: the app's own subtree sees the scope");

        Probe orphan = new Probe();
        FlutterUI.mount(orphan, new RenderHost(), new BuildOwner());
        assertNull(orphan.seen, "an unlinked root has no ancestors to inherit from");
    }

    @Test
    void aRouteRootInheritsThroughTheContextThatPushedIt() {
        Probe inApp = new Probe();
        mountAppWithScope("app-value", inApp);

        // the element that would call Navigator.of(context).push(...)
        Element pusher = elementOf(inApp);

        Probe page = new Probe();
        FlutterUI.mount(page, new RenderHost(), new BuildOwner(), pusher);

        assertSame("app-value", page.seen,
                "the pushed page resolves the app's scope through the pushing context");
    }

    @Test
    void theFallbackIsOnlyForLookups_notStructure() {
        Probe inApp = new Probe();
        mountAppWithScope("app-value", inApp);

        Probe page = new Probe();
        Element root = FlutterUI.mount(page, new RenderHost(), new BuildOwner(), elementOf(inApp));

        assertNull(root.parent(),
                "a route root stays a genuine root — render/host logic must not see it as embedded");
    }

    /** The element built for {@code w}: the probe records its own context. */
    private static Element elementOf(Probe w) {
        return w.context;
    }
}

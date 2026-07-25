package com.codename1.flutter.navigation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * The Flutter route stack, mapped onto CN1 Forms: every {@link #push} mounts
 * the route's widget tree in a NEW Form (the same mounting pattern as
 * {@code FlutterUI.runApp}) and shows it; {@link #pop} unmounts that route's
 * element tree and returns to the previous Form via {@code showBack()}.
 *
 * <p>M3 keeps ONE static stack per app process. The base route (the
 * {@code runApp} form) is implicit and never on the stack, so popping with
 * an empty stack — popping the last route — is a no-op. {@code pop} first
 * consults the dialog stack ({@link Dialogs}): a {@code Navigator.pop}
 * inside an open dialog's action dismisses that dialog, matching Flutter's
 * dialogs-are-routes behavior.</p>
 *
 * <p>When the pushed tree's root is a {@link
 * com.codename1.flutter.material.Scaffold} its root-mode mounting binds a
 * CN1 Toolbar to the new Form; the navigator then wires the Toolbar's BACK
 * arrow to {@code pop}.</p>
 *
 * <p>Headless (no Display): the stack bookkeeping still runs — no Forms are
 * created and the route's builder is not invoked (it would run on mount).</p>
 */
public class Navigator extends StatelessWidget {

    private static final List<RouteEntry> stack = new ArrayList<RouteEntry>();

    // --- Nested Navigator widget (Flutter's embeddable Navigator) --------------
    // A Navigator can also be used AS a widget (Reply's mail navigator, several
    // demos): it owns a private route table via onGenerateRoute/initialRoute. This
    // pass records the configuration and renders the initial route's page so the
    // subtree has content; the private route stack is deferred.
    private Object navigatorKey;
    private String initialRoute;
    private dart.runtime.Funcs.Func1<RouteSettings, Object> onGenerateRoute;
    private dart.runtime.Funcs.Func1<RouteSettings, Object> onUnknownRoute;
    private String restorationScopeId;
    private Object observers;
    private Object pages;
    private Object onPopPage;

    public Navigator() {
    }

    public Navigator(com.codename1.flutter.Key key) {
        key(key);
    }

    public void navigatorKey(Object v) {
        this.navigatorKey = v;
    }

    public void initialRoute(String v) {
        this.initialRoute = v;
    }

    public void onGenerateRoute(dart.runtime.Funcs.Func1<RouteSettings, Object> v) {
        this.onGenerateRoute = v;
    }

    public void onUnknownRoute(dart.runtime.Funcs.Func1<RouteSettings, Object> v) {
        this.onUnknownRoute = v;
    }

    public void restorationScopeId(String v) {
        this.restorationScopeId = v;
    }

    public void observers(Object v) {
        this.observers = v;
    }

    public void pages(Object v) {
        this.pages = v;
    }

    public void onPopPage(Object v) {
        this.onPopPage = v;
    }

    @Override
    public com.codename1.flutter.Widget build(com.codename1.flutter.BuildContext context) {
        if (onGenerateRoute != null) {
            RouteSettings settings = new RouteSettings();
            settings.name(initialRoute);
            Object route = onGenerateRoute.call(settings);
            if (route instanceof MaterialPageRoute) {
                dart.runtime.Funcs.Func1<com.codename1.flutter.BuildContext,
                        com.codename1.flutter.Widget> b = ((MaterialPageRoute) route).getBuilder();
                if (b != null) {
                    return b.call(context);
                }
            }
        }
        return null;
    }

    /**
     * Pushes the route: builds its page in a new Form and shows it.
     */
    public static void push(BuildContext context, MaterialPageRoute route) {
        RouteEntry e = new RouteEntry(route);
        if (Display.isInitialized()) {
            e.previousForm = Display.getInstance().getCurrent();
            RenderHost host = FlutterUI.mountInNewForm(new RouteWidget(route), pushingElement(context));
            e.form = host.form();
            e.rootElement = host.rootElement();
            Toolbar tb = e.form.getToolbar();
            if (tb != null && host.isFormToolbarBound()) {
                // a root Scaffold bound its AppBar to the Form Toolbar; give
                // that bar the material back arrow
                tb.setBackCommand("", new ActionListener<ActionEvent>() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        pop(null);
                    }
                });
            } else if (tb != null) {
                // The page draws its own AppBar in-canvas (a Scaffold nested
                // below another render widget, as the gallery's demo pages
                // are). Showing the Form's Toolbar too would put two bars on
                // the page AND shrink the Flutter canvas by the toolbar inset,
                // which is what left demo pages floating inside a margin.
                tb.setVisible(false);
                tb.setHidden(true);
            }
            stack.add(e);
            e.form.show();
        } else {
            stack.add(e);
        }
    }

    /**
     * Pops the topmost dialog if one is open, else the topmost pushed route.
     * Popping the last (base) route is a no-op.
     */
    public static void pop(BuildContext context) {
        if (Dialogs.popTopDialog()) {
            return;
        }
        if (stack.isEmpty()) {
            // the base runApp route: never popped
            return;
        }
        RouteEntry e = stack.remove(stack.size() - 1);
        if (e.rootElement != null) {
            FlutterUI.unmountTree(e.rootElement);
        }
        if (e.previousForm != null) {
            e.previousForm.showBack();
        }
    }

    /**
     * The number of pushed routes (the implicit base route not included).
     */
    public static int stackSize() {
        return stack.size();
    }

    // --- Named routes ---------------------------------------------------------
    // Flutter resolves a route NAME against the app's table: the `routes` map
    // first, then onGenerateRoute, then onUnknownRoute. MaterialApp publishes
    // its table here on build so a pushNamed from anywhere below can resolve it
    // — the gallery reaches every one of its demos this way
    // (Navigator.of(context).restorablePushNamed('/demo/<slug>')).

    private static Object routesTable;
    private static Funcs.Func1<RouteSettings, Route> generateRoute;
    private static Funcs.Func1<RouteSettings, Route> unknownRoute;

    /** Publishes the app's route table; called by MaterialApp on build. */
    public static void installRouteTable(Object routes,
            Funcs.Func1<RouteSettings, Route> onGenerateRoute,
            Funcs.Func1<RouteSettings, Route> onUnknownRoute) {
        routesTable = routes;
        generateRoute = onGenerateRoute;
        unknownRoute = onUnknownRoute;
    }

    /** Test / hot-restart hook: forgets the installed route table. */
    public static void resetRouteTable() {
        routesTable = null;
        generateRoute = null;
        unknownRoute = null;
    }

    /**
     * Resolves a route name the way Flutter does — the {@code routes} map
     * first, then {@code onGenerateRoute}, then {@code onUnknownRoute} — or
     * null when nothing claims the name.
     */
    @SuppressWarnings("unchecked")
    public static Route resolveRoute(String name, Object arguments) {
        RouteSettings settings = new RouteSettings();
        settings.name(name);
        settings.arguments(arguments);

        if (routesTable instanceof java.util.Map) {
            Object builder = ((java.util.Map<Object, Object>) routesTable).get(name);
            if (builder instanceof Funcs.Func1) {
                MaterialPageRoute<Object> route = new MaterialPageRoute<Object>();
                route.builder((Funcs.Func1<BuildContext, Widget>) builder);
                route.settings(settings);
                return route;
            }
        }
        Route r = generateRoute != null ? generateRoute.call(settings) : null;
        if (r == null && unknownRoute != null) {
            r = unknownRoute.call(settings);
        }
        return r;
    }

    /**
     * Resolves a route name and pushes it, returning whether anything was
     * pushed. An unresolvable name is logged rather than thrown: a dead link
     * in one corner of an app should not take the app down.
     */
    public static boolean pushNamed(BuildContext context, String name, Object arguments) {
        Route route = resolveRoute(name, arguments);
        if (route instanceof MaterialPageRoute) {
            push(context, (MaterialPageRoute) route);
            return true;
        }
        try {
            com.codename1.io.Log.p("Flutter runtime: no route for '" + name + "'"
                    + (route == null ? "" : " (unsupported route type " + route.getClass().getName() + ")"));
        } catch (Throwable t) {
            // headless: Log has no storage backend
        }
        return false;
    }

    /**
     * Test / hot-restart hook: forgets all pushed routes without unmounting.
     */
    public static void reset() {
        stack.clear();
    }

    /**
     * The single navigator handle for this process. Its {@code pop} delegates
     * to {@link Navigator#pop}; the restoration-push helpers are no-ops that
     * return an informational id (restoration is not persisted).
     */
    private static final NavigatorState STATE = new NavigatorState() {
        @Override
        public void pop(Object result) {
            Navigator.pop(null);
        }

        @Override
        public Object pushNamed(String routeName, Object arguments) {
            Navigator.pushNamed(null, routeName, arguments);
            return null;
        }

        @Override
        public String restorablePushNamed(String routeName, Object arguments) {
            Navigator.pushNamed(null, routeName, arguments);
            // restoration is not persisted; the id is informational
            return routeName == null ? "" : routeName;
        }

        @Override
        public Object pushReplacementNamed(String routeName, Object arguments, Object result) {
            Navigator.pop(null);
            Navigator.pushNamed(null, routeName, arguments);
            return null;
        }

        @Override
        public Object push(Object route) {
            if (route instanceof MaterialPageRoute) {
                Navigator.push(null, (MaterialPageRoute) route);
            }
            return null;
        }

        @Override
        public String restorablePush(
                Funcs.Func2<BuildContext, Object, Object> routeBuilder, Object arguments) {
            return Navigator.restorablePush(null, routeBuilder, arguments);
        }

        @Override
        public Object maybePop(Object result) {
            return Boolean.valueOf(Navigator.maybePop(null));
        }

        @Override
        public boolean canPop() {
            return !stack.isEmpty();
        }

        @Override
        public void popUntil(Funcs.Func1<Route<Object>, Boolean> predicate) {
            // Pop down to the first route the predicate accepts. With the base
            // runApp route off the stack, an always-false predicate unwinds to it.
            while (!stack.isEmpty()) {
                Route<Object> top = (Route<Object>) stack.get(stack.size() - 1).route;
                Boolean stop = predicate == null ? null : predicate.call(top);
                if (stop != null && stop.booleanValue()) {
                    return;
                }
                Navigator.pop(null);
            }
        }
    };

    /**
     * The nearest navigator's mutable state ({@code Navigator.of(context)}).
     * There is one navigator per process, but the handle REMEMBERS the calling
     * context: a route pushed through it inherits that context's scopes (see
     * {@code Element.contextFallback}), which is what makes Theme.of and
     * Localizations.of resolve inside the pushed page.
     */
    public static NavigatorState of(BuildContext context, Boolean rootNavigator) {
        if (context == null) {
            return STATE;
        }
        return new BoundState(context);
    }

    /** The element a push should inherit from, or null when unknown. */
    private static com.codename1.flutter.Element pushingElement(BuildContext context) {
        return context instanceof com.codename1.flutter.Element
                ? (com.codename1.flutter.Element) context : null;
    }

    /** A {@link NavigatorState} that pushes on behalf of a specific context. */
    private static final class BoundState extends NavigatorState {

        private final BuildContext context;

        BoundState(BuildContext context) {
            this.context = context;
        }

        @Override
        public void pop(Object result) {
            Navigator.pop(context);
        }

        @Override
        public Object pushNamed(String routeName, Object arguments) {
            Navigator.pushNamed(context, routeName, arguments);
            return null;
        }

        @Override
        public String restorablePushNamed(String routeName, Object arguments) {
            Navigator.pushNamed(context, routeName, arguments);
            return routeName == null ? "" : routeName;
        }

        @Override
        public Object pushReplacementNamed(String routeName, Object arguments, Object result) {
            Navigator.pop(context);
            Navigator.pushNamed(context, routeName, arguments);
            return null;
        }

        @Override
        public Object push(Object route) {
            if (route instanceof MaterialPageRoute) {
                Navigator.push(context, (MaterialPageRoute) route);
            }
            return null;
        }

        @Override
        public String restorablePush(
                Funcs.Func2<BuildContext, Object, Object> routeBuilder, Object arguments) {
            return Navigator.restorablePush(context, routeBuilder, arguments);
        }

        @Override
        public Object maybePop(Object result) {
            return Boolean.valueOf(Navigator.maybePop(context));
        }

        @Override
        public boolean canPop() {
            return !stack.isEmpty();
        }

        @Override
        public void popUntil(Funcs.Func1<Route<Object>, Boolean> predicate) {
            STATE.popUntil(predicate);
        }
    }

    /**
     * Restoration-aware push. Restoration is not persisted here, so the route
     * is pushed immediately when it is a {@link MaterialPageRoute} and an empty
     * (informational) restoration id is returned.
     */
    public static String restorablePush(BuildContext context,
            dart.runtime.Funcs.Func2<BuildContext, Object, Object> routeBuilder, Object arguments) {
        Object route = routeBuilder != null ? routeBuilder.call(context, arguments) : null;
        if (route instanceof MaterialPageRoute) {
            push(context, (MaterialPageRoute) route);
        }
        return "";
    }

    /**
     * Pops the topmost route if one exists, returning whether anything was
     * popped ({@code Navigator.maybePop}).
     */
    public static boolean maybePop(BuildContext context) {
        if (stack.isEmpty()) {
            return false;
        }
        pop(context);
        return true;
    }

    private static final class RouteEntry {
        final MaterialPageRoute route;
        Form form;
        Form previousForm;
        Element rootElement;

        RouteEntry(MaterialPageRoute route) {
            this.route = route;
        }
    }

    /**
     * Adapter mounting a route's WidgetBuilder as a widget tree root: the
     * builder runs during the first build, receiving a BuildContext that
     * lives inside the new page's element tree.
     */
    static final class RouteWidget extends StatelessWidget {

        private final MaterialPageRoute route;

        RouteWidget(MaterialPageRoute route) {
            this.route = route;
        }

        @Override
        public Widget build(BuildContext context) {
            Funcs.Func1<BuildContext, Widget> b = route.getBuilder();
            return b == null ? null : b.call(context);
        }
    }
}

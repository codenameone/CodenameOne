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
            RenderHost host = FlutterUI.mountInNewForm(new RouteWidget(route));
            e.form = host.form();
            e.rootElement = host.rootElement();
            Toolbar tb = e.form.getToolbar();
            if (tb != null) {
                // a root Scaffold bound itself to the Form Toolbar; give it
                // the material back arrow
                tb.setBackCommand("", new ActionListener<ActionEvent>() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        pop(null);
                    }
                });
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
    };

    /**
     * The nearest navigator's mutable state ({@code Navigator.of(context)}).
     * There is one navigator per process, so the handle is context-independent.
     */
    public static NavigatorState of(BuildContext context, Boolean rootNavigator) {
        return STATE;
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

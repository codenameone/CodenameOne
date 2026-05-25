/// In-app navigation API on top of the declarative `@Route` table.
///
/// `Navigation` is the imperative counterpart to the `Route` annotation:
/// declare your forms with `@Route("/users/:id")` once, then trigger
/// navigation from anywhere with `Navigation.navigate("/users/42")`. The same
/// route table that handles deep links is reused, so there is exactly one
/// place that knows how `/users/:id` maps to a form.
///
/// The class also exposes the navigation stack so applications can render
/// breadcrumb UIs without maintaining a parallel history:
///
/// ```java
/// Container breadcrumbs = new Container(BoxLayout.x());
/// for (final NavigationEntry e : Navigation.getStack()) {
///     Button crumb = new Button(e.getTitle());
///     crumb.addActionListener(evt -> Navigation.popTo(e));
///     breadcrumbs.add(crumb);
/// }
/// ```
///
/// The surface is intentionally tiny -- five static methods and one value
/// type. Applications that prefer raw `Form#show` / `Form#showBack` keep
/// working unchanged; the `Navigation` stack only records URL-driven
/// navigations.
///
/// All methods must be called on the EDT.
package com.codename1.router;

import com.codename1.ui.Display;
import com.codename1.ui.Form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Navigation {

    private static RouteDispatcher dispatcher;
    private static final List<NavigationEntry> stack = new ArrayList<NavigationEntry>();

    private Navigation() {
    }

    // ------------------------------------------------------------------------
    // Internal: dispatcher installation
    // ------------------------------------------------------------------------

    /// Installs the build-time-generated route dispatcher. Invoked once by
    /// `com.codename1.router.generated.Routes#bootstrap` during framework
    /// initialization. Application code should not call this.
    public static void setDispatcher(RouteDispatcher d) {
        dispatcher = d;
    }

    // ------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------

    /// Navigate to a path. Looks the URL up in the route table generated from
    /// `@Route` annotations, builds the matching `Form`, pushes it onto the
    /// navigation stack, and shows it.
    ///
    /// Accepts either a bare path (`/users/42`), a full URL with scheme +
    /// host (`https://example.com/users/42`), or a custom-scheme URL. Scheme
    /// and host are ignored -- only the path + query are matched.
    ///
    /// Returns `true` when a route matched and the form was shown, `false`
    /// when no route matched.
    public static boolean navigate(String path) {
        RouteDispatcher d = dispatcher;
        if (d == null || path == null) {
            return false;
        }
        Form f;
        try {
            f = d.dispatch(path);
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
            return false;
        }
        if (f == null) {
            return false;
        }
        stack.add(new NavigationEntry(path, f));
        f.show();
        return true;
    }

    /// Pop the top entry off the navigation stack and return to the previous
    /// one. Uses `Form#showBack` so the transition runs in reverse. Returns
    /// `true` when a frame was popped, `false` when the stack had at most one
    /// entry (already at the root, nothing to go back to).
    public static boolean back() {
        if (stack.size() <= 1) {
            return false;
        }
        stack.remove(stack.size() - 1);
        NavigationEntry now = stack.get(stack.size() - 1);
        now.getForm().showBack();
        return true;
    }

    /// The current entry (top of stack), or null when the stack is empty.
    public static NavigationEntry getCurrent() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /// Unmodifiable snapshot of the navigation stack, oldest entry first
    /// (breadcrumb order). The list is a copy: mutating navigations after
    /// the call do not affect it.
    public static List<NavigationEntry> getStack() {
        return Collections.unmodifiableList(new ArrayList<NavigationEntry>(stack));
    }

    /// Pop entries until `entry` is on top, then show its form via
    /// `Form#showBack`. Returns `true` when the entry was on the stack and
    /// we navigated back to it, `false` when the entry is not on the stack.
    /// Calling with the current entry is a no-op that returns `true`.
    public static boolean popTo(NavigationEntry entry) {
        if (entry == null) {
            return false;
        }
        // NavigationEntry doesn't override equals, so entry.equals(other) is
        // reference equality -- which is what we want here. Two navigations to
        // the same path are independent stack frames.
        int idx = -1;
        for (int i = 0; i < stack.size(); i++) {
            if (entry.equals(stack.get(i))) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return false;
        }
        if (idx == stack.size() - 1) {
            return true;
        }
        while (stack.size() > idx + 1) {
            stack.remove(stack.size() - 1);
        }
        entry.getForm().showBack();
        return true;
    }

    // ------------------------------------------------------------------------
    // Internal: framework-side entry point invoked by Display when the
    // platform delivers a deep link through `AppArg`.
    // ------------------------------------------------------------------------

    /// Dispatch a URL delivered by the platform. Invoked by
    /// `com.codename1.ui.Display#setProperty(String, String)` for URL-shaped
    /// `AppArg` values; applications should call `#navigate(String)` instead.
    public static boolean dispatchExternalUrl(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }
        if (Display.getInstance().isEdt()) {
            return navigate(url);
        }
        final String captured = url;
        final boolean[] holder = new boolean[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                holder[0] = navigate(captured);
            }
        });
        return holder[0];
    }
}

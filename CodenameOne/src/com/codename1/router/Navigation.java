/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.router;

import com.codename1.ui.Display;
import com.codename1.ui.Form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        List<NavigationEntry> before = new ArrayList<NavigationEntry>(stack);
        stack.add(new NavigationEntry(path, f));
        // BEFORE show(), which runs application code. The stack has already changed here, and
        // that is what continuity records -- so notifying now means a show callback that ends the
        // session (an expired login discovered on screen) is answered by clear() clearing the
        // pending flag, and the flush queued a moment ago then finds nothing owed. Notifying
        // afterwards described a session the callback had already ended, and checkpointed the
        // signed-out account's payload.
        stackChanged();
        List<NavigationEntry> expected = new ArrayList<NavigationEntry>(stack);
        try {
            f.show();
        } catch (RuntimeException e) {
            // The entry goes back. show() can throw before the form is ever installed, and the
            // entry left behind was a screen the user never saw -- persisted by the checkpoint
            // this method has already queued, and restored after a process death. The flush reads
            // the stack when it runs, so putting it back is what makes that checkpoint describe
            // the truth.
            rollBack(before, expected, f);
            throw e;
        }
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
        List<NavigationEntry> before = new ArrayList<NavigationEntry>(stack);
        stack.remove(stack.size() - 1);
        NavigationEntry now = stack.get(stack.size() - 1);
        Form back = now.getForm();
        // Before showBack(), for the reason navigate() gives.
        stackChanged();
        List<NavigationEntry> expected = new ArrayList<NavigationEntry>(stack);
        try {
            back.showBack();
        } catch (RuntimeException e) {
            rollBack(before, expected, back);
            throw e;
        }
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

    /// Undoes a navigation whose show() threw -- but only if nothing else touched the stack
    /// while it ran.
    ///
    /// `show()` runs application code, and that code can navigate. The case that matters is
    /// `Continuity.clear()`: a show listener discovers the session has expired, logs out --
    /// which empties this stack on purpose -- and then throws on the way out. Restoring
    /// unconditionally handed the signed-out account's forms straight back, reachable through
    /// `getStack()` and `back()` and persisted by the next checkpoint. That is the one thing the
    /// logout existed to prevent, undone by the rollback meant to help it.
    ///
    /// So the rollback applies only when the stack is still exactly what this method left it as.
    /// Anything else -- a logout, or a show listener that navigated somewhere of its own -- is a
    /// deliberate change by code that ran later, and it wins. `NavigationEntry` does not override
    /// equals, so comparing the lists compares entry IDENTITY, which is what makes "still exactly
    /// what I left" mean what it says.
    private static void rollBack(List<NavigationEntry> before, List<NavigationEntry> expected,
            Form intended) {
        if (!expected.equals(stack)) {
            return;
        }
        if (Display.getInstance().getCurrent() == intended) { //NOPMD CompareObjectsWithEquals
            // The form IS on screen. show() installs the form and only then runs onShowCompleted
            // and the show listeners, so a throw from one of those is a failure that happened
            // AFTER the navigation succeeded -- the entry describes the screen the user is
            // looking at, and rolling it back would leave Navigation.getCurrent() disagreeing
            // with Display.getCurrent(): back() would work on a stack whose top is not the
            // visible form, and a checkpoint would persist a screen the user is not on.
            //
            // The rollback is for the other case, which is the one it was written for: show()
            // threw BEFORE installing anything, so the entry is a screen nobody ever saw.
            //
            // Re-showing the previous form instead was the alternative, and it is worse: it runs
            // a second full show cycle -- transitions, listeners, whatever they do -- as error
            // handling, on a form the application has not asked to see again, and that cycle can
            // throw in its turn. Leaving the stack agreeing with the display costs nothing and
            // needs no application code to run.
            return;
        }
        stack.clear();
        stack.addAll(before);
    }

    /// Forgets the navigation history, leaving nothing to go back to.
    ///
    /// For a logout, which is the case that needs it: `Continuity.clear()` calls this, because a
    /// route stack is the previous account's work as surely as a stored checkpoint is. Left in
    /// place it kept two promises broken -- `Navigation#back()` reopened the signed-out account's
    /// forms, and the next navigation checkpointed and republished a stack that still began with
    /// their routes.
    ///
    /// The forms themselves are not touched: whatever is on screen stays there, and the caller
    /// navigates wherever it means to go next.
    ///
    /// Continuity IS notified, because for every caller except a logout this is a real change to
    /// where the user has been. It used to stay silent so that `Continuity.clear()` could call it
    /// without checkpointing the emptied stack back over what it was deleting -- but that made
    /// every other caller silent too: an application forgetting its back history and then not
    /// navigating left the previous routes in the stored checkpoint, so a process death restored
    /// exactly what it had just cleared. The logout path suppresses this at its own end, where
    /// the reason to suppress it lives.
    public static void clearStack() {
        stack.clear();
        stackChanged();
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
        List<NavigationEntry> before = new ArrayList<NavigationEntry>(stack);
        while (stack.size() > idx + 1) {
            stack.remove(stack.size() - 1);
        }
        Form target = entry.getForm();
        // Before showBack(), for the reason navigate() gives.
        stackChanged();
        List<NavigationEntry> expected = new ArrayList<NavigationEntry>(stack);
        try {
            target.showBack();
        } catch (RuntimeException e) {
            rollBack(before, expected, target);
            throw e;
        }
        return true;
    }

    /// Rebuilds the stack from a list of paths, showing only the last one.
    ///
    /// This is how `com.codename1.continuity.Continuity` puts the user back where they were: the
    /// saved state is a list of paths, and every one of them has to become a stack frame or
    /// `back()` would land on a screen that was never built. Replaying them with `navigate` would
    /// work and would also flash every intermediate screen past the user with a transition each,
    /// so the frames are built silently and only the top one is shown.
    ///
    /// Paths that no longer match a route are skipped rather than failing the restore. A rebuilt
    /// app legitimately drops routes, and refusing to restore anything because one deep frame went
    /// away would lose the whole session over a screen the user was not on.
    ///
    /// Replaces whatever was on the stack. Must be called on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `paths`: the paths, oldest first
    ///
    /// #### Returns
    ///
    /// true when at least one frame was rebuilt and shown
    public static boolean restoreStack(List<String> paths) {
        RouteDispatcher d = dispatcher;
        if (d == null || paths == null || paths.isEmpty()) {
            return false;
        }
        List<NavigationEntry> rebuilt = new ArrayList<NavigationEntry>();
        for (String path : paths) {
            if (sessionEnded()) {
                // A factory ended the continuity session -- it found the account signed out,
                // which is exactly the decision a route factory is entitled to make. Every later
                // factory would run for that account: constructing forms, and whatever they query
                // or write on the way. The lifecycle check in Continuity.restore() happens after
                // this method returns and can only empty the stack afterwards, which undoes none
                // of it.
                return false;
            }
            if (path == null || path.length() == 0) {
                continue;
            }
            Form f;
            try {
                f = d.dispatch(path);
            } catch (Throwable t) {
                com.codename1.io.Log.e(t);
                continue;
            }
            if (f != null) {
                rebuilt.add(new NavigationEntry(path, f));
            }
        }
        if (rebuilt.isEmpty() || sessionEnded()) {
            // Asked again, because the loop tests before each factory and the LAST one has no
            // "next" iteration to be stopped by. Showing here would put the signed-out account's
            // screen in front of the user.
            return false;
        }
        // The PREVIOUS stack is kept until the new one is really on screen. show() runs
        // application code -- the form's own show handling, and whatever listens to it -- and if
        // that throws, the old form is still displayed while getCurrent(), back() and the next
        // checkpoint would all be describing a stack the user never saw. A later navigation then
        // persists a restoration that failed.
        List<NavigationEntry> previous = new ArrayList<NavigationEntry>(stack);
        stack.clear();
        stack.addAll(rebuilt);
        // Resolved BEFORE the try, and that is not tidying. Reading from a generic list compiles
        // to a checkcast, and this virtual machine's CHECKCAST expands to nothing -- so a failed
        // cast hands the wrong object to the next instruction instead of throwing, and a handler
        // that catches RuntimeException around it is a handler that can never run. The guarded
        // region has to contain only the call being guarded. check-cast-semantics.sh refuses the
        // other shape, correctly, and caught this exact line.
        Form top = rebuilt.get(rebuilt.size() - 1).getForm();
        // What is DISPLAYED before the attempt, which is not the same thing as the stack.
        // Display.setCurrentForm() installs the new form and only then runs onShowCompleted and
        // the Show listeners, so a listener that throws leaves the failed form current -- and
        // restoring the list alone left getCurrent() and Navigation.getCurrent() describing
        // different screens.
        Form displayed = Display.getInstance().getCurrent();
        try {
            // show(), not showBack(): the user is arriving, not going back, and showBack would
            // run the reverse transition into a screen they have not seen yet.
            top.show();
        } catch (RuntimeException e) {
            stack.clear();
            stack.addAll(previous);
            // And the screen with it. show() rather than showBack(): the user is not going back,
            // an attempt that failed is being undone.
            try {
                Form now = Display.getInstance().getCurrent();
                if (displayed != null && displayed != now) { //NOPMD CompareObjectsWithEquals
                    displayed.show();
                }
            } catch (RuntimeException ignored) {
                // The original failure is the one worth reporting; losing it to a second one
                // while cleaning up would hide what actually went wrong.
                com.codename1.io.Log.e(ignored);
            }
            throw e;
        }
        stackChanged();
        return true;
    }

    /// Whether a continuity restore in progress has had its session ended underneath it.
    ///
    /// Answers false for every application that does not use continuity, and for every navigation
    /// that is not a restore, which is why it can sit in this loop.
    private static boolean sessionEnded() {
        try {
            return com.codename1.continuity.Continuity.restoreSessionEnded();
        } catch (Throwable t) {
            // Carrying on is the answer that keeps ordinary navigation working; this guard exists
            // for the signed-out case, not to gate the routing API on the continuity framework
            // being answerable.
            com.codename1.io.Log.e(t);
            return false;
        }
    }

    /// Tells the continuity framework that the stack moved, so it can checkpoint.
    ///
    /// A direct call rather than a listener: `Continuity.routeStackChanged()` returns immediately
    /// unless an application actually enabled continuity, and a listener registry here would be
    /// public API earned by one internal caller.
    private static void stackChanged() {
        try {
            com.codename1.continuity.Continuity.routeStackChanged();
        } catch (Throwable t) {
            com.codename1.io.Log.e(t);
        }
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

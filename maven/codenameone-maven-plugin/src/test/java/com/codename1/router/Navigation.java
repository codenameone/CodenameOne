/*
 * Test stub of com.codename1.router.Navigation. Mirrors the runtime API
 * surface RouteAnnotationProcessorTest exercises without dragging in
 * Display.callSerially / Form.show machinery.
 */
package com.codename1.router;

import com.codename1.ui.Form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Navigation {

    private static RouteDispatcher dispatcher;
    private static final List<NavigationEntry> stack = new ArrayList<NavigationEntry>();

    private Navigation() { }

    public static void setDispatcher(RouteDispatcher d) {
        dispatcher = d;
    }

    public static RouteDispatcher getDispatcherForTest() {
        return dispatcher;
    }

    public static boolean navigate(String path) {
        if (dispatcher == null || path == null) {
            return false;
        }
        Form f = dispatcher.dispatch(path);
        if (f == null) {
            return false;
        }
        stack.add(new NavigationEntry(path, f));
        f.show();
        return true;
    }

    public static boolean back() {
        if (stack.size() <= 1) {
            return false;
        }
        stack.remove(stack.size() - 1);
        stack.get(stack.size() - 1).getForm().showBack();
        return true;
    }

    public static NavigationEntry getCurrent() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    public static List<NavigationEntry> getStack() {
        return Collections.unmodifiableList(new ArrayList<NavigationEntry>(stack));
    }

    public static boolean popTo(NavigationEntry entry) {
        if (entry == null) {
            return false;
        }
        int idx = stack.indexOf(entry);
        if (idx < 0) {
            return false;
        }
        while (stack.size() > idx + 1) {
            stack.remove(stack.size() - 1);
        }
        entry.getForm().showBack();
        return true;
    }

    public static void resetForTest() {
        dispatcher = null;
        stack.clear();
    }
}

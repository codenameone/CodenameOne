/*
 * Test stub of com.codename1.ui.Display. Records the route dispatcher the
 * generated Routes class installs so RouteAnnotationProcessorTest can dispatch
 * URLs through it.
 */
package com.codename1.ui;

import com.codename1.router.RouteDispatcher;

public final class Display {
    private static final Display INSTANCE = new Display();
    public RouteDispatcher dispatcher;
    public int installCalls;

    public static Display getInstance() {
        return INSTANCE;
    }

    public void installRouteDispatcher(RouteDispatcher d) {
        System.out.println("[test-stub Display] installRouteDispatcher called with " + d
                + " (this=" + System.identityHashCode(this)
                + ", loader=" + getClass().getClassLoader() + ")");
        this.dispatcher = d;
        this.installCalls++;
    }

    public void reset() {
        dispatcher = null;
        installCalls = 0;
    }
}

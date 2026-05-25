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

    public static Display getInstance() {
        return INSTANCE;
    }

    public void installRouteDispatcher(RouteDispatcher d) {
        this.dispatcher = d;
    }

    public void reset() {
        dispatcher = null;
    }
}

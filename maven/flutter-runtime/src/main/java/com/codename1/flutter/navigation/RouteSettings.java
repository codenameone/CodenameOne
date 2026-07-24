package com.codename1.flutter.navigation;

/**
 * The data with which a route was pushed ({@code RouteSettings} in Flutter): its
 * name and optional arguments. new_gallery's {@code onGenerateRoute} matches on
 * {@link #name()} to build the right page.
 */
public class RouteSettings {

    private String name;
    private Object arguments;

    public RouteSettings() {
    }

    // Named-parameter setters.
    public void name(String v) {
        this.name = v;
    }

    public void arguments(Object v) {
        this.arguments = v;
    }

    public String name() {
        return name;
    }

    public Object arguments() {
        return arguments;
    }

    /** Returns a copy with the supplied fields overridden (null keeps current). */
    public RouteSettings copyWith(String name, Object arguments) {
        RouteSettings c = new RouteSettings();
        c.name = name != null ? name : this.name;
        c.arguments = arguments != null ? arguments : this.arguments;
        return c;
    }
}

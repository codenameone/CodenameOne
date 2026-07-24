package com.codename1.flutter.navigation;

/**
 * Base type of a navigable route — Flutter's {@code Route<T>}. Minimal marker
 * added so typed route factories (a demo function returning {@code
 * Route<String>}) accept the concrete Cupertino route subclasses. The
 * navigation category may later flesh this out; the Cupertino routes only
 * rely on it as a common supertype.
 *
 * @param <T> the value type the route completes with when popped
 */
public abstract class Route<T> {

    private RouteSettings settings;

    /**
     * Flutter's {@code Route.settings}. Accepts an {@code Object} because super-parameter
     * forwarding erases the argument type to {@code dynamic}; only a {@link RouteSettings} is
     * retained.
     */
    public void settings(Object v) {
        this.settings = (v instanceof RouteSettings) ? (RouteSettings) v : null;
    }

    public RouteSettings settings() {
        return settings;
    }
}

package com.codename1.flutter.navigation;

/**
 * The mutable state of a {@code Navigator} ({@code NavigatorState} in Flutter),
 * as reached via {@code Navigator.of(context)} or a {@code GlobalKey<NavigatorState>}.
 * Only the restoration-related push surface exercised by the gallery is modelled
 * here; each restorable push returns an opaque restoration id (a no-op string in
 * this implementation). The routing itself is handled by {@link Navigator}.
 */
public abstract class NavigatorState {

    /**
     * Push a route created by {@code routeBuilder}, returning a restoration id.
     * Restoration is not persisted, so the returned id is informational only.
     * {@code routeBuilder} is a {@code Route Function(BuildContext, Object?)}.
     */
    public String restorablePush(
            dart.runtime.Funcs.Func2<com.codename1.flutter.BuildContext, Object, Object> routeBuilder,
            Object arguments) {
        return "";
    }

    public String restorablePushNamed(String routeName, Object arguments) {
        return routeName == null ? "" : routeName;
    }

    public void pop(Object result) {
    }

    /**
     * Pop routes until {@code predicate} accepts the top route
     * ({@code NavigatorState.popUntil}). No route stack is kept, so this is a no-op.
     * {@code predicate} is a {@code bool Function(Route)}.
     */
    public void popUntil(dart.runtime.Funcs.Func1<Route<Object>, Boolean> predicate) {
    }

    /** Push a named route ({@code NavigatorState.pushNamed}). */
    public Object pushNamed(String routeName, Object arguments) {
        return null;
    }

    /** Replace the current route ({@code NavigatorState.pushReplacementNamed}). */
    public Object pushReplacementNamed(String routeName, Object arguments, Object result) {
        return null;
    }

    /** Pop if possible ({@code NavigatorState.maybePop}). */
    public Object maybePop(Object result) {
        return null;
    }

    /**
     * Whether the navigator can pop the current route ({@code NavigatorState.canPop}).
     * This minimal model keeps no route stack, so it reports {@code false}.
     */
    public boolean canPop() {
        return false;
    }

    /**
     * Push the given route onto the navigator ({@code NavigatorState.push}).
     * The route is not retained by this minimal model; the returned pop-result
     * future is always absent.
     */
    public Object push(Object route) {
        return null;
    }
}

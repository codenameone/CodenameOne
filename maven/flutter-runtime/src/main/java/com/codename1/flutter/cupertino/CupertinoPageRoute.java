package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;
import com.codename1.flutter.navigation.Route;

import dart.runtime.Funcs;

/**
 * A route that presents its page with the iOS slide-in transition — Flutter's
 * {@code CupertinoPageRoute}. The transition itself is not animated this pass;
 * the route carries the page {@code builder}, optional {@code settings} and
 * {@code title}. Subclassable (a demo overrides {@link #buildTransitions} to
 * disable the animation).
 *
 * @param <T> the value type the route completes with when popped
 */
public class CupertinoPageRoute<T> extends Route<T> {

    private Funcs.Func1<BuildContext, Widget> builder;
    private Object settings;
    private String title;

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void settings(Object v) {
        this.settings = v;
    }

    public void title(String v) {
        this.title = v;
    }

    public void maintainState(boolean v) {
    }

    public void fullscreenDialog(boolean v) {
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Wraps the page in its transition; the default is the identity (no
     * animation) — overridable by subclasses.
     */
    public Widget buildTransitions(BuildContext context, Animation<Double> animation,
                                   Animation<Double> secondaryAnimation, Widget child) {
        return child;
    }
}

package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Dialogs;

import dart.runtime.Funcs;

/**
 * Top-level entry points of the {@code animations} package. Currently hosts
 * {@code showModal}, which shows a modal route with an {@code animations}-package
 * transition (fade-scale by default). This pass presents the modal via the
 * Material {@link Dialogs#showDialog} plumbing; the package's custom
 * fade/scale reveal is deferred.
 */
public final class Animations {

    private Animations() {
    }

    /**
     * Shows a modal built by {@code builder} over the current route — the
     * {@code animations} package's top-level {@code showModal}. The
     * {@code configuration}, {@code useRootNavigator} and {@code filter}
     * parameters are captured for API shape.
     *
     * @return the route's completion result (a future value); ignored by
     *         callers that do not await the dismissal
     */
    public static Object showModal(BuildContext context, Object configuration,
                                   Object useRootNavigator,
                                   Funcs.Func1<BuildContext, Widget> builder,
                                   Object filter) {
        if (builder != null) {
            Dialogs.showDialog(context, builder);
        }
        return null;
    }
}

package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;

import dart.async.Future;
import dart.runtime.Funcs;

/**
 * Host class for Dart's top-level {@code showModalBottomSheet} function. This
 * milestone is a bookkeeping stub: it returns an already-completed
 * {@link Future} (the modal sheet is dismissed immediately) so a non-awaited
 * {@code showModalBottomSheet(...)} call transpiles and runs to completion. The
 * modal presentation of {@code builder}'s widget tree is deferred; wiring it to
 * a CN1 {@code Dialog} the way {@link Dialogs} does is a follow-up.
 */
public final class BottomSheets {

    private BottomSheets() {
    }

    public static Future<Object> showModalBottomSheet(BuildContext context,
                                                      Funcs.Func1<BuildContext, Widget> builder,
                                                      Color backgroundColor,
                                                      Double elevation,
                                                      Object shape,
                                                      Clip clipBehavior,
                                                      Object constraints,
                                                      Color barrierColor,
                                                      Boolean isScrollControlled,
                                                      Boolean useRootNavigator,
                                                      Boolean isDismissible,
                                                      Boolean enableDrag,
                                                      Boolean showDragHandle,
                                                      Object routeSettings,
                                                      Object transitionAnimationController) {
        return Future.value(null);
    }
}

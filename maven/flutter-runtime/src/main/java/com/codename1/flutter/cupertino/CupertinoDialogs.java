package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Dialogs;

import dart.runtime.Funcs;

/**
 * Host class for Dart's top-level {@code showCupertinoDialog} and
 * {@code showCupertinoModalPopup} functions. Both present the built widget
 * tree through the shared modeless dialog surface (see
 * {@link Dialogs#showDialog}) — the iOS-specific barrier / slide-up chrome is
 * approximate this pass.
 */
public final class CupertinoDialogs {

    private CupertinoDialogs() {
    }

    public static void showCupertinoDialog(BuildContext context,
                                           Funcs.Func1<BuildContext, Widget> builder,
                                           Boolean barrierDismissible, Color barrierColor,
                                           String barrierLabel, Boolean useRootNavigator,
                                           Object routeSettings) {
        Dialogs.showDialog(context, builder);
    }

    public static void showCupertinoModalPopup(BuildContext context,
                                               Funcs.Func1<BuildContext, Widget> builder,
                                               Color barrierColor, Boolean barrierDismissible,
                                               Boolean useRootNavigator, Object semanticsDismissible,
                                               Object routeSettings) {
        Dialogs.showDialog(context, builder);
    }
}

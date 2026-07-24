package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * The mutable state of a {@link Scaffold} ({@code ScaffoldState} in Flutter),
 * reached via {@code Scaffold.of(context)}. Only the surface exercised by the
 * gallery is modelled: showing a bottom sheet (returning a controller whose
 * {@code closed} future the caller awaits), showing snack bars, and opening the
 * drawers. Rendering of these is a later milestone; the methods keep the right
 * shape so callers transpile and compile.
 */
public class ScaffoldState {

    /**
     * Shows a persistent bottom sheet built by {@code builder}, returning a
     * controller. The sheet is not mounted at this milestone; the controller's
     * {@code closed} future completes immediately.
     */
    public PersistentBottomSheetController showBottomSheet(Funcs.Func1<BuildContext, Widget> builder,
            Double elevation, Color backgroundColor, Object shape, Clip clipBehavior,
            Object constraints, Boolean enableDrag) {
        return new PersistentBottomSheetController();
    }

    public void showSnackBar(SnackBar snackBar) {
    }

    public void openDrawer() {
    }

    public void openEndDrawer() {
    }
}

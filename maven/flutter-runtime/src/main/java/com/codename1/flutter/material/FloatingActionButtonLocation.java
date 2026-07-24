package com.codename1.flutter.material;

/**
 * Where a {@code FloatingActionButton} is placed within a {@code Scaffold} —
 * Flutter's {@code FloatingActionButtonLocation}.
 *
 * <p>Flutter exposes these as static const instances of a class; the app only
 * ever names a constant and the Scaffold slot receives it untyped, so an enum
 * carrying the matching constant names is sufficient for this pass.</p>
 */
public enum FloatingActionButtonLocation {
    startTop, miniStartTop, centerTop, miniCenterTop, endTop, miniEndTop,
    startFloat, miniStartFloat, centerFloat, miniCenterFloat, endFloat, miniEndFloat,
    startDocked, miniStartDocked, centerDocked, miniCenterDocked, endDocked, miniEndDocked
}

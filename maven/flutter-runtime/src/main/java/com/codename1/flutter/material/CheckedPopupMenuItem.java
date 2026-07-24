package com.codename1.flutter.material;

/**
 * A {@link PopupMenuItem} that shows a check mark when {@code checked} —
 * Flutter's {@code CheckedPopupMenuItem<T>}. Inherits value/child/onTap
 * handling from PopupMenuItem; the leading check-mark reveal is deferred, so
 * this pass records the checked state for API shape.
 *
 * @param <T> the value type carried by this menu item
 */
public class CheckedPopupMenuItem<T> extends PopupMenuItem<T> {

    private boolean checked;

    public void checked(boolean v) {
        this.checked = v;
    }

    public boolean isChecked() {
        return checked;
    }
}

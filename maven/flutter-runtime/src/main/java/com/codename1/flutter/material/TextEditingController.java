package com.codename1.flutter.material;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller for an editable text field. The controller and the CN1 text
 * component are kept in two-way sync by {@link TextFieldRenderElement}:
 * user edits flow into {@link #text()} (and notify listeners), while
 * {@link #setText(String)}/{@link #clear()} push into the mounted component.
 *
 * <p>Transpiler surface: the Dart constructor's named {@code text:} parameter
 * becomes the {@link #text(String)} setter, the Dart {@code text} getter
 * becomes {@link #text()}, and Dart {@code controller.text = v} assignments
 * are emitted as the explicit {@link #setText(String)} method.</p>
 */
public class TextEditingController {

    private String value = "";
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private TextFieldRenderElement bound;

    public TextEditingController() {
    }

    /**
     * Named parameter setter for the Dart {@code text:} constructor parameter
     * (initial value, no listener notification).
     */
    public void text(String v) {
        this.value = v == null ? "" : v;
    }

    /**
     * The current text. When a mounted TextField is bound this reads the
     * component's live text.
     */
    public String text() {
        if (bound != null && bound.isMounted()) {
            String s = bound.componentText();
            if (s != null) {
                value = s;
            }
        }
        return value;
    }

    /**
     * Imperative setter (Dart {@code controller.text = v}): updates the bound
     * component when mounted and notifies listeners.
     */
    public void setText(String v) {
        this.value = v == null ? "" : v;
        if (bound != null && bound.isMounted()) {
            bound.applyControllerText(this.value);
        }
        notifyListeners();
    }

    public void clear() {
        setText("");
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    // ------------------------------------------------------------------
    // Framework plumbing (package private)
    // ------------------------------------------------------------------

    void bind(TextFieldRenderElement e) {
        this.bound = e;
    }

    void unbind(TextFieldRenderElement e) {
        if (this.bound == e) {
            this.bound = null;
        }
    }

    /**
     * A user edit arrived from the component: absorb it (no push-back) and
     * notify listeners.
     */
    void valueFromComponent(String s) {
        this.value = s == null ? "" : s;
        notifyListeners();
    }

    private void notifyListeners() {
        for (Funcs.VoidFunc0 l : new ArrayList<Funcs.VoidFunc0>(listeners)) {
            l.call();
        }
    }
}

package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Groups form fields that validate/save together — Flutter's {@code Form}. A
 * {@link com.codename1.flutter.GlobalKey}{@code <FormState>} attached to the
 * form gives access to the {@link FormState} that drives
 * validate/save/reset across the fields. Structural pass-through for this
 * milestone: the {@code child} renders unchanged.
 */
public class Form extends Widget implements HasChild {

    private Widget child;
    private Object onChanged;
    private Object onWillPop;
    private Object canPop;
    private Object onPopInvoked;
    private Object autovalidateMode;

    public void child(Widget v) {
        this.child = v;
    }

    public void onChanged(Object v) {
        this.onChanged = v;
    }

    public void onWillPop(Object v) {
        this.onWillPop = v;
    }

    public void canPop(Object v) {
        this.canPop = v;
    }

    public void onPopInvoked(Object v) {
        this.onPopInvoked = v;
    }

    public void autovalidateMode(Object v) {
        this.autovalidateMode = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    /** Flutter's {@code Form.of} — the nearest enclosing {@link FormState}. */
    public static FormState of(BuildContext context) {
        return null;
    }

    /** Flutter's {@code Form.maybeOf}. */
    public static FormState maybeOf(BuildContext context) {
        return null;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}

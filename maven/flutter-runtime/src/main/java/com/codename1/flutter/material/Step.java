package com.codename1.flutter.material;

import com.codename1.flutter.Widget;

/**
 * One step of a {@link Stepper} — Flutter's {@code Step}. A configuration
 * object holding the step's {@code title}, optional {@code subtitle} and
 * {@code content}.
 */
public class Step {

    private Widget title;
    private Widget subtitle;
    private Widget content;
    private Object state;
    private boolean isActive;

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void content(Widget v) {
        this.content = v;
    }

    public void state(Object v) {
        this.state = v;
    }

    public void isActive(boolean v) {
        this.isActive = v;
    }

    public void stepStyle(Object v) {
    }

    public Widget getTitle() {
        return title;
    }

    public Widget getSubtitle() {
        return subtitle;
    }

    public Widget getContent() {
        return content;
    }
}

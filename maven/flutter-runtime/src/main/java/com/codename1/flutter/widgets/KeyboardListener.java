package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.FocusNode;
import com.codename1.flutter.Widget;
import com.codename1.flutter.services.KeyEvent;

import dart.runtime.Funcs;

/**
 * A raw keyboard listener — Flutter's {@code KeyboardListener}. Structural
 * pass-through: the single {@code child} renders unchanged; the focus node and
 * key-event callback are held for a later input pass.
 */
public class KeyboardListener extends Widget implements HasChild {

    private FocusNode focusNode;
    private Boolean autofocus;
    private Boolean includeSemantics;
    private Funcs.VoidFunc1<KeyEvent> onKeyEvent;
    private Widget child;

    public void focusNode(FocusNode v) { this.focusNode = v; }
    public void autofocus(boolean v) { this.autofocus = v; }
    public void includeSemantics(boolean v) { this.includeSemantics = v; }
    public void onKeyEvent(Funcs.VoidFunc1<KeyEvent> v) { this.onKeyEvent = v; }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}

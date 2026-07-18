package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Shared configuration of the material buttons ({@link ElevatedButton},
 * {@link TextButton}, {@link OutlinedButton}): a press callback (null means
 * disabled) and a content child consumed as the button's label or icon.
 */
public abstract class ButtonBase extends Widget {

    private Funcs.VoidFunc0 onPressed;
    private Widget child;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Funcs.VoidFunc0 getOnPressed() {
        return onPressed;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new ButtonRenderElement(this);
    }
}

package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.InputDecoration;
import com.codename1.flutter.material.TextEditingController;
import com.codename1.flutter.material.TextField;

import dart.runtime.Funcs;

/**
 * An iOS-style search field — Flutter's {@code CupertinoSearchTextField}.
 * Composed onto the material {@link TextField} with the placeholder as the CN1
 * hint; the magnifier/clear affordances are approximate this pass.
 */
public class CupertinoSearchTextField extends StatelessWidget {

    private Object controller;
    private String placeholder;
    private Funcs.VoidFunc1<String> onChanged;
    private Funcs.VoidFunc1<String> onSubmitted;

    public void controller(Object v) {
        this.controller = v;
    }

    public void placeholder(String v) {
        this.placeholder = v;
    }

    public void decoration(Object v) {
    }

    public void padding(Object v) {
    }

    public void restorationId(String v) {
    }

    public void onChanged(Funcs.VoidFunc1<String> v) {
        this.onChanged = v;
    }

    public void onSubmitted(Funcs.VoidFunc1<String> v) {
        this.onSubmitted = v;
    }

    public void onSuffixTap(Funcs.VoidFunc0 v) {
    }

    @Override
    public Widget build(BuildContext context) {
        TextField tf = new TextField();
        if (controller instanceof TextEditingController) {
            tf.controller((TextEditingController) controller);
        }
        if (placeholder != null) {
            InputDecoration d = new InputDecoration();
            d.hintText(placeholder);
            tf.decoration(d);
        }
        if (onChanged != null) {
            tf.onChanged(onChanged);
        }
        if (onSubmitted != null) {
            tf.onSubmitted(onSubmitted);
        }
        return tf;
    }
}

package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A stateless helper whose {@code build} is delegated to a closure — Flutter's
 * {@code Builder}. Useful to obtain a {@link BuildContext} below the current
 * widget.
 */
public class Builder extends Widget {

    private Funcs.Func1<BuildContext, Widget> builder;

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }

    @Override
    public Element createElement() {
        return new BuilderElement(this);
    }
}

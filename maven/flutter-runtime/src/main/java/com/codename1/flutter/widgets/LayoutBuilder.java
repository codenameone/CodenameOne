package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;

import dart.runtime.Funcs;

/**
 * Builds a widget tree that depends on the parent's size — Flutter's
 * {@code LayoutBuilder}. The builder receives {@link BoxConstraints} (logical
 * pixels).
 *
 * <p>Flutter invokes the builder during layout; this milestone invokes it once
 * at build time with the constraints of the available viewport (best effort),
 * which is correct for the common top-level responsive-breakpoint use.</p>
 */
public class LayoutBuilder extends Widget {

    private Funcs.Func2<BuildContext, BoxConstraints, Widget> builder;

    public void builder(Funcs.Func2<BuildContext, BoxConstraints, Widget> v) {
        this.builder = v;
    }

    public Funcs.Func2<BuildContext, BoxConstraints, Widget> getBuilder() {
        return builder;
    }

    @Override
    public Element createElement() {
        return new LayoutBuilderElement(this);
    }
}

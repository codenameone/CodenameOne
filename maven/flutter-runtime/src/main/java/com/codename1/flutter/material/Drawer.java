package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * A material navigation drawer panel. As a root Scaffold's {@code drawer}
 * it renders into the CN1 Toolbar side menu; embedded Scaffolds ignore it
 * with a log warning (see {@link ScaffoldRenderElement}). Standard Material
 * width: 304lp.
 */
public class Drawer extends Widget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new DrawerRenderElement(this);
    }
}

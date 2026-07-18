package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * A material app bar. Under a root Scaffold it renders into the CN1 Form's
 * Toolbar (title component + toolbar background color); elsewhere it renders
 * as a strip at the top of the Flutter canvas.
 */
public class AppBar extends Widget {

    private Widget title;
    private Color backgroundColor;
    private boolean centerTitle;
    private boolean centerTitleSet;

    public void title(Widget v) {
        this.title = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void centerTitle(boolean v) {
        this.centerTitle = v;
        this.centerTitleSet = true;
    }

    public Widget getTitle() {
        return title;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public boolean getCenterTitle() {
        return centerTitle;
    }

    public boolean isCenterTitleSet() {
        return centerTitleSet;
    }

    @Override
    public Element createElement() {
        return new AppBarRenderElement(this);
    }
}

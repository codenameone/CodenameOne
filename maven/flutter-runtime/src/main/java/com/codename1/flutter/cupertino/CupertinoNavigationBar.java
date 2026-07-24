package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.AppBar;

/**
 * The iOS top navigation bar — Flutter's {@code CupertinoNavigationBar}: a
 * centered middle title with optional leading/trailing widgets. Composed onto
 * the material {@link AppBar} with a centered title (visually approximate this
 * pass).
 */
public class CupertinoNavigationBar extends StatelessWidget {

    private Widget leading;
    private Widget middle;
    private Widget trailing;
    private Color backgroundColor;

    public void leading(Widget v) {
        this.leading = v;
    }

    public void automaticallyImplyLeading(boolean v) {
    }

    public void automaticallyImplyMiddle(boolean v) {
    }

    public void previousPageTitle(String v) {
    }

    public void middle(Widget v) {
        this.middle = v;
    }

    public void trailing(Widget v) {
        this.trailing = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void brightness(Object v) {
    }

    public void padding(Object v) {
    }

    public void border(Object v) {
    }

    public void transitionBetweenRoutes(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        AppBar bar = new AppBar();
        if (middle != null) {
            bar.title(middle);
        }
        bar.centerTitle(true);
        if (backgroundColor != null) {
            bar.backgroundColor(backgroundColor);
        }
        return bar;
    }
}

package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.AppBar;

/**
 * The large-title iOS navigation bar used inside a scroll view — Flutter's
 * {@code CupertinoSliverNavigationBar}. The collapsing large-title behavior is
 * not modeled this pass; it composes a static {@link AppBar} using the large
 * title (or middle) as its title.
 */
public class CupertinoSliverNavigationBar extends StatelessWidget {

    private Widget largeTitle;
    private Widget leading;
    private Widget middle;
    private Widget trailing;
    private Color backgroundColor;

    public void largeTitle(Widget v) {
        this.largeTitle = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void automaticallyImplyLeading(boolean v) {
    }

    public void automaticallyImplyTitle(boolean v) {
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

    public void border(Object v) {
    }

    public void stretch(boolean v) {
    }

    @Override
    public Widget build(BuildContext context) {
        AppBar bar = new AppBar();
        Widget title = largeTitle != null ? largeTitle : middle;
        if (title != null) {
            bar.title(title);
        }
        if (backgroundColor != null) {
            bar.backgroundColor(backgroundColor);
        }
        return bar;
    }
}

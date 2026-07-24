package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * An app bar that integrates into a {@link CustomScrollView} and can expand,
 * float, pin or snap as the user scrolls — Flutter's {@code SliverAppBar}. This
 * milestone renders the {@code title} (with {@code flexibleSpace} preferred when
 * present); the scroll-driven collapse/expand behavior is deferred.
 */
public class SliverAppBar extends StatelessWidget {

    private Widget title;
    private Widget leading;
    private DartList<Widget> actions;
    private Widget flexibleSpace;
    private Color backgroundColor;

    public void title(Widget v) {
        this.title = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void flexibleSpace(Widget v) {
        this.flexibleSpace = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void pinned(boolean v) {
    }

    public void floating(boolean v) {
    }

    public void snap(boolean v) {
    }

    public void expandedHeight(double v) {
    }

    public void automaticallyImplyLeading(boolean v) {
    }

    public void bottom(Widget v) {
    }

    public void centerTitle(boolean v) {
    }

    public void elevation(double v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (flexibleSpace != null) {
            return flexibleSpace;
        }
        if (title != null) {
            return title;
        }
        return new SizedBox();
    }
}

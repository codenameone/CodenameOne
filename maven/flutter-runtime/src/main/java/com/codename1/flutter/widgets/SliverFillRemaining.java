package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A sliver that fills the remaining viewport space with its child — Flutter's
 * {@code SliverFillRemaining}. Renders its {@code child}.
 */
public class SliverFillRemaining extends StatelessWidget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public void hasScrollBody(boolean v) {
    }

    public void fillOverscroll(boolean v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}

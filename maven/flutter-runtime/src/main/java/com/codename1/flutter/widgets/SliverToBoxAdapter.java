package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Adapts a single box widget so it can sit among slivers — Flutter's
 * {@code SliverToBoxAdapter}. Renders its {@code child}.
 */
public class SliverToBoxAdapter extends StatelessWidget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}

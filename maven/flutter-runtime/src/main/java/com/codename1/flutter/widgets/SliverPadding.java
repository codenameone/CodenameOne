package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Insets a sliver — Flutter's {@code SliverPadding}. Renders its {@code sliver}
 * wrapped in a {@link Padding} when the padding is an {@link EdgeInsets}.
 */
public class SliverPadding extends StatelessWidget {

    private Object padding;
    private Widget sliver;

    public void padding(Object v) {
        this.padding = v;
    }

    public void sliver(Widget v) {
        this.sliver = v;
    }

    @Override
    public Widget build(BuildContext context) {
        Widget inner = sliver != null ? sliver : new SizedBox();
        if (padding instanceof EdgeInsets) {
            Padding p = new Padding();
            p.padding((EdgeInsets) padding);
            p.child(inner);
            return p;
        }
        return inner;
    }
}

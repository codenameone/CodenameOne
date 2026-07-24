package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.Duration;

/**
 * Creates a {@link TabController} and shares it with descendant {@link TabBar} /
 * {@link TabBarView} widgets — Flutter's {@code DefaultTabController}. This
 * milestone renders the subtree ({@code child}); the descendant tab widgets
 * currently default to index 0 rather than resolving the inherited controller,
 * so {@link #of} returns a fresh controller of the configured length.
 */
public class DefaultTabController extends StatelessWidget {

    private long length;
    private long initialIndex;
    private Widget child;

    public void length(long v) {
        this.length = v;
    }

    public void initialIndex(long v) {
        this.initialIndex = v;
    }

    public void animationDuration(Duration v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public static TabController of(BuildContext context) {
        TabController c = new TabController();
        c.length(1);
        return c;
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}

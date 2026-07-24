package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.DartList;

/**
 * The page view paired with a {@link TabBar} — Flutter's {@code TabBarView}.
 * Shows the child at the {@link TabController}'s current index (index 0 when no
 * controller is attached). The horizontal swipe transition between pages is
 * deferred; the selected page renders.
 */
public class TabBarView extends StatelessWidget {

    private DartList<Widget> children;
    private TabController controller;

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void controller(TabController v) {
        this.controller = v;
    }

    public void physics(Object v) {
    }

    public void dragStartBehavior(Object v) {
    }

    public void viewportFraction(double v) {
    }

    public void clipBehavior(Clip v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (children == null || children.size() == 0) {
            return new SizedBox();
        }
        int idx = controller != null ? (int) controller.index() : 0;
        if (idx < 0 || idx >= children.size()) {
            idx = 0;
        }
        return children.get(idx);
    }
}

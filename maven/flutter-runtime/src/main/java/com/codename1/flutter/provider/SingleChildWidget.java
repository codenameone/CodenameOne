package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * provider's SingleChildWidget: the base of the composable provider widgets
 * (the element type held in a {@code MultiProvider}'s {@code providers} list).
 * It renders its single {@code child}; subclasses add the value they publish.
 */
public class SingleChildWidget extends StatelessWidget {

    protected Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}

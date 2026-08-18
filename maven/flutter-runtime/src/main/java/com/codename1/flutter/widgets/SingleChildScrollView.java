package com.codename1.flutter.widgets;

import com.codename1.flutter.Axis;
import com.codename1.flutter.Clip;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Makes its child scrollable: the child subtree becomes a real CN1 scrollable container
 * boundary laid out with an unbounded main axis inside.
 *
 * <p>{@code scrollDirection} picks the axis. It defaults to vertical, as in Flutter, and
 * the horizontal case is what lets wide content — a data table with more columns than fit
 * a phone — be reached rather than crushed into the available width.</p>
 */
public class SingleChildScrollView extends Widget {

    private EdgeInsets padding;
    private Widget child;
    private String restorationId;
    private Clip clipBehavior;
    private Axis scrollDirection = Axis.vertical;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void scrollDirection(Axis v) {
        this.scrollDirection = v == null ? Axis.vertical : v;
    }

    public Axis getScrollDirection() {
        return scrollDirection;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public EdgeInsets getPadding() {
        return padding;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new SingleChildScrollViewRenderElement(this);
    }
}

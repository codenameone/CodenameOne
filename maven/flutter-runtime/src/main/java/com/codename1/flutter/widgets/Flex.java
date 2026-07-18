package com.codename1.flutter.widgets;

import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.Element;
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Shared configuration of {@link Column} and {@link Row}.
 */
public abstract class Flex extends Widget {

    private DartList<Widget> children;
    private MainAxisAlignment mainAxisAlignment = MainAxisAlignment.start;
    private CrossAxisAlignment crossAxisAlignment = CrossAxisAlignment.center;
    private MainAxisSize mainAxisSize = MainAxisSize.max;

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void mainAxisAlignment(MainAxisAlignment v) {
        this.mainAxisAlignment = v == null ? MainAxisAlignment.start : v;
    }

    public void crossAxisAlignment(CrossAxisAlignment v) {
        this.crossAxisAlignment = v == null ? CrossAxisAlignment.center : v;
    }

    public void mainAxisSize(MainAxisSize v) {
        this.mainAxisSize = v == null ? MainAxisSize.max : v;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    public MainAxisAlignment getMainAxisAlignment() {
        return mainAxisAlignment;
    }

    public CrossAxisAlignment getCrossAxisAlignment() {
        return crossAxisAlignment;
    }

    public MainAxisSize getMainAxisSize() {
        return mainAxisSize;
    }

    /**
     * True for Column (vertical main axis), false for Row.
     */
    public abstract boolean isVertical();

    @Override
    public Element createElement() {
        return new FlexRenderElement(this);
    }
}

package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * Lays its {@code children} out horizontally, falling back to a vertical column
 * when they do not fit — Flutter's {@code OverflowBar} (the modern
 * {@code ButtonBar}). This pass always renders the horizontal {@link Row} form;
 * the overflow-to-column behavior is deferred.
 */
public class OverflowBar extends StatelessWidget {

    private double spacing;
    private Object alignment;
    private double overflowSpacing;
    private Object overflowAlignment;
    private Object overflowDirection;
    private Object textDirection;
    private DartList<Widget> children;

    public void spacing(double v) {
        this.spacing = v;
    }

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void overflowSpacing(double v) {
        this.overflowSpacing = v;
    }

    public void overflowAlignment(Object v) {
        this.overflowAlignment = v;
    }

    public void overflowDirection(Object v) {
        this.overflowDirection = v;
    }

    public void textDirection(Object v) {
        this.textDirection = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public double getSpacing() {
        return spacing;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Widget build(BuildContext context) {
        Row row = new Row();
        row.children(children);
        return row;
    }
}

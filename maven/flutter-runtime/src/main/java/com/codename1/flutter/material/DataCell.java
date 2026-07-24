package com.codename1.flutter.material;

import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * One cell in a {@link DataRow} — Flutter's {@code DataCell}. Wraps the cell's
 * {@code child} widget.
 */
public class DataCell {

    private final Widget child;
    private boolean placeholder;
    private boolean showEditIcon;
    private Funcs.VoidFunc0 onTap;

    public DataCell(Widget child) {
        this.child = child;
    }

    public void placeholder(boolean v) {
        this.placeholder = v;
    }

    public void showEditIcon(boolean v) {
        this.showEditIcon = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public void onLongPress(Funcs.VoidFunc0 v) {
    }

    public void onTapDown(Object v) {
    }

    public Widget getChild() {
        return child;
    }
}

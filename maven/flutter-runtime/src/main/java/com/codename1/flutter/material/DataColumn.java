package com.codename1.flutter.material;

import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A column description for a {@link DataTable} — Flutter's {@code DataColumn}.
 */
public class DataColumn {

    private Widget label;
    private String tooltip;
    private boolean numeric;
    private Funcs.VoidFunc2<Long, Boolean> onSort;

    public void label(Widget v) {
        this.label = v;
    }

    public void tooltip(String v) {
        this.tooltip = v;
    }

    public void numeric(boolean v) {
        this.numeric = v;
    }

    public void onSort(Funcs.VoidFunc2<Long, Boolean> v) {
        this.onSort = v;
    }

    public Widget getLabel() {
        return label;
    }

    public boolean isNumeric() {
        return numeric;
    }
}

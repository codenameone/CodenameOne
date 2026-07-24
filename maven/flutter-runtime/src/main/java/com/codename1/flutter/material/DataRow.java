package com.codename1.flutter.material;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * One row of a {@link DataTable} — Flutter's {@code DataRow}. Holds the row's
 * {@link DataCell}s.
 */
public class DataRow {

    private DartList<DataCell> cells;
    private Boolean selected;
    private Long index;
    private Funcs.VoidFunc1<Boolean> onSelectChanged;

    public DataRow() {
    }

    /** Dart's {@code DataRow.byIndex} named constructor. */
    public static DataRow byIndex(long index,
                                  Boolean selected,
                                  Funcs.VoidFunc1<Boolean> onSelectChanged,
                                  Object onLongPress,
                                  Object color,
                                  DartList<DataCell> cells) {
        DataRow r = new DataRow();
        r.index = index;
        r.selected = selected;
        r.onSelectChanged = onSelectChanged;
        r.cells = cells;
        return r;
    }

    public void cells(DartList<DataCell> v) {
        this.cells = v;
    }

    public void selected(boolean v) {
        this.selected = v;
    }

    public void onSelectChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onSelectChanged = v;
    }

    public void onLongPress(Object v) {
    }

    public void color(Object v) {
    }

    public DartList<DataCell> getCells() {
        return cells;
    }
}

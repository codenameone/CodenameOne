package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Row;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A {@link DataTable} that pages through a {@link DataTableSource} — Flutter's
 * {@code PaginatedDataTable}. This milestone renders the optional {@code header},
 * a heading row of the {@code columns}' labels, and the current page of rows
 * ({@code rowsPerPage} rows from {@code initialFirstRowIndex}) pulled from the
 * source. Paging controls, sorting and selection are deferred.
 */
public class PaginatedDataTable extends StatelessWidget {

    /** The default value for {@code rowsPerPage} — Flutter's {@code defaultRowsPerPage}. */
    public static final long defaultRowsPerPage = 10;

    private Widget header;
    private DartList<DataColumn> columns;
    private long rowsPerPage = 10;
    private long initialFirstRowIndex;
    private DataTableSource source;
    private Long sortColumnIndex;
    private Boolean sortAscending;
    private Funcs.VoidFunc1<Long> onPageChanged;

    public void header(Widget v) {
        this.header = v;
    }

    public void actions(DartList<Widget> v) {
    }

    public void columns(DartList<DataColumn> v) {
        this.columns = v;
    }

    public void sortColumnIndex(Long v) {
        this.sortColumnIndex = v;
    }

    public void sortAscending(Boolean v) {
        this.sortAscending = v;
    }

    public void onSelectAll(Funcs.VoidFunc1<Boolean> v) {
    }

    public void dataRowHeight(Double v) {
    }

    public void headingRowHeight(Double v) {
    }

    public void horizontalMargin(Double v) {
    }

    public void columnSpacing(Double v) {
    }

    public void showCheckboxColumn(Boolean v) {
    }

    public void showFirstLastButtons(Boolean v) {
    }

    public void initialFirstRowIndex(Long v) {
        this.initialFirstRowIndex = v;
    }

    public void onPageChanged(Funcs.VoidFunc1<Long> v) {
        this.onPageChanged = v;
    }

    public void rowsPerPage(Long v) {
        this.rowsPerPage = v;
    }

    public void availableRowsPerPage(DartList<Long> v) {
    }

    public void onRowsPerPageChanged(Funcs.VoidFunc1<Long> v) {
    }

    public void source(DataTableSource v) {
        this.source = v;
    }

    public void checkboxHorizontalMargin(Object v) {
    }

    public void controller(Object v) {
    }

    public void primary(Boolean v) {
    }

    /** Material's row metrics, in logical pixels. */
    private static final double HEADING_HEIGHT_LP = 56;
    private static final double ROW_HEIGHT_LP = 48;
    private static final double CELL_SPACING_LP = 12;

    /**
     * One table row.
     *
     * <p>Column widths follow Flutter's: the LABEL column absorbs the slack while the
     * numeric ones take only what they need. Giving every column an equal share — which is
     * what this did — squeezed the dessert names into a two-character ribbon while the
     * percentage columns sat in acres of space.</p>
     */
    private Row rowOf(DartList<Widget> cells, double height) {
        DartList<Widget> flexed = new DartList<Widget>();
        for (int i = 0; i < cells.size(); i++) {
            boolean numeric = isNumericColumn(i);
            com.codename1.flutter.widgets.Container cell =
                    new com.codename1.flutter.widgets.Container();
            cell.padding(com.codename1.flutter.EdgeInsets.symmetric(0, CELL_SPACING_LP / 2));
            cell.alignment(numeric
                    ? com.codename1.flutter.Alignment.centerRight
                    : com.codename1.flutter.Alignment.centerLeft);
            cell.child(cells.get(i));
            Expanded e = new Expanded();
            // The first column is the label column and gets the room; Flutter sizes the
            // numeric ones to content, and a 3:1 split is the same shape without needing
            // intrinsic widths.
            e.flex(i == 0 ? 3 : 1);
            e.child(cell);
            flexed.add(e);
        }
        Row r = new Row();
        r.crossAxisAlignment(CrossAxisAlignment.center);
        r.children(flexed);
        com.codename1.flutter.widgets.Container box =
                new com.codename1.flutter.widgets.Container();
        box.height(height);
        box.padding(com.codename1.flutter.EdgeInsets.symmetric(0, CELL_SPACING_LP));
        box.child(r);
        return rowOfBox(box);
    }

    /** Wraps the sized row so the Column sees a Row-shaped child. */
    private static Row rowOfBox(Widget box) {
        DartList<Widget> one = new DartList<Widget>();
        Expanded e = new Expanded();
        e.child(box);
        one.add(e);
        Row r = new Row();
        r.children(one);
        return r;
    }

    private boolean isNumericColumn(int index) {
        return columns != null && index < columns.size() && columns.get(index).isNumeric();
    }

    /**
     * The heading label, with the sort arrow on the column currently sorted and a tap that
     * re-sorts — the part that made the header decorative rather than usable.
     */
    private Widget headingCell(final int index) {
        final DataColumn column = columns.get(index);
        Widget label = column.getLabel();
        if (sortColumnIndex != null && sortColumnIndex.longValue() == index) {
            DartList<Widget> parts = new DartList<Widget>();
            parts.add(label);
            com.codename1.flutter.widgets.Icon arrow = new com.codename1.flutter.widgets.Icon(
                    isSortAscending()
                            ? com.codename1.flutter.Icons.keyboard_arrow_up
                            : com.codename1.flutter.Icons.keyboard_arrow_down);
            arrow.size(16);
            parts.add(arrow);
            Row withArrow = new Row();
            withArrow.mainAxisSize(MainAxisSize.min);
            withArrow.children(parts);
            label = withArrow;
        }
        if (column.getOnSort() == null) {
            return label;
        }
        InkWell tap = new InkWell();
        tap.child(label);
        tap.onTap(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                boolean sameColumn = sortColumnIndex != null
                        && sortColumnIndex.longValue() == index;
                // Tapping the sorted column reverses it; a new column starts ascending.
                boolean ascending = sameColumn ? !isSortAscending() : true;
                column.getOnSort().call(Long.valueOf(index), Boolean.valueOf(ascending));
            }
        });
        return tap;
    }

    private boolean isSortAscending() {
        return sortAscending == null || sortAscending.booleanValue();
    }

    /** The footer: which rows are showing, and the way to the next page. */
    private Widget footer(long total) {
        final long first = initialFirstRowIndex;
        final long last = Math.min(first + rowsPerPage, total);
        DartList<Widget> parts = new DartList<Widget>();

        com.codename1.flutter.widgets.Text range = new com.codename1.flutter.widgets.Text(
                (total == 0 ? 0 : first + 1) + "-" + last + " of " + total);
        parts.add(range);
        parts.add(pageButton(com.codename1.flutter.Icons.chevron_left,
                first > 0, Math.max(0, first - rowsPerPage)));
        parts.add(pageButton(com.codename1.flutter.Icons.chevron_right,
                last < total, first + rowsPerPage));

        Row row = new Row();
        row.mainAxisAlignment(com.codename1.flutter.MainAxisAlignment.end);
        row.crossAxisAlignment(CrossAxisAlignment.center);
        row.children(parts);
        com.codename1.flutter.widgets.Container box =
                new com.codename1.flutter.widgets.Container();
        box.height(HEADING_HEIGHT_LP);
        box.padding(com.codename1.flutter.EdgeInsets.symmetric(0, CELL_SPACING_LP));
        box.child(row);
        return box;
    }

    private Widget pageButton(com.codename1.flutter.IconData glyph, boolean enabled,
            final long targetFirstRow) {
        IconButton b = new IconButton();
        b.icon(new com.codename1.flutter.widgets.Icon(glyph));
        if (enabled && onPageChanged != null) {
            b.onPressed(new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    onPageChanged.call(Long.valueOf(targetFirstRow));
                }
            });
        }
        return b;
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> body = new DartList<Widget>();
        if (header != null) {
            com.codename1.flutter.widgets.Container headerBox =
                    new com.codename1.flutter.widgets.Container();
            headerBox.height(64);
            headerBox.padding(com.codename1.flutter.EdgeInsets.symmetric(0, CELL_SPACING_LP * 2));
            headerBox.alignment(com.codename1.flutter.Alignment.centerLeft);
            headerBox.child(header);
            body.add(headerBox);
        }
        if (columns != null) {
            DartList<Widget> labels = new DartList<Widget>();
            for (int i = 0; i < columns.size(); i++) {
                labels.add(headingCell(i));
            }
            body.add(rowOf(labels, HEADING_HEIGHT_LP));
        }
        long total = 0;
        if (source != null) {
            total = source.rowCount();
            long end = initialFirstRowIndex + rowsPerPage;
            for (long i = initialFirstRowIndex; i < end && i < total; i++) {
                DataRow dr = source.getRow(i);
                if (dr == null || dr.getCells() == null) {
                    continue;
                }
                DartList<DataCell> cells = dr.getCells();
                DartList<Widget> cellWidgets = new DartList<Widget>();
                for (int j = 0; j < cells.size(); j++) {
                    cellWidgets.add(cells.get(j).getChild());
                }
                body.add(rowOf(cellWidgets, ROW_HEIGHT_LP));
            }
        }
        body.add(footer(total));

        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(body);
        return col;
    }
}

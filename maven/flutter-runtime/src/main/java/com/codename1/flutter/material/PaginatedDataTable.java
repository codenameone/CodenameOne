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

    public void header(Widget v) {
        this.header = v;
    }

    public void actions(DartList<Widget> v) {
    }

    public void columns(DartList<DataColumn> v) {
        this.columns = v;
    }

    public void sortColumnIndex(long v) {
    }

    public void sortAscending(boolean v) {
    }

    public void onSelectAll(Funcs.VoidFunc1<Boolean> v) {
    }

    public void dataRowHeight(double v) {
    }

    public void headingRowHeight(double v) {
    }

    public void horizontalMargin(double v) {
    }

    public void columnSpacing(double v) {
    }

    public void showCheckboxColumn(boolean v) {
    }

    public void showFirstLastButtons(boolean v) {
    }

    public void initialFirstRowIndex(long v) {
        this.initialFirstRowIndex = v;
    }

    public void onPageChanged(Funcs.VoidFunc1<Long> v) {
    }

    public void rowsPerPage(long v) {
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

    public void primary(boolean v) {
    }

    private static Row rowOf(DartList<Widget> cells) {
        DartList<Widget> flexed = new DartList<Widget>();
        for (int i = 0; i < cells.size(); i++) {
            Expanded e = new Expanded();
            e.child(cells.get(i));
            flexed.add(e);
        }
        Row r = new Row();
        r.children(flexed);
        return r;
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> body = new DartList<Widget>();
        if (header != null) {
            body.add(header);
        }
        if (columns != null) {
            DartList<Widget> labels = new DartList<Widget>();
            for (int i = 0; i < columns.size(); i++) {
                labels.add(columns.get(i).getLabel());
            }
            body.add(rowOf(labels));
        }
        if (source != null) {
            long total = source.rowCount();
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
                body.add(rowOf(cellWidgets));
            }
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(body);
        return col;
    }
}

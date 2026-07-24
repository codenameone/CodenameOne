package com.codename1.flutter.material;

import com.codename1.flutter.foundation.ChangeNotifier;

/**
 * A source of {@link DataRow}s for a {@link PaginatedDataTable} — Flutter's
 * {@code DataTableSource}. Application code subclasses this, overriding
 * {@link #getRow}, {@link #rowCount}, {@link #isRowCountApproximate} and
 * {@link #selectedRowCount}, and calls {@code notifyListeners()} (inherited from
 * {@link ChangeNotifier}) when the data changes.
 */
public abstract class DataTableSource implements ChangeNotifier {

    public abstract DataRow getRow(long index);

    public abstract long rowCount();

    public abstract boolean isRowCountApproximate();

    public abstract long selectedRowCount();
}

package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.async.Future;
import dart.core.DateTime;

/**
 * A material date-range-picker dialog — Flutter's {@code DateRangePickerDialog}.
 * This milestone renders a placeholder surface; the range calendar and
 * confirm/cancel flow land in a later pass. The configured bounds are retained.
 */
public class DateRangePickerDialog extends StatelessWidget {

    private String restorationId;
    private DateTime firstDate;
    private DateTime lastDate;
    private DateTime currentDate;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void firstDate(DateTime v) {
        this.firstDate = v;
    }

    public void lastDate(DateTime v) {
        this.lastDate = v;
    }

    public void currentDate(DateTime v) {
        this.currentDate = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }

    /** Top-level {@code showDateRangePicker(...)} — shows the dialog and completes with the chosen range. */
    public static Future<Object> show(BuildContext context, DateTime firstDate, DateTime lastDate) {
        return null;
    }
}

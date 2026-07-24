package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.async.Future;
import dart.core.DateTime;

/**
 * A material date-picker dialog — Flutter's {@code DatePickerDialog}. This
 * milestone renders a placeholder surface; the calendar grid and confirm/cancel
 * flow land in a later pass. The configured date range is retained.
 */
public class DatePickerDialog extends StatelessWidget {

    private String restorationId;
    private DateTime initialDate;
    private DateTime firstDate;
    private DateTime lastDate;
    private DateTime currentDate;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void initialDate(DateTime v) {
        this.initialDate = v;
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

    /** Top-level {@code showDatePicker(...)} — shows the dialog and completes with the chosen date. */
    public static Future<DateTime> show(BuildContext context, DateTime initialDate,
            DateTime firstDate, DateTime lastDate) {
        return null;
    }
}

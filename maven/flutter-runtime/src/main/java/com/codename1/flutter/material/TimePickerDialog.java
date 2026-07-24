package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.async.Future;

/**
 * A material time-picker dialog — Flutter's {@code TimePickerDialog}. This
 * milestone renders a placeholder surface; the clock face and confirm/cancel
 * flow land in a later pass. The initial time is retained.
 */
public class TimePickerDialog extends StatelessWidget {

    private String restorationId;
    private TimeOfDay initialTime;

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void initialTime(TimeOfDay v) {
        this.initialTime = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }

    /** Top-level {@code showTimePicker(...)} — shows the dialog and completes with the chosen time. */
    public static Future<TimeOfDay> show(BuildContext context, TimeOfDay initialTime) {
        return null;
    }
}

package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.core.DateTime;
import dart.runtime.Funcs;

/**
 * The iOS date/time wheel — Flutter's {@code CupertinoDatePicker}. The picker
 * wheel is not modeled this pass; it composes an empty {@link Container}
 * placeholder while keeping the correct API shape (mode / initial value / the
 * change callback are captured).
 */
public class CupertinoDatePicker extends StatelessWidget {

    private CupertinoDatePickerMode mode;
    private DateTime initialDateTime;
    private Funcs.VoidFunc1<DateTime> onDateTimeChanged;

    public void backgroundColor(Color v) {
    }

    public void mode(CupertinoDatePickerMode v) {
        this.mode = v;
    }

    public void initialDateTime(DateTime v) {
        this.initialDateTime = v;
    }

    public void minimumDate(DateTime v) {
    }

    public void maximumDate(DateTime v) {
    }

    public void minimumYear(long v) {
    }

    public void maximumYear(long v) {
    }

    public void minuteInterval(long v) {
    }

    public void use24hFormat(boolean v) {
    }

    public void onDateTimeChanged(Funcs.VoidFunc1<DateTime> v) {
        this.onDateTimeChanged = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}

package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Container;

import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * The iOS countdown-timer wheel — Flutter's {@code CupertinoTimerPicker}. Like
 * {@link CupertinoDatePicker}, the wheel is not modeled this pass; it composes
 * an empty {@link Container} placeholder and captures the change callback.
 */
public class CupertinoTimerPicker extends StatelessWidget {

    private Duration initialTimerDuration;
    private Funcs.VoidFunc1<Duration> onTimerDurationChanged;

    public void backgroundColor(Color v) {
    }

    public void mode(Object v) {
    }

    public void initialTimerDuration(Duration v) {
        this.initialTimerDuration = v;
    }

    public void minuteInterval(long v) {
    }

    public void secondInterval(long v) {
    }

    public void onTimerDurationChanged(Funcs.VoidFunc1<Duration> v) {
        this.onTimerDurationChanged = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}

package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.Duration;

/**
 * A container that expands (a "container transform") from a closed state to a
 * full page — the {@code animations} package's {@code OpenContainer}. The
 * {@code closedBuilder} paints the resting state and {@code openBuilder} the
 * opened page; each is a closure {@code (context, action)} where {@code action}
 * opens/closes the container. This pass renders the closed state via the
 * closedBuilder (falling back to an empty box); the expand transition is
 * deferred.
 */
public class OpenContainer<T> extends StatelessWidget {

    private Object onClosed;
    private CloseContainerBuilder closedBuilder;
    private CloseContainerBuilder openBuilder;
    private boolean tappable = true;
    private Duration transitionDuration;
    private Color closedColor;
    private Color openColor;
    private Color middleColor;
    private Double closedElevation;
    private Double openElevation;
    private Object closedShape;
    private Object openShape;

    public void onClosed(Object v) {
        this.onClosed = v;
    }

    public void closedBuilder(CloseContainerBuilder v) {
        this.closedBuilder = v;
    }

    public void openBuilder(CloseContainerBuilder v) {
        this.openBuilder = v;
    }

    public void tappable(boolean v) {
        this.tappable = v;
    }

    public void transitionDuration(Duration v) {
        this.transitionDuration = v;
    }

    public void transitionType(Object v) {
    }

    public void closedColor(Color v) {
        this.closedColor = v;
    }

    public void openColor(Color v) {
        this.openColor = v;
    }

    public void middleColor(Color v) {
        this.middleColor = v;
    }

    public void closedElevation(double v) {
        this.closedElevation = v;
    }

    public void openElevation(double v) {
        this.openElevation = v;
    }

    public void closedShape(Object v) {
        this.closedShape = v;
    }

    public void openShape(Object v) {
        this.openShape = v;
    }

    public void routeSettings(String v) {
    }

    public void useRootNavigator(boolean v) {
    }

    public Object getClosedBuilder() {
        return closedBuilder;
    }

    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.FlutterErrorReport.unimplemented("OpenContainer", "the container transform renders nothing");
        return new SizedBox();
    }
}

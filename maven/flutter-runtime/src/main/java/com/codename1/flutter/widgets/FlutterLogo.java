package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Curve;

import dart.core.Duration;

/**
 * The Flutter logo as a widget — Flutter's {@code FlutterLogo}. Signature-only:
 * size/color/animation params are captured; nothing is painted this pass.
 */
public class FlutterLogo extends StatelessWidget {

    private double size;
    private Color textColor;
    private Object style;
    private Duration duration;
    private Curve curve;

    public void size(double v) { this.size = v; }
    public void textColor(Color v) { this.textColor = v; }
    public void style(Object v) { this.style = v; }
    public void duration(Duration v) { this.duration = v; }
    public void curve(Curve v) { this.curve = v; }

    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.FlutterErrorReport.unimplemented("FlutterLogo", "renders nothing");
        return null;
    }
}

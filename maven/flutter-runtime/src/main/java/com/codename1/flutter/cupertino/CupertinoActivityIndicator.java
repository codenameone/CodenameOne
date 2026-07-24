package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

/**
 * The iOS spinner — Flutter's {@code CupertinoActivityIndicator}. The animated
 * ticks are not drawn this pass; it reserves the correct footprint (a
 * {@code 2 * radius} box, default radius 10) so surrounding layout matches.
 */
public class CupertinoActivityIndicator extends StatelessWidget {

    private Double radius;

    public void color(Color v) {
    }

    public void animating(boolean v) {
    }

    public void radius(double v) {
        this.radius = v;
    }

    @Override
    public Widget build(BuildContext context) {
        double diameter = (radius != null ? radius : 10.0) * 2.0;
        SizedBox b = new SizedBox();
        b.width(diameter);
        b.height(diameter);
        return b;
    }
}

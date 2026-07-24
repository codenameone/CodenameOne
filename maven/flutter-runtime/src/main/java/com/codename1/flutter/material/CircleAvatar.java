package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A circular avatar showing an image or a child (initials/icon) — Flutter's
 * {@code CircleAvatar}. This milestone renders the {@code child} when present,
 * otherwise a fixed-size box sized from {@code radius}; drawing the
 * {@code backgroundImage} clipped to a circle is deferred.
 */
public class CircleAvatar extends StatelessWidget {

    private Widget child;
    private Color backgroundColor;
    private Color foregroundColor;
    private ImageProvider backgroundImage;
    private ImageProvider foregroundImage;
    private Double radius;

    public void child(Widget v) {
        this.child = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void foregroundColor(Color v) {
        this.foregroundColor = v;
    }

    public void backgroundImage(ImageProvider v) {
        this.backgroundImage = v;
    }

    public void foregroundImage(ImageProvider v) {
        this.foregroundImage = v;
    }

    public void onBackgroundImageError(Object v) {
    }

    public void radius(double v) {
        this.radius = v;
    }

    public void minRadius(double v) {
    }

    public void maxRadius(double v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (child != null) {
            return child;
        }
        SizedBox box = new SizedBox();
        double r = radius != null ? radius : 20.0;
        box.width(r * 2);
        box.height(r * 2);
        return box;
    }
}

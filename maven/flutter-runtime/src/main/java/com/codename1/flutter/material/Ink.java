package com.codename1.flutter.material;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Decoration;
import com.codename1.flutter.EdgeInsetsGeometry;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Paints a decoration (or image) as part of the Material so ink splashes render
 * above it — Flutter's {@code Ink} (and its {@code Ink.image} named constructor).
 *
 * <p>The decoration is painted by delegating to a {@link com.codename1.flutter.widgets.Container},
 * which already knows how to paint a colour, a box decoration and a background image. It
 * used to render only the child, so an Ink used for a card's tinted or pictured background
 * came out blank.
 */
public class Ink extends StatelessWidget {

    private EdgeInsetsGeometry padding;
    private Color color;
    private Decoration decoration;
    private double width;
    private double height;
    private Widget child;
    private ImageProvider image;
    private BoxFit fit;

    public void padding(EdgeInsetsGeometry v) { this.padding = v; }
    public void color(Color v) { this.color = v; }
    public void decoration(Decoration v) { this.decoration = v; }
    public void width(double v) { this.width = v; }
    public void height(double v) { this.height = v; }
    public void child(Widget v) { this.child = v; }

    /** Dart's {@code Ink.image(...)} named constructor. */
    public static Ink image(com.codename1.flutter.Key key, ImageProvider image, BoxFit fit, Widget child,
            Double width, Double height, EdgeInsetsGeometry padding, Object colorFilter, Object alignment,
            Object repeat, Object centerSlice, Object onImageError) {
        Ink ink = new Ink();
        ink.image = image;
        ink.fit = fit;
        ink.child = child;
        if (width != null) ink.width = width;
        if (height != null) ink.height = height;
        ink.padding = padding;
        return ink;
    }

    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.widgets.Container box =
                new com.codename1.flutter.widgets.Container();
        if (color != null) {
            box.color(color);
        }
        if (decoration != null) {
            box.decoration(decoration);
        } else if (image != null) {
            com.codename1.flutter.DecorationImage backdrop = new com.codename1.flutter.DecorationImage();
            backdrop.image(image);
            if (fit != null) {
                backdrop.fit(fit);
            }
            com.codename1.flutter.BoxDecoration d = new com.codename1.flutter.BoxDecoration();
            d.image(backdrop);
            box.decoration(d);
        }
        if (padding != null) {
            box.padding(padding);
        }
        if (width > 0) {
            box.width(width);
        }
        if (height > 0) {
            box.height(height);
        }
        box.child(child);
        return box;
    }
}

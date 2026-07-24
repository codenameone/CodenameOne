package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * An icon rendered from an {@link ImageProvider} rather than an icon font —
 * Flutter's {@code ImageIcon}. Captures the image, size and tint; this pass
 * reserves the icon's box via a {@link SizedBox}, with the actual image decode
 * and tinting deferred to the image layer.
 */
public class ImageIcon extends StatelessWidget {

    private final ImageProvider image;
    private Double size;
    private Color color;

    public ImageIcon(ImageProvider image) {
        this.image = image;
    }

    public void size(double v) {
        this.size = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void semanticLabel(String v) {
    }

    public ImageProvider getImage() {
        return image;
    }

    @Override
    public Widget build(BuildContext context) {
        SizedBox box = new SizedBox();
        double side = size != null ? size : 24.0;
        box.width(side);
        box.height(side);
        return box;
    }
}

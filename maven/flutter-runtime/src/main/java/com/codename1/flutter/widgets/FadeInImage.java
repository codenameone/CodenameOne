package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.Duration;

/**
 * Shows a {@code placeholder} image while the target {@code image} loads, then
 * cross-fades to it — Flutter's {@code FadeInImage}. This pass reserves the
 * box (when width/height are given) and holds both providers; the decode and
 * fade animation are deferred to the image layer.
 */
public class FadeInImage extends StatelessWidget {

    private ImageProvider placeholder;
    private ImageProvider image;
    private Duration fadeInDuration;
    private Duration fadeOutDuration;
    private Double width;
    private Double height;
    private Object fit;

    public void placeholder(ImageProvider v) {
        this.placeholder = v;
    }

    public void image(ImageProvider v) {
        this.image = v;
    }

    public void fadeInDuration(Duration v) {
        this.fadeInDuration = v;
    }

    public void fadeOutDuration(Duration v) {
        this.fadeOutDuration = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void fit(Object v) {
        this.fit = v;
    }

    public void alignment(Object v) {
    }

    public void repeat(Object v) {
    }

    public void placeholderFit(Object v) {
    }

    /** Whether the image is hidden from semantics — Flutter's {@code excludeFromSemantics}. */
    public void excludeFromSemantics(boolean v) {
    }

    public ImageProvider getImage() {
        return image;
    }

    @Override
    public Widget build(BuildContext context) {
        // Render the target image directly (the cross-fade from the placeholder is
        // deferred). Falls back to the placeholder, then an empty box, when absent.
        ImageProvider shown = image != null ? image : placeholder;
        if (shown != null) {
            Image img = new Image();
            img.image(shown);
            if (width != null) {
                img.width(width);
            }
            if (height != null) {
                img.height(height);
            }
            if (fit != null) {
                img.fit(fit);
            }
            return img;
        }
        SizedBox box = new SizedBox();
        if (width != null) {
            box.width(width);
        }
        if (height != null) {
            box.height(height);
        }
        return box;
    }
}

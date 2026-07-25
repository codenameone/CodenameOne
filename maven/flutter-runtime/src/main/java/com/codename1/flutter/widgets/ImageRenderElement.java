package com.codename1.flutter.widgets;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.FlutterAssets;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.io.Log;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Label;
import com.codename1.ui.URLImage;

import java.io.InputStream;

/**
 * Leaf render box for {@link Image} (UIID "FlutterImage").
 *
 * <ul>
 *   <li><b>asset</b>: loaded from {@code /assets/&lt;name&gt;} on the
 *       classpath (the app build copies {@code src/main/flutter/assets}
 *       there) as an EncodedImage.</li>
 *   <li><b>network</b>: a {@link URLImage} with a transparent placeholder
 *       sized from the width/height parameters (or 100lp), downloaded to
 *       storage and scaled by the URLImage adapter — the BoxFit for network
 *       images is therefore approximated by RESIZE_SCALE.</li>
 * </ul>
 *
 * <p>Sizing: explicit width/height tighten the incoming constraints; the
 * natural image size fills in unconstrained axes. BoxFit maps onto CN1 image
 * scaling at position time: {@code fill} stretches, {@code contain} (the
 * default) scales the larger dimension to fit, {@code cover} uses
 * {@code Image.fill} (scale smaller dimension, center-crop approximation),
 * {@code fitWidth}/{@code fitHeight} scale one axis, {@code none} keeps the
 * natural size.</p>
 */
public class ImageRenderElement extends RenderElement {

    private static final double FALLBACK_EXTENT_LP = 100;

    private com.codename1.ui.Image img;
    private String loadedSource;
    /** Device pixels per logical pixel in the loaded asset file (1, 2, 3...). */
    private double assetRatio = 1;

    public ImageRenderElement(Image widget) {
        super(widget);
    }

    private Image image() {
        return (Image) widget();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Label l = new Label("", "FlutterImage");
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        loadImage(l);
        return l;
    }

    @Override
    protected void updateComponent(Component c) {
        loadImage((Label) c);
    }

    private void loadImage(Label l) {
        String source = image().sourceKey();
        if (source.equals(loadedSource)) {
            return;
        }
        loadedSource = source;
        img = null;
        assetRatio = 1;
        try {
            if (image().getAssetName() != null) {
                FlutterAssets.Resolved res = FlutterAssets.open(getClass(), image().getAssetName());
                if (res == null) {
                    Log.p("Flutter runtime: asset image not found: " + image().getAssetName()
                            + " (resource " + FlutterAssets.resourceName(image().getAssetName()) + ")");
                } else {
                    img = EncodedImage.create(res.stream());
                    assetRatio = res.ratio();
                }
            } else if (image().getUrl() != null) {
                int pw = (int) Math.max(1, Math.round(Dp.px(
                        image().getWidth() != null ? image().getWidth() : FALLBACK_EXTENT_LP)));
                int ph = (int) Math.max(1, Math.round(Dp.px(
                        image().getHeight() != null ? image().getHeight() : FALLBACK_EXTENT_LP)));
                EncodedImage placeholder = EncodedImage.createFromImage(
                        com.codename1.ui.Image.createImage(pw, ph, 0x0), false);
                img = URLImage.createToStorage(placeholder,
                        "flutter-img-" + image().getUrl().hashCode(),
                        image().getUrl(), URLImage.RESIZE_SCALE);
            }
        } catch (Exception err) {
            Log.p("Flutter runtime: could not load image " + source);
            Log.e(err);
        }
        l.setIcon(img);
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Double wPx = image().getWidth() == null ? null : Double.valueOf(Dp.px(image().getWidth()));
        Double hPx = image().getHeight() == null ? null : Double.valueOf(Dp.px(image().getHeight()));
        BoxConstraints inner = constraints.tighten(wPx, hPx);
        // A density variant carries `assetRatio` device pixels per logical
        // pixel, so its natural size on screen is the decoded size rescaled
        // from that density to the screen's — a 3.0x file on a 3x screen is
        // 1:1, the same file on a 2x screen is two thirds the size.
        double naturalScale = assetRatio > 0 ? Dp.scale() / assetRatio : 1;
        Size natural = img == null
                ? new Size(wPx == null ? 0 : wPx, hPx == null ? 0 : hPx)
                : new Size(img.getWidth() * naturalScale, img.getHeight() * naturalScale);
        return inner.constrain(natural);
    }

    @Override
    public void position(int x, int y) {
        super.position(x, y);
        applyFit();
    }

    /**
     * Scales the icon into the laid-out box per the BoxFit (best-effort CN1
     * approximation; URLImage instances are left to their adapter).
     */
    private void applyFit() {
        Label l = (Label) component();
        if (l == null || img == null || img instanceof URLImage) {
            return;
        }
        int bw = (int) Math.round(size().width());
        int bh = (int) Math.round(size().height());
        int iw = img.getWidth();
        int ih = img.getHeight();
        if (bw <= 0 || bh <= 0 || iw <= 0 || ih <= 0) {
            return;
        }
        BoxFit fit = image().getFit() == null ? BoxFit.contain : image().getFit();
        com.codename1.ui.Image scaled;
        switch (fit) {
            case fill:
                scaled = img.scaled(bw, bh);
                break;
            case cover:
                scaled = img.fill(bw, bh);
                break;
            case fitWidth:
                scaled = img.scaled(bw, Math.max(1, ih * bw / iw));
                break;
            case fitHeight:
                scaled = img.scaled(Math.max(1, iw * bh / ih), bh);
                break;
            case none:
                scaled = img;
                break;
            case contain:
            default:
                double r = Math.min((double) bw / iw, (double) bh / ih);
                scaled = img.scaled(Math.max(1, (int) Math.round(iw * r)),
                        Math.max(1, (int) Math.round(ih * r)));
                break;
        }
        l.setIcon(scaled);
    }
}

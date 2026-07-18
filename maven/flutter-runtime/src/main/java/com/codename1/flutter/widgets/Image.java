package com.codename1.flutter.widgets;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

/**
 * An image, created via Dart's {@code Image.asset} (bundled under the app's
 * {@code /assets} resources) or {@code Image.network} named constructors.
 * Backed by a CN1 Label (UIID "FlutterImage") carrying an EncodedImage or a
 * URLImage.
 */
public class Image extends Widget {

    private final String assetName;
    private final String url;
    private Double width;
    private Double height;
    private BoxFit fit;

    private Image(String assetName, String url) {
        this.assetName = assetName;
        this.url = url;
    }

    /**
     * Dart's {@code Image.asset} named constructor in canonical positional
     * form.
     */
    public static Image asset(String name, Key key, Double width, Double height, BoxFit fit) {
        Image i = new Image(name, null);
        i.key(key);
        i.width = width;
        i.height = height;
        i.fit = fit;
        return i;
    }

    /**
     * Dart's {@code Image.network} named constructor in canonical positional
     * form.
     */
    public static Image network(String src, Key key, Double width, Double height, BoxFit fit) {
        Image i = new Image(null, src);
        i.key(key);
        i.width = width;
        i.height = height;
        i.fit = fit;
        return i;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getUrl() {
        return url;
    }

    public Double getWidth() {
        return width;
    }

    public Double getHeight() {
        return height;
    }

    public BoxFit getFit() {
        return fit;
    }

    /**
     * A stable identity for the image source, used to detect source changes
     * across in-place widget updates.
     */
    public String sourceKey() {
        return assetName != null ? "asset:" + assetName : "url:" + url;
    }

    @Override
    public Element createElement() {
        return new ImageRenderElement(this);
    }
}

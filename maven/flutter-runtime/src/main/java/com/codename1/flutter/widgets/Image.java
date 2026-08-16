package com.codename1.flutter.widgets;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * An image, created via Dart's {@code Image.asset} (bundled under the app's
 * {@code /assets} resources) or {@code Image.network} named constructors.
 * Backed by a CN1 Label (UIID "FlutterImage") carrying an EncodedImage or a
 * URLImage.
 */
public class Image extends Widget {

    private String assetName;
    private String url;
    private Double width;
    private Double height;
    private BoxFit fit;
    private ImageProvider imageProvider;
    private Funcs.Func4<BuildContext, Widget, Long, Boolean, Widget> frameBuilder;

    private Image(String assetName, String url) {
        this.assetName = assetName;
        this.url = url;
    }

    /** Dart's unnamed {@code Image(image: ...)} constructor, in allocate-then-setters form. */
    public Image() {
    }

    /**
     * {@code Image(image: provider)}: resolves the provider's source into this widget's
     * asset/url identity so the render path is unchanged.
     */
    public void image(ImageProvider v) {
        this.imageProvider = v;
        if (v != null) {
            String key = v.sourceKey();
            if (key != null && key.startsWith("asset:")) {
                this.assetName = key.substring("asset:".length());
            } else if (key != null && key.startsWith("url:")) {
                this.url = key.substring("url:".length());
            }
        }
    }

    public void width(Double v) {
        this.width = v;
    }

    public void height(Double v) {
        this.height = v;
    }

    public void fit(Object v) {
        this.fit = (v instanceof BoxFit) ? (BoxFit) v : null;
    }

    public void excludeFromSemantics(boolean v) {
    }

    public void frameBuilder(Funcs.Func4<BuildContext, Widget, Long, Boolean, Widget> v) {
        this.frameBuilder = v;
    }

    /**
     * Dart's {@code Image.asset} named constructor in canonical positional
     * form.
     */
    public static Image asset(String name, Key key, Double width, Double height, BoxFit fit,
                              String packageName) {
        // The package qualifier is part of the PATH, exactly as in AssetImage: an asset
        // shipped by a package lives at packages/<package>/<name>. Dropping it - which is
        // what happened while this factory had no such parameter - leaves the image looking
        // for a file that is not there, and the widget renders nothing with no error.
        Image i = new Image(com.codename1.flutter.AssetImage.qualify(name, packageName), null);
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

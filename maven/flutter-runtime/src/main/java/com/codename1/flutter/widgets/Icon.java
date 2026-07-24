package com.codename1.flutter.widgets;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.IconData;
import com.codename1.flutter.Widget;

/**
 * A material icon glyph, backed by a CN1 Label with a FontImage
 * (UIID "FlutterIcon"). Default size 24 logical pixels.
 */
public class Icon extends Widget {

    private final IconData icon;
    private Double size;
    private Color color;
    private String semanticLabel;

    public Icon(IconData icon) {
        this.icon = icon;
    }

    public void semanticLabel(String v) {
        this.semanticLabel = v;
    }

    public String getSemanticLabel() {
        return semanticLabel;
    }

    public void size(double v) {
        this.size = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public IconData getIcon() {
        return icon;
    }

    public Double getSize() {
        return size;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public Element createElement() {
        return new IconRenderElement(this);
    }
}

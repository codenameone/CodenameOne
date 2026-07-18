package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * A run of styled text, backed by a CN1 Label (UIID "FlutterText").
 */
public class Text extends Widget {

    private final String data;
    private TextStyle style;
    private TextAlign textAlign;

    public Text(String data) {
        this.data = data;
    }

    public void style(TextStyle v) {
        this.style = v;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public String getData() {
        return data;
    }

    public TextStyle getStyle() {
        return style;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    @Override
    public Element createElement() {
        return new TextRenderElement(this);
    }
}

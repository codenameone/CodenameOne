package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextOverflow;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * A run of styled text, backed by a CN1 Label (UIID "FlutterText").
 */
public class Text extends Widget {

    private final String data;
    private TextStyle style;
    private TextAlign textAlign;
    private String semanticsLabel;
    private TextOverflow overflow;
    private Long maxLines;

    public Text(String data) {
        this.data = data;
    }

    public void style(TextStyle v) {
        this.style = v;
    }

    public void semanticsLabel(String v) {
        this.semanticsLabel = v;
    }

    public void overflow(TextOverflow v) {
        this.overflow = v;
    }

    public void maxLines(long v) {
        this.maxLines = v;
    }

    public void softWrap(boolean v) {
    }

    public String getSemanticsLabel() {
        return semanticsLabel;
    }

    public TextOverflow getOverflow() {
        return overflow;
    }

    public Long getMaxLines() {
        return maxLines;
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

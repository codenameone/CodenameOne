package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.Widget;

/**
 * A paragraph of mixed-style text described by a {@link TextSpan} tree
 * (Flutter's RichText). The span tree is flattened into styled runs, wrapped
 * across lines with per-run fonts, and custom-painted honoring
 * {@link TextAlign}.
 */
public class RichText extends Widget {

    private TextSpan text;
    private TextAlign textAlign;

    public void text(TextSpan v) {
        this.text = v;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public TextSpan getText() {
        return text;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    @Override
    public Element createElement() {
        return new RichTextRenderElement(this);
    }
}

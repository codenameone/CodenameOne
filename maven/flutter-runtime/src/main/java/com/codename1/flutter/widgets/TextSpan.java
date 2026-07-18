package com.codename1.flutter.widgets;

import com.codename1.flutter.TextStyle;

import dart.core.DartList;

/**
 * A node in a styled-text tree (Flutter's TextSpan): an optional text run,
 * an optional style, and optional child spans. A child span INHERITS every
 * style property its own style leaves null from its parent chain (see
 * {@link RichTextRenderElement#flatten}).
 *
 * <p>Not a Widget — it is configuration consumed by {@link RichText}.</p>
 */
public class TextSpan {

    private String text;
    private TextStyle style;
    private DartList<TextSpan> children;

    public void text(String v) {
        this.text = v;
    }

    public void style(TextStyle v) {
        this.style = v;
    }

    public void children(DartList<TextSpan> v) {
        this.children = v;
    }

    public String getText() {
        return text;
    }

    public TextStyle getStyle() {
        return style;
    }

    public DartList<TextSpan> getChildren() {
        return children;
    }
}

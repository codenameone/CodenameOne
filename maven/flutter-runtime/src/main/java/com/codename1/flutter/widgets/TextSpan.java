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
public class TextSpan extends InlineSpan {

    private String text;
    private TextStyle style;
    private DartList<TextSpan> children;
    private Object recognizer;

    public void text(String v) {
        this.text = v;
    }

    public void recognizer(Object v) {
        this.recognizer = v;
    }

    public Object getRecognizer() {
        return recognizer;
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

    /**
     * {@code InlineSpan.toPlainText}: the concatenated raw text of this span and
     * all descendant spans, in depth-first order.
     */
    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        appendPlainText(sb);
        return sb.toString();
    }

    private void appendPlainText(StringBuilder sb) {
        if (text != null) {
            sb.append(text);
        }
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                TextSpan c = children.get(i);
                if (c != null) {
                    c.appendPlainText(sb);
                }
            }
        }
    }
}

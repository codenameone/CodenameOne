package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Key;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * Selectable, non-editable text — Flutter's {@code SelectableText}. Text
 * selection is not yet wired, so this composes a plain {@link Text} (or
 * {@link RichText} for the {@code .rich} constructor), which is faithful to the
 * rendered appearance.
 */
public class SelectableText extends StatelessWidget {

    private final String data;
    private TextSpan textSpan;
    private TextStyle style;
    private TextAlign textAlign;

    public SelectableText(String data) {
        this.data = data;
    }

    /**
     * Dart's {@code SelectableText.rich} named constructor in canonical
     * positional form.
     */
    public static SelectableText rich(TextSpan textSpan, Key key, TextStyle style,
                                      TextAlign textAlign, Long maxLines,
                                      Object textDirection) {
        SelectableText t = new SelectableText(null);
        t.key(key);
        t.textSpan = textSpan;
        t.style = style;
        t.textAlign = textAlign;
        return t;
    }

    public void style(TextStyle v) {
        this.style = v;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public void maxLines(long v) {
    }

    public void textScaleFactor(double v) {
    }

    public void showCursor(boolean v) {
    }

    public void semanticsLabel(String v) {
    }

    public void cursorColor(Object v) {
    }

    public void onTap(Object v) {
    }

    public void focusNode(Object v) {
    }

    public void scrollPhysics(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (textSpan != null) {
            RichText r = new RichText();
            r.text(textSpan);
            if (textAlign != null) {
                r.textAlign(textAlign);
            }
            return r;
        }
        Text t = new Text(data);
        if (style != null) {
            t.style(style);
        }
        if (textAlign != null) {
            t.textAlign(textAlign);
        }
        return t;
    }
}

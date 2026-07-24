package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * The default {@link TextStyle} for descendant {@code Text} widgets that do not
 * supply their own — Flutter's {@code DefaultTextStyle}, an
 * {@link InheritedWidget}. This pass stores the style and text layout hints and
 * renders its single {@code child}; propagating the style into unstyled Text is
 * deferred to the text layer.
 */
public class DefaultTextStyle extends InheritedWidget {

    private TextStyle style;
    private TextAlign textAlign;
    private Boolean softWrap;
    private Object overflow;
    private Integer maxLines;

    public void style(TextStyle v) {
        this.style = v;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public void softWrap(boolean v) {
        this.softWrap = v;
    }

    public void overflow(Object v) {
        this.overflow = v;
    }

    public void maxLines(int v) {
        this.maxLines = v;
    }

    public TextStyle getStyle() {
        return style;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    /**
     * Nearest ancestor DefaultTextStyle — Flutter's {@code
     * DefaultTextStyle.of(context)}. Inherited-widget lookup is not yet wired,
     * so this returns an empty fallback whose style is null.
     */
    public static DefaultTextStyle of(BuildContext context) {
        return new DefaultTextStyle();
    }
}

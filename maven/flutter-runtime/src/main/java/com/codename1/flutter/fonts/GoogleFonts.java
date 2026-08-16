package com.codename1.flutter.fonts;

import com.codename1.flutter.Color;
import com.codename1.flutter.FontWeight;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.material.TextTheme;

/**
 * A no-op equivalent of the {@code google_fonts} package. Flutter's GoogleFonts
 * downloads/registers web fonts and returns a styled {@link TextStyle}; here we
 * return a TextStyle carrying the requested size/weight/color and fall back to
 * the platform font (faithful web-font loading is a later pass). The
 * {@code *TextTheme} helpers pass the supplied theme straight through.
 */
public abstract class GoogleFonts {

    /** {@code GoogleFonts.config} — a static getter, hence a static field here. */
    public static final GoogleFontsConfig config = new GoogleFontsConfig();

    private GoogleFonts() {
    }

    /// {@code textStyle:} is the BASE the rest are layered onto - google_fonts copies the
    /// given style and overrides only what was named. Ignoring it dropped whichever theme
    /// style the caller was extending, so the text kept the font and lost everything else.
    private static TextStyle style(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        TextStyle t = textStyle != null
                ? textStyle.copyWith(null, null, null, null, null, null, null, null, null, null,
                        null, null, null)
                : new TextStyle();
        if (fontSize > 0) {
            t.fontSize(fontSize);
        }
        if (fontWeight != null) {
            t.fontWeight(fontWeight);
        }
        if (color != null) {
            t.color(color);
        }
        if (letterSpacing != null) {
            t.letterSpacing(letterSpacing.doubleValue());
        }
        if (height != null) {
            t.height(height.doubleValue());
        }
        return t;
    }

    public static TextStyle eczar(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle libreFranklin(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle merriweather(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle montserrat(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle oswald(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle robotoCondensed(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle robotoMono(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle workSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme ralewayTextTheme(TextTheme textTheme) {
        return textTheme != null ? textTheme : new TextTheme();
    }

    public static TextTheme rubikTextTheme(TextTheme textTheme) {
        return textTheme != null ? textTheme : new TextTheme();
    }

    public static TextTheme workSansTextTheme(TextTheme textTheme) {
        return textTheme != null ? textTheme : new TextTheme();
    }
}

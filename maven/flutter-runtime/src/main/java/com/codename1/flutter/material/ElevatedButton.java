package com.codename1.flutter.material;

/**
 * The material filled button: a primary-colored capsule with the on-primary
 * foreground, backed by a CN1 Button (UIID "FlutterElevatedButton").
 */
public class ElevatedButton extends ButtonBase {

    /**
     * Builds a {@link ButtonStyle} to hand to an ElevatedButton's {@code
     * style:} parameter. Parameter order matches the Dart stub.
     */
    public static ButtonStyle styleFrom(com.codename1.flutter.Color foregroundColor,
            com.codename1.flutter.Color backgroundColor, com.codename1.flutter.Color shadowColor,
            Double elevation, com.codename1.flutter.TextStyle textStyle,
            com.codename1.flutter.EdgeInsets padding, Object side, Object shape, Object alignment,
            Object tapTargetSize, Object visualDensity) {
        return ButtonStyle.styleFrom(foregroundColor, backgroundColor, shadowColor, elevation, textStyle,
                padding, side, shape, alignment, tapTargetSize, visualDensity);
    }

    /**
     * {@code ElevatedButton.icon}: a button whose content is an icon followed
     * by a label. This milestone consumes the label as the button content (the
     * leading icon is used when no label is supplied); a later pass composes
     * both into a Row.
     */
    public static ElevatedButton icon(com.codename1.flutter.Key key,
            dart.runtime.Funcs.VoidFunc0 onPressed, ButtonStyle style,
            com.codename1.flutter.Widget icon, com.codename1.flutter.Widget label) {
        ElevatedButton b = new ElevatedButton();
        b.key(key);
        b.onPressed(onPressed);
        b.style(style);
        b.child(label != null ? label : icon);
        return b;
    }
}

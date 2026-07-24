package com.codename1.flutter.material;

/**
 * The set of geometry-specific {@link TextTheme}s for a Material design
 * language — Flutter's {@code Typography}. A ThemeData is built from
 * {@code Typography.material2018(...)}; its {@code englishLike}/{@code dense}/
 * {@code tall} themes are merged by script. This pass records the supplied
 * themes; when none are given the getters return {@code null} and the caller's
 * ThemeData falls back to its own defaults.
 */
public class Typography {

    private Object platform;
    private TextTheme black;
    private TextTheme white;
    private TextTheme englishLike;
    private TextTheme dense;
    private TextTheme tall;

    private Typography() {
    }

    /** Dart's {@code Typography.material2018(...)} factory. */
    public static Typography material2018(Object platform, TextTheme black, TextTheme white,
            TextTheme englishLike, TextTheme dense, TextTheme tall) {
        return build(platform, black, white, englishLike, dense, tall);
    }

    /** Dart's {@code Typography.material2014(...)} factory. */
    public static Typography material2014(Object platform, TextTheme black, TextTheme white,
            TextTheme englishLike, TextTheme dense, TextTheme tall) {
        return build(platform, black, white, englishLike, dense, tall);
    }

    private static Typography build(Object platform, TextTheme black, TextTheme white,
            TextTheme englishLike, TextTheme dense, TextTheme tall) {
        Typography t = new Typography();
        t.platform = platform;
        t.black = black;
        t.white = white;
        t.englishLike = englishLike;
        t.dense = dense;
        t.tall = tall;
        return t;
    }

    public TextTheme black() {
        return black;
    }

    public TextTheme white() {
        return white;
    }

    public TextTheme englishLike() {
        return englishLike;
    }

    public TextTheme dense() {
        return dense;
    }

    public TextTheme tall() {
        return tall;
    }
}

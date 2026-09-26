/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.flutter.material;

import com.codename1.flutter.Brightness;
import com.codename1.flutter.Color;

/**
 * Material theme configuration: a color scheme, text themes, brightness and the
 * component sub-theme bundles. When no explicit color scheme is set one is
 * derived from the default M3 seed honoring the brightness. M4 maps the ACTIVE
 * ThemeData onto the CN1 UIManager through {@link ThemeDataAdapter}.
 *
 * <p>Named Dart constructor parameters map to setter methods; {@link #copyWith}
 * returns a merged copy. Sub-theme bundles owned by other runtime areas
 * (snackBarTheme, inputDecorationTheme, ...) are held opaquely as {@code Object}.</p>
 */
public class ThemeData {

    private static final Color DEFAULT_SEED = new Color(0xFF6750A4);

    private ColorScheme colorScheme;
    private TextTheme textTheme;
    private TextTheme resolvedTextTheme;
    private TextTheme primaryTextTheme = new TextTheme();
    private boolean useMaterial3 = true;
    private Brightness brightness;

    private Color primaryColor;
    private Color scaffoldBackgroundColor;
    private Color canvasColor;
    private Color cardColor;
    private Color dividerColor;
    private Color focusColor;
    private Color highlightColor;
    private Color splashColor;
    private Color hintColor;
    private Color disabledColor;
    private Color shadowColor;
    private Color indicatorColor;
    private Color secondaryHeaderColor;

    private IconThemeData iconTheme;
    private IconThemeData primaryIconTheme;
    private AppBarTheme appBarTheme;
    private ChipThemeData chipTheme;
    private CheckboxThemeData checkboxTheme;
    private CardTheme cardTheme;
    private BottomAppBarThemeData bottomAppBarTheme;
    private DividerThemeData dividerTheme;
    private NavigationRailThemeData navigationRailTheme;

    private Object snackBarTheme;
    private Object inputDecorationTheme;
    private Object radioTheme;
    private Object switchTheme;
    private Object tooltipTheme;
    private BottomSheetThemeData bottomSheetTheme;
    private SliderThemeData sliderTheme;
    private Object floatingActionButtonTheme;
    private Object elevatedButtonTheme;
    private Object textButtonTheme;
    private Object outlinedButtonTheme;
    private Object pageTransitionsTheme;
    private Object visualDensity;
    private Object typography;
    private Object platform;
    private Boolean applyElevationOverlayColor;
    private String fontFamily;

    /**
     * {@code ThemeData.dark}: a theme whose brightness is dark; the color
     * scheme is derived from the default seed honoring that brightness.
     */
    public static ThemeData dark(Boolean useMaterial3) {
        ThemeData t = new ThemeData();
        t.brightness(Brightness.dark);
        if (useMaterial3 != null) {
            t.useMaterial3(useMaterial3);
        }
        return t;
    }

    // ------------------------------------------------------------------
    // Named-parameter setters
    // ------------------------------------------------------------------

    public void colorScheme(ColorScheme v) { this.colorScheme = v; }
    public void colorSchemeSeed(Color seed) { this.colorScheme = ColorScheme.fromSeed(seed, brightness); }
    public void useMaterial3(boolean v) { this.useMaterial3 = v; }
    public void brightness(Brightness v) { this.brightness = v; }
    public void textTheme(TextTheme v) { this.textTheme = v; }
    public void primaryTextTheme(TextTheme v) { this.primaryTextTheme = v; }
    public void primaryColor(Color v) { this.primaryColor = v; }
    public void scaffoldBackgroundColor(Color v) { this.scaffoldBackgroundColor = v; }
    public void canvasColor(Color v) { this.canvasColor = v; }
    public void cardColor(Color v) { this.cardColor = v; }
    public void dividerColor(Color v) { this.dividerColor = v; }
    public void focusColor(Color v) { this.focusColor = v; }
    public void highlightColor(Color v) { this.highlightColor = v; }
    public void splashColor(Color v) { this.splashColor = v; }
    public void hintColor(Color v) { this.hintColor = v; }
    public void disabledColor(Color v) { this.disabledColor = v; }
    public void shadowColor(Color v) { this.shadowColor = v; }
    public void indicatorColor(Color v) { this.indicatorColor = v; }
    public void secondaryHeaderColor(Color v) { this.secondaryHeaderColor = v; }
    public void iconTheme(IconThemeData v) { this.iconTheme = v; }
    public void primaryIconTheme(IconThemeData v) { this.primaryIconTheme = v; }
    public void appBarTheme(AppBarTheme v) { this.appBarTheme = v; }
    public void chipTheme(ChipThemeData v) { this.chipTheme = v; }
    public void checkboxTheme(CheckboxThemeData v) { this.checkboxTheme = v; }
    public void cardTheme(CardTheme v) { this.cardTheme = v; }
    public void bottomAppBarTheme(BottomAppBarThemeData v) { this.bottomAppBarTheme = v; }
    public void dividerTheme(DividerThemeData v) { this.dividerTheme = v; }
    public void navigationRailTheme(NavigationRailThemeData v) { this.navigationRailTheme = v; }
    public void snackBarTheme(Object v) { this.snackBarTheme = v; }
    public void inputDecorationTheme(Object v) { this.inputDecorationTheme = v; }

    /**
     * {@code inputDecorationTheme}, when it is one this runtime understands.
     *
     * <p>Held opaquely because most of it is borders this port cannot draw, but
     * the fill IS drawable and the studies depend on it: Rally's login fields
     * are a dark fill on a dark page and rendered as white blocks without it.</p>
     */
    public InputDecorationThemeData inputDecorationTheme() {
        return inputDecorationTheme instanceof InputDecorationThemeData
                ? (InputDecorationThemeData) inputDecorationTheme : null;
    }
    public void radioTheme(Object v) { this.radioTheme = v; }
    public void switchTheme(Object v) { this.switchTheme = v; }
    public void tooltipTheme(Object v) { this.tooltipTheme = v; }
    public void bottomSheetTheme(BottomSheetThemeData v) { this.bottomSheetTheme = v; }
    public BottomSheetThemeData bottomSheetTheme() { return bottomSheetTheme != null ? bottomSheetTheme : new BottomSheetThemeData(); }
    public void sliderTheme(SliderThemeData v) { this.sliderTheme = v; }
    public SliderThemeData sliderTheme() { return sliderTheme != null ? sliderTheme : new SliderThemeData(); }
    public void floatingActionButtonTheme(Object v) { this.floatingActionButtonTheme = v; }
    public void elevatedButtonTheme(Object v) { this.elevatedButtonTheme = v; }
    public void textButtonTheme(Object v) { this.textButtonTheme = v; }
    public void outlinedButtonTheme(Object v) { this.outlinedButtonTheme = v; }
    /// The text-selection colours a study sets ({@code TextSelectionThemeData}). Held for
    /// the styling pass; selection rendering does not read it yet, but dropping the value
    /// entirely made the setting look unsupported rather than pending.
    private TextSelectionThemeData textSelectionTheme;

    public void textSelectionTheme(TextSelectionThemeData v) {
        this.textSelectionTheme = v;
    }

    public TextSelectionThemeData getTextSelectionTheme() {
        return textSelectionTheme;
    }

    public void pageTransitionsTheme(PageTransitionsTheme v) { this.pageTransitionsTheme = v; }
    public void visualDensity(Object v) { this.visualDensity = v; }
    public void typography(Object v) { this.typography = v; }
    public void platform(Object v) { this.platform = v; }
    public void applyElevationOverlayColor(boolean v) { this.applyElevationOverlayColor = v; }
    public void fontFamily(String v) { this.fontFamily = v; }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public boolean getUseMaterial3() { return useMaterial3; }

    /** The declared brightness, defaulting to light. */
    public Brightness brightness() { return brightness == null ? Brightness.light : brightness; }

    public ColorScheme colorScheme() {
        if (colorScheme == null) {
            colorScheme = ColorScheme.fromSeed(DEFAULT_SEED, brightness);
        }
        return colorScheme;
    }

    /**
     * The effective text theme: what {@code textTheme:} was given, otherwise the
     * scale the {@code typography:} asks for, otherwise the Material 3 defaults.
     *
     * <p>A theme that names {@code Typography.material2018} is asking for the
     * Material 2 type scale and inks, which differ from M3 in every size and in
     * the colour of the display roles. Ignoring that (which is what returning a
     * bare TextTheme did) rendered the gallery's demos in the wrong scale
     * throughout, most visibly on its own typography page.</p>
     */
    public TextTheme textTheme() {
        if (resolvedTextTheme == null) {
            // No typography named means the Material 3 default, not "no type scale at
            // all". An empty TextTheme leaves every role without a size or a line
            // height, so text falls back to whatever the font measures -- which is a few
            // logical pixels short per line, once per line, on every screen.
            resolvedTextTheme = typography instanceof Typography
                    ? ((Typography) typography).resolve(brightness == Brightness.dark)
                    : Typography.material2021().resolve(brightness == Brightness.dark);
            if (textTheme != null) {
                // MERGED over the defaults, not used instead of them. A style handed in
                // here states what it wants to change and inherits the rest, and the
                // thing it most often does not state is the LINE HEIGHT: Reply restyles
                // its roles with a font, a weight and a tracking, and Flutter keeps the
                // type scale's height under them. Returning the app's styles raw dropped
                // it, so every line was laid out at whatever the font measured -- three
                // lines per mail card, 26 device pixels short each card, and a list that
                // drifted further out of place the further down it went.
                resolvedTextTheme = resolvedTextTheme.merge(textTheme);
            }
        }
        return resolvedTextTheme;
    }
    public TextTheme primaryTextTheme() { return primaryTextTheme; }
    public Color primaryColor() { return primaryColor; }
    public Color scaffoldBackgroundColor() { return scaffoldBackgroundColor; }
    public Color canvasColor() { return canvasColor; }
    public Color cardColor() { return cardColor; }
    public Color dividerColor() { return dividerColor; }
    public Color focusColor() { return focusColor; }
    public Color highlightColor() { return highlightColor; }
    public Color splashColor() { return splashColor; }
    public Color hintColor() { return hintColor; }
    public Color disabledColor() { return disabledColor; }
    public Color shadowColor() { return shadowColor; }
    /**
     * The ambient icon style, never null.
     *
     * <p>Flutter's {@code ThemeData()} fills every slot in, so app code reads
     * {@code Theme.of(context).iconTheme} and calls {@code copyWith} on it
     * without a null check — Shrine and Crane both build their theme as
     * {@code _customIconTheme(base.iconTheme)}, which threw on a bare
     * {@code ThemeData()} and took the whole study down with it.</p>
     */
    public IconThemeData iconTheme() {
        if (iconTheme == null) {
            iconTheme = defaultIconTheme(colorScheme().onSurface());
        }
        return iconTheme;
    }

    /** The icon style for surfaces painted in the primary colour, never null. */
    public IconThemeData primaryIconTheme() {
        if (primaryIconTheme == null) {
            primaryIconTheme = defaultIconTheme(colorScheme().onPrimary());
        }
        return primaryIconTheme;
    }

    /** Material's default icon: 24 logical pixels, fully opaque, in {@code color}. */
    private static IconThemeData defaultIconTheme(Color color) {
        IconThemeData d = new IconThemeData();
        d.size(24);
        d.opacity(1.0);
        if (color != null) {
            d.color(color);
        }
        return d;
    }
    public AppBarTheme appBarTheme() { return appBarTheme; }
    public ChipThemeData chipTheme() { return chipTheme; }
    public CheckboxThemeData checkboxTheme() { return checkboxTheme; }
    public CardTheme cardTheme() { return cardTheme; }
    public BottomAppBarThemeData bottomAppBarTheme() { return bottomAppBarTheme; }
    public DividerThemeData dividerTheme() { return dividerTheme; }
    public NavigationRailThemeData navigationRailTheme() {
        return navigationRailTheme == null ? new NavigationRailThemeData() : navigationRailTheme;
    }
    public Object platform() { return platform; }

    private ThemeData shallowClone() {
        ThemeData c = new ThemeData();
        c.colorScheme = colorScheme;
        c.textTheme = textTheme;
        c.primaryTextTheme = primaryTextTheme;
        c.useMaterial3 = useMaterial3;
        c.brightness = brightness;
        c.primaryColor = primaryColor;
        c.scaffoldBackgroundColor = scaffoldBackgroundColor;
        c.canvasColor = canvasColor;
        c.cardColor = cardColor;
        c.dividerColor = dividerColor;
        c.focusColor = focusColor;
        c.highlightColor = highlightColor;
        c.splashColor = splashColor;
        c.hintColor = hintColor;
        c.disabledColor = disabledColor;
        c.shadowColor = shadowColor;
        c.indicatorColor = indicatorColor;
        c.secondaryHeaderColor = secondaryHeaderColor;
        c.iconTheme = iconTheme;
        c.primaryIconTheme = primaryIconTheme;
        c.appBarTheme = appBarTheme;
        c.chipTheme = chipTheme;
        c.checkboxTheme = checkboxTheme;
        c.cardTheme = cardTheme;
        c.bottomAppBarTheme = bottomAppBarTheme;
        c.dividerTheme = dividerTheme;
        c.navigationRailTheme = navigationRailTheme;
        c.snackBarTheme = snackBarTheme;
        c.inputDecorationTheme = inputDecorationTheme;
        c.radioTheme = radioTheme;
        c.switchTheme = switchTheme;
        c.tooltipTheme = tooltipTheme;
        c.bottomSheetTheme = bottomSheetTheme;
        c.floatingActionButtonTheme = floatingActionButtonTheme;
        c.elevatedButtonTheme = elevatedButtonTheme;
        c.textButtonTheme = textButtonTheme;
        c.outlinedButtonTheme = outlinedButtonTheme;
        c.pageTransitionsTheme = pageTransitionsTheme;
        c.visualDensity = visualDensity;
        c.typography = typography;
        c.platform = platform;
        c.applyElevationOverlayColor = applyElevationOverlayColor;
        c.fontFamily = fontFamily;
        return c;
    }

    /**
     * Returns a copy with the supplied (non-null) values overridden. Parameter
     * order matches the Dart stub.
     */
    public ThemeData copyWith(ColorScheme colorScheme, TextTheme textTheme, TextTheme primaryTextTheme,
                              Brightness brightness, Color primaryColor, Color scaffoldBackgroundColor,
                              Color canvasColor, Color cardColor, Color dividerColor, Color focusColor,
                              Color highlightColor, Color splashColor, Color hintColor, Color disabledColor,
                              IconThemeData iconTheme, AppBarTheme appBarTheme, ChipThemeData chipTheme,
                              CardTheme cardTheme, DividerThemeData dividerTheme, Object platform,
                              NavigationRailThemeData navigationRailTheme,
                              Boolean applyElevationOverlayColor,
                              BottomAppBarThemeData bottomAppBarTheme, BottomSheetThemeData bottomSheetTheme,
                              Object inputDecorationTheme, IconThemeData primaryIconTheme,
                              PageTransitionsTheme pageTransitionsTheme, Color indicatorColor,
                              TextSelectionThemeData textSelectionTheme, Object tabBarTheme,
                              Object snackBarTheme, Object tooltipTheme) {
        ThemeData c = shallowClone();
        if (colorScheme != null) c.colorScheme = colorScheme;
        if (textTheme != null) c.textTheme = textTheme;
        if (primaryTextTheme != null) c.primaryTextTheme = primaryTextTheme;
        if (brightness != null) c.brightness = brightness;
        if (primaryColor != null) c.primaryColor = primaryColor;
        if (scaffoldBackgroundColor != null) c.scaffoldBackgroundColor = scaffoldBackgroundColor;
        if (canvasColor != null) c.canvasColor = canvasColor;
        if (cardColor != null) c.cardColor = cardColor;
        if (dividerColor != null) c.dividerColor = dividerColor;
        if (focusColor != null) c.focusColor = focusColor;
        if (highlightColor != null) c.highlightColor = highlightColor;
        if (splashColor != null) c.splashColor = splashColor;
        if (hintColor != null) c.hintColor = hintColor;
        if (disabledColor != null) c.disabledColor = disabledColor;
        if (iconTheme != null) c.iconTheme = iconTheme;
        if (appBarTheme != null) c.appBarTheme = appBarTheme;
        if (chipTheme != null) c.chipTheme = chipTheme;
        if (cardTheme != null) c.cardTheme = cardTheme;
        if (dividerTheme != null) c.dividerTheme = dividerTheme;
        if (navigationRailTheme != null) c.navigationRailTheme = navigationRailTheme;
        if (platform != null) c.platform = platform;
        if (applyElevationOverlayColor != null) c.applyElevationOverlayColor = applyElevationOverlayColor;
        // The sub-themes a study overrides when it re-skins the app. copyWith had no
        // parameters for these, so a study's own bottom bar, sheet, input and tab styling
        // was dropped on the floor and it rendered with the base Material theme.
        if (bottomAppBarTheme != null) c.bottomAppBarTheme(bottomAppBarTheme);
        if (bottomSheetTheme != null) c.bottomSheetTheme(bottomSheetTheme);
        if (inputDecorationTheme != null) c.inputDecorationTheme(inputDecorationTheme);
        if (primaryIconTheme != null) c.primaryIconTheme(primaryIconTheme);
        if (pageTransitionsTheme != null) c.pageTransitionsTheme(pageTransitionsTheme);
        if (indicatorColor != null) c.indicatorColor(indicatorColor);
        if (textSelectionTheme != null) c.textSelectionTheme(textSelectionTheme);
        if (snackBarTheme != null) c.snackBarTheme(snackBarTheme);
        if (tooltipTheme != null) c.tooltipTheme(tooltipTheme);
        return c;
    }
}

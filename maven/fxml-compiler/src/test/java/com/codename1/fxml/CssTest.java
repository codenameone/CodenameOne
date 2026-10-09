/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.fxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.StyleEngine;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.fxcompat.runtime.css.CssSheetData;
import com.codename1.fxcompat.runtime.css.CssValue;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/// Style sheets from their text to the nodes they style: the build's
/// compiler writes the binary table, and the engine of the layer reads it
/// back from the application's resources and runs the cascade.
public class CssTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private FxmlHarness app;
    private VBox root;
    private HBox bar;
    private Label title;
    private Button ok;
    private Label plain;
    private Scene scene;

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
        StyleEngine.setInstance(null);
    }

    @After
    public void tearDown() throws IOException {
        if (app != null) {
            app.close();
        }
        Units.setScale(0);
        StyleEngine.setInstance(null);
    }

    /// Builds an application whose only resource is a style sheet, and a
    /// scene that uses it:
    ///
    /// ```
    /// VBox.root
    ///   HBox.bar
    ///     Label#title.title
    ///     Button.primary
    ///   Label
    /// ```
    private void styled(String name, String css) throws Exception {
        app = new FxmlHarness(name).resource("style/app.css", css);
        app.build();
        tree();
        scene.getStylesheets().add("file:/work/app/target/classes/style/app.css");
    }

    private void tree() {
        root = new VBox();
        bar = new HBox();
        bar.getStyleClass().add("bar");
        title = new Label("title");
        title.setId("title");
        title.getStyleClass().add("title");
        ok = new Button("ok");
        ok.getStyleClass().add("primary");
        plain = new Label("plain");
        bar.getChildren().addAll(title, ok);
        root.getChildren().addAll(bar, plain);
        scene = new Scene(root, 200, 200);
    }

    private static Paint fill(Region r) {
        return r.getBackground() == null ? null : r.getBackground().getFills().get(0).getFill();
    }

    // ---------------------------------------------------------- matching

    @Test
    public void selectorsMatchByTypeClassIdAndPosition() throws Exception {
        styled("match", "Label { -fx-text-fill: blue; }\n"
                + ".bar { -fx-spacing: 7; }\n"
                + "#title { -fx-underline: true; }\n"
                + ".bar > .primary { -fx-text-fill: red; }\n"
                + ".root Button { -fx-wrap-text: true; }\n"
                + ".root > Button { -fx-underline: true; }\n"
                + "VBox > Label { -fx-wrap-text: true; }\n"
                + "Label.title#title { -fx-opacity: 0.5; }\n"
                + "* { -fx-cursor: hand; }\n"
                + ".nothing .bar, .root .bar { -fx-alignment: center-right; }\n");
        assertEquals(Color.BLUE, title.getTextFill());
        assertEquals(Color.BLUE, plain.getTextFill());
        assertEquals(7, bar.getSpacing(), 0);
        assertTrue(title.isUnderline());
        assertFalse(plain.isUnderline());
        assertEquals(Color.RED, ok.getTextFill());
        // A descendant at any depth matches the descendant selector, only a
        // direct child matches '>'.
        assertTrue(ok.isWrapText());
        assertFalse(ok.isUnderline());
        assertTrue(plain.isWrapText());
        assertFalse(title.isWrapText());
        assertEquals(0.5, title.getOpacity(), 0);
        assertEquals(1, plain.getOpacity(), 0);
        assertEquals(Cursor.HAND, plain.getCursor());
        assertEquals(Cursor.HAND, root.getCursor());
        assertEquals(Pos.CENTER_RIGHT, bar.getAlignment());
    }

    @Test
    public void theCascadeOrdersBySpecificityThenSourceOrderThenImportance() throws Exception {
        styled("cascade", "#title { -fx-text-fill: green; }\n"
                + ".title { -fx-text-fill: red; -fx-opacity: 0.2; }\n"
                + "Label { -fx-text-fill: blue; -fx-opacity: 0.9 !important; -fx-underline: true; }\n"
                + "Label { -fx-underline: false; }\n"
                + ".bar .title { -fx-wrap-text: true; }\n"
                + ".title { -fx-wrap-text: false; }\n");
        // The id beats the class beats the type, whatever the order.
        assertEquals(Color.GREEN, title.getTextFill());
        assertEquals(Color.BLUE, plain.getTextFill());
        // !important beats a more specific rule.
        assertEquals(0.9, title.getOpacity(), 0);
        // Of two rules of one specificity the later wins.
        assertFalse(plain.isUnderline());
        // Two classes beat one that comes later.
        assertTrue(title.isWrapText());
    }

    @Test
    public void inlineStyleBeatsTheSheetsAndIsWithdrawn() throws Exception {
        styled("inline", ".title { -fx-text-fill: red; -fx-opacity: 0.5 !important; }\n");
        title.setStyle("-fx-text-fill: #00ff00; -fx-opacity: 0.25; -fx-underline: true");
        assertEquals(Color.LIME, title.getTextFill());
        assertTrue(title.isUnderline());
        // An important declaration of a sheet beats the inline style.
        assertEquals(0.5, title.getOpacity(), 0);
        title.setStyle(null);
        assertEquals(Color.RED, title.getTextFill());
        assertFalse(title.isUnderline());
    }

    @Test
    public void pseudoClassesRestyleAndRestore() throws Exception {
        styled("pseudo", ".primary { -fx-text-fill: blue; }\n"
                + ".primary:hover { -fx-text-fill: red; -fx-underline: true; }\n"
                + ".primary:hover:armed { -fx-text-fill: green; }\n"
                + ".bar:selected > Label { -fx-opacity: 0.5; }\n"
                + ".bar:selected .primary { -fx-wrap-text: true; }\n");
        PseudoClass hover = PseudoClass.getPseudoClass("hover");
        PseudoClass armed = PseudoClass.getPseudoClass("armed");
        PseudoClass selected = PseudoClass.getPseudoClass("selected");
        assertEquals(Color.BLUE, ok.getTextFill());
        ok.pseudoClassStateChanged(hover, true);
        assertEquals(Color.RED, ok.getTextFill());
        assertTrue(ok.isUnderline());
        ok.pseudoClassStateChanged(armed, true);
        assertEquals(Color.GREEN, ok.getTextFill());
        ok.pseudoClassStateChanged(armed, false);
        assertEquals(Color.RED, ok.getTextFill());
        ok.pseudoClassStateChanged(hover, false);
        assertEquals(Color.BLUE, ok.getTextFill());
        // What only the state set is gone, back to what the node had.
        assertFalse(ok.isUnderline());
        // A state of an ancestor restyles the descendants it selects.
        bar.pseudoClassStateChanged(selected, true);
        assertEquals(0.5, title.getOpacity(), 0);
        assertTrue(ok.isWrapText());
        assertEquals(1, plain.getOpacity(), 0);
        bar.pseudoClassStateChanged(selected, false);
        assertEquals(1, title.getOpacity(), 0);
        assertFalse(ok.isWrapText());
    }

    @Test
    public void aValueTheApplicationSetIsRestoredWhenTheRuleStopsMatching() throws Exception {
        styled("restore", ".loud { -fx-text-fill: red; -fx-padding: 9; }\n");
        plain.setTextFill(Color.NAVY);
        plain.setPadding(new Insets(1));
        plain.getStyleClass().add("loud");
        assertEquals(Color.RED, plain.getTextFill());
        assertEquals(new Insets(9), plain.getPadding());
        plain.getStyleClass().remove("loud");
        assertEquals(Color.NAVY, plain.getTextFill());
        assertEquals(new Insets(1), plain.getPadding());
        // Removing the sheet does the same for every node.
        plain.getStyleClass().add("loud");
        assertEquals(Color.RED, plain.getTextFill());
        scene.getStylesheets().clear();
        assertEquals(Color.NAVY, plain.getTextFill());
    }

    @Test
    public void nodesAddedLaterAndParentSheetsAreStyled() throws Exception {
        app = new FxmlHarness("parent").resource("a.css", "Label { -fx-text-fill: red; -fx-underline: true; }\n")
                .resource("b.css", "Label { -fx-text-fill: blue; }\n");
        app.build();
        tree();
        scene.getStylesheets().add("cn1res:/a.css");
        assertEquals(Color.RED, title.getTextFill());
        // A parent's sheet styles its subtree only, and after the scene's.
        bar.getStylesheets().add("cn1res:/b.css");
        assertEquals(Color.BLUE, title.getTextFill());
        assertTrue(title.isUnderline());
        assertEquals(Color.RED, plain.getTextFill());
        Label late = new Label("late");
        bar.getChildren().add(late);
        assertEquals(Color.BLUE, late.getTextFill());
        root.getChildren().add(late);
        assertEquals(Color.RED, late.getTextFill());
    }

    // ------------------------------------------------------------- values

    @Test
    public void lookedUpColoursResolveThroughTheAncestors() throws Exception {
        styled("lookup", ".root { -brand: #336699; -accent: -brand; -soft: derive(-brand, 100%); -fx-base: #101010; }\n"
                + ".bar { -brand: red; }\n"
                + "Label { -fx-text-fill: -accent; }\n"
                + "Button { -fx-text-fill: -brand; -fx-background-color: -soft; }\n"
                + "#title { -fx-background-color: linear-gradient(to right, -brand, white); }\n");
        // -accent is resolved where it is used: under .bar, -brand is red.
        assertEquals(Color.RED, title.getTextFill());
        assertEquals(Color.web("#336699"), plain.getTextFill());
        assertEquals(Color.RED, ok.getTextFill());
        // derive() is the desktop's: a dark colour is lightened by less than was asked.
        assertEquals(Color.web("#ff9999"), fill(ok));
        LinearGradient g = (LinearGradient) fill(title);
        assertEquals(Color.RED, g.getStops().get(0).getColor());
        assertEquals(Color.WHITE, g.getStops().get(1).getColor());
        // Redefining the colour restyles what looks it up.
        bar.setStyle("-brand: lime");
        assertEquals(Color.LIME, ok.getTextFill());
        assertEquals(Color.LIME, title.getTextFill());
    }

    @Test
    public void fontsInheritAndEmFollowsTheFontSize() throws Exception {
        styled("font", ".root { -fx-font-size: 20px; }\n"
                + ".bar { -fx-font-weight: bold; -fx-font-style: italic; }\n"
                + "#title { -fx-padding: 0.5em 1em; -fx-font-family: \"Courier New\"; }\n"
                + ".primary { -fx-font-size: 1.5em; -fx-padding: 1em; }\n"
                + "Label { -fx-pref-width: 2em; }\n");
        assertEquals(20, plain.getFont().getSize(), 0);
        assertEquals(40, plain.getPrefWidth(), 0);
        assertEquals(20, title.getFont().getSize(), 0);
        assertEquals("Courier New", title.getFont().getFamily());
        assertEquals(new Insets(10, 20, 10, 20), title.getPadding());
        assertTrue(title.getFont().getStyle(), title.getFont().getStyle().contains("Bold"));
        assertTrue(title.getFont().getStyle(), title.getFont().getStyle().contains("Italic"));
        assertFalse(plain.getFont().getStyle(), plain.getFont().getStyle().contains("Bold"));
        // An em font size is relative to the inherited size, and the node's
        // other em lengths to the result.
        assertEquals(30, ok.getFont().getSize(), 0);
        assertEquals(new Insets(30), ok.getPadding());
        root.setStyle("-fx-font-size: 10px");
        assertEquals(10, plain.getFont().getSize(), 0);
        assertEquals(15, ok.getFont().getSize(), 0);
        assertEquals(new Insets(15), ok.getPadding());
        assertEquals(new Insets(5, 10, 5, 10), title.getPadding());
    }

    @Test
    public void fontShorthand() throws Exception {
        styled("shorthand", "#title { -fx-font: italic bold 18px \"Helvetica Neue\"; }\n"
                + ".primary { -fx-font: 12pt serif; }\n");
        assertEquals(18, title.getFont().getSize(), 0);
        assertEquals("Helvetica Neue", title.getFont().getFamily());
        assertTrue(title.getFont().getStyle(), title.getFont().getStyle().contains("Bold"));
        assertTrue(title.getFont().getStyle(), title.getFont().getStyle().contains("Italic"));
        assertEquals(16, ok.getFont().getSize(), 0);
        assertNotNull(FontWeight.BOLD);
        assertNotNull(FontPosture.ITALIC);
    }

    @Test
    public void everyKindOfColour() throws Exception {
        String[][] cases = {
            {"#f00", "#ff0000"}, {"#f008", "#ff000088"}, {"#00ff0080", "#00ff0080"}, {"navy", "#000080"},
            {"transparent", "#00000000"}, {"rgb(255, 0, 128)", "#ff0080"}, {"rgb(100%, 0%, 50%)", "#ff0080"},
            {"rgba(0, 0, 255, 0.5)", "#0000ff80"}, {"hsb(120, 100%, 100%)", "#00ff00"},
            {"hsba(0, 100%, 100%, 0.5)", "#ff000080"}, {"hsl(240, 100%, 50%)", "#0000ff"},
            {"derive(#808080, -100%)", "#000000"}, {"derive(black, 100%)", "#999999"},
            {"RED", "#ff0000"},
        };
        StringBuilder css = new StringBuilder();
        for (int i = 0; i < cases.length; i++) {
            css.append(".c").append(i).append(" { -fx-text-fill: ").append(cases[i][0]).append("; }\n");
        }
        styled("colours", css.toString());
        for (int i = 0; i < cases.length; i++) {
            plain.getStyleClass().setAll("c" + i);
            Color want = Color.web(cases[i][1]);
            Color got = (Color) plain.getTextFill();
            assertEquals(cases[i][0], want.getRed(), got.getRed(), 0.005);
            assertEquals(cases[i][0], want.getGreen(), got.getGreen(), 0.005);
            assertEquals(cases[i][0], want.getBlue(), got.getBlue(), 0.005);
            assertEquals(cases[i][0], want.getOpacity(), got.getOpacity(), 0.005);
        }
    }

    @Test
    public void gradientsLengthsInsetsRadiiAndKeywords() throws Exception {
        styled("values", ".bar { -fx-background-color: linear-gradient(to bottom right, red 0%, blue 100%);"
                + " -fx-background-radius: 4 8; -fx-background-insets: 1 2 3 4; -fx-padding: 1px 2pt 1pc 1in;"
                + " -fx-border-color: black; -fx-border-width: 2; -fx-border-radius: 3;"
                + " -fx-alignment: bottom-left; -fx-spacing: 1.5em; -fx-min-width: 10mm; -fx-max-height: 1cm;"
                + " -fx-fill-height: false; }\n"
                + ".primary { -fx-background-color: radial-gradient(center 50% 50%, radius 50%, reflect, white, black 80%);"
                + " -fx-content-display: right; -fx-text-alignment: center; -fx-cursor: crosshair;"
                + " -fx-graphic-text-gap: 6; -fx-rotate: 45; -fx-scale-x: 2; -fx-translate-y: -3;"
                + " -fx-background-color: red, radial-gradient(center 50% 50%, radius 50%, reflect, white, black 80%); }\n");
        LinearGradient lg = (LinearGradient) fill(bar);
        assertTrue(lg.isProportional());
        assertEquals(0, lg.getStartX(), 0);
        assertEquals(1, lg.getEndX(), 0);
        assertEquals(1, lg.getEndY(), 0);
        assertEquals(Color.BLUE, lg.getStops().get(1).getColor());
        assertEquals(4, bar.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius(), 0);
        assertEquals(8, bar.getBackground().getFills().get(0).getRadii().getTopRightHorizontalRadius(), 0);
        assertEquals(new Insets(1, 2, 3, 4), bar.getBackground().getFills().get(0).getInsets());
        assertEquals(1, bar.getPadding().getTop(), 0);
        assertEquals(2 * 96.0 / 72, bar.getPadding().getRight(), 0.001);
        assertEquals(16, bar.getPadding().getBottom(), 0.001);
        assertEquals(96, bar.getPadding().getLeft(), 0.001);
        assertNotNull(bar.getBorder());
        assertEquals(Pos.BOTTOM_LEFT, bar.getAlignment());
        assertEquals(1.5 * bar.getChildren().size() * 0 + 1.5 * javafx.scene.text.Font.getDefault().getSize(),
                bar.getSpacing(), 0.001);
        assertEquals(10 * 96 / 25.4, bar.getMinWidth(), 0.001);
        assertEquals(96 / 2.54, bar.getMaxHeight(), 0.001);
        assertFalse(bar.isFillHeight());
        RadialGradient rg = (RadialGradient) fill(ok);
        assertEquals(CycleMethod.REFLECT, rg.getCycleMethod());
        assertEquals(0.5, rg.getRadius(), 0);
        assertEquals(0.8, rg.getStops().get(1).getOffset(), 0.0001);
        assertEquals(javafx.scene.control.ContentDisplay.RIGHT, ok.getContentDisplay());
        assertEquals(javafx.scene.text.TextAlignment.CENTER, ok.getTextAlignment());
        assertEquals(Cursor.CROSSHAIR, ok.getCursor());
        assertEquals(6, ok.getGraphicTextGap(), 0);
        assertEquals(45, ok.getRotate(), 0);
        assertEquals(2, ok.getScaleX(), 0);
        assertEquals(-3, ok.getTranslateY(), 0);
        // The layered background is a warning: only the last layer is kept.
        assertTrue(app.warnings.toString(), app.warnings.toString().contains("layer"));
    }

    // ---------------------------------------------------- the compiler

    @Test
    public void unknownPropertiesAndBadValuesAreWarningsWithALine() throws Exception {
        app = new FxmlHarness("warn").resource("app.css", "/* a comment\n over two lines */\n"
                + ".a { -fx-text-fill: red; }\n"
                + ".b {\n  -fx-text-fil: red;\n  -fx-opacity: lots;\n  -fx-text-fill: blue;\n}\n"
                + ".c:nth-child(2) { -fx-opacity: 1; }\n"
                + "@media print { .d { -fx-opacity: 0; } }\n");
        List<String> errors = app.compile();
        assertTrue(errors.toString(), errors.isEmpty());
        String file = app.file("app.css");
        String all = app.warnings.toString();
        assertEquals(all, 4, app.warnings.size());
        assertTrue(all, app.warnings.get(0).startsWith(file + ":5:"));
        assertTrue(all, app.warnings.get(0).contains("-fx-text-fil"));
        assertTrue(all, app.warnings.get(1).startsWith(file + ":6:"));
        assertTrue(all, app.warnings.get(1).contains("lots"));
        assertTrue(all, app.warnings.get(2).startsWith(file + ":9:"));
        assertTrue(all, app.warnings.get(2).contains("nth-child"));
        assertTrue(all, app.warnings.get(3).startsWith(file + ":10:"));
        assertTrue(all, app.warnings.get(3).contains("@media"));
        // The sheet is compiled with what was understood.
        CssSheetData data = read(app);
        assertEquals(2, data.ruleCount());
        assertEquals(1, data.declarationCount(data.blockOf(1)));
        assertEquals("-fx-text-fill", data.declaration(data.blockOf(1), 0).name());
    }

    @Test
    public void aSheetBrokenInItsStructureIsAnError() throws Exception {
        app = new FxmlHarness("broken").resource("a.css", ".a { -fx-opacity: 1; }\n.b {\n  -fx-opacity: 0;\n")
                .resource("b.css", ".a { -fx-opacity: 1; }\n}\n")
                .resource("c.css", ".a { -fx-opacity: 1; }\n.dangling\n")
                .resource("ok.css", ".a { -fx-opacity: 1; }\n");
        List<String> errors = app.compile();
        assertEquals(errors.toString(), 3, errors.size());
        assertTrue(errors.toString(), errors.get(0).startsWith(app.file("a.css") + ":2:"));
        assertTrue(errors.toString(), errors.get(0).contains("never closed"));
        assertTrue(errors.toString(), errors.get(1).startsWith(app.file("b.css") + ":2:"));
        assertTrue(errors.toString(), errors.get(2).startsWith(app.file("c.css") + ":2:"));
        assertFalse(app.compiledSheet("a.css").exists());
        assertTrue(app.compiledSheet("ok.css").exists());
    }

    private static CssSheetData read(FxmlHarness app) throws IOException {
        InputStream in = new FileInputStream(app.compiledSheet("app.css"));
        try {
            return CssSheetData.read(in);
        } finally {
            in.close();
        }
    }

    @Test
    public void theBinaryTableRoundTrips() throws Exception {
        app = new FxmlHarness("binary").resource("app.css", "@import \"base.css\";\n"
                + "@font-face { font-family: \"Brand\"; src: url(\"fonts/brand.ttf\"); }\n"
                + ".root { -brand: #123456; }\n"
                + ".a, VBox > #b:hover .c.d { -fx-text-fill: -brand !important; -fx-padding: 1em 2px;"
                + " -fx-background-color: linear-gradient(to right, red, derive(-brand, 20%) 50%, blue);"
                + " -fx-font: bold 12px \"Sans\"; -fx-alignment: center; -fx-wrap-text: true;"
                + " -fx-background-radius: 50%; }\n")
                .resource("base.css", "Label { -fx-opacity: 0.5; }\n")
                .resource("fonts/brand.ttf", "not a font");
        assertTrue(app.compile().isEmpty());
        CssSheetData first = read(app);
        assertEquals(1, first.importCount());
        assertEquals("base.css", first.importPath(0));
        assertEquals(1, first.fontCount());
        assertEquals("Brand", first.fontFamily(0));
        assertEquals("fonts/brand.ttf", first.fontSource(0));
        assertEquals(3, first.ruleCount());
        assertEquals(2, first.blockCount());
        // Two selectors of one rule share its block.
        assertEquals(first.blockOf(1), first.blockOf(2));
        assertEquals(".a", first.selector(1).toString());
        assertEquals("VBox > #b:hover .c.d", first.selector(2).toString());
        assertEquals(7, first.declarationCount(first.blockOf(1)));
        assertTrue(first.declaration(first.blockOf(1), 0).important());
        assertFalse(first.declaration(first.blockOf(1), 1).important());
        // Written again, the table is the same bytes: nothing is lost.
        ByteArrayOutputStream again = new ByteArrayOutputStream();
        first.write(again);
        ByteArrayOutputStream third = new ByteArrayOutputStream();
        CssSheetData.read(new ByteArrayInputStream(again.toByteArray())).write(third);
        assertEquals(again.size(), app.compiledSheet("app.css").length());
        assertTrue(java.util.Arrays.equals(again.toByteArray(), third.toByteArray()));
        CssValue gradient = first.declaration(first.blockOf(1), 2).value();
        assertNotNull(gradient);
    }

    @Test
    public void importsComeBeforeTheSheetThatImportsThem() throws Exception {
        app = new FxmlHarness("import").resource("css/app.css", "@import url(\"base.css\");\n"
                + "Label { -fx-text-fill: red; }\n")
                .resource("css/base.css", "Label { -fx-text-fill: blue; -fx-underline: true; }\n");
        app.build();
        tree();
        scene.getStylesheets().add("cn1res:/css/app.css");
        assertEquals(Color.RED, plain.getTextFill());
        assertTrue(plain.isUnderline());
    }

    @Test
    public void aSheetThatWasNotCompiledStylesNothing() throws Exception {
        app = new FxmlHarness("absent");
        app.build();
        tree();
        scene.getStylesheets().add("cn1res:/none.css");
        scene.getStylesheets().add("cn1res:/none.css");
        assertEquals(Color.BLACK, plain.getTextFill());
    }

    /// JavaFX reads a comment that runs to the end of its line, which CSS
    /// has not; a sheet written for it has them, and the declaration on
    /// the next line was swallowed into the comment's "property name".
    /// The two slashes of an address inside `url()` are no comment.
    @Test
    public void aLineCommentEndsWithItsLineAndAnEffectIsCompiled() throws Exception {
        app = new FxmlHarness("linecomment").resource("app.css", ".t {\n"
                + "    -fx-text-fill: #f2b179; // f9f6f2\n"
                + "    -fx-effect: dropshadow( three-pass-box, rgba(243, 215, 116, 1), 10, 0.5, 0, 0 );\n"
                + "}\n");
        assertTrue(app.compile().isEmpty());
        CssSheetData data = read(app);
        assertEquals(2, data.declarationCount(data.blockOf(0)));
        assertEquals("-fx-effect", data.declaration(data.blockOf(0), 1).name());
        CssValue effect = data.declaration(data.blockOf(0), 1).value();
        assertEquals(CssValue.EFFECT, effect.type());
        assertEquals(CssValue.EFFECT_DROP | (3 << 4), effect.flags());
        assertEquals(10, effect.num(0), 0);
        assertEquals(0.5, effect.num(1), 0);
        assertEquals(CssValue.COLOR, effect.part(0).type());
        assertEquals("a(b://c) d", com.codename1.fxml.css.CssDeclarations.stripComments("a(b://c) d// e").trim());
    }
}

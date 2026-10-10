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
package com.codename1.fxcompat;

import java.util.ArrayList;
import java.util.List;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.FrameClock;
import com.codename1.fxcompat.runtime.PeerPaint;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.Style;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// What the controls show: the progress controls that draw themselves,
/// the graphic of a labeled control, the states of a hyperlink and of a
/// toggle button, the arrow of a choice box, the dimming of a disabled
/// control and a label that gives way in a row that is too narrow.
public class ControlLookTest {

    private static final int ACCENT = 0x0096c9;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Renderer.setTrace(null);
        FrameClock.reset();
        Units.setScale(0);
    }

    /// One drawing operation of a traced paint.
    private static final class Op {
        String kind;
        double[] bounds;
        Paint paint;
        String text;

        double width() {
            return bounds[2] - bounds[0];
        }

        boolean accent() {
            return paint instanceof Color && (((Color) paint).cn1Argb() & 0xffffff) == ACCENT;
        }
    }

    private static List<Op> paint(Node node) {
        final List<Op> ops = new ArrayList<Op>();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                Op op = new Op();
                op.kind = operation;
                op.bounds = deviceBounds;
                op.paint = paint;
                op.text = text;
                ops.add(op);
            }
        });
        node.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(400, 400, 0).getGraphics(), 0, 0));
        Renderer.setTrace(null);
        return ops;
    }

    private static Op accentFill(List<Op> ops) {
        Op found = null;
        for (Op op : ops) {
            if ("fill".equals(op.kind) && op.accent()) {
                assertNull("one accent fill", found);
                found = op;
            }
        }
        return found;
    }

    private static List<String> texts(List<Op> ops) {
        List<String> out = new ArrayList<String>();
        for (Op op : ops) {
            if ("text".equals(op.kind)) {
                out.add(op.text);
            }
        }
        return out;
    }

    private static com.codename1.ui.Label nativeLabel(Control control) {
        Component c = control.cn1Native();
        assertTrue(c instanceof com.codename1.ui.Label);
        return (com.codename1.ui.Label) c;
    }

    // ------------------------------------------------------------ progress

    @Test
    public void aProgressBarIsAHundredByEighteenUnlessAskedOtherwise() {
        ProgressBar bar = new ProgressBar();
        assertEquals(100, bar.prefWidth(-1), 0.01);
        assertEquals(18, bar.prefHeight(-1), 0.01);
        ProgressBar wide = new ProgressBar(0.5);
        wide.setPrefWidth(150);
        VBox box = new VBox(bar, wide);
        box.resize(600, 200);
        box.layout();
        assertEquals(100, bar.getWidth(), 0.01);
        assertEquals(150, wide.getWidth(), 0.01);
        assertEquals(18, wide.getHeight(), 0.01);
    }

    private static javafx.scene.layout.Region part(ProgressBar bar, String name) {
        Node found = bar.lookup("." + name);
        assertTrue("no ." + name, found instanceof javafx.scene.layout.Region);
        return (javafx.scene.layout.Region) found;
    }

    /// The bar is two regions, as in JavaFX, so that a style sheet's
    /// `.track` and `.bar` rules have something to style.
    @Test
    public void aProgressBarFillsItsTrackInProportion() {
        ProgressBar bar = new ProgressBar(0.25);
        bar.resize(100, 18);
        bar.layout();
        assertEquals("the track spans the bar", 100, part(bar, "track").getWidth(), 0.01);
        assertEquals(25, part(bar, "bar").getWidth(), 0.01);
        assertEquals(0, part(bar, "bar").getLayoutX(), 0.01);
        assertEquals(18, part(bar, "bar").getHeight(), 0.01);

        bar.setProgress(1);
        assertEquals(100, part(bar, "bar").getWidth(), 0.01);
        bar.setProgress(0);
        assertFalse("nothing is done yet", part(bar, "bar").isVisible());
    }

    @Test
    public void aStyleSheetColoursTheTwoRegionsOfABar() {
        ProgressBar bar = new ProgressBar(0.5);
        bar.getStyleClass().add("time");
        StackPane root = new StackPane(bar);
        Scene scene = new Scene(root, 300, 100);
        root.setStyle("-fx-accent: #ff0000;");
        scene.cn1Layout(300, 100);
        javafx.scene.layout.Background fill = part(bar, "bar").getBackground();
        assertNotNull(fill);
        assertEquals(Color.web("#ff0000"), fill.getFills().get(0).getFill());
    }

    @Test
    public void anIndeterminateProgressBarRunsASegmentAlongTheTrack() {
        FrameClock.setManual(true);
        ProgressBar bar = new ProgressBar();
        Scene scene = new Scene(new StackPane(bar), 300, 100);
        assertNotNull(scene);
        assertTrue("an indeterminate bar in a scene takes frames", FrameClock.isActive());
        bar.resize(100, 18);
        bar.layout();
        double width = part(bar, "bar").getWidth();
        double at = part(bar, "bar").getLayoutX();
        assertTrue("a segment, not the whole track", width > 10 && width < 60);
        FrameClock.advance(300);
        bar.layout();
        assertEquals(width, part(bar, "bar").getWidth(), 0.01);
        assertTrue("the segment moved", part(bar, "bar").getLayoutX() > at + 5);
        assertTrue(part(bar, "bar").getLayoutX() + width <= 100.01);

        bar.setProgress(0.5);
        assertFalse("a determinate bar takes no frames", FrameClock.isActive());
    }

    @Test
    public void aDeterminateIndicatorShowsItsPercentageAndDone() {
        ProgressIndicator quarter = new ProgressIndicator(0.25);
        assertNull("the indicator draws itself", quarter.cn1Native());
        // A disc of 32 with a line of text below it, however wide and
        // tall the font of the platform makes that line.
        double line = com.codename1.fxcompat.runtime.Fonts.lineHeight(javafx.scene.text.Font.getDefault());
        double text = com.codename1.fxcompat.runtime.Fonts.width(javafx.scene.text.Font.getDefault(), "100%");
        assertEquals(Math.max(32, text), quarter.prefWidth(-1), 1.01);
        assertEquals(32 + 2 + line, quarter.prefHeight(-1), 1.01);
        quarter.resize(50, 50);
        quarter.layout();
        Node shown = quarter.lookup(".percentage");
        assertTrue(shown instanceof javafx.scene.text.Text);
        assertEquals("25%", ((javafx.scene.text.Text) shown).getText());
        assertNotNull("the part that is done", accentFill(paint(quarter)));

        quarter.setProgress(1);
        assertEquals("Done", ((javafx.scene.text.Text) shown).getText());
    }

    /// `-fx-progress-color` is the standard theme's name for the colour
    /// of what is done, and a rule that defines it recolours the disc.
    @Test
    public void aRuleDefiningTheProgressColourRecoloursTheIndicator() {
        ProgressIndicator done = new ProgressIndicator(1);
        StackPane root = new StackPane(done);
        Scene scene = new Scene(root, 300, 100);
        done.setStyle("-fx-progress-color: red;");
        scene.cn1Layout(300, 100);
        List<Op> ops = paint(done);
        assertEquals(Color.RED, ops.get(0).paint);
    }

    @Test
    public void anIndeterminateIndicatorIsARingOfDotsThatTurns() {
        FrameClock.setManual(true);
        ProgressIndicator spinner = new ProgressIndicator();
        Scene scene = new Scene(new StackPane(spinner), 300, 100);
        assertNotNull(scene);
        spinner.resize(50, 50);
        List<Op> ops = paint(spinner);
        assertEquals(12, ops.size());
        assertTrue(texts(ops).isEmpty());
        double firstBefore = ((Color) ops.get(0).paint).getOpacity();
        FrameClock.advance(300);
        double firstAfter = ((Color) paint(spinner).get(0).paint).getOpacity();
        assertTrue("the bright dot moved on", firstAfter < firstBefore);
    }

    /// A spinner on a tab that is not selected is in the scene and
    /// hidden. It used to take a frame sixty times a second all the same,
    /// which kept an application that showed nothing moving from idling.
    @Test
    public void anIndicatorThatIsHiddenTakesNoFrames() {
        FrameClock.setManual(true);
        ProgressIndicator spinner = new ProgressIndicator();
        StackPane tab = new StackPane(spinner);
        Scene scene = new Scene(new StackPane(tab), 300, 100);
        assertNotNull(scene);
        spinner.resize(50, 50);
        assertTrue(FrameClock.isActive());
        FrameClock.advance(16);
        assertTrue("visible, it goes on", FrameClock.isActive());
        tab.setVisible(false);
        FrameClock.advance(16);
        assertFalse("hidden, it leaves the clock", FrameClock.isActive());
        tab.setVisible(true);
        paint(spinner);
        assertTrue("painted again, it takes frames again", FrameClock.isActive());
    }

    // ------------------------------------------------------------- graphic

    @Test
    public void aGraphicIsShownBesideTheTextAndCounted() {
        Button button = new Button("Go");
        double textOnly = button.prefWidth(-1);
        double height = button.prefHeight(-1);
        Rectangle mark = new Rectangle(30, 10);
        button.setGraphic(mark);
        assertSame("the graphic is a node of the control", button, mark.getParent());
        assertEquals(textOnly + 30 + button.getGraphicTextGap(), button.prefWidth(-1), 1.01);
        assertEquals(height, button.prefHeight(-1), 1.01);

        double textWidth = Units.toLogical(
                nativeLabel(button).getUnselectedStyle().getFont().stringWidth("Go"));
        button.resize(button.prefWidth(-1), button.prefHeight(-1));
        button.layout();
        double left = mark.getBoundsInParent().getMinX();
        assertTrue("inside the button", left >= 0 && mark.getBoundsInParent().getMaxX() <= button.getWidth());

        button.setContentDisplay(ContentDisplay.RIGHT);
        button.layout();
        assertEquals("the graphic follows the text", left + textWidth + button.getGraphicTextGap(),
                mark.getBoundsInParent().getMinX(), 1.01);

        button.setGraphicTextGap(20);
        assertEquals(textOnly + 30 + 20, button.prefWidth(-1), 1.01);
        button.setGraphicTextGap(4);

        button.setContentDisplay(ContentDisplay.TOP);
        assertEquals(height + 10 + 4, button.prefHeight(-1), 1.01);
        assertEquals("as wide as the wider of the two", textOnly + Math.max(0, 30 - textWidth),
                button.prefWidth(-1), 1.01);
        button.resize(button.prefWidth(-1), button.prefHeight(-1));
        button.layout();
        assertTrue("above the text", mark.getBoundsInParent().getMaxY() <= button.getHeight() / 2 + 1);

        button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        assertNull(mark.getParent());
        assertEquals(textOnly, button.prefWidth(-1), 0.01);
        assertEquals(height, button.prefHeight(-1), 0.01);

        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        assertSame(button, mark.getParent());
        assertEquals("", nativeLabel(button).getText());
        assertEquals(textOnly - textWidth + 30, button.prefWidth(-1), 1.01);

        button.setGraphic(null);
        assertNull(mark.getParent());
    }

    @Test
    public void theConstructorsWithAGraphicShowIt() {
        Rectangle a = new Rectangle(16, 16);
        Label label = new Label("Name", a);
        assertSame(label, a.getParent());
        assertTrue(label.prefWidth(-1) >= new Label("Name").prefWidth(-1) + 16);

        Rectangle tall = new Rectangle(48, 48);
        Hyperlink link = new Hyperlink("Hyperlink with Image", tall);
        assertSame(link, tall.getParent());
        assertTrue("as tall as its graphic", link.prefHeight(-1) >= 48);
        link.resize(link.prefWidth(-1), link.prefHeight(-1));
        link.layout();
        assertTrue(tall.getBoundsInParent().getMinY() >= 0);
        assertTrue(tall.getBoundsInParent().getMaxY() <= link.getHeight() + 0.01);

        Rectangle c = new Rectangle(8, 8);
        ToggleButton toggle = new ToggleButton("On", c);
        assertSame(toggle, c.getParent());
    }

    // ----------------------------------------------------------- hyperlink

    @Test
    public void aHyperlinkIsAccentColouredAndUnderlinedOnlyWhileArmed() {
        Hyperlink link = new Hyperlink("site");
        com.codename1.ui.Label n = nativeLabel(link);
        assertEquals(ACCENT, n.getUnselectedStyle().getFgColor() & 0xffffff);
        assertEquals(Style.TEXT_DECORATION_NONE, n.getUnselectedStyle().getTextDecoration());
        link.arm();
        assertEquals(Style.TEXT_DECORATION_UNDERLINE, n.getUnselectedStyle().getTextDecoration());
        link.disarm();
        assertEquals(Style.TEXT_DECORATION_NONE, n.getUnselectedStyle().getTextDecoration());

        link.fire();
        assertTrue(link.isVisited());
        assertTrue("a visited link looks visited", (n.getUnselectedStyle().getFgColor() & 0xffffff) != ACCENT);
        link.setVisited(false);
        assertEquals(ACCENT, n.getUnselectedStyle().getFgColor() & 0xffffff);

        link.setTextFill(Color.RED);
        assertEquals(0xff0000, n.getUnselectedStyle().getFgColor() & 0xffffff);
        link.setUnderline(true);
        assertEquals(Style.TEXT_DECORATION_UNDERLINE, n.getUnselectedStyle().getTextDecoration());
    }

    // ------------------------------------------------------------ disabled

    @Test
    public void aDisabledControlIsDimmedOnce() {
        Button button = new Button("Go");
        assertEquals(1, PeerPaint.opacity(button), 0);
        button.setDisable(true);
        assertEquals(0.4, PeerPaint.opacity(button), 0.001);
        button.setOpacity(0.5);
        assertEquals(0.2, PeerPaint.opacity(button), 0.001);
        button.setOpacity(1);

        // The label is in a disabled button, which dims it already.
        Label inside = new Label("in");
        button.setGraphic(inside);
        assertTrue(inside.isDisabled());
        assertEquals(1, PeerPaint.opacity(inside), 0);

        // A pane is not dimmed; each control in a disabled pane is.
        CheckBox check = new CheckBox("c");
        HBox pane = new HBox(check);
        pane.setDisable(true);
        assertEquals(1, PeerPaint.opacity(pane), 0);
        assertEquals(0.4, PeerPaint.opacity(check), 0.001);
        pane.setDisable(false);
        assertEquals(1, PeerPaint.opacity(check), 0);
    }

    // -------------------------------------------------- choice and toggles

    @Test
    public void aChoiceBoxShowsADropDownArrowAfterItsText() {
        ChoiceBox<String> box = new ChoiceBox<String>();
        box.getItems().addAll("Dog", "Cat");
        box.setValue("Dog");
        com.codename1.ui.Label n = nativeLabel(box);
        assertEquals(com.codename1.ui.FontImage.MATERIAL_ARROW_DROP_DOWN, n.getMaterialIcon());
        assertEquals("the text is left of the arrow", Component.LEFT, n.getTextPosition());
    }

    @Test
    public void aToggleButtonHasTheChromeOfAButtonAndCentredText() {
        ToggleButton toggle = new ToggleButton("Cat");
        com.codename1.ui.Label n = nativeLabel(toggle);
        assertEquals("a theme with no toggle look gets the button look", "Button", n.getUIID());
        assertEquals(Component.CENTER, n.getAlignment());
        assertTrue(n instanceof com.codename1.ui.Button);
        com.codename1.ui.Button b = (com.codename1.ui.Button) n;
        assertTrue(b.isToggle());
        assertFalse(b.isSelected());
        toggle.setSelected(true);
        assertTrue("drawn pressed while selected", b.isSelected());

        assertEquals(Component.CENTER, nativeLabel(new Button("Go")).getAlignment());
        assertEquals(Component.LEFT, nativeLabel(new RadioButton("r")).getAlignment());
        assertEquals(Component.LEFT, nativeLabel(new Label("l")).getAlignment());
    }

    // ------------------------------------------------------------- overrun

    @Test
    public void aLabelGivesWayInARowThatIsTooNarrow() {
        // The theme's own default is no ellipsis, and it is what a label
        // gets once a theme was loaded: the layer asks for one itself.
        com.codename1.ui.plaf.LookAndFeel laf = com.codename1.ui.plaf.UIManager.getInstance().getLookAndFeel();
        boolean before = laf.isDefaultEndsWith3Points();
        laf.setDefaultEndsWith3Points(false);
        Label label;
        try {
            label = new Label("A long label that cannot be shown in full here");
            nativeLabel(label);
        } finally {
            laf.setDefaultEndsWith3Points(before);
        }
        Button ok = new Button("OK");
        double pref = label.prefWidth(-1);
        double min = label.minWidth(-1);
        assertTrue("min " + min + " pref " + pref, min < pref / 2);
        assertTrue(min > 0);
        assertTrue(nativeLabel(label).isEndsWith3Points());

        HBox row = new HBox(label, ok);
        double width = ok.prefWidth(-1) + pref / 2;
        row.resize(width, 40);
        row.layout();
        assertTrue("the label shrank: " + label.getWidth(), label.getWidth() < pref);
        assertTrue("the button after it is still in view",
                ok.getBoundsInParent().getMaxX() <= width + 0.01);
        assertEquals(ok.prefWidth(-1), ok.getWidth(), 0.01);
    }
}

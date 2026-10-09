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
package javafx.scene.text;

import java.util.ArrayList;
import java.util.List;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FontStyles;
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/// A pane that sets its children out as words on a line: one after the
/// other on a shared baseline, onto a new line when the width runs out
/// or a [Text] ends in a line feed.
///
/// Each child is one piece that is never split. JavaFX breaks a long
/// `Text` between its own words where the line ends; here a `Text` moves
/// to the next line whole, so rich text made of short runs - a word or a
/// phrase in each `Text`, which is what gives the runs their different
/// styles - is laid out the same, and a single long paragraph in one
/// `Text` is not wrapped. A `Text` with line feeds inside it keeps them
/// and is placed as the block it is. The `x`, `y` and wrapping width of
/// a child `Text` are not used, as in JavaFX.
///
/// `JUSTIFY` is laid out as `LEFT`.
///
/// #### Text of two directions
///
/// A line that holds right-to-left characters is ordered as the Unicode
/// bidirectional algorithm orders it at the level of runs: the texts of
/// the line are cut into runs of one direction each, a space or a mark
/// between two runs of a direction joins them, and a row of runs that go
/// against the direction of the line is seen in reverse. So two Arabic
/// words in two `Text` nodes of different colours are seen right to left
/// inside a line that reads left to right. The direction of a line is
/// that of its first letter. Which characters are right to left is what
/// `Display.isRTL` says; the letters inside a run are ordered by the
/// platform that draws the run. Numbers are taken as left-to-right
/// letters, and explicit embedding marks are not read.
public class TextFlow extends Pane {

    private final ObjectProperty<TextAlignment> textAlignment = new FxObject<TextAlignment>(this, "textAlignment",
            TextAlignment.LEFT, Dirty.LAYOUT);
    private final DoubleProperty lineSpacing = new FxDouble(this, "lineSpacing", 0, Dirty.LAYOUT);

    /// Creates an empty flow.
    public TextFlow() {
    }

    /// Creates a flow of some children.
    public TextFlow(Node... children) {
        cn1Children().addAll(children);
    }

    /// Returns how the lines sit in the width of the flow.
    public final TextAlignment getTextAlignment() {
        TextAlignment a = textAlignment.get();
        return a == null ? TextAlignment.LEFT : a;
    }

    /// Sets how the lines sit in the width of the flow.
    public final void setTextAlignment(TextAlignment value) {
        textAlignment.set(value);
    }

    /// How the lines sit in the width of the flow.
    public final ObjectProperty<TextAlignment> textAlignmentProperty() {
        return textAlignment;
    }

    /// Returns the space added between two lines.
    public final double getLineSpacing() {
        return lineSpacing.get();
    }

    /// Sets the space added between two lines.
    public final void setLineSpacing(double value) {
        lineSpacing.set(value);
    }

    /// The space added between two lines.
    public final DoubleProperty lineSpacingProperty() {
        return lineSpacing;
    }

    /// The width decides the height, as it decides where the lines end.
    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    private static final class Line {
        final List<Node> nodes = new ArrayList<Node>();
        final List<double[]> boxes = new ArrayList<double[]>();
        double top;
        double width;
        double above;
        double below;
        double empty;

        double height() {
            return nodes.isEmpty() ? empty : above + below;
        }
    }

    private static int trailingFeeds(String text) {
        int n = 0;
        for (int i = text.length() - 1; i >= 0 && text.charAt(i) == '\n'; i--) {
            n++;
        }
        return n;
    }

    /// The lines the children fall into for a width; a width of zero or
    /// less never wraps.
    private List<Line> lines(double limit) {
        List<Node> managed = getManagedChildren();
        List<Line> lines = new ArrayList<Line>();
        Line line = new Line();
        double lastLineHeight = 0;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            double w;
            double h;
            int feeds = 0;
            if (child.isResizable()) {
                w = child.prefWidth(-1);
                h = child.prefHeight(w);
            } else {
                Bounds b = child.getLayoutBounds();
                w = b.getWidth();
                h = b.getHeight();
            }
            double baseline = child.getBaselineOffset();
            if (child instanceof Text) {
                Text t = (Text) child;
                double lineHeight = Fonts.lineHeight(t.getFont());
                lastLineHeight = lineHeight;
                feeds = trailingFeeds(t.getText());
                if (feeds > 0) {
                    // The empty lines after the feeds belong to the flow,
                    // not to the height of this piece.
                    h = Math.max(lineHeight, h - feeds * (lineHeight + t.getLineSpacing()));
                }
            }
            // A child that has no baseline of its own, or was not given
            // its size yet, stands on the line.
            if (baseline == BASELINE_OFFSET_SAME_AS_HEIGHT || baseline > h
                    || (baseline <= 0 && child.isResizable())) {
                baseline = h;
            }
            if (limit > 0 && !line.nodes.isEmpty() && line.width + w > limit) {
                lines.add(line);
                line = new Line();
            }
            line.nodes.add(child);
            line.boxes.add(new double[] {line.width, w, h, baseline});
            line.width += w;
            line.above = Math.max(line.above, baseline);
            line.below = Math.max(line.below, h - baseline);
            for (int f = 0; f < feeds; f++) {
                lines.add(line);
                line = new Line();
                line.empty = lastLineHeight;
            }
        }
        if (!line.nodes.isEmpty() || !lines.isEmpty()) {
            lines.add(line);
        }
        double top = 0;
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            l.top = top;
            top += l.height() + getLineSpacing();
        }
        return lines;
    }

    private static double widest(List<Line> lines) {
        double max = 0;
        for (int i = 0; i < lines.size(); i++) {
            max = Math.max(max, lines.get(i).width);
        }
        return max;
    }

    private static double tall(List<Line> lines) {
        if (lines.isEmpty()) {
            return 0;
        }
        Line last = lines.get(lines.size() - 1);
        return last.top + last.height();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + snapSizeX(widest(lines(0))) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        double limit = width == -1 ? 0 : Math.max(1, width - in.getLeft() - in.getRight());
        return in.getTop() + snapSizeY(tall(lines(limit))) + in.getBottom();
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    /// The baseline of the first line.
    @Override
    public double getBaselineOffset() {
        List<Line> lines = lines(0);
        Insets in = getInsets();
        if (lines.isEmpty() || lines.get(0).nodes.isEmpty()) {
            return in.getTop();
        }
        return in.getTop() + lines.get(0).above;
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double inside = Math.max(1, getWidth() - in.getLeft() - in.getRight());
        List<Line> lines = lines(inside);
        TextAlignment align = getTextAlignment();
        for (int r = 0; r < lines.size(); r++) {
            Line line = lines.get(r);
            double x0 = in.getLeft();
            if (align == TextAlignment.CENTER) {
                x0 += (inside - line.width) / 2;
            } else if (align == TextAlignment.RIGHT) {
                x0 += inside - line.width;
            }
            for (int i = 0; i < line.nodes.size(); i++) {
                Node child = line.nodes.get(i);
                double[] box = line.boxes.get(i);
                if (child.isResizable()) {
                    child.resize(snapSizeX(box[1]), snapSizeY(box[2]));
                }
                child.relocate(snapPositionX(x0 + box[0]), snapPositionY(in.getTop() + line.top + line.above - box[3]));
            }
            reorder(line, x0, in.getTop());
        }
    }

    /// One run of a line: characters of one direction from one child, or
    /// a child that is not text.
    private static final class Run {
        Node node;
        double[] box;
        String text;
        /// 0 left to right, 1 right to left, 2 for neither yet.
        int kind;
        double width;
        double x;
    }

    private static int kind(char c) {
        if (com.codename1.ui.Display.getInstance().isRTL(c)) {
            return 1;
        }
        boolean letter = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c >= 0xc0;
        return letter ? 0 : 2;
    }

    private static boolean hasRightToLeft(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (kind(text.charAt(i)) == 1) {
                return true;
            }
        }
        return false;
    }

    /// The text of a child that can be cut into runs: a `Text` of one
    /// line. `null` for any other child.
    private static String cuttable(Node child) {
        if (!(child instanceof Text)) {
            return null;
        }
        String s = ((Text) child).getText();
        if (s == null) {
            return null;
        }
        s = s.substring(0, s.length() - trailingFeeds(s));
        return s.indexOf('\n') >= 0 ? null : s;
    }

    private static void reverse(List<Run> runs, int from, int to) {
        for (int a = from, b = to - 1; a < b; a++, b--) {
            Run kept = runs.get(a);
            runs.set(a, runs.get(b));
            runs.set(b, kept);
        }
    }

    /// Puts the runs of a line in the order they are seen in, when the
    /// line holds text of both directions.
    private void reorder(Line line, double x0, double top) {
        boolean mixed = false;
        for (int i = 0; i < line.nodes.size(); i++) {
            String s = cuttable(line.nodes.get(i));
            mixed = mixed || (s != null && hasRightToLeft(s));
        }
        if (!mixed) {
            for (int i = 0; i < line.nodes.size(); i++) {
                Node child = line.nodes.get(i);
                if (child instanceof Text) {
                    ((Text) child).flowRuns(null, null);
                }
            }
            return;
        }
        // Cut: a piece per stretch of one kind of character.
        List<Run> runs = new ArrayList<Run>();
        for (int i = 0; i < line.nodes.size(); i++) {
            Node child = line.nodes.get(i);
            String s = cuttable(child);
            if (s == null) {
                Run whole = new Run();
                whole.node = child;
                whole.box = line.boxes.get(i);
                whole.width = whole.box[1];
                runs.add(whole);
                continue;
            }
            int start = 0;
            while (start < s.length()) {
                int k = kind(s.charAt(start));
                int end = start + 1;
                while (end < s.length() && kind(s.charAt(end)) == k) {
                    end++;
                }
                Run piece = new Run();
                piece.node = child;
                piece.box = line.boxes.get(i);
                piece.text = s.substring(start, end);
                piece.kind = k;
                runs.add(piece);
                start = end;
            }
        }
        int base = 0;
        for (int i = 0; i < runs.size(); i++) {
            if (runs.get(i).kind != 2) {
                base = runs.get(i).kind;
                break;
            }
        }
        // What is neither takes the direction both its neighbours have,
        // and that of the line when they differ.
        int before = base;
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (r.kind != 2) {
                before = r.kind;
                continue;
            }
            int after = base;
            for (int j = i + 1; j < runs.size(); j++) {
                if (runs.get(j).kind != 2) {
                    after = runs.get(j).kind;
                    break;
                }
            }
            r.kind = before == after ? before : base;
        }
        // Join what one child has side by side in one direction.
        List<Run> joined = new ArrayList<Run>();
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            Run last = joined.isEmpty() ? null : joined.get(joined.size() - 1);
            if (last != null && last.node == r.node && last.kind == r.kind && r.text != null) {
                last.text = last.text + r.text;
            } else {
                joined.add(r);
            }
        }
        for (int i = 0; i < joined.size(); i++) {
            Run r = joined.get(i);
            if (r.text != null && r.node instanceof Text) {
                r.width = Fonts.width(((Text) r.node).getFont(), r.text);
            }
        }
        // Seen: a line that reads right to left starts at its end, and a
        // row of runs against the line is seen in reverse.
        if (base == 1) {
            reverse(joined, 0, joined.size());
        }
        int from = 0;
        while (from < joined.size()) {
            int to = from;
            while (to < joined.size() && joined.get(to).kind != base) {
                to++;
            }
            if (to > from) {
                reverse(joined, from, to);
                from = to;
            } else {
                from++;
            }
        }
        double at = x0;
        for (int i = 0; i < joined.size(); i++) {
            joined.get(i).x = at;
            at += joined.get(i).width;
        }
        for (int i = 0; i < line.nodes.size(); i++) {
            Node child = line.nodes.get(i);
            double left = Double.MAX_VALUE;
            int count = 0;
            for (int j = 0; j < joined.size(); j++) {
                if (joined.get(j).node == child) {
                    left = Math.min(left, joined.get(j).x);
                    count++;
                }
            }
            if (count == 0) {
                continue;
            }
            double[] box = line.boxes.get(i);
            child.relocate(snapPositionX(left), snapPositionY(top + line.top + line.above - box[3]));
            if (!(child instanceof Text) || cuttable(child) == null) {
                continue;
            }
            String[] parts = new String[count];
            double[] offsets = new double[count];
            int n = 0;
            for (int j = 0; j < joined.size(); j++) {
                Run r = joined.get(j);
                if (r.node == child) {
                    parts[n] = r.text;
                    offsets[n++] = r.x - left;
                }
            }
            ((Text) child).flowRuns(parts, offsets);
        }
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-text-alignment".equals(property)) {
            return getTextAlignment();
        } else if ("-fx-line-spacing".equals(property)) {
            return Double.valueOf(getLineSpacing());
        }
        return super.cn1StyleValue(property);
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-text-alignment".equals(property)) {
            Object k = FontStyles.keyword(TextAlignment.values(), value);
            if (!(k instanceof TextAlignment)) {
                return false;
            }
            setTextAlignment((TextAlignment) k);
            return true;
        } else if ("-fx-line-spacing".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            setLineSpacing(((Number) value).doubleValue());
            return true;
        }
        return super.cn1SetStyleValue(property, value);
    }
}

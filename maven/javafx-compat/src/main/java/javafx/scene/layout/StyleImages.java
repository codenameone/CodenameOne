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
package javafx.scene.layout;

import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

/// The pictures of a background and of a border: reading them from the
/// values a style sheet gives (`-fx-background-image` and the properties
/// beside it, `-fx-border-image-source` and its), and drawing them.
///
/// The values arrive as the text that was written. A style sheet's
/// addresses have been made whole by the build, which knows where the
/// sheet is; an inline style's are read from the root of the
/// application, as JavaFX reads them.
final class StyleImages {

    /// The most copies of one picture drawn in one direction.
    private static final int MOST = 2000;

    private static final HashMap<String, Image> LOADED = new HashMap<String, Image>();

    private StyleImages() {
    }

    // ------------------------------------------------------------ reading

    /// Splits at a separator that is outside parentheses and quotes.
    static List<String> split(String text, char separator) {
        ArrayList<String> out = new ArrayList<String>();
        if (text == null) {
            return out;
        }
        int depth = 0;
        char quote = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && (c == separator || (separator == ' ' && c <= ' '))) {
                String part = text.substring(start, i).trim();
                if (part.length() > 0 || separator != ' ') {
                    out.add(part);
                }
                start = i + 1;
            }
        }
        String last = text.substring(start).trim();
        if (last.length() > 0 || (separator != ' ' && !out.isEmpty())) {
            out.add(last);
        }
        return out;
    }

    private static String layer(List<String> layers, int i) {
        if (layers.isEmpty()) {
            return null;
        }
        // A property with fewer layers than there are pictures starts
        // over, as CSS has it.
        return layers.get(i % layers.size());
    }

    private static boolean is(String word, String expected) {
        return word != null && word.equalsIgnoreCase(expected);
    }

    /// The picture a `url(...)` names, `null` for `none` and for one that
    /// cannot be read.
    static Image image(String term) {
        if (term == null) {
            return null;
        }
        String t = term.trim();
        if (t.length() > 4 && t.regionMatches(true, 0, "url(", 0, 4) && t.endsWith(")")) {
            t = t.substring(4, t.length() - 1).trim();
        } else if (is(t, "none") || t.length() == 0) {
            return null;
        }
        char open = t.length() >= 2 ? t.charAt(0) : 0;
        if ((open == '"' || open == '\'') && t.charAt(t.length() - 1) == open) {
            t = t.substring(1, t.length() - 1);
        }
        if (t.length() == 0) {
            return null;
        }
        Image found = LOADED.get(t);
        if (found == null) {
            found = new Image(t);
            LOADED.put(t, found);
        }
        return found.isError() || found.cn1Native() == null ? null : found;
    }

    /// A length, or a share written with `%` as 0 to 1; NaN for neither.
    /// The second entry is 1 for a share.
    private static double[] measure(String term) {
        String t = term.trim();
        double factor = 1;
        double share = 0;
        int end = t.length();
        if (t.endsWith("%")) {
            factor = 0.01;
            share = 1;
            end--;
        } else if (t.regionMatches(true, Math.max(0, end - 2), "px", 0, 2)) {
            end -= 2;
        } else if (t.regionMatches(true, Math.max(0, end - 2), "em", 0, 2)) {
            factor = Font.getDefault().getSize();
            end -= 2;
        } else if (t.regionMatches(true, Math.max(0, end - 2), "pt", 0, 2)) {
            factor = 96.0 / 72;
            end -= 2;
        }
        try {
            return new double[] {Double.parseDouble(t.substring(0, end)) * factor, share};
        } catch (NumberFormatException notANumber) {
            return new double[] {Double.NaN, 0};
        }
    }

    private static BackgroundRepeat repeat(String word) {
        if (is(word, "repeat")) {
            return BackgroundRepeat.REPEAT;
        } else if (is(word, "space")) {
            return BackgroundRepeat.SPACE;
        } else if (is(word, "round")) {
            return BackgroundRepeat.ROUND;
        }
        return BackgroundRepeat.NO_REPEAT;
    }

    private static BackgroundSize size(String value) {
        List<String> terms = split(value, ' ');
        if (terms.isEmpty()) {
            return BackgroundSize.DEFAULT;
        }
        String first = terms.get(0);
        if (is(first, "cover")) {
            return new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, false, true);
        } else if (is(first, "contain")) {
            return new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, false);
        } else if (is(first, "stretch")) {
            return new BackgroundSize(1, 1, true, true, false, false);
        }
        double[] w = is(first, "auto") ? null : measure(first);
        double[] h = terms.size() < 2 || is(terms.get(1), "auto") ? null : measure(terms.get(1));
        double width = w == null || !(w[0] >= 0) ? BackgroundSize.AUTO : w[0];
        double height = h == null || !(h[0] >= 0) ? BackgroundSize.AUTO : h[0];
        return new BackgroundSize(width, height, w == null || w[1] != 0, h == null || h[1] != 0, false, false);
    }

    private static BackgroundPosition position(String value) {
        List<String> terms = split(value, ' ');
        Side hSide = Side.LEFT;
        Side vSide = Side.TOP;
        double h = 0;
        double v = 0;
        boolean hShare = true;
        boolean vShare = true;
        boolean hGiven = false;
        boolean vGiven = false;
        // What the last word was: 1 a horizontal side, 2 a vertical one;
        // a length after a side is the distance from it.
        int after = 0;
        for (int i = 0; i < terms.size(); i++) {
            String t = terms.get(i);
            if (is(t, "left") || is(t, "right")) {
                hSide = is(t, "left") ? Side.LEFT : Side.RIGHT;
                h = 0;
                hShare = true;
                hGiven = true;
                after = 1;
            } else if (is(t, "top") || is(t, "bottom")) {
                vSide = is(t, "top") ? Side.TOP : Side.BOTTOM;
                v = 0;
                vShare = true;
                vGiven = true;
                after = 2;
            } else if (is(t, "center")) {
                if (!hGiven && (i == 0 || vGiven)) {
                    h = 0.5;
                    hGiven = true;
                } else {
                    v = 0.5;
                    vGiven = true;
                }
                after = 0;
            } else {
                double[] m = measure(t);
                if (m[0] != m[0]) {
                    continue;
                }
                if (after == 1 || (after == 0 && !hGiven)) {
                    h = m[0];
                    hShare = m[1] != 0;
                    hGiven = true;
                } else {
                    v = m[0];
                    vShare = m[1] != 0;
                    vGiven = true;
                }
                after = 0;
            }
        }
        if (hGiven && !vGiven && terms.size() == 1) {
            // One value is the horizontal place; the other is the middle.
            v = 0.5;
        } else if (vGiven && !hGiven && terms.size() == 1) {
            h = 0.5;
        }
        return new BackgroundPosition(hSide, h, hShare, vSide, v, vShare);
    }

    /// The background pictures a style gives, bottom first; none when the
    /// source names none that can be read.
    static BackgroundImage[] backgrounds(Object source, Object repeat, Object position, Object size) {
        if (!(source instanceof String)) {
            return new BackgroundImage[0];
        }
        List<String> sources = split((String) source, ',');
        List<String> repeats = split(repeat instanceof String ? (String) repeat : null, ',');
        List<String> positions = split(position instanceof String ? (String) position : null, ',');
        List<String> sizes = split(size instanceof String ? (String) size : null, ',');
        ArrayList<BackgroundImage> out = new ArrayList<BackgroundImage>();
        for (int i = 0; i < sources.size(); i++) {
            Image image = image(sources.get(i));
            if (image == null) {
                continue;
            }
            BackgroundRepeat rx = BackgroundRepeat.REPEAT;
            BackgroundRepeat ry = BackgroundRepeat.REPEAT;
            BackgroundSize s = sizes.isEmpty() ? BackgroundSize.DEFAULT : size(layer(sizes, i));
            String r = layer(repeats, i);
            if (r != null) {
                List<String> words = split(r, ' ');
                String first = words.isEmpty() ? "repeat" : words.get(0);
                if (is(first, "repeat-x")) {
                    ry = BackgroundRepeat.NO_REPEAT;
                } else if (is(first, "repeat-y")) {
                    rx = BackgroundRepeat.NO_REPEAT;
                } else if (is(first, "stretch")) {
                    rx = BackgroundRepeat.NO_REPEAT;
                    ry = BackgroundRepeat.NO_REPEAT;
                    s = new BackgroundSize(1, 1, true, true, false, false);
                } else {
                    rx = repeat(first);
                    ry = words.size() > 1 ? repeat(words.get(1)) : rx;
                }
            }
            out.add(new BackgroundImage(image, rx, ry,
                    positions.isEmpty() ? BackgroundPosition.DEFAULT : position(layer(positions, i)), s));
        }
        return out.toArray(new BackgroundImage[0]);
    }

    private static BorderRepeat borderRepeat(String word) {
        if (is(word, "repeat")) {
            return BorderRepeat.REPEAT;
        } else if (is(word, "round")) {
            return BorderRepeat.ROUND;
        } else if (is(word, "space")) {
            return BorderRepeat.SPACE;
        }
        return BorderRepeat.STRETCH;
    }

    /// One to four widths, as CSS writes the sides of a box; `null` for a
    /// value that holds no number.
    private static BorderWidths widths(String value, boolean[] fill) {
        List<String> terms = split(value, ' ');
        double[] n = new double[4];
        boolean[] share = new boolean[4];
        int count = 0;
        for (int i = 0; i < terms.size(); i++) {
            String t = terms.get(i);
            if (is(t, "fill")) {
                if (fill != null) {
                    fill[0] = true;
                }
                continue;
            }
            double[] m = measure(t);
            if (m[0] == m[0] && m[0] >= 0 && count < 4) {
                n[count] = m[0];
                share[count++] = m[1] != 0;
            }
        }
        if (count == 0) {
            return null;
        }
        int right = count > 1 ? 1 : 0;
        int bottom = count > 2 ? 2 : 0;
        int left = count > 3 ? 3 : right;
        return new BorderWidths(n[0], n[right], n[bottom], n[left], share[0], share[right], share[bottom],
                share[left]);
    }

    /// The border pictures a style gives; none when the source names none
    /// that can be read.
    static BorderImage[] borders(Object source, Object slice, Object width, Object repeat, Object insets) {
        if (!(source instanceof String)) {
            return new BorderImage[0];
        }
        List<String> sources = split((String) source, ',');
        List<String> slices = split(slice instanceof String ? (String) slice : null, ',');
        List<String> widths = split(width instanceof String ? (String) width : null, ',');
        List<String> repeats = split(repeat instanceof String ? (String) repeat : null, ',');
        List<String> allInsets = split(insets instanceof String ? (String) insets : null, ',');
        ArrayList<BorderImage> out = new ArrayList<BorderImage>();
        for (int i = 0; i < sources.size(); i++) {
            Image image = image(sources.get(i));
            if (image == null) {
                continue;
            }
            boolean[] fill = new boolean[1];
            // A cut that is not given is the whole picture, which leaves
            // four corners and nothing between them.
            BorderWidths cut = slices.isEmpty() ? null : widths(layer(slices, i), fill);
            if (cut == null) {
                cut = new BorderWidths(1, 1, 1, 1, true, true, true, true);
            }
            BorderWidths w = widths.isEmpty() ? null : widths(layer(widths, i), null);
            BorderWidths in = allInsets.isEmpty() ? null : widths(layer(allInsets, i), null);
            BorderRepeat rx = BorderRepeat.STRETCH;
            BorderRepeat ry = BorderRepeat.STRETCH;
            String r = layer(repeats, i);
            if (r != null) {
                List<String> words = split(r, ' ');
                String first = words.isEmpty() ? "stretch" : words.get(0);
                if (is(first, "repeat-x")) {
                    rx = BorderRepeat.REPEAT;
                } else if (is(first, "repeat-y")) {
                    ry = BorderRepeat.REPEAT;
                } else {
                    rx = borderRepeat(first);
                    ry = words.size() > 1 ? borderRepeat(words.get(1)) : rx;
                }
            }
            out.add(new BorderImage(image, w, in == null ? Insets.EMPTY
                    : new Insets(in.getTop(), in.getRight(), in.getBottom(), in.getLeft()), cut, fill[0], rx, ry));
        }
        return out.toArray(new BorderImage[0]);
    }

    // ------------------------------------------------------------ drawing

    private static double tile(BackgroundRepeat repeat, double room, double size) {
        if (repeat == BackgroundRepeat.ROUND && size > 0) {
            // Resized so that a whole number of copies fits.
            return room / Math.max(1, Math.round(room / size));
        }
        return size;
    }

    /// Fills `room` with copies of length `size` and calls them out as
    /// starts in `starts`; answers how many.
    private static int starts(BackgroundRepeat repeat, double room, double size, double place, double[] starts) {
        if (!(size > 0)) {
            return 0;
        }
        if (repeat == BackgroundRepeat.NO_REPEAT) {
            starts[0] = place;
            return 1;
        }
        if (repeat == BackgroundRepeat.SPACE) {
            int n = (int) Math.floor(room / size);
            if (n < 2) {
                starts[0] = n == 1 ? place : (room - size) / 2;
                return 1;
            }
            n = Math.min(n, starts.length);
            double gap = (room - n * size) / (n - 1);
            for (int i = 0; i < n; i++) {
                starts[i] = i * (size + gap);
            }
            return n;
        }
        double first = repeat == BackgroundRepeat.ROUND ? 0 : place - Math.ceil(place / size) * size;
        int n = 0;
        for (double at = first; at < room - 1e-6 && n < starts.length; at += size) {
            starts[n++] = at;
        }
        return n;
    }

    /// Draws one background picture over a region of a size.
    static void paint(Renderer renderer, BackgroundImage bi, double w, double h) {
        com.codename1.ui.Image picture = bi.getImage().cn1Native();
        double iw = bi.getImage().getWidth();
        double ih = bi.getImage().getHeight();
        if (picture == null || !(iw > 0) || !(ih > 0) || !(w > 0) || !(h > 0)) {
            return;
        }
        BackgroundSize size = bi.getSize();
        double tw;
        double th;
        if (size.isCover() || size.isContain()) {
            double scale = size.isCover() ? Math.max(w / iw, h / ih) : Math.min(w / iw, h / ih);
            tw = iw * scale;
            th = ih * scale;
        } else {
            double sw = size.getWidth();
            double sh = size.getHeight();
            tw = sw == BackgroundSize.AUTO ? -1 : (size.isWidthAsPercentage() ? sw * w : sw);
            th = sh == BackgroundSize.AUTO ? -1 : (size.isHeightAsPercentage() ? sh * h : sh);
            if (tw < 0 && th < 0) {
                tw = iw;
                th = ih;
            } else if (tw < 0) {
                tw = th * iw / ih;
            } else if (th < 0) {
                th = tw * ih / iw;
            }
        }
        tw = tile(bi.getRepeatX(), w, tw);
        th = tile(bi.getRepeatY(), h, th);
        if (!(tw > 0) || !(th > 0)) {
            return;
        }
        BackgroundPosition p = bi.getPosition();
        double x = p.isHorizontalAsPercentage() ? (w - tw) * p.getHorizontalPosition() : p.getHorizontalPosition();
        double y = p.isVerticalAsPercentage() ? (h - th) * p.getVerticalPosition() : p.getVerticalPosition();
        if (p.getHorizontalSide() == Side.RIGHT) {
            x = w - tw - x;
        }
        if (p.getVerticalSide() == Side.BOTTOM) {
            y = h - th - y;
        }
        double[] xs = new double[(int) Math.min(MOST, Math.max(1, Math.ceil(w / tw) + 2))];
        double[] ys = new double[(int) Math.min(MOST, Math.max(1, Math.ceil(h / th) + 2))];
        int nx = starts(bi.getRepeatX(), w, tw, x, xs);
        int ny = starts(bi.getRepeatY(), h, th, y, ys);
        renderer.save();
        FxPath box = new FxPath();
        box.addRect(0, 0, w, h);
        renderer.clip(box);
        for (int row = 0; row < ny; row++) {
            for (int column = 0; column < nx; column++) {
                renderer.drawImage(picture, xs[column], ys[row], tw, th);
            }
        }
        renderer.restore();
    }

    private static double part(double value, boolean share, double whole) {
        return share ? value * whole : value;
    }

    private static com.codename1.ui.Image piece(com.codename1.ui.Image picture, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return null;
        }
        return picture.subImage(x, y, w, h, true);
    }

    /// Draws one piece of a border along a side, or over the middle.
    private static void side(Renderer renderer, com.codename1.ui.Image piece, double x, double y, double w, double h,
            double tw, double th, BorderRepeat rx, BorderRepeat ry) {
        if (piece == null || !(w > 0) || !(h > 0)) {
            return;
        }
        if (rx == BorderRepeat.STRETCH || !(tw > 0)) {
            tw = w;
        } else if (rx == BorderRepeat.ROUND) {
            tw = w / Math.max(1, Math.round(w / tw));
        }
        if (ry == BorderRepeat.STRETCH || !(th > 0)) {
            th = h;
        } else if (ry == BorderRepeat.ROUND) {
            th = h / Math.max(1, Math.round(h / th));
        }
        if (tw == w && th == h) {
            renderer.drawImage(piece, x, y, w, h);
            return;
        }
        double[] xs = new double[(int) Math.min(MOST, Math.ceil(w / tw) + 2)];
        double[] ys = new double[(int) Math.min(MOST, Math.ceil(h / th) + 2)];
        // A repeated side is centred, so that it is cut the same at both
        // corners.
        int nx = starts(rx == BorderRepeat.SPACE ? BackgroundRepeat.SPACE
                : (tw == w ? BackgroundRepeat.NO_REPEAT : BackgroundRepeat.REPEAT), w, tw,
                tw == w || rx == BorderRepeat.ROUND ? 0 : (w - tw) / 2, xs);
        int ny = starts(ry == BorderRepeat.SPACE ? BackgroundRepeat.SPACE
                : (th == h ? BackgroundRepeat.NO_REPEAT : BackgroundRepeat.REPEAT), h, th,
                th == h || ry == BorderRepeat.ROUND ? 0 : (h - th) / 2, ys);
        renderer.save();
        FxPath box = new FxPath();
        box.addRect(x, y, w, h);
        renderer.clip(box);
        for (int row = 0; row < ny; row++) {
            for (int column = 0; column < nx; column++) {
                renderer.drawImage(piece, x + xs[column], y + ys[row], tw, th);
            }
        }
        renderer.restore();
    }

    /// Draws one border picture around a region of a size.
    static void paint(Renderer renderer, BorderImage bi, double w, double h) {
        com.codename1.ui.Image picture = bi.getImage().cn1Native();
        if (picture == null) {
            return;
        }
        int iw = (int) Math.round(bi.getImage().getWidth());
        int ih = (int) Math.round(bi.getImage().getHeight());
        Insets in = bi.getInsets();
        double x = in.getLeft();
        double y = in.getTop();
        double aw = w - in.getLeft() - in.getRight();
        double ah = h - in.getTop() - in.getBottom();
        if (iw <= 0 || ih <= 0 || !(aw > 0) || !(ah > 0)) {
            return;
        }
        BorderWidths s = bi.getSlices();
        int st = (int) Math.min(ih, Math.round(part(s.getTop(), s.isTopAsPercentage(), ih)));
        int sb = (int) Math.min(ih - st, Math.round(part(s.getBottom(), s.isBottomAsPercentage(), ih)));
        int sl = (int) Math.min(iw, Math.round(part(s.getLeft(), s.isLeftAsPercentage(), iw)));
        int sr = (int) Math.min(iw - sl, Math.round(part(s.getRight(), s.isRightAsPercentage(), iw)));
        com.codename1.ui.Image[] p = bi.pieces();
        if (p == null) {
            int mw = iw - sl - sr;
            int mh = ih - st - sb;
            p = new com.codename1.ui.Image[] {
                piece(picture, 0, 0, sl, st), piece(picture, sl, 0, mw, st), piece(picture, iw - sr, 0, sr, st),
                piece(picture, 0, st, sl, mh), piece(picture, sl, st, mw, mh), piece(picture, iw - sr, st, sr, mh),
                piece(picture, 0, ih - sb, sl, sb), piece(picture, sl, ih - sb, mw, sb),
                piece(picture, iw - sr, ih - sb, sr, sb),
            };
            bi.pieces(p);
        }
        BorderWidths bw = bi.getWidths();
        double wt = Math.min(ah, Math.max(0, part(bw.getTop(), bw.isTopAsPercentage(), ah)));
        double wb = Math.min(ah - wt, Math.max(0, part(bw.getBottom(), bw.isBottomAsPercentage(), ah)));
        double wl = Math.min(aw, Math.max(0, part(bw.getLeft(), bw.isLeftAsPercentage(), aw)));
        double wr = Math.min(aw - wl, Math.max(0, part(bw.getRight(), bw.isRightAsPercentage(), aw)));
        double mw = aw - wl - wr;
        double mh = ah - wt - wb;
        // A side piece keeps its shape: as thick as the border, and as
        // long as that makes it.
        double topTile = st > 0 ? (iw - sl - sr) * wt / st : 0;
        double bottomTile = sb > 0 ? (iw - sl - sr) * wb / sb : 0;
        double leftTile = sl > 0 ? (ih - st - sb) * wl / sl : 0;
        double rightTile = sr > 0 ? (ih - st - sb) * wr / sr : 0;
        BorderRepeat rx = bi.getRepeatX();
        BorderRepeat ry = bi.getRepeatY();
        if (bi.isFilled()) {
            side(renderer, p[4], x + wl, y + wt, mw, mh, topTile > 0 ? topTile : bottomTile,
                    leftTile > 0 ? leftTile : rightTile, rx, ry);
        }
        side(renderer, p[1], x + wl, y, mw, wt, topTile, 0, rx, BorderRepeat.STRETCH);
        side(renderer, p[7], x + wl, y + ah - wb, mw, wb, bottomTile, 0, rx, BorderRepeat.STRETCH);
        side(renderer, p[3], x, y + wt, wl, mh, 0, leftTile, BorderRepeat.STRETCH, ry);
        side(renderer, p[5], x + aw - wr, y + wt, wr, mh, 0, rightTile, BorderRepeat.STRETCH, ry);
        side(renderer, p[0], x, y, wl, wt, 0, 0, BorderRepeat.STRETCH, BorderRepeat.STRETCH);
        side(renderer, p[2], x + aw - wr, y, wr, wt, 0, 0, BorderRepeat.STRETCH, BorderRepeat.STRETCH);
        side(renderer, p[6], x, y + ah - wb, wl, wb, 0, 0, BorderRepeat.STRETCH, BorderRepeat.STRETCH);
        side(renderer, p[8], x + aw - wr, y + ah - wb, wr, wb, 0, 0, BorderRepeat.STRETCH, BorderRepeat.STRETCH);
    }
}

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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;

import java.util.ArrayList;
import java.util.List;

/// A row of dashboard numbers, each over its caption, sized as one thing.
///
/// A label asked to fit its own text decides alone and after the fact: it
/// changes its font once it has been given a width, by which time the tile
/// around it has its height from the font it had before, and its neighbour
/// has decided something else. The result was a row whose numbers were three
/// sizes and whose captions were cut off, or pushed out of the tile.
///
/// So the row decides, before it lays its tiles out and from the width it
/// really has: one size for every number, the largest at which the longest
/// fits, and one for every caption, which goes onto a second line before it
/// is made any smaller.
public final class StatRow extends Container {
    /// The smallest a number and a caption are drawn, in millimetres.
    private static final float SMALLEST_VALUE = 2.6f;
    private static final float SMALLEST_CAPTION = 1.9f;

    private final int columns;
    private final List<Tile> tiles = new ArrayList<Tile>();
    private int laidOutAt;
    private String fitted = "";
    private String planned = "";
    private boolean correcting;

    StatRow(int columns) {
        super(new GridLayout(1, columns));
        this.columns = columns;
    }

    /// Adds a number: `value` as it is, over `caption` in the user's language.
    /// The number's label is called `name`.
    public StatRow stat(String value, String caption, String name) {
        Tile tile = new Tile(value, Lang.tr(caption), name);
        tiles.add(tile);
        add(tile);
        return this;
    }

    /// Takes every number out, for a row that is filled again.
    public void clear() {
        tiles.clear();
        removeAll();
    }

    @Override
    protected Dimension calcPreferredSize() {
        // Asked how tall it is before it knows how wide: answered for the
        // width it last had, or the room its nearest sized ancestor offers.
        fit(laidOutAt > 0 ? laidOutAt : roomOffered());
        planned = fitted;
        return super.calcPreferredSize();
    }

    @Override
    public void layoutContainer() {
        int width = getWidth();
        if (width > 0) {
            laidOutAt = width;
            fit(width);
            if (!fitted.equals(planned) && !correcting) {
                // The height was answered for another width, and the tiles
                // are a different height at this one.
                correcting = true;
                CN.callSerially(() -> {
                    correcting = false;
                    setShouldCalcPreferredSize(true);
                    Form form = getComponentForm();
                    if (form != null) {
                        form.revalidate();
                    }
                });
            }
        }
        super.layoutContainer();
    }

    private int roomOffered() {
        Container parent = getParent();
        while (parent != null) {
            if (parent.getWidth() > 0) {
                return parent.getWidth() - parent.getStyle().getHorizontalPadding();
            }
            parent = parent.getParent();
        }
        return CN.getDisplayWidth();
    }

    private void fit(int width) {
        if (tiles.isEmpty() || width <= 0) {
            return;
        }
        int across = Math.min(columns, tiles.size());
        Tile first = tiles.get(0);
        int room = (width - getStyle().getHorizontalPadding()) / across
                - first.getStyle().getHorizontalPadding()
                - first.getStyle().getHorizontalMargins();
        if (room <= 0) {
            return;
        }
        float value = 1f;
        float caption = 1f;
        for (Tile tile : tiles) {
            value = Math.min(value, share(room - tile.value.sides(), tile.value.width(tile.number)));
            tile.split(room - tile.upper.sides());
            caption = Math.min(caption, share(room - tile.upper.sides(), tile.widest()));
        }
        StringBuilder made = new StringBuilder();
        for (Tile tile : tiles) {
            made.append(tile.value.scale(value, SMALLEST_VALUE)).append('/');
            made.append(tile.upper.scale(caption, SMALLEST_CAPTION)).append('/');
            tile.lower.scale(caption, SMALLEST_CAPTION);
            made.append(tile.show()).append(' ');
        }
        fitted = made.toString();
    }

    /// The part of its full size at which text `needed` pixels wide fits
    /// `room`.
    private static float share(int room, int needed) {
        return needed <= room || needed <= 0 ? 1f : Math.max(0f, room) / (float) needed;
    }

    /// One number over its caption.
    private static final class Tile extends Container {
        private final String number;
        private final String caption;
        private final Sized value;
        private final Sized upper;
        private final Sized lower;
        private String top;
        private String bottom;

        Tile(String number, String caption, String name) {
            super(BoxLayout.y());
            setUIID("WlStat");
            this.number = number == null ? "" : number;
            this.caption = caption == null ? "" : caption;
            value = new Sized(Ui.plain(this.number, "WlStatValue"));
            value.label.setName(name);
            upper = new Sized(Ui.plain(this.caption, "WlStatLabel"));
            lower = new Sized(Ui.plain("", "WlStatLabel"));
            top = this.caption;
            bottom = "";
            add(value.label).add(upper.label).add(lower.label);
            show();
        }

        /// Puts the caption on one line if `room` holds it, and otherwise on
        /// two, broken at the space that leaves the longer line shortest.
        void split(int room) {
            top = caption;
            bottom = "";
            int whole = upper.width(caption);
            if (whole <= room) {
                return;
            }
            int best = whole;
            for (int at = caption.indexOf(' '); at > 0; at = caption.indexOf(' ', at + 1)) {
                String before = caption.substring(0, at);
                String after = caption.substring(at + 1);
                int longer = Math.max(upper.width(before), upper.width(after));
                if (longer < best) {
                    best = longer;
                    top = before;
                    bottom = after;
                }
            }
        }

        int widest() {
            return Math.max(upper.width(top), upper.width(bottom));
        }

        /// Writes the lines decided by [#split]; answers how many there are.
        int show() {
            if (!top.equals(upper.label.getText())) {
                upper.label.setText(top);
            }
            if (!bottom.equals(lower.label.getText())) {
                lower.label.setText(bottom);
            }
            boolean second = bottom.length() > 0;
            if (lower.label.isHidden() == second) {
                lower.label.setHidden(!second);
                lower.label.setVisible(second);
            }
            return second ? 2 : 1;
        }
    }

    /// A label and the font its style gave it, which is the size it is drawn
    /// at when there is room. The style is asked again every time, because a
    /// label is restyled under us: the desktop console gives it another name,
    /// and a change of theme gives it another font.
    private static final class Sized {
        private final Label label;
        private Font styled;
        private Font applied;
        private int appliedPixels;

        Sized(Label label) {
            this.label = label;
        }

        private Font styled() {
            Font now = label.getUnselectedStyle().getFont();
            if (now != applied || styled == null) {
                styled = now;
                applied = now;
                appliedPixels = Math.round(now.getPixelSize());
            }
            return styled;
        }

        int sides() {
            return label.getUnselectedStyle().getHorizontalPadding()
                    + label.getUnselectedStyle().getHorizontalMargins();
        }

        /// How wide `text` is at the full size.
        int width(String text) {
            return text.length() == 0 ? 0 : styled().stringWidth(text);
        }

        /// Draws the label at `share` of its full size, and never smaller
        /// than `smallest` millimetres; answers the size in pixels.
        int scale(float share, float smallest) {
            Font full = styled();
            int most = Math.round(full.getPixelSize());
            if (most <= 0) {
                // Not a font that can be drawn at another size.
                return 0;
            }
            int least = Math.min(most, CN.convertToPixels(smallest));
            int pixels = share >= 1f ? most : Math.max(least, (int) Math.floor(most * share));
            if (pixels != appliedPixels) {
                applied = pixels == most ? full : full.derive(pixels, full.getStyle());
                appliedPixels = pixels;
                label.getAllStyles().setFont(applied);
            }
            return pixels;
        }
    }
}

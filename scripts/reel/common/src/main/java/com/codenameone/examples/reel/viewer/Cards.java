/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.viewer;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.CN;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.reel.api.TitleCardDto;
import com.codenameone.examples.reel.ui.Art;
import com.codenameone.examples.reel.ui.Layouts;
import com.codenameone.examples.reel.ui.Ui;
import java.util.List;

/// Title cards: a landscape still of the title with what matters at a glance
/// laid over it (a badge, how far the profile got) and its name under it, alone
/// or in a row that scrolls sideways.
public final class Cards {
    private Cards() {
    }

    /// The width of a card, in millimetres: larger on a wide screen, where a
    /// row shows five or six of them, than on a phone, where it shows two and
    /// the start of a third.
    public static float width() {
        return Layouts.wide() ? 58f : 40f;
    }

    /// One card; a tap opens the title.
    public static Container card(final Form from, final TitleCardDto t) {
        float w = width();
        float h = w * 9f / 16f;
        Label still = new Label("", "RlPoster");
        Art.load(still, t.posterUrl(), w, h);
        Container tile = new Container(new LayeredLayout());
        tile.setUIID("RlTile");
        tile.add(still);
        String badge = t.badge() != null && t.badge().length() > 0 ? t.badge()
                : t.free() ? "Free" : t.rentable() ? "Rent" : null;
        if (badge != null) {
            Label b = Ui.badge(badge, "RlCardBadge");
            tile.add(b);
            ((LayeredLayout) tile.getLayout()).setInsets(b, "1mm auto auto 1mm");
        }
        if (t.progressPercent() > 0) {
            Progress bar = new Progress(t.progressPercent(), w);
            tile.add(bar);
            ((LayeredLayout) tile.getLayout()).setInsets(bar, "auto 0 0 0");
        }
        Container box = new Container(BoxLayout.y());
        box.setUIID("RlCard");
        box.add(tile);
        Label name = Ui.label(t.name(), "RlCardName");
        name.setPreferredW(CN.convertToPixels(w));
        box.add(name);
        Button tap = new Button();
        tap.setUIID("Container");
        tap.setName("title-" + t.id());
        tap.addActionListener(e -> TitleForm.show(from, t.id()));
        box.setLeadComponent(tap);
        box.add(tap);
        return box;
    }

    /// A headed row of cards that scrolls sideways, starting at the gutter and
    /// running out under the right-hand edge.
    public static Container row(Form from, String id, String heading, List<TitleCardDto> titles) {
        int side = CN.convertToPixels(Layouts.gutter());
        Container row = new Container(BoxLayout.y());
        row.setUIID("RlRow");
        Label title = Ui.label(heading, "RlRowTitle");
        title.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        title.getAllStyles().setPaddingLeft(side);
        row.add(title);
        Container strip = new Container(BoxLayout.x());
        strip.setUIID("RlStrip");
        strip.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        strip.getAllStyles().setPaddingLeft(side);
        strip.getAllStyles().setPaddingRight(side);
        strip.setScrollableX(true);
        strip.setScrollVisible(false);
        strip.setName("row-" + id);
        for (int iter = 0; titles != null && iter < titles.size(); iter++) {
            strip.add(card(from, titles.get(iter)));
        }
        row.add(strip);
        return row;
    }

    /// A thin bar showing how far into a title the profile got.
    public static final class Progress extends com.codename1.ui.Component {
        private final int percent;

        private final float widthMm;

        public Progress(int percent, float widthMm) {
            this.percent = Math.max(0, Math.min(100, percent));
            this.widthMm = widthMm;
            setUIID("RlProgress");
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
            int w = getWidth() * percent / 100;
            int alpha = g.getAlpha();
            g.setColor(0);
            g.setAlpha(150);
            g.fillRect(getX(), getY(), getWidth(), getHeight());
            g.setAlpha(alpha);
            g.setColor(getStyle().getFgColor());
            g.fillRect(getX(), getY(), w, getHeight());
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(CN.convertToPixels(widthMm),
                    CN.convertToPixels(0.7f));
        }
    }
}

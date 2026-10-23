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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.reel.api.TitleCardDto;
import com.codenameone.examples.reel.ui.Art;
import com.codenameone.examples.reel.ui.Ui;
import java.util.List;

/// Title cards: a poster with what matters at a glance (progress, a badge,
/// free or for rent), alone or in a row that scrolls sideways.
public final class Cards {
    public static final float POSTER_W = 26f;
    public static final float POSTER_H = 39f;

    private Cards() {
    }

    /// One card; a tap opens the title.
    public static Container card(final Form from, final TitleCardDto t) {
        Label poster = new Label(t.name(), "RlPoster");
        Art.load(poster, t.posterUrl(), POSTER_W, POSTER_H);
        Container box = new Container(new BorderLayout());
        box.setUIID("RlCard");
        box.add(BorderLayout.CENTER, poster);
        Container under = new Container(BoxLayout.y());
        if (t.progressPercent() > 0) {
            under.add(new Progress(t.progressPercent()));
        }
        String badge = t.badge() != null && t.badge().length() > 0 ? t.badge()
                : t.free() ? "Free" : t.rentable() ? "Rent" : null;
        if (badge != null) {
            under.add(Ui.badge(badge, "RlCardBadge"));
        }
        box.add(BorderLayout.SOUTH, under);
        Button tap = new Button();
        tap.setUIID("Container");
        tap.setName("title-" + t.id());
        tap.addActionListener(e -> TitleForm.show(from, t.id()));
        box.setLeadComponent(tap);
        box.add(BorderLayout.NORTH, tap);
        return box;
    }

    /// A headed row of cards that scrolls sideways.
    public static Container row(Form from, String id, String heading, List<TitleCardDto> titles) {
        Container row = new Container(BoxLayout.y());
        row.add(Ui.label(heading, "RlRowTitle"));
        Container strip = new Container(BoxLayout.x());
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

        public Progress(int percent) {
            this.percent = Math.max(0, Math.min(100, percent));
            setUIID("RlProgress");
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
            int w = getWidth() * percent / 100;
            g.setColor(getStyle().getFgColor());
            g.fillRect(getX(), getY(), w, getHeight());
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(com.codename1.ui.CN.convertToPixels(POSTER_W),
                    com.codename1.ui.CN.convertToPixels(0.8f));
        }
    }
}

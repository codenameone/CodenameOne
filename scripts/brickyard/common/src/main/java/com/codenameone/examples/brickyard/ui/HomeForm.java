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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.CN;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.api.PlayingDto;
import com.codenameone.examples.brickyard.api.PresenceDto;
import com.codenameone.examples.brickyard.api.QuestDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;

import java.util.List;
import java.util.Map;

/// Home: rows of experiences -- continue, featured, trending, then one row
/// per genre -- each scrolling sideways.
public final class HomeForm {
    private HomeForm() {
    }

    public static void show() {
        final Form f = Ui.section("Brickyard", "home", 0);
        Ui.settingsButton(f);
        final Container content = Ui.content(f);
        content.add(Ui.bricks());
        content.add(Ui.label("Loading...", "Muted"));
        capWidth(content);
        f.addSizeChangedListener(e -> {
            capWidth(content);
            f.revalidate();
        });
        f.show();
        // Live player counts first, so the tiles can say how many are in each
        // game; a failed count only loses the "playing" line.
        Api.experiences().playing(Net.to(counts -> {
            PLAYING.clear();
            for (PlayingDto c : counts) {
                PLAYING.put(c.experienceId(), Long.valueOf(c.players()));
            }
            browse(content, f);
        }, (status, message) -> browse(content, f)));
    }

    /// Players in each experience now, from the last refresh of Home.
    static final java.util.Map<String, Long> PLAYING = new java.util.HashMap<String, Long>();

    private static void browse(final Container content, final Form f) {
        Api.experiences().browse(null, Net.to(all -> {
            content.removeAll();
            content.add(Ui.bricks());
            // Games first, as a player expects of a game platform: friends in
            // a game, then the rows. The level and quests follow the first
            // rows instead of pushing them off the screen.
            friendsPlaying(content, f, all);
            fill(content, all);
            progress(content, f, afterSecondRow(content));
            f.revalidate();
        }, (status, message) -> {
            content.removeAll();
            content.add(Ui.text(message, "Muted"));
            content.add(Ui.secondary("Try again", "retryButton", e -> show()));
            f.revalidate();
        }));
    }

    /// The index just past the second row of games, or the end when there
    /// are fewer: a row is its title and its strip, so count the strips.
    static int afterSecondRow(Container content) {
        int rows = 0;
        for (int i = 0; i < content.getComponentCount(); i++) {
            String name = content.getComponentAt(i).getName();
            if (name != null && name.startsWith("row") && !"rowFriendsPlaying".equals(name) && ++rows == 2) {
                return i + 1;
            }
        }
        return content.getComponentCount();
    }

    /// The login streak and today's quests, placed at `index` among the rows.
    /// Silent when the call fails: Home still works without them.
    static void progress(final Container content, final Form f, int index) {
        final Container box = new Container(BoxLayout.y());
        box.setName("progress");
        content.addComponent(index, box);
        Api.progression().dailyLogin(Net.to(p -> {
            if (p != null) {
                box.addComponent(0, ProgressViews.hero(p, null));
                f.revalidate();
            }
        }, (status, message) -> { }));
        Api.progression().quests(Net.to(quests -> {
            if (quests == null) {
                return;
            }
            // Two at most, ready-to-claim first: Home is for the games, and
            // the Me page lists every quest.
            int shown = 0;
            for (int pass = 0; pass < 2 && shown < 2; pass++) {
                for (final QuestDto q : quests) {
                    boolean ready = q.claimed() == 0 && q.progress() >= q.target();
                    boolean open = q.claimed() == 0 && q.progress() < q.target();
                    if (shown < 2 && (pass == 0 ? ready : open)) {
                        box.add(ProgressViews.quest(q, HomeForm::show));
                        shown++;
                    }
                }
            }
            f.revalidate();
        }, (status, message) -> { }));
    }

    /// A "Friends playing" row: each friend in a game now, with Join. Silent
    /// when nobody is playing or the call fails.
    static void friendsPlaying(final Container content, final Form f, final List<ExperienceDto> all) {
        final Container row = new Container(BoxLayout.x());
        row.setScrollableX(true);
        // A swipeable row; the scroll thumb drew as a thin white line under it.
        row.setScrollVisible(false);
        row.setName("rowFriendsPlaying");
        final com.codename1.ui.Label title = Ui.label("", "RowTitle");
        content.add(title);
        content.add(row);
        Api.social().friendPresence(Net.to(presence -> {
            if (presence == null) {
                return;
            }
            for (final PresenceDto p : presence) {
                if (!FriendsForm.inGame(p)) {
                    continue;
                }
                String game = titleOf(all, p.experienceId());
                row.add(Ui.panel(Ui.person(p.username(), "CardTitle"),
                        Ui.label(game == null ? "Playing" : "Playing " + game, "CardMeta"),
                        Ui.primary("Join", "homeJoin" + p.username(), e -> FriendsForm.join(p.username()))));
            }
            if (row.getComponentCount() > 0) {
                title.setText("Friends playing");
            }
            f.revalidate();
        }, (status, message) -> { }));
    }

    static final int NEW_VISITS = 10;

    /// The widest the home column grows. On a tablet or desktop the phone
    /// column stretched edge to edge; the rest becomes side margin.
    static final int MAX_COLUMN_MM = 150;

    /// Centres `content` in a column at most MAX_COLUMN_MM wide.
    static void capWidth(Container content) {
        int max = CN.convertToPixels(MAX_COLUMN_MM);
        if (!CN.isTablet() && !CN.isDesktop()) {
            return;
        }
        int side = Math.max(CN.convertToPixels(2), (CN.getDisplayWidth() - max) / 2);
        com.codename1.ui.plaf.Style s = content.getAllStyles();
        s.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        s.setPadding(s.getPaddingTop(), s.getPaddingBottom(), side, side);
    }

    static String titleOf(List<ExperienceDto> all, String id) {
        if (all != null) {
            for (ExperienceDto e : all) {
                if (e.id().equals(id)) {
                    return e.title();
                }
            }
        }
        return null;
    }

    static void fill(Container content, List<ExperienceDto> all) {
        com.codenameone.examples.brickyard.play.PlayMcp.catalog(all);
        if (all == null || all.isEmpty()) {
            content.add(Ui.text("Nothing is published yet. Make the first experience in Create.", "Muted"));
            return;
        }
        row(content, "Continue", Experiences.recent(all));
        // Featured is the top few; Trending leaves those out, and a genre
        // with one game gets no row, so a game is not listed three times.
        List<ExperienceDto> featured = Experiences.featured(all);
        row(content, "Featured", featured);
        row(content, "Trending", Experiences.without(Experiences.trending(all), featured));
        for (Map.Entry<String, List<ExperienceDto>> genre : Experiences.genres(all).entrySet()) {
            if (genre.getValue().size() >= 2) {
                row(content, genre.getKey(), genre.getValue());
            }
        }
    }

    static void row(Container content, String title, List<ExperienceDto> items) {
        if (items.isEmpty()) {
            return;
        }
        content.add(Ui.label(title, "RowTitle"));
        Container row = new Container(BoxLayout.x());
        row.setScrollableX(true);
        // A swipeable row; the scroll thumb drew as a thin white line under it.
        row.setScrollVisible(false);
        row.setName("row" + title);
        for (ExperienceDto e : items) {
            row.add(card(e));
        }
        content.add(row);
    }

    static Container card(final ExperienceDto e) {
        com.codename1.ui.Button open = Ui.button("", "Card", "open" + e.id(), ev -> ExperienceForm.show(e.id()));
        // A tile, not a panel: the picture, the name and a thumbs-up line.
        com.codename1.ui.Label meta = Ui.label(meta(e), "CardMeta");
        com.codename1.ui.FontImage.setMaterialIcon(meta, com.codename1.ui.FontImage.MATERIAL_THUMB_UP, 2.4f);
        Container card = BoxLayout.encloseY(new Thumbnail(e, 30),
                Ui.label(e.title(), "CardTitle"),
                meta);
        card.setUIID("Card");
        card.setLeadComponent(open);
        card.add(open);
        open.setHidden(true);
        // The hidden lead button is what a screen reader activates; give it a name.
        open.setAccessibilityText("Open " + e.title());
        return card;
    }

    static String meta(ExperienceDto e) {
        int rating = Experiences.rating(e);
        Long now = PLAYING.get(e.id());
        String reach = now != null && now.longValue() > 0 ? now + " playing"
                : ExperienceForm.count(e.visits(), "visit");
        if (rating >= 0) {
            return rating + "% liked  -  " + reach;
        }
        // No publish date reaches the client, so "New" means barely played;
        // an unrated game with real traffic shows its reach alone.
        return e.visits() < NEW_VISITS ? "New  -  " + reach : reach;
    }
}

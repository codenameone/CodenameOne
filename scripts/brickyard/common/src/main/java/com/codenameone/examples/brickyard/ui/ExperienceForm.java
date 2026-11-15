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
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.brickyard.api.BadgeDto;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.api.VoteDto;
import com.codenameone.examples.brickyard.engage.Friends;
import com.codenameone.examples.brickyard.engage.Telemetry;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.play.PlaySession;

import java.util.List;

/// One experience: its picture, creator, rating, visits, badges, and the
/// Play button.
public final class ExperienceForm {
    private ExperienceForm() {
    }

    public static void show(final String id) {
        final Form f = Ui.form("", "experience");
        Ui.back(f, Nav::home);
        f.add(Ui.label("Loading...", "Muted"));
        f.show();
        Api.experiences().experience(id, Net.to(e -> {
            f.removeAll();
            fill(f, e);
            f.revalidate();
        }, (status, message) -> {
            f.removeAll();
            f.add(Ui.text(message, "Muted"));
            f.revalidate();
        }));
    }

    static void fill(final Form f, final ExperienceDto e) {
        f.setTitle(e.title());
        f.add(new Thumbnail(e.id(), 80));
        f.add(Ui.label(e.title(), "Logo"));
        f.add(Ui.label("by " + e.owner(), "Muted"));
        if (e.description() != null && e.description().length() > 0) {
            f.add(Ui.text(e.description(), "Body"));
        }
        final com.codename1.ui.Label stats = Ui.label(stats(e), "CardMeta");
        stats.setName("stats");
        f.add(stats);
        f.add(Ui.primary("Play", "playButton", ev -> play(e)));
        Container votes = BoxLayout.encloseX(
                Ui.secondary("Like", "likeButton", ev -> Api.experiences().vote(e.id(), new VoteDto(1),
                        Net.to(updated -> refresh(f, stats, updated), Ui::failed))),
                Ui.secondary("Dislike", "dislikeButton", ev -> Api.experiences().vote(e.id(), new VoteDto(-1),
                        Net.to(updated -> refresh(f, stats, updated), Ui::failed))),
                Ui.secondary("Favourite", "favouriteButton", ev -> Api.experiences().favourite(e.id(),
                        Net.to(updated -> refresh(f, stats, updated), Ui::failed))));
        f.add(votes);
        f.add(Ui.link("Leaderboards", "leaderboardsButton", ev -> LeaderboardForm.show(e.id(), e.title())));
        f.add(Friends.button(e.id(), e.title()));
        final Container badges = new Container(BoxLayout.y());
        badges.setName("badges");
        f.add(Ui.label("Badges", "RowTitle"));
        f.add(badges);
        Api.badges().badges(e.id(), Net.to(list -> {
            fillBadges(badges, list);
            f.revalidate();
        }, (status, message) -> badges.add(Ui.label("Badges could not be loaded.", "Muted"))));
    }

    static void fillBadges(Container badges, List<BadgeDto> list) {
        badges.removeAll();
        if (list == null || list.isEmpty()) {
            badges.add(Ui.label("No badges to earn here yet.", "Muted"));
            return;
        }
        for (BadgeDto b : list) {
            badges.add(Ui.panel(Ui.label(b.name(), "CardTitle"), Ui.text(
                    b.description() == null ? "" : b.description(), "CardMeta")));
        }
    }

    static String stats(ExperienceDto e) {
        int rating = Experiences.rating(e);
        return (rating < 0 ? "No votes yet" : rating + "% liked") + "  -  " + e.visits() + " visits  -  "
                + e.favourites() + " favourites  -  up to " + e.maxPlayers() + " players";
    }

    private static void refresh(Form f, com.codename1.ui.Label stats, ExperienceDto updated) {
        stats.setText(stats(updated));
        f.revalidate();
    }

    /// Downloads the published place and hands it to the player.
    static void play(final ExperienceDto e) {
        play(e, "");
    }

    /// Plays `e` on the server `serverId` names (a friend's), or any when empty.
    static void play(final ExperienceDto e, final String serverId) {
        if (e.publishedVersion() <= 0) {
            Ui.toast("This experience has not been published yet.");
            return;
        }
        Api.experiences().place(e.id(), Net.to(place -> {
            byte[] bytes = decode(place == null ? null : place.data());
            if (bytes == null) {
                Ui.toast("The place could not be read.");
                return;
            }
            Experiences.played(e.id());
            Telemetry.played(e.id());
            Friends.presence(e.title());
            Api.experiences().visit(e.id(), Net.to(v -> { }, (s, m) -> { }));
            PlaySession.start(e.id(), e.title(), bytes, e.maxPlayers(), serverId);
        }, Ui::failed));
    }

    static byte[] decode(String data) {
        if (data == null) {
            return null;
        }
        try {
            return Base64.decode(data.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException | RuntimeException err) {
            return null;
        }
    }
}

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
package com.codenameone.examples.dash.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.FontImage;
import com.codename1.ui.CN;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.dash.api.LevelStarsDto;
import com.codenameone.examples.dash.forge.ForgeHome;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.ViewMode;
import com.codenameone.examples.dash.rules.forge.Samples;
import com.codenameone.examples.dash.run.Levels;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The levels the game ships with, the stars earned on each, and the way to
/// everyone else's: the community browser in the track editor.
///
/// A run here is on this week's seed for the level, so it counts on the
/// weekly board as well as the all-time one; "Race the best" runs against
/// the week's leader as a ghost.
public final class LevelsForm {
    private LevelsForm() {
    }

    public static void show() {
        final Form f = Ui.form("Levels", "levels");
        Ui.back(f, Nav::home);
        final Container list = Ui.column();
        f.add(Ui.centeredColumn(list));
        fill(list, new HashMap<String, Integer>());
        f.add(Ui.primary("Community levels", "communityButton", e -> ForgeHome.show()));
        f.show();
        Api.forge().stars(Samples.OFFICIAL_ID, Net.to(stars -> Ui.later(() -> {
            fill(list, byLevel(stars));
            f.revalidate();
        }), (s, m) -> { }));
    }

    private static Map<String, Integer> byLevel(List<LevelStarsDto> stars) {
        Map<String, Integer> m = new HashMap<String, Integer>();
        for (LevelStarsDto s : stars) {
            m.put(s.levelId(), Integer.valueOf(s.stars()));
        }
        return m;
    }

    private static void fill(Container list, Map<String, Integer> stars) {
        list.removeAll();
        for (final LevelDef l : Content.levels()) {
            // A card per level: a lit view of its track, its name, what kind
            // of run it is and the stars won, a round play button, and the
            // two ways to compete underneath.
            Container card = new Container(new BorderLayout());
            card.setUIID("LevelCard");
            card.setName("level-" + l.id);
            int th = CN.convertToPixels(17);
            card.add(BorderLayout.WEST, new Label(Kit.thumb(l.theme, th * 3 / 5, th), "LevelThumb"));
            Container text = new Container(BoxLayout.y());
            text.add(new Label(l.name, "LevelName"));
            Label kind = new Label(describe(l), "TagChip");
            text.add(FlowLayout.encloseIn(kind));
            Integer s = stars.get(l.id);
            text.add(Kit.stars(s == null ? 0 : s.intValue(), 3, 4.5f));
            card.add(BorderLayout.CENTER, text);
            final Levels.Playable p = Levels.official(l.id);
            Button play = Ui.button("", "PlayRound", "play-" + l.id,
                    e -> RunForm.solo(p, week(l.id), "solo", null, LevelsForm::show));
            play.setMaterialIcon(FontImage.MATERIAL_PLAY_ARROW, 4.6f);
            card.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(play));
            Button ghost = Ui.button("Race the best", "CardAction", "ghost-" + l.id, e -> ghost(p));
            Ui.glyph(ghost, FontImage.MATERIAL_DIRECTIONS_RUN, 3.2f);
            Button scores = Ui.button("Scores", "CardAction", "scores-" + l.id,
                    e -> ScoresForm.show(l.id, LevelsForm::show));
            Ui.glyph(scores, FontImage.MATERIAL_LEADERBOARD, 3.2f);
            card.add(BorderLayout.SOUTH, GridLayout.encloseIn(2, ghost, scores));
            list.add(card);
        }
    }

    /// The week's leader on `p`, as a ghost on the same seed.
    static void ghost(final Levels.Playable p) {
        Api.game().ghost(p.level.id, "week", Net.to(g -> Ui.later(
                () -> RunForm.solo(p, week(p.level.id), "ghost", g, LevelsForm::show)),
                (s, m) -> Ui.later(() -> RunForm.solo(p, week(p.level.id), "ghost", null, LevelsForm::show))));
    }

    /// This week's seed for `levelId`, the one the weekly board counts.
    static long week(String levelId) {
        int day = (int) (System.currentTimeMillis() / 86400000L);
        return Content.weeklySeed(day / 7, levelId);
    }

    /// One line on what kind of level it is.
    public static String describe(LevelDef l) {
        boolean side = l.startView() == ViewMode.SIDE_2D;
        boolean turns = false;
        for (int i = 0; i < l.cues.size(); i++) {
            if (l.cues.get(i).mode != l.startView()) {
                turns = true;
            }
        }
        String view = side && turns ? "2D into 3D" : side ? "2D" : "3D";
        return view + (l.length <= 0 ? ", endless" : ", " + l.length / 1000 + " m");
    }
}

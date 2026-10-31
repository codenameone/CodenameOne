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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
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
            final Levels.Playable p = Levels.official(l.id);
            Integer s = stars.get(l.id);
            Container card = card(l, s == null ? 0 : s.intValue(),
                    e -> RunForm.solo(p, week(l.id), "solo", null, LevelsForm::show),
                    "Race the best", FontImage.MATERIAL_DIRECTIONS_RUN, e -> ghost(p),
                    e -> ScoresForm.show(l.id, LevelsForm::show));
            list.add(card);
        }
        list.add(cityCard());
        // In the list, so it scrolls up with the cards instead of sitting
        // under the last one half off the screen.
        list.add(Ui.primary("Community levels", "communityButton", e -> ForgeHome.show()));
    }

    /// A card per level: a lit view of its track, its name, what kind of run
    /// it is and the stars won, a round play button, and the two ways to
    /// compete underneath. The community levels of a pack use it too.
    public static Container card(LevelDef l, int stars, ActionListener<ActionEvent> play, String compete,
            char competeIcon, ActionListener<ActionEvent> onCompete, ActionListener<ActionEvent> onScores) {
        Container card = new Container(new BorderLayout());
        card.setUIID("LevelCard");
        card.setName("level-" + l.id);
        int th = CN.convertToPixels(17);
        card.add(BorderLayout.WEST, new Label(Kit.thumb(l.theme, th * 3 / 5, th), "LevelThumb"));
        Container text = new Container(BoxLayout.y());
        // The name wraps rather than truncates; the kind of run is a chip
        // and the length a line of its own, since "2D into 3D" and a
        // length side by side are wider than a narrow phone's card.
        text.add(Ui.text(l.name, "LevelName"));
        text.add(FlowLayout.encloseIn(new Label(view(l), "TagChip")));
        text.add(new Label(l.length <= 0 ? "Endless" : l.length / 1000 + " m", "LevelLength"));
        text.add(Kit.stars(stars, 3, 4.5f));
        card.add(BorderLayout.CENTER, text);
        Button go = Ui.button("", "PlayRound", "play-" + l.id, play);
        go.setMaterialIcon(FontImage.MATERIAL_PLAY_ARROW, 3.8f);
        card.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(go));
        Button versus = Ui.button(compete, "CardAction", "race-" + l.id, onCompete);
        Ui.glyph(versus, competeIcon, 3.2f);
        Button scores = Ui.button("Scores", "CardAction", "scores-" + l.id, onScores);
        Ui.glyph(scores, FontImage.MATERIAL_LEADERBOARD, 3.2f);
        card.add(BorderLayout.SOUTH, GridLayout.encloseIn(2, versus, scores));
        return card;
    }

    /// The way into city runs: any two points on a real street map.
    private static Container cityCard() {
        Container card = new Container(new BorderLayout());
        card.setUIID("LevelCard");
        card.setName("cityCard");
        Label icon = new Label("", "CityIcon");
        icon.setMaterialIcon(FontImage.MATERIAL_MAP, 9f);
        card.add(BorderLayout.WEST, icon);
        Container text = new Container(BoxLayout.y());
        text.add(Ui.text("Run your city", "LevelName"));
        text.add(Ui.text("Pick a start and a finish on a street map. The streets become the track, "
                + "every turn a corner.", "LevelLength"));
        card.add(BorderLayout.CENTER, text);
        Button go = Ui.button("", "PlayRound", "cityButton", e -> CityForm.show());
        go.setMaterialIcon(FontImage.MATERIAL_PLAY_ARROW, 3.8f);
        card.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(go));
        return card;
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
        return view(l) + (l.length <= 0 ? " - endless" : " - " + l.length / 1000 + " m");
    }

    /// How the level is seen: "2D", "3D", or "2D into 3D".
    static String view(LevelDef l) {
        boolean side = l.startView() == ViewMode.SIDE_2D;
        boolean turns = false;
        for (int i = 0; i < l.cues.size(); i++) {
            if (l.cues.get(i).mode != l.startView()) {
                turns = true;
            }
        }
        return side && turns ? "2D into 3D" : side ? "2D" : "3D";
    }
}

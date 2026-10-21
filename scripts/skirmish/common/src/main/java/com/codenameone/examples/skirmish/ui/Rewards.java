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
package com.codenameone.examples.skirmish.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.skirmish.api.AchievementDto;
import com.codenameone.examples.skirmish.api.ProgressDto;
import com.codenameone.examples.skirmish.api.RewardDto;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import java.util.ArrayList;
import java.util.List;

/// The player's progress as the client last saw it, and the celebrations when
/// it moves: a reward popup with a hero in it, a level-up, a new achievement.
///
/// The server decides every reward. This class only remembers the previous
/// [ProgressDto] so that, after a battle, it can tell what changed and say so.
// tag::skirmish-rewards[]
public final class Rewards {
    private static ProgressDto last;

    private Rewards() {
    }

    /// The progress last fetched, or null before the first fetch.
    public static ProgressDto progress() {
        return last;
    }

    /// Fetches the progress; `done` runs on the EDT either way.
    public static void refresh(final Runnable done) {
        if (Session.user() == null) {
            Ui.later(done);
            return;
        }
        Api.progress().progress(Net.to(p -> Ui.later(() -> {
            last = p;
            done.run();
        }), (s, m) -> Ui.later(done)));
    }

    /// Fetches the progress and celebrates what changed since the last fetch:
    /// levels gained and achievements unlocked by the battle just played.
    public static void afterBattle(final Runnable done) {
        final ProgressDto before = last;
        refresh(() -> {
            ProgressDto now = last;
            if (before == null || now == null) {
                done.run();
                return;
            }
            List<String> unlocked = new ArrayList<String>();
            for (AchievementDto a : now.achievements()) {
                if (a.unlocked() && !wasUnlocked(before, a.id())) {
                    unlocked.add(a.title());
                }
            }
            int gainedXp = now.xp() - before.xp();
            int chests = Math.max(0, now.chests() - before.chests());
            if (now.level() > before.level() || !unlocked.isEmpty() || chests > 0) {
                show(new RewardDto(now.level() > before.level() ? "Level up!" : "Battle rewards", 0, 0,
                        gainedXp, chests, now.level(), now.level() > before.level(), unlocked), done);
            } else {
                done.run();
            }
        });
    }

    /// A reward the server just granted, shown with the hero cheering.
    public static void show(RewardDto r, final Runnable done) {
        Telemetry.screen(r.levelUp() ? "levelUp" : "reward");
        final Dialog d = new Dialog(BoxLayout.y());
        d.setName("rewardDialog");
        d.setDialogUIID("RewardDialog");
        d.getContentPane().setUIID("RewardBody");
        d.getTitleComponent().setUIID("Container");
        // A fade, not the pulsate transition: pulsate scales a snapshot of the
        // dialog whose rounded corners were captured opaque, so it spun in on
        // a white box.
        d.setTransitionInAnimator(CommonTransitions.createFade(250));
        d.setTransitionOutAnimator(CommonTransitions.createFade(200));
        d.setDisposeWhenPointerOutOfBounds(false);
        // Wraps rather than truncating "Training co...".
        SpanLabel title = Ui.text(r.title(), r.levelUp() ? "LevelUpTitle" : "RewardTitle");
        title.setTextBlockAlign(Component.CENTER);
        d.add(title);
        String hero = CardArt.heroOf(Session.user() == null ? null : Session.user().username());
        Image art = CardArt.hero(hero, CN.convertToPixels(r.levelUp() ? 17 : 16));
        Label heroLabel = new Label(art, "RewardHero");
        d.add(FlowLayout.encloseCenter(heroLabel));
        if (r.levelUp()) {
            d.add(FlowLayout.encloseCenter(new Label("Level " + r.level(), "LevelBadgeBig")));
        }
        Container items = new Container(new FlowLayout(Component.CENTER));
        if (r.coins() > 0) {
            items.add(chip(FontImage.MATERIAL_MONETIZATION_ON, "+" + r.coins(), "RewardCoins"));
        }
        if (r.gems() > 0) {
            items.add(chip(FontImage.MATERIAL_DIAMOND, "+" + r.gems(), "RewardGems"));
        }
        if (r.xp() > 0) {
            items.add(chip(FontImage.MATERIAL_STAR, "+" + r.xp() + " XP", "RewardXp"));
        }
        if (r.chests() > 0) {
            items.add(chip(FontImage.MATERIAL_INVENTORY_2, "+" + r.chests() + " chest", "RewardChest"));
        }
        d.add(items);
        if (r.unlocked() != null) {
            for (String a : r.unlocked()) {
                Label l = new Label("Achievement: " + a, "RewardAchievement");
                l.setMaterialIcon(FontImage.MATERIAL_MILITARY_TECH);
                d.add(FlowLayout.encloseCenter(l));
            }
        }
        Button collect = Ui.wood("Collect", "collectReward", e -> {
            d.dispose();
            Session.refreshUser(() -> Ui.later(done), (s, m) -> Ui.later(done));
        });
        d.add(collect);
        // A scrolling body: showPacked clamped the height
        // first, and the last child -- the button -- absorbed the shortfall.
        d.getContentPane().setScrollableY(true);
        d.showPacked(BorderLayout.CENTER, true);
    }

    /// A short line under a celebration: how far the player is from the next
    /// level.
    static SpanLabel nextLevelHint(ProgressDto p) {
        return Ui.text((p.levelEnd() - p.xp()) + " XP to level " + (p.level() + 1), "Muted");
    }

    private static Label chip(char icon, String text, String uiid) {
        Label l = new Label(text, uiid);
        l.setMaterialIcon(icon);
        return l;
    }

    private static boolean wasUnlocked(ProgressDto p, String id) {
        for (AchievementDto a : p.achievements()) {
            if (a.id().equals(id)) {
                return a.unlocked();
            }
        }
        return false;
    }
}
// end::skirmish-rewards[]

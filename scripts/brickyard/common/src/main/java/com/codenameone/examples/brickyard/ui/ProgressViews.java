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

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.brickyard.api.ProgressDto;
import com.codenameone.examples.brickyard.api.QuestDto;
import com.codenameone.examples.brickyard.api.SeasonDto;
import com.codenameone.examples.brickyard.api.SeasonTierDto;
import com.codenameone.examples.brickyard.engage.Store;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;

import java.util.List;

/// The player's progression as cards: level and streak, a quest, the season
/// pass. Home shows the first two, the Me page all three.
public final class ProgressViews {
    /// Season XP per tier; the server's `Progression.XP_PER_TIER`.
    static final long XP_PER_TIER = 500;
    static final int YELLOW = 0xffc43c;
    static final int GREEN = 0x5ac86e;
    static final int PURPLE = 0x9a6cff;

    private ProgressViews() {
    }

    /// XP where level `n` starts: the inverse of the server's
    /// `1 + sqrt(xp / 100)`.
    static long levelStart(int level) {
        long n = Math.max(0, level - 1);
        return n * n * 100;
    }

    /// The level card: a badge with the number, XP to the next level, the
    /// login streak.
    public static Container hero(ProgressDto p, Component avatar) {
        long from = levelStart(p.level());
        long to = levelStart(p.level() + 1);
        Label badge = new Label(String.valueOf(p.level()), "LevelBadge");
        badge.setName("level");
        Label next = Ui.label((p.xp() - from) + " / " + (to - from) + " XP to level " + (p.level() + 1),
                "CardMeta");
        Label streak = Ui.label(p.streak() + " day streak", "Streak");
        FontImage.setMaterialIcon(streak, FontImage.MATERIAL_WHATSHOT, 3.2f);
        streak.setName("streak");
        Container text = BoxLayout.encloseY(Ui.label("Level " + p.level(), "CardTitle"),
                new Meter((double) (p.xp() - from) / Math.max(1, to - from), YELLOW), next, streak);
        Container row = new Container(new BorderLayout());
        row.setUIID("Panel");
        if (avatar != null) {
            row.add(BorderLayout.WEST, avatar);
        } else {
            row.add(BorderLayout.WEST, BorderLayout.centerCenter(badge));
        }
        row.add(BorderLayout.CENTER, text);
        row.setName("levelCard");
        return row;
    }

    /// One quest: its title, a bar, the reward, and Claim once it is done.
    public static Container quest(final QuestDto q, final Runnable after) {
        boolean done = q.progress() >= q.target();
        long shown = Math.min(q.progress(), q.target());
        String reward = "+" + q.rewardXp() + " XP" + (q.rewardBricks() > 0 ? "  +" + q.rewardBricks() + " Bricks" : "");
        Label cadence = Ui.label("WEEKLY".equals(q.cadence()) ? "WEEKLY" : "DAILY", "QuestCadence");
        Container text = BoxLayout.encloseY(BoxLayout.encloseX(cadence, Ui.label(q.title(), "CardTitle")),
                new Meter((double) shown / Math.max(1, q.target()), done ? GREEN : YELLOW),
                Ui.label(shown + " / " + q.target() + "    " + reward, "CardMeta"));
        Container row = new Container(new BorderLayout());
        row.setUIID("Panel");
        row.add(BorderLayout.CENTER, text);
        Component action;
        if (q.claimed() != 0) {
            Label l = Ui.label("Claimed", "Muted");
            FontImage.setMaterialIcon(l, FontImage.MATERIAL_CHECK_CIRCLE, 3.5f);
            action = l;
        } else if (done) {
            action = Ui.button("Claim", "ClaimButton", "claim" + q.id(),
                    e -> Api.progression().claimQuest(q.id(), Net.to(c -> {
                        Ui.toast("Quest claimed: " + reward);
                        Store.refresh();
                        after.run();
                    }, Ui::failed)));
        } else {
            action = Ui.label("", "Muted");
        }
        row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(action));
        row.setName("quest-" + q.id());
        return row;
    }

    /// The season pass: the season's name and days left, the bar to the next
    /// tier, a swipeable strip of tiers with a free and a Plus reward each,
    /// and the premium unlock.
    public static Container season(SeasonDto s, final ProgressDto p, List<SeasonTierDto> tiers,
            final Runnable after) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("SeasonPanel");
        box.setName("seasonPass");
        String name = s == null || s.name() == null ? "Season" : s.name();
        long left = s == null ? 0 : Math.max(0, (s.endsAt() - System.currentTimeMillis()) / 86400000L);
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Ui.label(name, "SeasonTitle"));
        if (left > 0) {
            head.add(BorderLayout.EAST, Ui.label(left + " days left", "CardMeta"));
        }
        box.add(head);
        long into = p.seasonXp() % XP_PER_TIER;
        boolean maxed = p.seasonTier() >= (tiers == null ? 0 : tiers.size());
        box.add(new Meter(maxed ? 1 : (double) into / XP_PER_TIER, PURPLE));
        box.add(Ui.label(maxed ? "Every tier reached" : "Tier " + p.seasonTier() + "  -  " + into + " / "
                + XP_PER_TIER + " XP to tier " + (p.seasonTier() + 1), "CardMeta"));
        Container strip = new Container(BoxLayout.x());
        strip.setScrollableX(true);
        strip.setScrollVisible(false);
        strip.setName("seasonTiers");
        if (tiers != null) {
            for (SeasonTierDto t : tiers) {
                strip.add(tier(t, p, after));
            }
        }
        box.add(strip);
        if (p.premium() == 0) {
            box.add(Ui.button("Unlock the Plus track - 400 Bricks", "PremiumButton", "buyPremium",
                    e -> Api.progression().buyPremium(Net.to(done -> {
                        Ui.toast("Plus track unlocked");
                        Store.refresh();
                        after.run();
                    }, Ui::failed))));
        }
        return box;
    }

    private static Container tier(final SeasonTierDto t, ProgressDto p, final Runnable after) {
        boolean reached = p.seasonTier() >= t.tier();
        Container c = new Container(BoxLayout.y());
        c.setUIID(reached ? "TierCardReached" : "TierCard");
        c.setName("tier" + t.tier());
        Label n = Ui.label("TIER " + t.tier(), reached ? "TierNumberReached" : "TierNumber");
        c.add(n);
        c.add(reward(t, "FREE", t.freeBricks(), reached, t.freeClaimed() != 0, true, after));
        c.add(reward(t, "PREMIUM", t.premiumBricks(), reached, t.premiumClaimed() != 0, p.premium() != 0, after));
        return c;
    }

    private static Component reward(final SeasonTierDto t, final String track, long bricks, boolean reached,
            boolean claimed, boolean open, final Runnable after) {
        boolean premium = "PREMIUM".equals(track);
        Container c = new Container(BoxLayout.y());
        c.setUIID(premium ? "TierRewardPlus" : "TierReward");
        Label amount = Ui.label(bricks + "", "TierAmount");
        FontImage.setMaterialIcon(amount, FontImage.MATERIAL_VIEW_MODULE, 3f);
        c.add(Ui.label(premium ? "PLUS" : "FREE", premium ? "TierTrackPlus" : "TierTrack"));
        c.add(amount);
        if (claimed) {
            Label l = Ui.label("Claimed", "TierState");
            FontImage.setMaterialIcon(l, FontImage.MATERIAL_CHECK, 2.6f);
            c.add(l);
        } else if (!open || !reached) {
            Label l = Ui.label(open ? "Locked" : "Plus", "TierState");
            FontImage.setMaterialIcon(l, FontImage.MATERIAL_LOCK, 2.6f);
            c.add(l);
        } else {
            Button b = Ui.button("Claim", "ClaimButton", "claimTier" + t.tier() + track,
                    e -> Api.progression().claimTier(t.tier(), track, Net.to(done -> {
                        Ui.toast("+" + bricks + " Bricks");
                        Store.refresh();
                        after.run();
                    }, Ui::failed)));
            c.add(b);
        }
        return c;
    }
}

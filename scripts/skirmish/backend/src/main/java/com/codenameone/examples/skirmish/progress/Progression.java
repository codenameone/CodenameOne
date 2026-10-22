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
package com.codenameone.examples.skirmish.progress;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.api.AchievementDto;
import com.codenameone.examples.skirmish.api.ProgressDto;
import com.codenameone.examples.skirmish.api.QuestDto;
import com.codenameone.examples.skirmish.api.RewardDto;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.domain.Progress;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.repository.ProgressRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Experience and levels, daily quests, chests and achievements: the reasons
/// to play one more battle. Every reward is computed and granted here; the
/// client only shows what it is told.
// tag::skirmish-progression[]
@Component
public class Progression {
    /// Chests wait in slots; a win with every slot full earns none.
    public static final int CHEST_SLOTS = 4;
    public static final int WIN_XP = 40;
    public static final int DRAW_XP = 20;
    public static final int LOSS_XP = 15;
    public static final int CROWN_XP = 5;
    public static final int TUTORIAL_XP = 100;
    public static final int TUTORIAL_COINS = 100;
    public static final int LEVEL_UP_COINS = 60;
    public static final int LEVEL_UP_GEMS = 5;

    private final ProgressRepository progress;
    private final ProfileRepository profiles;

    public Progression(ProgressRepository progress, ProfileRepository profiles) {
        this.progress = progress;
        this.profiles = profiles;
    }

    /// Counts a finished battle for one side. Called from inside the
    /// transaction that records the match, with the profile it is about to
    /// save, so the coins and gems this grants are saved with it.
    public RewardDto afterMatch(Profile p, boolean won, boolean draw, int crowns, int cards, int spells)
            throws IOException {
        Progress g = load(p.getUsername());
        int[] before = stats(g);
        g.setMatches(g.getMatches() + 1);
        g.setCards(g.getCards() + cards);
        g.setSpells(g.getSpells() + spells);
        g.setTowers(g.getTowers() + crowns);
        int chests = 0;
        if (won) {
            g.setWins(g.getWins() + 1);
            g.setStreak(g.getStreak() + 1);
            g.setBestStreak(Math.max(g.getBestStreak(), g.getStreak()));
            if (g.getChests() < CHEST_SLOTS) {
                g.setChests(g.getChests() + 1);
                chests = 1;
            }
        } else if (!draw) {
            g.setStreak(0);
        }
        int[] after = stats(g);
        Goals.Goal[] quests = Goals.questsFor(p.getUsername(), g.getQuestDay());
        for (int i = 0; i < 3; i++) {
            int gained = after[quests[i].stat] - before[quests[i].stat];
            setQuest(g, i, Math.min(quests[i].goal, quest(g, i) + gained));
        }
        int xp = (won ? WIN_XP : draw ? DRAW_XP : LOSS_XP) + CROWN_XP * crowns;
        Reward r = new Reward(won ? "Victory!" : draw ? "Draw" : "Defeat");
        r.chests = chests;
        r.xp(g, p, xp);
        unlock(g, p, r);
        progress.save(g);
        return r.dto(g);
    }

    @Transactional(rollbackFor = IOException.class)
    public ProgressDto view(String username) throws IOException {
        Profile p = profile(username);
        Progress g = load(username);
        progress.save(g);
        return dto(g, p);
    }

    @Transactional(rollbackFor = IOException.class)
    public RewardDto claim(String username, String questId) throws IOException {
        Profile p = profile(username);
        Progress g = load(username);
        Goals.Goal[] quests = Goals.questsFor(username, g.getQuestDay());
        int index = -1;
        for (int i = 0; i < 3; i++) {
            if (quests[i].id.equals(questId)) {
                index = i;
            }
        }
        if (index < 0) {
            throw new ResponseStatusException(404, "That is not one of today's quests");
        }
        if ((g.getClaimed() & (1 << index)) != 0) {
            throw new ResponseStatusException(409, "Already claimed");
        }
        Goals.Goal q = quests[index];
        if (quest(g, index) < q.goal) {
            throw new ResponseStatusException(409, "Not finished yet");
        }
        g.setClaimed(g.getClaimed() | (1 << index));
        Reward r = new Reward(q.title);
        r.coins(p, q.coins);
        r.xp(g, p, q.xp);
        unlock(g, p, r);
        progress.save(g);
        profiles.save(p);
        return r.dto(g);
    }

    /// Opens the oldest chest. What is inside grows with the level; every
    /// third chest also holds gems.
    @Transactional(rollbackFor = IOException.class)
    public RewardDto openChest(String username) throws IOException {
        Profile p = profile(username);
        Progress g = load(username);
        if (g.getChests() <= 0) {
            throw new ResponseStatusException(409, "No chest to open");
        }
        int level = Goals.levelOf(g.getXp());
        g.setChests(g.getChests() - 1);
        g.setChestsOpened(g.getChestsOpened() + 1);
        boolean rare = g.getChestsOpened() % 3 == 0;
        Reward r = new Reward(rare ? "Gold Chest" : "Wooden Chest");
        r.coins(p, 40 + 15 * level + (rare ? 40 : 0));
        if (rare) {
            r.gems(p, 4 + level);
        }
        r.xp(g, p, 20);
        unlock(g, p, r);
        progress.save(g);
        profiles.save(p);
        return r.dto(g);
    }

    /// The training battle pays once.
    @Transactional(rollbackFor = IOException.class)
    public RewardDto tutorialDone(String username) throws IOException {
        Profile p = profile(username);
        Progress g = load(username);
        Reward r = new Reward("Training complete");
        if (!g.isTutorialDone()) {
            g.setTutorialDone(true);
            r.coins(p, TUTORIAL_COINS);
            if (g.getChests() < CHEST_SLOTS) {
                g.setChests(g.getChests() + 1);
                r.chests = 1;
            }
            r.xp(g, p, TUTORIAL_XP);
            unlock(g, p, r);
            progress.save(g);
            profiles.save(p);
        }
        return r.dto(g);
    }

    /// Pays for stars won in a Forge mission. The caller has already verified
    /// the result and decided the amounts; this only books them, so experience
    /// still levels the player up and reaches achievements as it does anywhere.
    @Transactional(rollbackFor = IOException.class)
    public RewardDto missionReward(String username, String title, int coins, int xp) throws IOException {
        Profile p = profile(username);
        Progress g = load(username);
        Reward r = new Reward(title);
        if (coins > 0 || xp > 0) {
            r.coins(p, coins);
            r.xp(g, p, xp);
            unlock(g, p, r);
            progress.save(g);
            profiles.save(p);
        }
        return r.dto(g);
    }

    /// Gives a demo account some history -- a few levels, two chests, a
    /// finished quest waiting to be claimed -- so the development server's
    /// home screen shows what a regular player sees, and the tutorial does not
    /// start on every sign-in.
    @Transactional(rollbackFor = IOException.class)
    public void seedDemo(String username) throws IOException {
        Progress g = load(username);
        if (g.getMatches() > 0) {
            return;
        }
        g.setTutorialDone(true);
        g.setXp(420);
        g.setChests(2);
        g.setChestsOpened(1);
        g.setMatches(7);
        g.setWins(4);
        g.setCards(62);
        g.setSpells(9);
        g.setTowers(6);
        g.setStreak(2);
        g.setBestStreak(2);
        Goals.Goal[] today = Goals.questsFor(username, g.getQuestDay());
        g.setQuest0(today[0].goal);
        g.setQuest1(today[1].goal / 2);
        // Unlocked for what the counters say, without paying for it: the
        // profile here is a throwaway, never saved.
        Profile scratch = new Profile();
        unlock(g, scratch, new Reward(""));
        g.setXp(420);
        progress.save(g);
    }

    private Profile profile(String username) throws IOException {
        Profile p = profiles.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such player");
        }
        return p;
    }

    /// The player's row, created on first use, with today's quests reset when
    /// the day has turned.
    private Progress load(String username) throws IOException {
        Progress g = progress.findById(username);
        long today = Clock.dayOf(Clock.now());
        if (g == null) {
            g = new Progress();
            g.setUsername(username);
            g.setQuestDay(today);
            progress.insert(g);
        }
        if (g.getQuestDay() != today) {
            g.setQuestDay(today);
            g.setQuest0(0);
            g.setQuest1(0);
            g.setQuest2(0);
            g.setClaimed(0);
        }
        return g;
    }

    private static int[] stats(Progress g) {
        return new int[] {g.getWins(), g.getMatches(), g.getCards(), g.getSpells(), g.getTowers(),
            g.getBestStreak(), g.isTutorialDone() ? 1 : 0, Goals.levelOf(g.getXp())};
    }

    private static int quest(Progress g, int i) {
        return i == 0 ? g.getQuest0() : i == 1 ? g.getQuest1() : g.getQuest2();
    }

    private static void setQuest(Progress g, int i, int value) {
        if (i == 0) {
            g.setQuest0(value);
        } else if (i == 1) {
            g.setQuest1(value);
        } else {
            g.setQuest2(value);
        }
    }

    private static boolean unlocked(Progress g, String id) {
        return ("," + g.getAchievements() + ",").indexOf("," + id + ",") >= 0;
    }

    /// Unlocks every achievement now reached. Unlocking pays experience, which
    /// can level up, which can reach the level achievement: hence the loop.
    private static void unlock(Progress g, Profile p, Reward r) {
        boolean more = true;
        while (more) {
            more = false;
            int[] s = stats(g);
            for (Goals.Goal a : Goals.ACHIEVEMENTS) {
                if (!unlocked(g, a.id) && s[a.stat] >= a.goal) {
                    g.setAchievements(g.getAchievements().isEmpty() ? a.id : g.getAchievements() + "," + a.id);
                    r.unlocked.add(a.title);
                    r.gems(p, a.gems);
                    r.xp(g, p, a.xp);
                    more = true;
                }
            }
        }
    }

    private static ProgressDto dto(Progress g, Profile p) {
        int level = Goals.levelOf(g.getXp());
        Goals.Goal[] today = Goals.questsFor(g.getUsername(), g.getQuestDay());
        List<QuestDto> quests = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Goals.Goal q = today[i];
            quests.add(new QuestDto(q.id, q.title, quest(g, i), q.goal, q.coins, q.xp,
                    (g.getClaimed() & (1 << i)) != 0));
        }
        int[] s = stats(g);
        List<AchievementDto> achievements = new ArrayList<>();
        for (Goals.Goal a : Goals.ACHIEVEMENTS) {
            achievements.add(new AchievementDto(a.id, a.title, a.detail, Math.min(a.goal, s[a.stat]), a.goal,
                    unlocked(g, a.id)));
        }
        return new ProgressDto(level, g.getXp(), Goals.levelStart(level), Goals.levelStart(level + 1),
                g.isTutorialDone(), g.getChests(), p.getCoins(), p.getGems(), g.getStreak(), g.getWins(),
                quests, achievements);
    }

    /// A reward being put together; granting through it keeps the popup and
    /// the balances in step.
    private static final class Reward {
        final String title;
        final List<String> unlocked = new ArrayList<>();
        int coins;
        int gems;
        int xp;
        int chests;
        boolean levelUp;

        Reward(String title) {
            this.title = title;
        }

        void coins(Profile p, int n) {
            p.setCoins(p.getCoins() + n);
            coins += n;
        }

        void gems(Profile p, int n) {
            p.setGems(p.getGems() + n);
            gems += n;
        }

        /// Adds experience; each level gained pays coins, gems and a chest.
        void xp(Progress g, Profile p, int n) {
            int from = Goals.levelOf(g.getXp());
            g.setXp(g.getXp() + n);
            xp += n;
            int to = Goals.levelOf(g.getXp());
            for (int l = from; l < to; l++) {
                levelUp = true;
                coins(p, LEVEL_UP_COINS);
                gems(p, LEVEL_UP_GEMS);
                if (g.getChests() < CHEST_SLOTS) {
                    g.setChests(g.getChests() + 1);
                    chests++;
                }
            }
        }

        RewardDto dto(Progress g) {
            return new RewardDto(title, coins, gems, xp, chests, Goals.levelOf(g.getXp()), levelUp, unlocked);
        }
    }
}
// end::skirmish-progression[]

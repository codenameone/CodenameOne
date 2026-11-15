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
package com.codenameone.examples.brickyard.progress;

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.ProgressDto;
import com.codenameone.examples.brickyard.api.ProgressEventDto;
import com.codenameone.examples.brickyard.api.QuestDto;
import com.codenameone.examples.brickyard.api.SeasonTierDto;
import com.codenameone.examples.brickyard.domain.Progress;
import com.codenameone.examples.brickyard.domain.Quest;
import com.codenameone.examples.brickyard.domain.QuestProgress;
import com.codenameone.examples.brickyard.domain.SeasonClaim;
import com.codenameone.examples.brickyard.economy.Wallet;
import com.codenameone.examples.brickyard.moderation.Audit;
import com.codenameone.examples.brickyard.repository.AccountRepository;
import com.codenameone.examples.brickyard.repository.ProgressRepository;
import com.codenameone.examples.brickyard.repository.QuestProgressRepository;
import com.codenameone.examples.brickyard.repository.QuestRepository;
import com.codenameone.examples.brickyard.repository.SeasonClaimRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// XP and levels, the daily login streak, server-defined quests the engine
/// advances, and the season pass with a free and a premium track.
@Component
public class Progression {
    public static final int SEASON_TIERS = 10;
    public static final long XP_PER_TIER = 500;
    public static final long PREMIUM_PRICE = 400;
    /// XP an engine event is worth per unit.
    public static final long XP_PER_UNIT = 10;
    /// The most units one engine event may claim.
    public static final long MAX_UNITS = 100;
    public static final long WEEK = 7 * Clock.DAY;

    private final ProgressRepository progress;
    private final QuestRepository quests;
    private final QuestProgressRepository questProgress;
    private final SeasonClaimRepository claims;
    private final AccountRepository accounts;
    private final Wallet wallet;
    private final Audit audit;

    public Progression(ProgressRepository progress, QuestRepository quests,
            QuestProgressRepository questProgress, SeasonClaimRepository claims,
            AccountRepository accounts, Wallet wallet, Audit audit) {
        this.progress = progress;
        this.quests = quests;
        this.questProgress = questProgress;
        this.claims = claims;
        this.accounts = accounts;
        this.wallet = wallet;
        this.audit = audit;
    }

    /// Level 1 at no XP; each level needs more than the last.
    public static int level(long xp) {
        return 1 + (int) Math.sqrt(xp / 100.0);
    }

    public static int tier(long seasonXp) {
        return (int) Math.min(SEASON_TIERS, seasonXp / XP_PER_TIER);
    }

    private Progress row(String username) {
        Progress p = progress.findById(username);
        if (p == null) {
            if (username == null || accounts.findById(username) == null) {
                throw new ResponseStatusException(404, "No such account");
            }
            p = new Progress();
            p.setId(username);
            progress.insert(p);
        }
        return p;
    }

    @Transactional(rollbackFor = IOException.class)
    public ProgressDto describe(String username) {
        return toDto(row(username), 0);
    }

    /// The first sign-in of a day extends the streak (or restarts it after a
    /// missed day) and pays 5 Bricks per streak day, up to a week's worth.
    @Transactional(rollbackFor = IOException.class)
    public ProgressDto dailyLogin(String username) throws IOException {
        Progress p = row(username);
        long today = Clock.dayOf(Clock.now());
        if (p.getLastLoginDay() == today) {
            return toDto(p, 0);
        }
        p.setStreak(p.getLastLoginDay() == today - Clock.DAY ? p.getStreak() + 1 : 1);
        p.setLastLoginDay(today);
        p.setXp(p.getXp() + 50);
        progress.save(p);
        long bricks = 5L * Math.min(7, p.getStreak());
        wallet.post(username, bricks, "LOGIN_STREAK", "day " + p.getStreak());
        return toDto(p, bricks);
    }

    static long periodStart(String cadence, long now) {
        return "WEEKLY".equals(cadence) ? now - now % WEEK : Clock.dayOf(now);
    }

    @Transactional(rollbackFor = IOException.class)
    public QuestDto define(QuestDto q) throws IOException {
        if (q == null || q.target() < 1 || q.rewardXp() < 0 || q.rewardBricks() < 0 || q.rewardBricks() > 1000) {
            throw new ResponseStatusException(400, "A quest needs a target and modest rewards");
        }
        String cadence = "WEEKLY".equalsIgnoreCase(q.cadence()) ? "WEEKLY"
                : "DAILY".equalsIgnoreCase(q.cadence()) ? "DAILY" : null;
        if (cadence == null) {
            throw new ResponseStatusException(400, "A quest is DAILY or WEEKLY");
        }
        Quest e = new Quest();
        e.setId(Ids.next());
        e.setTitle(Text.required(q.title(), 80, "a title").trim());
        e.setCadence(cadence);
        e.setEventName(Text.required(q.eventName(), 64, "the event it counts").trim());
        e.setTarget(q.target());
        e.setRewardXp(q.rewardXp());
        e.setRewardBricks(q.rewardBricks());
        e.setActive(1);
        quests.insert(e);
        audit.record("QUEST_DEFINE", e.getId(), e.getTitle());
        return quest(e, null);
    }

    private QuestProgress progressOn(String username, Quest q, boolean create) {
        long start = periodStart(q.getCadence(), Clock.now());
        String id = username + "|" + q.getId() + "|" + start;
        QuestProgress p = questProgress.findById(id);
        if (p == null && create) {
            p = new QuestProgress();
            p.setId(id);
            p.setUsername(username);
            p.setQuestId(q.getId());
            p.setPeriodStart(start);
            questProgress.insert(p);
        }
        return p;
    }

    @Transactional(readOnly = true)
    public List<QuestDto> quests(String username) {
        List<QuestDto> out = new ArrayList<QuestDto>();
        for (Quest q : quests.active(1)) {
            out.add(quest(q, progressOn(username, q, false)));
        }
        return out;
    }

    /// An engine reports what a player did: XP and season XP for every
    /// event, and progress on each active quest that counts it.
    @Transactional(rollbackFor = IOException.class)
    public ProgressDto report(ProgressEventDto event) throws IOException {
        Access.requireEngine();
        if (event == null || event.eventName() == null || event.amount() < 1 || event.amount() > MAX_UNITS) {
            throw new ResponseStatusException(400, "An event needs a name and 1 to " + MAX_UNITS + " units");
        }
        Progress p = row(event.username());
        p.setXp(p.getXp() + event.amount() * XP_PER_UNIT);
        p.setSeasonXp(p.getSeasonXp() + event.amount() * XP_PER_UNIT);
        progress.save(p);
        for (Quest q : quests.active(1)) {
            if (q.getEventName().equals(event.eventName())) {
                QuestProgress qp = progressOn(event.username(), q, true);
                qp.setProgress(Math.min(q.getTarget(), qp.getProgress() + event.amount()));
                questProgress.save(qp);
            }
        }
        return toDto(p, 0);
    }

    @Transactional(rollbackFor = IOException.class)
    public QuestDto claimQuest(String username, String questId) throws IOException {
        Quest q = questId == null ? null : quests.findById(questId);
        if (q == null || q.getActive() == 0) {
            throw new ResponseStatusException(404, "No such quest");
        }
        QuestProgress qp = progressOn(username, q, false);
        if (qp == null || qp.getProgress() < q.getTarget()) {
            throw new ResponseStatusException(409, "That quest is not complete");
        }
        if (qp.getClaimed() != 0) {
            throw new ResponseStatusException(409, "That reward was already claimed");
        }
        qp.setClaimed(1);
        questProgress.save(qp);
        Progress p = row(username);
        p.setXp(p.getXp() + q.getRewardXp());
        p.setSeasonXp(p.getSeasonXp() + q.getRewardXp());
        progress.save(p);
        if (q.getRewardBricks() > 0) {
            wallet.post(username, q.getRewardBricks(), "QUEST", qp.getId());
        }
        return quest(q, qp);
    }

    public static long freeBricks(int tier) {
        return 10L * tier;
    }

    public static long premiumBricks(int tier) {
        return 25L * tier;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<SeasonTierDto> season(String username) {
        row(username);
        List<SeasonTierDto> out = new ArrayList<SeasonTierDto>();
        for (int t = 1; t <= SEASON_TIERS; t++) {
            out.add(tierDto(username, t));
        }
        return out;
    }

    private SeasonTierDto tierDto(String username, int t) {
        return new SeasonTierDto(t, t * XP_PER_TIER, freeBricks(t), premiumBricks(t),
                claims.findById(username + "|" + t + "|FREE") == null ? 0 : 1,
                claims.findById(username + "|" + t + "|PREMIUM") == null ? 0 : 1);
    }

    @Transactional(rollbackFor = IOException.class)
    public ProgressDto buyPremium(String username) throws IOException {
        Progress p = row(username);
        if (p.getPremium() != 0) {
            throw new ResponseStatusException(409, "You already have the premium pass");
        }
        wallet.post(username, -PREMIUM_PRICE, "SEASON_PREMIUM", "season");
        p.setPremium(1);
        progress.save(p);
        return toDto(p, 0);
    }

    @Transactional(rollbackFor = IOException.class)
    public SeasonTierDto claimTier(String username, int tier, String track) throws IOException {
        Progress p = row(username);
        boolean premium = "PREMIUM".equalsIgnoreCase(track);
        if (!premium && !"FREE".equalsIgnoreCase(track)) {
            throw new ResponseStatusException(400, "The track is FREE or PREMIUM");
        }
        if (tier < 1 || tier > SEASON_TIERS) {
            throw new ResponseStatusException(404, "No such tier");
        }
        if (tier(p.getSeasonXp()) < tier) {
            throw new ResponseStatusException(409, "That tier is not reached yet");
        }
        if (premium && p.getPremium() == 0) {
            throw new ResponseStatusException(402, "The premium track needs the premium pass");
        }
        String id = username + "|" + tier + "|" + (premium ? "PREMIUM" : "FREE");
        if (claims.findById(id) != null) {
            throw new ResponseStatusException(409, "That reward was already claimed");
        }
        SeasonClaim c = new SeasonClaim();
        c.setId(id);
        c.setUsername(username);
        c.setTier(tier);
        c.setTrack(premium ? "PREMIUM" : "FREE");
        c.setCreatedAt(Clock.now());
        claims.insert(c);
        wallet.post(username, premium ? premiumBricks(tier) : freeBricks(tier), "SEASON_REWARD", id);
        return tierDto(username, tier);
    }

    private static QuestDto quest(Quest q, QuestProgress p) {
        return new QuestDto(q.getId(), q.getTitle(), q.getCadence(), q.getEventName(), q.getTarget(),
                p == null ? 0 : p.getProgress(), q.getRewardXp(), q.getRewardBricks(),
                p == null ? 0 : p.getClaimed());
    }

    private static ProgressDto toDto(Progress p, long bricks) {
        return new ProgressDto(p.getId(), p.getXp(), level(p.getXp()), p.getStreak(), p.getSeasonXp(),
                tier(p.getSeasonXp()), p.getPremium(), bricks);
    }
}

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
package com.codenameone.examples.skirmish.social;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.api.FriendDto;
import com.codenameone.examples.skirmish.api.InviteClaimDto;
import com.codenameone.examples.skirmish.api.InviteDto;
import com.codenameone.examples.skirmish.api.RewardDto;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.domain.Referral;
import com.codenameone.examples.skirmish.domain.ReferralCode;
import com.codenameone.examples.skirmish.progress.Progression;
import com.codenameone.examples.skirmish.push.GamePushes;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.repository.ReferralCodeRepository;
import com.codenameone.examples.skirmish.repository.ReferralRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Invite a friend, from the server's side.
///
/// The link is not made here. The app mints it with Codename One's
/// `Invites`, which carries the player's code in the link's payload, follows
/// it through the store to the friend's first launch, and reports the
/// campaign and the referrer to Codename One analytics with every event the
/// friend sends after. What the server adds is the economy: whose code it was,
/// so that the friend gets a welcome gift on joining and the inviter is paid
/// once the friend has played a battle -- not for an install that never
/// plays.
///
/// The rules are what keep it from being farmed: only an account younger
/// than [#CLAIM_DAYS] can be claimed for, once, never by its own inviter or by
/// someone it invited, and an inviter is paid for at most [#MAX_REWARDED]
/// friends.
@Component
public class Referrals {
    public static final String JOINED = "joined";
    public static final String PLAYED = "played";
    public static final int FRIEND_COINS = 100;
    public static final int INVITER_COINS = 200;
    public static final int INVITER_XP = 20;
    public static final int MAX_REWARDED = 10;
    public static final int CLAIM_DAYS = 7;
    private static final int CODE_LENGTH = 10;
    private static final int PAGE = 100;

    private final ReferralCodeRepository codes;
    private final ReferralRepository referrals;
    private final ProfileRepository profiles;
    private final Progression progression;
    private final GamePushes pushes;

    public Referrals(ReferralCodeRepository codes, ReferralRepository referrals, ProfileRepository profiles,
            Progression progression, GamePushes pushes) {
        this.codes = codes;
        this.referrals = referrals;
        this.profiles = profiles;
        this.progression = progression;
        this.pushes = pushes;
    }

    /// The player's code, made the first time they ask for it.
    @Transactional(rollbackFor = IOException.class)
    public InviteDto invite(String username) throws IOException {
        List<ReferralCode> mine = codes.findByUsername(username);
        String code;
        if (mine.isEmpty()) {
            code = Ids.next().substring(0, CODE_LENGTH);
            while (codes.findById(code) != null) {
                code = Ids.next().substring(0, CODE_LENGTH);
            }
            ReferralCode c = new ReferralCode();
            c.setCode(code);
            c.setUsername(username);
            c.setCreatedAt(Clock.now());
            codes.insert(c);
        } else {
            code = mine.get(0).getCode();
        }
        return new InviteDto(code, (int) referrals.countByInviter(username),
                (int) referrals.countByInviterAndRewardedTrue(username), MAX_REWARDED, INVITER_COINS, FRIEND_COINS);
    }

    // tag::skirmish-referrals[]
    /// The friend's side: the app found an invite and the player is signed in.
    @Transactional(rollbackFor = IOException.class)
    public RewardDto claim(String username, InviteClaimDto claim) throws IOException {
        String code = claim == null || claim.code() == null ? "" : claim.code().trim();
        ReferralCode owner = code.length() == 0 ? null : codes.findById(code);
        if (owner == null) {
            throw new ResponseStatusException(404, "That invite is not known");
        }
        String inviter = owner.getUsername();
        if (inviter.equals(username)) {
            throw new ResponseStatusException(400, "That is your own invite");
        }
        if (referrals.findById(username) != null) {
            throw new ResponseStatusException(409, "An invite was already counted for you");
        }
        Referral back = referrals.findById(inviter);
        if (back != null && back.getInviter().equals(username)) {
            throw new ResponseStatusException(400, "You invited that player");
        }
        Profile p = profiles.findById(username);
        if (p == null || p.getCreatedAt() < Clock.now() - CLAIM_DAYS * Clock.DAY) {
            throw new ResponseStatusException(400, "Invites count for new players only");
        }
        Referral r = new Referral();
        r.setInvitee(username);
        r.setInviter(inviter);
        r.setCode(code);
        r.setCampaign(clip(claim.campaign(), 40));
        r.setChannel(clip(claim.channel(), 40));
        r.setMatchType(clip(claim.match(), 20));
        r.setState(JOINED);
        r.setJoinedAt(Clock.now());
        referrals.insert(r);
        Profile from = profiles.findById(inviter);
        pushes.friendJoined(inviter, p.getDisplayName());
        return progression.missionReward(username,
                "A gift from " + (from == null ? "your friend" : from.getDisplayName()), FRIEND_COINS, 0);
    }
    // end::skirmish-referrals[]

    /// Called after every battle the player finishes. The first one makes an
    /// invited player count, and pays whoever invited them.
    @Transactional(rollbackFor = IOException.class)
    public void played(String username) throws IOException {
        Referral r = referrals.findById(username);
        if (r == null || !JOINED.equals(r.getState())) {
            return;
        }
        r.setState(PLAYED);
        r.setPlayedAt(Clock.now());
        boolean pay = referrals.countByInviterAndRewardedTrue(r.getInviter()) < MAX_REWARDED;
        r.setRewarded(pay);
        referrals.save(r);
        if (pay) {
            Profile friend = profiles.findById(username);
            String name = friend == null ? "Your friend" : friend.getDisplayName();
            progression.missionReward(r.getInviter(), name + " played", INVITER_COINS, INVITER_XP);
            pushes.friendPlayed(r.getInviter(), name, INVITER_COINS);
        }
    }

    @Transactional(readOnly = true)
    public List<FriendDto> friends(String username) throws IOException {
        List<FriendDto> out = new ArrayList<>();
        for (Referral r : referrals.of(username, new PageRequest(0, PAGE))) {
            Profile p = profiles.findById(r.getInvitee());
            if (p != null) {
                out.add(new FriendDto(p.getDisplayName(), p.getTrophies(), r.getState(), r.isRewarded(),
                        r.getJoinedAt()));
            }
        }
        return out;
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}

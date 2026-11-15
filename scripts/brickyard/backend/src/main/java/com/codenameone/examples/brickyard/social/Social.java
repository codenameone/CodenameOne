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
package com.codenameone.examples.brickyard.social;

import com.codenameone.examples.brickyard.account.Limits;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.ChatDto;
import com.codenameone.examples.brickyard.api.FollowDto;
import com.codenameone.examples.brickyard.api.FriendDto;
import com.codenameone.examples.brickyard.api.FriendRequestDto;
import com.codenameone.examples.brickyard.api.InviteDto;
import com.codenameone.examples.brickyard.api.PartyDto;
import com.codenameone.examples.brickyard.api.PresenceDto;
import com.codenameone.examples.brickyard.api.WalletDto;
import com.codenameone.examples.brickyard.domain.Account;
import com.codenameone.examples.brickyard.domain.ChatMessage;
import com.codenameone.examples.brickyard.domain.Follow;
import com.codenameone.examples.brickyard.domain.FraudFlag;
import com.codenameone.examples.brickyard.domain.FriendRequest;
import com.codenameone.examples.brickyard.domain.Invite;
import com.codenameone.examples.brickyard.domain.Party;
import com.codenameone.examples.brickyard.domain.PartyMember;
import com.codenameone.examples.brickyard.domain.Presence;
import com.codenameone.examples.brickyard.domain.Referral;
import com.codenameone.examples.brickyard.domain.UserBlock;
import com.codenameone.examples.brickyard.economy.Wallet;
import com.codenameone.examples.brickyard.moderation.Sanctions;
import com.codenameone.examples.brickyard.repository.ChatMessageRepository;
import com.codenameone.examples.brickyard.repository.FollowRepository;
import com.codenameone.examples.brickyard.repository.FraudFlagRepository;
import com.codenameone.examples.brickyard.repository.FriendRequestRepository;
import com.codenameone.examples.brickyard.repository.InviteRepository;
import com.codenameone.examples.brickyard.repository.PartyMemberRepository;
import com.codenameone.examples.brickyard.repository.PartyRepository;
import com.codenameone.examples.brickyard.repository.PresenceRepository;
import com.codenameone.examples.brickyard.repository.ReferralRepository;
import com.codenameone.examples.brickyard.repository.UserBlockRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Friend requests, follows, presence, blocks, parties, chat, DMs and referrals.
@Component
public class Social {
    public static final int PARTY_SIZE = 6;
    /// Presence older than this reads as offline: a client refreshes it every minute.
    public static final long PRESENCE_TTL = 2 * Clock.MINUTE;
    public static final long CHAT_WINDOW = 60 * Clock.MINUTE;
    /// At most this many chat lines per sender in [#CHAT_BURST_MILLIS].
    public static final int CHAT_BURST = 10;
    public static final long CHAT_BURST_MILLIS = 10000;
    public static final long REFERRAL_BRICKS = 50;
    /// At most this many redemptions of one inviter's codes per day.
    public static final int REFERRALS_PER_DAY = 5;
    /// Only an account this young may redeem an invite.
    public static final long REFERRAL_WINDOW = 7 * Clock.DAY;

    private final Accounts accounts;
    private final Sanctions sanctions;
    private final Wallet wallet;
    private final FriendRequestRepository requests;
    private final FollowRepository follows;
    private final PresenceRepository presence;
    private final UserBlockRepository blocks;
    private final PartyRepository parties;
    private final PartyMemberRepository members;
    private final ChatMessageRepository chat;
    private final InviteRepository invites;
    private final ReferralRepository referrals;
    private final FraudFlagRepository fraud;
    /// Chat on or off for the account, checked on every line sent.
    private final Limits limits;

    public Social(Accounts accounts, Sanctions sanctions, Wallet wallet, FriendRequestRepository requests,
            FollowRepository follows, PresenceRepository presence, UserBlockRepository blocks,
            PartyRepository parties, PartyMemberRepository members, ChatMessageRepository chat,
            InviteRepository invites, ReferralRepository referrals, FraudFlagRepository fraud, Limits limits) {
        this.limits = limits;
        this.accounts = accounts;
        this.sanctions = sanctions;
        this.wallet = wallet;
        this.requests = requests;
        this.follows = follows;
        this.presence = presence;
        this.blocks = blocks;
        this.parties = parties;
        this.members = members;
        this.chat = chat;
        this.invites = invites;
        this.referrals = referrals;
        this.fraud = fraud;
    }

    public List<FriendRequestDto> incoming(String username) {
        List<FriendRequestDto> out = new ArrayList<FriendRequestDto>();
        for (FriendRequest r : requests.incoming(username, Accounts.PENDING)) {
            out.add(new FriendRequestDto(r.getFromUser(), r.getToUser(), r.getStatus()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public FriendDto accept(String username, String from) {
        FriendRequest r = pending(from, username);
        r.setStatus("ACCEPTED");
        requests.save(r);
        accounts.befriend(username, from);
        return new FriendDto(from);
    }

    @Transactional(rollbackFor = IOException.class)
    public FriendRequestDto decline(String username, String from) {
        FriendRequest r = pending(from, username);
        r.setStatus("DECLINED");
        requests.save(r);
        return new FriendRequestDto(from, username, r.getStatus());
    }

    private FriendRequest pending(String from, String to) {
        FriendRequest r = from == null ? null : requests.findById(from + "|" + to);
        if (r == null || !Accounts.PENDING.equals(r.getStatus())) {
            throw new ResponseStatusException(404, "No such friend request");
        }
        return r;
    }

    @Transactional(rollbackFor = IOException.class)
    public FollowDto follow(String username, String creator, boolean on) {
        accounts.account(creator);
        if (creator.equals(username)) {
            throw new ResponseStatusException(400, "You cannot follow yourself");
        }
        String id = username + "|" + creator;
        Follow f = follows.findById(id);
        if (on && f == null) {
            f = new Follow();
            f.setId(id);
            f.setUsername(username);
            f.setCreator(creator);
            follows.insert(f);
        } else if (!on && f != null) {
            follows.delete(f);
        }
        return new FollowDto(creator, follows.followers(creator).size());
    }

    public List<FollowDto> following(String username) {
        List<FollowDto> out = new ArrayList<FollowDto>();
        for (Follow f : follows.following(username)) {
            out.add(new FollowDto(f.getCreator(), follows.followers(f.getCreator()).size()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public PresenceDto setPresence(String username, PresenceDto p) {
        String status = p == null ? "ONLINE" : p.status();
        if (!"ONLINE".equals(status) && !"IN_GAME".equals(status) && !"OFFLINE".equals(status)) {
            throw new ResponseStatusException(400, "Presence is ONLINE, IN_GAME or OFFLINE");
        }
        Presence row = presence.findById(username);
        boolean fresh = row == null;
        if (fresh) {
            row = new Presence();
            row.setId(username);
        }
        boolean inGame = "IN_GAME".equals(status);
        row.setStatus(status);
        row.setExperienceId(inGame ? clip(p.experienceId(), 32) : "");
        row.setServerId(inGame ? clip(p.serverId(), 64) : "");
        row.setUpdatedAt(Clock.now());
        if (fresh) {
            presence.insert(row);
        } else {
            presence.save(row);
        }
        return toDto(row);
    }

    /// `username` left server `serverId`: back to ONLINE, unless their
    /// presence already points at another server (they moved on).
    @Transactional(rollbackFor = IOException.class)
    public void leftServer(String username, String serverId) {
        Presence row = presence.findById(username);
        if (row != null && "IN_GAME".equals(row.getStatus()) && serverId.equals(row.getServerId())) {
            row.setStatus("ONLINE");
            row.setExperienceId("");
            row.setServerId("");
            row.setUpdatedAt(Clock.now());
            presence.save(row);
        }
    }

    private static String clip(String s, int max) {
        String v = s == null ? "" : s;
        return v.length() > max ? v.substring(0, max) : v;
    }

    private PresenceDto presenceOf(String username) {
        Presence p = presence.findById(username);
        if (p == null || p.getUpdatedAt() < Clock.now() - PRESENCE_TTL) {
            return new PresenceDto(username, "OFFLINE", "", "", p == null ? 0 : p.getUpdatedAt());
        }
        return toDto(p);
    }

    public List<PresenceDto> friendPresence(String username) {
        List<PresenceDto> out = new ArrayList<PresenceDto>();
        for (String f : accounts.friendsOf(username)) {
            out.add(presenceOf(f));
        }
        return out;
    }

    /// The server a friend is playing in, which the client hands the engine
    /// as its teleport target. Only a friend who is in a game, now, has one.
    public PresenceDto joinTarget(String username, String friend) {
        sanctions.requireNotBanned(username);
        if (!accounts.friendsOf(username).contains(friend) || accounts.blockedEitherWay(username, friend)) {
            throw new ResponseStatusException(404, "That friend cannot be joined");
        }
        PresenceDto p = presenceOf(friend);
        if (!"IN_GAME".equals(p.status())) {
            throw new ResponseStatusException(409, "That friend is not in a game");
        }
        return p;
    }

    /// Blocking ends the friendship, any pending requests and a shared party's
    /// join path; the blocked account is not told.
    @Transactional(rollbackFor = IOException.class)
    public FriendDto block(String username, String other) {
        accounts.account(other);
        if (other.equals(username)) {
            throw new ResponseStatusException(400, "You cannot block yourself");
        }
        String id = username + "|" + other;
        if (blocks.findById(id) == null) {
            UserBlock b = new UserBlock();
            b.setId(id);
            b.setUsername(username);
            b.setBlocked(other);
            blocks.insert(b);
        }
        accounts.unfriend(username, other);
        for (String rid : new String[] {username + "|" + other, other + "|" + username}) {
            FriendRequest r = requests.findById(rid);
            if (r != null && Accounts.PENDING.equals(r.getStatus())) {
                r.setStatus("DECLINED");
                requests.save(r);
            }
        }
        return new FriendDto(other);
    }

    public List<FriendDto> blocks(String username) {
        List<FriendDto> out = new ArrayList<FriendDto>();
        for (UserBlock b : blocks.of(username)) {
            out.add(new FriendDto(b.getBlocked()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public PartyDto createParty(String username) throws IOException {
        sanctions.requireNotBanned(username);
        if (!members.ofUser(username).isEmpty()) {
            throw new ResponseStatusException(409, "Leave your party first");
        }
        Party p = new Party();
        p.setId(Ids.next());
        p.setLeader(username);
        p.setCreatedAt(Clock.now());
        parties.insert(p);
        addMember(p.getId(), username);
        return party(p);
    }

    private void addMember(String partyId, String username) {
        PartyMember m = new PartyMember();
        m.setId(partyId + "|" + username);
        m.setPartyId(partyId);
        m.setUsername(username);
        m.setJoinedAt(Clock.now());
        members.insert(m);
    }

    /// Only a friend of the leader may join, and never one either side blocked.
    @Transactional(rollbackFor = IOException.class)
    public PartyDto joinParty(String username, String partyId) {
        sanctions.requireNotBanned(username);
        Party p = party(partyId);
        if (!accounts.friendsOf(p.getLeader()).contains(username)
                || accounts.blockedEitherWay(username, p.getLeader())) {
            throw new ResponseStatusException(404, "No such party");
        }
        if (!members.ofUser(username).isEmpty()) {
            throw new ResponseStatusException(409, "Leave your party first");
        }
        if (members.members(p.getId()).size() >= PARTY_SIZE) {
            throw new ResponseStatusException(409, "That party is full");
        }
        addMember(p.getId(), username);
        return party(p);
    }

    /// Leaving hands the lead to the longest-standing member; the last one out ends the party.
    @Transactional(rollbackFor = IOException.class)
    public PartyDto leaveParty(String username, String partyId) {
        Party p = party(partyId);
        PartyMember m = members.findById(p.getId() + "|" + username);
        if (m == null) {
            throw new ResponseStatusException(404, "You are not in that party");
        }
        members.delete(m);
        List<PartyMember> left = members.members(p.getId());
        if (left.isEmpty()) {
            parties.delete(p);
            return new PartyDto(p.getId(), "", new ArrayList<String>());
        }
        if (p.getLeader().equals(username)) {
            p.setLeader(left.get(0).getUsername());
            parties.save(p);
        }
        return party(p);
    }

    public PartyDto myParty(String username) {
        List<PartyMember> mine = members.ofUser(username);
        if (mine.isEmpty()) {
            throw new ResponseStatusException(404, "You are not in a party");
        }
        return party(party(mine.get(0).getPartyId()));
    }

    private Party party(String id) {
        Party p = id == null ? null : parties.findById(id);
        if (p == null) {
            throw new ResponseStatusException(404, "No such party");
        }
        return p;
    }

    private PartyDto party(Party p) {
        List<String> names = new ArrayList<String>();
        for (PartyMember m : members.members(p.getId())) {
            names.add(m.getUsername());
        }
        return new PartyDto(p.getId(), p.getLeader(), names);
    }

    static String channel(String value) {
        if (value == null || value.length() < 1 || value.length() > 100 || value.startsWith("dm:")) {
            throw new ResponseStatusException(400, "That is not a chat channel");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == ':' || c == '-' || c == '_' || c == '.';
            if (!ok) {
                throw new ResponseStatusException(400, "That is not a chat channel");
            }
        }
        return value;
    }

    @Transactional(rollbackFor = IOException.class)
    public ChatDto send(String username, String channel, ChatDto message) throws IOException {
        return post(username, channel(channel), message);
    }

    private ChatDto post(String username, String channel, ChatDto message) throws IOException {
        sanctions.requireCanChat(username);
        limits.requireChat(username);
        String body = message == null || message.body() == null ? "" : message.body().trim();
        if (body.length() == 0 || body.length() > 200) {
            throw new ResponseStatusException(400, "A chat line is 1 to 200 characters");
        }
        for (int i = 0; i < body.length(); i++) {
            if (body.charAt(i) < ' ') {
                throw new ResponseStatusException(400, "That line has characters we cannot show");
            }
        }
        if (chat.sentSince(username, Clock.now() - CHAT_BURST_MILLIS).size() >= CHAT_BURST) {
            throw new ResponseStatusException(429, "Slow down");
        }
        ChatMessage m = new ChatMessage();
        m.setId(Ids.next());
        m.setChannel(channel);
        m.setSender(username);
        m.setBody(body);
        m.setCreatedAt(Clock.now());
        chat.insert(m);
        return new ChatDto(m.getId(), username, ChatFilter.filter(body, accounts.age(username)), m.getCreatedAt());
    }

    public List<ChatDto> read(String username, String channel) {
        return lines(username, channel(channel));
    }

    private List<ChatDto> lines(String username, String channel) {
        int age = accounts.age(username);
        List<ChatDto> out = new ArrayList<ChatDto>();
        for (ChatMessage m : chat.recent(channel, Clock.now() - CHAT_WINDOW)) {
            if (blocks.findById(username + "|" + m.getSender()) == null) {
                out.add(new ChatDto(m.getId(), m.getSender(), ChatFilter.filter(m.getBody(), age), m.getCreatedAt()));
            }
        }
        return out;
    }

    static String dmChannel(String a, String b) {
        return a.compareTo(b) < 0 ? "dm:" + a + "|" + b : "dm:" + b + "|" + a;
    }

    private void requireDm(String username, String other) {
        if (other == null || !accounts.friendsOf(username).contains(other)
                || accounts.blockedEitherWay(username, other)) {
            throw new ResponseStatusException(404, "You can only message your friends");
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public ChatDto sendDm(String username, String other, ChatDto message) throws IOException {
        requireDm(username, other);
        return post(username, dmChannel(username, other), message);
    }

    public List<ChatDto> dms(String username, String other) {
        requireDm(username, other);
        return lines(username, dmChannel(username, other));
    }

    @Transactional(rollbackFor = IOException.class)
    public InviteDto createInvite(String username, InviteDto request) throws IOException {
        Invite i = new Invite();
        i.setId(Ids.next().substring(0, 12));
        i.setInviter(username);
        i.setDeviceHash(deviceHash(request));
        i.setCreatedAt(Clock.now());
        invites.insert(i);
        return new InviteDto(i.getId(), "");
    }

    private static String deviceHash(InviteDto request) throws IOException {
        if (request == null || request.deviceId() == null || request.deviceId().trim().length() < 8) {
            throw new ResponseStatusException(400, "An invite needs the device id");
        }
        return Ids.sha256(request.deviceId().trim()).substring(0, 64);
    }

    /// Redeems an invite: both sides get [#REFERRAL_BRICKS]. Refused, and
    /// flagged for staff, when the code is the caller's own, when the
    /// redeeming device is the one that made the code, or when the inviter
    /// passed [#REFERRALS_PER_DAY] today. An account redeems once, and only
    /// within [#REFERRAL_WINDOW] of opening.
    ///
    /// Deliberately not one transaction: a refusal must keep the fraud flag
    /// it wrote. The referral row goes in first, keyed by the invited
    /// account, so a second redemption fails before any Bricks move.
    public WalletDto redeem(String username, InviteDto request) throws IOException {
        String device = deviceHash(request);
        Invite i = request.code() == null ? null : invites.findById(request.code().trim());
        if (i == null) {
            throw new ResponseStatusException(404, "No such invite");
        }
        if (referrals.findById(username) != null) {
            throw new ResponseStatusException(409, "You already redeemed an invite");
        }
        Account me = accounts.account(username);
        if (me.getCreatedAt() < Clock.now() - REFERRAL_WINDOW) {
            throw new ResponseStatusException(409, "Invites are for new accounts");
        }
        if (i.getInviter().equals(username)) {
            throw flag(username, "SELF_REFERRAL", "own code " + i.getId());
        }
        if (i.getDeviceHash().equals(device)) {
            throw flag(username, "SAME_DEVICE", "code " + i.getId() + " from " + i.getInviter());
        }
        if (referrals.byInviterSince(i.getInviter(), Clock.now() - Clock.DAY).size() >= REFERRALS_PER_DAY) {
            throw flag(i.getInviter(), "REFERRAL_VELOCITY", "redeemed by " + username);
        }
        Referral r = new Referral();
        r.setId(username);
        r.setInviter(i.getInviter());
        r.setCode(i.getId());
        r.setDeviceHash(device);
        r.setCreatedAt(Clock.now());
        referrals.insert(r);
        wallet.post(username, REFERRAL_BRICKS, "REFERRAL", i.getInviter());
        wallet.post(i.getInviter(), REFERRAL_BRICKS, "REFERRAL", username);
        return wallet.describe(username);
    }

    /// Writes the flag and answers the refusal to throw.
    private ResponseStatusException flag(String username, String kind, String detail) throws IOException {
        FraudFlag f = new FraudFlag();
        f.setId(Ids.next());
        f.setUsername(username);
        f.setKind(kind);
        f.setDetail(detail.length() > 400 ? detail.substring(0, 400) : detail);
        f.setCreatedAt(Clock.now());
        fraud.insert(f);
        return new ResponseStatusException(403, "That invite cannot be redeemed");
    }

    private static PresenceDto toDto(Presence p) {
        return new PresenceDto(p.getId(), p.getStatus(), p.getExperienceId(), p.getServerId(), p.getUpdatedAt());
    }
}

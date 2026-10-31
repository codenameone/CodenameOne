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
package com.codenameone.examples.pulse.social;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.account.Accounts;
import com.codenameone.examples.pulse.api.FeedItemDto;
import com.codenameone.examples.pulse.api.FriendDto;
import com.codenameone.examples.pulse.api.InviteDto;
import com.codenameone.examples.pulse.api.NudgeDto;
import com.codenameone.examples.pulse.domain.Booking;
import com.codenameone.examples.pulse.domain.ClassSession;
import com.codenameone.examples.pulse.domain.ClassType;
import com.codenameone.examples.pulse.domain.Friend;
import com.codenameone.examples.pulse.domain.Kudos;
import com.codenameone.examples.pulse.domain.Profile;
import com.codenameone.examples.pulse.domain.Workout;
import com.codenameone.examples.pulse.engagement.Engagement;
import com.codenameone.examples.pulse.push.Notifier;
import com.codenameone.examples.pulse.repository.BookingRepository;
import com.codenameone.examples.pulse.repository.CheckInRepository;
import com.codenameone.examples.pulse.repository.ClassTypeRepository;
import com.codenameone.examples.pulse.repository.FriendRepository;
import com.codenameone.examples.pulse.repository.KudosRepository;
import com.codenameone.examples.pulse.repository.NudgeRepository;
import com.codenameone.examples.pulse.repository.ProfileRepository;
import com.codenameone.examples.pulse.repository.WorkoutRepository;
import com.codenameone.examples.pulse.studio.Studio;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Friends at the studio: who they are, what they did, kudos for it, and
/// asking one along to a class.
///
/// A friendship is two rows, one per direction, each saying "I want to be
/// friends with". One row is a request; both are a friendship. Nothing about
/// a member -- their visits, their workouts -- is shown to anyone they have
/// not accepted, and a workout only when they marked it shared.
@Component
public class Social {
    /// [FriendDto#state] values.
    public static final String FRIENDS = "FRIENDS";
    public static final String REQUESTED = "REQUESTED";
    public static final String INCOMING = "INCOMING";
    static final String ACCEPTED = "ACCEPTED";
    static final int MOST_FRIENDS = 200;
    /// How far back the feed reaches.
    static final long FEED_MILLIS = 14L * 86400000L;

    private final FriendRepository friends;
    private final ProfileRepository profiles;
    private final WorkoutRepository workouts;
    private final KudosRepository kudos;
    private final CheckInRepository checkIns;
    private final BookingRepository bookings;
    private final ClassTypeRepository types;
    private final NudgeRepository nudges;
    private final Engagement engagement;
    private final Studio studio;
    private final Notifier notifier;

    public Social(FriendRepository friends, ProfileRepository profiles, WorkoutRepository workouts,
            KudosRepository kudos, CheckInRepository checkIns, BookingRepository bookings,
            ClassTypeRepository types, NudgeRepository nudges, Engagement engagement,
            Studio studio, Notifier notifier) {
        this.friends = friends;
        this.profiles = profiles;
        this.workouts = workouts;
        this.kudos = kudos;
        this.checkIns = checkIns;
        this.bookings = bookings;
        this.types = types;
        this.nudges = nudges;
        this.engagement = engagement;
        this.studio = studio;
        this.notifier = notifier;
    }

    /// Friends first, then requests either way, each by name.
    @Transactional(readOnly = true)
    public List<FriendDto> friends(String account) {
        Map<String, String> states = states(account);
        List<FriendDto> out = new ArrayList<FriendDto>();
        String[] order = {FRIENDS, INCOMING, REQUESTED};
        for (int pass = 0; pass < order.length; pass++) {
            for (Map.Entry<String, String> entry : states.entrySet()) {
                if (order[pass].equals(entry.getValue())) {
                    out.add(toDto(entry.getKey(), entry.getValue()));
                }
            }
        }
        return out;
    }

    /// Each other account the caller has a row with, or that has one with
    /// the caller, and what that makes them.
    private Map<String, String> states(String account) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        List<Friend> mine = friends.findByAccount(account);
        for (int iter = 0; iter < mine.size(); iter++) {
            out.put(mine.get(iter).getFriend(), REQUESTED);
        }
        List<Friend> theirs = friends.findByFriend(account);
        for (int iter = 0; iter < theirs.size(); iter++) {
            String other = theirs.get(iter).getAccount();
            out.put(other, out.containsKey(other) ? FRIENDS : INCOMING);
        }
        return out;
    }

    private String state(String account, String other) {
        String state = states(account).get(other);
        return state == null ? "" : state;
    }

    /// Asks someone to be friends, by the e-mail they sign in with. Asking
    /// someone who already asked the caller makes them friends. Someone with
    /// no account is answered the same as someone who has one: the caller
    /// learns nothing about who is a member.
    @Transactional(rollbackFor = IOException.class)
    public FriendDto request(String account, String email) throws IOException {
        String other = Accounts.email(email);
        Profile them = profiles.findById(other);
        if (other.equals(account)) {
            throw new ResponseStatusException(400, "That is you");
        }
        if (friends.findByAccount(account).size() >= MOST_FRIENDS) {
            throw new ResponseStatusException(409, "That is a lot of friends already");
        }
        if (them == null || them.isFlagged()) {
            return new FriendDto(other, other, REQUESTED, 0, 0);
        }
        link(account, other);
        return toDto(other, state(account, other));
    }

    /// Accepts a request someone sent.
    @Transactional(rollbackFor = IOException.class)
    public FriendDto accept(String account, String other) throws IOException {
        if (!INCOMING.equals(state(account, other))) {
            throw new ResponseStatusException(404, "No such request");
        }
        link(account, other);
        return toDto(other, FRIENDS);
    }

    private void link(String account, String other) throws IOException {
        List<Friend> mine = friends.findByAccount(account);
        for (int iter = 0; iter < mine.size(); iter++) {
            if (mine.get(iter).getFriend().equals(other)) {
                return;
            }
        }
        Friend row = new Friend();
        row.setId(Ids.next());
        row.setAccount(account);
        row.setFriend(other);
        row.setState(ACCEPTED);
        row.setCreatedAt(Instant.now());
        friends.insert(row);
    }

    /// Ends a friendship, declines a request or withdraws one: both
    /// directions go.
    @Transactional(rollbackFor = IOException.class)
    public FriendDto remove(String account, String other) {
        boolean any = false;
        List<Friend> mine = friends.findByAccount(account);
        for (int iter = 0; iter < mine.size(); iter++) {
            if (mine.get(iter).getFriend().equals(other)) {
                friends.deleteById(mine.get(iter).getId());
                any = true;
            }
        }
        List<Friend> theirs = friends.findByFriend(account);
        for (int iter = 0; iter < theirs.size(); iter++) {
            if (theirs.get(iter).getAccount().equals(other)) {
                friends.deleteById(theirs.get(iter).getId());
                any = true;
            }
        }
        if (!any) {
            throw new ResponseStatusException(404, "No such friend");
        }
        return new FriendDto(other, name(other), "", 0, 0);
    }

    private FriendDto toDto(String other, String state) {
        if (!FRIENDS.equals(state)) {
            return new FriendDto(other, name(other), state, 0, 0);
        }
        Instant last = checkIns.lastVisit(other);
        return new FriendDto(other, name(other), state, last == null ? 0 : last.toEpochMilli(),
                engagement.stats(other).weekStreak());
    }

    private String name(String account) {
        Profile who = profiles.findById(account);
        return who == null ? "" : who.getDisplayName();
    }

    /// The caller's and their friends' shared workouts of the last two weeks,
    /// newest first, with their kudos.
    @Transactional(readOnly = true)
    public List<FeedItemDto> feed(String account) {
        List<String> circle = circle(account);
        circle.add(account);
        List<Workout> rows = workouts.findByAccountInAndSharedTrueAndStartedAtGreaterThanEqualOrderByStartedAtDesc(
                circle, Instant.ofEpochMilli(System.currentTimeMillis() - FEED_MILLIS));
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < rows.size() && iter < 100; iter++) {
            ids.add(rows.get(iter).getId());
        }
        Map<String, int[]> counts = new HashMap<String, int[]>();
        List<Kudos> given = ids.isEmpty() ? new ArrayList<Kudos>() : kudos.findByWorkoutIdIn(ids);
        for (int iter = 0; iter < given.size(); iter++) {
            Kudos one = given.get(iter);
            int[] count = counts.get(one.getWorkoutId());
            if (count == null) {
                count = new int[2];
                counts.put(one.getWorkoutId(), count);
            }
            count[0]++;
            if (one.getFromAccount().equals(account)) {
                count[1] = 1;
            }
        }
        Map<String, String> names = new HashMap<String, String>();
        List<FeedItemDto> out = new ArrayList<FeedItemDto>();
        for (int iter = 0; iter < ids.size(); iter++) {
            Workout row = rows.get(iter);
            String who = names.get(row.getAccount());
            if (who == null) {
                who = name(row.getAccount());
                names.put(row.getAccount(), who);
            }
            int[] count = counts.get(row.getId());
            out.add(new FeedItemDto(row.getId(), "WORKOUT", row.getAccount(), who,
                    row.getActivity(), row.getStartedAt().toEpochMilli(), row.getMinutes(),
                    row.getAvgHr(), row.getCalories(), count == null ? 0 : count[0],
                    count != null && count[1] == 1, row.getSessionId()));
        }
        return out;
    }

    private List<String> circle(String account) {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, String> entry : states(account).entrySet()) {
            if (FRIENDS.equals(entry.getValue())) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    /// Kudos for a friend's shared workout. Given twice, counted once.
    @Transactional(rollbackFor = IOException.class)
    public FeedItemDto kudos(String account, String workoutId) throws IOException {
        Workout row = workoutId == null || workoutId.length() > 32 ? null
                : workouts.findById(workoutId);
        if (row == null || !row.isShared() || !FRIENDS.equals(state(account, row.getAccount()))) {
            throw new ResponseStatusException(404, "No such workout");
        }
        if (!kudos.existsByFromAccountAndWorkoutId(account, row.getId())) {
            Kudos one = new Kudos();
            one.setId(Ids.next());
            one.setFromAccount(account);
            one.setToAccount(row.getAccount());
            one.setWorkoutId(row.getId());
            one.setCreatedAt(Instant.now());
            kudos.insert(one);
        }
        List<String> ids = new ArrayList<String>();
        ids.add(row.getId());
        int count = kudos.findByWorkoutIdIn(ids).size();
        return new FeedItemDto(row.getId(), "WORKOUT", row.getAccount(), name(row.getAccount()),
                row.getActivity(), row.getStartedAt().toEpochMilli(), row.getMinutes(),
                row.getAvgHr(), row.getCalories(), count, true, row.getSessionId());
    }

    /// Asks a friend along to a class. Once per friend per class; a friend
    /// already in it is not asked.
    @Transactional(rollbackFor = IOException.class)
    public NudgeDto invite(String account, InviteDto request) throws IOException {
        if (request == null || request.friend() == null) {
            throw new ResponseStatusException(400, "Choose a friend");
        }
        if (!FRIENDS.equals(state(account, request.friend()))) {
            throw new ResponseStatusException(404, "No such friend");
        }
        ClassSession session = studio.session(request.sessionId());
        if (!Studio.SCHEDULED.equals(session.getState())
                || !session.getStartsAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(409, "That class has started or is off");
        }
        List<Booking> in = bookings.findBySessionIdOrderByCreatedAtAsc(session.getId());
        for (int iter = 0; iter < in.size(); iter++) {
            if (in.get(iter).getAccount().equals(request.friend())) {
                throw new ResponseStatusException(409, name(request.friend())
                        + " is in that class already");
            }
        }
        if (nudges.existsByAccountAndSessionIdAndKind(request.friend(), session.getId(),
                Notifier.INVITE)) {
            throw new ResponseStatusException(409, name(request.friend())
                    + " has been asked to that class");
        }
        ClassType type = types.findById(session.getClassTypeId());
        return Engagement.toDto(notifier.tell(request.friend(), Notifier.INVITE, "",
                session.getId(), name(account) + " asked you along",
                "Join " + name(account) + " at " + (type == null ? "a class" : type.getName())
                        + ".", Instant.now()));
    }
}

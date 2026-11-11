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
package com.codenameone.examples.splat.mission;

import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.splat.api.MissionDto;
import com.codenameone.examples.splat.domain.MissionRecord;
import com.codenameone.examples.splat.domain.Profile;
import com.codenameone.examples.splat.domain.Referral;
import com.codenameone.examples.splat.repository.MissionRecordRepository;
import com.codenameone.examples.splat.repository.ProfileRepository;
import com.codenameone.examples.splat.repository.ReferralRepository;
import com.codenameone.examples.splat.rules.Mission;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Player-built missions: storing them, listing them, and making one
/// playable online. The rules module resolves an arena id through
/// `Catalog.arena`, which knows a mission only once it has been
/// `register()`ed, so anything that takes a level from a player calls
/// [#resolve] before `Arenas.normalize` -- `Arenas` itself stays free of the
/// database.
///
/// A player's friends here are the players they invited and the one who
/// invited them, the same relation `/api/social/friends` shows.
@Component
public class Missions {
    public static final int MAX_BODY = 16 * 1024;
    public static final int MAX_PER_OWNER = 50;
    public static final int LIST_LIMIT = 50;
    public static final int CODE_LENGTH = 8;
    private static final int FRIEND_PAGE = 100;
    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final MissionRecordRepository missions;
    private final ProfileRepository profiles;
    private final ReferralRepository referrals;

    public Missions(MissionRecordRepository missions, ProfileRepository profiles, ReferralRepository referrals) {
        this.missions = missions;
        this.profiles = profiles;
        this.referrals = referrals;
    }

    @Transactional(rollbackFor = IOException.class)
    public MissionDto publish(String username, String json) throws IOException {
        if (json == null || json.length() == 0) {
            throw new ResponseStatusException(400, "No mission was sent");
        }
        if (json.length() > MAX_BODY) {
            throw new ResponseStatusException(413, "A mission is at most " + MAX_BODY + " characters");
        }
        Mission m;
        try {
            m = Mission.fromJson(json);
        } catch (IllegalArgumentException bad) {
            throw new ResponseStatusException(400, "That is not a mission");
        }
        if (missions.byOwner(username, new PageRequest(0, MAX_PER_OWNER)).size() >= MAX_PER_OWNER) {
            throw new ResponseStatusException(409, "You have published " + MAX_PER_OWNER + " missions already");
        }
        // The client's code means nothing here: the server names every mission.
        String code = freshCode();
        m.code = code;
        String problem = m.problem();
        if (problem != null) {
            throw new ResponseStatusException(400, problem);
        }
        MissionRecord r = new MissionRecord();
        r.setCode(code);
        r.setOwner(username);
        r.setName(m.name);
        r.setMode(m.mode.name());
        r.setBody(m.toJson());
        r.setCreatedAt(Instant.now());
        missions.insert(r);
        return view(r, true);
    }

    /// The caller's own missions and their friends', newest first.
    @Transactional(readOnly = true)
    public List<MissionDto> list(String username) throws IOException {
        Set<String> owners = new HashSet<String>();
        owners.add(username);
        for (Referral r : referrals.of(username, new PageRequest(0, FRIEND_PAGE))) {
            owners.add(r.getInvitee());
        }
        Referral invitedBy = referrals.findById(username);
        if (invitedBy != null) {
            owners.add(invitedBy.getInviter());
        }
        List<MissionRecord> all = new ArrayList<MissionRecord>();
        for (String owner : owners) {
            all.addAll(missions.byOwner(owner, new PageRequest(0, LIST_LIMIT)));
        }
        Collections.sort(all, new Comparator<MissionRecord>() {
            @Override
            public int compare(MissionRecord a, MissionRecord b) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
        });
        List<MissionDto> out = new ArrayList<MissionDto>();
        for (int i = 0; i < all.size() && i < LIST_LIMIT; i++) {
            out.add(view(all.get(i), false));
        }
        return out;
    }

    /// Any signed-in player may fetch a mission by its code: the code is the
    /// share link, as a party code is.
    @Transactional(readOnly = true)
    public MissionDto get(String code) throws IOException {
        MissionRecord r = code == null ? null : missions.findById(code);
        if (r == null) {
            throw new ResponseStatusException(404, "No mission has that code");
        }
        return view(r, true);
    }

    /// Makes a level reference playable: when its arena is a mission
    /// (`m-<code>`), loads it and registers it with the rules catalog. Answers
    /// the reference unchanged, so callers pass the result straight on to
    /// `Arenas.normalize`, which then accepts or rejects it as usual. An
    /// unknown mission stays unregistered and normalizes to null.
    @Transactional(readOnly = true)
    public String resolve(String ref) {
        if (ref == null || !ref.startsWith(Mission.PREFIX)) {
            return ref;
        }
        int colon = ref.indexOf(':');
        String code = ref.substring(Mission.PREFIX.length(), colon < 0 ? ref.length() : colon);
        if (code.length() == 0 || code.length() > 16) {
            return ref;
        }
        MissionRecord r = missions.findById(code);
        if (r != null) {
            try {
                Mission.fromJson(r.getBody()).register();
            } catch (IllegalArgumentException stale) {
                // A stored body the rules no longer read: leave it unplayable.
                return ref;
            }
        }
        return ref;
    }

    /// Counts a race started on a mission arena. Not transactional with the
    /// race: a lost count costs nothing.
    @Transactional(rollbackFor = IOException.class)
    public void played(String arenaId) {
        if (arenaId == null || !arenaId.startsWith(Mission.PREFIX)) {
            return;
        }
        MissionRecord r = missions.findById(arenaId.substring(Mission.PREFIX.length()));
        if (r != null) {
            r.setPlays(r.getPlays() + 1);
            missions.save(r);
        }
    }

    private String freshCode() throws IOException {
        while (true) {
            byte[] bytes = Crypto.randomBytes(CODE_LENGTH);
            char[] out = new char[CODE_LENGTH];
            for (int i = 0; i < CODE_LENGTH; i++) {
                out[i] = ALPHABET[(bytes[i] & 0xff) % ALPHABET.length];
            }
            String code = new String(out);
            if (missions.findById(code) == null) {
                return code;
            }
        }
    }

    private MissionDto view(MissionRecord r, boolean withBody) {
        Profile p = profiles.findById(r.getOwner());
        return new MissionDto(r.getCode(), r.getName(), r.getMode(), r.getOwner(),
                p == null ? r.getOwner() : p.getDisplayName(), r.getPlays(), r.getCreatedAt().toEpochMilli(),
                withBody ? r.getBody() : "");
    }
}

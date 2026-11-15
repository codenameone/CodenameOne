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
package com.codenameone.examples.brickyard.experience;

import com.codename1.appkit.server.Caller;
import com.codename1.appkit.server.Text;
import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.Roles;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.api.ExperienceSaveDto;
import com.codenameone.examples.brickyard.api.PlaceDto;
import com.codenameone.examples.brickyard.api.VersionDto;
import com.codenameone.examples.brickyard.domain.Experience;
import com.codenameone.examples.brickyard.domain.ExperienceVersion;
import com.codenameone.examples.brickyard.domain.Reaction;
import com.codenameone.examples.brickyard.repository.ExperienceRepository;
import com.codenameone.examples.brickyard.repository.ExperienceVersionRepository;
import com.codenameone.examples.brickyard.repository.ReactionRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Experiences and their versions. Exactly one version is PUBLISHED at a time
/// and the experience's publishedVersion names it; a version superseded by a
/// publish goes back to DRAFT, and one left by a rollback is ROLLED_BACK.
@Component
public class Experiences {
    public static final String PUBLIC = "PUBLIC";
    public static final String FRIENDS = "FRIENDS";
    public static final String PRIVATE = "PRIVATE";
    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String ROLLED_BACK = "ROLLED_BACK";
    public static final int MAX_PLAYERS = 100;

    private final ExperienceRepository experiences;
    private final ExperienceVersionRepository versions;
    private final ReactionRepository reactions;
    private final Accounts accounts;
    private final Places places;

    public Experiences(ExperienceRepository experiences, ExperienceVersionRepository versions,
            ReactionRepository reactions, Accounts accounts, Places places) {
        this.experiences = experiences;
        this.versions = versions;
        this.reactions = reactions;
        this.accounts = accounts;
        this.places = places;
    }

    @Transactional(rollbackFor = IOException.class)
    public ExperienceDto create(String owner, ExperienceSaveDto request) throws IOException {
        if (!Caller.hasRole(Roles.CREATOR)) {
            throw new ResponseStatusException(403, "Only a creator can open an experience");
        }
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to create");
        }
        accounts.account(owner);
        Experience e = new Experience();
        e.setId(Ids.next());
        e.setOwner(owner);
        e.setTitle(Text.required(request.title(), 80, "a title").trim());
        e.setDescription(request.description() == null ? "" : Text.required(request.description(), 1000,
                "the description"));
        e.setGenre(request.genre() == null ? "ALL" : Text.required(request.genre(), 32, "the genre"));
        if (request.maxPlayers() < 1 || request.maxPlayers() > MAX_PLAYERS) {
            throw new ResponseStatusException(400, "An experience takes 1 to " + MAX_PLAYERS + " players");
        }
        e.setMaxPlayers(request.maxPlayers());
        e.setAccessLevel(access(request.access()));
        e.setAgeRating(rating(request.ageRating()));
        e.setCreatedAt(Clock.now());
        experiences.insert(e);
        return toDto(e);
    }

    /// Published, public experiences the caller is old enough for, most visited first.
    @Transactional(readOnly = true)
    public List<ExperienceDto> browse(String genre) {
        int age = accounts.age(Caller.name());
        List<ExperienceDto> out = new ArrayList<ExperienceDto>();
        for (Experience e : experiences.published(0)) {
            if (PUBLIC.equals(e.getAccessLevel()) && age >= minimumAge(e.getAgeRating())
                    && (genre == null || genre.length() == 0 || genre.equals(e.getGenre()))) {
                out.add(toDto(e));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ExperienceDto describe(String id) {
        return toDto(visible(id));
    }

    /// The experience if the caller may see it: its creator and staff always;
    /// anyone else only once it is published, when its access allows them and
    /// they are old enough for its rating.
    public Experience visible(String id) {
        Experience e = find(id);
        if (Access.isOwner(e) || Access.isStaff() || Caller.hasRole(Roles.ENGINE)) {
            return e;
        }
        String me = Caller.name();
        boolean allowed = e.getPublishedVersion() > 0
                && (PUBLIC.equals(e.getAccessLevel())
                || FRIENDS.equals(e.getAccessLevel()) && accounts.friendsOf(e.getOwner()).contains(me));
        if (!allowed) {
            throw new ResponseStatusException(404, "No such experience");
        }
        if (accounts.age(me) < minimumAge(e.getAgeRating())) {
            throw new ResponseStatusException(403, "This experience is rated " + e.getAgeRating());
        }
        return e;
    }

    public Experience find(String id) {
        Experience e = id == null ? null : experiences.findById(id);
        if (e == null) {
            throw new ResponseStatusException(404, "No such experience");
        }
        return e;
    }

    /// Stores a new version. Both drafts and publishes are validated: a place
    /// that does not parse and load is refused before anything is written.
    @Transactional(rollbackFor = IOException.class)
    public VersionDto upload(String id, String data, boolean publish) throws IOException {
        Experience e = find(id);
        Access.requireOwner(e);
        byte[] bytes = data == null ? null : Base64.decode(data);
        if (bytes == null) {
            throw new ResponseStatusException(400, "The place file is not base64");
        }
        String report = places.validate(bytes);
        ExperienceVersion v = new ExperienceVersion();
        v.setId(Ids.next());
        v.setExperienceId(id);
        v.setVersionNumber(e.getLatestVersion() + 1);
        v.setPlace(bytes);
        v.setPlaceSize(bytes.length);
        v.setReport(report);
        v.setStatus(DRAFT);
        v.setCreatedAt(Clock.now());
        versions.insert(v);
        e.setLatestVersion(v.getVersionNumber());
        if (publish) {
            point(e, v, DRAFT);
        }
        experiences.save(e);
        return toDto(v);
    }

    /// Publishes an earlier version again; the one it replaces is ROLLED_BACK.
    @Transactional(rollbackFor = IOException.class)
    public VersionDto rollback(String id, int number) {
        Experience e = find(id);
        Access.requireOwner(e);
        if (number == e.getPublishedVersion()) {
            throw new ResponseStatusException(409, "That version is already published");
        }
        ExperienceVersion target = version(id, number);
        point(e, target, ROLLED_BACK);
        experiences.save(e);
        return toDto(target);
    }

    private void point(Experience e, ExperienceVersion target, String previousStatus) {
        if (e.getPublishedVersion() > 0) {
            ExperienceVersion current = version(e.getId(), e.getPublishedVersion());
            current.setStatus(previousStatus);
            versions.save(current);
        }
        target.setStatus(PUBLISHED);
        versions.save(target);
        e.setPublishedVersion(target.getVersionNumber());
    }

    @Transactional(readOnly = true)
    public List<VersionDto> versions(String id) {
        Access.requireOwner(find(id));
        List<VersionDto> out = new ArrayList<VersionDto>();
        for (ExperienceVersion v : versions.of(id)) {
            out.add(toDto(v));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PlaceDto place(String id) {
        Experience e = visible(id);
        if (e.getPublishedVersion() == 0) {
            throw new ResponseStatusException(404, "Nothing is published yet");
        }
        ExperienceVersion v = version(id, e.getPublishedVersion());
        return new PlaceDto(v.getVersionNumber(), Base64.encode(v.getPlace()));
    }

    @Transactional(rollbackFor = IOException.class)
    public ExperienceDto visit(String id) {
        Experience e = visible(id);
        e.setVisits(e.getVisits() + 1);
        experiences.save(e);
        return toDto(e);
    }

    /// Toggles the caller's favourite.
    @Transactional(rollbackFor = IOException.class)
    public ExperienceDto favourite(String id) {
        Experience e = visible(id);
        Reaction r = reaction(e);
        r.setFavourite(!r.isFavourite());
        e.setFavourites(e.getFavourites() + (r.isFavourite() ? 1 : -1));
        reactions.save(r);
        experiences.save(e);
        return toDto(e);
    }

    /// Records the caller's vote, replacing any earlier one: 1 up, -1 down, 0 none.
    @Transactional(rollbackFor = IOException.class)
    public ExperienceDto vote(String id, int vote) {
        if (vote < -1 || vote > 1) {
            throw new ResponseStatusException(400, "A vote is 1, 0 or -1");
        }
        Experience e = visible(id);
        Reaction r = reaction(e);
        count(e, r.getVote(), -1);
        r.setVote(vote);
        count(e, vote, 1);
        reactions.save(r);
        experiences.save(e);
        return toDto(e);
    }

    private static void count(Experience e, int vote, int by) {
        if (vote > 0) {
            e.setUpVotes(e.getUpVotes() + by);
        } else if (vote < 0) {
            e.setDownVotes(e.getDownVotes() + by);
        }
    }

    private Reaction reaction(Experience e) {
        String key = e.getId() + "|" + Caller.name();
        Reaction r = reactions.findById(key);
        if (r == null) {
            r = new Reaction();
            r.setId(key);
            r.setExperienceId(e.getId());
            r.setUsername(Caller.name());
            reactions.insert(r);
        }
        return r;
    }

    private ExperienceVersion version(String id, int number) {
        List<ExperienceVersion> found = versions.numbered(id, number);
        if (found.isEmpty()) {
            throw new ResponseStatusException(404, "No such version");
        }
        return found.get(0);
    }

    static String access(String value) {
        if (PUBLIC.equals(value) || FRIENDS.equals(value) || PRIVATE.equals(value)) {
            return value;
        }
        if (value == null) {
            return PRIVATE;
        }
        throw new ResponseStatusException(400, "Access is PUBLIC, FRIENDS or PRIVATE");
    }

    static String rating(String value) {
        if (value == null) {
            return "ALL";
        }
        minimumAge(value);
        return value;
    }

    /// The youngest age a rating admits: ALL, 9+, 13+ or 17+.
    static int minimumAge(String rating) {
        if ("ALL".equals(rating)) {
            return 0;
        }
        if ("9+".equals(rating)) {
            return 9;
        }
        if ("13+".equals(rating)) {
            return 13;
        }
        if ("17+".equals(rating)) {
            return 17;
        }
        throw new ResponseStatusException(400, "A rating is ALL, 9+, 13+ or 17+");
    }

    static ExperienceDto toDto(Experience e) {
        return new ExperienceDto(e.getId(), e.getOwner(), e.getTitle(), e.getDescription(), e.getGenre(),
                e.getMaxPlayers(), e.getAccessLevel(), e.getAgeRating(), e.getPublishedVersion(),
                e.getVisits(), e.getFavourites(), e.getUpVotes(), e.getDownVotes());
    }

    static VersionDto toDto(ExperienceVersion v) {
        return new VersionDto(v.getVersionNumber(), v.getStatus(), v.getCreatedAt(), v.getPlaceSize(),
                v.getReport());
    }
}

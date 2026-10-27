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
package com.codenameone.examples.reel.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Requests;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.HouseholdProfileDto;
import com.codenameone.examples.reel.api.ProfileEditDto;
import com.codenameone.examples.reel.domain.Household;
import com.codenameone.examples.reel.playback.MediaTokens;
import com.codenameone.examples.reel.repository.HouseholdRepository;
import com.codenameone.examples.reel.settings.Settings;

import java.time.Instant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The viewers inside an account: each with its own list, history,
/// recommendations and rating limit, a kids profile among them.
///
/// Every account has at least one, made the first time it is asked for. With
/// the profiles switch off the account has that one and no more. A PIN is
/// stored as a password hash and checked by [#unlock]; the PIN gates choosing
/// the profile in the app, which is a household lock, not a security boundary
/// against the account's own owner.
@Component
public class Households {
    public static final int MAX_PROFILES = 5;
    /// Ratings from mildest to strongest; a profile sees up to its limit.
    public static final String[] RATINGS = {"G", "PG", "PG-13", "R", "NC-17"};

    private final HouseholdRepository rows;
    private final PasswordEncoder encoder;
    private final Settings settings;
    private final MediaTokens tokens;

    public Households(HouseholdRepository rows, PasswordEncoder encoder, Settings settings, MediaTokens tokens) {
        this.tokens = tokens;
        this.rows = rows;
        this.encoder = encoder;
        this.settings = settings;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<HouseholdProfileDto> list(String username, String ownerName) throws IOException {
        List<Household> all = ensure(username, ownerName);
        List<HouseholdProfileDto> out = new ArrayList<HouseholdProfileDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(dto(all.get(iter)));
        }
        return out;
    }

    /// The profile `id` of the account, or its first when `id` is empty. Answers
    /// 404 for a profile that is not the account's, and 403 for a PIN-protected
    /// one the request carries no key for: the PIN is checked here, on every
    /// call, never only on the app's profile picker.
    @Transactional(rollbackFor = IOException.class)
    public Household resolve(String username, String id) throws IOException {
        Household row = owned(username, id);
        if (row.getPinHash().length() > 0
                && !tokens.verifyProfile(Requests.header(Requests.PROFILE_KEY), username, row.getId())) {
            throw new ResponseStatusException(403, "Enter this profile's PIN first");
        }
        return row;
    }

    private Household owned(String username, String id) throws IOException {
        List<Household> all = ensure(username, "");
        if (id == null || id.length() == 0) {
            return all.get(0);
        }
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).getId().equals(id)) {
                return all.get(iter);
            }
        }
        throw new ResponseStatusException(404, "No such profile");
    }

    /// Profile edits change the PINs and rating limits themselves, so once any
    /// grown-up profile has a PIN they need the key of an unlocked grown-up
    /// profile. Without one there is no parental control to get around.
    private void requireAdult(String username) throws IOException {
        List<Household> all = ensure(username, "");
        boolean guarded = false;
        String key = Requests.header(Requests.PROFILE_KEY);
        for (int iter = 0; iter < all.size(); iter++) {
            Household h = all.get(iter);
            if (h.isKids()) {
                continue;
            }
            if (h.getPinHash().length() > 0) {
                guarded = true;
            }
            if (tokens.verifyProfile(key, username, h.getId())) {
                return;
            }
        }
        if (guarded) {
            throw new ResponseStatusException(403, "Unlock a grown-up profile to change profiles");
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public HouseholdProfileDto create(String username, ProfileEditDto wanted) throws IOException {
        settings.require(Features.PROFILES);
        requireAdult(username);
        List<Household> all = ensure(username, "");
        if (all.size() >= MAX_PROFILES) {
            throw new ResponseStatusException(409, "An account has at most " + MAX_PROFILES + " profiles");
        }
        Household row = new Household();
        row.setId(Ids.next());
        row.setUsername(username);
        row.setCreatedAt(Instant.now());
        apply(row, wanted);
        rows.insert(row);
        return dto(row);
    }

    @Transactional(rollbackFor = IOException.class)
    public HouseholdProfileDto update(String username, String id, ProfileEditDto wanted) throws IOException {
        requireAdult(username);
        Household row = owned(username, id);
        apply(row, wanted);
        rows.save(row);
        return dto(row);
    }

    @Transactional(rollbackFor = IOException.class)
    public void delete(String username, String id) throws IOException {
        requireAdult(username);
        List<Household> all = ensure(username, "");
        Household row = owned(username, id);
        if (all.size() == 1) {
            throw new ResponseStatusException(409, "An account keeps at least one profile");
        }
        rows.deleteById(row.getId());
    }

    /// Checks a profile's PIN and answers the profile with its key. A profile
    /// without one unlocks with anything. Guessing is rate limited per account
    /// in [com.codenameone.examples.reel.SecurityConfig]: four digits are ten
    /// thousand tries.
    @Transactional(rollbackFor = IOException.class)
    public HouseholdProfileDto unlock(String username, String id, String pin) throws IOException {
        Household row = owned(username, id);
        if (row.getPinHash().length() > 0 && (pin == null || !encoder.matches(pin, row.getPinHash()))) {
            throw new ResponseStatusException(403, "That PIN is not right");
        }
        HouseholdProfileDto d = dto(row);
        return new HouseholdProfileDto(d.id(), d.name(), d.avatar(), d.kids(), d.maxRating(), d.pinProtected(),
                d.language(), tokens.profile(username, row.getId()));
    }

    /// Whether a profile may see a title of this rating.
    /// A profile by id, whoever owns it; for the admin views.
    @Transactional(readOnly = true)
    public Household find(String id) {
        return rows.findById(id);
    }

    public static boolean allows(Household profile, String rating) {
        return rank(rating) <= rank(profile.getMaxRating());
    }

    static int rank(String rating) {
        for (int iter = 0; iter < RATINGS.length; iter++) {
            if (RATINGS[iter].equals(rating)) {
                return iter;
            }
        }
        // Unrated counts as the strongest: a kids profile does not see it.
        return RATINGS.length - 1;
    }

    private List<Household> ensure(String username, String ownerName) throws IOException {
        List<Household> all = rows.findByUsernameOrderByCreatedAtAsc(username);
        if (!all.isEmpty()) {
            return all;
        }
        Household first = new Household();
        first.setId(Ids.next());
        first.setUsername(username);
        first.setName(ownerName == null || ownerName.trim().length() == 0 ? "Me" : first(ownerName));
        first.setAvatar("red");
        first.setMaxRating("NC-17");
        first.setLanguage("en");
        first.setCreatedAt(Instant.now());
        rows.insert(first);
        all = new ArrayList<Household>();
        all.add(first);
        return all;
    }

    private void apply(Household row, ProfileEditDto wanted) {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String name = wanted.name() == null ? "" : wanted.name().trim();
        if (name.length() == 0 || name.length() > 60) {
            throw new ResponseStatusException(400, "A profile name is 1 to 60 characters");
        }
        row.setName(name);
        row.setAvatar(safe(wanted.avatar(), 32, "red"));
        row.setKids(wanted.kids());
        String rating = wanted.kids() ? "PG" : safe(wanted.maxRating(), 8, "NC-17");
        if (rank(rating) == RATINGS.length - 1 && !"NC-17".equals(rating)) {
            rating = "NC-17";
        }
        row.setMaxRating(rating);
        row.setLanguage(safe(wanted.language(), 8, "en"));
        if (wanted.clearPin()) {
            row.setPinHash("");
        } else if (wanted.pin() != null && wanted.pin().length() > 0) {
            String pin = wanted.pin();
            if (pin.length() != 4 || !digits(pin)) {
                throw new ResponseStatusException(400, "A PIN is four digits");
            }
            row.setPinHash(encoder.encode(pin));
        }
    }

    private static boolean digits(String text) {
        for (int iter = 0; iter < text.length(); iter++) {
            if (text.charAt(iter) < '0' || text.charAt(iter) > '9') {
                return false;
            }
        }
        return true;
    }

    private static String safe(String value, int max, String fallback) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return fallback;
        }
        return text.length() > max ? text.substring(0, max) : text;
    }

    private static String first(String name) {
        String text = name.trim();
        int space = text.indexOf(' ');
        String out = space > 0 ? text.substring(0, space) : text;
        return out.length() > 60 ? out.substring(0, 60) : out;
    }

    public static HouseholdProfileDto dto(Household row) {
        return new HouseholdProfileDto(row.getId(), row.getName(), row.getAvatar(), row.isKids(),
                row.getMaxRating(), row.getPinHash().length() > 0, row.getLanguage(), null);
    }
}

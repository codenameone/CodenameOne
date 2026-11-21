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
package com.codenameone.examples.bazaar.loyalty;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.domain.Club;
import com.codenameone.examples.bazaar.domain.ClubItem;
import com.codenameone.examples.bazaar.domain.ClubMember;
import com.codenameone.examples.bazaar.domain.LoyaltyMember;
import com.codenameone.examples.bazaar.domain.LoyaltyProgram;
import com.codenameone.examples.bazaar.domain.LoyaltyTier;
import com.codenameone.examples.bazaar.repository.ClubItemRepository;
import com.codenameone.examples.bazaar.repository.ClubMemberRepository;
import com.codenameone.examples.bazaar.repository.ClubRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyMemberRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyProgramRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyTierRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// What a shopper belongs to in a store, as the `eligibility` strings
/// member-only discounts carry: `TIER:<id>` for their loyalty tier and
/// `CLUB:<id>` for each club they are a paid-up member of. Also the rule
/// that keeps member-only and early-access products from everyone else.
@Component
public class Memberships {
    public static final String ACTIVE = "ACTIVE";
    static final long HOUR = 3600000L;

    private final LoyaltyProgramRepository programs;
    private final LoyaltyMemberRepository members;
    private final LoyaltyTierRepository tiers;
    private final ClubRepository clubs;
    private final ClubMemberRepository clubMembers;
    private final ClubItemRepository items;

    public Memberships(LoyaltyProgramRepository programs, LoyaltyMemberRepository members,
            LoyaltyTierRepository tiers, ClubRepository clubs, ClubMemberRepository clubMembers,
            ClubItemRepository items) {
        this.programs = programs;
        this.members = members;
        this.tiers = tiers;
        this.clubs = clubs;
        this.clubMembers = clubMembers;
        this.items = items;
    }

    /// The memberships of `username` in the store; empty for a guest.
    @Transactional(readOnly = true)
    public Set<String> of(String storeId, String username, Instant now) {
        Set<String> out = new HashSet<String>();
        if (username == null || username.length() == 0) {
            return out;
        }
        LoyaltyProgram p = programs.findByStoreId(storeId);
        LoyaltyMember m = members.findByStoreIdAndUsername(storeId, username);
        if (p != null && p.isActive() && m != null && m.getTierId().length() > 0) {
            out.add("TIER:" + m.getTierId());
        }
        List<ClubMember> joined = clubMembers.findByStoreIdAndUsername(storeId, username);
        for (int iter = 0; iter < joined.size(); iter++) {
            ClubMember c = joined.get(iter);
            if ((ACTIVE.equals(c.getStatus()) || "CANCELLED".equals(c.getStatus()))
                    && c.getRenewsAt().isAfter(now)) {
                out.add("CLUB:" + c.getClubId());
            }
        }
        return out;
    }

    /// The most early-access hours any of `memberships` grants.
    @Transactional(readOnly = true)
    public long earlyHours(Set<String> memberships) {
        long best = 0;
        for (String m : memberships) {
            if (m.startsWith("TIER:")) {
                LoyaltyTier t = tiers.findById(m.substring(5));
                best = t == null ? best : Math.max(best, t.getEarlyAccessHours());
            } else if (m.startsWith("CLUB:")) {
                Club c = clubs.findById(m.substring(5));
                best = c == null || !c.isActive() ? best : Math.max(best, c.getEarlyAccessHours());
            }
        }
        return best;
    }

    /// Whether the store puts anything behind a club.
    @Transactional(readOnly = true)
    public boolean anyRules(String storeId) {
        return !items.findByStoreId(storeId).isEmpty();
    }

    /// The products of the store a shopper with `memberships` may not even
    /// see: those a members-only rule covers, for anyone outside its club.
    /// Early access is different -- the product shows, and the quote refuses
    /// it ([#restriction]) until the shopper's window opens.
    ///
    /// @param membership product id -> ids of the collections it is in
    @Transactional(readOnly = true)
    public Set<String> hidden(String storeId, Iterable<String> productIds,
            java.util.Map<String, List<String>> membership, Set<String> memberships) {
        Set<String> out = new HashSet<String>();
        List<ClubItem> rules = items.findByStoreId(storeId);
        if (rules.isEmpty()) {
            return out;
        }
        for (String id : productIds) {
            List<String> in = membership.get(id);
            for (int iter = 0; iter < rules.size(); iter++) {
                ClubItem r = rules.get(iter);
                boolean covers = id.equals(r.getProductId())
                        || r.getCollectionId().length() > 0 && in != null && in.contains(r.getCollectionId());
                if (covers && r.isMembersOnly() && !memberships.contains("CLUB:" + r.getClubId())) {
                    out.add(id);
                    break;
                }
            }
        }
        return out;
    }

    /// Why the shopper may not buy `productId` (in `collectionIds`) yet, or
    /// null when they may.
    @Transactional(readOnly = true)
    public String restriction(String storeId, String productId, Set<String> collectionIds,
            String title, Set<String> memberships, Instant now) {
        List<ClubItem> rules = items.findByStoreId(storeId);
        if (rules.isEmpty()) {
            return null;
        }
        long early = -1;
        for (int iter = 0; iter < rules.size(); iter++) {
            ClubItem r = rules.get(iter);
            boolean covers = productId.equals(r.getProductId())
                    || r.getCollectionId().length() > 0 && collectionIds.contains(r.getCollectionId());
            if (!covers) {
                continue;
            }
            boolean member = memberships.contains("CLUB:" + r.getClubId());
            if (r.isMembersOnly() && !member) {
                return title + " is for club members";
            }
            long publicFrom = r.getPublicFrom().toEpochMilli();
            if (publicFrom > now.toEpochMilli()) {
                if (early < 0) {
                    early = earlyHours(memberships);
                }
                long membersFrom = r.getMembersFrom().toEpochMilli();
                boolean opened = member && membersFrom > 0 && membersFrom <= now.toEpochMilli()
                        || early > 0 && publicFrom - early * HOUR <= now.toEpochMilli();
                if (!opened) {
                    return title + " is not on sale yet";
                }
            }
        }
        return null;
    }
}

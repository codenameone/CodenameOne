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
package com.codenameone.examples.skirmish.rules.forge;

import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.CardSet;
import com.codenameone.examples.skirmish.rules.MatchRules;

import java.util.ArrayList;
import java.util.List;

/// Everything a creator makes in the Forge, shared as one unit: characters,
/// arenas, missions and the campaigns that string them together.
///
/// A pack is edited in place by the app and written with [PackCodec]. Once
/// published, a version never changes; its [#hash] is what replays and mission
/// results name, so a battle can always be checked against the exact content it
/// was played on.
public final class ContentPack {
    /// The pack format. A reader refuses a newer one rather than guess.
    public static final int FORMAT = 1;

    public String id = "";
    public String name = "";
    public String author = "";
    public String description = "";
    /// Bumped by the server on each publish.
    public int version;

    public List<CardDef> cards = new ArrayList<CardDef>();
    public List<Arena> arenas = new ArrayList<Arena>();
    public List<MissionDef> missions = new ArrayList<MissionDef>();
    public List<CampaignDef> campaigns = new ArrayList<CampaignDef>();
    public List<AssetRef> assets = new ArrayList<AssetRef>();

    /// The built-in cards plus this pack's.
    public CardSet cardSet() {
        return CardSet.withCustom(cards);
    }

    /// One of this pack's arenas by id, or the classic field for "classic".
    public Arena arena(String arenaId) {
        if (arenaId == null || "classic".equals(arenaId)) {
            return Arena.CLASSIC;
        }
        for (int i = 0; i < arenas.size(); i++) {
            if (arenas.get(i).id().equals(arenaId)) {
                return arenas.get(i);
            }
        }
        return null;
    }

    public MissionDef mission(String missionId) {
        for (int i = 0; i < missions.size(); i++) {
            if (missions.get(i).id.equals(missionId)) {
                return missions.get(i);
            }
        }
        return null;
    }

    public CampaignDef campaign(String campaignId) {
        for (int i = 0; i < campaigns.size(); i++) {
            if (campaigns.get(i).id.equals(campaignId)) {
                return campaigns.get(i);
            }
        }
        return null;
    }

    public AssetRef asset(String sha256) {
        for (int i = 0; i < assets.size(); i++) {
            if (assets.get(i).sha256.equals(sha256)) {
                return assets.get(i);
            }
        }
        return null;
    }

    /// The rules a mission of this pack is played under.
    public MatchRules rulesFor(String missionId) {
        MissionDef m = mission(missionId);
        if (m == null) {
            throw new IllegalArgumentException("no mission " + missionId);
        }
        return m.toRules(cardSet(), arena(m.arena));
    }

    /// A 64-bit FNV-1a hash of the pack's canonical text, in hex.
    public String hash() {
        return hashOf(PackCodec.write(this));
    }

    public static String hashOf(String canonical) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < canonical.length(); i++) {
            char c = canonical.charAt(i);
            h ^= c & 0xff;
            h *= 0x100000001b3L;
            h ^= (c >>> 8) & 0xff;
            h *= 0x100000001b3L;
        }
        String hex = Long.toHexString(h);
        StringBuilder sb = new StringBuilder();
        for (int i = hex.length(); i < 16; i++) {
            sb.append('0');
        }
        return sb.append(hex).toString();
    }

    /// A copy that shares nothing mutable with this one.
    public ContentPack copy() {
        return PackCodec.read(PackCodec.write(this));
    }
}

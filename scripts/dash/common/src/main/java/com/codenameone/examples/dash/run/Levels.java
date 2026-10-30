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
package com.codenameone.examples.dash.run;

import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.forge.ContentPack;
import com.codenameone.examples.dash.rules.forge.ContentRef;
import com.codenameone.examples.dash.rules.forge.LevelRun;
import com.codenameone.examples.dash.rules.forge.PackCodec;
import com.codenameone.examples.dash.rules.forge.Samples;
import java.util.HashMap;
import java.util.Map;

/// The levels this device can play, by the name a replay or a race gives them:
/// a bare id for an official level, `pack@hash#level` for a community one.
///
/// Community packs arrive from the server ([#remember]) when a browser screen
/// opens one, a party picks one or a replay names one; a reference to a pack
/// that was never fetched resolves to null, and the caller fetches it.
public final class Levels {
    /// A playable level and the catalog its placements name.
    public static final class Playable {
        public final String ref;
        public final LevelDef level;
        public final Catalog catalog;

        Playable(String ref, LevelDef level, Catalog catalog) {
            this.ref = ref;
            this.level = level;
            this.catalog = catalog;
        }
    }

    private static final Map<String, ContentPack> PACKS = new HashMap<String, ContentPack>();

    private Levels() {
    }

    /// Keeps `pack` for later references to it; returns it.
    public static synchronized ContentPack remember(ContentPack pack) {
        PACKS.put(pack.id + "@" + pack.hash(), pack);
        PACKS.put(pack.id, pack);
        return pack;
    }

    /// Decodes a pack the server sent and keeps it.
    public static ContentPack remember(String content) {
        return remember(PackCodec.read(content));
    }

    public static Playable official(String levelId) {
        LevelDef l = Content.level(levelId);
        return l == null ? null : new Playable(levelId, l, Content.catalog());
    }

    /// `levelId` of `pack`, named the way a replay names it.
    public static Playable of(ContentPack pack, String levelId) {
        if (Samples.OFFICIAL_ID.equals(pack.id)) {
            return official(levelId);
        }
        LevelDef l = pack.level(levelId);
        return l == null ? null : new Playable(LevelRun.replayLevelId(pack, levelId), l, pack.catalog());
    }

    /// A level of a pack still being edited, for a playtest. Its reference
    /// names the pack as it is right now, which the server has never
    /// published, so a playtest is never submitted.
    public static Playable draft(ContentPack pack, LevelDef level) {
        return new Playable(pack.id + "@" + pack.hash() + "#" + level.id, level, pack.catalog());
    }

    /// The level a replay or race calls `ref`, or null when its pack is not
    /// here yet (see [#packOf]).
    public static synchronized Playable resolve(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.indexOf('@') < 0) {
            return official(ref);
        }
        ContentRef r = ContentRef.parse(ref);
        ContentPack p = PACKS.get(r.pack + "@" + r.hash);
        return p == null ? null : of(p, r.level);
    }

    /// The pack id a community reference names, for fetching it; null for an
    /// official level.
    public static String packOf(String ref) {
        int at = ref == null ? -1 : ref.indexOf('@');
        return at <= 0 ? null : ref.substring(0, at);
    }
}

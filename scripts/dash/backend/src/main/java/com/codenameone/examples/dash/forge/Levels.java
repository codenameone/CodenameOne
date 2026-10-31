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
package com.codenameone.examples.dash.forge;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.CityRoute;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.forge.ContentPack;
import com.codenameone.examples.dash.rules.forge.LevelRun;
import com.codenameone.examples.dash.rules.forge.Samples;
import java.io.IOException;

/// Turns the level names the API takes into something to run. A level is an
/// official id (`neon-causeway`) or `pack/level` for a published community
/// pack; a race always runs the pack's current version.
@Component
public class Levels {
    /// A level ready to run, with the id its replays carry.
    public static final class Resolved {
        public final String ref;
        public final String replayLevel;
        public final LevelDef level;
        public final Catalog catalog;

        Resolved(String ref, String replayLevel, LevelDef level, Catalog catalog) {
            this.ref = ref;
            this.replayLevel = replayLevel;
            this.level = level;
            this.catalog = catalog;
        }
    }

    private final Forge forge;

    public Levels(Forge forge) {
        this.forge = forge;
    }

    /// The level's canonical name, or null when it is not one the API takes.
    /// Cheap and lock free: it only checks the shape, [#resolve] does the rest.
    public String normalize(String ref) {
        if (ref == null || ref.length() == 0 || ref.length() > 160) {
            return null;
        }
        int slash = ref.indexOf('/');
        if (slash < 0) {
            return Content.level(ref) != null || isCity(ref) ? ref : null;
        }
        if (slash == 0 || slash == ref.length() - 1 || ref.indexOf('/', slash + 1) >= 0) {
            return null;
        }
        return ref;
    }

    /// Whether `ref` is the id of a city route's level: [CityRoute#id]'s
    /// shape. The route itself is only ever in a result, so the server has
    /// boards for a route but cannot build its level from the id alone.
    static boolean isCity(String ref) {
        if (!ref.startsWith(CityRoute.ID_PREFIX) || ref.length() == CityRoute.ID_PREFIX.length()
                || ref.length() > CityRoute.ID_PREFIX.length() + 16) {
            return false;
        }
        for (int i = CityRoute.ID_PREFIX.length(); i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        return true;
    }

    /// The level `ref` names, or null -- for a city route too, whose level is
    /// built from a route the server does not keep.
    public Resolved resolve(String ref) throws IOException {
        String r = normalize(ref);
        if (r == null || isCity(r)) {
            return null;
        }
        int slash = r.indexOf('/');
        if (slash < 0) {
            return new Resolved(r, r, Content.level(r), Content.catalog());
        }
        String packId = r.substring(0, slash);
        String levelId = r.substring(slash + 1);
        ContentPack pack = forge.published(packId);
        LevelDef level = pack == null ? null : pack.level(levelId);
        if (level == null) {
            return null;
        }
        Catalog catalog = Samples.OFFICIAL_ID.equals(pack.id) ? Content.catalog() : pack.catalog();
        return new Resolved(r, LevelRun.replayLevelId(pack, levelId), level, catalog);
    }
}

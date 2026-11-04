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
package com.codenameone.examples.dash.rules.forge;

import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.LevelDef;
import java.util.ArrayList;
import java.util.List;

/// The checks a pack passes before it is published: shape and limits here, each
/// level through the rules' own [com.codenameone.examples.dash.rules.Validator],
/// and every level proven beatable by a bot. The editor runs the same checks as
/// the author types; the server runs them again and trusts nothing else.
public final class Validator {
    public static final int MAX_LEVELS = 30;
    public static final int MAX_DEFS = 40;
    public static final int MAX_ASSETS = 40;
    public static final int MAX_PLACEMENTS = 2000;
    public static final int MAX_TEXT = 280;
    public static final int MAX_LONG_TEXT = 1200;
    public static final int MAX_MODEL_BYTES = 5 * 1024 * 1024;
    public static final int MAX_IMAGE_BYTES = 1024 * 1024;

    private Validator() {
    }

    /// Every problem with the pack, errors first; empty when it may be published.
    /// Warnings from the level checks are included with a `warning:` prefix and
    /// do not block publishing ([#isValid]).
    public static List<String> check(ContentPack p) {
        List<String> errors = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();
        if (!isSlug(p.id, 3, 40)) {
            errors.add("The pack id is 3 to 40 lower-case letters, digits or hyphens");
        }
        if (p.name.trim().length() == 0 || p.name.length() > 60) {
            errors.add("The pack needs a name of up to 60 characters");
        }
        if (p.description.length() > MAX_LONG_TEXT) {
            errors.add("The description is too long");
        }
        if (p.levels.isEmpty()) {
            errors.add("A pack needs at least one level");
        }
        if (p.levels.size() > MAX_LEVELS || p.defs.size() > MAX_DEFS || p.assets.size() > MAX_ASSETS) {
            errors.add("A pack holds at most " + MAX_LEVELS + " levels, " + MAX_DEFS + " definitions and "
                    + MAX_ASSETS + " files");
        }
        for (String ref : p.art.values()) {
            if (!ref.startsWith("asset:") || p.asset(ref.substring(6)) == null) {
                errors.add("Art " + ref + " is not one of the pack's files");
            }
        }
        Catalog catalog = p.catalog();
        List<String> seen = new ArrayList<String>();
        for (int i = 0; i < p.levels.size(); i++) {
            LevelDef d = p.levels.get(i);
            String name = "Level " + (d.name == null ? d.id : d.name) + ": ";
            if (!isSlug(d.id, 1, 32) || seen.contains(d.id)) {
                errors.add(name + "needs a unique id of lower-case letters, digits or hyphens");
            }
            seen.add(d.id);
            int placements = d.placements.size();
            for (int c = 0; c < d.chunks.size(); c++) {
                placements += d.chunks.get(c).placements().size();
            }
            if (placements > MAX_PLACEMENTS) {
                errors.add(name + "has more than " + MAX_PLACEMENTS + " placements");
                continue;
            }
            List<com.codenameone.examples.dash.rules.Validator.Problem> ps =
                    com.codenameone.examples.dash.rules.Validator.check(d, catalog);
            for (int k = 0; k < ps.size(); k++) {
                if (ps.get(k).error) {
                    errors.add(name + ps.get(k).message);
                } else {
                    warnings.add("warning: " + name + ps.get(k).message);
                }
            }
            if (!com.codenameone.examples.dash.rules.Validator.hasErrors(ps)
                    && !com.codenameone.examples.dash.rules.Validator.beatable(d, catalog)) {
                errors.add(name + "the bot could not finish it; leave a way through");
            }
        }
        errors.addAll(warnings);
        return errors;
    }

    public static boolean isValid(ContentPack p) {
        return !hasErrors(check(p));
    }

    public static boolean hasErrors(List<String> problems) {
        for (int i = 0; i < problems.size(); i++) {
            if (!problems.get(i).startsWith("warning: ")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSlug(String s, int min, int max) {
        if (s == null || s.length() < min || s.length() > max) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-')) {
                return false;
            }
        }
        return true;
    }
}

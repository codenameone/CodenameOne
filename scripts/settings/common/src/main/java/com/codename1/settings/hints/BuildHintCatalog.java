/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.settings.hints;

import com.codename1.build.shared.BuildHints;
import com.codename1.build.shared.HintType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The build hints the Settings tool offers for editing.
 *
 * <p>Built from {@link BuildHints}, the same table the {@code @Ios} / {@code @Android}
 * annotations are generated from and the same one the builders' drift gate checks. It
 * used to be scraped out of the developer guide's AsciiDoc table at runtime, with the
 * type guessed by string-matching the description prose -- so a hint the guide did not
 * mention was invisible here, and one whose wording changed silently changed type.</p>
 */
public final class BuildHintCatalog {
    private final Map<String, BuildHintMetadata> hints = new LinkedHashMap<String, BuildHintMetadata>();

    public Collection<BuildHintMetadata> all() {
        return hints.values();
    }

    public BuildHintMetadata get(String name) {
        return hints.get(name);
    }

    public boolean contains(String name) {
        return hints.containsKey(name);
    }

    public List<BuildHintMetadata> search(String query) {
        ArrayList<BuildHintMetadata> out = new ArrayList<BuildHintMetadata>();
        for (BuildHintMetadata hint : hints.values()) {
            if (hint.matches(query)) {
                out.add(hint);
            }
        }
        return out;
    }

    public void add(BuildHintMetadata hint) {
        if (hint != null && hint.name() != null && hint.name().trim().length() > 0) {
            hints.put(hint.name().trim(), hint);
        }
    }

    /**
     * Every hint the catalog describes, including the ones consumed only by the
     * build service. Dynamic families such as {@code android.permission.<NAME>}
     * are left out: their names are patterns rather than keys, so there is
     * nothing for the editor to set.
     */
    public static BuildHintCatalog load() {
        BuildHintCatalog catalog = new BuildHintCatalog();
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (h.isDynamic()) {
                continue;
            }
            catalog.add(new BuildHintMetadata(
                    h.name(),
                    h.doc(),
                    toSettingsType(h.type()),
                    h.platform(),
                    h.values(),
                    h.def(),
                    annotationOf(h)));
        }
        return catalog;
    }

    private static String annotationOf(BuildHints.Hint h) {
        if (!h.isAnnotated()) {
            return null;
        }
        return "@" + h.group().annotationSimpleName() + "(" + h.attr() + ")";
    }

    /**
     * Maps the catalog's type to this tool's vocabulary. Derived rather than
     * duplicated so the two cannot drift apart again.
     */
    private static BuildHintType toSettingsType(HintType type) {
        try {
            return BuildHintType.valueOf(BuildHints.settingsType(type));
        } catch (IllegalArgumentException ex) {
            return BuildHintType.TEXT;
        }
    }
}

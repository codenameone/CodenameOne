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
package com.codename1.hardening;

/**
 * The hardening level a build requested, from the {@code harden.level} build hint.
 * The level is the primary control; individual {@code harden.*} switches override
 * what a level turns on. Each level is a superset of the previous one.
 */
public enum HardeningProfile {
    /** No transform runs; the input jar is returned untouched. */
    OFF,
    /** Class/method/field renaming plus encryption of constant strings. */
    STANDARD,
    /** Adds encryption of all strings and control-flow obfuscation (opaque predicates). */
    AGGRESSIVE,
    /**
     * Raises control-flow obfuscation intensity on top of aggressive: two opaque-predicate
     * guards per eligible method instead of one ({@link HardeningConfig#getControlFlowIntensity()}).
     * It adds no new kind of transform -- in particular there is no reflective-name hiding, since
     * Codename One has no runtime reflection for such names to feed.
     */
    PARANOID;

    /** Parses a level name case-insensitively; returns {@code null} for an unknown value. */
    public static HardeningProfile parse(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim().toUpperCase();
        if (v.isEmpty()) {
            return null;
        }
        for (HardeningProfile p : values()) {
            if (p.name().equals(v)) {
                return p;
            }
        }
        return null;
    }

    public boolean isAtLeast(HardeningProfile other) {
        return ordinal() >= other.ordinal();
    }

    /** Renaming applies at STANDARD and above. */
    public boolean renamesByDefault() {
        return isAtLeast(STANDARD);
    }

    /** STANDARD encrypts constant strings only; AGGRESSIVE and up encrypt all strings. */
    public boolean encryptsAllStringsByDefault() {
        return isAtLeast(AGGRESSIVE);
    }

    public boolean encryptsConstantStringsByDefault() {
        return isAtLeast(STANDARD);
    }

    public boolean controlFlowByDefault() {
        return isAtLeast(AGGRESSIVE);
    }
}

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
package com.codename1.annotations.buildhints;

/// Accepted values of the `harden.level` build hint.
///
/// Each constant carries the string the build actually receives, which is not
/// always the constant's own name.
///
/// Generated from com.codename1.build.shared.BuildHints by
/// BuildHintCodeGenerator. Do not edit by hand -- edit the catalog and
/// re-run scripts/gen-build-hint-annotations.sh.
public enum HardenLevel {
    OFF("off"),
    STANDARD("standard"),
    AGGRESSIVE("aggressive"),
    PARANOID("paranoid");

    private final String wire;

    HardenLevel(String wire) {
        this.wire = wire;
    }

    /// The value written into the build hint.
    public String wireValue() {
        return wire;
    }
}

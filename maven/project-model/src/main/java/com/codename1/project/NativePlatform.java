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
package com.codename1.project;

/// A platform that can carry native-interface implementations, with the
/// directory name and source language each build tool uses for it.
public enum NativePlatform {
    /// The simulator and desktop builds.
    JAVASE("javase", "java"),
    /// Android.
    ANDROID("android", "java"),
    /// iOS, Objective-C and Swift sources.
    IOS("ios", "objectivec"),
    /// The JavaScript port.
    JAVASCRIPT("javascript", "javascript"),
    /// The native Windows port, plain C.
    WIN("win", "c"),
    /// The native Linux port, plain C.
    LINUX("linux", "c");

    private final String id;
    private final String language;

    NativePlatform(String id, String language) {
        this.id = id;
        this.language = language;
    }

    /// The platform's name as it appears in directory names and in the
    /// `codename1.platform` property, for example `android`.
    public String id() {
        return id;
    }

    /// The source directory name under a platform directory, for example
    /// `objectivec` for iOS.
    public String language() {
        return language;
    }

    /// The platform whose [id()] is `id`, or null when there is none.
    public static NativePlatform fromId(String id) {
        for (NativePlatform p : values()) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }
}

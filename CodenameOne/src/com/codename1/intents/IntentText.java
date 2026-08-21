/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.intents;

/// The one definition of what makes a name a name.
///
/// This rule was restated at every door that carries user-visible text, and each restatement
/// tested for *empty* rather than for *blank*. A title of spaces is present, so every one of
/// those fallbacks declined to fire and the spaces travelled all the way out: as an Android
/// shortcut label, an iOS `NSUserActivity.title`, a Spotlight entry, an entity picker's type
/// name, a `@Parameter` prompt. Each is a launcher or suggestion entry the user can see and
/// cannot read, and none of them points back at the declaration that caused it.
///
/// Four separate review findings were four instances of that one rule, so it lives in one
/// place now and every door calls it.
final class IntentText {

    private IntentText() {
    }

    /// The text if it is text, otherwise the fallback.
    ///
    /// #### Parameters
    ///
    /// - `value`: the supplied text, which may be null, empty or whitespace
    /// - `fallback`: what to use instead; null when "no name" is the right answer
    static String orFallback(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }
}

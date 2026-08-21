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
package com.codenameone.examples.hellocodenameone;

import com.codename1.annotations.AppIntent;
import com.codename1.annotations.IntentParam;
import com.codename1.intents.IntentResult;

/// The app intents this sample declares, so CI compiles what the build generates for them.
///
/// Declaring these is the coverage. Without an `@AppIntent` anywhere in the project the iOS
/// builder writes no Swift at all, so `CN1AppIntents.swift`, `CN1AppEntities.swift`, the
/// `AppShortcutsProvider` and the `CN1IntentHost` shim are never compiled by Xcode in CI, and
/// the Android builder writes no `cn1_shortcuts.xml` for AAPT to reject. Every generated-code
/// mistake in that half of the feature -- a Swift identifier that collides with a member the
/// generator itself emits, a resource name AAPT will not take, a label that needs escaping --
/// is a build error that only appears once something declares an intent.
///
/// The set is chosen to cover the shapes that generate differently rather than to be a
/// realistic app: a closed vocabulary (an `AppEnum`), an optional parameter with a default, an
/// entity parameter (an `EntityQuery` and a key-path phrase), a destructive intent (the
/// confirmation path), and one that takes nothing at all -- which is the only shape Android
/// will publish as a static launcher shortcut.
public class IntentsDemo {

    private IntentsDemo() {
    }

    /// Counts notes. Parameterless, discoverable and not destructive, which is exactly the
    /// shape AndroidGradleBuilder publishes into `cn1_shortcuts.xml` -- so this is the
    /// declaration that gets the Android resource path compiled by AAPT.
    ///
    /// The apostrophe in the title is deliberate. A string resource value needs Android's own
    /// escaping on top of XML escaping, and an unescaped apostrophe fails the APK build; this
    /// makes CI prove that rather than leaving it to a unit test's idea of what AAPT does.
    @AppIntent(value = "cn1_note_count", title = "Count today's notes", headless = true,
            timeoutSeconds = 5,
            phrases = {"Count notes in ${applicationName}"})
    public static IntentResult countNotes() {
        int count = DemoNote.recent().size();
        return IntentResult.value(Integer.valueOf(count))
                .withDialog("There are " + count + " notes.");
    }

    /// A closed vocabulary and an optional parameter with a declared default: the first
    /// generates a Swift `AppEnum`, the second is what the coercion substitutes when the
    /// platform sends nothing.
    @AppIntent(value = "cn1_log_note", title = "Log a note", headless = true, timeoutSeconds = 5,
            phrases = {"Log a note in ${applicationName}"})
    public static IntentResult logNote(
            @IntentParam(value = "kind", title = "What kind of note?",
                    options = {"idea", "todo", "reminder"}) String kind,
            @IntentParam(value = "minutes", title = "How many minutes?", required = false,
                    defaultValue = "5") int minutes) {
        return IntentResult.value(kind + "/" + minutes)
                .withDialog("Logged a " + minutes + " minute " + kind + ".");
    }

    /// An entity parameter. The phrase names it and nothing else, because Apple accepts at most
    /// one parameter per phrase and only an entity in that position -- so this also covers the
    /// key-path interpolation the generator emits for it.
    @AppIntent(value = "cn1_open_note", title = "Open a note",
            phrases = {"Open ${note} in ${applicationName}"})
    public static IntentResult openNote(
            @IntentParam(value = "note", title = "Which note?") DemoNote note) {
        return IntentResult.value(note.getId())
                .withDialog("Opening " + note.getTitle() + ".");
    }

    /// Destructive, which is the branch that generates a confirmation before the handler runs.
    @AppIntent(value = "cn1_wipe_notes", title = "Delete all notes", destructive = true,
            headless = true, timeoutSeconds = 5)
    public static IntentResult wipeNotes() {
        return IntentResult.spoken("Deleted every note.");
    }
}

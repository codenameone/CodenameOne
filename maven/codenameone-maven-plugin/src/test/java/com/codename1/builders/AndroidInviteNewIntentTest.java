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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An App Link delivered to a resumed activity has to reach the invite code.
 *
 * <p>{@code onNewIntent} stores the url in {@code AppArg} and the port stops
 * there. The generated lifecycle returns early when {@code wasStopped} is false
 * -- it re-shows the current form and nothing else -- so the documented
 * {@code checkForInvite()} call in the application's {@code start()} never
 * runs, and the next {@code onStop()} clears the property again. The delivery
 * worked and the invite was silently lost: no claim, no {@code invite_opened}.
 * The stub therefore overrides {@code onNewIntent} and consumes it.</p>
 *
 * <p>Generated rather than done in the port, and that is the load-bearing part:
 * {@code AndroidImplementation} referencing {@code com.codename1.analytics.invite}
 * would make {@code PlatformFeatureCatalog} match the prefix for EVERY
 * application, putting a Play Install Referrer dependency and an API 21 floor
 * on apps that never heard of invites. That is the {@code DatabaseConfig} bug,
 * already fixed once. So the reference may only exist inside code emitted for
 * an app whose own classes use invites.</p>
 *
 * <p>Asserted against the builder's source text, as {@code StubLifecycleCastTest}
 * does: the stub is assembled inline across a few hundred lines with no seam to
 * call, and what has to stay true is a property of the assembly.</p>
 */
public class AndroidInviteNewIntentTest {

    private static final String BUILDER =
            "src/main/java/com/codename1/builders/AndroidGradleBuilder.java";

    /** The port source, relative to the plugin module the tests run in. */
    private static final String ANDROID_PORT =
            "../../Ports/Android/src/com/codename1/impl/android/AndroidImplementation.java";

    private String source() throws IOException {
        File builder = new File(BUILDER);
        assertTrue(builder.isFile(), "the builder must be readable: " + builder.getAbsolutePath());
        return new String(Files.readAllBytes(builder.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    void theStubOverridesOnNewIntentAndConsumesTheInvite() throws IOException {
        String source = source();
        int declared = source.indexOf("String inviteNewIntent = \"\";");
        assertTrue(declared > 0, "the onNewIntent splice is gone, so a resumed App Link is lost");
        assertTrue(source.contains("protected void onNewIntent(android.content.Intent intent)"),
                "the generated stub no longer overrides onNewIntent");
        assertTrue(source.contains("com.codename1.analytics.invite.Invites.checkForInvite();"),
                "the generated onNewIntent no longer consumes the invite");
        assertTrue(source.contains("+ inviteNewIntent"),
                "the splice is built and never emitted into the stub");
    }

    @Test
    void theOverrideRunsOnTheEventDispatchThread() throws IOException {
        String source = source();
        int splice = source.indexOf("String inviteNewIntent = \"\";");
        int end = source.indexOf("String inviteRegisterInstall", splice);
        assertTrue(splice > 0 && end > splice, "the splice block moved");
        String block = source.substring(splice, end);
        // onNewIntent runs on Android's UI thread, not the Codename One EDT.
        assertTrue(block.contains("Display.getInstance().callSerially("),
                "the generated onNewIntent touches invite state off the EDT");
        assertTrue(block.contains("if(!Display.isInitialized())"),
                "the generated onNewIntent can run before Display exists");
    }

    @Test
    void theConsumedUrlIsClearedOnAcopyNotOnTheCallersIntent() throws IOException {
        // dispatchNewIntentUrl runs from CodenameOneActivity.onNewIntent, and
        // the ordinary way to extend that is super.onNewIntent(intent) followed
        // by the subclass reading intent.getData(). Clearing the data on THAT
        // object set it to null underneath the override, so custom deep-link
        // routing that worked before lost the url entirely.
        File port = new File(ANDROID_PORT);
        assertTrue(port.isFile(), "the port must be readable: " + port.getAbsolutePath());
        String source = new String(Files.readAllBytes(port.toPath()), StandardCharsets.UTF_8);
        int at = source.indexOf("static void dispatchNewIntentUrl(");
        assertTrue(at > 0, "dispatchNewIntentUrl is gone");
        String block = source.substring(at, source.indexOf("\n    }", at));
        assertTrue(!block.contains("intent.setData(null)"),
                "the caller's intent is mutated, so a subclass reading it after "
                        + "super.onNewIntent() finds no data");
        assertTrue(block.contains("new android.content.Intent(intent)")
                        && block.contains("consumed.setData(null)"),
                "the url is no longer consumed on a copy");
    }

    @Test
    void standardLaunchModeIsWarnedAboutRatherThanRefused() throws IOException {
        // It refused the build outright until a review round pointed at the
        // generated stub's `private Form currentForm` -- an INSTANCE field. A
        // standard-mode App Link starts a SECOND activity, whose copy of that
        // field is null, so wasStopped is true and the generated run() reaches
        // createStartInvocation(): the application's start() runs and reads the
        // link out of getAppArg() exactly as on a cold launch. The invite is
        // delivered, and refusing rejected a configuration the app already
        // built and shipped with.
        String source = source();
        int guard = source.indexOf("\"standard\".equals(launchMode)");
        assertTrue(guard > 0, "the launch-mode guard is gone");
        int end = source.indexOf("\n            }", guard);
        assertTrue(end > guard, "the guard block moved");
        String block = source.substring(guard, end);
        assertTrue(block.contains("warn("),
                "a working launch mode is refused instead of warned about");
        assertTrue(!block.contains("throw new BuildException"),
                "standard launch mode still fails the build");
    }

    @Test
    void theInviteReferenceOnlyExistsForAppsThatUseInvites() throws IOException {
        String source = source();
        int splice = source.indexOf("String inviteNewIntent = \"\";");
        int gate = source.indexOf("if (usesInvites) {", splice);
        int body = source.indexOf("com.codename1.analytics.invite.Invites.checkForInvite();",
                splice);
        assertTrue(gate > splice && gate < body,
                "the onNewIntent splice is emitted for every app, which puts a Play Install "
                        + "Referrer dependency and an API 21 floor on all of them");
    }
}

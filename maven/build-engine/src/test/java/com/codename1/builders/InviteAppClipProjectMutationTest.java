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
 * The App Clip has to survive the two things the Xcode project script does to
 * every target, and neither failure says anything at build time.
 *
 * <p>Asserted against the builder's source text, as {@code StubLifecycleCastTest}
 * and {@code AndroidInviteNewIntentTest} do: both properties are of an inline
 * assembly a few hundred lines long with no seam to call, and the cost of
 * getting either wrong is a clip that is simply absent or does not launch --
 * with an app that builds, signs and ships.</p>
 */
public class InviteAppClipProjectMutationTest {

    private static final String BUILDER =
            "src/main/java/com/codename1/builders/IPhoneBuilder.java";

    private String source() throws IOException {
        File builder = new File(BUILDER);
        assertTrue(builder.isFile(), "the builder must be readable: " + builder.getAbsolutePath());
        return new String(Files.readAllBytes(builder.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    void wantingAclipIsEnoughToMutateTheProject() throws IOException {
        // The clip target is created inside the needsXcodeProjectMutation
        // block. Without the flag in that condition, an invite-enabled app
        // that uses no pods and no other extension -- the DEFAULT shape of an
        // app that just switched invites on -- built with no clip at all, and
        // every iOS install settled as no_match for ever.
        String source = source();
        int at = source.indexOf("boolean needsXcodeProjectMutation =");
        assertTrue(at > 0, "the mutation gate is gone");
        String condition = source.substring(at, source.indexOf(";", at));
        assertTrue(condition.contains("inviteAppClipTargetWanted"),
                "an invite-only iOS build does not enter the block that creates its "
                        + "App Clip, so the clip is never generated or embedded");
    }

    @Test
    void theGlobalDeploymentPassLeavesTheClipAlone() throws IOException {
        // fix_xcode_schemes.rb runs twice -- again after pods integration --
        // and the global pass rewrites IPHONEOS_DEPLOYMENT_TARGET on every
        // target it does not skip. The clip's settings are appended after that
        // pass, which covers the first run only: on the second the target
        // already exists, so the guard that stops it being created twice also
        // skips restoring its floor. An App Clip below iOS 14 does not launch.
        String source = source();
        int at = source.indexOf("deploymentTargetStr = \"begin");
        assertTrue(at > 0, "the global deployment-target pass moved");
        String pass = source.substring(at, source.indexOf("rescue => e", at));
        assertTrue(pass.contains("InviteAppClipBuilder.PRODUCT_TYPE"),
                "the pass does not skip the App Clip, so a second run drops it to the "
                        + "app's deployment target");
    }
}

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

import com.codename1.util.InviteAppClipBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The invite App Clip is its own target with a fixed iOS 14 minimum, so the SDK floor the
/// app target is raised to must reach it separately: Xcode 27 (floor 15.0) rejected every
/// project with the clip in target CN1InviteClip while the app target built.
class InviteAppClipDeploymentFloorTest {

    @Test
    void xcode27FloorRaisesTheClip() {
        assertEquals("15.0", IPhoneBuilder.inviteAppClipDeploymentTarget("15.0"));
    }

    @Test
    void olderXcodeLeavesTheClipAtItsOwnMinimum() {
        assertEquals(InviteAppClipBuilder.DEPLOYMENT_TARGET,
                IPhoneBuilder.inviteAppClipDeploymentTarget("12.0"));
    }

    @Test
    void unknownFloorLeavesTheClipAlone() {
        assertEquals(InviteAppClipBuilder.DEPLOYMENT_TARGET,
                IPhoneBuilder.inviteAppClipDeploymentTarget(null));
        assertEquals(InviteAppClipBuilder.DEPLOYMENT_TARGET,
                IPhoneBuilder.inviteAppClipDeploymentTarget(""));
    }
}

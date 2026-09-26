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
package com.codename1.maven;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The release gate routes a build to one daemon release's canary queue through
 * {@code -Dcodename1.ant.<platform>.targetType}. Only the queue may be steered.
 */
public class CN1BuildMojoReleaseGateQueueTest {

    @Test
    public void passesTheQueueToBuildXmlWithoutThePrefix() {
        Properties system = new Properties();
        system.setProperty("codename1.ant.android.targetType", "debug_gate_android_builddaemon-master-x");
        Properties out = CN1BuildMojo.releaseGateQueueOverrides(system);
        assertEquals("debug_gate_android_builddaemon-master-x", out.getProperty("android.targetType"));
        assertEquals(1, out.size());
    }

    @Test
    public void ignoresEverythingThatIsNotAQueue() {
        Properties system = new Properties();
        system.setProperty("codename1.ant.dist.jar", "/tmp/other.jar");
        system.setProperty("codename1.ant.CodeNameOneBuildClient.jar", "/tmp/evil.jar");
        system.setProperty("codename1.ant..targetType", "x");
        system.setProperty("android.targetType", "unprefixed");
        system.setProperty("user.home", "/home/x");
        assertTrue(CN1BuildMojo.releaseGateQueueOverrides(system).isEmpty());
    }
}

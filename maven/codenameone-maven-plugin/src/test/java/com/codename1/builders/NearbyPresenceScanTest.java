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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Presence observation is recognised from the START call alone.
 *
 * <p>The manifest an observing app gets is bigger than the one an
 * associating app gets: an exported companion service and the background
 * companion permissions. Which one an app receives turns entirely on this
 * one classification, and the scanner rule lives in an anonymous visitor
 * callback with no seam to call -- so this pins the rule by source text,
 * the way {@code HealthScannerParityTest} does.</p>
 *
 * <p>The rule it pins: {@code stopObservingPresence} is a cleanup call. An
 * app version that dropped observation still makes it, to undo an
 * observation a previous version persisted, and a substring match on
 * {@code ObservingPresence} classified that app as observing -- keeping
 * the service and the permissions in the manifest of an app that starts
 * no observation at all.</p>
 */
public class NearbyPresenceScanTest {

    private static String scanner() throws Exception {
        File f = new File("src/main/java/com/codename1/builders/"
                + "AndroidGradleBuilder.java");
        assertTrue(f.exists(), "scanner source must be readable: "
                + f.getAbsolutePath());
        return new String(Files.readAllBytes(f.toPath()),
                StandardCharsets.UTF_8);
    }

    @Test
    public void presenceIsMatchedOnTheStartCallExactly() throws Exception {
        String src = scanner();
        assertTrue(src.contains("\"startObservingPresence\".equals(method)"),
                "presence observation must be recognised from"
                + " startObservingPresence by exact name");
        assertFalse(src.contains("method.contains(\"ObservingPresence\")"),
                "a substring match also classifies stopObservingPresence"
                + " as observing");
    }

    /**
     * Touching the facade at all is still companion use. Only the presence
     * half is gated on the start call; an app that merely associates must
     * keep its companion feature and its association permissions.
     */
    @Test
    public void anyCompanionCallStillCountsAsCompanionUse() throws Exception {
        String src = scanner();
        int at = src.indexOf("\"startObservingPresence\".equals(method)");
        assertTrue(at > 0, "the presence rule must be present");
        String before = src.substring(Math.max(0, at - 1200), at);
        assertTrue(before.contains("usesNearbyCompanion = true;"),
                "companion use must be set for any CompanionDevices call,"
                + " not only for the observing one");
    }
}

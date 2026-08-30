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
package com.codename1.call;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every port answers a missing screening role with {@code false}, not an error.
 *
 * <p>{@code CallDirectory.requestScreeningRole()} documents it in one line --
 * "Resolves false where the role was refused <b>or does not exist</b>" -- and
 * that second clause is the whole of what this guards. iOS has no screening
 * role at all and Android has none below API 29, so on both the answer is
 * permanently no; routing it through {@code deliverAck} turns the documented
 * {@code false} into a {@code CallException}, and portable code that handles
 * denial in its success callback breaks on exactly the platforms where denial
 * is the only possible outcome.</p>
 *
 * <p>Read from source because neither bridge can run here: an iOS bridge needs
 * a device and the Android one needs a framework. That makes this a shape
 * check rather than a behaviour check, and it is deliberately narrow -- it
 * asserts which delivery call each refusal path uses, which is the single
 * decision that was wrong.</p>
 *
 * <p>Skipped rather than failed when a port is not in the tree, which is how
 * this module is built in some checkouts; CI has the whole repository.</p>
 */
class ScreeningRoleContractParityTest {

    private static final String IOS_BRIDGE =
            "../../Ports/iOSPort/src/com/codename1/impl/ios/IOSCallBridge.java";

    private static final String ANDROID_BRIDGE =
            "../../Ports/Android/src/com/codename1/impl/android/call/"
            + "AndroidCallBridge.java";

    static boolean portsPresent() {
        return new File(IOS_BRIDGE).exists() && new File(ANDROID_BRIDGE).exists();
    }

    private static String requestScreeningRoleBody(String path)
            throws Exception {
        String src = new String(Files.readAllBytes(new File(path).toPath()),
                StandardCharsets.UTF_8);
        int at = src.indexOf("public void requestScreeningRole(int requestId)");
        assertTrue(at >= 0, path + " has to implement requestScreeningRole");
        // To the next method declaration, which is enough: every refusal this
        // is about returns before then.
        int end = src.indexOf("\n    }\n", at);
        assertTrue(end > at, "the method has to end");
        return code(src.substring(at, end));
    }

    /**
     * The source with its comment lines removed.
     *
     * <p>The checks below look for constructs that must not appear, and the
     * comments explaining why they must not appear name them -- so an
     * assertion run over the raw text fails on the note describing the very
     * bug it guards against.</p>
     */
    private static String code(String src) {
        StringBuilder sb = new StringBuilder();
        String[] lines = src.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().startsWith("//")) {
                sb.append(lines[i]).append('\n');
            }
        }
        return sb.toString();
    }

    @Test
    @EnabledIf("portsPresent")
    void iosResolvesFalseBecauseTheRoleDoesNotExistThere() throws Exception {
        // iOS enables caller identification in Settings and gives an app no
        // way to ask, so "no" is the only answer this can ever produce. It
        // used to produce NOT_SUPPORTED, which is an exception on the one
        // platform whose answer is never going to change.
        String body = requestScreeningRoleBody(IOS_BRIDGE);
        assertTrue(body.contains("deliverAckValue"),
                "iOS has to resolve the documented false");
        assertFalse(body.contains("NOT_SUPPORTED"),
                "and must not report the permanent answer as a failure");
    }

    @Test
    @EnabledIf("portsPresent")
    void androidResolvesFalseWhereTheRoleDoesNotExistEither() throws Exception {
        // Below API 29 there is no role to be granted, which is the same
        // "does not exist" the contract names -- so it gets the same answer
        // as iOS rather than the failure it used to share with the
        // no-foreground-activity case.
        String body = requestScreeningRoleBody(ANDROID_BRIDGE);
        int unsupported = body.indexOf("!isDirectorySupported()");
        int value = body.indexOf("deliverAckValue");
        assertTrue(unsupported >= 0,
                "Android still has to test whether the role exists");
        assertTrue(value > unsupported,
                "and answer that case with a resolved false");
        // The other half stays a failure, and the distinction is the point:
        // no foreground activity means the role exists and could be granted,
        // this process simply cannot raise the dialog. Saying false there
        // would tell an app the user declined when nobody was asked.
        assertTrue(body.contains("deliverAck("),
                "while a missing activity stays a failure");
    }
}

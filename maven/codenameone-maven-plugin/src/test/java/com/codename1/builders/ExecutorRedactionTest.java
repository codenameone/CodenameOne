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

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * What the build log is allowed to contain.
 *
 * <p>{@code exec} appends every argument to the message handed back to the
 * customer and to the daemon's stdout, so a credential on a command line is
 * retained in both. These pin the redaction, and they exist because the first
 * version of it took argument INDICES and the caller counted one wrong: it
 * redacted the {@code -P} flag and printed the password beside it. A leak that
 * has been "fixed" and still leaks is worse than one nobody has touched, because
 * nobody looks at it twice.</p>
 */
public class ExecutorRedactionTest {

    private static String render(Set<String> secrets, String... args) {
        StringBuilder sb = new StringBuilder();
        for (String a : args) {
            sb.append(Executor.redactArg(secrets, a)).append(' ');
        }
        return sb.toString();
    }

    /** The exact command the keychain import runs. */
    @Test
    public void securityImportDoesNotPrintTheCertificatePassword() {
        String pass = "s3cr3t-p12-password";
        Set<String> secrets = Executor.redactionSet(new String[]{pass});
        String line = render(secrets, "security", "import", "/tmp/x.p12",
                "-k", "/tmp/kc.keychain-db", "-P", pass, "-T", "/usr/bin/codesign");
        assertTrue("the password must not survive into the log: " + line,
                line.indexOf(pass) < 0);
        // The shape is preserved, so the log still reads as the command it was.
        assertTrue(line.contains("-P ***"));
        assertTrue(line.contains("security import"));
    }

    /** The exact command notarization runs. */
    @Test
    public void notarytoolDoesNotPrintTheAppSpecificPassword() {
        String pass = "abcd-efgh-ijkl-mnop";
        Set<String> secrets = Executor.redactionSet(new String[]{pass});
        String line = render(secrets, "xcrun", "notarytool", "submit", "/tmp/a.dmg",
                "--apple-id", "dev@example.com", "--team-id", "ABCDEF1234",
                "--password", pass, "--wait");
        assertTrue("the password must not survive into the log: " + line,
                line.indexOf(pass) < 0);
        assertTrue(line.contains("--password ***"));
        // Everything that is not a secret is still legible -- a log nobody can
        // read is a log nobody uses.
        assertTrue(line.contains("dev@example.com"));
        assertTrue(line.contains("ABCDEF1234"));
    }

    /** A secret glued to its flag is still a secret. */
    @Test
    public void anAttachedSecretIsRedactedToo() {
        Set<String> secrets = Executor.redactionSet(new String[]{"hunter2"});
        assertEquals("-Phunter2 redacts to the flag plus ***",
                "-P***", Executor.redactArg(secrets, "-Phunter2"));
    }

    /**
     * An empty secret would match every empty argument and turn the command line
     * into noise, which teaches whoever reads the log to stop trusting it.
     */
    @Test
    public void emptyAndNullSecretsAreIgnored() {
        Set<String> secrets = Executor.redactionSet(new String[]{null, "", "real"});
        assertEquals(1, secrets.size());
        assertEquals("--wait", Executor.redactArg(secrets, "--wait"));
        assertEquals("***", Executor.redactArg(secrets, "real"));
    }

    /** No secrets at all leaves the command untouched. */
    @Test
    public void nothingIsRedactedWithoutSecrets() {
        Set<String> secrets = Executor.redactionSet(null);
        assertEquals("xcodebuild", Executor.redactArg(secrets, "xcodebuild"));
    }
}

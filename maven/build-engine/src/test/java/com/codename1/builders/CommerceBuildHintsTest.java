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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommerceBuildHintsTest {

    private static BuildRequest request(String... pairs) {
        BuildRequest r = new BuildRequest();
        for (int i = 0; i < pairs.length; i += 2) {
            r.putArgument(pairs[i], pairs[i + 1]);
        }
        return r;
    }

    private static String line(String name, String value) {
        return "        Display.getInstance().setProperty(\"" + name + "\", \"" + value + "\");\n";
    }

    @Test
    public void aBuildWithNoCommerceHintsStampsNothing() {
        assertEquals("", CommerceBuildHints.stubProperties(request()));
        assertEquals("", CommerceBuildHints.stubProperties(request("commerce.cloud.endpoint", "  ")));
    }

    @Test
    public void theSwitchIsNormalised() {
        assertEquals(line("commerce.cloud.enabled", "false"),
                CommerceBuildHints.stubProperties(request("commerce.cloud.enabled", " FALSE ")));
        assertEquals(line("commerce.cloud.enabled", "true"),
                CommerceBuildHints.stubProperties(request("commerce.cloud.enabled", "True")));
    }

    @Test
    public void aSwitchThatIsNotABooleanFailsTheBuild() {
        assertThrows(IllegalArgumentException.class,
                () -> CommerceBuildHints.stubProperties(request("commerce.cloud.enabled", "yes")));
    }

    @Test
    public void theEndpointLosesItsTrailingSlash() {
        assertEquals(line("commerce.cloud.endpoint", "https://c.example.com/api/v2/commerce"),
                CommerceBuildHints.stubProperties(
                        request("commerce.cloud.endpoint", " https://c.example.com/api/v2/commerce// ")));
    }

    @Test
    public void anEndpointThatCouldBreakTheGeneratedSourceFailsTheBuild() {
        String[] bad = {"c.example.com", "ftp://c.example.com", "https://", "https://c.example.com/\"x",
            "https://c.example.com/a b", "https://c.example.com/\\"};
        for (String value : bad) {
            assertThrows(IllegalArgumentException.class,
                    () -> CommerceBuildHints.stubProperties(request("commerce.cloud.endpoint", value)), value);
        }
    }

    @Test
    public void thePublicKeyIsStampedAsWritten() {
        String key = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo=";
        assertEquals(line("commerce.cloud.publicKey", key),
                CommerceBuildHints.stubProperties(request("commerce.cloud.publicKey", key)));
        assertThrows(IllegalArgumentException.class,
                () -> CommerceBuildHints.stubProperties(request("commerce.cloud.publicKey", "not a key\"")));
    }

    @Test
    public void everyHintSetIsStampedInAFixedOrder() {
        assertEquals(line("commerce.cloud.enabled", "true")
                + line("commerce.cloud.endpoint", "http://localhost:8080/api/v2/commerce"),
                CommerceBuildHints.stubProperties(request(
                        "commerce.cloud.endpoint", "http://localhost:8080/api/v2/commerce",
                        "commerce.cloud.enabled", "true")));
    }

    @Test
    void plainHttpIsOnlyForLoopback() {
        String[] ok = {"http://localhost/api", "http://LOCALHOST:8080/api", "http://127.0.0.1:9000",
            "http://10.0.2.2:8080/api", "http://[::1]:8080/api", "https://commerce.example.com/api"};
        for (String u : ok) {
            assertTrue(CommerceBuildHints.isUsableEndpoint(u), u);
        }
        String[] bad = {"http://commerce.example.com/api", "http://localhost.evil.com/api",
            "http://localhost@evil.com/api", "http://127.0.0.2/api", "http://10.0.2.3/api",
            "http://[::1]evil/api"};
        for (String u : bad) {
            assertFalse(CommerceBuildHints.isUsableEndpoint(u), u);
            assertThrows(IllegalArgumentException.class,
                    () -> CommerceBuildHints.stubProperties(request("commerce.cloud.endpoint", u)));
        }
    }
}

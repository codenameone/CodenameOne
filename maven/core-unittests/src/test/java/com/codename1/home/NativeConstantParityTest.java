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
package com.codename1.home;

import com.codename1.home.commissioning.CommissioningStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The iOS bridge's enum constants are these enums' ordinals.
 *
 * <p>Nothing crosses the smart-home wire as a name except errors: an availability, a
 * commissioning style and a value kind all travel as the ordinal the port wrote into
 * {@code CN1SmartHome.h} by hand. A constant appended to one of these enums, or reordered in it,
 * silently repoints every one of those defines -- and the failure is a device reporting the wrong
 * state, which no build and no simulator run can show. So the header is read here and compared.</p>
 *
 * <p>Skipped rather than failed when the iOS port is not in the tree, which is how this module is
 * built in some checkouts; CI has the whole repository.</p>
 */
class NativeConstantParityTest {

    private static final String HEADER =
            "../../Ports/iOSPort/nativeSources/CN1SmartHome.h";

    static boolean iosPortPresent() {
        return new File(HEADER).exists();
    }

    private static Map<String, Integer> defines(String prefix) throws Exception {
        String header = new String(Files.readAllBytes(new File(HEADER).toPath()),
                StandardCharsets.UTF_8);
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        Matcher m = Pattern.compile("#define\\s+" + Pattern.quote(prefix)
                + "([A-Z0-9_]+)\\s+(\\d+)").matcher(header);
        while (m.find()) {
            out.put(m.group(1), Integer.valueOf(m.group(2)));
        }
        assertTrue(out.size() > 1, "no " + prefix + "* defines found in " + HEADER);
        return out;
    }

    private static void assertMatches(String prefix, Enum<?>[] values) throws Exception {
        Map<String, Integer> defined = defines(prefix);
        for (Enum<?> value : values) {
            Integer ordinal = defined.get(value.name());
            assertNotNull(ordinal, prefix + value.name()
                    + " is missing from the iOS header; every constant in "
                    + value.getDeclaringClass().getSimpleName()
                    + " has to be spelled there, because the port answers with the ordinal");
            assertEquals(value.ordinal(), ordinal.intValue(),
                    prefix + value.name() + " is " + ordinal + " in the iOS header and "
                            + value.ordinal() + " in Java -- the port would report a"
                            + " different state than it means");
        }
        assertEquals(values.length, defined.size(),
                "the header defines " + defined.size() + " " + prefix + "* constants and Java has "
                        + values.length + ": " + defined.keySet());
    }

    @Test
    @EnabledIf("iosPortPresent")
    void availabilityOrdinalsMatchTheIosHeader() throws Exception {
        assertMatches("CN1_HOME_AVAIL_", HomeAvailability.values());
    }

    @Test
    @EnabledIf("iosPortPresent")
    void authorizationOrdinalsMatchTheIosHeader() throws Exception {
        assertMatches("CN1_HOME_AUTH_", HomeAuthorizationStatus.values());
    }

    @Test
    @EnabledIf("iosPortPresent")
    void valueKindOrdinalsMatchTheIosHeader() throws Exception {
        assertMatches("CN1_HOME_KIND_", TraitValueKind.values());
    }

    @Test
    @EnabledIf("iosPortPresent")
    void structureChangeOrdinalsMatchTheIosHeader() throws Exception {
        assertMatches("CN1_HOME_CHANGE_", StructureChangeKind.values());
    }

    @Test
    @EnabledIf("iosPortPresent")
    void commissioningStyleOrdinalsMatchTheIosHeader() throws Exception {
        assertMatches("CN1_HOME_COMMISSION_", CommissioningStyle.values());
    }
}

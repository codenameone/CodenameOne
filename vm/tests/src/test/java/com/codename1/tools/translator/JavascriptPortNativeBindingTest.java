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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/// Every `native` method in the browser vault has a `bindNative` entry under the exact name the
/// translator will emit for it.
///
/// This is not a style check. A native whose binding name is misspelled compiles, links and
/// ships; the generated stub throws `Missing javascript native method` the first time the feature
/// is used, which for the vault's device protection means the failure arrives in a user's browser
/// rather than in a build. The name is long, mechanical and derived from a descriptor -- exactly
/// the shape of thing that is got wrong by hand and never noticed.
///
/// The expected name comes from [JavascriptNameUtil#methodIdentifier], the same code the
/// generator uses, so the part most likely to be wrong cannot drift from the thing that checks
/// it.
///
/// #### Why this reads source rather than bytecode
///
/// Neither of the obvious inputs exists where this workflow runs. `maven/parparvm` builds the jar
/// of compiled port classes and this job builds `vm/` only; compiling the sources here would need
/// `codenameone-core` at the version being developed, and this module pins a released one. So the
/// declarations are parsed out of the source, which is enough because the only hand-written step
/// that remains is Java type to JVM descriptor -- and [#descriptorConversionIsCorrect()] pins
/// that against a signature whose descriptor is not in doubt.
class JavascriptPortNativeBindingTest {

    private static final Path PORT_ROOT =
            Paths.get("..", "..", "Ports", "JavaScriptPort").toAbsolutePath().normalize();

    private static final String COVERED_CLASS = "com.codename1.impl.html5.HTML5DeviceProtection";

    private static final Path COVERED_SOURCE = PORT_ROOT.resolve(Paths.get("src", "main", "java",
            "com", "codename1", "impl", "html5", "HTML5DeviceProtection.java"));

    /// `[modifiers] native <return> <name>(<args>);`
    private static final Pattern NATIVE = Pattern.compile(
            "(?:^|\\n)\\s*(?:public\\s+|private\\s+|protected\\s+|static\\s+|final\\s+)*"
            + "native\\s+([A-Za-z0-9_.\\[\\]]+)\\s+([A-Za-z0-9_]+)\\s*\\(([^)]*)\\)\\s*;");

    @Test
    void everyCoveredNativeIsBoundInPortJs() throws Exception {
        String portJs = portJs();
        List<String> natives = nativesOf(source());
        // Guards the guard: if the class were renamed, its natives removed, or the pattern above
        // stopped matching the source's style, the loop below would pass over an empty list and
        // report success.
        assertEquals(6, natives.size(),
                "expected the six device-protection natives, found: " + natives);

        List<String> missing = new ArrayList<String>();
        for (String identifier : natives) {
            if (portJs.indexOf(identifier) < 0) {
                missing.add(identifier);
            }
        }
        assertTrue(missing.isEmpty(),
                "native methods with no bindNative entry in port.js: " + missing);
    }

    @Test
    void descriptorConversionIsCorrect() {
        // The one hand-written step, pinned. `byte[] nativeWrap(String, byte[], byte[])` has
        // exactly one JVM descriptor and exactly one translator identifier, and the identifier is
        // computed by the translator rather than written out here.
        assertEquals("(Ljava/lang/String;[B[B)[B",
                descriptorOf("byte[]", "String keyId, byte[] plaintext, byte[] aad"));
        assertEquals("()[B", descriptorOf("byte[]", ""));
        assertEquals("(I)V", descriptorOf("void", "int count"));
        assertEquals("(Ljava/lang/String;)Z", descriptorOf("boolean", "String name"));

        String identifier = JavascriptNameUtil.methodIdentifier(
                COVERED_CLASS.replace('.', '/'), "nativeWrap", "(Ljava/lang/String;[B[B)[B");
        assertEquals("cn1_com_codename1_impl_html5_HTML5DeviceProtection_nativeWrap"
                + "_java_lang_String_byte_1ARRAY_byte_1ARRAY_R_byte_1ARRAY", identifier);
    }

    @Test
    void aWrongNameWouldBeDetected() throws Exception {
        // The matcher has to be able to say no. Without this, a bug that made every lookup
        // succeed would leave the first test green forever.
        assertTrue(portJs().indexOf("cn1_com_codename1_impl_html5_HTML5DeviceProtection_"
                + "nativeWrapp_java_lang_String") < 0,
                "a deliberately misspelled name must not be found");
    }

    private static String portJs() throws Exception {
        Path path = PORT_ROOT.resolve(Paths.get("src", "main", "webapp", "port.js"));
        assertTrue(Files.exists(path), "port.js not found at " + path);
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(text.indexOf("bindNative(") >= 0, "port.js does not look like the port bindings");
        return text;
    }

    private static String source() throws Exception {
        assertTrue(Files.exists(COVERED_SOURCE), "port source not found at " + COVERED_SOURCE);
        return new String(Files.readAllBytes(COVERED_SOURCE), StandardCharsets.UTF_8);
    }

    private static List<String> nativesOf(String source) {
        List<String> out = new ArrayList<String>();
        Matcher matcher = NATIVE.matcher(source);
        while (matcher.find()) {
            String descriptor = descriptorOf(matcher.group(1), matcher.group(3));
            out.add(JavascriptNameUtil.methodIdentifier(COVERED_CLASS.replace('.', '/'),
                    matcher.group(2), descriptor));
        }
        return out;
    }

    /// Java source types to a JVM descriptor, for the small set of types the covered natives use.
    ///
    /// Deliberately narrow, and it throws rather than guessing: a native that introduced a type
    /// this does not know should fail the test rather than be silently mangled into a name that
    /// then fails to match and reports as an unbound native, which would send the next reader
    /// looking in port.js for a bug that is here.
    private static String descriptorOf(String returnType, String parameters) {
        StringBuilder b = new StringBuilder("(");
        String trimmed = parameters.trim();
        if (trimmed.length() > 0) {
            String[] each = trimmed.split(",");
            for (int iter = 0; iter < each.length; iter++) {
                String declaration = each[iter].trim();
                int space = declaration.lastIndexOf(' ');
                assertTrue(space > 0, "unparsable parameter: " + declaration);
                b.append(typeDescriptor(declaration.substring(0, space).trim()));
            }
        }
        b.append(')').append(typeDescriptor(returnType.trim()));
        return b.toString();
    }

    private static String typeDescriptor(String type) {
        if (type.endsWith("[]")) {
            return "[" + typeDescriptor(type.substring(0, type.length() - 2).trim());
        }
        if ("void".equals(type)) {
            return "V";
        }
        if ("int".equals(type)) {
            return "I";
        }
        if ("long".equals(type)) {
            return "J";
        }
        if ("boolean".equals(type)) {
            return "Z";
        }
        if ("byte".equals(type)) {
            return "B";
        }
        if ("char".equals(type)) {
            return "C";
        }
        if ("double".equals(type)) {
            return "D";
        }
        if ("float".equals(type)) {
            return "F";
        }
        if ("short".equals(type)) {
            return "S";
        }
        if ("String".equals(type) || "java.lang.String".equals(type)) {
            return "Ljava/lang/String;";
        }
        throw new IllegalArgumentException("this test does not know the type " + type
                + "; teach it rather than letting the name be guessed");
    }
}

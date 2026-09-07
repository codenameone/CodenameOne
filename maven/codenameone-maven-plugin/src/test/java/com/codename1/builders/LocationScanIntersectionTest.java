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
import java.io.FileOutputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crosses the staged-source scanner's rules against each other.
 *
 * <p>The scanner is a pile of rules over text -- comments are stripped,
 * literals are masked, Kotlin templates are code, a wildcard import counts as
 * naming, a getter is reachable as a property, a factory stands in for the
 * class it returns, the button is reachable reflectively. Each is right on its
 * own. The bugs have been in their INTERSECTIONS, which is a different thing
 * and is not covered by testing each rule alone.</p>
 *
 * <p>Two build-refusing defects were found this way before review reached
 * them: a Kotlin log line became button use as soon as it interpolated, and a
 * class that reflected for anything at all became button use if it merely
 * mentioned the button in a comment. Both were combinations of rules that
 * passed their own tests.</p>
 *
 * <p>Add a case here whenever a rule is added, not only a test of the rule.</p>
 */
public class LocationScanIntersectionTest {

    @Test
    public void aWildcardImportCrossesEveryOtherRule() throws Exception {
        assertTrue(button("package com.example;\n"
                + "import com.codename1.location.*;\n"
                + "public class A { LocationButton b = new LocationButton(); }\n",
                "A.java"), "wildcard import + the button");
        assertTrue(provider("package com.example;\n"
                + "import com.codename1.location.*;\n"
                + "public class A { GeofenceManager g; }\n", "A.java"),
                "wildcard import + a framework wrapper");
        assertTrue(provider("package com.example\n"
                + "import com.google.android.gms.location.*\n"
                + "class A(val c: FusedLocationProviderClient) {\n"
                + "  fun f() = c.lastLocation\n}\n", "A.kt"),
                "wildcard import + a Kotlin property getter");
        assertTrue(provider("package com.example\n"
                + "import com.google.android.gms.location.*\n"
                + "class A { fun f(x: Any) = LocationServices"
                + ".getFusedLocationProviderClient(x).lastLocation }\n", "A.kt"),
                "wildcard import + the factory");
    }

    @Test
    public void aCommentNeverBecomesUse() throws Exception {
        assertFalse(provider("package com.example;\n"
                + "// com.codename1.location.GeofenceManager, once\n"
                + "public class A { }\n", "A.java"),
                "a wrapper named in a comment");
        assertFalse(provider("package com.example\nclass A {\n"
                + "  // LocationServices.getFusedLocationProviderClient(x)"
                + ".lastLocation\n}\n", "A.kt"),
                "the factory named in a comment");
        assertFalse(button("package com.example;\npublic class A {\n"
                + "  // Class.forName(\"com.codename1.location.LocationButton\")\n"
                + "}\n", "A.java"),
                "a reflective lookup entirely inside a comment");
    }

    @Test
    public void aTemplateIsCodeForOneScanAndTextForTheOther() throws Exception {
        // The provider scan keeps a template-bearing literal whole, because
        // what is inside ${...} is compiled and masking it would hide a call.
        assertTrue(provider("package com.example\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient\n"
                + "class A(val c: FusedLocationProviderClient) {\n"
                + "  fun f() = \"at ${c.lastLocation}\"\n}\n", "A.kt"),
                "a call inside a template is still a call");
        // The button scan masks them, because it has its own way of reading a
        // literal and inheriting this rule made a log line into button use.
        assertFalse(button("package com.example\nclass A(val n: Int) {\n"
                + "  fun f() = \"building ${n} of "
                + "com.codename1.location.LocationButton\"\n}\n", "A.kt"),
                "a message that interpolates is still a message");
        assertFalse(button("package com.example\nclass A(val n: Int) {\n"
                + "  fun f() = \"\"\"${n} "
                + "com.codename1.location.LocationButton\"\"\"\n}\n", "A.kt"),
                "and the same of a raw string");
    }

    @Test
    public void aRawStringObeysTheSameRulesAsAPlainOne() throws Exception {
        assertTrue(provider("package com.example\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient\n"
                + "class A(val c: FusedLocationProviderClient) {\n"
                + "  fun f() = \"\"\"at ${c.lastLocation}\"\"\"\n}\n", "A.kt"),
                "a raw string carrying a template carries its call");
        assertFalse(provider("package com.example\nclass A {\n"
                + "  fun f() = \"\"\"android.location.LocationManager"
                + ".requestLocationUpdates\"\"\"\n}\n", "A.kt"),
                "a raw string without one is prose");
    }

    @Test
    public void reflectionAndCommentsDoNotCombineIntoUse() throws Exception {
        // The rule reads a string's TEXT, which is the point; it must not read
        // a comment's. Any class that reflects for anything would otherwise
        // become button use by mentioning it in prose.
        assertFalse(button("package com.example;\npublic class A {\n"
                + "  // com.codename1.location.LocationButton, one day\n"
                + "  Object f() throws Exception {\n"
                + "    return Class.forName(\"com.example.Thing\");\n  }\n}\n",
                "A.java"),
                "reflection elsewhere plus a comment is not use");
        assertTrue(button("package com.example;\npublic class A {\n"
                + "  Object f() throws Exception {\n"
                + "    return Class.forName("
                + "\"com.codename1.location.LocationButton\");\n  }\n}\n",
                "A.java"),
                "but the real lookup still counts");
    }

    private static boolean button(String source, String name) throws Exception {
        return LocationButtonManifestFragments.sourcesNameTheButton(
                write(source, name));
    }

    private static boolean provider(String source, String name)
            throws Exception {
        return LocationButtonManifestFragments.sourcesCallPlatformLocation(
                write(source, name));
    }

    private static File write(String source, String name) throws Exception {
        File root = File.createTempFile("cn1-lb-cross", "");
        root.delete();
        root.mkdirs();
        root.deleteOnExit();
        File at = new File(root, "com/example/" + name);
        at.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(at);
        try {
            out.write(source.getBytes("UTF-8"));
        } finally {
            out.close();
        }
        return root;
    }
}

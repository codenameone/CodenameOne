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
package com.codename1.androidcompat.runtime;

import com.codename1.android.rescompiler.ResourceCompiler;
import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// Compiles a res tree with the build's compiler and reads it back with the
/// runtime's table reader: the two sides of the binary format must agree,
/// and variant selection must follow Android's rules.
public class ResTableRoundTripTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static void write(File f, String s) throws IOException {
        f.getParentFile().mkdirs();
        Writer w = new OutputStreamWriter(new FileOutputStream(f), Charset.forName("UTF-8"));
        try {
            w.write(s);
        } finally {
            w.close();
        }
    }

    private ResTable compile() throws IOException {
        File res = tmp.newFolder("res");
        write(new File(res, "values/v.xml"), "<resources><string name=\"hello\">Hello</string>"
                + "<color name=\"bg\">#ffffff</color><dimen name=\"pad\">12dp</dimen>"
                + "<string-array name=\"days\"><item>Mon</item><item>Tue</item></string-array>"
                + "<plurals name=\"n\"><item quantity=\"one\">one</item><item quantity=\"other\">many</item></plurals>"
                + "<style name=\"Parent\"><item name=\"android:textColor\">@color/bg</item></style>"
                + "<style name=\"Parent.Child\"><item name=\"android:textSize\">10sp</item></style>"
                + "</resources>");
        write(new File(res, "values-night/v.xml"), "<resources><color name=\"bg\">#000000</color></resources>");
        write(new File(res, "values-fr/v.xml"), "<resources><string name=\"hello\">Bonjour</string></resources>");
        write(new File(res, "values-fr-rCA/v.xml"), "<resources><string name=\"hello\">Allo</string></resources>");
        write(new File(res, "values-sw600dp/v.xml"), "<resources><dimen name=\"pad\">24dp</dimen></resources>");
        write(new File(res, "drawable-mdpi/i.png"), "x");
        write(new File(res, "drawable-xhdpi/i.png"), "x");
        write(new File(res, "drawable-xxxhdpi/i.png"), "x");
        ResourceCompiler.Request r = new ResourceCompiler.Request();
        r.res.add(new ResourceCompiler.ResSource(res, "com.t"));
        r.javaOut = tmp.newFolder("java");
        r.resourcesOut = tmp.newFolder("out");
        r.frameworkSymbols = getClass().getResourceAsStream("/" + ResourceCompiler.FRAMEWORK_SYMBOLS_RESOURCE);
        assertNotNull("framework symbols on the test classpath", r.frameworkSymbols);
        ResourceCompiler.Result result = new ResourceCompiler().compile(r);
        assertTrue(result.diagnostics.toString(), !result.hasErrors());
        InputStream in = new FileInputStream(new File(r.resourcesOut, ResourceCompiler.APP_TABLE));
        try {
            return ResTable.read(in);
        } finally {
            in.close();
        }
    }

    private static DeviceConfig device(String lang, String region, boolean night, int widthDp, int dpi) {
        DeviceConfig d = new DeviceConfig();
        d.language = lang;
        d.region = region;
        d.night = night ? 2 : 1;
        d.widthDp = widthDp;
        d.heightDp = 800;
        d.smallestWidthDp = Math.min(widthDp, 800);
        d.densityDpi = dpi;
        d.sdkVersion = 34;
        d.generation = (int) (Math.random() * 100000) + 1;
        return d;
    }

    @Test
    public void localeFallsBackFromRegionToLanguageToDefault() throws IOException {
        ResTable t = compile();
        ResTable.Entry hello = t.get("string/hello");
        assertEquals("Hello", ((ResValue) hello.best(device("en", "US", false, 400, 320))).string);
        assertEquals("Bonjour", ((ResValue) hello.best(device("fr", "FR", false, 400, 320))).string);
        assertEquals("Allo", ((ResValue) hello.best(device("fr", "CA", false, 400, 320))).string);
    }

    @Test
    public void nightAndSmallestWidthSelectVariants() throws IOException {
        ResTable t = compile();
        assertEquals(0xffffffff, ((ResValue) t.get("color/bg").best(device("en", "US", false, 400, 320))).data);
        assertEquals(0xff000000, ((ResValue) t.get("color/bg").best(device("en", "US", true, 400, 320))).data);
        int phone = ((ResValue) t.get("dimen/pad").best(device("en", "US", false, 400, 320))).data;
        int tablet = ((ResValue) t.get("dimen/pad").best(device("en", "US", false, 800, 320))).data;
        assertEquals(12f, android.util.TypedValue.complexToFloat(phone), 0.001f);
        assertEquals(24f, android.util.TypedValue.complexToFloat(tablet), 0.001f);
    }

    @Test
    public void densityPrefersTheNearestBucketScalingDown() throws IOException {
        ResTable t = compile();
        ResTable.Entry img = t.get("drawable/i");
        assertEquals(320, img.bestConfig(device("en", "US", false, 400, 320)).getDensity());
        assertEquals(320, img.bestConfig(device("en", "US", false, 400, 240)).getDensity());
        assertEquals(640, img.bestConfig(device("en", "US", false, 400, 480)).getDensity());
        assertEquals(160, img.bestConfig(device("en", "US", false, 400, 120)).getDensity());
        assertTrue(img.best(device("en", "US", false, 400, 320)) instanceof ResTable.FileRef);
    }

    @Test
    public void bagsKeepParentsKeysAndOrder() throws IOException {
        ResTable t = compile();
        ResTable.Bag days = (ResTable.Bag) t.get("array/days").best(device("en", "US", false, 400, 320));
        assertEquals("Mon", days.values[0].string);
        assertEquals("Tue", days.values[1].string);
        ResTable.Bag plural = (ResTable.Bag) t.get("plurals/n").best(device("en", "US", false, 400, 320));
        assertEquals("one", plural.get(PluralRules.ONE).string);
        assertEquals("many", plural.get(PluralRules.OTHER).string);
        ResTable.Bag child = (ResTable.Bag) t.get("style/Parent.Child").best(device("en", "US", false, 400, 320));
        assertEquals(t.get("style/Parent").id, child.parent);
        assertNull(t.get("style/Missing"));
    }

    @Test
    public void pluralRules() {
        assertEquals(PluralRules.ONE, PluralRules.select("en", 1));
        assertEquals(PluralRules.OTHER, PluralRules.select("en", 2));
        assertEquals(PluralRules.FEW, PluralRules.select("ru", 3));
        assertEquals(PluralRules.MANY, PluralRules.select("ru", 5));
        assertEquals(PluralRules.ONE, PluralRules.select("ru", 21));
        assertEquals(PluralRules.ONE, PluralRules.select("fr", 0));
        assertEquals(PluralRules.OTHER, PluralRules.select("ja", 1));
        assertEquals(PluralRules.TWO, PluralRules.select("ar", 2));
    }
}

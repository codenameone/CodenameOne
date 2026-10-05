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
package com.codename1.android.rescompiler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceCompilerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    static void write(File f, String s) throws IOException {
        f.getParentFile().mkdirs();
        Writer w = new OutputStreamWriter(new FileOutputStream(f), Charset.forName("UTF-8"));
        try {
            w.write(s);
        } finally {
            w.close();
        }
    }

    static final String FRAMEWORK = "res attr/text 0x01010000\n"
            + "res attr/layout_width 0x01010001\n"
            + "res attr/layout_height 0x01010002\n"
            + "res attr/textColor 0x01010003\n"
            + "res attr/onClick 0x01010004\n"
            + "res attr/name 0x01010005\n"
            + "attr android:name string\n"
            + "res style/Theme 0x01030000\n"
            + "attr android:text string\n"
            + "attr android:textColor reference|color\n"
            + "attr android:onClick string\n"
            + "attr android:layout_width dimension|enum enum:match_parent=-1 enum:wrap_content=-2\n"
            + "attr android:layout_height dimension|enum enum:match_parent=-1 enum:wrap_content=-2\n"
            + "view TextView android.widget.TextView\n"
            + "view LinearLayout android.widget.LinearLayout\n";

    private ResourceCompiler.Result compile(File res) throws IOException {
        ResourceCompiler.Request r = new ResourceCompiler.Request();
        r.res.add(new ResourceCompiler.ResSource(res, null));
        r.manifest = new File(res.getParentFile(), "AndroidManifest.xml");
        r.javaOut = tmp.newFolder("java");
        r.resourcesOut = tmp.newFolder("out");
        r.onClickNamesOut = new File(r.resourcesOut, "onclick.txt");
        r.frameworkSymbols = new ByteArrayInputStream(FRAMEWORK.getBytes("UTF-8"));
        return new ResourceCompiler().compile(r);
    }

    private File project() throws IOException {
        File root = tmp.newFolder("main");
        write(new File(root, "AndroidManifest.xml"), "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " package=\"com.x\"><application android:label=\"@string/app\" android:name=\".App\">"
                + "<activity android:name=\".Main\"><intent-filter><action android:name=\"android.intent.action.MAIN\"/>"
                + "<category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter></activity>"
                + "<activity android:name=\"com.other.Second\"/></application></manifest>");
        File res = new File(root, "res");
        write(new File(res, "values/strings.xml"), "<resources><string name=\"app\">App</string>"
                + "<color name=\"red\">#f00</color><style name=\"Base\" parent=\"@android:style/Theme\">"
                + "<item name=\"android:textColor\">@color/red</item></style><style name=\"Base.Child\"/>"
                + "<declare-styleable name=\"Gauge\"><attr name=\"maxValue\" format=\"integer\"/>"
                + "<attr name=\"android:text\"/></declare-styleable></resources>");
        write(new File(res, "values-fr/strings.xml"), "<resources><string name=\"app\">Appli</string></resources>");
        write(new File(res, "layout/main.xml"), "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                + "<TextView android:id=\"@+id/label\" android:text=\"@string/app\" android:onClick=\"tapped\""
                + " android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                + "<com.x.Gauge android:layout_width=\"10dp\" android:layout_height=\"10dp\"/></LinearLayout>");
        return res;
    }

    @Test
    public void compilesRClassFactoryAndTable() throws IOException {
        ResourceCompiler.Result r = compile(project());
        assertFalse(r.diagnostics.toString(), r.hasErrors());
        String rJava = new String(Files.readAllBytes(r.javaFiles.get(0).toPath()), "UTF-8");
        assertTrue(rJava, rJava.contains("package com.x;"));
        assertTrue(rJava.contains("public static final int app = 0x7f"));
        assertTrue(rJava.contains("public static final int label = 0x7f"));
        assertTrue(rJava.contains("public static final int Base_Child = 0x7f"));
        assertTrue(rJava, rJava.contains("public static final int[] Gauge = {0x01010000, 0x7f"));
        assertTrue(rJava.contains("public static final int Gauge_android_text = 0;"));
        assertTrue(rJava.contains("public static final int Gauge_maxValue = 1;"));
        String impl = new String(Files.readAllBytes(r.javaFiles.get(1).toPath()), "UTF-8");
        assertTrue(impl, impl.contains("return new android.widget.TextView(context, attrs);"));
        assertTrue(impl.contains("return new com.x.Gauge(context, attrs);"));
        assertTrue(impl.contains("if (type == com.x.Main.class)"));
        assertTrue(impl.contains("if (type == com.other.Second.class)"));
        assertTrue(impl.contains("return new com.x.App();"));
        assertTrue(impl.contains(", true);"));
        assertEquals("tapped\n", new String(Files.readAllBytes(new File(r.resourceFiles.get(0).getParentFile(),
                "onclick.txt").toPath()), "UTF-8"));
    }

    /// `android:launchMode` reaches the runtime, so a singleTop or singleTask
    /// activity receives `onNewIntent` instead of a duplicate screen.
    @Test
    public void compilesLaunchMode() throws IOException {
        File res = project();
        File manifest = new File(res.getParentFile(), "AndroidManifest.xml");
        String m = new String(Files.readAllBytes(manifest.toPath()), "UTF-8");
        write(manifest, m.replace("<activity android:name=\".Main\">",
                "<activity android:name=\".Main\" android:launchMode=\"singleTask\">"));
        ResourceCompiler.Result r = compile(res);
        assertFalse(r.diagnostics.toString(), r.hasErrors());
        String impl = new String(Files.readAllBytes(r.javaFiles.get(1).toPath()), "UTF-8");
        assertTrue(impl, impl.contains("launchMode(com.x.Main.class, 2);"));
        assertFalse(impl, impl.contains("launchMode(com.other.Second.class"));
    }

    @Test
    public void unknownLaunchModeWarnsAndStaysStandard() throws IOException {
        File res = project();
        File manifest = new File(res.getParentFile(), "AndroidManifest.xml");
        String m = new String(Files.readAllBytes(manifest.toPath()), "UTF-8");
        write(manifest, m.replace("<activity android:name=\".Main\">",
                "<activity android:name=\".Main\" android:launchMode=\"sometimes\">"));
        ResourceCompiler.Result r = compile(res);
        assertTrue(r.diagnostics.toString(), r.diagnostics.toString().contains("W0305"));
        String impl = new String(Files.readAllBytes(r.javaFiles.get(1).toPath()), "UTF-8");
        assertFalse(impl, impl.contains("launchMode("));
    }

    @Test
    public void compilesConfigChangesAndAcceptsFragments() throws IOException {
        File res = project();
        File manifest = new File(res.getParentFile(), "AndroidManifest.xml");
        String m = new String(Files.readAllBytes(manifest.toPath()), "UTF-8");
        write(manifest, m.replace("<activity android:name=\".Main\">",
                "<activity android:name=\".Main\" android:configChanges=\"orientation|screenSize| bogus\">"));
        write(new File(res, "layout/frags.xml"), "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                + "<fragment android:name=\"com.x.Titles\" android:id=\"@+id/titles\""
                + " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"/></LinearLayout>");
        ResourceCompiler.Result r = compile(res);
        assertFalse(r.diagnostics.toString(), r.hasErrors());
        String all = r.diagnostics.toString();
        assertTrue(all, all.contains("W0303"));
        assertTrue(all, all.contains("bogus"));
        String impl = new String(Files.readAllBytes(r.javaFiles.get(1).toPath()), "UTF-8");
        assertTrue(impl, impl.contains("configChanges(com.x.Main.class, 0x00000480);"));
        assertFalse(impl, impl.contains("configChanges(com.other.Second.class"));
        assertFalse(impl, impl.contains("\"fragment\""));
        String rJava = new String(Files.readAllBytes(r.javaFiles.get(0).toPath()), "UTF-8");
        assertTrue(rJava, rJava.contains("public static final int titles = 0x7f"));
    }

    @Test
    public void reportsUnknownViewsAndReferences() throws IOException {
        File res = project();
        write(new File(res, "layout/bad.xml"), "<CalendarView xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " android:text=\"@string/missing\"/>");
        ResourceCompiler.Result r = compile(res);
        assertTrue(r.hasErrors());
        String all = r.diagnostics.toString();
        assertTrue(all, all.contains("E0201"));
        assertTrue(all, all.contains("E0102"));
        assertTrue(all, all.contains("layout/bad.xml"));
    }

    /// The output directory persists between builds and the asset index is
    /// what AssetManager lists: an asset deleted from the project -- the last
    /// one, which takes the assets directory with it -- leaves both.
    @Test
    public void deletedAssetsLeaveTheIndexAndTheOutput() throws IOException {
        File res = project();
        File assets = new File(res.getParentFile(), "assets");
        write(new File(assets, "a.txt"), "a");
        write(new File(assets, "dir/b.txt"), "b");
        ResourceCompiler.Request r = new ResourceCompiler.Request();
        r.res.add(new ResourceCompiler.ResSource(res, null));
        r.manifest = new File(res.getParentFile(), "AndroidManifest.xml");
        r.javaOut = tmp.newFolder("java");
        r.resourcesOut = tmp.newFolder("out");
        r.onClickNamesOut = new File(r.resourcesOut, "onclick.txt");
        r.assetsDir = assets;
        r.frameworkSymbols = new ByteArrayInputStream(FRAMEWORK.getBytes("UTF-8"));
        new ResourceCompiler().compile(r);
        File index = new File(r.resourcesOut, ResourceCompiler.ASSET_INDEX);
        assertEquals(2, flattenedAssets(r.resourcesOut));
        assertTrue(new String(Files.readAllBytes(index.toPath()), "UTF-8").contains("dir/b.txt"));

        assertTrue(new File(assets, "dir/b.txt").delete());
        r.frameworkSymbols = new ByteArrayInputStream(FRAMEWORK.getBytes("UTF-8"));
        new ResourceCompiler().compile(r);
        assertEquals(1, flattenedAssets(r.resourcesOut));
        assertFalse(new String(Files.readAllBytes(index.toPath()), "UTF-8").contains("dir/b.txt"));

        assertTrue(new File(assets, "a.txt").delete());
        assertTrue(new File(assets, "dir").delete());
        assertTrue(assets.delete());
        r.frameworkSymbols = new ByteArrayInputStream(FRAMEWORK.getBytes("UTF-8"));
        new ResourceCompiler().compile(r);
        assertEquals(0, flattenedAssets(r.resourcesOut));
        assertFalse("the index outlived the last asset", index.exists());
    }

    /// Activity filters reach the generated code whole. Flattened to their
    /// actions, an ACTION_VIEW filter for a custom scheme captured every
    /// browser and telephone intent the application started.
    @Test
    public void intentFiltersKeepTheirDataTypesAndCategories() throws IOException {
        File res = project();
        File manifest = new File(res.getParentFile(), "AndroidManifest.xml");
        String m = new String(Files.readAllBytes(manifest.toPath()), "UTF-8");
        write(manifest, m.replace("<activity android:name=\"com.other.Second\"/>",
                "<activity android:name=\"com.other.Second\"><intent-filter>"
                        + "<action android:name=\"android.intent.action.VIEW\"/>"
                        + "<category android:name=\"android.intent.category.DEFAULT\"/>"
                        + "<category android:name=\"android.intent.category.BROWSABLE\"/>"
                        + "<data android:scheme=\"myapp\" android:host=\"example.com\" android:port=\"8080\"/>"
                        + "<data android:pathPrefix=\"/items\"/><data android:pathPattern=\".*\\\\.pdf\"/>"
                        + "</intent-filter><intent-filter><action android:name=\"android.intent.action.SEND\"/>"
                        + "<category android:name=\"android.intent.category.DEFAULT\"/>"
                        + "<data android:mimeType=\"image/*\"/></intent-filter>"
                        + "<intent-filter><action android:name=\"com.x.ADVANCED\"/>"
                        + "<data android:scheme=\"x\" android:pathAdvancedPattern=\"/[a-z]+\"/></intent-filter>"
                        + "</activity>"));
        ResourceCompiler.Result r = compile(res);
        assertFalse(r.diagnostics.toString(), r.hasErrors());
        String impl = new String(Files.readAllBytes(r.javaFiles.get(1).toPath()), "UTF-8");
        assertTrue(impl, impl.contains("android.content.IntentFilter f = intentFilter(com.other.Second.class);"));
        assertTrue(impl, impl.contains("f.addAction(\"android.intent.action.VIEW\");"));
        assertTrue(impl, impl.contains("f.addCategory(\"android.intent.category.BROWSABLE\");"));
        assertTrue("the filter's scheme was dropped: " + impl, impl.contains("f.addDataScheme(\"myapp\");"));
        assertTrue(impl, impl.contains("f.addDataAuthority(\"example.com\", \"8080\");"));
        assertTrue(impl, impl.contains("f.addDataPath(\"/items\", 1);"));
        // aapt unescapes ".*\\.pdf" to the pattern ".*\.pdf", quoted again here.
        assertTrue(impl, impl.contains("f.addDataPath(\".*\\\\.pdf\", 2);"));
        assertTrue(impl, impl.contains("f.addDataType(\"image/*\");"));
        // The launcher filter is kept too: it lists no DEFAULT, so it takes
        // no implicit start, as on Android.
        assertTrue(impl, impl.contains("intentFilter(com.x.Main.class);"));
        // A filter with a constraint the runtime cannot evaluate is dropped,
        // never kept without it.
        assertFalse(impl, impl.contains("com.x.ADVANCED"));
        assertTrue(r.diagnostics.toString(), r.diagnostics.toString().contains("W0304"));
    }

    /// An activity the manifest disables is never registered: it used to be
    /// selectable by its filters, startable by name, and a disabled launcher
    /// started at boot.
    @Test
    public void disabledActivitiesAreLeftOut() throws IOException {
        File res = project();
        File manifest = new File(res.getParentFile(), "AndroidManifest.xml");
        String m = new String(Files.readAllBytes(manifest.toPath()), "UTF-8");
        write(manifest, m.replace("<activity android:name=\".Main\">",
                "<activity android:name=\".Main\" android:enabled=\"false\">")
                .replace("<activity android:name=\"com.other.Second\"/>",
                        "<activity android:name=\"com.other.Second\"/>"
                                + "<activity android:name=\"com.other.Third\" android:enabled=\"@bool/third\"/>"));
        ResourceCompiler.Result r = compile(res);
        assertFalse(r.diagnostics.toString(), r.hasErrors());
        String impl = new String(Files.readAllBytes(r.javaFiles.get(1).toPath()), "UTF-8");
        assertFalse("a disabled activity was registered: " + impl, impl.contains("com.x.Main"));
        assertTrue(impl, impl.contains("activity(com.other.Second.class"));
        assertTrue("a configuration-dependent enabled flag was dropped", impl.contains("com.other.Third.class"));
    }

    /// Two assets whose paths have the same `String.hashCode()` -- `Aa` and
    /// `BB` collide -- each keep their own output file.
    @Test
    public void assetsWithCollidingPathHashesKeepSeparateCopies() throws IOException {
        assertEquals("Aa/file.txt".hashCode(), "BB/file.txt".hashCode());
        File res = project();
        File assets = new File(res.getParentFile(), "assets");
        write(new File(assets, "Aa/file.txt"), "first");
        write(new File(assets, "BB/file.txt"), "second");
        ResourceCompiler.Request r = new ResourceCompiler.Request();
        r.res.add(new ResourceCompiler.ResSource(res, null));
        r.manifest = new File(res.getParentFile(), "AndroidManifest.xml");
        r.javaOut = tmp.newFolder("java");
        r.resourcesOut = tmp.newFolder("out");
        r.onClickNamesOut = new File(r.resourcesOut, "onclick.txt");
        r.assetsDir = assets;
        r.frameworkSymbols = new ByteArrayInputStream(FRAMEWORK.getBytes("UTF-8"));
        new ResourceCompiler().compile(r);
        assertEquals("the two assets share one output file", 2, flattenedAssets(r.resourcesOut));
        String index = new String(Files.readAllBytes(new File(r.resourcesOut, ResourceCompiler.ASSET_INDEX).toPath()),
                "UTF-8");
        for (String line : index.split("\n")) {
            String[] kv = line.split("\t");
            String expected = kv[0].startsWith("Aa/") ? "first" : "second";
            assertEquals(line, expected, new String(Files.readAllBytes(new File(r.resourcesOut, kv[1]).toPath()),
                    "UTF-8"));
        }
    }

    private static int flattenedAssets(File out) {
        int n = 0;
        for (File f : out.listFiles()) {
            if (f.getName().startsWith("andra_")) {
                n++;
            }
        }
        return n;
    }
}

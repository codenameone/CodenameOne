/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Flattened Flutter assets follow the source tree across incremental builds:
 * what disappears from src/main/flutter disappears from the output, and nothing
 * the plugin did not write is ever removed.
 */
public class TranscodeFlutterAssetsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private TranscodeFlutterMojo mojo;
    private File flutter;
    private File classes;

    @Before
    public void setUp() throws Exception {
        File target = tmp.newFolder("target");
        classes = new File(target, "classes");
        flutter = tmp.newFolder("flutter");
        MavenProject project = new MavenProject();
        Build build = new Build();
        build.setDirectory(target.getAbsolutePath());
        build.setOutputDirectory(classes.getAbsolutePath());
        project.setBuild(build);
        mojo = new TranscodeFlutterMojo();
        set("project", project);
        set("flutterSourceDir", flutter);
    }

    @Test
    public void aRenamedAssetLeavesNoStaleCopy() throws Exception {
        File old = asset("assets/icons/old_name.png");
        asset("assets/kept.png");
        mojo.copyAssets();
        File oldFlat = flat("assets/icons/old_name.png");
        assertTrue(oldFlat.isFile());

        assertTrue(old.delete());
        asset("assets/icons/new_name.png");
        mojo.copyAssets();

        assertFalse("the renamed-away asset must not be packaged again", oldFlat.exists());
        assertTrue(flat("assets/icons/new_name.png").isFile());
        assertTrue(flat("assets/kept.png").isFile());
    }

    @Test
    public void theManifestListsEveryBundledAssetKey() throws Exception {
        asset("assets/a.png");
        asset("assets/2.5x/a.png");
        asset("packages/lib/assets/3x/b.png");
        mojo.copyAssets();
        File manifest = new File(classes, TranscodeFlutterMojo.ASSET_MANIFEST);
        assertTrue(manifest.isFile());
        assertEquals(java.util.Arrays.asList("assets/2.5x/a.png", "assets/a.png", "packages/lib/assets/3x/b.png"),
                Files.readAllLines(manifest.toPath(), java.nio.charset.StandardCharsets.UTF_8));

        // Recorded like the assets: gone with them when the Flutter tree is.
        mojo.removeStaleAssets(new HashSet<String>());
        assertFalse(manifest.exists());
    }

    @Test
    public void removingFlutterRemovesItsAssets() throws Exception {
        asset("assets/a.png");
        asset("packages/lib/assets/b.png");
        mojo.copyAssets();
        assertTrue(flat("assets/a.png").isFile());

        // What executeImpl does when src/main/flutter is gone.
        mojo.removeStaleAssets(new HashSet<String>());

        assertFalse(flat("assets/a.png").exists());
        assertFalse(flat("packages/lib/assets/b.png").exists());
    }

    @Test
    public void filesThePluginDidNotWriteAreNeverRemoved() throws Exception {
        asset("assets/a.png");
        mojo.copyAssets();
        // The application's own resource, sharing the output directory and even
        // the prefix: it is not in the plugin's record, so it is not the plugin's.
        File own = new File(classes, "cn1f_app_owned.bin");
        Files.write(own.toPath(), new byte[] {1});

        mojo.removeStaleAssets(new HashSet<String>());

        assertTrue(own.isFile());
    }

    /**
     * The same vectors as the runtime's FlutterAssetsTest and the benchmark's
     * stage_assets.py: the three encoders must agree name for name, or an asset
     * the build writes is one the runtime never finds.
     */
    @Test
    public void encodingMatchesTheRuntimeAndIsPrefixFree() {
        assertEquals("cn1f_assets_sstudies_sreply__card.png",
                TranscodeFlutterMojo.flatAssetName("assets/studies/reply_card.png"));
        assertEquals("cn1f_a___sb.png", TranscodeFlutterMojo.flatAssetName("a_/b.png"));
        assertEquals("cn1f_a_s__b.png", TranscodeFlutterMojo.flatAssetName("a/_b.png"));
        assertFalse(TranscodeFlutterMojo.flatAssetName("a_/b.png")
                .equals(TranscodeFlutterMojo.flatAssetName("a/_b.png")));
    }

    private File asset(String key) throws Exception {
        File f = new File(flutter, key);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), new byte[] {0});
        return f;
    }

    private File flat(String key) {
        return new File(classes, TranscodeFlutterMojo.flatAssetName(key));
    }

    private void set(String name, Object value) throws Exception {
        for (Class<?> c = mojo.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(mojo, value);
                return;
            } catch (NoSuchFieldException keepLooking) {
                // up the chain
            }
        }
        throw new NoSuchFieldException(name);
    }
}

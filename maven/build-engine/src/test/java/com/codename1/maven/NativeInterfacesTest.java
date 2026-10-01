/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.maven;

import com.codename1.build.BuildFailureException;
import com.codename1.build.SystemStreamLog;
import com.codename1.maven.stubgen.TestNativeInterface;
import com.codename1.project.BuildSystem;
import com.codename1.project.NativePlatform;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The native-interface lifecycle on a Gradle-shaped project that starts with
/// no platform directories at all.
class NativeInterfacesTest {
    @TempDir
    File tmp;

    /// A classes directory holding only the fixture interface, so the scan
    /// finds exactly one.
    private File classes() throws Exception {
        File dir = new File(tmp, "classes");
        String path = TestNativeInterface.class.getName().replace('.', '/') + ".class";
        File out = new File(dir, path);
        out.getParentFile().mkdirs();
        try (InputStream in = TestNativeInterface.class.getClassLoader().getResourceAsStream(path)) {
            Files.copy(in, out.toPath());
        }
        return dir;
    }

    private ProjectLayout gradle() {
        File root = new File(tmp, "app");
        root.mkdirs();
        return ProjectLayouts.of(BuildSystem.GRADLE, ProjectKind.APP, root, root);
    }

    @Test
    void generatesOnlyIntoTheLayoutsNativeDirectories() throws Exception {
        ProjectLayout layout = gradle();
        NativeInterfaces ni = new NativeInterfaces(new SystemStreamLog(), classes(), Collections.<String>emptyList());
        assertEquals(Collections.singletonList(TestNativeInterface.class.getName()), ni.find());
        assertTrue(layout.existingNativePlatforms().isEmpty(), "nothing exists before generation");

        List<File> written = ni.generate(layout, null, false, false, false, "overwrite");
        assertFalse(written.isEmpty());
        String javase = TestNativeInterface.class.getName().replace('.', File.separatorChar) + "Impl.java";
        assertTrue(new File(layout.nativeSourceDir(NativePlatform.JAVASE), javase).isFile());
        assertTrue(new File(layout.nativeSourceDir(NativePlatform.ANDROID), javase).isFile());
        assertTrue(new File(new File(layout.rootDir(), "src/ios/objectivec"),
                TestNativeInterface.class.getName().replace('.', '_') + "Impl.m").isFile());
        // No Maven-style module directories.
        assertFalse(new File(layout.rootDir(), "javase").exists());
        assertFalse(new File(layout.rootDir(), "android").exists());
    }

    @Test
    void doesNotOverwriteUnlessAsked() throws Exception {
        ProjectLayout layout = gradle();
        NativeInterfaces ni = new NativeInterfaces(new SystemStreamLog(), classes(), Collections.<String>emptyList());
        ni.generate(layout, null, false, false, false, "overwrite");
        File impl = new File(layout.nativeSourceDir(NativePlatform.JAVASE),
                TestNativeInterface.class.getName().replace('.', File.separatorChar) + "Impl.java");
        Files.write(impl.toPath(), "// mine".getBytes("UTF-8"));

        assertTrue(ni.generate(layout, null, false, false, false, "overwrite").isEmpty());
        assertEquals("// mine", new String(Files.readAllBytes(impl.toPath()), "UTF-8"));

        ni.generate(layout, null, false, false, true, "overwrite");
        assertFalse("// mine".equals(new String(Files.readAllBytes(impl.toPath()), "UTF-8")));
    }

    @Test
    void reportsAMissingImplementationWithTheFileToCreate() throws Exception {
        ProjectLayout layout = gradle();
        NativeInterfaces ni = new NativeInterfaces(new SystemStreamLog(), classes(), Collections.<String>emptyList());
        assertEquals(1, ni.missingImplementations(layout, NativePlatform.IOS).size());

        ni.generate(layout, null, false, false, false, "overwrite");
        assertTrue(ni.missingImplementations(layout, NativePlatform.IOS).isEmpty());
        assertTrue(ni.missingImplementations(layout, NativePlatform.JAVASE).isEmpty());

        File impl = new File(layout.nativeSourceDir(NativePlatform.JAVASE),
                TestNativeInterface.class.getName().replace('.', File.separatorChar) + "Impl.java");
        assertTrue(impl.delete());
        List<String> missing = ni.missingImplementations(layout, NativePlatform.JAVASE);
        assertEquals(1, missing.size());
        assertTrue(missing.get(0).contains(impl.getPath()), missing.get(0));
    }

    @Test
    void anUnknownInterfaceNameIsRefused() throws Exception {
        NativeInterfaces ni = new NativeInterfaces(new SystemStreamLog(), classes(), Collections.<String>emptyList());
        assertThrows(BuildFailureException.class,
                () -> ni.generate(gradle(), "NoSuchThing", false, false, false, "overwrite"));
        ni.generate(gradle(), "TestNativeInterface", false, false, false, "overwrite");
    }
}

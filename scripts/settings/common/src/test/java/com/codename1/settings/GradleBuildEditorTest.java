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
package com.codename1.settings;

import com.codename1.settings.extensions.DependencyEditor;
import com.codename1.settings.extensions.GradleBuildEditor;
import com.codename1.settings.extensions.MavenDependency;
import com.codename1.settings.extensions.PomEditor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GradleBuildEditorTest {
    private static final MavenDependency JAR = new MavenDependency("com.example", "lib", "1.2");
    private static final MavenDependency CN1LIB = new MavenDependency("com.example", "ext", "2.0", "pom");

    @Test
    public void addsToTheExistingBlockKeepingItsIndentation() {
        String script = "plugins {\n    id(\"com.codenameone\")\n}\n\ndependencies {\n"
                + "  implementation(\"org.other:thing:1\")\n}\n";
        String updated = GradleBuildEditor.addDependency(script, JAR);
        assertEquals("plugins {\n    id(\"com.codenameone\")\n}\n\ndependencies {\n"
                + "  implementation(\"org.other:thing:1\")\n"
                + "  implementation(\"com.example:lib:1.2\")\n}\n", updated);
        assertTrue(GradleBuildEditor.containsDependency(updated, JAR));
        assertEquals(updated, GradleBuildEditor.addDependency(updated, JAR));
    }

    /// A cn1lib is the dependency a POM declares `<type>pom</type>`, and in
    /// Gradle it goes through the plugin's `cn1lib` configuration.
    @Test
    public void aCn1libGoesThroughTheCn1libConfiguration() {
        String updated = GradleBuildEditor.addDependency("dependencies {\n}\n", CN1LIB);
        assertEquals("dependencies {\n    cn1lib(\"com.example:ext:2.0\")\n}\n", updated);
    }

    @Test
    public void appendsABlockWhenThereIsNone() {
        String script = "plugins {\n    id(\"com.codenameone\")\n}";
        assertEquals("plugins {\n    id(\"com.codenameone\")\n}\n\ndependencies {\n"
                + "    implementation(\"com.example:lib:1.2\")\n}\n", GradleBuildEditor.addDependency(script, JAR));
        assertEquals("dependencies {\n    implementation(\"com.example:lib:1.2\")\n}\n",
                GradleBuildEditor.addDependency("", JAR));
    }

    /// `buildscript { dependencies { } }` is the build's own classpath, not
    /// the application's; neither a lookup nor an add may land there.
    @Test
    public void ignoresNestedDependencyBlocks() {
        String script = "buildscript {\n    dependencies {\n        classpath(\"com.example:lib:1.2\")\n    }\n}\n";
        assertFalse(GradleBuildEditor.containsDependency(script, JAR));
        String updated = GradleBuildEditor.addDependency(script, JAR);
        assertTrue(updated.startsWith(script));
        assertTrue(updated.endsWith("\ndependencies {\n    implementation(\"com.example:lib:1.2\")\n}\n"), updated);
    }

    /// Braces and coordinates inside comments and strings are not code.
    @Test
    public void ignoresCommentsAndStrings() {
        String script = "// dependencies { implementation(\"com.example:lib:1.2\") }\n"
                + "val note = \"dependencies {\"\n"
                + "dependencies {\n"
                + "    // implementation(\"com.example:lib:1.2\")\n"
                + "    /* } */\n"
                + "}\n";
        assertFalse(GradleBuildEditor.containsDependency(script, JAR));
        String updated = GradleBuildEditor.addDependency(script, JAR);
        assertTrue(updated.contains("    /* } */\n    implementation(\"com.example:lib:1.2\")\n}\n"), updated);
        assertTrue(updated.startsWith("// dependencies { implementation(\"com.example:lib:1.2\") }\n"));
    }

    @Test
    public void matchesByGroupAndArtifactWhateverTheVersionOrQuoting() {
        assertTrue(GradleBuildEditor.containsDependency(
                "dependencies {\n    implementation(\"com.example:lib:9.9\")\n}\n", JAR));
        assertTrue(GradleBuildEditor.containsDependency(
                "dependencies {\n    implementation 'com.example:lib:1.2'\n}\n", JAR));
        assertTrue(GradleBuildEditor.containsDependency(
                "dependencies {\n    cn1lib(\"com.example:lib\")\n}\n", JAR));
        assertFalse(GradleBuildEditor.containsDependency(
                "dependencies {\n    implementation(\"com.example:library:1.2\")\n}\n", JAR));
    }

    /// A declaration inside a named block is removed on its own; the block and its
    /// other entries stay.
    @Test
    public void removesANestedDeclarationWithoutItsSiblings() {
        String script = "dependencies {\n"
                + "    constraints {\n"
                + "        implementation(\"com.example:lib:1.2\")\n"
                + "        implementation(\"com.example:other:1\")\n"
                + "    }\n"
                + "    implementation(\"com.example:keep:1\") {\n"
                + "        exclude(group = \"com.example\", module = \"lib\")\n"
                + "    }\n"
                + "}\n";
        assertTrue(GradleBuildEditor.containsDependency(script, JAR));
        String updated = GradleBuildEditor.removeDependency(script, JAR);
        assertEquals("dependencies {\n"
                + "    constraints {\n"
                + "        implementation(\"com.example:other:1\")\n"
                + "    }\n"
                + "    implementation(\"com.example:keep:1\") {\n"
                + "        exclude(group = \"com.example\", module = \"lib\")\n"
                + "    }\n"
                + "}\n", updated);
    }

    @Test
    public void removesOnlyTheSelectedDeclaration() {
        String script = "dependencies {\n"
                + "    implementation(\"com.example:keep:1\")\n"
                + "    implementation(\"com.example:lib:1.2\") // pinned\n"
                + "    cn1lib(\"com.example:ext:2.0\")\n"
                + "}\n";
        String updated = GradleBuildEditor.removeDependency(script, JAR);
        assertEquals("dependencies {\n"
                + "    implementation(\"com.example:keep:1\")\n"
                + "    cn1lib(\"com.example:ext:2.0\")\n"
                + "}\n", updated);
        assertEquals(updated, GradleBuildEditor.removeDependency(updated, JAR));
        assertEquals("dependencies {\n    implementation(\"com.example:keep:1\")\n}\n",
                GradleBuildEditor.removeDependency(updated, CN1LIB));
    }

    @Test
    public void removesADeclarationWithAMultiLineConfiguration() {
        String script = "dependencies {\n"
                + "    implementation(\"com.example:lib:1.2\") {\n"
                + "        exclude(group = \"org.unwanted\")\n"
                + "    }\n"
                + "    implementation(\"com.example:keep:1\")\n"
                + "}\n";
        assertEquals("dependencies {\n    implementation(\"com.example:keep:1\")\n}\n",
                GradleBuildEditor.removeDependency(script, JAR));
    }

    @Test
    public void removesFromAOneLineBlock() {
        assertEquals("dependencies { }\n",
                GradleBuildEditor.removeDependency("dependencies { implementation(\"com.example:lib:1.2\") }\n", JAR));
    }

    @Test
    public void addsToAOneLineBlock() {
        assertEquals("dependencies { implementation(\"a:b:1\") \n    implementation(\"com.example:lib:1.2\")\n}\n",
                GradleBuildEditor.addDependency("dependencies { implementation(\"a:b:1\") }\n", JAR));
    }

    /// `${cn1.version}` is a Maven property; inside a Kotlin string it would be
    /// a template naming a variable the script lacks.
    @Test
    public void aMavenPropertyVersionIsNotWrittenIntoTheScript() {
        MavenDependency dep = new MavenDependency("com.codenameone", "cn1-extra", "${cn1.version}");
        String updated = GradleBuildEditor.addDependency("dependencies {\n}\n", dep);
        assertEquals("dependencies {\n    implementation(\"com.codenameone:cn1-extra\")\n}\n", updated);
        assertTrue(GradleBuildEditor.containsDependency(updated, dep));
    }

    @Test
    public void keepsCrlfLineEndingsOnRemoval() {
        String script = "dependencies {\r\n    implementation(\"com.example:lib:1.2\")\r\n"
                + "    implementation(\"a:b:1\")\r\n}\r\n";
        assertEquals("dependencies {\r\n    implementation(\"a:b:1\")\r\n}\r\n",
                GradleBuildEditor.removeDependency(script, JAR));
    }

    @Test
    public void theBuildSystemPicksTheEditor() {
        assertSame(GradleBuildEditor.INSTANCE, DependencyEditor.forBuildSystem("GRADLE"));
        assertSame(PomEditor.INSTANCE, DependencyEditor.forBuildSystem("MAVEN"));
        assertNull(DependencyEditor.forBuildSystem("ANT"));
        assertNull(DependencyEditor.forBuildSystem(null));
        assertEquals("build.gradle.kts", GradleBuildEditor.INSTANCE.fileLabel());
        assertEquals("common/pom.xml", PomEditor.INSTANCE.fileLabel());
    }
}

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
package com.codename1.maven;

import com.codename1.build.BuildFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MainClassPreflightTest {
    @TempDir
    Path tmp;

    private Properties settings(String pkg, String main) {
        Properties settings = new Properties();
        settings.setProperty("codename1.packageName", pkg);
        settings.setProperty("codename1.mainName", main);
        return settings;
    }

    private byte[] clazz(String name, String parent, boolean lifecycleMethods, int access) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, access, name, null, parent, null);
        if (lifecycleMethods) {
            for (String method : new String[] {"init", "start", "stop", "destroy"}) {
                boolean init = "init".equals(method);
                MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC, method,
                        init ? "(Ljava/lang/Object;)V" : "()V", null, null);
                mv.visitCode();
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, init ? 2 : 1);
                mv.visitEnd();
            }
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private byte[] app(String name) {
        return clazz(name, "java/lang/Object", true, Opcodes.ACC_PUBLIC);
    }

    private File jar(byte[]... classes) throws Exception {
        File file = Files.createTempFile(tmp, "app", ".jar").toFile();
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(file.toPath()))) {
            for (byte[] bytes : classes) {
                String name = new org.objectweb.asm.ClassReader(bytes).getClassName();
                out.putNextEntry(new JarEntry(name + ".class"));
                out.write(bytes);
                out.closeEntry();
            }
        }
        return file;
    }

    private String failure(File jar, Properties settings) {
        String message = assertThrows(BuildFailureException.class,
                () -> MainClassPreflight.validate(jar, settings)).getMessage();
        assertTrue(message.contains("mvn cn1:settings"), message);
        assertTrue(message.contains("codenameone_settings.properties"), message);
        assertTrue(message.contains("codename1.mainName"), message);
        assertTrue(message.contains("codename1.packageName"), message);
        return message;
    }

    @Test
    void matchingMainClassPassesWithoutRequiringLifecycleShape() throws Exception {
        MainClassPreflight.validate(jar(clazz("com/example/MyApp", "java/lang/Object", false,
                Opcodes.ACC_PUBLIC)), settings("com.example", "MyApp"));
    }

    @Test
    void renamedClassNamesTheMissingClassAndCandidate() throws Exception {
        String message = failure(jar(app("com/example/HelloWorld")), settings("com.example", "MyApp"));
        assertTrue(message.contains("main class com.example.MyApp"), message);
        assertTrue(message.contains("Possible main class: com.example.HelloWorld"), message);
    }

    @Test
    void movedPackageNamesTheCandidate() throws Exception {
        String message = failure(jar(app("com/other/MyApp")), settings("com.example", "MyApp"));
        assertTrue(message.contains("Possible main class: com.other.MyApp"), message);
    }

    @Test
    void ambiguousCandidatesDoNotGuess() throws Exception {
        String message = failure(jar(app("com/example/First"), app("com/example/Second")),
                settings("com.example", "MyApp"));
        assertFalse(message.contains("Possible main class"), message);
    }

    @Test
    void noCandidateStillExplainsTheFix() throws Exception {
        String message = failure(jar(clazz("com/example/Helper", "java/lang/Object", false,
                Opcodes.ACC_PUBLIC)), settings("com.example", "MyApp"));
        assertFalse(message.contains("Possible main class"), message);
    }

    @Test
    void lifecycleSubclassIsSuggestedWithoutLoadingFrameworkClasses() throws Exception {
        String message = failure(jar(clazz("com/example/NewApp", "com/codename1/system/Lifecycle",
                false, Opcodes.ACC_PUBLIC)), settings("com.example", "MyApp"));
        assertTrue(message.contains("Possible main class: com.example.NewApp"), message);
    }

    @Test
    void inheritedLifecycleIsRecognizedAndAbstractBaseIsNotACandidate() throws Exception {
        String message = failure(jar(
                clazz("com/example/Base", "com/codename1/system/Lifecycle", false,
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT),
                clazz("com/example/NewApp", "com/example/Base", false, Opcodes.ACC_PUBLIC)),
                settings("com.example", "MyApp"));
        assertTrue(message.contains("Possible main class: com.example.NewApp"), message);
    }

    @Test
    void missingSettingsExplainHowToConfigureTheMainClass() throws Exception {
        File jar = jar(app("com/example/MyApp"));
        failure(jar, new Properties());
        failure(jar, settings("com.example", " "));
        failure(jar, settings("", "MyApp"));
    }

    private TestProjectHost host(String platform, File dependency, Properties settings) throws Exception {
        TestProjectHost host = new TestProjectHost();
        host.projectDir = Files.createTempDirectory(tmp, "project").toFile();
        host.buildDir = new File(host.projectDir, "build");
        host.outputDir = new File(host.buildDir, "classes");
        assertTrue(host.outputDir.mkdirs());
        host.classpath.add(host.outputDir.getAbsolutePath());
        host.classpath.add(dependency.getAbsolutePath());
        host.projectProperties.setProperty("codename1.projectPlatform", platform);
        try (OutputStream out = Files.newOutputStream(new File(host.projectDir,
                "codenameone_settings.properties").toPath())) {
            settings.store(out, "test");
        }
        return host;
    }

    @Test
    void allTargetsRejectMissingMainBeforeUpdatingOrSubmitting() throws Exception {
        File dependency = jar(app("com/example/Renamed"));
        for (String[] target : new String[][] {
                {"android", "android-device"}, {"ios", "ios-device"},
                {"javascript", "javascript"}, {"javase", "mac-os-x-desktop"},
                {"javase", "windows-desktop"}, {"android", "android-source"},
                {"ios", "ios-source"}}) {
            TestProjectHost host = host(target[0], dependency, settings("com.example", "MyApp"));
            AppBuilder builder = new AppBuilder(host) {
                @Override
                protected void updateCodenameOne(boolean force) {
                    fail("Missing main class must fail before the build client runs");
                }
            }.platform(target[0]).buildTarget(target[1]);
            String message = assertThrows(BuildFailureException.class, builder::execute).getMessage();
            assertTrue(message.contains("main class com.example.MyApp"), message);
            assertTrue(message.contains("Possible main class: com.example.Renamed"), message);
        }
    }

    @Test
    void stagingAcceptsMainFromDependencyAndHonorsCommandLineOverrides() throws Exception {
        File dependency = jar(app("com/other/Renamed"));
        TestProjectHost host = host("android", dependency, settings("com.example", "MyApp"));
        host.userProperties.setProperty("codename1.packageName", "com.other");
        host.userProperties.setProperty("codename1.mainName", "Renamed");
        new AppBuilder(host).platform("android").buildTarget("android-device").stageOnly(true).execute();
        assertTrue(new File(host.buildDir, "test-android-device-jar-with-dependencies.jar").isFile());
    }

    @Test
    void stagingRejectsMissingMainToo() throws Exception {
        TestProjectHost host = host("android", jar(app("com/example/Renamed")),
                settings("com.example", "MyApp"));
        AppBuilder builder = new AppBuilder(host).platform("android").buildTarget("android-device")
                .stageOnly(true);
        assertTrue(assertThrows(BuildFailureException.class, builder::execute).getMessage()
                .contains("main class com.example.MyApp"));
    }
}

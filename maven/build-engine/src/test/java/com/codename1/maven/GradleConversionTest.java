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
import com.codename1.build.SystemStreamLog;
import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleConversionTest {
    private static final String UNTOUCHED_API = "package a.backend;\n"
            + "@RestController public class Api {\n"
            + "  @GetMapping(\"/healthz\") public String h() { return \"ok\"; }\n"
            + "  @PostMapping(\"/echo\") public String e(String s) { return s; }\n"
            + "}\n";

    @TempDir
    Path tmp;

    private static File touch(File base, String path, String content) throws IOException {
        File f = new File(base, path);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static GradleConversion converter() {
        return new GradleConversion(new SystemStreamLog());
    }

    private File antApp() throws IOException {
        File ant = new File(tmp.toFile(), "AntApp");
        touch(ant, "build.xml", "<project/>");
        touch(ant, "codenameone_settings.properties", "codename1.mainName=AntApp\ncodename1.packageName=a\n"
                + "codename1.cssTheme=true\ncodename1.arg.java.version=8\n");
        touch(ant, "icon.png", "png");
        touch(ant, "src/a/AntApp.java", "package a; public class AntApp {}");
        touch(ant, "src/a/Helper.kt", "package a\nclass Helper");
        touch(ant, "src/a/Old.mirah", "class Old; end");
        touch(ant, "src/theme.res", "stale");
        touch(ant, "src/messages.properties", "hello=Hello");
        touch(ant, "css/theme.css", "Form { color: red; }");
        touch(ant, "test/a/AntAppTest.java", "package a; public class AntAppTest {}");
        touch(ant, "native/android/a/MyNativeImpl.java", "package a; public class MyNativeImpl {}");
        touch(ant, "native/ios/a_MyNativeImpl.m", "// ios");
        touch(ant, "lib/CodenameOne.jar", "framework");
        touch(ant, "lib/json-helper.jar", "the app's own");
        touch(ant, "lib/impl/cls/a/FromCn1lib.class", "extracted");
        return ant;
    }

    private File mavenApp(String backendApi) throws IOException {
        File mvn = new File(tmp.toFile(), "mvnapp");
        touch(mvn, "pom.xml", "<project><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version><properties><maps.version>1.2</maps.version></properties></project>");
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><dependencies>"
                + "<dependency><groupId>com.codenameone</groupId><artifactId>codenameone-core</artifactId></dependency>"
                + "<dependency><groupId>com.acme</groupId><artifactId>maps-lib</artifactId>"
                + "<version>${maps.version}</version><type>pom</type></dependency>"
                + "<dependency><groupId>com.codenameone</groupId><artifactId>googlemaps-lib</artifactId>"
                + "<version>1.0</version><type>pom</type></dependency>"
                + "<dependency><groupId>org.example</groupId><artifactId>util</artifactId><version>2.0</version>"
                + "</dependency>"
                + "<dependency><groupId>org.example</groupId><artifactId>api</artifactId><version>${api.version}</version>"
                + "<scope>provided</scope></dependency>"
                + "<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4</version>"
                + "<scope>test</scope></dependency>"
                + "</dependencies></project>");
        touch(mvn, "common/codenameone_settings.properties", "codename1.mainName=MvnApp\ncodename1.arg.java.version=17\n");
        touch(mvn, "common/icon.png", "png");
        touch(mvn, "common/src/main/java/a/MvnApp.java", "package a; public class MvnApp {}");
        touch(mvn, "common/src/main/css/theme.css", "Form {}");
        touch(mvn, "android/pom.xml", "<project/>");
        touch(mvn, "android/src/main/java/a/MyNativeImpl.java", "package a; public class MyNativeImpl {}");
        touch(mvn, "android/src/main/resources/android.png", "png");
        touch(mvn, "ios/src/main/objectivec/.gitignore", "");
        touch(mvn, "backend/pom.xml", "<project><dependencies>"
                + "<dependency><groupId>com.codenameone</groupId><artifactId>codenameone-backend</artifactId>"
                + "</dependency>"
                + "<dependency><groupId>org.example</groupId><artifactId>payments</artifactId><version>3.1</version>"
                + "</dependency>"
                + "</dependencies></project>");
        touch(mvn, "backend/application.properties", "cn1.server.port=8080\n");
        touch(mvn, "backend/application-prod.properties", "cn1.server.port=80\n");
        touch(mvn, "backend/src/main/java/a/backend/Api.java", backendApi);
        return mvn;
    }

    @Test
    void anAntProjectIsSortedIntoTheGradleLayout() throws Exception {
        File out = new File(tmp.toFile(), "out");
        ProjectLayout to = converter().convert(antApp(), out, "9.9.9");

        assertEquals(BuildSystem.GRADLE, to.buildSystem());
        assertTrue(new File(out, "src/main/java/a/AntApp.java").isFile());
        assertTrue(new File(out, "src/main/kotlin/a/Helper.kt").isFile(), "Kotlin is not a resource");
        assertFalse(new File(out, "src/main/resources/a/Helper.kt").exists());
        assertTrue(new File(out, "src/main/resources/messages.properties").isFile());
        assertFalse(new File(out, "src/main/resources/theme.res").exists(),
                "a CSS project's saved theme.res would shadow the compiled one");
        assertFalse(new File(out, "src/main/resources/a/Old.mirah").exists());
        assertTrue(new File(out, "src/main/css/theme.css").isFile());
        assertTrue(new File(out, "src/test/java/a/AntAppTest.java").isFile());
        assertTrue(new File(out, "src/android/java/a/MyNativeImpl.java").isFile());
        assertTrue(new File(out, "src/ios/objectivec/a_MyNativeImpl.m").isFile());
        assertFalse(new File(out, "src/javase").exists(), "only the platforms that have code get a directory");
        assertTrue(new File(out, "icon.png").isFile());

        String settings = read(new File(out, "codenameone_settings.properties"));
        assertTrue(settings.contains("codename1.arg.java.version=17"), settings);
        assertFalse(settings.contains("java.version=8"), settings);
        assertTrue(read(new File(out, "settings.gradle.kts")).contains("id(\"com.codenameone\") version \"9.9.9\""));
        assertTrue(read(new File(out, "settings.gradle.kts")).contains("rootProject.name = \"AntApp\""));
        String build = read(new File(out, "build.gradle.kts"));
        int plugins = build.indexOf("kotlin(\"jvm\") version \"" + GradleConversion.KOTLIN_VERSION + "\"");
        assertTrue(plugins > 0 && plugins < build.indexOf("dependencies {"),
                "a Kotlin project needs the Kotlin plugin, first in the script: " + build);
        assertTrue(new File(out, "gradlew").canExecute());
        assertTrue(new File(out, "libs/json-helper.jar").isFile(), "the app's own jar comes along");
        assertFalse(new File(out, "libs/CodenameOne.jar").exists(), "the plugin supplies the framework");
        assertTrue(build.contains("implementation(files(\"libs/json-helper.jar\"))"), build);
        assertFalse(build.contains("CodenameOne.jar"), build);

        ProjectLayout detected = ProjectLayouts.detect(out);
        assertEquals(BuildSystem.GRADLE, detected.buildSystem());
        assertEquals(ProjectKind.APP, detected.kind());
    }

    @Test
    void aJavaOnlyProjectGetsNoKotlinPlugin() throws Exception {
        File ant = antApp();
        assertTrue(new File(ant, "src/a/Helper.kt").delete());
        File out = new File(tmp.toFile(), "out");
        converter().convert(ant, out, "1.0");
        assertFalse(read(new File(out, "build.gradle.kts")).contains("kotlin("));
    }

    @Test
    void legacyCn1libsAreRefusedByName() throws Exception {
        File ant = antApp();
        touch(ant, "lib/Maps.cn1lib", "zip");
        File out = new File(tmp.toFile(), "out");
        BuildFailureException ex = assertThrows(BuildFailureException.class,
                () -> converter().convert(ant, out, "1.0"));
        assertTrue(ex.getMessage().contains("Maps.cn1lib"), ex.getMessage());
        assertTrue(ex.getMessage().contains("cn1lib(\""), "the refusal says what to do instead");
        assertFalse(new File(out, "settings.gradle.kts").exists(), "nothing is half converted");
    }

    @Test
    void aMavenProjectsCn1libsModuleIsRefusedToo() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "cn1libs/Maps/jars/main.zip", "zip");
        BuildFailureException ex = assertThrows(BuildFailureException.class,
                () -> converter().convert(mvn, new File(tmp.toFile(), "out"), "1.0"));
        assertTrue(ex.getMessage().contains("Maps"), ex.getMessage());
    }

    @Test
    void aMavenProjectKeepsItsCodeAndDependencies() throws Exception {
        File out = new File(tmp.toFile(), "out");
        converter().convert(mavenApp(UNTOUCHED_API), out, "1.0");

        assertTrue(new File(out, "src/main/java/a/MvnApp.java").isFile());
        assertTrue(new File(out, "src/main/css/theme.css").isFile());
        assertTrue(new File(out, "src/android/java/a/MyNativeImpl.java").isFile());
        assertTrue(new File(out, "src/android/resources/android.png").isFile());
        assertFalse(new File(out, "src/ios").exists(), "an empty native tree is not carried over");
        assertFalse(new File(out, "backend").exists(), "the untouched skeleton is left behind");
        assertFalse(new File(out, "pom.xml").exists());
        assertFalse(new File(out, "common").exists());

        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("cn1lib(\"com.acme:maps-lib:1.2\")"),
                "a property version resolves through the parent pom: " + build);
        assertTrue(build.contains("cn1lib(\"com.codenameone:googlemaps-lib:1.0\")"),
                "a cn1lib in the com.codenameone group is not the framework: " + build);
        assertTrue(build.contains("implementation(\"org.example:util:2.0\")"), build);
        assertTrue(build.contains("// compileOnly(\"org.example:api:VERSION\") -- set the version"),
                "a version no property defines is commented out, not written as a Kotlin template: " + build);
        assertFalse(build.contains("\"org.example:api:${"), build);
        assertFalse(build.contains("codenameone-core"), "the plugin adds the framework: " + build);
        assertTrue(build.contains("testImplementation(\"junit:junit:4\")"),
                "the copied tests keep their libraries: " + build);
    }

    @Test
    void aBackendWithCodeOfItsOwnIsConverted() throws Exception {
        File out = new File(tmp.toFile(), "out");
        int end = UNTOUCHED_API.lastIndexOf('}');
        String api = UNTOUCHED_API.substring(0, end)
                + "  @GetMapping(\"/orders\") public String o() { return \"\"; }\n}\n";
        converter().convert(mavenApp(api), out, "1.0");
        assertTrue(new File(out, "backend/src/main/java/a/backend/Api.java").isFile());
        assertTrue(new File(out, "backend/application.properties").isFile());
        assertTrue(new File(out, "backend/application-prod.properties").isFile());
        assertFalse(new File(out, "backend/pom.xml").exists());
        String backendBuild = read(new File(out, "backend/build.gradle.kts"));
        assertTrue(backendBuild.contains("implementation(\"org.example:payments:3.1\")"),
                "the backend keeps its own libraries: " + backendBuild);
        assertFalse(backendBuild.contains("codenameone-backend"), "the plugin adds the runtime: " + backendBuild);
        assertFalse(backendBuild.contains("kotlin("), "a Java backend needs no Kotlin plugin: " + backendBuild);
    }

    @Test
    void aKotlinBackendGetsTheKotlinPluginInItsOwnScript() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "backend/src/main/kotlin/a/backend/Orders.kt", "package a.backend\nclass Orders");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        assertTrue(read(new File(out, "backend/build.gradle.kts")).contains("kotlin(\"jvm\")"));
        assertFalse(read(new File(out, "build.gradle.kts")).contains("kotlin(\"jvm\")"),
                "the application has no Kotlin of its own");
    }

    @Test
    void theUntouchedSkeletonIsKeptOnRequest() throws Exception {
        File out = new File(tmp.toFile(), "out");
        converter().includeUntouchedBackend(true).convert(mavenApp(UNTOUCHED_API), out, "1.0");
        assertTrue(new File(out, "backend/application.properties").isFile());
    }

    @Test
    void refusesANonEmptyTargetAndAGradleSource() throws Exception {
        File ant = antApp();
        File out = new File(tmp.toFile(), "out");
        touch(out, "keep.txt", "x");
        assertThrows(BuildFailureException.class, () -> converter().convert(ant, out, "1.0"));

        File converted = new File(tmp.toFile(), "converted");
        converter().convert(ant, converted, "1.0");
        assertThrows(BuildFailureException.class,
                () -> converter().convert(converted, new File(tmp.toFile(), "again"), "1.0"));
    }
}

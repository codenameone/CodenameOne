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
    /// The backend Api the archetype and the Gradle template generate, in package
    /// a.backend: the one a conversion may leave behind as untouched.
    private static final String UNTOUCHED_API = template("backend/Api.java.txt")
            .replace("${package}", "a.backend").replace("__BACKEND__", ":backend:");

    private static final String GENERATED_BACKEND_POM = "<project><dependencies>"
            + "<dependency><groupId>com.codenameone</groupId><artifactId>codenameone-backend</artifactId>"
            + "</dependency>"
            + "<dependency><groupId>org.xerial</groupId><artifactId>sqlite-jdbc</artifactId><version>3</version>"
            + "</dependency>"
            + "</dependencies><build><plugins><plugin><groupId>com.codenameone</groupId>"
            + "<artifactId>codenameone-maven-plugin</artifactId></plugin></plugins></build></project>";

    /// [#GENERATED_BACKEND_POM] plus a library of the developer's own.
    private static final String BACKEND_POM_WITH_PAYMENTS = GENERATED_BACKEND_POM.replace("</dependencies>",
            "<dependency><groupId>org.example</groupId><artifactId>payments</artifactId><version>3.1</version>"
                    + "</dependency></dependencies>");

    private static String template(String path) {
        try {
            return GradleProjectTemplate.text(path);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

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
                + "<version>1.0</version><properties><maps.version>1.2</maps.version></properties>"
                + "<dependencyManagement><dependencies><dependency><groupId>org.example</groupId>"
                + "<artifactId>managed</artifactId><version>5.0</version></dependency></dependencies>"
                + "</dependencyManagement></project>");
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
                + "<dependency><groupId>org.example</groupId><artifactId>managed</artifactId></dependency>"
                + "<dependency><groupId>org.example</groupId><artifactId>unmanaged</artifactId></dependency>"
                + "<dependency><groupId>org.example</groupId><artifactId>natives</artifactId><version>2</version>"
                + "<classifier>linux</classifier><type>zip</type></dependency>"
                + "<dependency><groupId>org.example</groupId><artifactId>fixtures</artifactId><version>3</version>"
                + "<type>test-jar</type><scope>test</scope></dependency>"
                + "</dependencies></project>");
        touch(mvn, "common/codenameone_settings.properties", "codename1.mainName=MvnApp\ncodename1.arg.java.version=17\n");
        touch(mvn, "common/icon.png", "png");
        touch(mvn, "common/src/main/java/a/MvnApp.java", "package a; public class MvnApp {}");
        touch(mvn, "common/src/main/css/theme.css", "Form {}");
        touch(mvn, "android/pom.xml", "<project/>");
        touch(mvn, "android/src/main/java/a/MyNativeImpl.java", "package a; public class MyNativeImpl {}");
        touch(mvn, "android/src/main/resources/android.png", "png");
        touch(mvn, "ios/src/main/objectivec/.gitignore", "");
        // As the archetype writes it: the runtime and the SQLite driver.
        touch(mvn, "backend/pom.xml", GENERATED_BACKEND_POM);
        touch(mvn, "backend/application.properties", template("backend/application.properties.txt"));
        touch(mvn, "backend/application-dev.properties", template("backend/application-dev.properties.txt"));
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
    void anAntProjectsIosExtrasGoWhereTheGradleBuildReadsThem() throws Exception {
        File ant = antApp();
        touch(ant, "native/ios/app_extensions/Widget.zip", "zip");
        touch(ant, "native/ios/strings/fr.lproj/Localizable.strings", "\"a\" = \"b\";");
        File out = new File(tmp.toFile(), "out");
        converter().convert(ant, out, "1.0");
        assertTrue(new File(out, "src/ios/app_extensions/Widget.zip").isFile());
        assertTrue(new File(out, "src/ios/strings/fr.lproj/Localizable.strings").isFile());
        assertFalse(new File(out, "src/ios/objectivec/app_extensions").exists(), "not as native sources");
        assertFalse(new File(out, "src/ios/objectivec/strings").exists(), "not as native sources");
        assertTrue(new File(out, "src/ios/objectivec/a_MyNativeImpl.m").isFile());
    }

    @Test
    void anEmptyRelativePathTakesNothingFromTheDirectoryAbove() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "pom.xml", "<project><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version><dependencies><dependency><groupId>org.example</groupId>"
                + "<artifactId>inherited</artifactId><version>7</version></dependency></dependencies></project>");
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>corporate</artifactId>"
                + "<version>1.0</version><relativePath/></parent><dependencies>"
                + "<dependency><groupId>org.example</groupId><artifactId>util</artifactId><version>2.0</version>"
                + "</dependency></dependencies></project>");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("implementation(\"org.example:util:2.0\")"), build);
        assertFalse(build.contains("inherited"), "the parent comes from a repository, not ..: " + build);
    }

    @Test
    void anAntProjectsGuiBuilderAndViewSourcesComeAlong() throws Exception {
        File ant = antApp();
        touch(ant, "res/guibuilder/a/Main.gui", "<component/>");
        touch(ant, "rad/views/a/MyView.xml", "<y/>");
        File out = new File(tmp.toFile(), "out");
        converter().convert(ant, out, "1.0");
        assertTrue(new File(out, "src/main/guibuilder/a/Main.gui").isFile());
        assertTrue(new File(out, "src/main/rad/views/a/MyView.xml").isFile());
    }

    @Test
    void aProvidedDependencyIsOnTheTestClasspathToo() throws Exception {
        File out = new File(tmp.toFile(), "out");
        converter().convert(mavenApp(UNTOUCHED_API), out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("compileOnly(\"org.example:api:"), build);
        assertTrue(build.contains("testImplementation(\"org.example:api:"), build);
        assertFalse(build.contains("implementation(\"org.example:api:"), "never in the application: " + build);
    }

    @Test
    void profileDependenciesFollowWhatAPlainMavenBuildActivates() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/src/main/extra/marker", "");
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><dependencies>"
                + "<dependency><groupId>org.example</groupId><artifactId>util</artifactId><version>2.0</version>"
                + "</dependency></dependencies><profiles>"
                + "<profile><id>present</id><activation><file><exists>${basedir}/src/main/extra</exists></file>"
                + "</activation><dependencies><dependency><groupId>org.example</groupId><artifactId>present</artifactId>"
                + "<version>1</version></dependency></dependencies></profile>"
                + "<profile><id>absent</id><activation><file><exists>${basedir}/nowhere</exists></file>"
                + "</activation><dependencies><dependency><groupId>org.example</groupId><artifactId>absent</artifactId>"
                + "<version>1</version></dependency></dependencies></profile>"
                + "<profile><id>ci</id><activation><property><name>ci</name></property></activation>"
                + "<dependencies><dependency><groupId>org.example</groupId><artifactId>ci-only</artifactId>"
                + "<version>1</version></dependency></dependencies></profile>"
                + "</profiles></project>");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("\n    implementation(\"org.example:present:1\")"), build);
        assertFalse(build.contains("org.example:absent"), "a file activation that fails is off: " + build);
        assertTrue(build.contains("profile 'ci' of pom.xml"), build);
        assertTrue(build.contains("    // implementation(\"org.example:ci-only:1\")"),
                "undecided: written, but commented out: " + build);
    }

    @Test
    void anActivatedProfileSwitchesTheDefaultOneOff() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/production.marker", "");
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><profiles>"
                + "<profile><id>dev</id><activation><activeByDefault>true</activeByDefault></activation>"
                + "<dependencies><dependency><groupId>org.example</groupId><artifactId>dev-db</artifactId>"
                + "<version>1</version></dependency></dependencies></profile>"
                + "<profile><id>prod</id><activation><file><exists>production.marker</exists></file>"
                + "</activation><dependencies><dependency><groupId>org.example</groupId><artifactId>prod-db</artifactId>"
                + "<version>1</version></dependency></dependencies></profile>"
                + "</profiles></project>");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("implementation(\"org.example:prod-db:1\")"), build);
        assertFalse(build.contains("dev-db"), "Maven drops the default profile once another is on: " + build);

        // With nothing else on, the default profile is.
        if (!new File(mvn, "common/production.marker").delete()) {
            throw new IOException("could not delete the marker");
        }
        File again = new File(tmp.toFile(), "again");
        converter().convert(mvn, again, "1.0");
        String dev = read(new File(again, "build.gradle.kts"));
        assertTrue(dev.contains("implementation(\"org.example:dev-db:1\")"), dev);
        assertFalse(dev.contains("prod-db"), dev);
    }

    @Test
    void anActiveProfilesRepositoryComesAlongAndATestCn1libIsNotShipped() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><dependencies>"
                + "<dependency><groupId>com.acme</groupId><artifactId>fixtures-lib</artifactId><version>1</version>"
                + "<type>pom</type><scope>test</scope></dependency></dependencies><profiles>"
                + "<profile><id>vendor</id><activation><activeByDefault>true</activeByDefault></activation>"
                + "<repositories><repository><id>v</id><url>https://maven.vendor.example/repo</url></repository>"
                + "</repositories><dependencies><dependency><groupId>com.vendor</groupId><artifactId>sdk</artifactId>"
                + "<version>2</version></dependency></dependencies></profile>"
                + "<profile><id>off</id><activation><property><name>x</name></property></activation>"
                + "<repositories><repository><id>o</id><url>https://maven.off.example/repo</url></repository>"
                + "</repositories></profile>"
                + "</profiles></project>");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("implementation(\"com.vendor:sdk:2\")"), build);
        assertTrue(build.contains("maven(url = uri(\"https://maven.vendor.example/repo\"))"), build);
        assertFalse(build.contains("maven.off.example"), "an undecided profile's repository stays out: " + build);
        assertFalse(build.contains("\n    cn1lib(\"com.acme:fixtures-lib"), "a test cn1lib is not shipped: " + build);
        assertTrue(build.contains("// cn1lib(\"com.acme:fixtures-lib:1\") -- test-scoped"), build);
    }

    @Test
    void anAntProjectsSimulatorJarsBecomeJavaseDependencies() throws Exception {
        File ant = antApp();
        touch(ant, "native/javase/vendor-sim.jar", "jar");
        touch(ant, "native/javase/a/MyNativeImpl.java", "package a; public class MyNativeImpl {}");
        File out = new File(tmp.toFile(), "out");
        converter().convert(ant, out, "1.0");
        assertTrue(new File(out, "libs/javase/vendor-sim.jar").isFile());
        assertFalse(new File(out, "src/javase/java/vendor-sim.jar").exists(), "not a source file");
        assertTrue(new File(out, "src/javase/java/a/MyNativeImpl.java").isFile());
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("javaseImplementation(files(\"libs/javase/vendor-sim.jar\"))"), build);
    }

    @Test
    void aJavaProjectCallingTheKotlinRuntimeKeepsTheStdlib() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><dependencies>"
                + "<dependency><groupId>org.jetbrains.kotlin</groupId><artifactId>kotlin-stdlib</artifactId>"
                + "<version>2.0</version></dependency></dependencies></project>");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("implementation(\"org.jetbrains.kotlin:kotlin-stdlib:2.0\")"),
                "no Kotlin plugin supplies it here: " + build);
    }

    @Test
    void sourceRootsThePomAddsAreCompiledAfterConversion() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><build><sourceDirectory>src/java</sourceDirectory>"
                + "<resources><resource><directory>${project.basedir}/assets</directory></resource></resources>"
                + "<plugins><plugin><groupId>org.codehaus.mojo</groupId><artifactId>build-helper-maven-plugin</artifactId>"
                + "<executions><execution><goals><goal>add-source</goal></goals><configuration><sources>"
                + "<source>generated-by-hand</source></sources></configuration></execution>"
                + "<execution><goals><goal>add-test-source</goal></goals><configuration><sources>"
                + "<source>it/java</source></sources></configuration></execution></executions>"
                + "</plugin></plugins></build></project>");
        touch(mvn, "common/src/java/a/Moved.java", "package a; public class Moved {}");
        touch(mvn, "common/assets/logo.txt", "logo");
        touch(mvn, "common/generated-by-hand/a/Helper.java", "package a; public class Helper {}");
        touch(mvn, "common/it/java/a/ItTest.java", "package a; public class ItTest {}");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        assertTrue(new File(out, "src/main/java/a/Moved.java").isFile());
        assertFalse(new File(out, "src/java").exists(), "the uncompiled copy under src/ is gone");
        assertTrue(new File(out, "src/main/resources/logo.txt").isFile());
        assertTrue(new File(out, "src/main/java/a/Helper.java").isFile());
        assertTrue(new File(out, "src/test/java/a/ItTest.java").isFile());
        assertFalse(new File(out, "src/main/java/a/MvnApp.java").exists(),
                "<sourceDirectory> replaces src/main/java in Maven, so it is not compiled here either");
    }

    @Test
    void resourcesMavenLeavesOutAreNotPackagedEither() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><build><resources>"
                + "<resource><directory>src/main/resources</directory><excludes><exclude>**/*.env</exclude>"
                + "</excludes></resource>"
                + "<resource><directory>src/main/resources</directory><includes><include>keep/prod.env</include>"
                + "</includes></resource>"
                + "<resource><directory>extra</directory><includes><include>**/*.json</include></includes>"
                + "<filtering>true</filtering></resource>"
                + "</resources></build></project>");
        touch(mvn, "common/src/main/resources/app.properties", "a=b");
        touch(mvn, "common/src/main/resources/secrets/dev.env", "TOKEN=x");
        touch(mvn, "common/src/main/resources/keep/prod.env", "kept by the second entry");
        touch(mvn, "common/extra/data/config.json", "{}");
        touch(mvn, "common/extra/data/notes.txt", "not included");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        assertTrue(new File(out, "src/main/resources/app.properties").isFile());
        assertFalse(new File(out, "src/main/resources/secrets/dev.env").exists(), "excluded by the pom");
        assertTrue(new File(out, "src/main/resources/keep/prod.env").isFile(), "another entry includes it");
        assertTrue(new File(out, "src/main/resources/data/config.json").isFile());
        assertFalse(new File(out, "src/main/resources/data/notes.txt").exists(), "not among the includes");
    }

    @Test
    void mavenPathPatternsMatchAsMavenDoes() {
        assertTrue(GradleConversion.antMatches("**/*.env", "a/b/c.env"));
        assertTrue(GradleConversion.antMatches("**/*.env", "c.env"));
        assertFalse(GradleConversion.antMatches("*.env", "a/c.env"));
        assertTrue(GradleConversion.antMatches("secrets/", "secrets/x/y.txt"));
        assertTrue(GradleConversion.antMatches("a?c.txt", "abc.txt"));
        assertFalse(GradleConversion.antMatches("a?c.txt", "a/c.txt"));
    }

    @Test
    void platformModuleDependenciesAreNotDroppedSilently() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><artifactId>mvnapp-common</artifactId><dependencies>"
                + "<dependency><groupId>org.example</groupId><artifactId>util</artifactId><version>2.0</version>"
                + "</dependency></dependencies></project>");
        touch(mvn, "javase/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><repositories><repository><id>desk</id>"
                + "<url>https://maven.desktop.example/repo</url></repository></repositories><dependencies>"
                + "<dependency><groupId>com.acme</groupId><artifactId>mvnapp-common</artifactId><version>1.0</version>"
                + "</dependency>"
                + "<dependency><groupId>com.codenameone</groupId><artifactId>codenameone-javase</artifactId>"
                + "<version>1</version><scope>provided</scope></dependency>"
                + "<dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId>"
                + "<version>5</version><scope>test</scope></dependency>"
                + "<dependency><groupId>org.example</groupId><artifactId>desktop-only</artifactId><version>3</version>"
                + "</dependency>"
                + "<dependency><groupId>org.example</groupId><artifactId>util</artifactId><version>2.0</version>"
                + "</dependency></dependencies></project>");
        touch(mvn, "android/pom.xml", "<project><dependencies>"
                + "<dependency><groupId>org.example</groupId><artifactId>android-sdk-glue</artifactId>"
                + "<version>4</version></dependency></dependencies></project>");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("    javaseImplementation(\"org.example:desktop-only:3\")"), build);
        assertTrue(build.contains("maven(url = uri(\"https://maven.desktop.example/repo\"))"),
                "where the JavaSE dependency resolves from: " + build);
        assertFalse(build.contains("mvnapp-common"), "the application's own module is not a dependency: " + build);
        assertFalse(build.contains("junit-jupiter"), "the platform modules' tests are not converted: " + build);
        assertFalse(build.contains("javaseImplementation(\"org.example:util"), "already common's: " + build);
        assertTrue(build.contains("    // implementation(\"org.example:android-sdk-glue:4\")"), build);
        assertTrue(build.contains("android/pom.xml declared these for the android build only"), build);
    }

    @Test
    void aBackendWhosePomWasChangedIsKeptEvenWithTheGeneratedCode() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "backend/pom.xml", BACKEND_POM_WITH_PAYMENTS);
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        assertTrue(new File(out, "backend/src/main/java/a/backend/Api.java").isFile(),
                "a library added to the pom is someone's work");
        assertTrue(read(new File(out, "backend/build.gradle.kts")).contains("org.example:payments:3.1"));
        assertTrue(GradleConversion.pomAsGenerated(new File(mavenApp(UNTOUCHED_API), "backend/pom.xml")),
                "the archetype's own pom is still the skeleton");
    }

    @Test
    void whatThePomReplacesIsLeftOutAndTargetPathsAreKept() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><dependencies>"
                + "<dependency><groupId>org.example</groupId><artifactId>driver</artifactId><version>9</version>"
                + "<scope>runtime</scope></dependency></dependencies>"
                + "<build><sourceDirectory>src/app</sourceDirectory><resources>"
                + "<resource><directory>meta</directory><targetPath>META-INF/services</targetPath></resource>"
                + "</resources></build></project>");
        touch(mvn, "common/src/app/a/Real.java", "package a; public class Real {}");
        touch(mvn, "common/src/main/resources/stale.properties", "x=y");
        touch(mvn, "common/meta/com.example.Spi", "a.Real");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        assertTrue(new File(out, "src/main/java/a/Real.java").isFile());
        assertFalse(new File(out, "src/main/java/a/MvnApp.java").exists(),
                "Maven compiled src/app instead of src/main/java");
        assertFalse(new File(out, "src/main/resources/stale.properties").exists(),
                "declaring <resources> leaves src/main/resources out");
        assertTrue(new File(out, "src/main/resources/META-INF/services/com.example.Spi").isFile(),
                "the resource keeps its targetPath");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("runtimeOnly(\"org.example:driver:9\")"), build);
        assertTrue(build.contains("testCompileOnly(\"org.example:driver:9\")"),
                "Maven compiles the tests against runtime dependencies: " + build);
    }

    @Test
    void theIconTheSettingsNameComesAlong() throws Exception {
        File ant = antApp();
        touch(ant, "codenameone_settings.properties", "codename1.mainName=AntApp\ncodename1.packageName=a\n"
                + "codename1.icon=branding/app.png\n");
        touch(ant, "branding/app.png", "png");
        File out = new File(tmp.toFile(), "out");
        converter().convert(ant, out, "1.0");
        assertTrue(new File(out, "branding/app.png").isFile());
        assertTrue(read(new File(out, "codenameone_settings.properties")).contains("codename1.icon=branding/app.png"));
    }

    @Test
    void aScopeGivenByAPropertyIsTheScopeMavenUses() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><properties><fixtures.scope>test</fixtures.scope></properties>"
                + "<dependencies><dependency><groupId>org.example</groupId><artifactId>fixtures</artifactId>"
                + "<version>1</version><scope>${fixtures.scope}</scope></dependency></dependencies></project>");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("testImplementation(\"org.example:fixtures:1\")"), build);
        assertFalse(build.contains("\n    implementation(\"org.example:fixtures"), "not shipped: " + build);
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
        assertTrue(build.contains("implementation(\"org.example:managed:5.0\")"),
                "a version left to the parent's dependencyManagement is resolved: " + build);
        assertTrue(build.contains("// implementation(\"org.example:unmanaged:VERSION\")"),
                "an unresolvable managed version is commented out, not left versionless: " + build);
        assertTrue(build.contains("implementation(\"org.example:natives:2:linux@zip\")"),
                "the classifier and type select the same artifact Maven used: " + build);
        assertTrue(build.contains("testImplementation(\"org.example:fixtures:3:tests\")"), build);
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
        File mvn = mavenApp(api);
        touch(mvn, "backend/pom.xml", BACKEND_POM_WITH_PAYMENTS);
        touch(mvn, "backend/application-prod.properties", "cn1.server.port=80\n");
        converter().convert(mvn, out, "1.0");
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

    /// Gradle refuses a version for a plugin the root project already put on
    /// the classpath, so a Kotlin backend under a Kotlin application names none.
    @Test
    void aKotlinBackendUnderAKotlinAppDeclaresNoSecondVersion() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "common/src/main/kotlin/a/Helper.kt", "package a\nclass Helper");
        touch(mvn, "backend/src/main/kotlin/a/backend/Orders.kt", "package a.backend\nclass Orders");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        assertTrue(read(new File(out, "build.gradle.kts")).contains("kotlin(\"jvm\") version"));
        String backend = read(new File(out, "backend/build.gradle.kts"));
        assertTrue(backend.contains("kotlin(\"jvm\")") && !backend.contains("kotlin(\"jvm\") version"), backend);
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

    /// The effective dependencies, as Maven sees them: inherited ones, the
    /// exclusions, a system jar, the pom's repositories, and only the Kotlin
    /// artifacts the Kotlin plugin supplies left out.
    @Test
    void dependenciesConvertAsMavenResolvesThem() throws Exception {
        File mvn = mavenApp(UNTOUCHED_API);
        touch(mvn, "pom.xml", "<project><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version><properties><maps.version>1.2</maps.version></properties>"
                + "<repositories><repository><id>acme</id><url>https://maven.acme.example/releases</url>"
                + "</repository><repository><id>central</id><url>https://repo.maven.apache.org/maven2</url>"
                + "</repository></repositories>"
                + "<dependencies><dependency><groupId>org.example</groupId><artifactId>inherited</artifactId>"
                + "<version>7</version></dependency></dependencies></project>");
        touch(mvn, "common/lib/vendor.jar", "jar");
        touch(mvn, "common/pom.xml", "<project><parent><groupId>com.acme</groupId><artifactId>mvnapp</artifactId>"
                + "<version>1.0</version></parent><dependencies>"
                + "<dependency><groupId>org.example</groupId><artifactId>heavy</artifactId><version>1</version>"
                + "<exclusions><exclusion><groupId>commons-logging</groupId><artifactId>commons-logging</artifactId>"
                + "</exclusion></exclusions></dependency>"
                + "<dependency><groupId>com.vendor</groupId><artifactId>vendor</artifactId><version>1</version>"
                + "<scope>system</scope><systemPath>${basedir}/lib/vendor.jar</systemPath></dependency>"
                + "<dependency><groupId>org.jetbrains.kotlin</groupId><artifactId>kotlin-stdlib</artifactId>"
                + "<version>2.0</version></dependency>"
                + "<dependency><groupId>org.jetbrains.kotlin</groupId><artifactId>kotlin-reflect</artifactId>"
                + "<version>2.0</version></dependency>"
                + "</dependencies></project>");
        // Kotlin sources: the Kotlin plugin is applied and supplies the stdlib.
        touch(mvn, "common/src/main/kotlin/a/K.kt", "package a\nclass K");
        File out = new File(tmp.toFile(), "out");
        converter().convert(mvn, out, "1.0");
        String build = read(new File(out, "build.gradle.kts"));
        assertTrue(build.contains("implementation(\"org.example:inherited:7\")"), "inherited from the parent: " + build);
        assertTrue(build.contains("implementation(\"org.example:heavy:1\") {\n"
                + "        exclude(group = \"commons-logging\", module = \"commons-logging\")\n    }"), build);
        assertTrue(build.contains("implementation(files(\"libs/vendor.jar\"))"), build);
        assertTrue(new File(out, "libs/vendor.jar").isFile(), "the system jar is carried over");
        assertTrue(build.contains("kotlin-reflect:2.0"), "kotlin-reflect is not the plugin's to supply: " + build);
        assertFalse(build.contains("kotlin-stdlib"), build);
        assertTrue(build.contains("maven(url = uri(\"https://maven.acme.example/releases\"))"), build);
        assertFalse(build.contains("repo.maven.apache.org"), "Central needs no declaration: " + build);
        assertTrue(build.indexOf("repositories {") < build.indexOf("dependencies {"), build);
    }

    /// Keeping the two generated routes is not the same as being untouched.
    @Test
    void aBackendWithCustomizedGeneratedRoutesIsKept() throws Exception {
        File out = new File(tmp.toFile(), "out");
        converter().convert(mavenApp(UNTOUCHED_API.replace("return \"ok\";", "return audit(\"ok\");")
                .replace("public class Api {", "public class Api {\n  String audit(String s) { return s; }")), out,
                "1.0");
        assertTrue(new File(out, "backend/src/main/java/a/backend/Api.java").isFile(),
                "a changed route body is someone's work, and is converted");
    }
}

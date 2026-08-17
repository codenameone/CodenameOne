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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Coverage for the @AppIntent / @IntentEntity processor: validation messages,
 * the generated registry, and the intents.json manifest the native builders
 * read.
 */
public class AppIntentAnnotationProcessorTest {

    private static final String REGISTRY_PATH =
            "com/codename1/intents/generated/IntentRegistry.class";
    private static final String BOOTSTRAP_PATH = "cn1app/IntentBootstrap.class";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // ------------------------------------------------------------------
    // The happy path
    // ------------------------------------------------------------------

    @Test
    public void generatesRegistryBootstrapAndManifest() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log a workout\",\n"
                        + "        phrases = {\"Log a workout in ${applicationName}\"},\n"
                        + "        headless = true, timeoutSeconds = 5)\n"
                        + "public static IntentResult logWorkout(\n"
                        + "        @IntentParam(value = \"kind\", title = \"What kind?\",\n"
                        + "                     options = {\"run\", \"ride\"}) String kind,\n"
                        + "        @IntentParam(\"minutes\") int minutes) {\n"
                        + "    return IntentResult.spoken(kind + minutes);\n"
                        + "}\n"));

        ProcessorContext ctx = run(classes, true);

        assertTrue("registry must be generated",
                new File(classes, REGISTRY_PATH).exists());
        assertTrue("bootstrap must be generated so the stub can install the registry",
                new File(classes, BOOTSTRAP_PATH).exists());

        String manifest = manifest(ctx);
        assertTrue(manifest.contains("\"log_workout\""));
        assertTrue(manifest.contains("\"headless\": true"));
        assertTrue(manifest.contains("\"options\": [\"run\", \"ride\"]"));
        assertTrue("an int parameter is an integer on the wire",
                manifest.contains("\"type\": \"int\""));
    }

    @Test
    public void aProjectWithNoIntentsGeneratesNothing() throws Exception {
        File classes = compile("package com.example;\n"
                + "public class Plain { public void nothing() {} }\n");

        ProcessorContext ctx = run(classes, true);

        assertFalse(new File(classes, REGISTRY_PATH).exists());
        assertFalse(new File(classes, BOOTSTRAP_PATH).exists());
        assertTrue("nothing to declare means no manifest to write",
                ctx.getEmittedResources().isEmpty());
    }

    @Test
    public void anIntentMayTakeAContextAsItsFirstParameter() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"sync_now\", title = \"Sync\", headless = true)\n"
                        + "public static IntentResult sync(IntentContext ctx,\n"
                        + "        @IntentParam(\"full\") boolean full) {\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        assertTrue(new File(classes, REGISTRY_PATH).exists());
    }

    @Test
    public void aVoidHandlerIsAllowed() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"ping_now\", title = \"Ping\")\n"
                        + "public static void ping() { }\n"));

        run(classes, true);

        assertTrue(new File(classes, REGISTRY_PATH).exists());
    }

    /// Generating code that compiles is not the same as generating code that
    /// works. This loads the emitted registry and drives it, so a wrong argument
    /// order or a broken coercion is caught here rather than on a device.
    @Test
    public void theGeneratedRegistryActuallyDispatches() throws Exception {
        File classes = compile(source(
                "public static String seen;\n"
                        + "@AppIntent(value = \"log_workout\", title = \"Log a workout\",\n"
                        + "        headless = true)\n"
                        + "public static IntentResult logWorkout(\n"
                        + "        @IntentParam(\"kind\") String kind,\n"
                        + "        @IntentParam(\"minutes\") int minutes,\n"
                        + "        @IntentParam(\"hard\") boolean hard) {\n"
                        + "    seen = kind + \"/\" + minutes + \"/\" + hard;\n"
                        + "    return IntentResult.spoken(\"logged\");\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();

            Object declarations = registry.getClass().getMethod("describe").invoke(registry);
            assertEquals(1, ((java.util.List<?>) declarations).size());

            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put("kind", "run");
            // Deliberately the wrong Java type for an int parameter: the wire
            // format carries whatever the platform sent, and the generated
            // coercion has to cope without casting.
            params.put("minutes", "20");
            params.put("hard", Boolean.TRUE);

            Object result = registry.getClass()
                    .getMethod("invoke", String.class, Map.class,
                            loader.loadClass("com.codename1.intents.IntentContext"))
                    .invoke(registry, "log_workout", params, null);

            assertTrue("the handler must have run", result != null);
            Object seen = loader.loadClass("com.example.Handlers")
                    .getField("seen").get(null);
            assertEquals("run/20/true", seen);

            Object unknown = registry.getClass()
                    .getMethod("invoke", String.class, Map.class,
                            loader.loadClass("com.codename1.intents.IntentContext"))
                    .invoke(registry, "nope", params, null);
            assertTrue("an unknown id returns null so the facade can report it",
                    unknown == null);
        } finally {
            loader.close();
        }
    }

    /// A nested handler and a nested entity are the ordinary case, not an edge one -- an
    /// application naturally declares its entity as a static nested class beside the code that
    /// uses it. The class index reports binary names (`Outer$Inner`), which are not legal in
    /// generated source, so this fails to compile if the emitter forgets to convert them.
    @Test
    public void nestedHandlersAndEntitiesGenerateCompilableSource() throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.App",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "import com.codename1.intents.IntentResult;\n"
                                + "import java.util.List;\n"
                                + "import java.util.ArrayList;\n"
                                + "public class App {\n"
                                + "  @IntentEntity(value = \"workout\", title = \"Workout\")\n"
                                + "  public static class Workout {\n"
                                + "    @EntityId public String getId() { return \"1\"; }\n"
                                + "    @EntityTitle public String getName() { return \"Run\"; }\n"
                                + "    @EntityQuery(EntityQuery.Kind.BY_ID)\n"
                                + "    public static Workout byId(String id) { return new Workout(); }\n"
                                + "    @EntityQuery(EntityQuery.Kind.SUGGESTED)\n"
                                + "    public static List<Workout> recent() { return new ArrayList<Workout>(); }\n"
                                + "  }\n"
                                + "  @AppIntent(value = \"show_workout\", title = \"Show\")\n"
                                + "  public static IntentResult show(\n"
                                + "      @IntentParam(\"workout\") Workout w) { return IntentResult.ok(); }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));

        // A ProcessingException here means the generated source did not compile.
        run(classes, true);

        assertTrue(new File(classes, REGISTRY_PATH).exists());
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    @Test
    public void aPhraseWithoutTheApplicationNameIsRejected() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\",\n"
                        + "        phrases = {\"Log a workout\"})\n"
                        + "public static void log() { }\n"));

        assertError(classes, "must contain ${applicationName}");
    }

    @Test
    public void aNonStaticHandlerIsRejected() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public IntentResult log() { return IntentResult.ok(); }\n"));

        assertError(classes, "must be public static");
    }

    @Test
    public void anUnannotatedParameterIsRejected() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(String kind) { }\n"));

        assertError(classes, "needs @IntentParam");
    }

    @Test
    public void aMalformedIdIsRejected() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"LogWorkout\", title = \"Log\")\n"
                        + "public static void log() { }\n"));

        assertError(classes, "must match");
    }

    @Test
    public void aPhrasePlaceholderMustNameARealParameter() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\",\n"
                        + "        phrases = {\"Log ${minutes} in ${applicationName}\"})\n"
                        + "public static void log() { }\n"));

        assertError(classes, "declares no parameter with that name");
    }

    @Test
    public void anUnknownReturnTypeIsRejected() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static String log() { return null; }\n"));

        assertError(classes, "must return IntentResult or void");
    }

    @Test
    public void aParameterOfAnUndeclaredTypeIsRejectedWithGuidance() throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Thing",
                        "package com.example;\npublic class Thing {}\n"),
                classes, Arrays.asList(testClassesDir()));
        // Second class in the same output dir referencing the first.
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Handlers",
                        "package com.example;\n"
                                + "import com.codename1.annotations.AppIntent;\n"
                                + "import com.codename1.annotations.IntentParam;\n"
                                + "public class Handlers {\n"
                                + "  @AppIntent(value = \"do_thing\", title = \"Do\")\n"
                                + "  public static void go(@IntentParam(\"t\") Thing t) { }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir(), classes));

        assertError(classes, "not annotated @IntentEntity");
    }

    // ------------------------------------------------------------------
    // Entities
    // ------------------------------------------------------------------

    @Test
    public void anEntityWithoutAByIdQueryIsRejected() throws Exception {
        File classes = compile("package com.example;\n"
                + "import com.codename1.annotations.IntentEntity;\n"
                + "import com.codename1.annotations.EntityId;\n"
                + "@IntentEntity(\"playlist\")\n"
                + "public class Playlist {\n"
                + "  @EntityId public String getId() { return \"1\"; }\n"
                + "}\n");

        assertError(classes, "@EntityQuery(BY_ID)");
    }

    @Test
    public void anIndexedEntityWithoutATitleIsRejected() throws Exception {
        File classes = compile("package com.example;\n"
                + "import com.codename1.annotations.*;\n"
                + "import java.util.List;\n"
                + "@IntentEntity(value = \"playlist\", indexed = true)\n"
                + "public class Playlist {\n"
                + "  @EntityId public String getId() { return \"1\"; }\n"
                + "  @EntityQuery(EntityQuery.Kind.BY_ID)\n"
                + "  public static Playlist byId(String id) { return null; }\n"
                + "}\n");

        assertError(classes, "must declare an @EntityTitle");
    }

    @Test
    public void anEntityParameterGeneratesResolutionThroughItsByIdQuery() throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Playlist",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "import java.util.List;\n"
                                + "@IntentEntity(value = \"playlist\", title = \"Playlist\")\n"
                                + "public class Playlist {\n"
                                + "  @EntityId public String getId() { return \"1\"; }\n"
                                + "  @EntityTitle public String getName() { return \"Focus\"; }\n"
                                + "  @EntityQuery(EntityQuery.Kind.BY_ID)\n"
                                + "  public static Playlist byId(String id) { return new Playlist(); }\n"
                                + "  @EntityQuery(EntityQuery.Kind.SUGGESTED)\n"
                                + "  public static List<Playlist> recent() { return null; }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Handlers",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "import com.codename1.intents.IntentResult;\n"
                                + "public class Handlers {\n"
                                + "  @AppIntent(value = \"play_list\", title = \"Play\")\n"
                                + "  public static IntentResult play(\n"
                                + "      @IntentParam(value = \"playlist\", title = \"Which playlist?\")"
                                + " Playlist p) { return IntentResult.ok(); }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir(), classes));

        ProcessorContext ctx = run(classes, true);

        assertTrue(new File(classes, REGISTRY_PATH).exists());
        String manifest = manifest(ctx);
        assertTrue(manifest.contains("\"entityType\": \"playlist\""));
        assertTrue(manifest.contains("\"type\": \"entity\""));
        assertTrue("the declared queries travel to the native side",
                manifest.contains("\"BY_ID\""));
        assertTrue(manifest.contains("\"SUGGESTED\""));
    }

    @Test
    public void aDuplicateIntentIdIsRejected() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"A\")\n"
                        + "public static void a() { }\n"
                        + "@AppIntent(value = \"log_workout\", title = \"B\")\n"
                        + "public static void b() { }\n"));

        assertError(classes, "already declared by");
    }

    // ------------------------------------------------------------------
    // Harness
    // ------------------------------------------------------------------

    private static String source(String body) {
        return "package com.example;\n"
                + "import com.codename1.annotations.AppIntent;\n"
                + "import com.codename1.annotations.IntentParam;\n"
                + "import com.codename1.intents.IntentContext;\n"
                + "import com.codename1.intents.IntentResult;\n"
                + "public class Handlers {\n" + body + "}\n";
    }

    private File compile(String src) throws Exception {
        File classes = tmp.newFolder();
        String fqn = src.contains("class Handlers") ? "com.example.Handlers"
                : src.contains("class Playlist") ? "com.example.Playlist"
                : "com.example.Plain";
        JavaSourceCompiler.compile(JavaSourceCompiler.singleSource(fqn, src), classes,
                Arrays.asList(testClassesDir()));
        return classes;
    }

    private ProcessorContext run(File classes, boolean expectClean) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        AppIntentAnnotationProcessor proc = new AppIntentAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        if (expectClean && ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("unexpected errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
                sb.append("  ").append(e).append('\n');
            }
            fail(sb.toString());
        }
        return ctx;
    }

    private void assertError(File classes, String fragment) throws Exception {
        ProcessorContext ctx = run(classes, false);
        assertTrue("expected a validation error", ctx.hasErrors());
        StringBuilder all = new StringBuilder();
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
            all.append(e).append('\n');
        }
        assertTrue("expected an error containing \"" + fragment + "\" but got:\n" + all,
                all.toString().contains(fragment));
        assertFalse("nothing may be generated while an error is pending",
                new File(classes, REGISTRY_PATH).exists());
    }

    private static String manifest(ProcessorContext ctx) {
        Map<String, byte[]> res = new LinkedHashMap<String, byte[]>(ctx.getEmittedResources());
        byte[] bytes = res.get("intents.json");
        assertTrue("intents.json must be emitted", bytes != null);
        return new String(bytes, Charset.forName("UTF-8"));
    }

    private static File testClassesDir() throws Exception {
        URL url = AppIntentAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}

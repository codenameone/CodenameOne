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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    /// A date arrives as epoch millis from the platforms and as text from a language model
    /// invoking the intent through Intents.asTools(). Both are advertised in the tool schema,
    /// so both have to resolve; anything else silently becomes a null argument.
    @Test
    public void aDateParameterAcceptsBothEpochMillisAndIso8601() throws Exception {
        File classes = compile(source(
                "public static java.util.Date seen;\n"
                        + "@AppIntent(value = \"log_workout\", title = \"Log a workout\")\n"
                        + "public static IntentResult logWorkout(\n"
                        + "        @IntentParam(value = \"when\", required = false)\n"
                        + "        java.util.Date when) {\n"
                        + "    seen = when;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));
            java.lang.reflect.Field seen = loader.loadClass("com.example.Handlers")
                    .getField("seen");

            // 2026-03-14T00:00:00Z
            long midnightUtc = 1773446400000L;

            assertEquals(new java.util.Date(midnightUtc),
                    read(invoke, registry, seen, Long.valueOf(midnightUtc)));
            assertEquals(new java.util.Date(midnightUtc),
                    read(invoke, registry, seen, String.valueOf(midnightUtc)));
            assertEquals("a bare date is midnight UTC", new java.util.Date(midnightUtc),
                    read(invoke, registry, seen, "2026-03-14"));
            assertEquals(new java.util.Date(midnightUtc),
                    read(invoke, registry, seen, "2026-03-14T00:00:00Z"));
            assertEquals("fractional seconds are accepted and kept",
                    new java.util.Date(midnightUtc + 250),
                    read(invoke, registry, seen, "2026-03-14T00:00:00.250Z"));
            assertEquals("an offset is applied rather than ignored",
                    new java.util.Date(midnightUtc + 3600000L),
                    read(invoke, registry, seen, "2026-03-14T00:00:00-01:00"));
            // Optional on purpose: an unparseable value has to become "absent" rather than
            // "some date", and only an optional parameter can show that. A required one fails
            // the invocation instead, which is the other half of the same rule.
            // Supplied and unreadable is rejected rather than silently becoming absent: the
            // caller sent something, and optionality is about presence, not validity.
            assertRejected(invoke, registry, "log_workout", "when", "next tuesday", "not a date");
        } finally {
            loader.close();
        }
    }

    /// Out-of-range fields are the reason the parser sets a strict Calendar: a lenient one
    /// rolls 2026-13-40 forward into a real date in 2027 and the handler acts on a day nobody
    /// named.
    @Test
    public void anImpossibleDateIsRejectedRatherThanNormalized() throws Exception {
        File classes = compile(source(
                "public static java.util.Date seen;\n"
                        + "@AppIntent(value = \"log_workout\", title = \"Log a workout\")\n"
                        + "public static IntentResult logWorkout(\n"
                        + "        @IntentParam(value = \"when\", required = false)\n"
                        + "        java.util.Date when) {\n"
                        + "    seen = when;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));
            java.lang.reflect.Field seen = loader.loadClass("com.example.Handlers")
                    .getField("seen");

            assertRejected(invoke, registry, "log_workout", "when", "2026-13-01", "not a date");
            assertRejected(invoke, registry, "log_workout", "when", "2026-03-40", "not a date");
            assertRejected(invoke, registry, "log_workout", "when", "2026-03-14T25:00:00Z",
                    "not a date");
            assertRejected(invoke, registry, "log_workout", "when", "2026-02-29", "not a date");
        } finally {
            loader.close();
        }
    }

    /// A required parameter has no default to fall back to, so a present-but-unconvertible
    /// value must stop the invocation rather than reach the handler as 0. Committing a side
    /// effect on a number nobody supplied is the failure worth preventing here.
    @Test
    public void aRequiredNumberRejectsAValueThatIsNotOne() throws Exception {
        File classes = compile(source(
                "public static int seen = -1;\n"
                        + "@AppIntent(value = \"log_workout\", title = \"Log a workout\")\n"
                        + "public static IntentResult logWorkout(\n"
                        + "        @IntentParam(\"minutes\") int minutes) {\n"
                        + "    seen = minutes;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));
            java.lang.reflect.Field seen = loader.loadClass("com.example.Handlers")
                    .getField("seen");

            Map<String, Object> bad = new LinkedHashMap<String, Object>();
            bad.put("minutes", "abc");
            try {
                invoke.invoke(registry, "log_workout", bad, null);
                fail("a required int given \"abc\" must not run the handler");
            } catch (java.lang.reflect.InvocationTargetException e) {
                assertTrue(e.getCause() instanceof IllegalArgumentException);
                assertTrue(e.getCause().getMessage(),
                        e.getCause().getMessage().contains("minutes"));
            }
            assertEquals("the handler must not have run", -1, seen.getInt(null));

            // The convertible forms still work, so this is strictness rather than a new rule
            // about what a number may look like on the wire.
            Map<String, Object> good = new LinkedHashMap<String, Object>();
            good.put("minutes", "20");
            invoke.invoke(registry, "log_workout", good, null);
            assertEquals(20, seen.getInt(null));
        } finally {
            loader.close();
        }
    }

    /// The conversions that silently succeed with a wrong answer, which is worse than failing:
    /// longValue() saturates, a (float) cast overflows to infinity, and an out-of-range ISO
    /// offset shifts the instant. Each would hand a side-effecting handler a number or a moment
    /// the caller never sent.
    @Test
    public void aRequiredNumberRejectsValuesItCannotRepresent() throws Exception {
        File classes = compile(source(
                "public static double seen = -1;\n"
                        + "@AppIntent(value = \"set_size\", title = \"Set\")\n"
                        + "public static IntentResult set(@IntentParam(\"big\") long big) {\n"
                        + "    seen = big;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));

            // 1e20 is integral and finite, and longValue() would saturate it to Long.MAX_VALUE.
            assertRejected(invoke, registry, "set_size", "big", Double.valueOf(1e20), "range");
            // Still accepts what it can represent, so this is a bound rather than a new rule.
            Map<String, Object> ok = new LinkedHashMap<String, Object>();
            ok.put("big", Long.valueOf(1234567890123L));
            invoke.invoke(registry, "set_size", ok, null);
            assertEquals(1234567890123d,
                    loader.loadClass("com.example.Handlers").getField("seen").getDouble(null), 0d);
        } finally {
            loader.close();
        }
    }

    @Test
    public void aRequiredFloatRejectsAValueThatWouldBecomeInfinity() throws Exception {
        File classes = compile(source(
                "public static float seen = -1;\n"
                        + "@AppIntent(value = \"set_ratio\", title = \"Set\")\n"
                        + "public static IntentResult set(@IntentParam(\"r\") float r) {\n"
                        + "    seen = r;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));

            assertRejected(invoke, registry, "set_ratio", "r", Double.valueOf(1e100), "range");

            Map<String, Object> ok = new LinkedHashMap<String, Object>();
            ok.put("r", Double.valueOf(1.5));
            invoke.invoke(registry, "set_ratio", ok, null);
            assertEquals(1.5f,
                    loader.loadClass("com.example.Handlers").getField("seen").getFloat(null), 0f);
        } finally {
            loader.close();
        }
    }

    @Test
    public void anOutOfRangeIsoOffsetIsRejected() throws Exception {
        File classes = compile(source(
                "public static java.util.Date seen;\n"
                        + "@AppIntent(value = \"log_workout\", title = \"Log a workout\")\n"
                        + "public static IntentResult logWorkout(\n"
                        + "        @IntentParam(value = \"when\", required = false)\n"
                        + "        java.util.Date when) {\n"
                        + "    seen = when;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));
            java.lang.reflect.Field seen = loader.loadClass("com.example.Handlers")
                    .getField("seen");

            assertRejected(invoke, registry, "log_workout", "when",
                    "2026-03-14T12:00:00+01:99", "not a date");
            assertRejected(invoke, registry, "log_workout", "when",
                    "2026-03-14T12:00:00+25:00", "not a date");
            assertRejected(invoke, registry, "log_workout", "when",
                    "2026-03-14T12:00:00+19:00", "not a date");
            assertEquals("a legitimate offset still applies",
                    new java.util.Date(1773489600000L - 3600000L),
                    read(invoke, registry, seen, "2026-03-14T12:00:00+01:00"));
        } finally {
            loader.close();
        }
    }

    /// Double.parseDouble accepts "NaN" and "Infinity", and the string form is the one a
    /// language model actually writes. A handler that receives NaN cannot recover from it by
    /// any arithmetic it performs afterwards.
    @Test
    public void aRequiredNumberRejectsNonFiniteText() throws Exception {
        File classes = compile(source(
                "public static double seen = -1;\n"
                        + "@AppIntent(value = \"set_ratio\", title = \"Set\")\n"
                        + "public static IntentResult set(@IntentParam(\"r\") double r) {\n"
                        + "    seen = r;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));

            assertRejected(invoke, registry, "set_ratio", "r", "NaN", "finite");
            assertRejected(invoke, registry, "set_ratio", "r", "Infinity", "finite");
            assertRejected(invoke, registry, "set_ratio", "r", "-Infinity", "finite");
            assertRejected(invoke, registry, "set_ratio", "r", Double.valueOf(Double.NaN),
                    "finite");

            Map<String, Object> ok = new LinkedHashMap<String, Object>();
            ok.put("r", "2.5");
            invoke.invoke(registry, "set_ratio", ok, null);
            assertEquals(2.5d,
                    loader.loadClass("com.example.Handlers").getField("seen").getDouble(null), 0d);
        } finally {
            loader.close();
        }
    }

    /// Reading HH:mm and ignoring whatever follows accepted "12:34junk" as a moment in time.
    @Test
    public void anIsoTimeMustBeFullyConsumed() throws Exception {
        File classes = compile(source(
                "public static java.util.Date seen;\n"
                        + "@AppIntent(value = \"log_workout\", title = \"Log a workout\")\n"
                        + "public static IntentResult logWorkout(\n"
                        + "        @IntentParam(value = \"when\", required = false)\n"
                        + "        java.util.Date when) {\n"
                        + "    seen = when;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));
            java.lang.reflect.Field seen = loader.loadClass("com.example.Handlers")
                    .getField("seen");

            assertRejected(invoke, registry, "log_workout", "when", "2026-03-14T12:34junk",
                    "not a date");
            assertRejected(invoke, registry, "log_workout", "when", "2026-03-14T12:34:56junk",
                    "not a date");
            assertRejected(invoke, registry, "log_workout", "when", "2026-03-14T12:34:56.",
                    "not a date");
            assertRejected(invoke, registry, "log_workout", "when", "2026-03-14T12:34:56.1x2",
                    "not a date");

            // The forms that are real still parse, so this bounds the grammar rather than
            // shrinking it.
            assertNotNull(read(invoke, registry, seen, "2026-03-14T12:34"));
            assertNotNull(read(invoke, registry, seen, "2026-03-14T12:34:56"));
            assertNotNull(read(invoke, registry, seen, "2026-03-14T12:34:56.789"));
            assertNotNull(read(invoke, registry, seen, "2026-03-14T12:34:56.789Z"));
        } finally {
            loader.close();
        }
    }

    /// Invokes an intent with one bad value and asserts it failed loudly rather than running.
    private static void assertRejected(java.lang.reflect.Method invoke, Object registry,
                                       String intentId, String param, Object value,
                                       String expectedInMessage) throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(param, value);
        try {
            invoke.invoke(registry, intentId, params, null);
            fail(value + " must not reach the handler");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(String.valueOf(e.getCause()),
                    e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage(),
                    e.getCause().getMessage().contains(expectedInMessage));
        }
    }

    /// Invokes the fixture with one parameter value and reports what the handler received.

    private static Object read(java.lang.reflect.Method invoke, Object registry,
                               java.lang.reflect.Field seen, Object value) throws Exception {
        seen.set(null, null);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("when", value);
        invoke.invoke(registry, "log_workout", params, null);
        return seen.get(null);
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

    /// Apple's metadata processor stops on "Multiple parameters detected in phrase" and emits no
    /// App Intents metadata at all -- the app builds, ships, and simply has no intents. Catching
    /// it here is the difference between a named declaration and an opaque toolchain failure.
    @Test
    public void aPhraseMayReferenceAtMostOneParameter() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\",\n"
                        + "        phrases = {\"Log a ${minutes} minute ${kind} in ${applicationName}\"})\n"
                        + "public static void log(@IntentParam(\"kind\") String kind,\n"
                        + "        @IntentParam(\"minutes\") int minutes) { }\n"));

        assertError(classes, "at most one parameter per phrase");
    }

    /// Same shape of failure, different Apple message: "AppEntity and AppEnum are the only
    /// allowed types for App Shortcut parameters".
    @Test
    public void aPhraseParameterMustBeAnEntity() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\",\n"
                        + "        phrases = {\"Log a ${kind} in ${applicationName}\"})\n"
                        + "public static void log(@IntentParam(\"kind\") String kind) { }\n"));

        assertError(classes, "Only an entity can be a spoken phrase parameter");
    }

    /// "App Intent 'X' must be visible for App Shortcuts use."
    @Test
    public void aPhraseOnANonDiscoverableIntentIsRejected() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\", discoverable = false,\n"
                        + "        phrases = {\"Log a workout in ${applicationName}\"})\n"
                        + "public static void log() { }\n"));

        assertError(classes, "declares phrases but discoverable=false");
    }

    /// The counterpart to the two rejections above: an entity in a phrase, on its own, is the
    /// form Apple actually accepts, so it has to keep compiling.
    @Test
    public void oneEntityInAPhraseIsAccepted() throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.App",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "import com.codename1.intents.IntentResult;\n"
                                + "public class App {\n"
                                + "  @IntentEntity(value = \"playlist\", title = \"Playlist\")\n"
                                + "  public static class Playlist {\n"
                                + "    @EntityId public String getId() { return \"1\"; }\n"
                                + "    @EntityTitle public String getName() { return \"Focus\"; }\n"
                                + "    @EntityQuery(EntityQuery.Kind.BY_ID)\n"
                                + "    public static Playlist byId(String id) { return null; }\n"
                                + "  }\n"
                                + "  @AppIntent(value = \"play_list\", title = \"Play\",\n"
                                + "          phrases = {\"Play ${playlist} in ${applicationName}\"})\n"
                                + "  public static IntentResult play(\n"
                                + "      @IntentParam(\"playlist\") Playlist p) { return IntentResult.ok(); }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));

        run(classes, true);

        assertTrue(new File(classes, REGISTRY_PATH).exists());
    }

    /// A default outside its own vocabulary compiles and then throws on every invocation that
    /// omits the value, because the generated oneOf() enforces the vocabulary against defaults
    /// too. The handler never runs, for a declaration that reads as perfectly reasonable.
    @Test
    public void aDefaultOutsideItsOptionsIsRejected() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(@IntentParam(value = \"kind\",\n"
                        + "        required = false, defaultValue = \"walk\",\n"
                        + "        options = {\"run\", \"ride\"}) String kind) { }\n"));

        assertError(classes, "not one of its options");
    }

    @Test
    public void aDefaultInsideItsOptionsIsAccepted() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(@IntentParam(value = \"kind\",\n"
                        + "        required = false, defaultValue = \"run\",\n"
                        + "        options = {\"run\", \"ride\"}) String kind) { }\n"));

        run(classes, true);

        assertTrue(new File(classes, REGISTRY_PATH).exists());
    }

    /// A malformed default is a compile-time constant that silently became something else:
    /// numeric() emits 0 for an int it cannot parse, an unparseable boolean becomes false, and
    /// an unparseable date becomes null. An omitted value then reached the handler as a value
    /// the declaration never named.
    /// The two halves contradict each other, and the platforms resolved the contradiction
    /// differently: Android published a parameterless static shortcut and ran it with the
    /// default, while the generated Swift kept the parameter non-optional and prompted.
    @Test
    public void aRequiredParameterWithADefaultIsRejected() throws Exception {
        assertError(compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(@IntentParam(value = \"minutes\",\n"
                        + "        defaultValue = \"20\") int m) { }\n")),
                "is required and also declares defaultValue");
    }

    /// A closed vocabulary is only enforced for strings -- oneOf() on the Java side, an AppEnum
    /// on the iOS side -- so publishing one on any other type advertises a restriction nothing
    /// applies, and a caller passing 999 to options={"1","2"} runs the handler.
    @Test
    public void optionsOnATypeThatCannotEnforceThemIsRejected() throws Exception {
        assertError(compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(@IntentParam(value = \"level\",\n"
                        + "        options = {\"1\", \"2\"}) int level) { }\n")),
                "declares options on a int");
    }

    /// Callers disagreed about what a non-positive budget meant -- platform dispatch
    /// substituted the default, Intents.invoke built an already-expired context, and a model
    /// waited on the raw number -- so the same handler got contradictory deadlines depending on
    /// who called it.
    @Test
    public void aNonPositiveTimeoutIsRejected() throws Exception {
        assertError(compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\", timeoutSeconds = 0)\n"
                        + "public static void log() { }\n")),
                "at least 1 second");
        assertError(compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\", timeoutSeconds = -5)\n"
                        + "public static void log() { }\n")),
                "at least 1 second");
    }

    /// Epoch millis are a number like any other, and longValue() truncates and saturates
    /// silently -- so a date arrived as a moment the caller never named.
    @Test
    public void aNumericDateRejectsValuesItCannotRepresent() throws Exception {
        File classes = compile(source(
                "public static java.util.Date seen;\n"
                        + "@AppIntent(value = \"log_workout\", title = \"Log a workout\")\n"
                        + "public static IntentResult logWorkout(\n"
                        + "        @IntentParam(value = \"when\", required = false)\n"
                        + "        java.util.Date when) {\n"
                        + "    seen = when;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));
            java.lang.reflect.Field seen = loader.loadClass("com.example.Handlers")
                    .getField("seen");

            assertRejected(invoke, registry, "log_workout", "when", Double.valueOf(1.5),
                    "not a moment in time");
            assertRejected(invoke, registry, "log_workout", "when", Double.valueOf(1e20),
                    "not a moment in time");
            assertEquals("a real epoch value still works",
                    new java.util.Date(1773446400000L),
                    read(invoke, registry, seen, Long.valueOf(1773446400000L)));
        } finally {
            loader.close();
        }
    }

    @Test
    public void aMalformedDefaultIsRejectedForItsType() throws Exception {
        assertError(compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(@IntentParam(value = \"minutes\",\n"
                        + "        required = false, defaultValue = \"abc\") int m) { }\n")),
                "not a valid int");
        assertError(compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(@IntentParam(value = \"hard\",\n"
                        + "        required = false, defaultValue = \"maybe\") boolean b) { }\n")),
                "not a valid boolean");
        assertError(compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(@IntentParam(value = \"when\",\n"
                        + "        required = false, defaultValue = \"soon\")\n"
                        + "        java.util.Date d) { }\n")),
                "not a valid date");
        // Out of range for the declared type, not merely unparseable.
        assertError(compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(@IntentParam(value = \"minutes\",\n"
                        + "        required = false, defaultValue = \"4294967296\") int m) { }\n")),
                "not a valid int");
    }

    /// Shape is not enough: these all match the pattern and are all rejected by the generated
    /// parser, so a default that looked valid became null and the handler saw no value at all.
    @Test
    public void aDateDefaultIsValidatedSemanticallyNotJustByShape() throws Exception {
        String[] impossible = {"2026-13-01", "2026-02-30", "2026-01-01T25:00",
            "2026-01-01T12:61", "2026-01-01T12:00:61", "2026-01-01T12:00+19:00",
            "2026-01-01T12:00+01:99"};
        for (String bad : impossible) {
            assertError(compile(source(
                    "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                            + "public static void log(@IntentParam(value = \"when\",\n"
                            + "        required = false, defaultValue = \"" + bad + "\")\n"
                            + "        java.util.Date d) { }\n")),
                    "not a valid date");
        }
    }

    /// The counterpart: every form the generated parser accepts has to survive validation, or
    /// the build would reject defaults that work perfectly at runtime.
    @Test
    public void everyAcceptedDateFormPassesValidation() throws Exception {
        String[] good = {"2026-03-14", "2026-03-14T12:34", "2026-03-14T12:34:56",
            "2026-03-14T12:34:56.789", "2026-03-14T12:34:56Z", "2026-03-14T12:34:56+01:00",
            "2028-02-29T00:00", "2026-12-31T23:59:59.999-05:00"};
        for (String ok : good) {
            File classes = compile(source(
                    "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                            + "public static void log(@IntentParam(value = \"when\",\n"
                            + "        required = false, defaultValue = \"" + ok + "\")\n"
                            + "        java.util.Date d) { }\n"));
            run(classes, true);
            assertTrue(ok + " must be accepted",
                    new File(classes, REGISTRY_PATH).exists());
        }
    }

    @Test
    public void wellFormedDefaultsAreAccepted() throws Exception {
        File classes = compile(source(
                "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static void log(\n"
                        + "        @IntentParam(value = \"minutes\", required = false,\n"
                        + "                     defaultValue = \"20\") int m,\n"
                        + "        @IntentParam(value = \"hard\", required = false,\n"
                        + "                     defaultValue = \"true\") boolean b,\n"
                        + "        @IntentParam(value = \"ratio\", required = false,\n"
                        + "                     defaultValue = \"1.5\") double r,\n"
                        + "        @IntentParam(value = \"epoch\", required = false,\n"
                        + "                     defaultValue = \"1773446400000\") java.util.Date e,\n"
                        + "        @IntentParam(value = \"iso\", required = false,\n"
                        + "                     defaultValue = \"2026-03-14\") java.util.Date i) { }\n"));

        run(classes, true);

        assertTrue(new File(classes, REGISTRY_PATH).exists());
    }

    /// Making a parameter optional must not change whether a supplied value is checked. It
    /// changes what happens when the value is absent, and nothing else.
    @Test
    public void anOptionalNumberRejectsASuppliedValueItCannotRepresent() throws Exception {
        File classes = compile(source(
                "public static int seen = -1;\n"
                        + "@AppIntent(value = \"log_workout\", title = \"Log\")\n"
                        + "public static IntentResult log(@IntentParam(value = \"minutes\",\n"
                        + "        required = false, defaultValue = \"20\") int m) {\n"
                        + "    seen = m;\n"
                        + "    return IntentResult.ok();\n"
                        + "}\n"));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            java.lang.reflect.Method invoke = registry.getClass().getMethod("invoke",
                    String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"));
            java.lang.reflect.Field seen = loader.loadClass("com.example.Handlers")
                    .getField("seen");

            // Absent: the declared default, which is what optional means.
            invoke.invoke(registry, "log_workout", new LinkedHashMap<String, Object>(), null);
            assertEquals(20, seen.getInt(null));

            // Supplied but not representable: rejected, exactly as when it is required.
            assertRejected(invoke, registry, "log_workout", "minutes", Double.valueOf(1.5),
                    "whole number");
            assertRejected(invoke, registry, "log_workout", "minutes",
                    Long.valueOf(4294967296L), "range");
            assertEquals("the handler must not have run again", 20, seen.getInt(null));
        } finally {
            loader.close();
        }
    }

    /// A variable segment can only be satisfied by a route segment that accepts any value.
    /// Matching it against a literal passed the build and then produced a URL the declared
    /// route could never match, so navigation silently did nothing on a device.
    @Test
    public void aRouteVariableMustMapToAWildcardSegment() throws Exception {
        File classes = routeFixture("/orders/recent");

        assertError(classes, "does not match any @Route");
    }

    @Test
    public void aRouteVariableMatchesADeclaredWildcard() throws Exception {
        File classes = routeFixture("/orders/:id");

        run(classes, true);

        assertTrue(new File(classes, REGISTRY_PATH).exists());
    }

    /// A @Route with the given pattern plus an intent whose opensRoute is "/orders/{id}".
    private File routeFixture(String routePattern) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Screens",
                        "package com.example;\n"
                                + "import com.codename1.annotations.Route;\n"
                                + "import com.codename1.ui.Form;\n"
                                + "public class Screens {\n"
                                + "  @Route(\"" + routePattern + "\")\n"
                                + "  public static Form screen() { return null; }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Handlers",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "import com.codename1.intents.IntentResult;\n"
                                + "public class Handlers {\n"
                                + "  @AppIntent(value = \"show_order\", title = \"Show\",\n"
                                + "          opensRoute = \"/orders/{id}\")\n"
                                + "  public static IntentResult show(\n"
                                + "      @IntentParam(\"id\") String id) { return IntentResult.ok(); }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir(), classes));
        return classes;
    }

    /// An optional entity parameter's default is an id like any other and has to resolve
    /// through the same BY_ID query; ignoring it handed the handler null for exactly the case
    /// the default exists for.
    @Test
    public void anOptionalEntityDefaultResolvesThroughItsQuery() throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.App",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "import com.codename1.intents.IntentResult;\n"
                                + "public class App {\n"
                                + "  public static String seen;\n"
                                + "  @IntentEntity(value = \"playlist\", title = \"Playlist\")\n"
                                + "  public static class Playlist {\n"
                                + "    public String pid;\n"
                                + "    @EntityId public String getId() { return pid; }\n"
                                + "    @EntityTitle public String getName() { return pid; }\n"
                                + "    @EntityQuery(EntityQuery.Kind.BY_ID)\n"
                                + "    public static Playlist byId(String id) {\n"
                                + "      Playlist p = new Playlist(); p.pid = id; return p; }\n"
                                + "  }\n"
                                + "  @AppIntent(value = \"play_list\", title = \"Play\")\n"
                                + "  public static IntentResult play(\n"
                                + "      @IntentParam(value = \"playlist\", required = false,\n"
                                + "                   defaultValue = \"favourites\") Playlist p) {\n"
                                + "    seen = p == null ? null : p.getId();\n"
                                + "    return IntentResult.ok(); }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));

        run(classes, true);

        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader());
        try {
            Object registry = loader.loadClass(
                    "com.codename1.intents.generated.IntentRegistry").newInstance();
            registry.getClass().getMethod("invoke", String.class, Map.class,
                    loader.loadClass("com.codename1.intents.IntentContext"))
                    .invoke(registry, "play_list", new LinkedHashMap<String, Object>(), null);

            assertEquals("the declared default has to resolve like any other id",
                    "favourites", loader.loadClass("com.example.App").getField("seen").get(null));
        } finally {
            loader.close();
        }
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

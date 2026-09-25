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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `@OpenTelemetry` on the app: the bootstrap it generates, compiled against the
/// real core, and every configuration mistake the build can see refused there.
public class TelemetryAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void aRelayConfigurationBecomesTheBootstrap() throws Exception {
        File classes = compile("@OpenTelemetry(relay = \"https://api.example.com\", "
                + "serviceName = \"shop-app\", relayToken = \"t0k\", sampleRatio = 0.25, "
                + "propagateTo = {\"cdn.example.com\"})");
        ProcessorContext ctx = run(classes);
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertTrue("the bootstrap was not compiled",
                new File(classes, "cn1app/TelemetryBootstrap.class").isFile());
        String source = bootstrapSource(classes);
        assertContains(source, "com.codename1.telemetry.Telemetry.install(");
        assertContains(source, ".relay(\"https://api.example.com\")");
        assertContains(source, ".serviceName(\"shop-app\")");
        assertContains(source, ".relayToken(\"t0k\")");
        assertContains(source, ".sampleRatio(0.25)");
        assertContains(source, ".propagateTo(\"cdn.example.com\")");
        assertFalse("consent is opt-in, not the default", source.contains("requireAnalyticsConsent"));
    }

    @Test
    public void theConsentFlagReachesTheBootstrap() throws Exception {
        File classes = compile("@OpenTelemetry(relay = \"https://api.example.com\", "
                + "requireAnalyticsConsent = true)");
        ProcessorContext ctx = run(classes);
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertContains(bootstrapSource(classes), ".requireAnalyticsConsent(true)");
    }

    @Test
    public void aDirectConfigurationCarriesItsHeaders() throws Exception {
        File classes = compile("@OpenTelemetry(endpoint = \"https://collector.example.com:4318\", "
                + "headers = \"Authorization: Api-Token dt0c01.x\", protobuf = false)");
        ProcessorContext ctx = run(classes);
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        String source = bootstrapSource(classes);
        assertContains(source, ".direct(\"https://collector.example.com:4318\")");
        assertContains(source, ".header(\"Authorization\", \"Api-Token dt0c01.x\")");
        assertContains(source, ".protobuf(false)");
    }

    @Test
    public void anAppWithoutTheAnnotationGetsNoBootstrap() throws Exception {
        File classes = compile("");
        ProcessorContext ctx = run(classes);
        assertFalse(ctx.hasErrors());
        assertFalse("an app that never asked carries a telemetry bootstrap",
                new File(classes, "cn1app/TelemetryBootstrap.class").exists());
    }

    @Test
    public void removingTheAnnotationRemovesTheBootstrapAnEarlierBuildLeft() throws Exception {
        // Maven keeps target/classes between builds, and the builders install
        // whatever bootstrap class is there.
        File classes = compile("");
        Map<String, String> earlier = new LinkedHashMap<String, String>();
        earlier.put("cn1app.TelemetryBootstrap",
                "package cn1app;\npublic final class TelemetryBootstrap {\n}\n");
        JavaSourceCompiler.compile(earlier, classes, Arrays.asList(testClassesDir()));
        File stale = new File(classes, "cn1app/TelemetryBootstrap.class");
        assertTrue("the fixture did not compile", stale.isFile());
        ProcessorContext ctx = run(classes);
        assertFalse(ctx.hasErrors());
        assertFalse("a bootstrap for telemetry nobody asks for any more was left to ship",
                stale.exists());
    }

    @Test
    public void nowhereToSendIsRefused() throws Exception {
        assertRefused("@OpenTelemetry(serviceName = \"x\")");
    }

    @Test
    public void bothARelayAndACollectorIsRefused() throws Exception {
        assertRefused("@OpenTelemetry(relay = \"https://a\", endpoint = \"https://b\")");
    }

    @Test
    public void aCredentialOnARelayIsRefused() throws Exception {
        // The relay exists so the credential stays on the server; a header here
        // would ship it in the app while looking as though it did not.
        assertRefused("@OpenTelemetry(relay = \"https://a\", headers = \"Authorization: x\")");
    }

    @Test
    public void aMalformedHeaderIsRefused() throws Exception {
        assertRefused("@OpenTelemetry(endpoint = \"https://a\", headers = \"Authorization\")");
    }

    @Test
    public void aHeaderThatIsNotValidHttpIsRefused() throws Exception {
        assertRefused("@OpenTelemetry(endpoint = \"https://a\", headers = \"Bad Name: v\")");
        assertRefused("@OpenTelemetry(endpoint = \"https://a\", "
                + "headers = \"X-Token: a\\r\\nInjected: b\")");
    }

    @Test
    public void aRatioOutOfRangeIsRefused() throws Exception {
        assertRefused("@OpenTelemetry(endpoint = \"https://a\", sampleRatio = 2)");
    }

    @Test
    public void aUrlThatIsNotHttpIsRefused() throws Exception {
        assertRefused("@OpenTelemetry(endpoint = \"collector:4318\")");
        assertRefused("@OpenTelemetry(endpoint = \"https://\")");
        assertRefused("@OpenTelemetry(relay = \"https:///path\")");
        assertRefused("@OpenTelemetry(endpoint = \"https://host:port/\")");
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("https://collector.example:4318/otlp"));
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("http://[::1]:4318"));
        // A port outside TCP's range is five digits too.
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://collector.example:99999"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://collector.example:0"));
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("https://collector.example:65535"));
        // The runtime's host rule, so nothing the build accepts throws at start-up.
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://collector!x.example"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://[not-ipv6]:4318"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://[1234]"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://bad value@collector.example"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://u%zz@collector.example"));
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("https://user:p%40ss@collector.example"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://collector.example/bad path"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://[:::]:4318"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://[1::2::3]:4318"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://[1:2:3:4:5:6:7:8:9]:4318"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://[::ffff:300.0.0.1]:4318"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://[1:2:3:4:5:6:7]:4318"));
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://[12345::1]:4318"));
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("https://[::1]:4318"));
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("https://[::ffff:10.0.0.7]:4318"));
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("https://[2001:db8::1]:4318"));
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("https://[1:2:3:4:5:6:7:8]:4318"));
        assertTrue(TelemetryAnnotationProcessor.isHttpUrl("https://[::]:4318"));
    }

    @Test
    public void aMalformedHeaderIsRefusedWithoutQuotingIt() throws Exception {
        File classes = compile("@OpenTelemetry(endpoint = \"https://c.example\", "
                + "headers = \"Authorization Bearer s3cret\")");
        ProcessorContext ctx = run(classes);
        assertTrue(ctx.hasErrors());
        assertFalse("the refusal quoted a credential: " + ctx.getErrors(),
                String.valueOf(ctx.getErrors()).contains("s3cret"));
    }

    @Test
    public void anEndpointWithAFragmentIsRefused() throws Exception {
        assertFalse(TelemetryAnnotationProcessor.isHttpUrl("https://c.example/v1#api-key=s3cret"));
    }

    @Test
    public void aHeaderTheExporterOwnsIsRefused() throws Exception {
        assertRefused("@OpenTelemetry(endpoint = \"https://c.example\", headers = \"Content-Type: application/json\")");
    }

    @Test
    public void aRelayTokenNoHeaderMayCarryIsRefused() throws Exception {
        // TelemetryConfig would throw on it inside the bootstrap, before
        // Display.init; the build says so instead.
        assertRefused("@OpenTelemetry(relay = \"https://api.example\", relayToken = \"bad\\nvalue\")");
        assertRefused("@OpenTelemetry(relay = \"https://api.example\", relayToken = \"trailing \")");
        assertRefused("@OpenTelemetry(relay = \"https://api.example\", relayToken = \" leading\")");
    }

    // ------------------------------------------------------------------

    private void assertRefused(String annotation) throws Exception {
        File classes = compile(annotation);
        ProcessorContext ctx = run(classes);
        assertTrue("accepted " + annotation, ctx.hasErrors());
        assertFalse(new File(classes, "cn1app/TelemetryBootstrap.class").exists());
    }

    private static void assertContains(String source, String expected) {
        assertTrue("expected " + expected + " in:\n" + source, source.contains(expected));
    }

    private File compile(String annotation) throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.ShopApp",
                "package com.example;\n"
                        + "import com.codename1.annotations.OpenTelemetry;\n"
                        + annotation + "\n"
                        + "public class ShopApp {\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        return classes;
    }

    private ProcessorContext run(File classes) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        TelemetryAnnotationProcessor proc = new TelemetryAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                proc.processClass(cls, ctx);
            }
        }
        proc.finish(ctx);
        return ctx;
    }

    private static String bootstrapSource(File classes) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        for (AnnotatedClass cls : index.values()) {
            if (cls.getClassAnnotation(TelemetryAnnotationProcessor.OPEN_TELEMETRY_DESC) != null) {
                return TelemetryAnnotationProcessor.generateBootstrapSource(
                        cls.getClassAnnotation(TelemetryAnnotationProcessor.OPEN_TELEMETRY_DESC));
            }
        }
        throw new AssertionError("no @OpenTelemetry class in " + index.keySet());
    }

    private static File testClassesDir() throws Exception {
        URL url = TelemetryAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}

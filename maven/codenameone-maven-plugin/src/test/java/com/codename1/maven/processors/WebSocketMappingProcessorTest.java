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
import com.codename1.backend.HttpServer;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * `@WebSocketMapping`, and the entry point it generates.
 *
 * Asserted on the emitted source for the reason the controller tests give for the
 * same choice: the generated file is compiled straight to classes and never
 * written anywhere a test can open it, and the behavioural alternative -- calling
 * main() -- starts a server.
 */
public class WebSocketMappingProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String ENDPOINT =
            "package com.example;\n"
            + "import com.codename1.backend.WebSocket;\n"
            + "import com.codename1.backend.WebSocketSession;\n"
            + "import com.codename1.backend.annotations.WebSocketMapping;\n"
            + "@WebSocketMapping(\"/chat\")\n"
            + "public class Chat implements WebSocket {\n"
            + "    public void onOpen(WebSocketSession s) { }\n"
            + "    public void onText(WebSocketSession s, String m) { }\n"
            + "    public void onBinary(WebSocketSession s, byte[] m, int o, int l) { }\n"
            + "}\n";

    @Test
    public void registersTheEndpointOnTheBuilder() throws Exception {
        RestControllerAnnotationProcessor processor = run(compile("Chat", ENDPOINT));
        String bootstrap = processor.generateBootstrap("com.example");
        assertTrue("the endpoint is not registered through the callback:\n" + bootstrap,
                bootstrap.indexOf("registry.route(\"/chat\", new com.example.Chat())") >= 0);
        assertTrue("the entry point no longer goes through the builder:\n" + bootstrap,
                bootstrap.indexOf("com.codename1.backend.Backend.builder()") >= 0);
        assertTrue("the entry point does not run the server:\n" + bootstrap,
                bootstrap.indexOf(".run();") >= 0);
    }

    @Test
    public void aModuleWithOnlyWebSocketsStillGetsAnEntryPoint() throws Exception {
        // Guarding on controllers alone left a websocket-only server with no main
        // at all -- and the failure mode is that the build succeeds and produces
        // nothing runnable.
        ProcessorContext ctx = runContext(compile("Chat", ENDPOINT));
        assertFalse("a websocket-only module should build", ctx.hasErrors());
        byte[] name = ctx.getEmittedResources()
                .get(RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE);
        assertNotNull("no entry point was named for a websocket-only module", name);
        assertEquals("com.example.BackendApplication", new String(name, "UTF-8"));
    }

    @Test
    public void thePathIsRelativeToAClassLevelRequestMapping() throws Exception {
        String source =
                "package com.example;\n"
                + "import com.codename1.backend.WebSocket;\n"
                + "import com.codename1.backend.WebSocketSession;\n"
                + "import com.codename1.backend.annotations.RequestMapping;\n"
                + "import com.codename1.backend.annotations.WebSocketMapping;\n"
                + "@RequestMapping(\"/api\")\n"
                + "@WebSocketMapping(\"/chat\")\n"
                + "public class Scoped implements WebSocket {\n"
                + "    public void onOpen(WebSocketSession s) { }\n"
                + "    public void onText(WebSocketSession s, String m) { }\n"
                + "    public void onBinary(WebSocketSession s, byte[] m, int o, int l) { }\n"
                + "}\n";
        String bootstrap = run(compile("Scoped", source)).generateBootstrap("com.example");
        assertTrue("the base path was not applied:\n" + bootstrap,
                bootstrap.indexOf("registry.route(\"/api/chat\"") >= 0);
    }

    @Test
    public void refusesAClassThatDoesNotImplementWebSocket() throws Exception {
        // Otherwise the generated entry point does not compile, and the error
        // names a file nobody wrote.
        String source =
                "package com.example;\n"
                + "import com.codename1.backend.annotations.WebSocketMapping;\n"
                + "@WebSocketMapping(\"/chat\")\n"
                + "public class NotAnEndpoint {\n"
                + "}\n";
        assertTrue("a class that is not a WebSocket should be refused",
                runContext(compile("NotAnEndpoint", source)).hasErrors());
    }

    @Test
    public void refusesAnAbstractEndpoint() throws Exception {
        String source =
                "package com.example;\n"
                + "import com.codename1.backend.WebSocket;\n"
                + "import com.codename1.backend.WebSocketSession;\n"
                + "import com.codename1.backend.annotations.WebSocketMapping;\n"
                + "@WebSocketMapping(\"/chat\")\n"
                + "public abstract class Abstract implements WebSocket {\n"
                + "    public void onOpen(WebSocketSession s) { }\n"
                + "    public void onText(WebSocketSession s, String m) { }\n"
                + "    public void onBinary(WebSocketSession s, byte[] m, int o, int l) { }\n"
                + "}\n";
        assertTrue("an abstract endpoint cannot be instantiated and should be refused",
                runContext(compile("Abstract", source)).hasErrors());
    }

    @Test
    public void aBarePathIsNormalisedRatherThanRefused() throws Exception {
        // @GetMapping is lenient about the leading slash and so is this, because
        // the annotation set is Spring's on purpose and a reader should not have
        // to learn where the two differ.
        String source = ENDPOINT.replace("@WebSocketMapping(\"/chat\")",
                "@WebSocketMapping(\"chat\")");
        String bootstrap = run(compile("Chat", source)).generateBootstrap("com.example");
        assertTrue("a bare path should be normalised:\n" + bootstrap,
                bootstrap.indexOf("registry.route(\"/chat\"") >= 0);
    }

    @Test
    public void refusesAPathThatCannotBeNormalised() throws Exception {
        // A class-level @RequestMapping with no leading slash cannot be repaired
        // without guessing what the author meant, and the result would never
        // match a request target.
        String source =
                "package com.example;\n"
                + "import com.codename1.backend.WebSocket;\n"
                + "import com.codename1.backend.WebSocketSession;\n"
                + "import com.codename1.backend.annotations.RequestMapping;\n"
                + "import com.codename1.backend.annotations.WebSocketMapping;\n"
                + "@RequestMapping(\"api\")\n"
                + "@WebSocketMapping(\"/chat\")\n"
                + "public class Bare implements WebSocket {\n"
                + "    public void onOpen(WebSocketSession s) { }\n"
                + "    public void onText(WebSocketSession s, String m) { }\n"
                + "    public void onBinary(WebSocketSession s, byte[] m, int o, int l) { }\n"
                + "}\n";
        assertTrue("a base path with no leading slash should be refused",
                runContext(compile("Bare", source)).hasErrors());
    }

    @Test
    public void refusesTwoEndpointsOnOnePath() throws Exception {
        // Left unrefused this is decided by scan order, which is not something an
        // author can see or control.
        String second = ENDPOINT.replace("class Chat", "class Chat2");
        File classes = tmp.newFolder();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Chat", ENDPOINT);
        sources.put("com.example.Chat2", second);
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        assertTrue("two endpoints claiming one path should be a build error",
                runContext(classes).hasErrors());
    }

    @Test
    public void theGeneratedEntryPointIsStableAcrossBuilds() throws Exception {
        // Sorted by path, so the emitted text does not depend on scan order. A
        // bootstrap whose text moves recompiles for no reason and diffs noisily.
        String second = ENDPOINT.replace("class Chat", "class Alerts")
                .replace("\"/chat\"", "\"/alerts\"");
        File classes = tmp.newFolder();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Chat", ENDPOINT);
        sources.put("com.example.Alerts", second);
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        String bootstrap = run(classes).generateBootstrap("com.example");
        int alerts = bootstrap.indexOf("registry.route(\"/alerts\"");
        int chat = bootstrap.indexOf("registry.route(\"/chat\"");
        assertTrue("both endpoints should be registered:\n" + bootstrap, alerts >= 0 && chat >= 0);
        assertTrue("registration order should follow the path, not the scan:\n" + bootstrap,
                alerts < chat);
    }

    @Test
    public void acceptsAnEndpointThatInheritsWebSocket() throws Exception {
        // A direct-interface check calls this a build error even though the class
        // is assignable to WebSocket and the registration would be valid.
        String base =
                "package com.example;\n"
                + "import com.codename1.backend.WebSocket;\n"
                + "import com.codename1.backend.WebSocketSession;\n"
                + "public abstract class Base implements WebSocket {\n"
                + "    public void onOpen(WebSocketSession s) { }\n"
                + "    public void onText(WebSocketSession s, String m) { }\n"
                + "    public void onBinary(WebSocketSession s, byte[] m, int o, int l) { }\n"
                + "}\n";
        String derived =
                "package com.example;\n"
                + "import com.codename1.backend.annotations.WebSocketMapping;\n"
                + "@WebSocketMapping(\"/chat\")\n"
                + "public class Derived extends Base {\n"
                + "}\n";
        File classes = tmp.newFolder();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Base", base);
        sources.put("com.example.Derived", derived);
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        ProcessorContext ctx = runContext(classes);
        assertFalse("an endpoint inheriting WebSocket should be accepted", ctx.hasErrors());
    }

    @Test
    public void refusesAPathWithAQueryString() throws Exception {
        // tryUpgrade strips the query before looking the path up, so this endpoint
        // goes into the route map under a key nothing can ever match: the
        // application builds, starts, and the endpoint is simply unreachable.
        String source = ENDPOINT.replace("@WebSocketMapping(\"/chat\")",
                "@WebSocketMapping(\"/chat?room=1\")");
        assertTrue("a mapped path with a query string is unreachable and should be refused",
                runContext(compile("Chat", source)).hasErrors());
    }

    // ------------------------------------------------------------------ helpers

    private File compile(String simpleName, String source) throws Exception {
        File classes = tmp.newFolder();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example." + simpleName, source);
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        return classes;
    }

    private RestControllerAnnotationProcessor run(File classes) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        RestControllerAnnotationProcessor processor = new RestControllerAnnotationProcessor();
        drive(processor, index, classes);
        return processor;
    }

    private ProcessorContext runContext(File classes) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        RestControllerAnnotationProcessor processor = new RestControllerAnnotationProcessor();
        return drive(processor, index, classes);
    }

    private ProcessorContext drive(RestControllerAnnotationProcessor processor,
                                   Map<String, AnnotatedClass> index, File classes)
            throws Exception {
        List<String> cp = new ArrayList<String>();
        for (File f : backendClasspath()) {
            cp.add(f.getAbsolutePath());
        }
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog(), tmp.newFolder(), new Properties(), null,
                Collections.<String>emptyList(), "UTF-8", cp);
        processor.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                processor.processClass(cls, ctx);
            }
        }
        processor.finish(ctx);
        return ctx;
    }

    private static List<File> backendClasspath() throws Exception {
        URL url = HttpServer.class.getProtectionDomain().getCodeSource().getLocation();
        return Arrays.asList(new File(url.toURI()));
    }
}

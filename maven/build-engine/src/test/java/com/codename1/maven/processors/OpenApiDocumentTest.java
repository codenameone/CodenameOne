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

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.build.SystemStreamLog;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The OpenAPI document the router processor writes: what it says about the routes,
/// the parameters and the schemas, and that only a build that asks links the
/// handler that serves it.
public class OpenApiDocumentTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String SOURCE =
            "package com.example;\n"
            + "import com.codename1.backend.annotations.*;\n"
            + "import com.codename1.backend.validation.*;\n"
            + "import com.codename1.annotations.JsonProperty;\n"
            + "import com.codename1.annotations.JsonIgnore;\n"
            + "import java.util.*;\n"
            + "@RestController\n"
            + "@RequestMapping(\"/api\")\n"
            + "public class Notes {\n"
            + "    public enum Color { RED, GREEN }\n"
            + "    public static class Tag { public String label; }\n"
            + "    public static class Note {\n"
            + "        public long id;\n"
            + "        @NotBlank @Size(max = 80) public String title;\n"
            + "        @JsonProperty(\"body_text\") public String body;\n"
            + "        @JsonIgnore public String secret;\n"
            + "        @Min(0) @Max(5) public int stars;\n"
            + "        public Color color;\n"
            + "        public List<Tag> tags;\n"
            + "        public Map<String, Integer> counts;\n"
            + "        public Date created;\n"
            + "    }\n"
            + "    public static class Page<T> { public List<T> items; public int total; }\n"
            + "    @GetMapping(\"/notes/{id}\")\n"
            + "    public Note get(@PathVariable(\"id\") long id) { return new Note(); }\n"
            + "    @GetMapping(\"/notes\")\n"
            + "    public Page<Note> list(@RequestParam(value = \"q\", required = false) String q,\n"
            + "            @RequestParam(value = \"size\", defaultValue = \"20\") @Max(100) int size,\n"
            + "            @RequestHeader(\"X-Trace\") String trace) { return new Page<Note>(); }\n"
            + "    @PostMapping(\"/notes\")\n"
            + "    @ResponseStatus(201)\n"
            + "    public Note create(@Valid @RequestBody Note note) { return note; }\n"
            + "    @DeleteMapping(\"/notes/{id}\")\n"
            + "    @PreAuthorize(\"hasRole('ADMIN')\")\n"
            + "    public void delete(@PathVariable(\"id\") long id) { }\n"
            + "    @GetMapping(\"/files/{*path}\")\n"
            + "    public String file(@PathVariable(\"path\") String path) { return path; }\n"
            + "    @GetMapping(\"/orgs/{org}/count\")\n"
            + "    public int count() { return 0; }\n"
            + "}\n";

    private static final String SECURITY =
            "package com.example;\n"
            + "import com.codename1.backend.annotations.*;\n"
            + "import com.codename1.backend.security.*;\n"
            + "@Configuration public class SecurityConfig {\n"
            + "    @Bean public SecurityFilterChain chain(HttpSecurity http) {\n"
            + "        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())\n"
            + "            .httpBasic(Customizer.withDefaults());\n"
            + "        return http.build();\n"
            + "    }\n"
            + "}\n";

    @Test
    public void describesTheRoutesTheRouterServes() throws Exception {
        File module = tmp.newFolder();
        File classes = compile(SOURCE);
        ProcessorContext ctx = run(classes, module, null, true);
        Map doc = Json.parseObject(document(classes));
        // The plugin's generator is tested against this exact document, so a change
        // here is a change that generator has to be shown to accept: copy the new
        // output over the golden file (-Dopenapi.updateGolden=true does) and run
        // GenerateOpenApiMojoTest.
        File golden = new File(System.getProperty("user.dir"),
                "../codenameone-maven-plugin/src/test/resources/openapi/backend-generated.json");
        if (Boolean.getBoolean("openapi.updateGolden")) {
            Files.write(golden.toPath(), document(classes).getBytes(StandardCharsets.UTF_8));
        }
        if (golden.isFile()) {
            assertEquals(new String(Files.readAllBytes(golden.toPath()), StandardCharsets.UTF_8),
                    document(classes));
        }

        assertEquals("3.1.0", doc.get("openapi"));
        Map paths = (Map) doc.get("paths");
        assertEquals(new java.util.TreeSet<Object>(Arrays.asList("/api/notes/{id}",
                "/api/notes", "/api/files/{path}", "/api/orgs/{org}/count")), new java.util.TreeSet<Object>(paths.keySet()));

        Map get = op(paths, "/api/notes/{id}", "get");
        assertEquals("get", get.get("operationId"));
        Map id = (Map) ((List) get.get("parameters")).get(0);
        assertEquals("path", id.get("in"));
        assertEquals(Boolean.TRUE, id.get("required"));
        assertEquals("int64", ((Map) id.get("schema")).get("format"));
        assertEquals("#/components/schemas/Notes_Note", schemaOf(get, "200").get("$ref"));

        Map list = op(paths, "/api/notes", "get");
        List params = (List) list.get("parameters");
        Map q = (Map) params.get(0);
        assertEquals(Boolean.FALSE, q.get("required"));
        Map size = (Map) params.get(1);
        assertEquals("a default makes it optional", Boolean.FALSE, size.get("required"));
        assertEquals(20L, ((Number) ((Map) size.get("schema")).get("default")).longValue());
        assertEquals(100L, ((Number) ((Map) size.get("schema")).get("maximum")).longValue());
        assertEquals("header", ((Map) params.get(2)).get("in"));
        assertEquals("#/components/schemas/Notes_Page_Notes_Note",
                schemaOf(list, "200").get("$ref"));

        Map post = op(paths, "/api/notes", "post");
        Map responses = (Map) post.get("responses");
        assertTrue(responses.containsKey("201"));
        assertTrue(responses.containsKey("400"));
        Map body = (Map) post.get("requestBody");
        assertEquals(Boolean.TRUE, body.get("required"));
        assertNotNull(((Map) body.get("content")).get("application/json"));

        Map delete = op(paths, "/api/notes/{id}", "delete");
        Map deleted = (Map) delete.get("responses");
        assertTrue(deleted.containsKey("204"));
        assertNull("no body on a 204", ((Map) deleted.get("204")).get("content"));
        assertTrue("method security is a 401/403", deleted.containsKey("403"));

        Map schemas = (Map) ((Map) doc.get("components")).get("schemas");
        Map note = (Map) schemas.get("Notes_Note");
        Map props = (Map) note.get("properties");
        assertTrue(props.containsKey("body_text"));
        assertFalse("@JsonIgnore is not in the JSON form", props.containsKey("secret"));
        assertFalse(props.containsKey("body"));
        assertEquals(Arrays.asList("title"), note.get("required"));
        Map title = (Map) props.get("title");
        assertEquals(80L, ((Number) title.get("maxLength")).longValue());
        assertEquals(Arrays.asList("RED", "GREEN"), ((Map) props.get("color")).get("enum"));
        assertEquals("#/components/schemas/Notes_Tag",
                ((Map) ((Map) props.get("tags")).get("items")).get("$ref"));
        assertEquals("integer", ((Map) ((Map) props.get("counts"))
                .get("additionalProperties")).get("type"));
        assertEquals("int64", ((Map) props.get("created")).get("format"));
        Map page = (Map) ((Map) schemas.get("Notes_Page_Notes_Note")).get("properties");
        assertEquals("#/components/schemas/Notes_Note",
                ((Map) ((Map) page.get("items")).get("items")).get("$ref"));

        assertTrue("written beside the classes", new File(classes.getParentFile(),
                "openapi.json").isFile());
        assertNotNull(ctx.getEmittedResources().get(
                RestControllerAnnotationProcessor.OPENAPI_RESOURCE));
        assertTrue("a development build serves it", new File(classes, "com/example/"
                + RestControllerAnnotationProcessor.OPENAPI_CLASS + ".class").isFile());
    }

    @Test
    public void aPackagedBuildThatDidNotAskServesNothing() throws Exception {
        File classes = compile(SOURCE);
        ProcessorContext ctx = run(classes, tmp.newFolder(), null, false);
        assertTrue("the document is still written", new File(classes.getParentFile(),
                "openapi.json").isFile());
        assertFalse(new File(classes, "com/example/"
                + RestControllerAnnotationProcessor.OPENAPI_CLASS + ".class").isFile());
        assertNull("a build that serves nothing ships no copy of the API surface",
                ctx.getEmittedResources().get(RestControllerAnnotationProcessor.OPENAPI_RESOURCE));
    }

    @Test
    public void onlyABuildThatAsksLinksTheHandler() throws Exception {
        File module = tmp.newFolder();
        File classes = compile(SOURCE);
        run(classes, module, "cn1.openapi.enabled=true\ncn1.openapi.title=Notes API\n"
                + "cn1.openapi.security=bearer\n", false);
        File holder = new File(classes, "com/example/"
                + RestControllerAnnotationProcessor.OPENAPI_CLASS + ".class");
        assertTrue(holder.isFile());
        Map doc = Json.parseObject(document(classes));
        assertEquals("Notes API", ((Map) doc.get("info")).get("title"));
        Map schemes = (Map) ((Map) doc.get("components")).get("securitySchemes");
        assertEquals("bearer", ((Map) schemes.get("bearerAuth")).get("scheme"));
    }

    @Test
    public void pathTemplatesDropTheirPatterns() {
        assertEquals("/a/{id}/b", OpenApiWriter.templatePath("/a/{id:[0-9]{1,3}}/b"));
        assertEquals("/files/{rest}", OpenApiWriter.templatePath("/files/{*rest}"));
        assertEquals("/", OpenApiWriter.templatePath(""));
    }

    @Test
    public void securityDefaultsAndPathVariablesAreDescribedPerOperation() throws Exception {
        File module = tmp.newFolder();
        File classes = compile(SOURCE);
        run(classes, module, "cn1.openapi.enabled=true\ncn1.openapi.security=bearer\n", false);
        Map doc = Json.parseObject(document(classes));
        assertNull("no document-wide requirement: public routes stay public",
                doc.get("security"));
        Map paths = (Map) doc.get("paths");
        assertNotNull(op(paths, "/api/notes/{id}", "delete").get("security"));
        assertNull(op(paths, "/api/notes/{id}", "get").get("security"));
        Map size = null;
        for (Object o : (List) op(paths, "/api/notes", "get").get("parameters")) {
            if ("size".equals(((Map) o).get("name"))) {
                size = (Map) ((Map) o).get("schema");
            }
        }
        assertNotNull(size);
        assertTrue("an integer's default is a number", size.get("default") instanceof Number);
        assertEquals(20L, ((Number) size.get("default")).longValue());
        List params = (List) op(paths, "/api/orgs/{org}/count", "get").get("parameters");
        assertNotNull("an unbound template variable is still declared", params);
        assertEquals("org", ((Map) params.get(0)).get("name"));
        assertEquals(Boolean.TRUE, ((Map) params.get(0)).get("required"));
    }

    @Test
    public void twoClassesOfOneSimpleNameGetTwoSchemas() throws Exception {
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.a.Account", "package com.a; public class Account { public String a; }");
        sources.put("com.b.Account", "package com.b; public class Account { public long b; }");
        sources.put("com.example.Accounts", "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController public class Accounts {\n"
                + "  @GetMapping(\"/a\") public com.a.Account a() { return null; }\n"
                + "  @GetMapping(\"/b\") public com.b.Account b() { return null; }\n"
                + "}\n");
        sources.put("com.example.MoreAccounts", "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController public class MoreAccounts {\n"
                + "  @GetMapping(\"/c\") public int a() { return 0; }\n"
                + "}\n");
        File classes = compile(sources);
        run(classes, tmp.newFolder(), null, false);
        Map doc = Json.parseObject(document(classes));
        Map schemas = (Map) ((Map) doc.get("components")).get("schemas");
        assertEquals(2, schemas.size());
        Map paths = (Map) doc.get("paths");
        Object a = schemaOf(op(paths, "/a", "get"), "200").get("$ref");
        Object b = schemaOf(op(paths, "/b", "get"), "200").get("$ref");
        assertFalse("v1.Account and v2.Account must not share a schema", a.equals(b));
        assertNotNull(((Map) ((Map) schemas.get(((String) b).substring(21))).get("properties"))
                .get("b"));
        Object idA = op(paths, "/a", "get").get("operationId");
        Object idC = op(paths, "/c", "get").get("operationId");
        assertFalse("operationIds are unique", idA.equals(idC));
    }

    @Test
    public void theSwaggerPageAllowsOnlyItsOwnScript() throws Exception {
        java.lang.reflect.Method page = com.codename1.impl.backend.OpenApiDocs.class
                .getDeclaredMethod("page", String.class, String.class);
        page.setAccessible(true);
        java.lang.reflect.Method policy = com.codename1.impl.backend.OpenApiDocs.class
                .getDeclaredMethod("policy", String.class);
        policy.setAccessible(true);
        String script = (String) field("INLINE_SCRIPT");
        String hash = "sha256-" + java.util.Base64.getEncoder().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256")
                        .digest(script.getBytes(StandardCharsets.UTF_8)));
        assertEquals("the policy names the script it allows", hash, field("INLINE_SCRIPT_HASH"));
        String def = (String) field("DEFAULT_ASSETS");
        String html = (String) page.invoke(null, def, "/v3/api-docs?a=1&b='x'");
        assertTrue(html.contains("<script>" + script + "</script>"));
        assertTrue("the path is an attribute, escaped as one",
                html.contains("data-url=\"/v3/api-docs?a=1&amp;b=&#39;x&#39;\""));
        assertTrue("the pinned bundle is integrity checked",
                html.contains((String) field("DEFAULT_JS_INTEGRITY")));
        String own = (String) page.invoke(null, "/assets/swagger", "/v3/api-docs");
        assertFalse(own.contains("integrity="));
        String csp = (String) policy.invoke(null, def);
        assertTrue(csp, csp.contains("script-src https://cdn.jsdelivr.net '" + hash + "'"));
        assertTrue(csp, csp.contains("connect-src 'self'"));
        assertTrue(((String) policy.invoke(null, "/assets/swagger")).contains("script-src 'self'"));
    }

    private static Object field(String name) throws Exception {
        java.lang.reflect.Field f = com.codename1.impl.backend.OpenApiDocs.class
                .getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    @Test
    public void theDocumentServesFromTheRuntime() throws Exception {
        com.codename1.backend.Config config = com.codename1.impl.backend.BackendAccess.get()
                .testConfig(new Properties(), "dev");
        com.codename1.impl.backend.OpenApiDocs docs =
                com.codename1.impl.backend.OpenApiDocs.fromConfig(config, "{\"openapi\":\"3.1.0\"}");
        assertNotNull(docs);
        Properties off = new Properties();
        off.setProperty("cn1.openapi.enabled", "false");
        assertNull(com.codename1.impl.backend.OpenApiDocs.fromConfig(
                com.codename1.impl.backend.BackendAccess.get().testConfig(off, "dev"), "{}"));
    }

    private static Map op(Map paths, String path, String method) {
        Map item = (Map) paths.get(path);
        assertNotNull(path, item);
        Map op = (Map) item.get(method);
        assertNotNull(method + " " + path, op);
        return op;
    }

    private static Map schemaOf(Map op, String status) {
        Map ok = (Map) ((Map) op.get("responses")).get(status);
        Map content = (Map) ok.get("content");
        return (Map) ((Map) content.get("application/json")).get("schema");
    }

    private static String document(File classes) throws Exception {
        return new String(Files.readAllBytes(new File(classes.getParentFile(), "openapi.json")
                .toPath()), StandardCharsets.UTF_8);
    }

    private File compile(String source) throws Exception {
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Notes", source);
        return compile(sources);
    }

    private File compile(Map<String, String> sources) throws Exception {
        File root = tmp.newFolder();
        File classes = new File(root, "classes");
        assertTrue(classes.mkdirs());
        sources.put("com.example.SecurityConfig", SECURITY);
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        return classes;
    }

    private ProcessorContext run(File classes, File module, String properties, boolean dev)
            throws Exception {
        if (properties != null) {
            File res = new File(module, "src/main/resources");
            assertTrue(res.mkdirs());
            FileOutputStream out = new FileOutputStream(new File(res, "application.properties"));
            try {
                out.write(properties.getBytes(StandardCharsets.UTF_8));
            } finally {
                out.close();
            }
        }
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(dev);
        List<String> cp = new ArrayList<String>();
        for (File f : backendClasspath()) {
            cp.add(f.getAbsolutePath());
        }
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog(), module, new Properties(), null,
                Collections.<String>emptyList(), "UTF-8", cp);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                proc.processClass(cls, ctx);
            }
        }
        proc.finish(ctx);
        if (ctx.hasErrors()) {
            fail(ctx.getErrors().toString());
        }
        return ctx;
    }

    private static List<File> backendClasspath() throws Exception {
        URL url = HttpServer.class.getProtectionDomain().getCodeSource().getLocation();
        return Arrays.asList(new File(url.toURI()));
    }
}

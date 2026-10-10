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

import static org.junit.Assert.*;

import com.codename1.backend.HttpServer;
import com.codename1.backend.mvc.*;
import com.codename1.build.SystemStreamLog;
import com.codename1.maven.annotations.*;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

/** Runs compiled views and generated MVC routes, not snapshots of generated source. */
public class MvcTemplatesTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private File project, classes;
    private ProcessorContext context;
    private URLClassLoader loader;
    private static final String TYPES =
            "package sample; public class Product { public String name; public int quantity; public"
                + " boolean active; public Product() {} public String getLabel() { return name; }"
                + " }";
    private static final String DECL =
            "<!-- cn1:model product sample.Product --><!-- cn1:model products"
                    + " java.util.List<sample.Product> -->";

    private void setup() throws Exception {
        project = tmp.newFolder();
        classes = new File(project, "target/classes");
        assertTrue(classes.mkdirs());
        JavaSourceCompiler.compile(
                Collections.singletonMap("sample.Product", TYPES), classes, classpath());
        context =
                new ProcessorContext(
                        classes,
                        tmp.newFolder(),
                        ClassScanner.scan(classes),
                        new SystemStreamLog(),
                        project,
                        new Properties(),
                        null,
                        Collections.<String>emptyList(),
                        "UTF-8",
                        Arrays.asList(classpath().get(0).getPath(), classes.getPath()));
    }

    private static List<File> classpath() throws Exception {
        return Collections.singletonList(
                new File(
                        HttpServer.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI()));
    }

    private void template(String name, String text) throws Exception {
        File file = new File(project, "src/main/resources/templates/" + name + ".html");
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private void compile() throws Exception {
        Map<String, String> sources = new MvcTemplates(context).sources();
        List<File> cp = new ArrayList<File>(classpath());
        cp.add(classes);
        JavaSourceCompiler.compile(sources, classes, cp);
        loader =
                new URLClassLoader(
                        new URL[] {classes.toURI().toURL()}, getClass().getClassLoader());
    }

    private Object product(String name, int quantity) throws Exception {
        Object p = loader.loadClass("sample.Product").newInstance();
        p.getClass().getField("name").set(p, name);
        p.getClass().getField("quantity").setInt(p, quantity);
        return p;
    }

    private String render(String view, Model model) throws Exception {
        try {
            return new String(
                    (byte[])
                            loader.loadClass("com.codename1.generated.mvc.Views")
                                    .getMethod("render", String.class, Model.class)
                                    .invoke(null, view, model),
                    StandardCharsets.UTF_8);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void typedRenderingFragmentsUnicodeEscapingAndUrls() throws Exception {
        setup();
        template(
                "products",
                DECL
                        + "<!doctype html><html><body><div th:replace=\"~{layout ::"
                        + " header}\"></div><ul th:fragment=\"rows\"><li th:each=\"p, s :"
                        + " ${products}\" th:if=\"${p.quantity > 0}\" th:classappend=\"${s.first ?"
                        + " 'first' : 'rest'}\"><a"
                        + " th:href=\"@{/products/{id}(id=${p.quantity},q=${p.name})}\""
                        + " th:text=\"${p.label}\">sample</a></li></ul></body></html>");
        template("layout", "<header th:fragment=\"header\">Catalog</header>");
        compile();
        Object p = product("<שלום & \"😀\">", 2);
        Model m = new Model().addAttribute("products", Arrays.asList(p));
        String html = render("products", m);
        assertTrue(html, html.contains("<header>Catalog</header>"));
        assertTrue(html, html.contains("&lt;שלום &amp; &quot;😀&quot;&gt;"));
        assertTrue(html, html.contains("/products/2?q=%3C"));
        assertFalse(html, html.contains("th:"));
        String fragment = render("products :: rows", m);
        assertTrue(fragment, fragment.startsWith("<ul>"));
        assertFalse(fragment, fragment.contains("<html>"));
        assertFalse(fragment, fragment.contains("Catalog"));
        try {
            render("products :: rows", new Model());
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("products"));
        }
        try {
            render(
                    "products :: rows",
                    new Model().addAttribute("products", Arrays.asList("wrong")));
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("element type"));
        }
    }

    @Test
    public void formsPreserveRejectedValuesAndErrors() throws Exception {
        setup();
        template(
                "edit",
                DECL
                        + "<form th:object=\"${product}\" method=\"post\"><input"
                        + " th:field=\"*{name}\"><input th:field=\"*{quantity}\"><input"
                        + " type=\"checkbox\" th:field=\"*{active}\"><textarea"
                        + " th:field=\"*{name}\"></textarea><select th:field=\"*{name}\"><option"
                        + " value=\"A\">A</option><option value=\"B\">B</option></select><span"
                        + " th:errors=\"*{quantity}\"></span></form>");
        compile();
        BindingResult errors = new BindingResult();
        errors.submitted("quantity", "oops <");
        errors.rejectValue("quantity", "Must be a number <");
        Model m =
                new Model()
                        .addAttribute("product", product("B", 3))
                        .addAttribute("BindingResult.product", errors)
                        .addAttribute(
                                "_csrf",
                                new com.codename1.backend.security.CsrfToken() {
                                    public String getToken() {
                                        return "token<&";
                                    }

                                    public String getHeaderName() {
                                        return "X-CSRF-TOKEN";
                                    }

                                    public String getParameterName() {
                                        return "_csrf";
                                    }
                                });
        String html = render("edit", m);
        assertTrue(html, html.contains("value=\"oops &lt;\""));
        assertTrue(html, html.contains("Must be a number &lt;"));
        assertTrue(html, html.contains("value=\"B\" selected=\"selected\""));
        assertTrue(html, html.contains("token&lt;&amp;"));
        assertTrue(html, html.contains("name=\"_active\""));
        template("dynamic", "<form th:attr=\"HX-POST=@{/save}\"><button>Save</button></form>");
        compile();
        assertTrue(render("dynamic", m).contains("name=\"_csrf\""));
    }

    @Test
    public void rejectsBadTemplatesAtBuildTime() throws Exception {
        for (String html :
                Arrays.asList(
                        DECL + "<p th:text=\"${product.missing}\"></p>",
                        "<p th:utext=\"'unsafe'\"></p>",
                        "<span th:errors=\"*{name}\"></span>",
                        "<p th:text=\"${undeclared}\"></p>",
                        "<div th:fragment=\"loop\" th:replace=\"~{bad :: loop}\"></div>",
                        DECL + "<p th:text=\"${product.getLabel()}\"></p>")) {
            setup();
            template("bad", html);
            try {
                new MvcTemplates(context).sources();
                fail(html);
            } catch (IllegalArgumentException expected) {
                assertTrue(
                        expected.getMessage(),
                        expected.getMessage().contains("bad")
                                || expected.getMessage().contains("Recursive"));
            }
        }
    }

    @Test
    public void changesAndDeletionsReplaceRegistry() throws Exception {
        setup();
        template("a", "<p>A</p>");
        template("b", "<p>B</p>");
        compile();
        assertTrue(render("b", new Model()).contains("B"));
        Files.delete(new File(project, "src/main/resources/templates/b.html").toPath());
        template("a", "<p>New</p>");
        compile();
        assertTrue(render("a", new Model()).contains("New"));
        try {
            render("b", new Model());
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Unknown compiled view"));
        }
    }

    @Test
    public void rendersConcurrentlyWithoutSharingModels() throws Exception {
        setup();
        template("a", "<!-- cn1:model name java.lang.String --><p th:text=\"${name}\"></p>");
        compile();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> tasks = new ArrayList<Future<String>>();
            for (int i = 0; i < 40; i++) {
                final int id = i;
                tasks.add(
                        pool.submit(
                                new Callable<String>() {
                                    public String call() throws Exception {
                                        return render(
                                                "a", new Model().addAttribute("name", "n" + id));
                                    }
                                }));
            }
            for (int i = 0; i < tasks.size(); i++)
                assertTrue(tasks.get(i).get().contains(">n" + i + "<"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void generatedControllerBindsFormsRendersAndPreservesRestBodies() throws Exception {
        setup();
        template(
                "edit",
                DECL
                        + "<form th:object=\"${product}\"><input th:field=\"*{quantity}\"><span"
                        + " th:errors=\"*{quantity}\"></span><b"
                        + " th:text=\"${product.name}\"></b></form>");
        String source =
                "package sample; import com.codename1.backend.*; import"
                    + " com.codename1.backend.annotations.*; import com.codename1.backend.mvc.*;"
                    + " @Controller public class Pages {@GetMapping(\"/edit\") public String"
                    + " edit(Model model) {Product p=new"
                    + " Product();p.name=\"Hello\";model.addAttribute(\"product\",p);return"
                    + " \"edit\";}@PostMapping(\"/edit\") public String"
                    + " save(@ModelAttribute(\"product\") Product product, BindingResult errors)"
                    + " {if(product.quantity<1)errors.rejectValue(\"quantity\",\"Positive quantity"
                    + " required\");return"
                    + " errors.hasErrors()?\"edit\":\"redirect:/edit\";}@GetMapping(\"/body\")"
                    + " @ResponseBody public String body(){return"
                    + " \"literal\";}@GetMapping(\"/model\") public ModelAndView view(){Product"
                    + " p=new Product();p.name=\"MV\";return new"
                    + " ModelAndView(\"edit\").addObject(\"product\",p);} }";
        List<File> cp = new ArrayList<File>(classpath());
        cp.add(classes);
        JavaSourceCompiler.compile(Collections.singletonMap("sample.Pages", source), classes, cp);
        context =
                new ProcessorContext(
                        classes,
                        tmp.newFolder(),
                        ClassScanner.scan(classes),
                        new SystemStreamLog(),
                        project,
                        new Properties(),
                        null,
                        Collections.<String>emptyList(),
                        "UTF-8",
                        Arrays.asList(cp.get(0).getPath(), classes.getPath()));
        RestControllerAnnotationProcessor processor = new RestControllerAnnotationProcessor();
        processor.start(context);
        for (AnnotatedClass cls : context.getClassIndex().values())
            processor.processClass(cls, context);
        processor.finish(context);
        assertFalse(context.getErrors().toString(), context.hasErrors());
        loader =
                new URLClassLoader(
                        new URL[] {classes.toURI().toURL()}, getClass().getClassLoader());
        Class<?> controller = loader.loadClass("sample.Pages"),
                router = loader.loadClass("sample.PagesRouter");
        Object handler = router.getConstructor(controller).newInstance(controller.newInstance());
        Method handle = router.getMethod("handle", HttpServer.Request.class);
        assertTrue(
                body(handle.invoke(handler, request("GET", "/edit", null, false)))
                        .contains("Hello"));
        assertEquals("literal", body(handle.invoke(handler, request("GET", "/body", null, false))));
        assertTrue(
                body(handle.invoke(handler, request("GET", "/model", null, false))).contains("MV"));
        Object invalid =
                handle.invoke(
                        handler, request("POST", "/edit", "name=A&quantity=no&_active=on", false));
        assertTrue(body(invalid).contains("value=\"no\""));
        assertTrue(body(invalid).contains("Invalid value"));
        Object saved =
                handle.invoke(
                        handler, request("POST", "/edit", "name=A&quantity=2&_active=on", false));
        assertEquals(303, field(saved, "status"));
        Object hx =
                handle.invoke(
                        handler, request("POST", "/edit", "name=A&quantity=2&_active=on", true));
        assertEquals(200, field(hx, "status"));
    }

    @Test
    public void packagedAssetsAreExactAndTemplatesArePrivate() throws Exception {
        setup();
        File asset = new File(project, "src/main/resources/static/probe.bin");
        asset.getParentFile().mkdirs();
        byte[] data = new byte[3000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        Files.write(asset.toPath(), data);
        JavaSourceCompiler.compile(MvcAssets.sources(project), classes, classpath());
        loader =
                new URLClassLoader(
                        new URL[] {classes.toURI().toURL()}, getClass().getClassLoader());
        HttpServer.Handler assets =
                (HttpServer.Handler)
                        loader.loadClass("com.codename1.generated.mvc.Assets").newInstance();
        assertArrayEquals(
                data,
                (byte[])
                        field(
                                assets.handle(request("GET", "/static/probe.bin?v=1", null, false)),
                                "body"));
        assertNull(assets.handle(request("GET", "/templates/edit.html", null, false)));
        assertNull(assets.handle(request("GET", "/static/../templates/edit.html", null, false)));
        assertNull(assets.handle(request("POST", "/static/probe.bin", null, false)));
    }

    @Test
    public void nullsBooleanAttributesAndUnsafeContexts() throws Exception {
        setup();
        template(
                "a",
                DECL
                        + "<input th:disabled=\"${product.active}\"><p"
                        + " th:text=\"${product.name}\"></p>");
        compile();
        String html = render("a", new Model().addAttribute("product", null));
        assertTrue(html, html.contains("<p></p>"));
        assertFalse(html, html.contains("disabled"));
        template("a", DECL + "<script th:text=\"${product.name}\"></script>");
        try {
            new MvcTemplates(context).sources();
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("script/style"));
        }
        for (String url :
                Arrays.asList(
                        "javascript:alert(1)",
                        "JaVaScRiPt:alert(1)",
                        "data:text/html,test",
                        "java\nscript:alert(1)")) {
            try {
                Html.attribute(new com.codename1.backend.ByteSink(16), "href", url);
                fail(url);
            } catch (IllegalArgumentException expected) {
            }
        }
        assertTrue(Html.checked("on", "true"));
        assertFalse(Html.checked(Boolean.TRUE, "false"));
    }

    @Test
    public void urlSchemesAreIndependentOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            for (String url :
                    Arrays.asList(
                            "HTTPS://example.test/",
                            "MaIlTo:test@example.test",
                            "TEL:123",
                            "/products")) {
                Html.safeUrl(url);
            }
            for (String url :
                    Arrays.asList(
                            "JAVASCRIPT:alert(1)",
                            "ma\u0131lto:test@example.test",
                            "ma\u0130lto:test@example.test")) {
                try {
                    Html.safeUrl(url);
                    fail(url);
                } catch (IllegalArgumentException expected) {
                }
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void modelErrorsAndUnsafeRedirects() throws Exception {
        setup();
        template("a", DECL + "<p th:text=\"${product.name}\"></p>");
        compile();
        try {
            render("a", new Model().addAttribute("product", "wrong"));
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Wrong model type"));
        }
        for (String location :
                Arrays.asList(
                        "//evil.test/",
                        "https://evil.test/",
                        "/\\evil.test/",
                        "/ok\r\nLocation: evil")) {
            try {
                Htmx.redirect(location);
                fail(location);
            } catch (IllegalArgumentException expected) {
            }
        }
        Object response = Htmx.redirect("/products");
        assertEquals(200, field(response, "status"));
        assertTrue(field(response, "extraHeaders").toString().contains("/products"));
    }

    @Test
    public void indexedModelsCheckElementTypesBeforePropertyAccess() throws Exception {
        setup();
        template("a", DECL + "<p th:text=\"${products[0].name}\"></p>");
        compile();
        assertTrue(
                render(
                                "a",
                                new Model()
                                        .addAttribute(
                                                "products", Arrays.asList(product("Indexed", 1))))
                        .contains("Indexed"));
        try {
            render("a", new Model().addAttribute("products", Arrays.asList("wrong")));
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("indexed element"));
        }
    }

    private void fixtureSources(Map<String, String> sources) throws Exception {
        List<File> cp = new ArrayList<File>(classpath());
        cp.add(classes);
        JavaSourceCompiler.compile(sources, classes, cp);
        context =
                new ProcessorContext(
                        classes,
                        tmp.newFolder(),
                        ClassScanner.scan(classes),
                        new SystemStreamLog(),
                        project,
                        new Properties(),
                        null,
                        Collections.<String>emptyList(),
                        "UTF-8",
                        Arrays.asList(cp.get(0).getPath(), classes.getPath()));
    }

    private HttpServer.Handler controller(String source) throws Exception {
        fixtureSources(Collections.singletonMap("sample.Pages", source));
        RestControllerAnnotationProcessor processor = new RestControllerAnnotationProcessor();
        processor.start(context);
        for (AnnotatedClass cls : context.getClassIndex().values())
            processor.processClass(cls, context);
        processor.finish(context);
        assertFalse(context.getErrors().toString(), context.hasErrors());
        loader =
                new URLClassLoader(
                        new URL[] {classes.toURI().toURL()}, getClass().getClassLoader());
        Class<?> pages = loader.loadClass("sample.Pages");
        return (HttpServer.Handler)
                loader.loadClass("sample.PagesRouter")
                        .getConstructor(pages)
                        .newInstance(pages.newInstance());
    }

    @Test
    public void mixedCaseDynamicAttributesUseCanonicalSecurityChecks() throws Exception {
        setup();
        for (String attribute :
                Arrays.asList("HX-ON:click", "Hx-Vals", "HX-HEADERS", "OnClick", "TH:text")) {
            template("a", "<div th:attr=\"" + attribute + "='payload'\"></div>");
            try {
                new MvcTemplates(context).sources();
                fail(attribute);
            } catch (IllegalArgumentException expected) {
                assertTrue(
                        expected.getMessage(),
                        expected.getMessage().contains("Unsupported dynamic attribute"));
            }
        }
        template(
                "a",
                "<!-- cn1:model url java.lang.String --><a href=\"/old\""
                    + " th:attr=\"HREF=${url}\">link</a>");
        compile();
        String html = render("a", new Model().addAttribute("url", "/new"));
        assertTrue(html, html.contains("href=\"/new\""));
        assertFalse(html, html.contains("/old"));
        try {
            render("a", new Model().addAttribute("url", "javascript:alert(1)"));
            fail("Uppercase HREF must still validate URLs");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Unsafe URL"));
        }
        try {
            Html.attribute(new com.codename1.backend.ByteSink(16), "HREF", "javascript:alert(1)");
            fail("The runtime helper must validate uppercase URL attributes too");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Unsafe URL"));
        }
    }

    @Test
    public void boxedNumbersCompareByValueWithNullsAndJavaPromotion() throws Exception {
        setup();
        template(
                "a",
                "<!-- cn1:model left java.lang.Integer --><!-- cn1:model right java.lang.Integer"
                    + " --><b th:text=\"${left == right}\"></b><i th:text=\"${left !="
                    + " right}\"></i>");
        template(
                "mixed",
                "<!-- cn1:model left java.lang.Integer --><!-- cn1:model right java.lang.Long --><b"
                    + " th:text=\"${left eq right}\"></b><i th:text=\"${left == 1000}\"></i>");
        template(
                "longs",
                "<!-- cn1:model left java.lang.Long --><!-- cn1:model right java.lang.Long -->"
                        + "<b th:text=\"${left == right}\"></b>");
        template(
                "floating",
                "<!-- cn1:model left java.lang.Float --><!-- cn1:model right java.lang.Double -->"
                        + "<b th:text=\"${left == right}\"></b>");
        compile();
        Model model =
                new Model()
                        .addAttribute("left", new Integer(1000))
                        .addAttribute("right", new Integer(1000));
        assertTrue(render("a", model).contains("<b>true</b><i>false</i>"));
        model.addAttribute("right", 1001);
        assertTrue(render("a", model).contains("<b>false</b><i>true</i>"));
        model.addAttribute("left", null);
        assertTrue(render("a", model).contains("<b>false</b>"));
        model.addAttribute("right", null);
        assertTrue(render("a", model).contains("<b>true</b>"));
        model.addAttribute("left", 1000).addAttribute("right", 1000L);
        assertTrue(render("mixed", model).contains("<b>true</b><i>true</i>"));
        model.addAttribute("left", 9007199254740992L).addAttribute("right", 9007199254740993L);
        assertTrue(render("longs", model).contains("<b>false</b>"));
        model.addAttribute("left", 1.5f).addAttribute("right", 1.5d);
        assertTrue(render("floating", model).contains("<b>true</b>"));
        model.addAttribute("left", Float.NaN).addAttribute("right", Double.NaN);
        assertTrue(render("floating", model).contains("<b>false</b>"));
    }

    @Test
    public void iterationParityMatchesThymeleafAndCssChildCounting() throws Exception {
        setup();
        template(
                "a",
                DECL
                        + "<p th:each=\"p, s : ${products}\""
                        + " th:attr=\"data-index=${s.index},data-count=${s.count},data-even=${s.even},data-odd=${s.odd}\"></p>");
        compile();
        String html =
                render(
                        "a",
                        new Model()
                                .addAttribute(
                                        "products",
                                        Arrays.asList(product("a", 1), product("b", 2))));
        assertTrue(
                html,
                html.contains(
                        "data-index=\"0\" data-count=\"1\" data-even=\"false\" data-odd=\"true\""));
        assertTrue(
                html,
                html.contains(
                        "data-index=\"1\" data-count=\"2\" data-even=\"true\" data-odd=\"false\""));
    }

    @Test
    public void redirectsNeedNoTemplatesAndVaryByHtmxRequestKind() throws Exception {
        setup();
        HttpServer.Handler handler =
                controller(
                        "package sample; import com.codename1.backend.annotations.*; @Controller"
                            + " public class Pages { @GetMapping(\"/old\") public String redirect()"
                            + " {return \"redirect:/new\";} }");
        for (boolean hx : Arrays.asList(false, true)) {
            Object response = handler.handle(request("GET", "/old", null, hx));
            assertEquals(hx ? 200 : 303, field(response, "status"));
            Map<?, ?> headers = (Map<?, ?>) field(response, "extraHeaders");
            assertEquals("/new", headers.get(hx ? "HX-Redirect" : "Location"));
            assertEquals("HX-Request, HX-History-Restore-Request", headers.get("Vary"));
        }
        Object history = handler.handle(request("GET", "/old", null, true, true));
        assertEquals(303, field(history, "status"));
        assertEquals(
                "HX-Request, HX-History-Restore-Request",
                ((Map<?, ?>) field(history, "extraHeaders")).get("Vary"));
    }

    @Test
    public void staticAssetRootCannotBeASymlink() throws Exception {
        setup();
        File root = new File(project, "src/main/resources/static");
        assertTrue(root.getParentFile().mkdirs());
        File outside = tmp.newFolder();
        Files.write(
                new File(outside, "secret.txt").toPath(),
                "private".getBytes(StandardCharsets.UTF_8));
        try {
            Files.createSymbolicLink(root.toPath(), outside.toPath());
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException unavailable) {
            org.junit.Assume.assumeNoException(unavailable);
        }
        try {
            MvcAssets.sources(project);
            fail("A symlinked public root must not embed files outside the project");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("symlink"));
        }
    }

    @Test
    public void colonsInPathsQueriesAndFragmentsAreNotSchemes() throws Exception {
        setup();
        template("a", "<a th:href=\"@{/events/12:30}\">event</a>");
        compile();
        assertTrue(render("a", new Model()).contains("href=\"/events/12:30\""));
        for (String url :
                Arrays.asList("/objects/urn:123", "relative/path:part", "?time=12:30", "#urn:123"))
            Html.safeUrl(url);
        for (String url :
                Arrays.asList("javascript:alert(1)", "data:text/html,x", "ftp://example.test/")) {
            try {
                Html.safeUrl(url);
                fail(url);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("Unsafe URL"));
            }
        }
    }

    @Test
    public void setterBindingPreservesJavaBeansAcronyms() throws Exception {
        setup();
        fixtureSources(
                Collections.singletonMap(
                        "sample.Form",
                        "package sample; public class Form {private String URL, name; public void"
                            + " setURL(String value){URL=value;} public String getURL(){return"
                            + " URL;}public void setName(String value){name=value;} public String"
                            + " getName(){return name;} }"));
        template(
                "a",
                "<!-- cn1:model form sample.Form --><form th:object=\"${form}\"><input"
                    + " th:field=\"*{URL}\"><input th:field=\"*{name}\"></form>");
        HttpServer.Handler handler =
                controller(
                        "package sample; import com.codename1.backend.annotations.*; @Controller"
                            + " public class Pages { @PostMapping(\"/save\") public String"
                            + " save(@ModelAttribute(\"form\") Form form) {return \"a\";} }");
        String html =
                body(
                        handler.handle(
                                request(
                                        "POST",
                                        "/save",
                                        "URL=https%3A%2F%2Fexample.test&name=Alice",
                                        false)));
        assertTrue(html, html.contains("value=\"https://example.test\""));
        assertTrue(html, html.contains("value=\"Alice\""));
    }

    @Test
    public void propertiesResolveInheritedInterfacesAndDefaultGetters() throws Exception {
        setup();
        Map<String, String> fixtures = new LinkedHashMap<String, String>();
        fixtures.put(
                "sample.Named",
                "package sample; public interface Named {default String getName(){return"
                    + " \"inherited\";}} ");
        fixtures.put("sample.Left", "package sample; public interface Left extends Named {} ");
        fixtures.put("sample.Right", "package sample; public interface Right extends Named {} ");
        fixtures.put(
                "sample.Child", "package sample; public interface Child extends Left,Right {} ");
        fixtures.put(
                "sample.DefaultProduct",
                "package sample; public class DefaultProduct implements Child {} ");
        fixtures.put(
                "sample.BaseProduct",
                "package sample; public class BaseProduct {public String getName(){return \"class"
                    + " getter\";}} ");
        fixtures.put(
                "sample.OverrideProduct",
                "package sample; public class OverrideProduct extends BaseProduct implements Child"
                    + " {} ");
        fixtureSources(fixtures);
        template(
                "a",
                "<!-- cn1:model typed sample.Child --><!-- cn1:model concrete sample.DefaultProduct"
                    + " --><!-- cn1:model overridden sample.OverrideProduct --><b"
                    + " th:text=\"${typed.name}\"></b><i th:text=\"${concrete.name}\"></i><em"
                    + " th:text=\"${overridden.name}\"></em>");
        compile();
        Object product = loader.loadClass("sample.DefaultProduct").newInstance();
        String html =
                render(
                        "a",
                        new Model()
                                .addAttribute("typed", product)
                                .addAttribute("concrete", product)
                                .addAttribute(
                                        "overridden",
                                        loader.loadClass("sample.OverrideProduct").newInstance()));
        assertTrue(html, html.contains("<b>inherited</b><i>inherited</i><em>class getter</em>"));
    }

    @Test
    public void svgLinksRejectUnsafeSchemes() throws Exception {
        setup();
        template(
                "svg",
                "<!-- cn1:model url java.lang.String --><svg><a"
                    + " th:attr=\"XLINK:HREF=${url}\">Open</a></svg>");
        compile();
        for (String url :
                Arrays.asList(
                        "javascript:alert(1)",
                        "JaVaScRiPt:alert(1)",
                        "data:text/html,test",
                        "java\nscript:alert(1)")) {
            try {
                render("svg", new Model().addAttribute("url", url));
                fail(url);
            } catch (IllegalArgumentException expected) {
            }
        }
        assertTrue(
                render("svg", new Model().addAttribute("url", "/products#list"))
                        .contains("xlink:href=\"/products#list\""));
    }

    @Test
    public void templateRootCannotBeASymlink() throws Exception {
        setup();
        File root = new File(project, "src/main/resources/templates");
        assertTrue(root.getParentFile().mkdirs());
        File outside = tmp.newFolder();
        Files.write(
                new File(outside, "secret.html").toPath(),
                "private".getBytes(StandardCharsets.UTF_8));
        try {
            Files.createSymbolicLink(root.toPath(), outside.toPath());
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException unavailable) {
            org.junit.Assume.assumeNoException(unavailable);
        }
        try {
            new MvcTemplates(context).sources();
            fail("Template root symlink accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("symlink"));
        }
    }

    @Test
    public void urlParametersPrecedeFragments() throws Exception {
        setup();
        template(
                "urls",
                "<!-- cn1:model page java.lang.Integer --><a"
                    + " th:href=\"@{/products#list(page=${page})}\">a</a><a"
                    + " th:href=\"@{/products?sort=name#list(page=${page})}\">b</a><a"
                    + " th:href=\"@{/products/{id}#{section}(id=${page},section='a"
                    + " b',q='x&y')}\">c</a><a"
                    + " th:href=\"@{/products#list?ignored(page=${page})}\">d</a><a"
                    + " th:href=\"@{/products#list}\">e</a>");
        compile();
        org.jsoup.select.Elements links =
                org.jsoup.Jsoup.parse(render("urls", new Model().addAttribute("page", 2)))
                        .select("a");
        assertEquals("/products?page=2#list", links.get(0).attr("href"));
        assertEquals("/products?sort=name&page=2#list", links.get(1).attr("href"));
        assertEquals("/products/2?q=x%26y#a%20b", links.get(2).attr("href"));
        assertEquals("/products?page=2#list?ignored", links.get(3).attr("href"));
        assertEquals("/products#list", links.get(4).attr("href"));
    }

    @Test
    public void mixedCaseAssetExtensionsHaveBrowserMimeTypes() throws Exception {
        setup();
        File root = new File(project, "src/main/resources/static");
        assertTrue(root.mkdirs());
        String[] names = {"App.JS", "site.CsS", "icon.SVG"};
        String[] types = {
            "text/javascript; charset=utf-8", "text/css; charset=utf-8", "image/svg+xml"
        };
        for (String name : names)
            Files.write(new File(root, name).toPath(), "asset".getBytes(StandardCharsets.UTF_8));
        JavaSourceCompiler.compile(MvcAssets.sources(project), classes, classpath());
        loader =
                new URLClassLoader(
                        new URL[] {classes.toURI().toURL()}, getClass().getClassLoader());
        HttpServer.Handler assets =
                (HttpServer.Handler)
                        loader.loadClass("com.codename1.generated.mvc.Assets").newInstance();
        for (int i = 0; i < names.length; i++) {
            Object response = assets.handle(request("GET", "/static/" + names[i], null, false));
            assertEquals(types[i], field(response, "contentType"));
            assertEquals(
                    "nosniff",
                    ((Map<?, ?>) field(response, "extraHeaders")).get("X-Content-Type-Options"));
            assertEquals("asset", body(response));
        }
        assertNull(assets.handle(request("GET", "/static/app.js", null, false)));
    }

    @Test
    public void submitMethodOverridesIncludeCsrfTokens() throws Exception {
        setup();
        String declaration = "<!-- cn1:model method java.lang.String -->";
        template(
                "forms",
                declaration
                        + "<form id=\"static\"><button"
                        + " formmethod=\"post\">Save</button></form><form id=\"input\""
                        + " method=\"get\"><input type=\"submit\" formmethod=\"POST\"></form><form"
                        + " id=\"dynamic\"><button"
                        + " th:attr=\"formmethod=${method}\">Save</button></form><form"
                        + " id=\"fragment\"><div th:replace=\"~{control ::"
                        + " save}\"></div></form><form id=\"external\"></form><button"
                        + " form=\"external\" formmethod=\"post\">Save</button><form"
                        + " id=\"get\"><button formmethod=\"get\">Search</button></form>");
        template(
                "control",
                declaration
                        + "<button th:fragment=\"save\""
                        + " th:attr=\"formmethod=${method}\">Save</button>");
        compile();
        Model model =
                new Model()
                        .addAttribute("method", "post")
                        .addAttribute(
                                "_csrf",
                                new com.codename1.backend.security.CsrfToken() {
                                    public String getToken() {
                                        return "token<&";
                                    }

                                    public String getHeaderName() {
                                        return "X-CSRF-TOKEN";
                                    }

                                    public String getParameterName() {
                                        return "_csrf";
                                    }
                                });
        org.jsoup.nodes.Document html = org.jsoup.Jsoup.parse(render("forms", model));
        for (String id : Arrays.asList("static", "input", "dynamic", "fragment")) {
            org.jsoup.select.Elements tokens = html.select("form#" + id + " input[name=_csrf]");
            assertEquals(id, 1, tokens.size());
            assertEquals("token<&", tokens.first().val());
        }
        assertEquals("token<&", html.select("input[name=_csrf][form=external]").val());
        assertTrue(html.select("form#get input[name=_csrf]").isEmpty());
        model.addAttribute("method", "get");
        html = org.jsoup.Jsoup.parse(render("forms", model));
        assertTrue(
                html.select("form#dynamic input[name=_csrf], form#fragment input[name=_csrf]")
                        .isEmpty());
        assertFalse(
                render("forms", new Model().addAttribute("method", "post"))
                        .contains("name=\"_csrf\""));
    }

    @Test
    public void implicitOptionValuesFollowRenderedText() throws Exception {
        setup();
        template(
                "options",
                DECL
                        + "<!-- cn1:model label java.lang.String -->"
                        + "<form th:object=\"${product}\"><select th:field=\"*{name}\">"
                        + "<option>Other</option><option th:text=\"${label}\">Prototype</option>"
                        + "<option value=\"explicit\" th:text=\"${label}\">Prototype</option>"
                        + "</select></form>");
        compile();
        Model model =
                new Model()
                        .addAttribute("product", product("A & B", 0))
                        .addAttribute("label", " \tA  &\n B\r ");
        org.jsoup.select.Elements options =
                org.jsoup.Jsoup.parse(render("options", model)).select("option");
        assertFalse(options.get(0).hasAttr("selected"));
        assertTrue(options.get(1).hasAttr("selected"));
        assertEquals("A & B", options.get(1).text());
        assertFalse(options.get(2).hasAttr("selected"));
        BindingResult errors = new BindingResult();
        errors.submitted("name", "explicit");
        model.addAttribute("BindingResult.product", errors);
        options = org.jsoup.Jsoup.parse(render("options", model)).select("option");
        assertFalse(options.get(1).hasAttr("selected"));
        assertTrue(options.get(2).hasAttr("selected"));
    }

    @Test
    public void omittedPrimitiveFieldsKeepDefaultsAndMarkersStillClearCheckboxes()
            throws Exception {
        setup();
        fixtureSources(
                Collections.singletonMap(
                        "sample.DefaultForm",
                        "package sample; public class DefaultForm { public byte b=1; public short"
                            + " s=2; public int n=3; public long l=4; public float f=5; public"
                            + " double d=6; public char c='Q'; public boolean active=true; private"
                            + " int count=7; public int calls; public void setCount(int"
                            + " v){count=v;calls++;} public String summary(){return"
                            + " b+\"|\"+s+\"|\"+n+\"|\"+l+\"|\"+f+\"|\"+d+\"|\"+c+\"|\"+active+\"|\"+count+\"|\"+calls;}"
                            + " }"));
        HttpServer.Handler handler =
                controller(
                        "package sample; import com.codename1.backend.annotations.*; import"
                            + " com.codename1.backend.mvc.*; @Controller public class Pages {"
                            + " @PostMapping(\"/bind\") @ResponseBody public String"
                            + " bind(@ModelAttribute(\"form\") DefaultForm form, BindingResult"
                            + " errors) { return"
                            + " form.summary()+\"|\"+errors.hasErrors()+\"|\"+errors.fieldValue(\"n\","
                            + " form.n); } @PostMapping(\"/strict\") @ResponseBody public String"
                            + " strict(@ModelAttribute(\"form\") DefaultForm form) {return"
                            + " form.summary();} }");
        String defaults = "1|2|3|4|5.0|6.0|Q|true|7|0";
        assertEquals(
                defaults + "|false|3", body(handler.handle(request("POST", "/bind", "", false))));
        Object strict = handler.handle(request("POST", "/strict", "", false));
        assertEquals(200, field(strict, "status"));
        assertEquals(defaults, body(strict));
        assertEquals(
                "1|2|3|4|5.0|6.0|Q|false|7|0|false|3",
                body(handler.handle(request("POST", "/bind", "_active=on", false))));
        assertEquals(
                "1|2|8|4|5.0|6.0|Z|true|9|1|false|8",
                body(handler.handle(request("POST", "/bind", "n=8&c=Z&count=9", false))));
        for (String invalid : Arrays.asList("n=", "n=bad", "c=", "c=long")) {
            assertEquals(
                    400,
                    field(handler.handle(request("POST", "/strict", invalid, false)), "status"));
            assertEquals(
                    "true",
                    body(handler.handle(request("POST", "/bind", invalid, false)))
                            .split("\\|", -1)[10]);
        }
    }

    @Test
    public void rawTextElementsRejectDynamicFragmentInsertionAndErrors() throws Exception {
        for (String tag : Arrays.asList("script", "style")) {
            for (String directive : Arrays.asList("insert", "errors")) {
                setup();
                template(
                        "parts",
                        "<!-- cn1:model payload java.lang.String --><th:block th:fragment=\"code\""
                            + " th:text=\"${payload}\"></th:block>");
                template(
                        "unsafe",
                        DECL
                                + "<form th:object=\"${product}\"><"
                                + tag
                                + " th:"
                                + directive
                                + "=\""
                                + (directive.equals("insert") ? "~{parts :: code}" : "*{name}")
                                + "\"></"
                                + tag
                                + "></form>");
                try {
                    new MvcTemplates(context).sources();
                    fail(tag + " th:" + directive);
                } catch (IllegalArgumentException expected) {
                    assertTrue(
                            expected.getMessage(), expected.getMessage().contains("script/style"));
                }
            }
        }
        setup();
        template("safe", "<script src=\"/app.js\"></script><style>p {color: red}</style>");
        compile();
        assertTrue(render("safe", new Model()).contains("p {color: red}"));
    }

    @Test
    public void omittedReferenceFieldsPreserveDefaultsButExplicitEmptyValuesBind()
            throws Exception {
        setup();
        fixtureSources(
                Collections.singletonMap(
                        "sample.BoxedForm",
                        "package sample; public class BoxedForm { public Byte b=1; public Short"
                            + " s=2; public Integer n=3; public Long l=4L; public Float f=5F;"
                            + " public Double d=6D; public Character c='Q'; public Boolean"
                            + " active=true; public String name=\"default\"; private Integer"
                            + " count=7; public int calls; public void setCount(Integer"
                            + " v){count=v;calls++;} public String summary(){return"
                            + " b+\"|\"+s+\"|\"+n+\"|\"+l+\"|\"+f+\"|\"+d+\"|\"+c+\"|\"+active+\"|\"+name+\"|\"+count+\"|\"+calls;}"
                            + " }"));
        HttpServer.Handler handler =
                controller(
                        "package sample; import com.codename1.backend.annotations.*; import"
                            + " com.codename1.backend.mvc.*; @Controller public class Pages {"
                            + " @PostMapping(\"/bind\") @ResponseBody public String"
                            + " bind(@ModelAttribute(\"form\") BoxedForm form, BindingResult"
                            + " errors) { return"
                            + " form.summary()+\"|\"+errors.hasErrors()+\"|\"+errors.fieldValue(\"n\",form.n);"
                            + " } }");
        assertEquals(
                "1|2|3|4|5.0|6.0|Q|true|default|7|0|false|3",
                body(handler.handle(request("POST", "/bind", "", false))));
        assertEquals(
                "1|2|3|4|5.0|6.0|Q|false|default|7|0|false|3",
                body(handler.handle(request("POST", "/bind", "_active=on", false))));
        assertEquals(
                "1|2|null|4|5.0|6.0|null|true||null|1|false|",
                body(handler.handle(request("POST", "/bind", "n=&c=&name=&count=", false))));
    }

    @Test
    public void htmxDescendantsAndAssociatedControlsCarryCsrf() throws Exception {
        setup();
        template(
                "hx",
                "<!-- cn1:model target java.lang.String --><form id=\"post\"><button"
                    + " hx-post=\"/save\">Save</button></form><form id=\"put\"><a"
                    + " hx-put=\"/save\">Save</a></form><form id=\"patch\"><div"
                    + " th:replace=\"~{hxparts :: save}\"></div></form><form id=\"delete\"><input"
                    + " th:attr=\"hx-delete=${target}\"></form><form id=\"external\"></form><button"
                    + " form=\"external\" hx-post=\"/save\">Save</button><form id=\"get\"><button"
                    + " hx-get=\"/search\">Search</button></form>");
        template("hxparts", "<button th:fragment=\"save\" hx-patch=\"/save\">Save</button>");
        compile();
        Model model =
                new Model()
                        .addAttribute("target", "/save")
                        .addAttribute(
                                "_csrf",
                                new com.codename1.backend.security.DefaultCsrfToken(
                                        "X-CSRF-TOKEN", "_csrf", "token<&"));
        org.jsoup.nodes.Document html = org.jsoup.Jsoup.parse(render("hx", model));
        for (String id : Arrays.asList("post", "put", "patch", "delete")) {
            assertEquals(id, "token<&", html.select("form#" + id + " input[name=_csrf]").val());
        }
        assertEquals("token<&", html.select("input[name=_csrf][form=external]").val());
        assertTrue(html.select("form#get input[name=_csrf]").isEmpty());
        model.addAttribute("target", null);
        assertTrue(
                org.jsoup.Jsoup.parse(render("hx", model))
                        .select("form#delete input[name=_csrf]")
                        .isEmpty());
    }

    @Test
    public void urlExpressionsPreserveRepeatedQueryNames() throws Exception {
        setup();
        template(
                "repeat",
                "<a th:href=\"@{/search#list(tag='a b',tag='c&d',page=2)}\">Search</a><a"
                    + " th:href=\"@{/products/{id}/{id}#{section}(id=7,tag='a',section='details',tag='b')}\">Product</a>");
        compile();
        org.jsoup.select.Elements links =
                org.jsoup.Jsoup.parse(render("repeat", new Model())).select("a");
        assertEquals("/search?tag=a%20b&tag=c%26d&page=2#list", links.get(0).attr("href"));
        assertEquals("/products/7/7?tag=a&tag=b#details", links.get(1).attr("href"));
        template("repeat", "<a th:href=\"@{/products/{id}(id=1,id=2)}\">Ambiguous</a>");
        try {
            new MvcTemplates(context).sources();
            fail("Ambiguous path parameter accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("one value"));
        }
    }

    @Test
    public void dynamicInputTypesBindCheckboxRadioAndText() throws Exception {
        setup();
        template(
                "type",
                DECL
                        + "<!-- cn1:model kind java.lang.String --><form"
                        + " th:object=\"${product}\"><input type=\"text\" th:field=\"*{active}\""
                        + " th:attr=\"TYPE=${kind}\"></form>");
        compile();
        Object p = product("test", 1);
        p.getClass().getField("active").setBoolean(p, true);
        Model model = new Model().addAttribute("product", p);
        for (String kind : Arrays.asList("checkbox", "RADIO", "text")) {
            model.addAttribute("kind", kind);
            org.jsoup.nodes.Document html = org.jsoup.Jsoup.parse(render("type", model));
            assertEquals(
                    !kind.equals("text"),
                    html.select("input[name=active]").first().hasAttr("checked"));
            assertEquals(
                    kind.equals("checkbox") ? 1 : 0, html.select("input[name=_active]").size());
            assertEquals("true", html.select("input[name=active]").val());
        }
        p.getClass().getField("active").setBoolean(p, false);
        model.addAttribute("kind", "checkbox");
        org.jsoup.nodes.Document html = org.jsoup.Jsoup.parse(render("type", model));
        assertFalse(html.select("input[name=active]").first().hasAttr("checked"));
        assertEquals("true", html.select("input[name=active]").val());
    }

    @Test
    public void hugeAssetsAreRejectedBeforeAllocation() throws Exception {
        setup();
        File file = new File(project, "src/main/resources/static/huge.bin");
        assertTrue(file.getParentFile().mkdirs());
        try (RandomAccessFile sparse = new RandomAccessFile(file, "rw")) {
            sparse.setLength((long) Integer.MAX_VALUE + 1);
        }
        try {
            MvcAssets.sources(project);
            fail("Oversize asset accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("2 MiB"));
        } catch (OutOfMemoryError allocation) {
            fail("Asset size was not checked before allocation");
        }
    }

    @Test
    public void truthConversionMatchesSupportedThymeleafScalars() throws Exception {
        setup();
        template(
                "truth",
                "<!-- cn1:model value java.lang.String -->"
                        + "<b th:if=\"${value}\">if</b><i th:unless=\"${value}\">unless</i>"
                        + "<input th:disabled=\"${value}\"><em th:text=\"${value and true}\"></em>"
                        + "<strong th:text=\"${not value}\"></strong>");
        compile();
        for (String value : Arrays.asList("false", "NO", " Off ", null)) {
            org.jsoup.nodes.Document html =
                    org.jsoup.Jsoup.parse(
                            render("truth", new Model().addAttribute("value", value)));
            assertTrue(html.select("b").isEmpty());
            assertEquals("unless", html.select("i").text());
            assertFalse(html.select("input").first().hasAttr("disabled"));
            assertEquals("false", html.select("em").text());
            assertEquals("true", html.select("strong").text());
        }
        for (String value : Arrays.asList("true", "yes", "on", "", "0"))
            assertTrue(Html.truth(value));
        for (Object value : Arrays.<Object>asList(false, 0, 0L, 0.0, '\0'))
            assertFalse(Html.truth(value));
        for (Object value : Arrays.<Object>asList(true, 1, -1L, 0.5, 'x'))
            assertTrue(Html.truth(value));
    }

    @Test
    public void objectDataUrlsRejectExecutableSchemes() throws Exception {
        setup();
        template(
                "object",
                "<!-- cn1:model url java.lang.String --><object th:attr=\"DATA=${url}\"></object>");
        compile();
        for (String url :
                Arrays.asList("data:text/html,<script>alert(1)</script>", "JaVaScRiPt:alert(1)")) {
            try {
                render("object", new Model().addAttribute("url", url));
                fail("Accepted executable object data: " + url);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("URL"));
            }
        }
        for (String url :
                Arrays.asList("/static/manual.pdf", "https://example.com/manual.pdf?a=1&b=2")) {
            assertEquals(
                    url,
                    org.jsoup.Jsoup.parse(render("object", new Model().addAttribute("url", url)))
                            .select("object")
                            .attr("data"));
        }
    }

    @Test
    public void genericPropertiesRetainOwnerAndInheritedTypeArguments() throws Exception {
        setup();
        Map<String, String> fixtures = new LinkedHashMap<String, String>();
        fixtures.put(
                "sample.Box",
                "package sample; public class Box<T> { public T value; public T getValue(){return"
                        + " value;} public java.util.List<T> getItems(){return"
                        + " java.util.Collections.singletonList(value);} }");
        fixtures.put("sample.Middle", "package sample; public class Middle<U> extends Box<U> {}");
        fixtures.put(
                "sample.ProductBox",
                "package sample; public class ProductBox extends Middle<Product> { public"
                        + " ProductBox(){value=new Product();value.name=\"generic\";} }");
        fixtures.put("sample.Named", "package sample; public interface Named<T> { T getValue(); }");
        fixtures.put(
                "sample.Child", "package sample; public interface Child<U> extends Named<U> {}");
        fixtures.put(
                "sample.NamedProduct",
                "package sample; public class NamedProduct extends ProductBox implements"
                        + " Child<Product> {}");
        fixtures.put(
                "sample.Fields",
                "package sample; public class Fields<T> { public T value; public T[] values; }");
        fixtureSources(fixtures);
        template(
                "generic",
                "<!-- cn1:model direct sample.Box<sample.Product> --><!-- cn1:model inherited"
                    + " sample.ProductBox --><!-- cn1:model named sample.Child<sample.Product>"
                    + " --><!-- cn1:model fields sample.Fields<sample.Product> --><b"
                    + " th:text=\"${direct.value.name}\"></b><i"
                    + " th:text=\"${inherited.value.name}\"></i><em"
                    + " th:text=\"${named.value.name}\"></em><u th:each=\"item : ${direct.items}\""
                    + " th:text=\"${item.name}\"></u><s th:text=\"${fields.value.name}\"></s><small"
                    + " th:text=\"${fields.values[0].name}\"></small>");
        compile();
        Object box = loader.loadClass("sample.ProductBox").newInstance();
        Object fields = loader.loadClass("sample.Fields").newInstance();
        Object value = box.getClass().getField("value").get(box);
        fields.getClass().getField("value").set(fields, value);
        Object array = Array.newInstance(value.getClass(), 1);
        Array.set(array, 0, value);
        fields.getClass().getField("values").set(fields, array);
        String html =
                render(
                        "generic",
                        new Model()
                                .addAttribute("direct", box)
                                .addAttribute("inherited", box)
                                .addAttribute(
                                        "named",
                                        loader.loadClass("sample.NamedProduct").newInstance())
                                .addAttribute("fields", fields));
        assertTrue(
                html,
                html.contains(
                        "<b>generic</b><i>generic</i><em>generic</em><u>generic</u><s>generic</s><small>generic</small>"));
    }

    @Test
    public void inheritedGenericPropertiesSupportNestedAndMultipleArguments() throws Exception {
        setup();
        Map<String, String> fixtures = new LinkedHashMap<String, String>();
        fixtures.put(
                "sample.Pair",
                "package sample; public class Pair<A,B> { public B field; public B"
                    + " getValue(){return field;} }");
        fixtures.put(
                "sample.Nested",
                "package sample; public class Nested<T> extends Pair<String,java.util.List<T>> {}");
        fixtureSources(fixtures);
        template(
                "nested",
                "<!-- cn1:model nested sample.Nested<sample.Product> --><!-- cn1:model pair"
                    + " sample.Pair<java.util.Map<java.lang.String,sample.Product>,java.util.List<sample.Product>>"
                    + " --><b th:text=\"${nested.value[0].name}\"></b><i"
                    + " th:text=\"${nested.field[0].name}\"></i><em"
                    + " th:text=\"${pair.value[0].name}\"></em>");
        compile();
        Object nested = loader.loadClass("sample.Nested").newInstance();
        nested.getClass()
                .getField("field")
                .set(nested, Collections.singletonList(product("nested", 1)));
        Object pair = loader.loadClass("sample.Pair").newInstance();
        pair.getClass().getField("field").set(pair, Collections.singletonList(product("pair", 1)));
        String html =
                render(
                        "nested",
                        new Model().addAttribute("nested", nested).addAttribute("pair", pair));
        assertTrue(html, html.contains("<b>nested</b><i>nested</i><em>pair</em>"));
    }

    @Test
    public void incrementalBuildRemovesObsoleteAssetClassesFromPackagedOutput() throws Exception {
        setup();
        template("home", "<h1>Home</h1>");
        File assets = new File(project, "src/main/resources/static");
        assertTrue(assets.mkdirs());
        Files.write(new File(assets, "a.txt").toPath(), "keep".getBytes(StandardCharsets.UTF_8));
        File removed = new File(assets, "z.txt");
        Files.write(removed.toPath(), "removed-private-content".getBytes(StandardCharsets.UTF_8));
        String pages =
                "package sample; import com.codename1.backend.annotations.*; @Controller public"
                    + " class Pages { @GetMapping(\"/\") public String home(){return \"home\";} }";
        controller(pages);
        File generated = new File(classes, "com/codename1/generated/mvc");
        assertTrue(new File(generated, "Asset1.class").isFile());
        fixtureSources(
                Collections.singletonMap(
                        "com.codename1.generated.mvc.Asset99",
                        "package com.codename1.generated.mvc; public class Asset99 {}"));
        assertTrue(removed.delete());
        controller(pages);
        assertFalse(
                "Removed asset bytecode survives incremental compilation",
                new File(generated, "Asset1.class").exists());
        assertTrue("User classes must be preserved", new File(generated, "Asset99.class").isFile());
        assertTrue(new File(generated, "Asset0.class").isFile());
        File jar = new File(project, "application.jar");
        try (java.util.jar.JarOutputStream out =
                        new java.util.jar.JarOutputStream(new FileOutputStream(jar));
                java.util.stream.Stream<java.nio.file.Path> files = Files.walk(classes.toPath())) {
            for (java.nio.file.Path file :
                    (Iterable<java.nio.file.Path>) files.filter(Files::isRegularFile)::iterator) {
                out.putNextEntry(
                        new java.util.jar.JarEntry(
                                classes.toPath()
                                        .relativize(file)
                                        .toString()
                                        .replace(File.separatorChar, '/')));
                Files.copy(file, out);
                out.closeEntry();
            }
        }
        try (java.util.jar.JarFile packaged = new java.util.jar.JarFile(jar)) {
            assertNull(packaged.getEntry("com/codename1/generated/mvc/Asset1.class"));
            assertNotNull(packaged.getEntry("com/codename1/generated/mvc/Asset0.class"));
        }
        assertTrue(new File(assets, "a.txt").delete());
        controller(pages);
        assertFalse(new File(generated, "Asset0.class").exists());
    }

    @Test
    public void checkboxMarkersMirrorDisabledAndFormAttributes() throws Exception {
        setup();
        template(
                "markers",
                DECL
                        + "<!-- cn1:model disabled java.lang.Boolean --><!-- cn1:model owner"
                        + " java.lang.String --><!-- cn1:model kind java.lang.String --><form"
                        + " id=\"other\"></form><div th:object=\"${product}\"><input id=\"static\""
                        + " type=\"checkbox\" th:field=\"*{active}\" disabled form=\"other\"><input"
                        + " id=\"dynamic\" th:field=\"*{active}\" disabled form=\"old\""
                        + " th:disabled=\"${disabled}\" th:attr=\"type=${kind},form=${owner}\">"
                        + "</div>");
        compile();
        Model model =
                new Model()
                        .addAttribute("product", product("item", 1))
                        .addAttribute("disabled", true)
                        .addAttribute("owner", "other")
                        .addAttribute("kind", "checkbox");
        for (Object disabled : Arrays.asList(true, false, null)) {
            model.addAttribute("disabled", disabled);
            org.jsoup.nodes.Document html = org.jsoup.Jsoup.parse(render("markers", model));
            for (String id : Arrays.asList("static", "dynamic")) {
                org.jsoup.nodes.Element checkbox = html.getElementById(id);
                org.jsoup.nodes.Element marker = checkbox.previousElementSibling();
                assertEquals("_active", marker.attr("name"));
                assertEquals(checkbox.hasAttr("disabled"), marker.hasAttr("disabled"));
                assertEquals(checkbox.attr("form"), marker.attr("form"));
            }
        }
        model.addAttribute("owner", null);
        org.jsoup.nodes.Element checkbox =
                org.jsoup.Jsoup.parse(render("markers", model)).getElementById("dynamic");
        assertFalse(checkbox.hasAttr("form"));
        assertFalse(checkbox.previousElementSibling().hasAttr("form"));
        model.addAttribute("kind", "radio");
        assertEquals(
                1,
                org.jsoup.Jsoup.parse(render("markers", model))
                        .select("input[name=_active]")
                        .size());
    }

    @Test
    public void dataHtmxAliasesRejectExecutableAttributes() throws Exception {
        setup();
        for (String attribute :
                Arrays.asList(
                        "data-hx-on:click",
                        "DATA-HX-ON::before-request",
                        "data-hx-vals",
                        "Data-Hx-Headers")) {
            template(
                    "alias",
                    "<!-- cn1:model payload java.lang.String --><button th:attr=\""
                            + attribute
                            + "=${payload}\">Go</button>");
            try {
                new MvcTemplates(context).sources();
                fail("Accepted executable alias " + attribute);
            } catch (IllegalArgumentException expected) {
                assertTrue(
                        expected.getMessage(),
                        expected.getMessage().contains("Unsupported dynamic attribute"));
            }
        }
    }

    @Test
    public void dataHtmxAliasesUseUrlValidationAndCsrfHandling() throws Exception {
        setup();
        template(
                "alias",
                "<!-- cn1:model url java.lang.String --><form id=\"static\""
                    + " data-hx-post=\"/save\"></form><form id=\"dynamic\""
                    + " data-hx-post=\"/fallback\" th:attr=\"DATA-HX-POST=${url}\"></form><button"
                    + " id=\"external\" form=\"static\""
                    + " th:attr=\"data-hx-delete=${url}\">Delete</button><a"
                    + " th:attr=\"data-hx-get=${url}\">Get</a>");
        compile();
        Model model =
                new Model()
                        .addAttribute("url", "/save")
                        .addAttribute(
                                "_csrf",
                                new com.codename1.backend.security.CsrfToken() {
                                    public String getToken() {
                                        return "token";
                                    }

                                    public String getHeaderName() {
                                        return "X-CSRF-TOKEN";
                                    }

                                    public String getParameterName() {
                                        return "_csrf";
                                    }
                                });
        org.jsoup.nodes.Document html = org.jsoup.Jsoup.parse(render("alias", model));
        assertEquals(1, html.select("form#static input[name=_csrf]").size());
        assertEquals(1, html.select("form#dynamic input[name=_csrf]").size());
        assertEquals(1, html.select("input[name=_csrf][form=static]").size());
        model.addAttribute("url", null);
        html = org.jsoup.Jsoup.parse(render("alias", model));
        assertTrue(
                html.select("form#dynamic input[name=_csrf], input[name=_csrf][form=static]")
                        .isEmpty());
        assertFalse(html.getElementById("dynamic").hasAttr("data-hx-post"));
        assertFalse(html.getElementById("dynamic").hasAttr("hx-post"));
        for (String url : Arrays.asList("javascript:alert(1)", "data:text/html,bad")) {
            model.addAttribute("url", url);
            try {
                render("alias", model);
                fail("Unsafe alias URL " + url);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("URL"));
            }
        }
    }

    @Test
    public void nestedModelDeclarationsAndPropertiesUseJavaSourceNames() throws Exception {
        setup();
        fixtureSources(
                Collections.singletonMap(
                        "sample.Forms",
                        "package sample; public class Forms { public static class Edit { public"
                            + " String name=\"nested\"; public Edit getSelf(){return this;} public"
                            + " java.util.List<Edit> getItems(){return"
                            + " java.util.Collections.singletonList(this);} } }"));
        template(
                "nested",
                "<!-- cn1:model form sample.Forms.Edit --><!-- cn1:model forms"
                        + " java.util.List<sample.Forms.Edit> --><b th:text=\"${form.name}\"></b><i"
                        + " th:text=\"${form.self.name}\"></i><em th:each=\"item : ${form.items}\""
                        + " th:text=\"${item.name}\"></em><u th:text=\"${forms[0].name}\"></u>");
        compile();
        Object form = loader.loadClass("sample.Forms$Edit").newInstance();
        String html =
                render(
                        "nested",
                        new Model()
                                .addAttribute("form", form)
                                .addAttribute("forms", Collections.singletonList(form)));
        assertTrue(html, html.contains("<b>nested</b><i>nested</i><em>nested</em><u>nested</u>"));
    }

    @Test
    public void nestedClasspathModelsPreserveActualDollarNames() throws Exception {
        setup();
        Map<String, String> fixtures = new LinkedHashMap<String, String>();
        fixtures.put(
                "sample.Forms",
                "package sample; public class Forms { public static class Edit { public String"
                    + " name=\"nested\"; } }");
        fixtures.put(
                "sample.Dollar$Model",
                "package sample; public class Dollar$Model { public String name=\"dollar\"; }");
        fixtureSources(fixtures);
        context =
                new ProcessorContext(
                        classes,
                        tmp.newFolder(),
                        Collections.<String, AnnotatedClass>emptyMap(),
                        new SystemStreamLog(),
                        project,
                        new Properties(),
                        null,
                        Collections.<String>emptyList(),
                        "UTF-8",
                        Arrays.asList(classpath().get(0).getPath(), classes.getPath()));
        template(
                "classpath",
                "<!-- cn1:model form sample.Forms.Edit --><!-- cn1:model binary sample.Forms$Edit"
                    + " --><!-- cn1:model dollar sample.Dollar$Model --><b"
                    + " th:text=\"${form.name}\"></b><i th:text=\"${binary.name}\"></i><em"
                    + " th:text=\"${dollar.name}\"></em>");
        compile();
        Object form = loader.loadClass("sample.Forms$Edit").newInstance();
        String html =
                render(
                        "classpath",
                        new Model()
                                .addAttribute("form", form)
                                .addAttribute("binary", form)
                                .addAttribute(
                                        "dollar",
                                        loader.loadClass("sample.Dollar$Model").newInstance()));
        assertTrue(html, html.contains("<b>nested</b><i>nested</i><em>dollar</em>"));
    }

    @Test
    public void booleanCheckboxesUsePresenceWithCustomValuesAndRetainRadioSemantics()
            throws Exception {
        setup();
        template(
                "checks",
                DECL
                        + "<!-- cn1:model candidate java.lang.String --><!-- cn1:model kind"
                        + " java.lang.String --><div th:object=\"${product}\"><input id=\"check\""
                        + " th:attr=\"type=${kind}\" th:field=\"*{active}\""
                        + " th:value=\"${candidate}\"><input id=\"radio\" type=\"radio\""
                        + " th:field=\"*{active}\" value=\"false\"></div>");
        HttpServer.Handler handler =
                controller(
                        "package sample; import com.codename1.backend.annotations.*; import"
                            + " com.codename1.backend.mvc.*; @Controller public class Pages {"
                            + " @GetMapping(\"/checks\") public String checks(){return \"checks\";}"
                            + " @PostMapping(\"/bind\") @ResponseBody public String"
                            + " bind(@ModelAttribute(\"product\") Product p, BindingResult errors)"
                            + " { return"
                            + " p.active+\"|\"+errors.hasErrors()+\"|\"+errors.fieldValue(\"active\","
                            + " p.active); } }");
        Object product = product("item", 1);
        product.getClass().getField("active").setBoolean(product, true);
        Model model = new Model().addAttribute("product", product).addAttribute("kind", "checkbox");
        for (String candidate : Arrays.asList("yes", "1", "false", "", "custom")) {
            model.addAttribute("candidate", candidate);
            org.jsoup.nodes.Document html = org.jsoup.Jsoup.parse(render("checks", model));
            assertTrue(candidate, html.getElementById("check").hasAttr("checked"));
            assertEquals(candidate, html.getElementById("check").val());
            assertFalse(html.getElementById("radio").hasAttr("checked"));
            String submitted =
                    "_active=on&active=" + java.net.URLEncoder.encode(candidate, "UTF-8");
            assertEquals(
                    "true|false|true",
                    body(handler.handle(request("POST", "/bind", submitted, false))));
        }
        assertEquals(
                "false|false|false",
                body(handler.handle(request("POST", "/bind", "_active=on", false))));
        assertEquals(
                "false|false|false",
                body(handler.handle(request("POST", "/bind", "active=false", false))));
        assertTrue(
                body(handler.handle(request("POST", "/bind", "active=invalid", false)))
                        .contains("|true|"));
        product.getClass().getField("active").setBoolean(product, false);
        model.addAttribute("candidate", "false");
        org.jsoup.nodes.Document html = org.jsoup.Jsoup.parse(render("checks", model));
        assertFalse(html.getElementById("check").hasAttr("checked"));
        assertTrue(html.getElementById("radio").hasAttr("checked"));
    }

    @Test
    public void incrementalBuildRemovesViewsWhenLastViewRouteIsRemoved() throws Exception {
        setup();
        template("private", "<p>deleted private template</p>");
        controller(
                "package sample; import com.codename1.backend.annotations.*; @Controller public"
                    + " class Pages { @GetMapping(\"/\") public String page(){return \"private\";}"
                    + " }");
        File views = new File(classes, "com/codename1/generated/mvc/Views.class");
        assertTrue(views.isFile());
        assertTrue(new File(project, "src/main/resources/templates/private.html").delete());
        controller(
                "package sample; import com.codename1.backend.annotations.*; import"
                        + " com.codename1.backend.HttpServer; @Controller public class Pages {"
                        + " @GetMapping(\"/\") public HttpServer.Response page(){return"
                        + " HttpServer.Response.text(200,\"plain\");} }");
        assertFalse("Deleted template bytecode survives rebuild", views.exists());
        fixtureSources(
                Collections.singletonMap(
                        "com.codename1.generated.mvc.Views",
                        "package com.codename1.generated.mvc; public class Views {}"));
        MvcAssets.removeObsoleteClasses(classes, Collections.<String>emptySet());
        assertTrue("User-written Views must survive", views.isFile());
    }

    @Test
    public void inheritedGenericScalarFormPropertiesBindResolvedTypes() throws Exception {
        setup();
        Map<String, String> fixtures = new LinkedHashMap<String, String>();
        fixtures.put(
                "sample.Base",
                "package sample; public class Base<T> { private T value; public T field; public"
                        + " void setValue(T value){this.value=value;} public T getValue(){return"
                        + " value;} }");
        fixtures.put("sample.Middle", "package sample; public class Middle<U> extends Base<U> {}");
        fixtures.put(
                "sample.IntForm",
                "package sample; public class IntForm extends Middle<Integer> { public"
                        + " IntForm(){field=7;setValue(8);} }");
        fixtureSources(fixtures);
        HttpServer.Handler handler =
                controller(
                        "package sample; import com.codename1.backend.annotations.*; import"
                                + " com.codename1.backend.mvc.*; @Controller public class Pages {"
                                + " @PostMapping(\"/bind\") @ResponseBody public String"
                                + " bind(@ModelAttribute(\"form\") IntForm f, BindingResult"
                                + " errors){return"
                                + " f.getValue()+\"|\"+f.field+\"|\"+errors.hasErrors();} }");
        assertEquals(
                "12|34|false",
                body(handler.handle(request("POST", "/bind", "value=12&field=34", false))));
        assertEquals("8|7|false", body(handler.handle(request("POST", "/bind", "", false))));
        assertEquals(
                "null|null|false",
                body(handler.handle(request("POST", "/bind", "value=&field=", false))));
        assertEquals(
                "8|7|true",
                body(handler.handle(request("POST", "/bind", "value=bad&field=bad", false))));
    }

    @Test
    public void dynamicHtmxExpressionAttributesAreRejectedIncludingAliases() throws Exception {
        setup();
        for (String name : Arrays.asList("hx-vars", "hx-request", "hx-trigger")) {
            for (String prefix : Arrays.asList("", "DATA-")) {
                template(
                        "executable",
                        "<!-- cn1:model payload java.lang.String --><button th:attr=\""
                                + prefix
                                + name
                                + "=${payload}\">Go</button>");
                try {
                    new MvcTemplates(context).sources();
                    fail("Accepted executable attribute " + prefix + name);
                } catch (IllegalArgumentException expected) {
                    assertTrue(
                            expected.getMessage(),
                            expected.getMessage().contains("Unsupported dynamic attribute"));
                }
            }
        }
        template(
                "executable",
                "<button hx-trigger=\"click[ctrlKey]\" hx-request=\"{&quot;timeout&quot;:1000}\""
                        + " hx-vars=\"count:1\">Go</button>");
        compile();
        assertTrue(render("executable", new Model()).contains("click[ctrlKey]"));
    }

    @Test
    public void numericConditionalsRemainNumericInComparisonsAndArithmetic() throws Exception {
        setup();
        template(
                "conditional",
                "<!-- cn1:model flag java.lang.Boolean --><!-- cn1:model whole java.lang.Long"
                    + " --><!-- cn1:model count java.lang.Integer --><b th:text=\"${(flag ? 1 :"
                    + " 2.5) > 0}\"></b><i th:text=\"${(flag ? 1 : 2.5) + 1}\"></i><em"
                    + " th:text=\"${(flag ? whole : count) == whole}\"></em><u th:text=\"${(flag ?"
                    + " whole : count) > 0}\"></u><small th:text=\"${flag ? (1 + 1) :"
                    + " 3}\"></small>");
        compile();
        Model model =
                new Model()
                        .addAttribute("whole", 9007199254740993L)
                        .addAttribute("count", 2)
                        .addAttribute("flag", true);
        String html = render("conditional", model);
        assertTrue(
                html,
                html.contains("<b>true</b><i>2.0</i><em>true</em><u>true</u><small>2</small>"));
        model.addAttribute("flag", false);
        html = render("conditional", model);
        assertTrue(
                html,
                html.contains("<b>true</b><i>3.5</i><em>false</em><u>true</u><small>3</small>"));
    }

    @Test
    public void primitiveIsGettersTakePrecedenceRegardlessOfDeclarationOrder() throws Exception {
        setup();
        Map<String, String> fixtures = new LinkedHashMap<String, String>();
        fixtures.put(
                "sample.GetFirst",
                "package sample; public class GetFirst { public Boolean getActive(){return false;}"
                        + " public boolean isActive(){return true;} }");
        fixtures.put(
                "sample.IsFirst",
                "package sample; public class IsFirst { public boolean isActive(){return true;}"
                        + " public Boolean getActive(){return false;} }");
        fixtures.put(
                "sample.Inherited",
                "package sample; public class Inherited extends IsFirst { public Boolean"
                        + " getActive(){return false;} }");
        fixtures.put(
                "sample.BoxedIs",
                "package sample; public class BoxedIs { public Boolean isActive(){return false;}"
                        + " public boolean getActive(){return true;} }");
        fixtureSources(fixtures);
        template(
                "beans",
                "<!-- cn1:model a sample.GetFirst --><!-- cn1:model b sample.IsFirst --><!--"
                        + " cn1:model c sample.Inherited --><!-- cn1:model d sample.BoxedIs --><b"
                        + " th:text=\"${a.active}\"></b><i th:text=\"${b.active}\"></i><em"
                        + " th:text=\"${c.active}\"></em><u th:text=\"${d.active}\"></u>");
        compile();
        String html =
                render(
                        "beans",
                        new Model()
                                .addAttribute(
                                        "a", loader.loadClass("sample.GetFirst").newInstance())
                                .addAttribute("b", loader.loadClass("sample.IsFirst").newInstance())
                                .addAttribute(
                                        "c", loader.loadClass("sample.Inherited").newInstance())
                                .addAttribute(
                                        "d", loader.loadClass("sample.BoxedIs").newInstance()));
        assertTrue(html, html.contains("<b>true</b><i>true</i><em>true</em><u>true</u>"));
    }

    private static volatile int benchmarkSink;

    @Test
    public void renderingBenchmark() throws Exception {
        org.junit.Assume.assumeTrue(Boolean.getBoolean("cn1.mvc.benchmark"));
        setup();
        template("bench", "<!-- cn1:model name java.lang.String --><p th:text=\"${name}\"></p>");
        Map<String, String> sources = new MvcTemplates(context).sources();
        String common =
                "package sample; import com.codename1.backend.*; import"
                        + " com.codename1.backend.mvc.*; ";
        sources.put(
                "sample.Compiled",
                common
                        + "public class Compiled implements java.util.concurrent.Callable<byte[]> {"
                        + " private final Model model=new Model().addAttribute(\"name\",\"Hello"
                        + " <world> 😀\"); public byte[] call(){return"
                        + " com.codename1.generated.mvc.Views.render(\"bench\",model);} }");
        sources.put(
                "sample.Handwritten",
                common
                        + "public class Handwritten implements"
                        + " java.util.concurrent.Callable<byte[]> { private final Model model=new"
                        + " Model().addAttribute(\"name\",\"Hello <world> 😀\"); private static"
                        + " final byte[]"
                        + " BEFORE=Html.utf8(\"<html><head></head><body><p>\"),AFTER=Html.utf8(\"</p></body></html>\");"
                        + " public byte[] call(){ByteSink out=new ByteSink(1024);Object"
                        + " raw=Html.require(model,\"name\",\"bench\");if(raw!=null && !(raw"
                        + " instanceof String))throw new"
                        + " IllegalStateException();out.put(BEFORE,0,BEFORE.length);Html.text(out,(String)raw);out.put(AFTER,0,AFTER.length);return"
                        + " Html.bytes(out);} }");
        List<File> cp = new ArrayList<File>(classpath());
        cp.add(classes);
        JavaSourceCompiler.compile(sources, classes, cp);
        loader =
                new URLClassLoader(
                        new URL[] {classes.toURI().toURL()}, getClass().getClassLoader());
        Callable<byte[]> compiled =
                (Callable<byte[]>) loader.loadClass("sample.Compiled").newInstance();
        Callable<byte[]> handwritten =
                (Callable<byte[]>) loader.loadClass("sample.Handwritten").newInstance();
        assertArrayEquals(handwritten.call(), compiled.call());
        for (int i = 0; i < 30000; i++) {
            benchmarkSink = compiled.call().length;
            benchmarkSink = handwritten.call().length;
        }
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean)
                        java.lang.management.ManagementFactory.getThreadMXBean();
        if (bean.isThreadAllocatedMemorySupported()) bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().getId();
        int count = 100000;
        StringBuilder rows =
                new StringBuilder(
                        "renderer,trial,ns_per_render,renders_per_second,allocated_bytes_per_render\n");
        for (int trial = 0; trial < 7; trial++)
            for (int order = 0; order < 2; order++) {
                boolean generated = (trial + order) % 2 == 0;
                Callable<byte[]> renderer = generated ? compiled : handwritten;
                long allocation =
                        bean.isThreadAllocatedMemorySupported()
                                ? bean.getThreadAllocatedBytes(thread)
                                : -1;
                long start = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    byte[] bytes = renderer.call();
                    benchmarkSink = bytes.length + bytes[bytes.length - 1];
                }
                long elapsed = System.nanoTime() - start;
                double allocated =
                        allocation < 0
                                ? -1
                                : (bean.getThreadAllocatedBytes(thread) - allocation)
                                        / (double) count;
                rows.append(generated ? "compiled" : "handwritten")
                        .append(',')
                        .append(trial)
                        .append(',')
                        .append(elapsed / (double) count)
                        .append(',')
                        .append(count * 1e9 / elapsed)
                        .append(',')
                        .append(allocated)
                        .append('\n');
            }
        File output = new File("target/mvc-render-benchmark.csv");
        Files.write(output.toPath(), rows.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("MVC render benchmark: " + output.getAbsolutePath());
    }

    static HttpServer.Request request(String method, String target, String body, boolean hx)
            throws Exception {
        return request(method, target, body, hx, false);
    }

    static HttpServer.Request request(
            String method, String target, String body, boolean hx, boolean history)
            throws Exception {
        String head =
                method
                        + " "
                        + target
                        + " HTTP/1.1\r\nContent-Type: application/x-www-form-urlencoded\r\n"
                        + (hx ? "HX-Request: true\r\n" : "")
                        + (history ? "HX-History-Restore-Request: true\r\n" : "")
                        + "\r\n";
        byte[] raw = head.getBytes(StandardCharsets.UTF_8);
        List<Integer> slices = new ArrayList<Integer>();
        int pos = head.indexOf("\r\n") + 2;
        while (head.charAt(pos) != '\r') {
            int colon = head.indexOf(':', pos), end = head.indexOf("\r\n", pos);
            slices.add(pos);
            slices.add(colon - pos);
            slices.add(colon + 2);
            slices.add(end - colon - 2);
            pos = end + 2;
        }
        int[] indices = new int[slices.size()];
        for (int i = 0; i < indices.length; i++) indices[i] = slices.get(i);
        Constructor<?> ctor =
                HttpServer.Request.class.getDeclaredConstructor(
                        String.class,
                        String.class,
                        String.class,
                        byte[].class,
                        int[].class,
                        int.class,
                        String.class,
                        int.class,
                        int.class);
        ctor.setAccessible(true);
        return (HttpServer.Request)
                ctor.newInstance(
                        method,
                        target,
                        "HTTP/1.1",
                        raw,
                        indices,
                        indices.length / 4,
                        body,
                        method.length() + 1,
                        target.getBytes(StandardCharsets.UTF_8).length);
    }

    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static String body(Object response) throws Exception {
        return new String((byte[]) field(response, "body"), StandardCharsets.UTF_8);
    }
}

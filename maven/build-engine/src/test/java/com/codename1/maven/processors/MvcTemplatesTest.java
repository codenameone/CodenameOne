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

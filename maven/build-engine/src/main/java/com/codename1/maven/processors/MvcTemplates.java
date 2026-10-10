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

import com.codename1.maven.annotations.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;

/** Build-time HTML compiler shared by the Maven and Gradle adapters. */
final class MvcTemplates {
    private static final String PACKAGE = "com.codename1.generated.mvc";
    private static final String HTML = MvcExpression.HTML;
    private static final Set<String> BOOLEAN =
            new HashSet<String>(
                    Arrays.asList(
                            "checked",
                            "selected",
                            "disabled",
                            "readonly",
                            "multiple",
                            "required",
                            "autofocus",
                            "hidden"));
    private static final Set<String> ATTRS =
            new HashSet<String>(
                    Arrays.asList(
                            "href",
                            "src",
                            "action",
                            "value",
                            "id",
                            "name",
                            "class",
                            "title",
                            "alt",
                            "placeholder",
                            "method",
                            "for"));
    private static final Set<String> DIRECTIVES =
            new HashSet<String>(
                    Arrays.asList(
                            "text",
                            "if",
                            "unless",
                            "each",
                            "object",
                            "field",
                            "errors",
                            "attr",
                            "fragment",
                            "insert",
                            "replace",
                            "classappend"));
    private final ProcessorContext ctx;
    private final Map<String, Template> templates = new TreeMap<String, Template>();
    private final Map<String, String> methods = new LinkedHashMap<String, String>();
    private final Set<String> compiling = new HashSet<String>();
    private final List<byte[]> constants = new ArrayList<byte[]>();
    private int sequence;

    private static final class Template {
        String name;
        Document document;
        Map<String, String> models = new LinkedHashMap<String, String>();
        Map<String, Element> fragments = new LinkedHashMap<String, Element>();
    }

    MvcTemplates(ProcessorContext ctx) {
        this.ctx = ctx;
    }

    static File projectDirectory(ProcessorContext ctx) {
        File output = ctx.getOutputClassDir();
        // Maven backend modules have no mobile codenameone_settings.properties.
        if (output != null
                && output.getParentFile() != null
                && "target".equals(output.getParentFile().getName()))
            return output.getParentFile().getParentFile();
        return ctx.getProjectDir();
    }

    Map<String, String> sources() {
        File project = projectDirectory(ctx);
        if (project == null)
            throw new IllegalArgumentException("MVC templates require a project directory");
        File root = new File(project, "src/main/resources/templates");
        if (Files.isSymbolicLink(root.toPath()))
            throw new IllegalArgumentException("MVC templates root cannot be a symlink");
        load(root, root);
        for (Template t : templates.values()) {
            compile(t.name);
            for (String fragment : t.fragments.keySet()) compile(t.name + " :: " + fragment);
        }
        StringBuilder source =
                new StringBuilder(
                        "package "
                                + PACKAGE
                                + ";\n"
                                + "import com.codename1.backend.*;\n"
                                + "import com.codename1.backend.mvc.*;\n"
                                + "@com.codename1.backend.annotations.Generated\n"
                                + "public final class Views {\n"
                                + "private Views() {}\n");
        // Split constants to avoid the JVM's UTF-8 constant and method-size limits.
        for (int i = 0; i < constants.size(); i++)
            source.append("private static final byte[] C")
                    .append(i)
                    .append(" = Html.utf8(")
                    .append(q(new String(constants.get(i), StandardCharsets.UTF_8)))
                    .append(");\n");
        source.append(
                        "public static HttpServer.Response respond(HttpServer.Request request,"
                                + " String view, Model model, int status) {\n")
                .append("if(view==null) return HttpServer.Response.text(404, \"\");\n")
                .append(
                        "if(view.startsWith(\"redirect:\")) return Html.redirect(request,"
                                + " view.substring(9));\n")
                .append(
                        "return new HttpServer.Response(status, \"text/html; charset=utf-8\","
                                + " render(view, model)).header(\"Vary\", \"HX-Request,"
                                + " HX-History-Restore-Request\");\n"
                                + "}\n")
                .append(
                        "public static byte[] render(String view, Model model) { ByteSink out = new"
                                + " ByteSink(1024);\n");
        for (String key : methods.keySet())
            source.append("if (")
                    .append(q(key))
                    .append(".equals(view)) { ")
                    .append(methodName(key))
                    .append("(out, model); return Html.bytes(out); }\n");
        source.append(
                "throw new IllegalArgumentException(\"Unknown compiled view: \" + view); }\n");
        for (String method : methods.values()) source.append(method);
        source.append("}\n");
        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put(PACKAGE + ".Views", source.toString());
        return result;
    }

    private void load(File root, File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File file : files) {
            if (Files.isSymbolicLink(file.toPath()))
                throw new IllegalArgumentException("Template symlinks are not supported: " + file);
            if (file.isDirectory()) {
                load(root, file);
                continue;
            }
            if (!file.getName().endsWith(".html")) continue;
            Template t = new Template();
            t.name =
                    root.toPath()
                            .relativize(file.toPath())
                            .toString()
                            .replace(File.separatorChar, '/');
            t.name = t.name.substring(0, t.name.length() - 5);
            try {
                String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                t.document = Jsoup.parse(text, "", Parser.htmlParser().setTrackPosition(true));
                t.document.outputSettings().prettyPrint(false);
                Matcher declarations =
                        Pattern.compile(
                                        "<!--\\s*cn1:model\\s+([A-Za-z][A-Za-z0-9_]*)\\s+(.+?)\\s*-->")
                                .matcher(text);
                t.models.put("_csrf", "com.codename1.backend.security.CsrfToken");
                while (declarations.find()) {
                    String name = declarations.group(1), type = declarations.group(2).trim();
                    checkType(type);
                    if (t.models.put(name, type) != null)
                        throw new IllegalArgumentException("Duplicate/reserved model name " + name);
                }
                validate(t, t.document);
            } catch (IOException error) {
                throw new IllegalArgumentException("Cannot read template " + file, error);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(file + ": " + error.getMessage(), error);
            }
            templates.put(t.name, t);
        }
    }

    private void checkType(String type) {
        // Only named reference types and generic arguments: declarations are not Java snippets.
        if (!type.matches("[A-Za-z_$][A-Za-z0-9_$.]*(\\s*<.*>)?(\\[\\])*")
                || type.matches(".*[^A-Za-z0-9_$.,<>\\[\\] ].*"))
            throw new IllegalArgumentException("Invalid model type " + type);
        int depth = 0;
        for (char c : type.toCharArray()) {
            if (c == '<') depth++;
            if (c == '>') depth--;
            if (depth < 0) throw new IllegalArgumentException("Invalid model type " + type);
        }
        if (depth != 0) throw new IllegalArgumentException("Invalid model type " + type);
        for (String word : type.split("[^A-Za-z0-9_$.]+")) {
            if (word.isEmpty()) continue;
            if (!Arrays.asList(
                                    "java.lang.String",
                                    "java.lang.Boolean",
                                    "java.lang.Byte",
                                    "java.lang.Short",
                                    "java.lang.Integer",
                                    "java.lang.Long",
                                    "java.lang.Float",
                                    "java.lang.Double",
                                    "java.lang.Character",
                                    "java.util.List",
                                    "java.util.Set",
                                    "java.util.Collection",
                                    "java.util.Map")
                            .contains(word)
                    && RestControllerAnnotationProcessor.resolveClass(ctx, word.replace('.', '/'))
                            == null)
                throw new IllegalArgumentException("Unknown model type " + word);
        }
    }

    private void validate(Template t, Node node) {
        if (node instanceof Element) {
            Element e = (Element) node;
            if ((e.normalName().equals("script") || e.normalName().equals("style"))
                    && (e.hasAttr("th:text") || e.hasAttr("th:insert") || e.hasAttr("th:errors")))
                throw problem(t, e, "Dynamic script/style content is not supported");
            for (Attribute a : e.attributes())
                if (a.getKey().startsWith("th:")) {
                    String n = a.getKey().substring(3);
                    if (!DIRECTIVES.contains(n) && !ATTRS.contains(n) && !BOOLEAN.contains(n))
                        throw problem(t, e, "Unsupported directive " + a.getKey());
                }
            if (e.hasAttr("th:fragment")) {
                String name = e.attr("th:fragment");
                if (!name.matches("[A-Za-z][A-Za-z0-9_-]*"))
                    throw problem(
                            t,
                            e,
                            "Fragment names must be simple identifiers (parameters are not"
                                    + " supported)");
                if (t.fragments.put(name, e) != null)
                    throw problem(t, e, "Duplicate fragment " + name);
            }
        }
        for (Node child : node.childNodes()) validate(t, child);
    }

    private String methodName(String key) {
        return "view" + new ArrayList<String>(methods.keySet()).indexOf(key);
    }

    private void compile(String key) {
        if (compiling.contains(key))
            throw new IllegalArgumentException("Recursive fragment inclusion: " + key);
        if (methods.containsKey(key)) return;
        int sep = key.indexOf(" :: ");
        String name = sep < 0 ? key : key.substring(0, sep);
        Template t = templates.get(name);
        if (t == null) throw new IllegalArgumentException("Unknown template " + name);
        Node root = sep < 0 ? t.document : t.fragments.get(key.substring(sep + 4));
        if (root == null) throw new IllegalArgumentException("Unknown fragment " + key);
        methods.put(key, "");
        compiling.add(key);
        Map<String, MvcExpression.Value> env = new LinkedHashMap<String, MvcExpression.Value>();
        StringBuilder prelude = new StringBuilder(), body = new StringBuilder();
        for (Map.Entry<String, String> entry : t.models.entrySet()) {
            String variable = "model" + sequence++;
            env.put(entry.getKey(), new MvcExpression.Value(variable, entry.getValue()));
        }
        render(t, root, env, null, null, body);
        // A fragment only requires the model names it actually reads.
        for (Map.Entry<String, String> entry : t.models.entrySet()) {
            String variable = env.get(entry.getKey()).code;
            if (!Pattern.compile("\\b" + variable + "\\b").matcher(body).find()) continue;
            String type = entry.getValue(), raw = MvcExpression.raw(type);
            prelude.append("Object raw_")
                    .append(variable)
                    .append(" = Html.require(model, ")
                    .append(q(entry.getKey()))
                    .append(", ")
                    .append(q(key))
                    .append(");\n")
                    .append("if (raw_")
                    .append(variable)
                    .append(" != null && !(raw_")
                    .append(variable)
                    .append(" instanceof ")
                    .append(raw)
                    .append(")) throw new IllegalStateException(")
                    .append(
                            q(
                                    "Wrong model type for "
                                            + entry.getKey()
                                            + " in "
                                            + key
                                            + ": expected "
                                            + type))
                    .append(");\n")
                    .append(type)
                    .append(' ')
                    .append(variable)
                    .append(" = (")
                    .append(type)
                    .append(")raw_")
                    .append(variable)
                    .append(";\n");
        }
        methods.put(
                key,
                "private static void "
                        + methodName(key)
                        + "(ByteSink out, Model model) {\n"
                        + prelude
                        + body
                        + "}\n");
        compiling.remove(key);
    }

    private void render(
            Template t,
            Node node,
            Map<String, MvcExpression.Value> env,
            String form,
            String select,
            StringBuilder out) {
        try {
            renderNode(t, node, env, form, select, out);
        } catch (IllegalArgumentException error) {
            throw problem(t, node, error.getMessage());
        }
    }

    private void renderNode(
            Template t,
            Node node,
            Map<String, MvcExpression.Value> original,
            String form,
            String select,
            StringBuilder out) {
        if (node instanceof Comment) {
            if (!((Comment) node).getData().trim().startsWith("cn1:model"))
                literal(out, node.outerHtml());
            return;
        }
        if (!(node instanceof Element)) {
            literal(out, node.outerHtml());
            return;
        }
        Element e = (Element) node;
        Map<String, MvcExpression.Value> env =
                new LinkedHashMap<String, MvcExpression.Value>(original);
        int braces = 0;
        if (e.hasAttr("th:each")) {
            String each = e.attr("th:each");
            int colon = each.indexOf(':');
            if (colon < 0) throw new IllegalArgumentException("Expected th:each='item : ${items}'");
            String[] names = each.substring(0, colon).trim().split("\\s*,\\s*");
            if (names.length > 2) throw new IllegalArgumentException("Invalid iteration variables");
            for (String name : names)
                if (!name.matches("[A-Za-z][A-Za-z0-9_]*"))
                    throw new IllegalArgumentException("Invalid iteration variable");
            MvcExpression.Value list = expression(each.substring(colon + 1), env, form);
            String itemType = MvcExpression.element(list.type),
                    listVar = "list" + sequence++,
                    itemVar = "item" + sequence++;
            out.append(list.type)
                    .append(' ')
                    .append(listVar)
                    .append(" = ")
                    .append(list.code)
                    .append(";\nif(")
                    .append(listVar)
                    .append(" != null) {\n");
            braces++;
            String index = "index" + sequence++;
            out.append("int ").append(index).append(" = 0;\n");
            String rawItem = "rawItem" + sequence++;
            out.append("for(Object ").append(rawItem).append(" : ").append(listVar).append(") {\n");
            braces++;
            out.append("if(")
                    .append(rawItem)
                    .append(" != null && !(")
                    .append(rawItem)
                    .append(" instanceof ")
                    .append(MvcExpression.raw(itemType))
                    .append(
                            ")) throw new IllegalStateException(\"Wrong collection element"
                                    + " type\");\n")
                    .append(itemType)
                    .append(' ')
                    .append(itemVar)
                    .append(" = (")
                    .append(itemType)
                    .append(")")
                    .append(rawItem)
                    .append(";\n");
            env.put(names[0], new MvcExpression.Value(itemVar, itemType));
            String status = "status" + sequence++;
            out.append("com.codename1.backend.mvc.IterationStatus ")
                    .append(status)
                    .append(" = new com.codename1.backend.mvc.IterationStatus(")
                    .append(index)
                    .append("++, ")
                    .append(listVar)
                    .append(list.type.endsWith("[]") ? ".length" : ".size()")
                    .append(");\n");
            env.put(
                    names.length == 2 ? names[1] : names[0] + "Stat",
                    new MvcExpression.Value(status, "com.codename1.backend.mvc.IterationStatus"));
        }
        if (e.hasAttr("th:object")) {
            form = e.attr("th:object").trim();
            if (!form.matches("\\$\\{[A-Za-z][A-Za-z0-9_]*}"))
                throw new IllegalArgumentException("th:object requires a named form model");
            form = form.substring(2, form.length() - 1);
            if (!env.containsKey(form))
                throw new IllegalArgumentException("Undeclared form " + form);
        }
        for (String condition : Arrays.asList("if", "unless"))
            if (e.hasAttr("th:" + condition)) {
                out.append("if(")
                        .append(condition.equals("unless") ? "!" : "")
                        .append(
                                MvcExpression.truth(
                                        expression(e.attr("th:" + condition), env, form)))
                        .append(") {\n");
                braces++;
            }
        if (e.hasAttr("th:replace")) {
            include(t, e.attr("th:replace"), out);
            close(out, braces);
            return;
        }
        String tag = e.normalName();
        boolean block = tag.equals("#root") || tag.equals("th:block");
        Map<String, MvcExpression.Value> dynamic = new LinkedHashMap<String, MvcExpression.Value>();
        for (Attribute a : e.attributes()) {
            String key = a.getKey();
            if (!key.startsWith("th:")) continue;
            String n = key.substring(3);
            if (ATTRS.contains(n) || BOOLEAN.contains(n))
                dynamic.put(n, expression(a.getValue(), env, form));
            if (n.equals("attr"))
                for (String assignment : split(a.getValue(), ',')) {
                    int equal = assignment.indexOf('=');
                    if (equal < 1)
                        throw new IllegalArgumentException("Expected attribute=expression");
                    String attr =
                            assignment
                                    .substring(0, equal)
                                    .trim()
                                    .toLowerCase(java.util.Locale.ROOT);
                    if (!attr.matches("[a-z][a-z0-9:_-]*")
                            || attr.startsWith("on")
                            || attr.equals("style")
                            || attr.equals("srcdoc")
                            || attr.startsWith("th:")
                            || attr.startsWith("hx-on")
                            || attr.equals("hx-vals")
                            || attr.equals("hx-headers"))
                        throw new IllegalArgumentException("Unsupported dynamic attribute " + attr);
                    dynamic.put(attr, expression(assignment.substring(equal + 1), env, form));
                }
        }
        if (e.hasAttr("th:classappend")) {
            MvcExpression.Value v = expression(e.attr("th:classappend"), env, form);
            MvcExpression.Value base = dynamic.get("class");
            dynamic.put(
                    "class",
                    new MvcExpression.Value(
                            (base == null ? q(e.attr("class")) : HTML + "string(" + base.code + ")")
                                    + " + \" \" + "
                                    + HTML
                                    + "string("
                                    + v.code
                                    + ")",
                            "java.lang.String"));
        }
        String field = null, fieldValue = null;
        if (e.hasAttr("th:field")) {
            field = field(e.attr("th:field"), form);
            MvcExpression.Value value = expression(e.attr("th:field"), env, form);
            fieldValue =
                    HTML + "field(model, " + q(form) + ", " + q(field) + ", " + value.code + ")";
            dynamic.put("name", new MvcExpression.Value(q(field), "java.lang.String"));
            if (!e.hasAttr("id") && !dynamic.containsKey("id"))
                dynamic.put("id", new MvcExpression.Value(q(field), "java.lang.String"));
            if (tag.equals("select")) select = fieldValue;
            else if (tag.equals("input")) {
                if (e.attr("type").equalsIgnoreCase("checkbox")
                        || e.attr("type").equalsIgnoreCase("radio")) {
                    MvcExpression.Value candidate = dynamic.get("value");
                    if (candidate == null)
                        candidate =
                                new MvcExpression.Value(
                                        q(e.hasAttr("value") ? e.attr("value") : "true"),
                                        "java.lang.String");
                    dynamic.put("value", candidate);
                    dynamic.put(
                            "checked",
                            new MvcExpression.Value(
                                    HTML + "checked(" + fieldValue + ", " + candidate.code + ")",
                                    "boolean"));
                    if (e.attr("type").equalsIgnoreCase("checkbox"))
                        literal(
                                out,
                                "<input type=\"hidden\" name=\"_" + field + "\" value=\"on\">");
                } else
                    dynamic.put("value", new MvcExpression.Value(fieldValue, "java.lang.Object"));
            } else if (!tag.equals("textarea"))
                throw new IllegalArgumentException("th:field requires input, select or textarea");
        }
        if (tag.equals("option") && select != null) {
            MvcExpression.Value candidate = dynamic.get("value");
            if (candidate == null) {
                if (e.hasAttr("value"))
                    candidate = new MvcExpression.Value(q(e.attr("value")), "java.lang.String");
                else {
                    MvcExpression.Value text =
                            e.hasAttr("th:text")
                                    ? expression(e.attr("th:text"), env, form)
                                    : new MvcExpression.Value(q(e.wholeText()), "java.lang.String");
                    candidate =
                            new MvcExpression.Value(
                                    HTML + "optionValue(" + text.code + ")", "java.lang.String");
                }
            }
            dynamic.put(
                    "selected",
                    new MvcExpression.Value(
                            HTML + "checked(" + select + ", " + candidate.code + ")", "boolean"));
        }
        if (tag.equals("button") || tag.equals("input")) {
            MvcExpression.Value method = dynamic.get("formmethod");
            if (method != null || e.hasAttr("formmethod")) {
                MvcExpression.Value owner = dynamic.get("form");
                String ownerCode =
                        owner != null ? owner.code : e.hasAttr("form") ? q(e.attr("form")) : "null";
                // Emit beside the submit control so fragments and conditional controls retain
                // their rendering scope. Preserve explicit form ownership for external controls.
                out.append("if(\"post\".equalsIgnoreCase(Html.string(")
                        .append(method == null ? q(e.attr("formmethod")) : method.code)
                        .append("))) Html.csrf(out, model, ")
                        .append(ownerCode)
                        .append(");\n");
            }
        }
        if (!block) {
            literal(out, "<" + tag);
            for (Attribute a : e.attributes())
                if (!a.getKey().startsWith("th:")
                        && !a.getKey().equals("xmlns:th")
                        && !dynamic.containsKey(a.getKey())) literal(out, " " + a.html());
            for (Map.Entry<String, MvcExpression.Value> a : dynamic.entrySet())
                out.append(HTML)
                        .append(BOOLEAN.contains(a.getKey()) ? "booleanAttribute" : "attribute")
                        .append("(out, ")
                        .append(q(a.getKey()))
                        .append(", ")
                        .append(a.getValue().code)
                        .append(");\n");
            literal(out, ">");
        }
        if (tag.equals("form")) {
            MvcExpression.Value method = dynamic.get("method");
            String methodCode =
                    method == null
                            ? q(e.hasAttr("method") ? e.attr("method") : "get")
                            : method.code;
            // htmx submissions of a form serialize the hidden token as well.
            out.append("if(!\"get\".equalsIgnoreCase(Html.string(")
                    .append(methodCode)
                    .append(")) || ")
                    .append(
                            e.hasAttr("hx-post")
                                    || e.hasAttr("hx-put")
                                    || e.hasAttr("hx-patch")
                                    || e.hasAttr("hx-delete")
                                    || dynamic.containsKey("hx-post")
                                    || dynamic.containsKey("hx-put")
                                    || dynamic.containsKey("hx-patch")
                                    || dynamic.containsKey("hx-delete"))
                    .append(") Html.csrf(out, model);\n");
        }
        if (e.hasAttr("th:insert")) include(t, e.attr("th:insert"), out);
        else if (e.hasAttr("th:text"))
            out.append(HTML)
                    .append("text(out, ")
                    .append(expression(e.attr("th:text"), env, form).code)
                    .append(");\n");
        else if (e.hasAttr("th:errors")) {
            String errorField = field(e.attr("th:errors"), form);
            out.append(HTML)
                    .append("text(out, Html.errors(model, ")
                    .append(q(form))
                    .append(", ")
                    .append(q(errorField))
                    .append("));\n");
        } else if (tag.equals("textarea") && field != null)
            out.append(HTML).append("text(out, ").append(fieldValue).append(");\n");
        else for (Node child : e.childNodes()) render(t, child, env, form, select, out);
        if (!block && !e.tag().isEmpty()) literal(out, "</" + tag + ">");
        close(out, braces);
    }

    private void include(Template current, String reference, StringBuilder out) {
        reference = reference.trim();
        if (reference.startsWith("~{") && reference.endsWith("}"))
            reference = reference.substring(2, reference.length() - 1).trim();
        String[] pair = reference.split("\\s*::\\s*", -1);
        if (pair.length != 2
                || !pair[0].matches("[A-Za-z0-9_/-]*")
                || !pair[1].matches("[A-Za-z][A-Za-z0-9_-]*"))
            throw new IllegalArgumentException("Expected a static template :: fragment reference");
        String key = (pair[0].isEmpty() ? current.name : pair[0]) + " :: " + pair[1];
        compile(key);
        out.append(methodName(key)).append("(out, model);\n");
    }

    private MvcExpression.Value expression(
            String value, Map<String, MvcExpression.Value> env, String form) {
        value = value.trim();
        MvcExpression parser = new MvcExpression(ctx, env);
        if (value.startsWith("@{") && value.endsWith("}"))
            return url(value.substring(2, value.length() - 1), env, form);
        if ((value.startsWith("${") || value.startsWith("*{")) && value.endsWith("}")) {
            boolean selection = value.startsWith("*");
            value = value.substring(2, value.length() - 1);
            if (selection) {
                if (form == null)
                    throw new IllegalArgumentException("Selection expression requires th:object");
                value = form + "." + value;
            }
        }
        return parser.parse(value);
    }

    private MvcExpression.Value url(
            String value, Map<String, MvcExpression.Value> env, String form) {
        int open = value.indexOf('(');
        String path = open < 0 ? value : value.substring(0, open);
        path = path.trim();
        if (!path.startsWith("/")
                || path.startsWith("//")
                || path.indexOf('\\') >= 0
                || path.matches(".*[\\s\"<>].*"))
            throw new IllegalArgumentException("URL expressions require a local absolute path");
        Map<String, MvcExpression.Value> args = new LinkedHashMap<String, MvcExpression.Value>();
        if (open >= 0) {
            if (!value.endsWith(")")) throw new IllegalArgumentException("Invalid URL expression");
            for (String assignment : split(value.substring(open + 1, value.length() - 1), ',')) {
                int eq = assignment.indexOf('=');
                if (eq < 1) throw new IllegalArgumentException("Expected URL parameter=value");
                String name = assignment.substring(0, eq).trim();
                if (!name.matches("[A-Za-z][A-Za-z0-9_]*"))
                    throw new IllegalArgumentException("Invalid URL parameter");
                args.put(name, expression(assignment.substring(eq + 1), env, form));
            }
        }
        int hash = path.indexOf('#');
        String fragment = hash < 0 ? "" : path.substring(hash);
        if (hash >= 0) path = path.substring(0, hash);
        StringBuilder code = new StringBuilder(urlPath(path, args));
        // Consume fragment placeholders before turning the remaining arguments into a query.
        String fragmentCode = urlPath(fragment, args);
        boolean query = path.contains("?");
        for (Map.Entry<String, MvcExpression.Value> arg : args.entrySet()) {
            code.append(" + ")
                    .append(q((query ? "&" : "?") + arg.getKey() + "="))
                    .append(" + Html.urlPart(")
                    .append(arg.getValue().code)
                    .append(')');
            query = true;
        }
        if (!fragment.isEmpty()) code.append(" + ").append(fragmentCode);
        return new MvcExpression.Value("(" + code + ")", "java.lang.String");
    }

    private static String urlPath(String path, Map<String, MvcExpression.Value> args) {
        StringBuilder code = new StringBuilder();
        Matcher placeholders = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)}").matcher(path);
        int pos = 0;
        while (placeholders.find()) {
            MvcExpression.Value arg = args.remove(placeholders.group(1));
            if (arg == null)
                throw new IllegalArgumentException(
                        "Missing URL path parameter " + placeholders.group(1));
            if (code.length() > 0) code.append(" + ");
            code.append(q(path.substring(pos, placeholders.start())))
                    .append(" + Html.urlPart(")
                    .append(arg.code)
                    .append(')');
            pos = placeholders.end();
        }
        if (code.length() > 0) code.append(" + ");
        code.append(q(path.substring(pos)));
        return code.toString();
    }

    private static String field(String value, String form) {
        if (form == null || !value.matches("\\*\\{([A-Za-z][A-Za-z0-9_]*|\\*)}"))
            throw new IllegalArgumentException(
                    "Field/errors require a scalar *{field} inside th:object");
        return value.substring(2, value.length() - 1);
    }

    static List<String> split(String value, char delimiter) {
        List<String> result = new ArrayList<String>();
        int depth = 0, start = 0;
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') quote = c;
            else if (c == '(' || c == '{' || c == '[') depth++;
            else if (c == ')' || c == '}' || c == ']') depth--;
            else if (c == delimiter && depth == 0) {
                result.add(value.substring(start, i).trim());
                start = i + 1;
            }
        }
        result.add(value.substring(start).trim());
        return result;
    }

    private void literal(StringBuilder out, String text) {
        // Adjacent markup is one byte copy, even when it came from separate DOM nodes.
        Matcher tail =
                Pattern.compile("out\\.put\\(C([0-9]+), 0, C[0-9]+\\.length\\);\\n$").matcher(out);
        if (tail.find()) {
            int index = Integer.parseInt(tail.group(1));
            byte[] previous = constants.get(index);
            byte[] next = text.getBytes(StandardCharsets.UTF_8);
            if (previous.length + next.length <= 4096) {
                byte[] combined = new byte[previous.length + next.length];
                System.arraycopy(previous, 0, combined, 0, previous.length);
                System.arraycopy(next, 0, combined, previous.length, next.length);
                constants.set(index, combined);
                return;
            }
        }
        for (int start = 0; start < text.length(); ) {
            int end = Math.min(start + 4096, text.length());
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
            byte[] bytes = text.substring(start, end).getBytes(StandardCharsets.UTF_8);
            int index = constants.size();
            constants.add(bytes);
            out.append("out.put(C")
                    .append(index)
                    .append(", 0, C")
                    .append(index)
                    .append(".length);\n");
            start = end;
        }
    }

    private static void close(StringBuilder out, int braces) {
        for (int i = 0; i < braces; i++) out.append("}\n");
    }

    private static String q(String text) {
        return MvcForms.q(text);
    }

    private static IllegalArgumentException problem(Template t, Node node, String message) {
        return new IllegalArgumentException(
                t.name
                        + ".html:"
                        + node.sourceRange().start().lineNumber()
                        + ":"
                        + node.sourceRange().start().columnNumber()
                        + ": "
                        + message);
    }
}

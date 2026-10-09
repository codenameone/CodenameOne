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
package com.codename1.fxml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.codename1.compat.jdk.ResourceNames;
import com.codename1.fxml.FxmlDocument.Attribute;
import com.codename1.fxml.FxmlDocument.Element;
import com.codename1.fxml.css.CssValue;
import com.codename1.fxml.css.CssValueParser;

/// Turns one FXML document into the Java source of a class that builds the
/// same object tree with plain constructors and setters.
///
/// #### The generated class
///
/// `com.codename1.generated.fxml.Fxml_<name>`, where `<name>` is the
/// document's resource path without its extension, encoded so that two
/// paths never share a name ([#className(String)]). It has
///
/// - `CN1_PATH`, the resource path; `CN1_CONTROLLER`, the binary name of
///   the `fx:controller` class or an empty string; `CN1_HANDLERS`, the
///   `#handler` names the document uses, separated by commas. The step
///   that generates the dispatcher after compilation reads these three out
///   of the class file.
/// - `load(FxmlContext)`, which builds the tree and answers the root.
/// - one private method per element, so that a large document does not
///   outgrow the size a single method may have.
///
/// #### What decides the code
///
/// Every class a document names is looked up in the class files of the
/// application and of its class path ([ClassModel]); the compiler therefore
/// knows the parameter type of each setter and converts an attribute's text
/// to it while compiling. A class or a property that is not there is an
/// error with the line of the element -- not something to find out on a
/// device. The application's own classes are among them, because documents
/// are compiled after it is ([FxmlClassCompiler]): a custom control is an
/// element like any other. The generated source is then compiled by javac,
/// which checks what this compiler does not.
final class FxmlCompiler {

    /// The package of every generated class.
    static final String PACKAGE = "com.codename1.generated.fxml";
    private static final String CONTEXT = "com.codename1.fxcompat.runtime.FxmlContext";
    private static final String EVENT_HANDLER = "javafx/event/EventHandler";
    private static final String LIST = "java/util/List";
    private static final Type OBJECT = Type.getObjectType("java/lang/Object");
    private static final Type STRING = Type.getObjectType("java/lang/String");

    /// The default property of the classes of the layer, for the ones that
    /// do not say it with `@DefaultProperty`: nearest class first.
    private static final String[][] DEFAULT_PROPERTIES = {
        {"javafx/scene/layout/Pane", "children"}, {"javafx/scene/Group", "children"},
        {"javafx/scene/text/TextFlow", "children"}, {"javafx/scene/control/ScrollPane", "content"},
        {"javafx/scene/control/TabPane", "tabs"}, {"javafx/scene/control/Tab", "content"},
        {"javafx/scene/control/TitledPane", "content"}, {"javafx/scene/control/Accordion", "panes"},
        {"javafx/scene/control/SplitPane", "items"}, {"javafx/scene/control/ToolBar", "items"},
        {"javafx/scene/control/MenuBar", "menus"}, {"javafx/scene/control/Menu", "items"},
        {"javafx/scene/control/MenuButton", "items"}, {"javafx/scene/control/ContextMenu", "items"},
        {"javafx/scene/control/ButtonBar", "buttons"}, {"javafx/scene/control/ListView", "items"},
        {"javafx/scene/control/TableView", "items"}, {"javafx/scene/control/TableColumn", "columns"},
        {"javafx/scene/control/TreeView", "root"}, {"javafx/scene/control/TreeItem", "children"},
        {"javafx/scene/control/ComboBox", "items"}, {"javafx/scene/control/ChoiceBox", "items"},
        {"javafx/scene/control/ToggleGroup", "toggles"}, {"javafx/scene/control/DialogPane", "content"},
        {"javafx/scene/control/Labeled", "text"}, {"javafx/scene/control/TextInputControl", "text"},
        {"javafx/scene/text/Text", "text"}, {"javafx/scene/image/ImageView", "image"},
        {"javafx/scene/shape/Path", "elements"}, {"javafx/scene/shape/Polygon", "points"},
        {"javafx/scene/shape/Polyline", "points"}, {"javafx/scene/Scene", "root"},
        {"javafx/stage/Stage", "scene"}, {"javafx/animation/Timeline", "keyFrames"},
        {"javafx/animation/SequentialTransition", "children"},
        {"javafx/animation/ParallelTransition", "children"},
    };

    /// A compile error with the element it is about.
    private static final class Failure extends Exception {
        private static final long serialVersionUID = 1L;
        final transient Element at;

        Failure(Element at, String message) {
            super(message);
            this.at = at;
        }
    }

    /// An expression of the generated source and the type it has.
    private static final class Value {
        final String code;
        final Type type;

        Value(String code, Type type) {
            this.code = code;
            this.type = type;
        }
    }

    private final ClassModel model;
    private final Map<String, FxmlDocument> documents;
    private final Messages messages;
    private final CssValueParser values = new CssValueParser();

    // The document being compiled.
    private FxmlDocument doc;
    private final List<String> methods = new ArrayList<String>();
    private final Map<String, Type> ids = new HashMap<String, Type>();
    private final Set<String> handlers = new LinkedHashSet<String>();
    private String controller;
    private int counter;

    /// Creates a compiler.
    ///
    /// #### Parameters
    ///
    /// - `model`: the classes of the application's class path
    ///
    /// - `documents`: every document of the application by resource path,
    ///   which an `<fx:include>` is resolved in
    ///
    /// - `messages`: where errors go
    FxmlCompiler(ClassModel model, Map<String, FxmlDocument> documents, Messages messages) {
        this.model = model;
        this.documents = documents;
        this.messages = messages;
    }

    /// The simple name of the class generated for the document at a
    /// resource path. The encoding is one to one: a letter or a digit
    /// stands for itself, `/` is `__`, `_` is `_u`, `.` is `_d`, `-` is
    /// `_m` and any other character is `_x` and its code in four hex
    /// digits.
    static String className(String path) {
        String p = path.endsWith(".fxml") ? path.substring(0, path.length() - 5) : path;
        StringBuilder s = new StringBuilder("Fxml_");
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                s.append(c);
            } else if (c == '/') {
                s.append("__");
            } else if (c == '_') {
                s.append("_u");
            } else if (c == '.') {
                s.append("_d");
            } else if (c == '-') {
                s.append("_m");
            } else {
                String hex = Integer.toHexString(c);
                s.append("_x").append("0000".substring(hex.length())).append(hex);
            }
        }
        return s.toString();
    }

    /// Compiles a document. Answers the source of its class, or `null`
    /// with the reason recorded as an error.
    String compile(FxmlDocument document) throws IOException {
        doc = document;
        methods.clear();
        ids.clear();
        handlers.clear();
        controller = null;
        counter = 0;
        try {
            return generate();
        } catch (Failure f) {
            Element at = f.at == null ? doc.root : f.at;
            messages.error(doc.file, at.line, at.column, f.getMessage() + " (at " + at.text() + ")");
            return null;
        }
    }

    private String generate() throws Failure, IOException {
        Element root = doc.root;
        String declared = root.fx("controller");
        if (declared != null) {
            controller = declared.trim();
            if (!isQualifiedName(controller)) {
                throw new Failure(root, "fx:controller=\"" + declared + "\" is not a class name");
            }
        }
        Value tree = value(root, true);
        String name = className(doc.path);
        StringBuilder s = new StringBuilder();
        s.append("// Generated from ").append(doc.path).append(" by the Codename One FXML compiler.\n");
        s.append("// Do not edit: the file is written again by every build.\n");
        s.append("package ").append(PACKAGE).append(";\n\n");
        s.append("@SuppressWarnings({\"rawtypes\", \"unchecked\", \"cast\", \"deprecation\"})\n");
        s.append("public final class ").append(name).append(" {\n\n");
        s.append("    public static final String CN1_PATH = ").append(literal(doc.path)).append(";\n");
        s.append("    public static final String CN1_CONTROLLER = ")
                .append(literal(controller == null ? "" : controller)).append(";\n");
        StringBuilder names = new StringBuilder();
        for (String h : handlers) {
            names.append(names.length() == 0 ? "" : ",").append(h);
        }
        s.append("    public static final String CN1_HANDLERS = ").append(literal(names.toString())).append(";\n\n");
        s.append("    private ").append(name).append("() {\n    }\n\n");
        s.append("    public static Object load(").append(CONTEXT).append(" c) throws Exception {\n");
        s.append("        Object root = ").append(tree.code).append(";\n");
        s.append("        c.done();\n");
        s.append("        return root;\n");
        s.append("    }\n");
        for (String m : methods) {
            s.append('\n').append(m);
        }
        s.append("}\n");
        return s.toString();
    }

    // ------------------------------------------------------------- names

    private static boolean isIdentifier(String s) {
        if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isQualifiedName(String s) {
        int from = 0;
        while (true) {
            int dot = s.indexOf('.', from);
            if (!isIdentifier(s.substring(from, dot < 0 ? s.length() : dot))) {
                return false;
            }
            if (dot < 0) {
                return true;
            }
            from = dot + 1;
        }
    }

    /// A Java string literal in ASCII.
    static String literal(String text) {
        StringBuilder s = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                s.append('\\').append(c);
            } else if (c == '\n') {
                s.append('\\').append('n');
            } else if (c == '\r') {
                s.append('\\').append('r');
            } else if (c == '\t') {
                s.append('\\').append('t');
            } else if (c < ' ' || c > '~') {
                String hex = Integer.toHexString(c);
                s.append('\\').append('u').append("0000".substring(hex.length())).append(hex);
            } else {
                s.append(c);
            }
        }
        return s.append('"').toString();
    }

    private static String cap(String property) {
        if (property.length() == 0) {
            return property;
        }
        char first = property.charAt(0);
        // By hand: a property name is ASCII, and the locale must not fold it.
        return (first >= 'a' && first <= 'z' ? (char) (first - ('a' - 'A')) : first) + property.substring(1);
    }

    /// The type as Java source spells it.
    private static String java(Type t) {
        if (t.getSort() == Type.ARRAY) {
            StringBuilder s = new StringBuilder(java(t.getElementType()));
            for (int i = 0; i < t.getDimensions(); i++) {
                s.append("[]");
            }
            return s.toString();
        }
        return t.getClassName().replace('$', '.');
    }

    private static boolean isObject(Type t) {
        return t.getSort() == Type.OBJECT;
    }

    /// The class a name in the document stands for: a qualified name, or a
    /// simple one found through the imports.
    private ClassModel.Info resolve(String name) throws IOException {
        if (name.indexOf('.') >= 0) {
            ClassModel.Info direct = qualified(name);
            if (direct != null) {
                return direct;
            }
        }
        for (String imported : doc.imports) {
            ClassModel.Info found = null;
            if (imported.endsWith(".*")) {
                found = qualified(imported.substring(0, imported.length() - 1) + name);
            } else if (imported.equals(name) || imported.endsWith("." + name)) {
                found = qualified(imported);
            } else {
                int dot = name.indexOf('.');
                if (dot > 0 && (imported.equals(name.substring(0, dot))
                        || imported.endsWith("." + name.substring(0, dot)))) {
                    // A nested class named through its imported outer class.
                    found = qualified(imported + name.substring(dot));
                }
            }
            if (found != null) {
                return found;
            }
        }
        return name.indexOf('.') < 0 ? model.find("java/lang/" + name) : null;
    }

    /// The class of a dotted name, trying each split between package and
    /// nested class.
    private ClassModel.Info qualified(String dotted) throws IOException {
        String internal = dotted.replace('.', '/');
        while (true) {
            ClassModel.Info info = model.find(internal);
            if (info != null) {
                return info;
            }
            int slash = internal.lastIndexOf('/');
            if (slash < 0) {
                return null;
            }
            internal = internal.substring(0, slash) + '$' + internal.substring(slash + 1);
        }
    }

    private ClassModel.Info need(String name, Element at) throws Failure, IOException {
        ClassModel.Info info = resolve(name);
        if (info == null) {
            throw new Failure(at, "unknown class " + name + ": it is not imported by a <?import?> of this"
                    + " document, or it is neither a class of the application nor on its class path. Import a"
                    + " custom control by its full name (<?import com.example.MyControl?>) or its package"
                    + " (<?import com.example.*?>)");
        }
        if (!info.isPublic()) {
            throw new Failure(at, "the class " + info.sourceName() + " is not public");
        }
        return info;
    }

    // ------------------------------------------------------- properties

    private ClassModel.Method setter(String owner, String property) throws IOException {
        List<ClassModel.Method> all = model.methods(owner, "set" + cap(property));
        ClassModel.Method getter = getter(owner, property);
        ClassModel.Method first = null;
        for (ClassModel.Method m : all) {
            if (m.isStatic() || m.parameters.length != 1) {
                continue;
            }
            if (getter != null && getter.returnType().equals(m.parameters[0])) {
                return m;
            }
            first = first == null ? m : first;
        }
        return first;
    }

    private ClassModel.Method getter(String owner, String property) throws IOException {
        for (String prefix : new String[] {"get", "is"}) {
            for (ClassModel.Method m : model.methods(owner, prefix + cap(property))) {
                if (!m.isStatic() && m.parameters.length == 0 && m.returnType().getSort() != Type.VOID) {
                    return m;
                }
            }
        }
        return null;
    }

    private ClassModel.Method staticSetter(String owner, String property) throws IOException {
        for (ClassModel.Method m : model.methods(owner, "set" + cap(property))) {
            if (m.isStatic() && m.parameters.length == 2) {
                return m;
            }
        }
        return null;
    }

    private boolean isList(Type t) throws IOException {
        return isObject(t) && model.isAssignable(t.getInternalName(), LIST);
    }

    /// The element type a list getter declares, or `null` when the class
    /// file does not say (a raw list, a type variable, a wildcard).
    private static Type elementType(ClassModel.Method getter) {
        String sig = getter.signature;
        if (sig == null) {
            return null;
        }
        int close = sig.indexOf(')');
        int open = sig.indexOf('<', close);
        if (close < 0 || open < 0 || open + 1 >= sig.length() || sig.charAt(open + 1) != 'L') {
            return null;
        }
        int end = open + 2;
        while (end < sig.length() && sig.charAt(end) != ';' && sig.charAt(end) != '<') {
            end++;
        }
        return Type.getObjectType(sig.substring(open + 2, end));
    }

    private String defaultProperty(String owner) throws IOException {
        String annotated = model.annotatedDefaultProperty(owner);
        if (annotated != null) {
            return annotated;
        }
        String best = null;
        int bestDepth = Integer.MAX_VALUE;
        for (String[] entry : DEFAULT_PROPERTIES) {
            int depth = depth(owner, entry[0]);
            if (depth >= 0 && depth < bestDepth) {
                best = entry[1];
                bestDepth = depth;
            }
        }
        return best;
    }

    /// How many classes `ancestor` is above `cls`; -1 when it is not.
    private int depth(String cls, String ancestor) throws IOException {
        int depth = 0;
        String at = cls;
        while (at != null) {
            if (at.equals(ancestor)) {
                return depth;
            }
            ClassModel.Info info = model.find(at);
            at = info == null ? null : info.superName;
            depth++;
        }
        return -1;
    }

    // ----------------------------------------------------------- values

    /// Builds the value an element stands for, in a method of its own, and
    /// answers the call of that method.
    private Value value(Element e, boolean isRoot) throws Failure, IOException {
        StringBuilder body = new StringBuilder();
        Type type;
        boolean instance = false;
        Set<Attribute> consumed = new LinkedHashSet<Attribute>();
        Set<Element> consumedElements = new LinkedHashSet<Element>();
        if (e.fx) {
            if ("root".equals(e.name)) {
                if (!isRoot) {
                    throw new Failure(e, "<fx:root> can only be the root element of a document");
                }
                String typeName = e.plain("type");
                if (typeName == null) {
                    throw new Failure(e, "<fx:root> needs a type attribute");
                }
                ClassModel.Info info = need(typeName.trim(), e);
                type = Type.getObjectType(info.name);
                body.append("        Object given = c.root();\n");
                body.append("        if (!(given instanceof ").append(java(type)).append(")) {\n");
                body.append("            throw c.error(").append(literal("the root set on the loader must be a "
                        + java(type) + ", as the <fx:root> element says")).append(");\n        }\n");
                body.append("        ").append(java(type)).append(" v = (").append(java(type)).append(") given;\n");
                instance = true;
            } else if ("include".equals(e.name)) {
                String source = e.plain("source");
                if (source == null) {
                    throw new Failure(e, "<fx:include> needs a source attribute");
                }
                FxmlDocument included = documents.get(resolvePath(source));
                if (included == null) {
                    throw new Failure(e, "the included document " + source + " (" + resolvePath(source)
                            + ") is not among the application's desktop resources");
                }
                type = rootType(included, e);
                String id = e.fx("id");
                body.append("        Object loaded = c.include(").append(literal(source)).append(", ")
                        .append(id == null ? "null" : literal(id)).append(");\n");
                body.append("        if (!(loaded instanceof ").append(java(type)).append(")) {\n");
                body.append("            throw c.error(").append(literal("the included document " + source
                        + " did not answer a " + java(type))).append(");\n        }\n");
                body.append("        ").append(java(type)).append(" v = (").append(java(type))
                        .append(") loaded;\n");
                if (id != null) {
                    checkId(id, e);
                    ids.put(id, type);
                    String includedController = included.root.fx("controller");
                    if (includedController != null) {
                        ids.put(id + "Controller", Type.getObjectType(includedController.trim().replace('.', '/')));
                    }
                }
            } else if ("reference".equals(e.name)) {
                String source = e.plain("source");
                if (source == null) {
                    throw new Failure(e, "<fx:reference> needs a source attribute");
                }
                Value ref = reference(source.trim(), e);
                type = ref.type;
                body.append("        ").append(java(type)).append(" v = ").append(ref.code).append(";\n");
            } else if ("script".equals(e.name)) {
                throw new Failure(e, "<fx:script> is not supported: a document is compiled to Java, and a"
                        + " script would have to be interpreted on the device. Write the code in the controller");
            } else {
                throw new Failure(e, "<fx:" + e.name + "> is not supported");
            }
        } else {
            ClassModel.Info info = need(e.name, e);
            type = construct(e, info, body, consumed, consumedElements);
            instance = true;
        }
        if (e.fx("controller") != null && !isRoot) {
            throw new Failure(e, "fx:controller can only be on the root element");
        }
        if (isRoot && controller != null) {
            String source = controller.replace('$', '.');
            body.insert(0, "        c.controller(" + source + ".class, " + literal(controller) + ");\n");
        }
        String id = e.fx("id");
        if (id != null && !(e.fx && "include".equals(e.name))) {
            checkId(id, e);
            ids.put(id, type);
            body.append("        c.id(").append(literal(id)).append(", v);\n");
        }
        for (Attribute a : e.attributes) {
            if (a.fx || consumed.contains(a)) {
                continue;
            }
            if (e.fx && ("source".equals(a.name) || "type".equals(a.name) || "resources".equals(a.name)
                    || "charset".equals(a.name))) {
                continue;
            }
            int dot = a.name.lastIndexOf('.');
            if (dot > 0) {
                staticAttribute(e, a, a.name.substring(0, dot), a.name.substring(dot + 1), type, body);
            } else if (instance || (e.fx && "include".equals(e.name))) {
                attribute(e, a, type, body);
            } else {
                throw new Failure(e, a.text() + " cannot be set here");
            }
        }
        if (instance) {
            children(e, type, body, consumedElements);
        }
        String method = "e" + counter++;
        StringBuilder m = new StringBuilder();
        m.append("    private static ").append(java(type)).append(' ').append(method).append('(').append(CONTEXT)
                .append(" c) throws Exception {\n").append(body).append("        return v;\n    }\n");
        methods.add(m.toString());
        return new Value(method + "(c)", type);
    }

    private void checkId(String id, Element e) throws Failure {
        if (!isIdentifier(id)) {
            throw new Failure(e, "fx:id=\"" + id + "\" is not a valid name");
        }
        if (ids.containsKey(id)) {
            throw new Failure(e, "fx:id=\"" + id + "\" is used twice in this document");
        }
    }

    private String resolvePath(String source) {
        if (source.startsWith("/")) {
            return ResourceNames.normalize(source);
        }
        int slash = doc.path.lastIndexOf('/');
        return ResourceNames.normalize(slash < 0 ? source : doc.path.substring(0, slash + 1) + source);
    }

    /// The type of the root of another document, resolved with that
    /// document's own imports.
    private Type rootType(FxmlDocument other, Element at) throws Failure, IOException {
        FxmlDocument mine = doc;
        doc = other;
        try {
            Element root = other.root;
            String name = root.fx && "root".equals(root.name) ? root.plain("type") : root.fx ? null : root.name;
            ClassModel.Info info = name == null ? null : resolve(name.trim());
            if (info == null) {
                throw new Failure(at, "cannot tell what the included document " + other.path
                        + " builds: its root element " + root.text() + " is not a known class");
            }
            return Type.getObjectType(info.name);
        } finally {
            doc = mine;
        }
    }

    private Value reference(String id, Element at) throws Failure {
        if ("controller".equals(id)) {
            if (controller == null) {
                throw new Failure(at, "$controller is used but the document has no fx:controller");
            }
            String source = controller.replace('$', '.');
            return new Value("((" + source + ") c.controller())", Type.getObjectType(controller.replace('.', '/')));
        }
        Type type = ids.get(id);
        if (type == null) {
            throw new Failure(at, "nothing with fx:id=\"" + id + "\" is defined before this point");
        }
        // The cast is to the type the compiler itself recorded for the id
        // in this document, so it cannot fail.
        return new Value("((" + java(type) + ") c.get(" + literal(id) + "))", type);
    }

    // ----------------------------------------------------- construction

    private Type construct(Element e, ClassModel.Info info, StringBuilder body, Set<Attribute> consumed,
            Set<Element> consumedElements) throws Failure, IOException {
        Type type = Type.getObjectType(info.name);
        String source = info.sourceName();
        String value = e.fx("value");
        String factory = e.fx("factory");
        String constant = e.fx("constant");
        if (value != null) {
            body.append("        ").append(source).append(" v = ").append(coerce(value, type, e)).append(";\n");
            return type;
        }
        if (constant != null) {
            for (ClassModel.Field f : info.fields) {
                if (f.name.equals(constant) && (f.access & Opcodes.ACC_STATIC) != 0
                        && (f.access & Opcodes.ACC_PUBLIC) != 0) {
                    Type ft = boxed(Type.getType(f.descriptor));
                    body.append("        ").append(java(ft)).append(" v = ").append(source).append('.')
                            .append(constant).append(";\n");
                    return ft;
                }
            }
            throw new Failure(e, source + " has no public static field " + constant);
        }
        if (factory != null) {
            for (ClassModel.Method m : model.methods(info.name, factory)) {
                if (m.isStatic() && m.parameters.length == 0 && isObject(m.returnType())) {
                    Type rt = m.returnType();
                    body.append("        ").append(java(rt)).append(" v = ").append(source).append('.')
                            .append(factory).append("();\n");
                    return rt;
                }
            }
            throw new Failure(e, source + " has no public static method " + factory + "() without arguments");
        }
        if (info.isAbstract()) {
            throw new Failure(e, source + " is abstract and cannot be an element");
        }
        ClassModel.Method noArgs = null;
        List<ClassModel.Method> named = new ArrayList<ClassModel.Method>();
        for (ClassModel.Method m : info.methods) {
            if ("<init>".equals(m.name) && m.isPublic()) {
                if (m.parameters.length == 0) {
                    noArgs = m;
                } else if (m.allArgsNamed()) {
                    named.add(m);
                }
            }
        }
        // What the document sets: an attribute, or a property element that
        // holds one value.
        Map<String, Object> given = new LinkedHashMap<String, Object>();
        boolean allSettable = true;
        for (Attribute a : e.attributes) {
            if (!a.fx && a.name.indexOf('.') < 0) {
                given.put(a.name, a);
                allSettable &= setter(info.name, a.name) != null || getter(info.name, a.name) != null;
            }
        }
        for (Element child : e.elements()) {
            if (!child.fx && child.name.indexOf('.') < 0 && Character.isLowerCase(child.name.charAt(0))
                    && !given.containsKey(child.name)) {
                given.put(child.name, child);
                allSettable &= setter(info.name, child.name) != null || getter(info.name, child.name) != null;
            }
        }
        if (noArgs != null && (allSettable || named.isEmpty())) {
            body.append("        ").append(source).append(" v = new ").append(source).append("();\n");
            return type;
        }
        ClassModel.Method best = null;
        int bestMatched = -1;
        for (ClassModel.Method m : named) {
            int matched = 0;
            for (String arg : m.argNames) {
                matched += given.containsKey(arg) ? 1 : 0;
            }
            if (matched > bestMatched || (matched == bestMatched && best != null
                    && m.parameters.length < best.parameters.length)) {
                best = m;
                bestMatched = matched;
            }
        }
        if (best == null) {
            throw new Failure(e, source + " has no public constructor without arguments and none whose"
                    + " arguments are all named with @NamedArg, so a document cannot create it");
        }
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < best.parameters.length; i++) {
            Type pt = best.parameters[i];
            Object from = given.get(best.argNames[i]);
            String code;
            if (from instanceof Attribute) {
                Attribute a = (Attribute) from;
                consumed.add(a);
                code = attributeValue(a.value, pt, e, a);
            } else if (from instanceof Element) {
                Element child = (Element) from;
                consumedElements.add(child);
                List<Element> inner = child.elements();
                if (inner.size() == 1) {
                    Value v = value(inner.get(0), false);
                    requireAssignable(v.type, pt, child);
                    code = v.code;
                } else if (inner.isEmpty() && child.content().length() > 0) {
                    code = coerce(child.content(), pt, child);
                } else {
                    throw new Failure(child, "the constructor argument " + child.name + " needs exactly one value");
                }
            } else if (best.argDefaults[i] != null && best.argDefaults[i].length() > 0) {
                code = coerce(best.argDefaults[i], pt, e);
            } else {
                code = zero(pt);
            }
            args.append(i == 0 ? "" : ", ").append(code);
        }
        body.append("        ").append(source).append(" v = new ").append(source).append('(').append(args)
                .append(");\n");
        return type;
    }

    private static String zero(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
                return "false";
            case Type.CHAR:
                return "'\\0'";
            case Type.BYTE:
                return "(byte) 0";
            case Type.SHORT:
                return "(short) 0";
            case Type.INT:
                return "0";
            case Type.LONG:
                return "0L";
            case Type.FLOAT:
                return "0f";
            case Type.DOUBLE:
                return "0.0";
            default:
                return "(" + java(t) + ") null";
        }
    }

    private static Type boxed(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
                return Type.getObjectType("java/lang/Boolean");
            case Type.CHAR:
                return Type.getObjectType("java/lang/Character");
            case Type.BYTE:
                return Type.getObjectType("java/lang/Byte");
            case Type.SHORT:
                return Type.getObjectType("java/lang/Short");
            case Type.INT:
                return Type.getObjectType("java/lang/Integer");
            case Type.LONG:
                return Type.getObjectType("java/lang/Long");
            case Type.FLOAT:
                return Type.getObjectType("java/lang/Float");
            case Type.DOUBLE:
                return Type.getObjectType("java/lang/Double");
            default:
                return t;
        }
    }

    private void requireAssignable(Type from, Type to, Element at) throws Failure, IOException {
        Type target = boxed(to);
        if (!isObject(from) || !isObject(target)) {
            if (from.equals(target)) {
                return;
            }
            throw new Failure(at, "a " + java(from) + " cannot be used where a " + java(to) + " is expected");
        }
        if (model.find(from.getInternalName()) == null || model.find(target.getInternalName()) == null) {
            // A class the model cannot read; javac decides.
            return;
        }
        if (!model.isAssignable(from.getInternalName(), target.getInternalName())) {
            throw new Failure(at, "a " + java(from) + " cannot be used where a " + java(to) + " is expected");
        }
    }

    // ------------------------------------------------------- attributes

    private void staticAttribute(Element e, Attribute a, String className, String property, Type type,
            StringBuilder body) throws Failure, IOException {
        ClassModel.Info owner = need(className, e);
        ClassModel.Method m = staticSetter(owner.name, property);
        if (m == null) {
            throw new Failure(e, a.text() + ": " + owner.sourceName() + " has no static property " + property
                    + " (a public static set" + cap(property) + " with two arguments)");
        }
        requireAssignable(type, m.parameters[0], e);
        body.append("        ").append(owner.sourceName()).append(".set").append(cap(property)).append("(v, ")
                .append(attributeValue(a.value, m.parameters[1], e, a)).append(");\n");
    }

    private void attribute(Element e, Attribute a, Type type, StringBuilder body) throws Failure, IOException {
        if (!isObject(type)) {
            throw new Failure(e, a.text() + " cannot be set on a " + java(type));
        }
        String owner = type.getInternalName();
        String raw = a.value;
        if (raw.startsWith("${") && raw.endsWith("}")) {
            binding(e, a, owner, raw.substring(2, raw.length() - 1).trim(), body);
            return;
        }
        ClassModel.Method set = setter(owner, a.name);
        if (set != null) {
            Type pt = set.parameters[0];
            if (isObject(pt) && EVENT_HANDLER.equals(pt.getInternalName())) {
                if (!raw.startsWith("#")) {
                    throw new Failure(e, a.text() + ": a handler is written as \"#method\", naming a method of"
                            + " the controller. Script handlers are not supported, because a document is"
                            + " compiled to Java");
                }
                String name = raw.substring(1).trim();
                if (!isIdentifier(name)) {
                    throw new Failure(e, a.text() + ": '" + name + "' is not a method name");
                }
                if (controller == null) {
                    throw new Failure(e, a.text() + ": the document has no fx:controller for the handler to be a"
                            + " method of. A controller set with setController() is not known to the compiler;"
                            + " name its class with fx:controller");
                }
                handlers.add(name);
                body.append("        v.set").append(cap(a.name)).append("(c.handler(").append(literal(name))
                        .append("));\n");
                return;
            }
            body.append("        v.set").append(cap(a.name)).append('(').append(attributeValue(raw, pt, e, a))
                    .append(");\n");
            return;
        }
        ClassModel.Method get = getter(owner, a.name);
        if (get != null && isList(get.returnType())) {
            Type element = elementType(get);
            Type target = element == null ? STRING : element;
            int from = 0;
            while (from <= raw.length()) {
                int comma = raw.indexOf(',', from);
                if (comma < 0) {
                    comma = raw.length();
                }
                String item = raw.substring(from, comma).trim();
                if (item.length() > 0) {
                    body.append("        ((java.util.List) v.").append(get.name).append("()).add(")
                            .append(attributeValue(item, target, e, a)).append(");\n");
                }
                from = comma + 1;
            }
            return;
        }
        throw new Failure(e, a.text() + ": " + java(type) + " has no property " + a.name + " (no public set"
                + cap(a.name) + " and no list get" + cap(a.name) + ")");
    }

    /// The code of an attribute's value: a resource key, a location, a
    /// reference, or text converted to the target type.
    private String attributeValue(String raw, Type target, Element e, Attribute a) throws Failure, IOException {
        if (raw.startsWith("\\")) {
            return coerce(raw.substring(1), target, e);
        }
        if (raw.startsWith("%")) {
            requireString(target, e, a);
            return "c.string(" + literal(raw.substring(1)) + ")";
        }
        if (raw.startsWith("@")) {
            requireString(target, e, a);
            return "c.url(" + literal(raw.substring(1)) + ")";
        }
        if (raw.startsWith("${")) {
            throw new Failure(e, a.text() + ": a ${binding} can only be the whole value of a property");
        }
        if (raw.startsWith("$")) {
            Value ref = reference(raw.substring(1).trim(), e);
            requireAssignable(ref.type, target, e);
            return ref.code;
        }
        return coerce(raw, target, e);
    }

    private void requireString(Type target, Element e, Attribute a) throws Failure {
        if (!isObject(target) || !("java/lang/String".equals(target.getInternalName())
                || "java/lang/Object".equals(target.getInternalName())
                || "java/lang/CharSequence".equals(target.getInternalName()))) {
            throw new Failure(e, a.text() + ": the value is text, but the property is a " + java(target));
        }
    }

    private void binding(Element e, Attribute a, String owner, String expression, StringBuilder body)
            throws Failure, IOException {
        int dot = expression.indexOf('.');
        if (dot <= 0 || !isIdentifier(expression.substring(0, dot)) || !isIdentifier(expression.substring(dot + 1))) {
            throw new Failure(e, a.text() + ": only a binding to one property of the controller or of an element"
                    + " with an fx:id is supported, as in ${controller.name} or ${slider.value}. Write any"
                    + " other expression as a binding in the controller's initialize()");
        }
        String property = expression.substring(dot + 1);
        Value source = reference(expression.substring(0, dot), e);
        if (model.find(source.type.getInternalName()) != null
                && model.methods(source.type.getInternalName(), property + "Property").isEmpty()) {
            throw new Failure(e, a.text() + ": " + java(source.type) + " has no " + property + "Property()");
        }
        if (model.methods(owner, a.name + "Property").isEmpty()) {
            throw new Failure(e, a.text() + ": " + owner.replace('/', '.') + " has no " + a.name
                    + "Property() to bind");
        }
        ClassModel.Method set = setter(owner, a.name);
        String mode = "BIND_SAME";
        if (set != null) {
            Type pt = set.parameters[0];
            if (isObject(pt) && "java/lang/String".equals(pt.getInternalName())) {
                mode = "BIND_STRING";
            } else if (pt.getSort() >= Type.BYTE && pt.getSort() <= Type.DOUBLE) {
                mode = "BIND_NUMBER";
            }
        }
        body.append("        ").append(CONTEXT).append(".bind(v.").append(a.name).append("Property(), ")
                .append(source.code).append('.').append(property).append("Property(), ").append(CONTEXT)
                .append('.').append(mode).append(");\n");
    }

    // --------------------------------------------------------- children

    private void children(Element e, Type type, StringBuilder body, Set<Element> consumed)
            throws Failure, IOException {
        List<Value> defaults = new ArrayList<Value>();
        Element firstDefault = null;
        for (Element child : e.elements()) {
            if (consumed.contains(child)) {
                continue;
            }
            if (child.fx && "define".equals(child.name)) {
                for (Element defined : child.elements()) {
                    body.append("        ").append(value(defined, false).code).append(";\n");
                }
                continue;
            }
            int dot = child.name.lastIndexOf('.');
            if (!child.fx && dot > 0 && Character.isLowerCase(child.name.charAt(dot + 1))
                    && resolve(child.name) == null) {
                staticElement(child, child.name.substring(0, dot), child.name.substring(dot + 1), type, body);
            } else if (!child.fx && dot < 0 && Character.isLowerCase(child.name.charAt(0))) {
                propertyElement(child, type, body);
            } else {
                firstDefault = firstDefault == null ? child : firstDefault;
                defaults.add(value(child, false));
            }
        }
        String text = e.content();
        if (defaults.isEmpty() && text.length() == 0) {
            return;
        }
        if (!isObject(type)) {
            throw new Failure(e, "a " + java(type) + " cannot have content");
        }
        if (isList(type)) {
            for (Value v : defaults) {
                body.append("        ((java.util.List) v).add(").append(v.code).append(");\n");
            }
            if (text.length() > 0) {
                body.append("        ((java.util.List) v).add(").append(literal(text)).append(");\n");
            }
            return;
        }
        String property = defaultProperty(type.getInternalName());
        if (property == null) {
            throw new Failure(firstDefault == null ? e : firstDefault, java(type) + " has no default property,"
                    + " so content must be inside a property element such as <children>");
        }
        assign(e, type, property, defaults, text, body);
    }

    private void propertyElement(Element child, Type type, StringBuilder body) throws Failure, IOException {
        if (!isObject(type)) {
            throw new Failure(child, "a " + java(type) + " has no properties");
        }
        for (Attribute a : child.attributes) {
            throw new Failure(child, a.text() + ": a property element takes no attributes");
        }
        List<Value> values = new ArrayList<Value>();
        for (Element inner : child.elements()) {
            if (inner.fx && "define".equals(inner.name)) {
                throw new Failure(inner, "<fx:define> cannot be inside a property element");
            }
            values.add(value(inner, false));
        }
        assign(child, type, child.name, values, child.content(), body);
    }

    /// Gives a property its content: one value through the setter, or
    /// every value added to a list the getter answers.
    private void assign(Element at, Type type, String property, List<Value> values, String text,
            StringBuilder body) throws Failure, IOException {
        String owner = type.getInternalName();
        ClassModel.Method set = setter(owner, property);
        ClassModel.Method get = getter(owner, property);
        boolean list = get != null && isList(get.returnType());
        if (set != null && values.size() == 1 && text.length() == 0) {
            Type from = values.get(0).type;
            Type to = boxed(set.parameters[0]);
            boolean fits = !isObject(from) || !isObject(to) || model.find(from.getInternalName()) == null
                    || model.isAssignable(from.getInternalName(), to.getInternalName());
            if (fits || !list) {
                requireAssignable(from, set.parameters[0], at);
                body.append("        v.set").append(cap(property)).append('(').append(values.get(0).code)
                        .append(");\n");
                return;
            }
        }
        if (set != null && values.isEmpty() && text.length() > 0 && !list) {
            body.append("        v.set").append(cap(property)).append('(')
                    .append(coerce(text, set.parameters[0], at)).append(");\n");
            return;
        }
        if (list) {
            Type element = elementType(get);
            for (Value v : values) {
                if (element != null) {
                    requireAssignable(v.type, element, at);
                }
                body.append("        ((java.util.List) v.").append(get.name).append("()).add(").append(v.code)
                        .append(");\n");
            }
            if (text.length() > 0) {
                body.append("        ((java.util.List) v.").append(get.name).append("()).add(")
                        .append(coerce(text, element == null ? STRING : element, at)).append(");\n");
            }
            return;
        }
        if (set == null && get == null) {
            throw new Failure(at, java(type) + " has no property " + property + " (no public set" + cap(property)
                    + " and no get" + cap(property) + ")");
        }
        throw new Failure(at, "the property " + property + " of " + java(type) + " takes one value, and "
                + (values.isEmpty() ? "none" : String.valueOf(values.size())) + " were given");
    }

    private void staticElement(Element child, String className, String property, Type type, StringBuilder body)
            throws Failure, IOException {
        ClassModel.Info owner = need(className, child);
        ClassModel.Method m = staticSetter(owner.name, property);
        if (m == null) {
            throw new Failure(child, owner.sourceName() + " has no static property " + property
                    + " (a public static set" + cap(property) + " with two arguments)");
        }
        requireAssignable(type, m.parameters[0], child);
        List<Element> inner = child.elements();
        String code;
        if (inner.size() == 1) {
            Value v = value(inner.get(0), false);
            requireAssignable(v.type, m.parameters[1], child);
            code = v.code;
        } else if (inner.isEmpty() && child.content().length() > 0) {
            code = coerce(child.content(), m.parameters[1], child);
        } else {
            throw new Failure(child, "a static property takes exactly one value");
        }
        body.append("        ").append(owner.sourceName()).append(".set").append(cap(property)).append("(v, ")
                .append(code).append(");\n");
    }

    // ---------------------------------------------------------- coercion

    private static String fold(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c == '-' ? '_' : c);
        }
        return out.toString();
    }

    private Failure cannot(String text, Type target, Element at) {
        return new Failure(at, "\"" + text + "\" is not a " + java(target));
    }

    /// The code of `text` as a value of `target`.
    private String coerce(String text, Type target, Element at) throws Failure, IOException {
        String t = text.trim();
        switch (target.getSort()) {
            case Type.BOOLEAN:
                if ("true".equalsIgnoreCase(t) || "false".equalsIgnoreCase(t)) {
                    return "true".equalsIgnoreCase(t) ? "true" : "false";
                }
                throw cannot(text, target, at);
            case Type.CHAR:
                if (text.length() == 1) {
                    String quoted = literal(text);
                    return "'" + ("\"".equals(text) ? "\\\"" : "'".equals(text) ? "\\'"
                            : quoted.substring(1, quoted.length() - 1)) + "'";
                }
                throw cannot(text, target, at);
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.LONG:
                try {
                    long n = Long.parseLong(t);
                    if (target.getSort() == Type.LONG) {
                        return n + "L";
                    }
                    if (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE) {
                        throw cannot(text, target, at);
                    }
                    return target.getSort() == Type.INT ? String.valueOf(n) : "(" + java(target) + ") " + n;
                } catch (NumberFormatException e) {
                    throw cannot(text, target, at);
                }
            case Type.FLOAT:
                return "(float) " + number(t, target, at);
            case Type.DOUBLE:
                return number(t, target, at);
            case Type.OBJECT:
                return coerceObject(text, t, target, at);
            case Type.ARRAY:
                // A comma separated list is an array of a primitive, as the
                // dividerPositions of a SplitPane are.
                if (target.getDimensions() == 1 && target.getElementType().getSort() != Type.OBJECT) {
                    StringBuilder array = new StringBuilder("new " + java(target) + " {");
                    if (t.length() > 0) {
                        String[] parts = t.split(",", -1);
                        for (int i = 0; i < parts.length; i++) {
                            array.append(i > 0 ? ", " : "").append(coerce(parts[i], target.getElementType(), at));
                        }
                    }
                    return array.append('}').toString();
                }
                throw cannot(text, target, at);
            default:
                throw cannot(text, target, at);
        }
    }

    private String number(String t, Type target, Element at) throws Failure {
        if ("Infinity".equals(t) || "+Infinity".equals(t)) {
            return "Double.POSITIVE_INFINITY";
        }
        if ("-Infinity".equals(t)) {
            return "Double.NEGATIVE_INFINITY";
        }
        if ("NaN".equals(t)) {
            return "Double.NaN";
        }
        try {
            double d = Double.parseDouble(t);
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                throw cannot(t, target, at);
            }
            return Double.toString(d);
        } catch (NumberFormatException e) {
            throw cannot(t, target, at);
        }
    }

    private String coerceObject(String text, String t, Type target, Element at) throws Failure, IOException {
        String name = target.getInternalName();
        if ("java/lang/String".equals(name) || "java/lang/Object".equals(name)
                || "java/lang/CharSequence".equals(name)) {
            return literal(text);
        }
        if ("java/lang/Boolean".equals(name)) {
            return "true".equals(coerce(t, Type.BOOLEAN_TYPE, at)) ? "Boolean.TRUE" : "Boolean.FALSE";
        }
        if ("java/lang/Integer".equals(name) || "java/lang/Short".equals(name) || "java/lang/Byte".equals(name)) {
            return "Integer".equals(java(target).substring(10))
                    ? "Integer.valueOf(" + coerce(t, Type.INT_TYPE, at) + ")"
                    : java(target) + ".valueOf((" + ("java/lang/Short".equals(name) ? "short" : "byte") + ") "
                            + coerce(t, Type.INT_TYPE, at) + ")";
        }
        if ("java/lang/Long".equals(name)) {
            return "Long.valueOf(" + coerce(t, Type.LONG_TYPE, at) + ")";
        }
        if ("java/lang/Double".equals(name) || "java/lang/Number".equals(name)) {
            return "Double.valueOf(" + number(t, target, at) + ")";
        }
        if ("java/lang/Float".equals(name)) {
            return "Float.valueOf((float) " + number(t, target, at) + ")";
        }
        if ("java/lang/Character".equals(name)) {
            return "Character.valueOf(" + coerce(text, Type.CHAR_TYPE, at) + ")";
        }
        ClassModel.Info info = model.find(name);
        if (info == null) {
            throw new Failure(at, "\"" + text + "\" cannot be converted: the class " + java(target)
                    + " is not on the application's class path");
        }
        if (info.isEnum()) {
            String wanted = fold(t);
            for (ClassModel.Field f : info.fields) {
                if ((f.access & Opcodes.ACC_ENUM) != 0 && (f.name.equals(t) || fold(f.name).equals(wanted))) {
                    return info.sourceName() + "." + f.name;
                }
            }
            throw new Failure(at, "\"" + text + "\" is not a constant of " + java(target));
        }
        if ("javafx/geometry/Insets".equals(name)) {
            List<String> parts = numbers(t, target, at);
            if (parts.size() == 1) {
                return "new javafx.geometry.Insets(" + parts.get(0) + ")";
            }
            if (parts.size() == 4) {
                return "new javafx.geometry.Insets(" + parts.get(0) + ", " + parts.get(1) + ", " + parts.get(2)
                        + ", " + parts.get(3) + ")";
            }
            throw new Failure(at, "\"" + text + "\" is not an Insets: one number, or top right bottom left");
        }
        if ("javafx/scene/text/Font".equals(name)) {
            CssValue font = values.parse("-fx-font", t);
            if (font == null || font.type() != CssValue.FONT || font.unit(0) != CssValue.UNIT_PX) {
                throw new Failure(at, "\"" + text + "\" is not a font: write it as in a style sheet,"
                        + " such as \"bold 14px Arial\"");
            }
            int weight = font.flags() & 0xffff;
            return "javafx.scene.text.Font.font(" + literal(font.text()) + ", "
                    + (weight == 0 ? "javafx.scene.text.FontWeight.NORMAL"
                            : "javafx.scene.text.FontWeight.findByWeight(" + weight + ")")
                    + ", javafx.scene.text.FontPosture." + ((font.flags() >> 16) == 2 ? "ITALIC" : "REGULAR") + ", "
                    + Double.toString(font.num(0)) + ")";
        }
        if ("javafx/scene/paint/Color".equals(name) && values.constantColor(t) == null) {
            throw new Failure(at, "\"" + text + "\" is not a colour");
        }
        if ("javafx/scene/Cursor".equals(name)) {
            return "javafx.scene.Cursor.cursor(" + literal(t) + ")";
        }
        for (ClassModel.Method m : model.methods(name, "valueOf")) {
            if (m.isStatic() && m.parameters.length == 1 && isObject(m.parameters[0])
                    && "java/lang/String".equals(m.parameters[0].getInternalName())
                    && isObject(m.returnType())
                    && model.isAssignable(m.returnType().getInternalName(), name)) {
                return info.sourceName() + ".valueOf(" + literal(t) + ")";
            }
        }
        throw new Failure(at, "\"" + text + "\" cannot be converted to a " + java(target)
                + ": the class has no valueOf(String). Write the value as an element");
    }

    private List<String> numbers(String t, Type target, Element at) throws Failure {
        List<String> out = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i <= t.length(); i++) {
            char c = i < t.length() ? t.charAt(i) : ' ';
            if (c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r') {
                if (current.length() > 0) {
                    out.add(number(current.toString(), target, at));
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        return out;
    }
}

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
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessorContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// JSON codecs for the application's own classes, written at build time -- what
/// Jackson does for a Spring controller at run time, without reflection.
///
/// A route that returns or accepts an entity or a DTO gets, for that class and
/// every class its fields reach, a `<Name>Cn1Json` class in the same package with
/// a `write` and, where a body needs it, a `read`. The router calls them directly.
///
/// The JSON form is the one the app's `@Mapped` mapper uses, so one class shared
/// between the app and the server has one JSON form: the class's non-static,
/// non-transient fields -- a public one directly, any other through a
/// `getX`/`isX` and `setX` pair -- named by the field or `@JsonProperty`, and
/// `@JsonIgnore` leaving one out. A `Date` is milliseconds since the epoch (read
/// back from that or ISO-8601), a `byte[]` base64, an enum its name. Unknown
/// members in a body are ignored and absent ones leave the field's default, as a
/// Spring Boot application does.
final class BackendJsonCodecs {
    static final String JSON_PROPERTY = "Lcom/codename1/annotations/JsonProperty;";
    static final String JSON_IGNORE = "Lcom/codename1/annotations/JsonIgnore;";
    private static final String WRITABLE = "com/codename1/backend/Json$Writable";
    private static final String JSON = "com.codename1.backend.Json";
    private static final String CODEC = "com.codename1.backend.JsonCodec";
    private static final String SINK = "com.codename1.backend.ByteSink";

    private static final Set<String> PRIMITIVES = new HashSet<String>(Arrays.asList(
            "int", "long", "short", "byte", "double", "float", "boolean", "char"));
    private static final Set<String> BOXES = new HashSet<String>(Arrays.asList(
            "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
            "java.lang.Double", "java.lang.Float", "java.lang.Boolean", "java.lang.Character"));
    /// Collection declarations, and what a body's array becomes for each.
    private static final Map<String, String> COLLECTIONS = new LinkedHashMap<String, String>();
    /// Map declarations, and what a body's object becomes for each.
    private static final Map<String, String> MAPS = new LinkedHashMap<String, String>();

    static {
        COLLECTIONS.put("java.util.List", "java.util.ArrayList");
        COLLECTIONS.put("java.util.Collection", "java.util.ArrayList");
        COLLECTIONS.put("java.util.ArrayList", "java.util.ArrayList");
        COLLECTIONS.put("java.util.LinkedList", "java.util.LinkedList");
        COLLECTIONS.put("java.util.Set", "java.util.LinkedHashSet");
        COLLECTIONS.put("java.util.HashSet", "java.util.HashSet");
        COLLECTIONS.put("java.util.LinkedHashSet", "java.util.LinkedHashSet");
        COLLECTIONS.put("java.util.TreeSet", "java.util.TreeSet");
        MAPS.put("java.util.Map", "java.util.LinkedHashMap");
        MAPS.put("java.util.HashMap", "java.util.HashMap");
        MAPS.put("java.util.LinkedHashMap", "java.util.LinkedHashMap");
        MAPS.put("java.util.TreeMap", "java.util.TreeMap");
    }

    private enum Kind { PRIMITIVE, BOX, STRING, DATE, BYTES, ENUM, ANY, RAW_MAP, RAW_LIST,
        COLLECTION, MAP, DTO }

    /// One property of a class: its JSON name, declared type, and how to reach it.
    static final class Prop {
        String json;
        String type;
        /// How the writer reads it off `v` -- `v.name` or `v.getName()` -- or null.
        String get;
        /// The field the reader assigns directly, or null.
        String field;
        /// The setter the reader calls instead, or null.
        String setter;
    }

    /// A class that gets a codec.
    static final class Dto {
        final String binary;
        final AnnotatedClass cls;
        final List<Prop> props = new ArrayList<Prop>();
        final List<String> subclasses = new ArrayList<String>();
        String problem;
        String readProblem;
        boolean ownWriter;
        boolean write;
        boolean read;

        Dto(String binary, AnnotatedClass cls) {
            this.binary = binary;
            this.cls = cls;
        }
    }

    /// The generated class that writes a value whose declared type says nothing
    /// -- an `Object` or raw `Map` or `List` field -- by what it turns out to be.
    static final String VALUES = "cn1app.JsonValues";

    private final ProcessorContext ctx;
    private final Map<String, Dto> dtos = new LinkedHashMap<String, Dto>();
    private int counter;
    /// Whether any codec writes a value through [#VALUES].
    private boolean valuesUsed;

    BackendJsonCodecs(ProcessorContext ctx) {
        this.ctx = ctx;
    }

    private static final String ATTRIBUTE = "cn1.backend.jsonCodecs";

    /// The build's one set of codecs, shared by the router and the bean
    /// processors: an MCP tool and a route that use one class must use one codec,
    /// and the value dispatcher has to know every class either of them writes.
    static BackendJsonCodecs of(ProcessorContext ctx) {
        Object existing = ctx.getAttribute(ATTRIBUTE);
        if (existing instanceof BackendJsonCodecs) {
            return (BackendJsonCodecs) existing;
        }
        BackendJsonCodecs created = new BackendJsonCodecs(ctx);
        ctx.setAttribute(ATTRIBUTE, created);
        return created;
    }

    /// The JSON Schema type of a value this set reads into `type`, or "" when
    /// any JSON value will do.
    String schemaType(String type) {
        String t = strip(type);
        Kind kind = t == null ? null : kindOf(t);
        if (kind == null || kind == Kind.ANY) {
            return "";
        }
        switch (kind) {
            case COLLECTION:
            case RAW_LIST:
                return "array";
            case MAP:
            case RAW_MAP:
            case DTO:
                return "object";
            case PRIMITIVE:
            case BOX:
                if ("boolean".equals(t) || "java.lang.Boolean".equals(t)) {
                    return "boolean";
                }
                if ("double".equals(t) || "float".equals(t) || "java.lang.Double".equals(t)
                        || "java.lang.Float".equals(t)) {
                    return "number";
                }
                return "char".equals(t) || "java.lang.Character".equals(t) ? "string"
                        : "integer";
            default:
                return "string";
        }
    }

    /// `s` as a Java string literal.
    static String javaString(String s) {
        return quote(s);
    }

    /// Null when a value of `javaType` can be written as JSON, or why not.
    String checkWrite(String javaType) {
        return check(javaType, false, new HashSet<String>());
    }

    /// Null when a body can be read into `javaType`, or why not.
    String checkRead(String javaType) {
        return check(javaType, true, new HashSet<String>());
    }

    /// Whether any codec is needed at all.
    boolean isEmpty() {
        return dtos.isEmpty();
    }

    // ------------------------------------------------------------------
    // Analysis
    // ------------------------------------------------------------------

    private String check(String type, boolean read, Set<String> visiting) {
        type = strip(type);
        if (type == null) {
            return "a wildcard or type variable, which names no type the build can write "
                    + "code for; declare the concrete type";
        }
        Kind kind = kindOf(type);
        if (kind == null) {
            return whyNot(type);
        }
        switch (kind) {
            case COLLECTION: {
                List<String> args = args(type);
                if (read && "java.util.TreeSet".equals(raw(type))) {
                    // A TreeSet orders by compareTo, so the codec's first add of an
                    // element that is not Comparable fails -- and on the translated
                    // runtime a failed cast is not even an exception. Refused here.
                    String element = args.isEmpty() ? "java.lang.Object" : strip(args.get(0));
                    if (element == null || !isComparable(element)) {
                        return "a TreeSet of " + element + ", which is not Comparable, so a "
                                + "body cannot fill one; implement Comparable or use a Set";
                    }
                }
                return args.isEmpty() ? null : check(args.get(0), read, visiting);
            }
            case MAP: {
                List<String> args = args(type);
                if (args.isEmpty()) {
                    return null;
                }
                if (!"java.lang.String".equals(strip(args.get(0)))) {
                    return "a JSON object's names are strings, so a map in JSON is keyed by "
                            + "String, not " + args.get(0);
                }
                return check(args.get(1), read, visiting);
            }
            case DTO:
                return checkDto(raw(type), read, visiting);
            default:
                // Including Object and a raw Map or List, which are accepted on
                // purpose although their contents are only known at run time.
                // JsonValues writes each value by its class: one this build has
                // a codec for, a JDK shape, or else a 500 that names the class --
                // never its toString(). Writing EVERY application class, as
                // Jackson can, would mean a codec for each class in the module,
                // linked into every binary with an Object field; and refusing
                // these declarations would refuse the free-form maps and lists
                // handlers commonly return. A class meant to go out inside one
                // is declared with its type, which is what the 500 says.
                return null;
        }
    }

    private String checkDto(String binary, boolean read, Set<String> visiting) {
        Dto dto = dto(binary);
        if (dto.problem != null) {
            return dto.problem;
        }
        if (read) {
            dto.read = true;
            if (dto.readProblem != null) {
                return dto.readProblem;
            }
        } else {
            dto.write = true;
        }
        if (!visiting.add((read ? "r:" : "w:") + binary)) {
            return null;                 // a class reaching itself is fine; data decides
        }
        if (!read && dto.ownWriter) {
            return null;
        }
        for (Prop p : dto.props) {
            if (read ? p.field == null && p.setter == null : p.get == null) {
                continue;
            }
            String why = check(p.type, read, visiting);
            if (why != null) {
                return "field " + p.json + " of " + binary + " is " + why;
            }
        }
        if (!read) {
            for (String sub : dto.subclasses) {
                String why = checkDto(sub, false, visiting);
                if (why != null) {
                    return why;
                }
            }
        }
        return null;
    }

    private Dto dto(String binary) {
        Dto existing = dtos.get(binary);
        if (existing != null) {
            return existing;
        }
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                binary.replace('.', '/'));
        Dto dto = new Dto(binary, cls);
        dtos.put(binary, dto);
        if (cls == null) {
            dto.problem = binary + ", which the build cannot find on its class path to read "
                    + "its fields";
            return dto;
        }
        if (cls.isInterface()) {
            dto.problem = "the interface " + binary + "; a JSON object needs a class whose "
                    + "fields say what it holds";
            return dto;
        }
        if (isInnerClass(cls)) {
            dto.problem = "the inner class " + binary + ", which can only be created by an "
                    + "instance of its outer class; make it static";
            return dto;
        }
        dto.ownWriter = implementsWritable(cls, new HashSet<String>());
        collectProps(dto, cls);
        if (dto.problem != null) {
            return dto;
        }
        if (cls.isAbstract()) {
            dto.readProblem = "the abstract class " + binary + ", which a body cannot "
                    + "create; accept a concrete subclass";
        } else if (!hasNoArgConstructor(cls)) {
            dto.readProblem = binary + ", which has no constructor without arguments for a "
                    + "body to create it with; add one";
        }
        dto.subclasses.addAll(subclassesOf(binary));
        return dto;
    }

    private void collectProps(Dto dto, AnnotatedClass cls) {
        List<AnnotatedClass> chain = new ArrayList<AnnotatedClass>();
        for (AnnotatedClass c = cls; c != null; ) {
            chain.add(0, c);
            String parent = c.getSuperInternalName();
            if (parent == null || parent.startsWith("java/")) {
                break;
            }
            c = RestControllerAnnotationProcessor.resolveClass(ctx, parent);
        }
        String pkg = packageOf(dto.binary);
        Set<String> names = new HashSet<String>();
        for (AnnotatedClass c : chain) {
            for (FieldInfo f : c.getFields()) {
                int access = f.getAccess();
                if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT
                        | Opcodes.ACC_SYNTHETIC)) != 0 || f.getName().startsWith("this$")
                        || f.getAnnotation(JSON_IGNORE) != null) {
                    continue;
                }
                Prop p = new Prop();
                AnnotationValues renamed = f.getAnnotation(JSON_PROPERTY);
                String json = renamed == null ? null : renamed.getString("value");
                p.json = json == null || json.length() == 0 ? f.getName() : json;
                if (hasTypeVariable(f.getSignature())) {
                    dto.problem = "a class whose field " + f.getName() + " has a type variable "
                            + "for its type (" + c.getBinaryName() + "), so the build cannot "
                            + "know what it holds; declare a concrete subclass";
                    return;
                }
                p.type = RestClientAnnotationProcessor.javaTypeFor(
                        Type.getType(f.getDescriptor()), f.getSignature());
                // The codec lives in the class's own package, so a field it can
                // name is read and written directly; any other goes through the
                // JavaBeans accessors, as it does for the app's mapper.
                boolean reachable = f.isPublic() || (!f.isPrivate()
                        && packageOf(c.getBinaryName()).equals(pkg));
                String cap = Character.toUpperCase(f.getName().charAt(0))
                        + f.getName().substring(1);
                if (reachable) {
                    p.get = "v." + f.getName();
                } else {
                    String getter = findMethod(chain, "get" + cap, "()" + f.getDescriptor());
                    if (getter == null && "Z".equals(f.getDescriptor())) {
                        getter = findMethod(chain, "is" + cap, "()Z");
                    }
                    p.get = getter == null ? null : "v." + getter + "()";
                }
                if (reachable && !f.isFinal()) {
                    p.field = f.getName();
                } else {
                    p.setter = findMethod(chain, "set" + cap, "(" + f.getDescriptor() + ")V");
                }
                if (p.get == null && p.field == null && p.setter == null) {
                    continue;            // not a property, as for the app's mapper
                }
                if (!names.add(p.json)) {
                    dto.problem = "a class with two fields named \"" + p.json + "\" in JSON ("
                            + dto.binary + "); rename one with @JsonProperty";
                    return;
                }
                dto.props.add(p);
            }
        }
    }

    private static String findMethod(List<AnnotatedClass> chain, String name, String descriptor) {
        for (int i = chain.size() - 1; i >= 0; i--) {
            for (MethodInfo m : chain.get(i).getMethods()) {
                if (m.isPublic() && !m.isStatic() && name.equals(m.getName())
                        && descriptor.equals(m.getDescriptor())) {
                    return name;
                }
            }
        }
        return null;
    }

    private List<String> subclassesOf(String binary) {
        String internal = binary.replace('.', '/');
        List<String> out = new ArrayList<String>();
        final Map<String, Integer> depth = new LinkedHashMap<String, Integer>();
        for (AnnotatedClass c : ctx.getClassIndex().values()) {
            if (c.isSynthetic() || c.isInterface() || isAnonymous(c.getBinaryName())) {
                continue;
            }
            int d = 0;
            String parent = c.getSuperInternalName();
            while (parent != null && !parent.startsWith("java/")) {
                d++;
                if (parent.equals(internal)) {
                    out.add(c.getBinaryName());
                    depth.put(c.getBinaryName(), Integer.valueOf(d));
                    break;
                }
                AnnotatedClass up = ctx.lookup(parent);
                parent = up == null ? null : up.getSuperInternalName();
            }
        }
        // Most derived first, so each value is written as the class it really is.
        Collections.sort(out, (a, b) -> depth.get(b).compareTo(depth.get(a)));
        return out;
    }

    private boolean implementsWritable(AnnotatedClass cls, Set<String> seen) {
        return implementsInterface(cls, WRITABLE, seen);
    }

    private boolean implementsInterface(AnnotatedClass cls, String itfName, Set<String> seen) {
        if (cls == null) {
            return false;
        }
        for (String itf : cls.getInterfaceInternalNames()) {
            if (itfName.equals(itf)) {
                return true;
            }
            if (seen.add(itf) && implementsInterface(
                    RestControllerAnnotationProcessor.resolveClass(ctx, itf), itfName, seen)) {
                return true;
            }
        }
        String parent = cls.getSuperInternalName();
        if (parent == null || parent.startsWith("java/") || !seen.add(parent)) {
            return false;
        }
        return implementsInterface(RestControllerAnnotationProcessor.resolveClass(ctx, parent),
                itfName, seen);
    }

    /// Whether a TreeSet can order values of `type`.
    private boolean isComparable(String type) {
        Kind kind = kindOf(type);
        if (kind == Kind.STRING || kind == Kind.BOX || kind == Kind.DATE || kind == Kind.ENUM) {
            return true;
        }
        if (kind != Kind.DTO) {
            return false;
        }
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                raw(type).replace('.', '/'));
        return implementsInterface(cls, "java/lang/Comparable", new HashSet<String>());
    }

    private static boolean hasNoArgConstructor(AnnotatedClass cls) {
        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() && "()V".equals(m.getDescriptor()) && !m.isPrivate()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInnerClass(AnnotatedClass cls) {
        for (FieldInfo f : cls.getFields()) {
            if (f.getName().startsWith("this$") && !f.isStatic()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAnonymous(String binary) {
        int dollar = binary.lastIndexOf('$');
        return dollar >= 0 && dollar + 1 < binary.length()
                && Character.isDigit(binary.charAt(dollar + 1));
    }

    /// Whether a field's generic signature uses a type variable anywhere, which
    /// the signature parser erases to Object.
    static boolean hasTypeVariable(String signature) {
        if (signature == null) {
            return false;
        }
        for (int i = 0; i < signature.length(); i++) {
            char c = signature.charAt(i);
            if (c == 'T' && (i == 0 || "<;[+-".indexOf(signature.charAt(i - 1)) >= 0)) {
                return true;
            }
            if (c == 'L') {
                // Skip a class name, which may itself contain a T.
                while (i < signature.length() && signature.charAt(i) != ';'
                        && signature.charAt(i) != '<') {
                    i++;
                }
                if (i < signature.length() && signature.charAt(i) == '<') {
                    i--;                 // let the loop see the '<'
                }
            }
        }
        return false;
    }

    private Kind kindOf(String type) {
        if (PRIMITIVES.contains(type)) {
            return Kind.PRIMITIVE;
        }
        if (BOXES.contains(type)) {
            return Kind.BOX;
        }
        if ("java.lang.String".equals(type)) {
            return Kind.STRING;
        }
        if ("java.util.Date".equals(type)) {
            return Kind.DATE;
        }
        if ("byte[]".equals(type)) {
            return Kind.BYTES;
        }
        if ("java.lang.Object".equals(type)) {
            return Kind.ANY;
        }
        if (type.endsWith("[]")) {
            return null;
        }
        String raw = raw(type);
        boolean generic = type.indexOf('<') >= 0;
        if ("java.util.Map".equals(raw) && !generic) {
            return Kind.RAW_MAP;
        }
        if (("java.util.List".equals(raw) || "java.util.Collection".equals(raw)) && !generic) {
            return Kind.RAW_LIST;
        }
        if (COLLECTIONS.containsKey(raw)) {
            return Kind.COLLECTION;
        }
        if (MAPS.containsKey(raw)) {
            return Kind.MAP;
        }
        // The JDK's classes and the runtime's own -- an HttpServer.Response
        // inside a list -- are not data with fields to write; their fields are
        // the implementation's.
        if (raw.startsWith("java.") || raw.startsWith("com.codename1.backend.")) {
            return null;
        }
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                raw.replace('.', '/'));
        if (cls != null && cls.isEnum()) {
            return Kind.ENUM;
        }
        return Kind.DTO;
    }

    private static String whyNot(String type) {
        if (type.endsWith("[]")) {
            return "an array, which has no JSON form here other than byte[]; use a List";
        }
        if (type.startsWith("com.codename1.backend.")) {
            return "the runtime type " + type + ", which is not data to write as JSON";
        }
        return "the JDK type " + type + ", which has no JSON form here";
    }

    // ------------------------------------------------------------------
    // Code
    // ------------------------------------------------------------------

    /// Statements writing `expr`, a value of `type`, into the sink `out`, as a
    /// value nested `depth` objects deep.
    String writeStatements(String type, String expr, String depth, String indent) {
        StringBuilder sb = new StringBuilder();
        write(sb, strip(type), expr, depth, indent);
        return sb.toString();
    }

    /// Statements reading `json`, the parsed JSON at member `name` or element
    /// `index` of `at`, into `target`, which is already declared.
    String readStatements(String type, String json, String at, String name, String index,
                          String depth, String target, String indent) {
        StringBuilder sb = new StringBuilder();
        read(sb, strip(type), json, at, name, index, depth, target, indent);
        return sb.toString();
    }

    private void write(StringBuilder sb, String type, String expr, String depth, String ind) {
        Kind kind = kindOf(type);
        switch (kind) {
            case PRIMITIVE:
                if ("double".equals(type)) {
                    sb.append(ind).append(JSON).append(".writeValue(Double.valueOf(").append(expr)
                      .append("), out);\n");
                } else if ("float".equals(type)) {
                    sb.append(ind).append(JSON).append(".writeValue(Float.valueOf(").append(expr)
                      .append("), out);\n");
                } else if ("boolean".equals(type)) {
                    sb.append(ind).append("out.putAscii((").append(expr)
                      .append(") ? \"true\" : \"false\");\n");
                } else if ("char".equals(type)) {
                    sb.append(ind).append(JSON).append(".writeString(String.valueOf(").append(expr)
                      .append("), out);\n");
                } else {
                    sb.append(ind).append("out.putNumber(").append(expr).append(");\n");
                }
                return;
            case BOX:
            case STRING:
                sb.append(ind).append(JSON).append(".writeValue(").append(expr)
                  .append(", out);\n");
                return;
            case ANY:
            case RAW_MAP:
            case RAW_LIST:
                // Its declared type says nothing, and Json.writeValue knows no
                // generated codec: an application object in here would be written
                // as its toString(). The dispatcher asks what it is at run time.
                valuesUsed = true;
                sb.append(ind).append(VALUES).append(".write(").append(expr)
                  .append(", out, ").append(depth).append(");\n");
                return;
            case DATE:
                sb.append(ind).append(CODEC).append(".writeDate(").append(expr)
                  .append(", out);\n");
                return;
            case BYTES: {
                String v = fresh("b");
                sb.append(ind).append("byte[] ").append(v).append(" = ").append(expr).append(";\n");
                sb.append(ind).append("if (").append(v).append(" == null) {\n");
                sb.append(ind).append("    out.putAscii(\"null\");\n");
                sb.append(ind).append("} else {\n");
                sb.append(ind).append("    ").append(JSON).append(".writeString(")
                  .append("com.codename1.backend.Base64.encode(").append(v).append("), out);\n");
                sb.append(ind).append("}\n");
                return;
            }
            case ENUM: {
                String v = fresh("e");
                sb.append(ind).append(source(type)).append(' ').append(v).append(" = ")
                  .append(expr).append(";\n");
                sb.append(ind).append("if (").append(v).append(" == null) {\n");
                sb.append(ind).append("    out.putAscii(\"null\");\n");
                sb.append(ind).append("} else {\n");
                sb.append(ind).append("    ").append(JSON).append(".writeString(").append(v)
                  .append(".name(), out);\n");
                sb.append(ind).append("}\n");
                return;
            }
            case DTO:
                sb.append(ind).append(codecSource(raw(type))).append(".write(").append(expr)
                  .append(", out, ").append(depth).append(" + 1);\n");
                return;
            case COLLECTION: {
                String element = elementType(type, 0);
                String c = fresh("c");
                String first = fresh("f");
                String it = fresh("i");
                String e = fresh("e");
                sb.append(ind).append("java.util.Collection ").append(c).append(" = ")
                  .append(expr).append(";\n");
                sb.append(ind).append("if (").append(c).append(" == null) {\n");
                sb.append(ind).append("    out.putAscii(\"null\");\n");
                sb.append(ind).append("} else {\n");
                sb.append(ind).append("    out.put('[');\n");
                sb.append(ind).append("    boolean ").append(first).append(" = true;\n");
                sb.append(ind).append("    for (java.util.Iterator ").append(it).append(" = ")
                  .append(c).append(".iterator(); ").append(it).append(".hasNext();) {\n");
                sb.append(ind).append("        Object ").append(e).append(" = ").append(it)
                  .append(".next();\n");
                sb.append(ind).append("        if (").append(first).append(") {\n");
                sb.append(ind).append("            ").append(first).append(" = false;\n");
                sb.append(ind).append("        } else {\n");
                sb.append(ind).append("            out.put(',');\n");
                sb.append(ind).append("        }\n");
                write(sb, element, cast(element, e), depth, ind + "        ");
                sb.append(ind).append("    }\n");
                sb.append(ind).append("    out.put(']');\n");
                sb.append(ind).append("}\n");
                return;
            }
            case MAP: {
                String value = elementType(type, 1);
                String m = fresh("m");
                String first = fresh("f");
                String it = fresh("i");
                String e = fresh("e");
                String v = fresh("v");
                sb.append(ind).append("java.util.Map ").append(m).append(" = ").append(expr)
                  .append(";\n");
                sb.append(ind).append("if (").append(m).append(" == null) {\n");
                sb.append(ind).append("    out.putAscii(\"null\");\n");
                sb.append(ind).append("} else {\n");
                sb.append(ind).append("    out.put('{');\n");
                sb.append(ind).append("    boolean ").append(first).append(" = true;\n");
                sb.append(ind).append("    for (java.util.Iterator ").append(it).append(" = ")
                  .append(m).append(".entrySet().iterator(); ").append(it)
                  .append(".hasNext();) {\n");
                sb.append(ind).append("        java.util.Map.Entry ").append(e)
                  .append(" = (java.util.Map.Entry) ").append(it).append(".next();\n");
                sb.append(ind).append("        if (").append(first).append(") {\n");
                sb.append(ind).append("            ").append(first).append(" = false;\n");
                sb.append(ind).append("        } else {\n");
                sb.append(ind).append("            out.put(',');\n");
                sb.append(ind).append("        }\n");
                sb.append(ind).append("        ").append(JSON).append(".writeString(String.valueOf(")
                  .append(e).append(".getKey()), out);\n");
                sb.append(ind).append("        out.put(':');\n");
                sb.append(ind).append("        Object ").append(v).append(" = ").append(e)
                  .append(".getValue();\n");
                write(sb, value, cast(value, v), depth, ind + "        ");
                sb.append(ind).append("    }\n");
                sb.append(ind).append("    out.put('}');\n");
                sb.append(ind).append("}\n");
                return;
            }
            default:
                throw new IllegalStateException(type);
        }
    }

    private void read(StringBuilder sb, String type, String json, String at, String name,
                      String index, String depth, String target, String ind) {
        Kind kind = kindOf(type);
        String where = at + ", " + name + ", " + index;
        switch (kind) {
            case PRIMITIVE:
                sb.append(ind).append("if (").append(json).append(" != null) {\n");
                sb.append(ind).append("    ").append(target).append(" = ")
                  .append(scalar(type, json, where)).append(";\n");
                sb.append(ind).append("}\n");
                return;
            case BOX: {
                String prim = unbox(type);
                sb.append(ind).append(target).append(" = ").append(json).append(" == null ? null : ")
                  .append(type).append(".valueOf(").append(scalar(prim, json, where))
                  .append(");\n");
                return;
            }
            case STRING:
                sb.append(ind).append(target).append(" = ").append(CODEC).append(".readString(")
                  .append(json).append(", ").append(where).append(");\n");
                return;
            case DATE:
                sb.append(ind).append(target).append(" = ").append(CODEC).append(".readDate(")
                  .append(json).append(", ").append(where).append(");\n");
                return;
            case BYTES:
                sb.append(ind).append(target).append(" = ").append(CODEC).append(".readBytes(")
                  .append(json).append(", ").append(where).append(");\n");
                return;
            case ANY:
                sb.append(ind).append(target).append(" = ").append(json).append(";\n");
                return;
            case RAW_MAP:
            case RAW_LIST: {
                String shape = kind == Kind.RAW_MAP ? "java.util.Map" : "java.util.List";
                sb.append(ind).append("if (").append(json).append(" != null && !(").append(json)
                  .append(" instanceof ").append(shape).append(")) {\n");
                sb.append(ind).append("    throw ").append(CODEC).append(".mismatch(").append(where)
                  .append(", \"").append(kind == Kind.RAW_MAP ? "an object" : "an array")
                  .append("\", ").append(json).append(");\n");
                sb.append(ind).append("}\n");
                sb.append(ind).append(target).append(" = (").append(shape).append(") ")
                  .append(json).append(";\n");
                return;
            }
            case ENUM: {
                String s = fresh("s");
                String err = fresh("x");
                sb.append(ind).append("if (").append(json).append(" == null) {\n");
                sb.append(ind).append("    ").append(target).append(" = null;\n");
                sb.append(ind).append("} else {\n");
                sb.append(ind).append("    if (!(").append(json).append(" instanceof String)) {\n");
                sb.append(ind).append("        throw ").append(CODEC).append(".mismatch(")
                  .append(where).append(", ").append(quote(enumChoices(type))).append(", ")
                  .append(json).append(");\n");
                sb.append(ind).append("    }\n");
                sb.append(ind).append("    String ").append(s).append(" = (String) ").append(json)
                  .append(";\n");
                sb.append(ind).append("    try {\n");
                sb.append(ind).append("        ").append(target).append(" = ").append(source(type))
                  .append(".valueOf(").append(s).append(");\n");
                sb.append(ind).append("    } catch (IllegalArgumentException ").append(err)
                  .append(") {\n");
                sb.append(ind).append("        throw ").append(CODEC).append(".mismatch(")
                  .append(where).append(", ").append(quote(enumChoices(type))).append(", ")
                  .append(json).append(");\n");
                sb.append(ind).append("    }\n");
                sb.append(ind).append("}\n");
                return;
            }
            case DTO:
                sb.append(ind).append(target).append(" = ").append(codecSource(raw(type)))
                  .append(".read(").append(json).append(", ").append(where).append(", ")
                  .append(depth).append(" + 1);\n");
                return;
            case COLLECTION:
            case MAP: {
                boolean map = kind == Kind.MAP;
                String element = elementType(type, map ? 1 : 0);
                String concrete = (map ? MAPS : COLLECTIONS).get(raw(type));
                String declared = map ? "java.util.Map" : "java.util.List";
                String src = fresh("l");
                String path = fresh("p");
                String out = fresh("c");
                String loop = fresh("i");
                String item = fresh("j");
                String value = fresh("v");
                String key = fresh("k");
                sb.append(ind).append("if (").append(json).append(" == null) {\n");
                sb.append(ind).append("    ").append(target).append(" = null;\n");
                sb.append(ind).append("} else {\n");
                sb.append(ind).append("    if (!(").append(json).append(" instanceof ")
                  .append(declared).append(")) {\n");
                sb.append(ind).append("        throw ").append(CODEC).append(".mismatch(")
                  .append(where).append(", \"").append(map ? "an object" : "an array")
                  .append("\", ").append(json).append(");\n");
                sb.append(ind).append("    }\n");
                sb.append(ind).append("    ").append(declared).append(' ').append(src)
                  .append(" = (").append(declared).append(") ").append(json).append(";\n");
                sb.append(ind).append("    ").append(CODEC).append(".Path ").append(path)
                  .append(" = ").append(CODEC).append(".enter(").append(where).append(");\n");
                String elementSource = source(element);
                String generic = map ? "<String, " + elementSource + ">" : "<" + elementSource + ">";
                sb.append(ind).append("    ").append(concrete).append(generic).append(' ')
                  .append(out).append(" = new ").append(concrete).append(generic).append("();\n");
                if (map) {
                    sb.append(ind).append("    for (java.util.Iterator ").append(loop)
                      .append(" = ").append(src).append(".entrySet().iterator(); ").append(loop)
                      .append(".hasNext();) {\n");
                    sb.append(ind).append("        java.util.Map.Entry ").append(value)
                      .append("$e = (java.util.Map.Entry) ").append(loop).append(".next();\n");
                    sb.append(ind).append("        String ").append(key).append(" = String.valueOf(")
                      .append(value).append("$e.getKey());\n");
                    sb.append(ind).append("        Object ").append(item).append(" = ")
                      .append(value).append("$e.getValue();\n");
                    sb.append(ind).append("        ").append(elementSource).append(' ')
                      .append(value).append(" = null;\n");
                    read(sb, element, item, path, key, "-1", depth, value, ind + "        ");
                    sb.append(ind).append("        ").append(out).append(".put(").append(key)
                      .append(", ").append(value).append(");\n");
                } else {
                    sb.append(ind).append("    for (int ").append(loop).append(" = 0; ")
                      .append(loop).append(" < ").append(src).append(".size(); ").append(loop)
                      .append("++) {\n");
                    sb.append(ind).append("        Object ").append(item).append(" = ").append(src)
                      .append(".get(").append(loop).append(");\n");
                    sb.append(ind).append("        ").append(elementSource).append(' ')
                      .append(value).append(" = null;\n");
                    read(sb, element, item, path, "null", loop, depth, value, ind + "        ");
                    sb.append(ind).append("        ").append(out).append(".add(").append(value)
                      .append(");\n");
                }
                sb.append(ind).append("    }\n");
                sb.append(ind).append("    ").append(target).append(" = ").append(out).append(";\n");
                sb.append(ind).append("}\n");
                return;
            }
            default:
                throw new IllegalStateException(type);
        }
    }

    private static String scalar(String prim, String json, String where) {
        String read = CODEC + ".readLong(" + json + ", " + where + ", ";
        if ("int".equals(prim)) {
            return "(int) " + read + "Integer.MIN_VALUE, Integer.MAX_VALUE)";
        }
        if ("long".equals(prim)) {
            return read + "Long.MIN_VALUE, Long.MAX_VALUE)";
        }
        if ("short".equals(prim)) {
            return "(short) " + read + "Short.MIN_VALUE, Short.MAX_VALUE)";
        }
        if ("byte".equals(prim)) {
            return "(byte) " + read + "Byte.MIN_VALUE, Byte.MAX_VALUE)";
        }
        if ("double".equals(prim)) {
            return CODEC + ".readDouble(" + json + ", " + where + ")";
        }
        if ("float".equals(prim)) {
            return CODEC + ".readFloat(" + json + ", " + where + ")";
        }
        if ("boolean".equals(prim)) {
            return CODEC + ".readBoolean(" + json + ", " + where + ")";
        }
        return CODEC + ".readChar(" + json + ", " + where + ")";
    }

    private static String unbox(String box) {
        if ("java.lang.Integer".equals(box)) {
            return "int";
        }
        if ("java.lang.Character".equals(box)) {
            return "char";
        }
        return box.substring("java.lang.".length()).toLowerCase(java.util.Locale.ROOT);
    }

    private String enumChoices(String type) {
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                raw(type).replace('.', '/'));
        StringBuilder out = new StringBuilder("one of");
        String sep = " ";
        if (cls != null) {
            for (FieldInfo f : cls.getFields()) {
                if ((f.getAccess() & Opcodes.ACC_ENUM) != 0) {
                    out.append(sep).append(f.getName());
                    sep = ", ";
                }
            }
        }
        return out.toString();
    }

    /// `var`, an Object, as a value of `type` for the write code: cast only where
    /// that code needs the static type, and each cast is of a value the typed
    /// collection it came out of holds.
    private String cast(String type, String var) {
        Kind kind = kindOf(type);
        switch (kind) {
            case DATE:
            case BYTES:
            case ENUM:
            case DTO:
                return "((" + source(raw(type)) + ") " + var + ")";
            case COLLECTION:
                return "((java.util.Collection) " + var + ")";
            case MAP:
                return "((java.util.Map) " + var + ")";
            default:
                return var;
        }
    }

    // ------------------------------------------------------------------
    // Codec classes
    // ------------------------------------------------------------------

    /// The codec classes the checks above asked for, by binary name, and the
    /// value dispatcher when one of them needs it.
    Map<String, String> sources() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Dto dto : dtos.values()) {
            if (dto.problem != null || (!dto.write && !dto.read)) {
                continue;
            }
            out.put(codecBinary(dto.binary), codecSource(dto));
        }
        if (valuesUsed) {
            out.put(VALUES, valuesSource());
        }
        return out;
    }

    /// `cn1app.JsonValues`: writes a value by its run-time class. Each of the
    /// application's classes this build writes goes through its codec, the JDK's
    /// JSON shapes through Json, and anything else is refused with an exception
    /// the server answers with 500 -- never written as its toString(), which
    /// would ship a quoted class name as data.
    private String valuesSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("package cn1app;\n\n");
        sb.append("// Generated: writes a value by what it is. Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class JsonValues {\n");
        sb.append("    private JsonValues() {\n    }\n\n");
        sb.append("    public static void write(Object v, ").append(SINK)
          .append(" out, int depth) {\n");
        sb.append("        if (v == null) {\n");
        sb.append("            out.putAscii(\"null\");\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        if (depth > ").append(CODEC).append(".MAX_DEPTH) {\n");
        sb.append("            throw ").append(CODEC).append(".tooDeep(\"value\");\n");
        sb.append("        }\n");
        for (Dto dto : dtos.values()) {
            if (dto.problem != null || !dto.write) {
                continue;
            }
            // Each codec dispatches to its own subclasses, so the order here does
            // not decide which fields a subclass instance is written with.
            String type = source(dto.binary);
            sb.append("        if (v instanceof ").append(type).append(") {\n");
            sb.append("            ").append(codecBinary(dto.binary)).append(".write((")
              .append(type).append(") v, out, depth);\n");
            sb.append("            return;\n");
            sb.append("        }\n");
        }
        sb.append("        if (v instanceof java.util.Map) {\n");
        sb.append("            out.put('{');\n");
        sb.append("            boolean first = true;\n");
        sb.append("            for (java.util.Iterator it = ((java.util.Map) v).entrySet().iterator(); "
                + "it.hasNext();) {\n");
        sb.append("                java.util.Map.Entry e = (java.util.Map.Entry) it.next();\n");
        sb.append("                if (first) {\n");
        sb.append("                    first = false;\n");
        sb.append("                } else {\n");
        sb.append("                    out.put(',');\n");
        sb.append("                }\n");
        sb.append("                ").append(JSON)
          .append(".writeString(String.valueOf(e.getKey()), out);\n");
        sb.append("                out.put(':');\n");
        sb.append("                write(e.getValue(), out, depth + 1);\n");
        sb.append("            }\n");
        sb.append("            out.put('}');\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        if (v instanceof java.util.Collection) {\n");
        sb.append("            out.put('[');\n");
        sb.append("            boolean first = true;\n");
        sb.append("            for (java.util.Iterator it = ((java.util.Collection) v).iterator(); "
                + "it.hasNext();) {\n");
        sb.append("                if (first) {\n");
        sb.append("                    first = false;\n");
        sb.append("                } else {\n");
        sb.append("                    out.put(',');\n");
        sb.append("                }\n");
        sb.append("                write(it.next(), out, depth + 1);\n");
        sb.append("            }\n");
        sb.append("            out.put(']');\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        if (v instanceof java.util.Date) {\n");
        sb.append("            ").append(CODEC).append(".writeDate((java.util.Date) v, out);\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        if (v instanceof Enum) {\n");
        sb.append("            ").append(JSON).append(".writeString(((Enum) v).name(), out);\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        if (v instanceof String || v instanceof Number || v instanceof Boolean\n");
        sb.append("                || v instanceof Character || v instanceof byte[]\n");
        sb.append("                || v instanceof ").append(JSON).append(".Writable) {\n");
        sb.append("            ").append(JSON).append(".writeValue(v, out);\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        throw new IllegalStateException(v.getClass().getName() + \" has no JSON \"\n");
        sb.append("                + \"form: it is not one of the classes this build writes a codec \"\n");
        sb.append("                + \"for. Declare the field or element with its type.\");\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String codecSource(Dto dto) {
        String pkg = packageOf(dto.binary);
        String simple = simpleOf(codecBinary(dto.binary));
        String type = source(dto.binary);
        StringBuilder sb = new StringBuilder();
        if (pkg.length() > 0) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("// Generated: the JSON form of ").append(type).append(". Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(simple).append(" {\n");
        sb.append("    private ").append(simple).append("() {\n    }\n");
        if (dto.write) {
            sb.append("\n    public static void write(").append(type).append(" v, ").append(SINK)
              .append(" out, int depth) {\n");
            sb.append("        if (v == null) {\n");
            sb.append("            out.putAscii(\"null\");\n");
            sb.append("            return;\n");
            sb.append("        }\n");
            sb.append("        if (depth > ").append(CODEC).append(".MAX_DEPTH) {\n");
            sb.append("            throw ").append(CODEC).append(".tooDeep(")
              .append(quote(simpleOf(dto.binary))).append(");\n");
            sb.append("        }\n");
            for (String sub : dto.subclasses) {
                String subSource = source(sub);
                sb.append("        if (v instanceof ").append(subSource).append(") {\n");
                sb.append("            ").append(codecSource(sub)).append(".write((")
                  .append(subSource).append(") v, out, depth);\n");
                sb.append("            return;\n");
                sb.append("        }\n");
            }
            if (dto.ownWriter) {
                sb.append("        v.writeTo(out);\n");
            } else {
                sb.append("        out.put('{');\n");
                boolean first = true;
                for (Prop p : dto.props) {
                    String get = p.get;
                    if (get == null) {
                        continue;
                    }
                    sb.append("        ");
                    if (plainName(p.json)) {
                        sb.append("out.putAscii(\"").append(first ? "" : ",").append("\\\"")
                          .append(p.json).append("\\\":\");\n");
                    } else {
                        if (!first) {
                            sb.append("out.put(',');\n        ");
                        }
                        sb.append(JSON).append(".writeString(").append(quote(p.json))
                          .append(", out);\n        out.put(':');\n");
                    }
                    write(sb, strip(p.type), get, "depth", "        ");
                    first = false;
                }
                sb.append("        out.put('}');\n");
            }
            sb.append("    }\n");
        }
        if (dto.read && dto.readProblem == null) {
            sb.append("\n    public static ").append(type).append(" read(Object json, ")
              .append(CODEC).append(".Path at, String name, int index, int depth) {\n");
            sb.append("        if (json == null) {\n");
            sb.append("            return null;\n");
            sb.append("        }\n");
            sb.append("        if (!(json instanceof java.util.Map) || depth > ").append(CODEC)
              .append(".MAX_DEPTH) {\n");
            sb.append("            throw ").append(CODEC)
              .append(".mismatch(at, name, index, \"an object\", json);\n");
            sb.append("        }\n");
            sb.append("        java.util.Map m = (java.util.Map) json;\n");
            sb.append("        ").append(CODEC).append(".Path here = ").append(CODEC)
              .append(".enter(at, name, index);\n");
            sb.append("        ").append(type).append(" v = new ").append(type).append("();\n");
            for (Prop p : dto.props) {
                boolean direct = p.field != null;
                if (!direct && p.setter == null) {
                    continue;
                }
                String t = strip(p.type);
                String j = fresh("j");
                String tmp = fresh("t");
                boolean primitive = PRIMITIVES.contains(t);
                sb.append("        Object ").append(j).append(" = m.get(").append(quote(p.json))
                  .append(");\n");
                if (primitive) {
                    // null, like an absent member, leaves the field's default --
                    // Jackson's answer for a primitive too.
                    String value = scalar(t, j, "here, " + quote(p.json) + ", -1");
                    sb.append("        if (").append(j).append(" != null) {\n");
                    sb.append("            ").append(direct ? "v." + p.field + " = " + value + ";"
                            : "v." + p.setter + "(" + value + ");").append("\n");
                    sb.append("        }\n");
                    continue;
                }
                sb.append("        if (").append(j).append(" != null || m.containsKey(")
                  .append(quote(p.json)).append(")) {\n");
                sb.append("            ").append(source(t)).append(' ').append(tmp).append(" = ")
                  .append(defaultOf(t)).append(";\n");
                read(sb, t, j, "here", quote(p.json), "-1", "depth", tmp, "            ");
                sb.append("            ").append(direct ? "v." + p.field + " = " + tmp + ";"
                        : "v." + p.setter + "(" + tmp + ");").append("\n");
                sb.append("        }\n");
            }
            sb.append("        return v;\n");
            sb.append("    }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static boolean plainName(String name) {
        if (name.length() == 0) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')
                    && c != '_' && c != '-' && c != '$' && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static String defaultOf(String type) {
        if ("boolean".equals(type)) {
            return "false";
        }
        if ("char".equals(type)) {
            return "'\\0'";
        }
        return PRIMITIVES.contains(type) ? "0" : "null";
    }

    private static String codecBinary(String binary) {
        String pkg = packageOf(binary);
        String simple = binary.substring(pkg.length() == 0 ? 0 : pkg.length() + 1)
                .replace('$', '_') + "Cn1Json";
        return pkg.length() == 0 ? simple : pkg + "." + simple;
    }

    private static String codecSource(String binary) {
        return codecBinary(binary);
    }

    private String fresh(String prefix) {
        return prefix + (counter++) + "$";
    }

    // ------------------------------------------------------------------
    // Type strings
    // ------------------------------------------------------------------

    /// The type with a `? extends` bound reduced to the bound; null for a bare
    /// wildcard or a `? super` one, which name nothing to write code for.
    private static String strip(String type) {
        if (type == null) {
            return null;
        }
        String t = type.trim();
        if (t.startsWith("? extends ")) {
            return strip(t.substring("? extends ".length()));
        }
        if (t.startsWith("?")) {
            return null;
        }
        return t;
    }

    private static String raw(String type) {
        int lt = type.indexOf('<');
        return lt < 0 ? type : type.substring(0, lt);
    }

    private static List<String> args(String type) {
        int lt = type.indexOf('<');
        int gt = type.lastIndexOf('>');
        if (lt < 0 || gt <= lt) {
            return Collections.emptyList();
        }
        return RestControllerAnnotationProcessor.splitTypeArguments(type.substring(lt + 1, gt));
    }

    /// The `i`th type argument, or Object for a raw declaration.
    private static String elementType(String type, int i) {
        List<String> a = args(type);
        if (a.size() <= i) {
            return "java.lang.Object";
        }
        String s = strip(a.get(i));
        return s == null ? "java.lang.Object" : s;
    }

    /// A type as Java source spells it: nested classes with a dot.
    static String source(String type) {
        return type.replace('$', '.');
    }

    private static String packageOf(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? "" : binary.substring(0, dot);
    }

    private static String simpleOf(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? binary : binary.substring(dot + 1);
    }

    private static String quote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                out.append(String.format("\\u%04x", Integer.valueOf(c)));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }
}

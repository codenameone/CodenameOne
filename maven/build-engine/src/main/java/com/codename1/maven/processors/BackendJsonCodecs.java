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
    static final String JSON_CREATOR = "Lcom/codename1/annotations/JsonCreator;";
    static final String JSON_STRICT = "Lcom/codename1/annotations/JsonStrict;";
    static final String JSON_OMIT_NULL = "Lcom/codename1/annotations/JsonOmitNull;";
    static final String JSON_FORMAT = "Lcom/codename1/annotations/JsonFormat;";
    static final String JSON_ADAPTER = "Lcom/codename1/annotations/JsonAdapter;";
    private static final String TYPE_ADAPTER = "com/codename1/annotations/JsonTypeAdapter";
    private static final String WRITABLE = "com/codename1/backend/Json$Writable";
    private static final String JSON = "com.codename1.backend.Json";
    private static final String CODEC = "com.codename1.impl.backend.JsonCodec";
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
        COLLECTION, MAP, DTO, ARRAY, ADAPTED }

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
        /// A record component: the reader passes it to the canonical constructor.
        boolean component;
        /// Further names a body may carry it under (`@JsonProperty(aliases)`).
        final List<String> aliases = new ArrayList<String>();
        /// `@JsonProperty(required = true)`: a body without it is refused.
        boolean required;
        /// `@JsonOmitNull`, on the field or its class: null is left out.
        boolean omitNull;
        /// `@JsonFormat(STRING)`: a number in quotes, a date in ISO-8601.
        boolean asString;
        /// The field's own `@JsonAdapter`, by binary name, or null.
        String adapter;
    }

    /// The `@JsonCreator` constructor or factory of a class, or a record's
    /// canonical constructor: what a body is read into all at once.
    static final class Creator {
        /// `<init>`, or the static factory's name.
        String method;
        /// Its parameters, in order, each read from the member it names.
        final List<Prop> params = new ArrayList<Prop>();
    }

    /// A class that gets a codec.
    static final class Dto {
        /// The class, or a parameterized use of a generic one (`a.Page<a.User>`),
        /// which gets a codec of its own.
        final String key;
        final String binary;
        final AnnotatedClass cls;
        /// The class's type variables, as the key's arguments bind them.
        final Map<String, String> vars = new LinkedHashMap<String, String>();
        Creator creator;
        /// `@JsonStrict`: an unknown member is refused.
        boolean strict;
        final List<Prop> props = new ArrayList<Prop>();
        final List<String> subclasses = new ArrayList<String>();
        String problem;
        String readProblem;
        boolean ownWriter;
        boolean write;
        boolean read;

        Dto(String key, AnnotatedClass cls) {
            this.key = key;
            this.binary = raw(key);
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
    /// The application's `JsonTypeAdapter` classes the codecs call, each by the
    /// field of [#ADAPTERS] that holds its one instance.
    private final Map<String, String> adapters = new LinkedHashMap<String, String>();

    /// The generated class that holds one instance of each `JsonTypeAdapter`.
    static final String ADAPTERS = "cn1app.JsonAdapters";

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
        if (kind == null || kind == Kind.ANY || kind == Kind.ADAPTED) {
            return "";              // an adapter's form is whatever it returns
        }
        switch (kind) {
            case COLLECTION:
            case RAW_LIST:
            case ARRAY:
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
                return checkDto(dtoKey(type), read, visiting);
            case ARRAY: {
                String element = type.substring(0, type.length() - 2);
                if (element.endsWith("[]") && !"byte[]".equals(element)) {
                    return "the array " + type + ", which has more than one dimension; use a "
                            + "List of arrays";
                }
                if (element.indexOf('<') >= 0) {
                    return "an array of " + element + ", which Java cannot create for a body "
                            + "to fill; use a List";
                }
                return check(element, read, visiting);
            }
            case ADAPTED:
                return adapterProblem(classAdapter(type), type);
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
            if (read ? p.field == null && p.setter == null && !p.component : p.get == null) {
                continue;
            }
            String why = p.adapter != null ? adapterProblem(p.adapter, p.type)
                    : check(p.type, read, visiting);
            if (why != null) {
                return "field " + p.json + " of " + binary + " is " + why;
            }
        }
        if (read && dto.creator != null) {
            for (Prop p : dto.creator.params) {
                String why = check(p.type, true, visiting);
                if (why != null) {
                    return "parameter " + p.json + " of the @JsonCreator of " + binary + " is "
                            + why;
                }
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

    /// What a DTO type is keyed by: the raw class, or for a generic class used
    /// with its type arguments the whole type, so `Page<User>` and `Page<Order>`
    /// each get a codec that knows what their items are.
    private String dtoKey(String type) {
        String raw = raw(type);
        List<String> args = args(type);
        if (args.isEmpty()) {
            return raw;
        }
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                raw.replace('.', '/'));
        if (cls == null || typeParameters(cls.getSignature()).size() != args.size()) {
            return raw;
        }
        StringBuilder key = new StringBuilder(raw).append('<');
        for (int i = 0; i < args.size(); i++) {
            String a = strip(args.get(i));
            key.append(i == 0 ? "" : ", ").append(a == null ? "?" : a);
        }
        return key.append('>').toString();
    }

    private Dto dto(String key) {
        Dto existing = dtos.get(key);
        if (existing != null) {
            return existing;
        }
        String binary = raw(key);
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                binary.replace('.', '/'));
        Dto dto = new Dto(key, cls);
        dtos.put(key, dto);
        if (cls != null) {
            List<String> params = typeParameters(cls.getSignature());
            List<String> args = args(key);
            for (int i = 0; i < params.size() && i < args.size(); i++) {
                if (!"?".equals(args.get(i))) {
                    dto.vars.put(params.get(i), args.get(i));
                }
            }
        }
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
        dto.strict = cls.getClassAnnotation(JSON_STRICT) != null;
        collectProps(dto, cls);
        if (dto.problem != null) {
            return dto;
        }
        if (!cls.isRecord()) {
            collectCreator(dto, cls);
        }
        // A refused creator is a read problem of its own; writing is unaffected.
        if (dto.readProblem == null && cls.isAbstract()) {
            dto.readProblem = "the abstract class " + binary + ", which a body cannot "
                    + "create; accept a concrete subclass";
        } else if (dto.readProblem == null && !cls.isRecord() && dto.creator == null
                && !hasNoArgConstructor(cls)) {
            dto.readProblem = binary + ", which has no constructor without arguments for a "
                    + "body to create it with; add one, or mark the constructor to read it "
                    + "with @JsonCreator";
        }
        for (String sub : subclassesOf(binary)) {
            // A generic subclass would need its own type arguments, which a value
            // declared as this class does not give; one that binds them all is
            // concrete and is written as itself.
            AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(ctx,
                    sub.replace('.', '/'));
            if (dto.vars.isEmpty() || c == null || typeParameters(c.getSignature()).isEmpty()) {
                dto.subclasses.add(sub);
            }
        }
        return dto;
    }

    /// The `@JsonCreator` of `cls`, if it has one: a non-private constructor, or
    /// a static factory returning the class, whose every parameter names its
    /// member with `@JsonProperty`.
    private void collectCreator(Dto dto, AnnotatedClass cls) {
        MethodInfo found = null;
        for (MethodInfo m : cls.getMethods()) {
            if (m.getAnnotation(JSON_CREATOR) == null) {
                continue;
            }
            if (found != null) {
                dto.readProblem = dto.binary + ", which marks two members @JsonCreator; a body "
                        + "is read through one";
                return;
            }
            found = m;
        }
        if (found == null) {
            return;
        }
        String where = "the @JsonCreator " + (found.isConstructor() ? "constructor"
                : "method " + found.getName()) + " of " + dto.binary;
        if (found.isPrivate()) {
            dto.readProblem = where + ", which is private; the generated codec cannot call it";
            return;
        }
        if (!found.isConstructor() && (!found.isStatic() || !Type.getReturnType(
                found.getDescriptor()).getClassName().equals(dto.binary))) {
            dto.readProblem = where + ", which has to be a constructor or a static method "
                    + "returning " + source(dto.binary);
            return;
        }
        if (found.isConstructor() && cls.isAbstract()) {
            return;                      // refused below as abstract
        }
        Type[] types = Type.getArgumentTypes(found.getDescriptor());
        List<String> generic = parameterTypes(found.getSignature(), dto.vars);
        List<Map<String, AnnotationValues>> annotations = found.getParameterAnnotations();
        Creator creator = new Creator();
        creator.method = found.getName();
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < types.length; i++) {
            AnnotationValues named = annotations == null || i >= annotations.size()
                    || annotations.get(i) == null ? null : annotations.get(i).get(JSON_PROPERTY);
            String json = named == null ? null : named.getString("value");
            if (json == null || json.length() == 0) {
                dto.readProblem = where + ": parameter " + (i + 1) + " has no @JsonProperty "
                        + "naming the member it is read from, and a compiled class keeps no "
                        + "parameter names to match it by";
                return;
            }
            Prop p = new Prop();
            p.json = json;
            readOptions(p, named);
            if (generic == null) {
                p.type = RestClientAnnotationProcessor.javaTypeFor(types[i], null);
            } else if (i < generic.size() && generic.get(i) != null) {
                p.type = generic.get(i);
            } else {
                dto.readProblem = where + ": parameter " + json + " has a type variable for its "
                        + "type, so the build cannot know what it holds";
                return;
            }
            if (!names.add(json)) {
                dto.readProblem = where + " reads \"" + json + "\" into two parameters";
                return;
            }
            creator.params.add(p);
        }
        dto.creator = creator;
    }

    /// `aliases` and `required` of a `@JsonProperty`, onto `p`.
    private static void readOptions(Prop p, AnnotationValues named) {
        if (named == null) {
            return;
        }
        Object aliases = named.get("aliases");
        if (aliases instanceof List) {
            for (Object a : (List<?>) aliases) {
                if (a instanceof String && ((String) a).length() > 0
                        && !a.equals(p.json) && !p.aliases.contains(a)) {
                    p.aliases.add((String) a);
                }
            }
        }
        p.required = named.getBoolOrDefault("required", false);
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
        // Each class's type variables as the one below it binds them, from the
        // DTO's own arguments up: Page<User> extends Base<T> makes Base's T User.
        List<Map<String, String>> vars = new ArrayList<Map<String, String>>();
        for (int i = 0; i < chain.size(); i++) {
            vars.add(null);
        }
        vars.set(chain.size() - 1, dto.vars);
        for (int i = chain.size() - 1; i > 0; i--) {
            vars.set(i - 1, superVars(chain.get(i), vars.get(i), chain.get(i - 1)));
        }
        String pkg = packageOf(dto.binary);
        boolean omitAll = cls.getClassAnnotation(JSON_OMIT_NULL) != null;
        Set<String> names = new HashSet<String>();
        // A record's state is its components: private final fields, in
        // declaration order, read through the public accessor of the same name
        // and set all at once through the canonical constructor. Java records
        // carry no JavaBeans getters or setters, so the rules below would find
        // nothing and the record would go over the wire as {}.
        boolean record = cls.isRecord();
        for (int level = 0; level < chain.size(); level++) {
            AnnotatedClass c = chain.get(level);
            for (FieldInfo f : c.getFields()) {
                int access = f.getAccess();
                boolean ignored = f.getAnnotation(JSON_IGNORE) != null;
                if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT
                        | Opcodes.ACC_SYNTHETIC)) != 0 || f.getName().startsWith("this$")
                        || ignored && !record) {
                    continue;
                }
                Prop p = new Prop();
                AnnotationValues renamed = f.getAnnotation(JSON_PROPERTY);
                String json = renamed == null ? null : renamed.getString("value");
                p.json = json == null || json.length() == 0 ? f.getName() : json;
                readOptions(p, renamed);
                if (hasTypeVariable(f.getSignature())) {
                    p.type = resolve(f.getSignature(), vars.get(level));
                    if (p.type == null) {
                        dto.problem = "a class whose field " + f.getName() + " has a type "
                                + "variable for its type (" + c.getBinaryName() + "), so the "
                                + "build cannot know what it holds; declare it with its type "
                                + "arguments, as in Page<User>, or declare a concrete subclass";
                        return;
                    }
                } else {
                    p.type = RestClientAnnotationProcessor.javaTypeFor(
                            Type.getType(f.getDescriptor()), f.getSignature());
                }
                p.omitNull = (omitAll || f.getAnnotation(JSON_OMIT_NULL) != null)
                        && !PRIMITIVES.contains(p.type);
                String option = fieldOptions(p, f);
                if (option != null) {
                    dto.problem = "a class whose field " + f.getName() + " of "
                            + c.getBinaryName() + " " + option;
                    return;
                }
                // The codec lives in the class's own package, so a field it can
                // name is read and written directly; any other goes through the
                // JavaBeans accessors, as it does for the app's mapper.
                boolean reachable = f.isPublic() || (!f.isPrivate()
                        && packageOf(c.getBinaryName()).equals(pkg));
                String cap = Character.toUpperCase(f.getName().charAt(0))
                        + f.getName().substring(1);
                if (record) {
                    // An ignored component is still an argument of the canonical
                    // constructor: it is neither written nor read, and arrives as
                    // its type's default.
                    p.get = ignored ? null : "v." + f.getName() + "()";
                    p.component = !ignored;
                    if (ignored) {
                        p.json = null;
                        dto.props.add(p);
                        continue;
                    }
                    if (!names.add(p.json)) {
                        dto.problem = "a record with two components named \"" + p.json
                                + "\" in JSON (" + dto.binary + "); rename one with @JsonProperty";
                        return;
                    }
                    for (String alias : p.aliases) {
                        if (!names.add(alias)) {
                            dto.problem = "a record whose alias \"" + alias + "\" of component "
                                    + f.getName() + " is already the name of another member ("
                                    + dto.binary + ")";
                            return;
                        }
                    }
                    dto.props.add(p);
                    continue;
                }
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
                for (String alias : p.aliases) {
                    if (!names.add(alias)) {
                        dto.problem = "a class whose alias \"" + alias + "\" of field "
                                + f.getName() + " is already the name of another member ("
                                + dto.binary + ")";
                        return;
                    }
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
                // The compile classpath too: a project subclass can reach the
                // declared type through a dependency's class, and stopping at it
                // dropped the subclass from dispatch -- its fields silently unwritten.
                AnnotatedClass up = RestControllerAnnotationProcessor.resolveClass(ctx, parent);
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

    /// The `@JsonFormat` and `@JsonAdapter` of field `f` onto `p`, or why the
    /// field cannot have them.
    private static String fieldOptions(Prop p, FieldInfo f) {
        AnnotationValues adapter = f.getAnnotation(JSON_ADAPTER);
        if (adapter != null) {
            Object value = adapter.get("value");
            if (!(value instanceof Type)) {
                return "has a @JsonAdapter that names no class";
            }
            p.adapter = ((Type) value).getClassName();
            if (PRIMITIVES.contains(p.type)) {
                return "is a " + p.type + " with a @JsonAdapter, which can hand back null; "
                        + "declare it with its box type";
            }
        }
        AnnotationValues format = f.getAnnotation(JSON_FORMAT);
        Object shape = format == null ? null : format.get("value");
        String name = shape instanceof String[] && ((String[]) shape).length == 2
                ? ((String[]) shape)[1] : null;
        if (name == null || "DEFAULT".equals(name)) {
            return null;
        }
        if (p.adapter != null) {
            return "has both @JsonAdapter and @JsonFormat; its adapter decides its form";
        }
        boolean date = "java.util.Date".equals(p.type);
        boolean number = (PRIMITIVES.contains(p.type) || BOXES.contains(p.type))
                && !"boolean".equals(p.type) && !"char".equals(p.type)
                && !"java.lang.Boolean".equals(p.type) && !"java.lang.Character".equals(p.type);
        if ("STRING".equals(name) && (date || number)) {
            p.asString = true;
            return null;
        }
        if ("NUMBER".equals(name) && date) {
            return null;                 // a date's default form
        }
        return "is marked @JsonFormat(" + name + "), which a " + p.type + " has no form of; "
                + "STRING applies to numbers and dates, NUMBER to dates";
    }

    /// The `@JsonAdapter` of the class `type` names, by binary name.
    private String classAdapter(String type) {
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                raw(type).replace('.', '/'));
        AnnotationValues adapter = cls == null ? null : cls.getClassAnnotation(JSON_ADAPTER);
        Object value = adapter == null ? null : adapter.get("value");
        return value instanceof Type ? ((Type) value).getClassName() : null;
    }

    /// Null when the adapter class `adapter` can convert a `type` for a
    /// generated codec, or why not. Registers it in [#ADAPTERS] when it can.
    private String adapterProblem(String adapter, String type) {
        if (adapter == null) {
            return "a class whose @JsonAdapter names no class";
        }
        AnnotatedClass cls = RestControllerAnnotationProcessor.resolveClass(ctx,
                adapter.replace('.', '/'));
        String what = "converted by the JSON adapter " + source(adapter) + ", ";
        if (cls == null) {
            return what + "which the build cannot find on its class path";
        }
        if (cls.isInterface() || cls.isAbstract() || isInnerClass(cls)
                || !cls.isAccessibleFromAnywhere() || !hasPublicNoArgConstructor(cls)) {
            return what + "which the generated code cannot create; make it a public, "
                    + "concrete, top-level or static class with a public constructor without "
                    + "arguments";
        }
        String converts = adaptedType(cls, Collections.<String, String>emptyMap(),
                new HashSet<String>());
        if (converts == null) {
            return what + "which does not implement JsonTypeAdapter with a type argument; "
                    + "declare it as implements JsonTypeAdapter<" + source(raw(type)) + ">";
        }
        if (!raw(converts).equals(raw(type))) {
            return what + "which converts " + source(converts) + ", not " + source(type);
        }
        adapterField(adapter);
        return null;
    }

    /// The `T` of `JsonTypeAdapter<T>` as `cls`, or a superclass, binds it.
    private String adaptedType(AnnotatedClass cls, Map<String, String> vars, Set<String> seen) {
        if (cls == null || !seen.add(cls.getInternalName())) {
            return null;
        }
        List<String> parts = classSignatureParts(cls.getSignature());
        for (int i = 1; i < parts.size(); i++) {
            String itf = resolve(parts.get(i), vars);
            if (itf != null && ("com.codename1.annotations.JsonTypeAdapter").equals(raw(itf))) {
                List<String> a = args(itf);
                return a.size() == 1 ? strip(a.get(0)) : null;
            }
        }
        String parent = cls.getSuperInternalName();
        if (parent == null || parent.startsWith("java/")) {
            return null;
        }
        AnnotatedClass up = RestControllerAnnotationProcessor.resolveClass(ctx, parent);
        return up == null ? null : adaptedType(up, superVars(cls, vars, up), seen);
    }

    /// The field of [#ADAPTERS] holding the one instance of `adapter`.
    private String adapterField(String adapter) {
        String field = adapters.get(adapter);
        if (field == null) {
            field = "a" + adapters.size();
            adapters.put(adapter, field);
        }
        return field;
    }

    private static boolean hasPublicNoArgConstructor(AnnotatedClass cls) {
        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() && "()V".equals(m.getDescriptor()) && m.isPublic()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Generic signatures
    // ------------------------------------------------------------------

    /// The names of the type parameters a class or method signature declares.
    static List<String> typeParameters(String signature) {
        List<String> out = new ArrayList<String>();
        if (signature == null || !signature.startsWith("<")) {
            return out;
        }
        int i = 1;
        while (signature.charAt(i) != '>') {
            int colon = signature.indexOf(':', i);
            out.add(signature.substring(i, colon));
            i = colon;
            while (signature.charAt(i) == ':') {
                i++;
                if (signature.charAt(i) != ':') {
                    i = skipType(signature, i);
                }
            }
        }
        return out;
    }

    /// A class signature's superclass and interfaces, each as a signature.
    static List<String> classSignatureParts(String signature) {
        List<String> out = new ArrayList<String>();
        if (signature == null) {
            return out;
        }
        int i = afterTypeParameters(signature);
        while (i < signature.length()) {
            int end = skipType(signature, i);
            out.add(signature.substring(i, end));
            i = end;
        }
        return out;
    }

    /// Each parameter of a method signature as a Java type, its type variables
    /// replaced from `vars` (null for one that is not there); null when the
    /// method has no generic signature.
    static List<String> parameterTypes(String signature, Map<String, String> vars) {
        if (signature == null) {
            return null;
        }
        List<String> out = new ArrayList<String>();
        int i = afterTypeParameters(signature) + 1;            // past '('
        while (signature.charAt(i) != ')') {
            int end = skipType(signature, i);
            out.add(resolve(signature.substring(i, end), vars));
            i = end;
        }
        return out;
    }

    /// The type variables of `parent` as `child`, whose own are `vars`, binds
    /// them in its extends clause.
    private static Map<String, String> superVars(AnnotatedClass child, Map<String, String> vars,
                                                 AnnotatedClass parent) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        List<String> parts = classSignatureParts(child.getSignature());
        String superType = parts.isEmpty() ? null : resolve(parts.get(0), vars);
        if (superType == null) {
            return out;
        }
        List<String> params = typeParameters(parent.getSignature());
        List<String> args = args(superType);
        for (int i = 0; i < params.size() && i < args.size(); i++) {
            String a = strip(args.get(i));
            if (a != null) {
                out.put(params.get(i), a);
            }
        }
        return out;
    }

    private static int afterTypeParameters(String signature) {
        if (!signature.startsWith("<")) {
            return 0;
        }
        int i = 1;
        while (signature.charAt(i) != '>') {
            i = signature.indexOf(':', i);
            while (signature.charAt(i) == ':') {
                i++;
                if (signature.charAt(i) != ':') {
                    i = skipType(signature, i);
                }
            }
        }
        return i + 1;
    }

    /// The index just past the type signature starting at `i`.
    private static int skipType(String s, int i) {
        char c = s.charAt(i);
        switch (c) {
            case '[':
            case '+':
            case '-':
                return skipType(s, i + 1);
            case 'T':
                return s.indexOf(';', i) + 1;
            case 'L': {
                int depth = 0;
                for (int j = i; j < s.length(); j++) {
                    char ch = s.charAt(j);
                    if (ch == '<') {
                        depth++;
                    } else if (ch == '>') {
                        depth--;
                    } else if (ch == ';' && depth == 0) {
                        return j + 1;
                    }
                }
                return s.length();
            }
            default:
                return i + 1;            // a primitive, or the wildcard '*'
        }
    }

    /// A type signature as a Java type -- in the form the rest of this class
    /// reads, `a.b.C<a.D, ? extends a.E>` -- with its type variables replaced
    /// from `vars`; null when one of them is not there.
    static String resolve(String signature, Map<String, String> vars) {
        if (signature == null) {
            return null;
        }
        int[] at = {0};
        boolean[] ok = {true};
        String type = resolve(signature, at, vars, ok);
        return ok[0] ? type : null;
    }

    private static String resolve(String s, int[] at, Map<String, String> vars, boolean[] ok) {
        char c = s.charAt(at[0]++);
        switch (c) {
            case 'B': return "byte";
            case 'C': return "char";
            case 'D': return "double";
            case 'F': return "float";
            case 'I': return "int";
            case 'J': return "long";
            case 'S': return "short";
            case 'Z': return "boolean";
            case 'V': return "void";
            case '[':
                return resolve(s, at, vars, ok) + "[]";
            case 'T': {
                int semi = s.indexOf(';', at[0]);
                String bound = vars == null ? null : vars.get(s.substring(at[0], semi));
                at[0] = semi + 1;
                if (bound == null) {
                    ok[0] = false;
                    return "java.lang.Object";
                }
                return bound;
            }
            case 'L': {
                StringBuilder out = new StringBuilder();
                while (true) {
                    char ch = s.charAt(at[0]++);
                    if (ch == ';') {
                        return out.toString();
                    }
                    if (ch == '/') {
                        out.append('.');
                    } else if (ch == '.') {
                        out.append('$');
                    } else if (ch == '<') {
                        out.append('<');
                        boolean first = true;
                        while (s.charAt(at[0]) != '>') {
                            out.append(first ? "" : ", ");
                            first = false;
                            char w = s.charAt(at[0]);
                            if (w == '*') {
                                at[0]++;
                                out.append('?');
                            } else if (w == '+' || w == '-') {
                                at[0]++;
                                out.append(w == '+' ? "? extends " : "? super ")
                                   .append(resolve(s, at, vars, ok));
                            } else {
                                out.append(resolve(s, at, vars, ok));
                            }
                        }
                        at[0]++;
                        out.append('>');
                    } else {
                        out.append(ch);
                    }
                }
            }
            default:
                ok[0] = false;
                return "java.lang.Object";
        }
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
            return Kind.ARRAY;
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
        if (cls != null && cls.getClassAnnotation(JSON_ADAPTER) != null) {
            return Kind.ADAPTED;
        }
        if (cls != null && cls.isEnum()) {
            return Kind.ENUM;
        }
        return Kind.DTO;
    }

    private static String whyNot(String type) {
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
                sb.append(ind).append(codecBinary(dtoKey(type))).append(".write(").append(expr)
                  .append(", out, ").append(depth).append(" + 1);\n");
                return;
            case ADAPTED:
                writeAdapted(sb, classAdapter(type), expr, depth, ind);
                return;
            case ARRAY: {
                String element = type.substring(0, type.length() - 2);
                String a = fresh("a");
                String i = fresh("i");
                sb.append(ind).append(source(type)).append(' ').append(a).append(" = ")
                  .append(expr).append(";\n");
                sb.append(ind).append("if (").append(a).append(" == null) {\n");
                sb.append(ind).append("    out.putAscii(\"null\");\n");
                sb.append(ind).append("} else {\n");
                sb.append(ind).append("    out.put('[');\n");
                sb.append(ind).append("    for (int ").append(i).append(" = 0; ").append(i)
                  .append(" < ").append(a).append(".length; ").append(i).append("++) {\n");
                sb.append(ind).append("        if (").append(i).append(" > 0) {\n");
                sb.append(ind).append("            out.put(',');\n");
                sb.append(ind).append("        }\n");
                write(sb, element, a + "[" + i + "]", depth, ind + "        ");
                sb.append(ind).append("    }\n");
                sb.append(ind).append("    out.put(']');\n");
                sb.append(ind).append("}\n");
                return;
            }
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
                sb.append(ind).append(target).append(" = ").append(codecBinary(dtoKey(type)))
                  .append(".read(").append(json).append(", ").append(where).append(", ")
                  .append(depth).append(" + 1);\n");
                return;
            case ADAPTED:
                readAdapted(sb, classAdapter(type), json, where, target, ind);
                return;
            case ARRAY: {
                String element = type.substring(0, type.length() - 2);
                String src = fresh("l");
                String path = fresh("p");
                String out = fresh("c");
                String loop = fresh("i");
                String value = fresh("v");
                sb.append(ind).append("if (").append(json).append(" == null) {\n");
                sb.append(ind).append("    ").append(target).append(" = null;\n");
                sb.append(ind).append("} else {\n");
                sb.append(ind).append("    if (!(").append(json).append(" instanceof java.util.List)) {\n");
                sb.append(ind).append("        throw ").append(CODEC).append(".mismatch(")
                  .append(where).append(", \"an array\", ").append(json).append(");\n");
                sb.append(ind).append("    }\n");
                sb.append(ind).append("    java.util.List ").append(src).append(" = (java.util.List) ")
                  .append(json).append(";\n");
                sb.append(ind).append("    ").append(CODEC).append(".Path ").append(path)
                  .append(" = ").append(CODEC).append(".enter(").append(where).append(");\n");
                sb.append(ind).append("    ").append(source(type)).append(' ').append(out)
                  .append(" = ").append(newArray(element, src + ".size()")).append(";\n");
                sb.append(ind).append("    for (int ").append(loop).append(" = 0; ").append(loop)
                  .append(" < ").append(out).append(".length; ").append(loop).append("++) {\n");
                sb.append(ind).append("        ").append(source(element)).append(' ').append(value)
                  .append(" = ").append(defaultOf(element)).append(";\n");
                read(sb, element, src + ".get(" + loop + ")", path, "null", loop, depth, value,
                        ind + "        ");
                sb.append(ind).append("        ").append(out).append('[').append(loop)
                  .append("] = ").append(value).append(";\n");
                sb.append(ind).append("    }\n");
                sb.append(ind).append("    ").append(target).append(" = ").append(out).append(";\n");
                sb.append(ind).append("}\n");
                return;
            }
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

    /// Writes `expr` through the application's adapter: its tree form, written
    /// as any free-form value is.
    private void writeAdapted(StringBuilder sb, String adapter, String expr, String depth,
                              String ind) {
        valuesUsed = true;
        sb.append(ind).append(VALUES).append(".write(").append(ADAPTERS).append('.')
          .append(adapterField(adapter)).append(".toJson(").append(expr).append("), out, ")
          .append(depth).append(");\n");
    }

    /// Reads `json` through the application's adapter, adding where it was to a
    /// value the adapter refuses.
    private void readAdapted(StringBuilder sb, String adapter, String json, String where,
                             String target, String ind) {
        String x = fresh("x");
        sb.append(ind).append("try {\n");
        sb.append(ind).append("    ").append(target).append(" = ").append(ADAPTERS).append('.')
          .append(adapterField(adapter)).append(".fromJson(").append(json).append(");\n");
        sb.append(ind).append("} catch (IllegalArgumentException ").append(x).append(") {\n");
        sb.append(ind).append("    throw ").append(CODEC).append(".refused(").append(where)
          .append(", ").append(x).append(");\n");
        sb.append(ind).append("}\n");
    }

    /// `new E[length]`, with the brackets where Java wants them for an element
    /// that is itself an array.
    private static String newArray(String element, String length) {
        String base = element;
        StringBuilder dims = new StringBuilder();
        while (base.endsWith("[]")) {
            base = base.substring(0, base.length() - 2);
            dims.append("[]");
        }
        return "new " + source(base) + "[" + length + "]" + dims;
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
            case ADAPTED:
                return "((" + source(raw(type)) + ") " + var + ")";
            case ARRAY:
                return "((" + source(type) + ") " + var + ")";
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
        Map<String, String> owners = new LinkedHashMap<String, String>();
        for (Dto dto : dtos.values()) {
            if (dto.problem != null || (!dto.write && !dto.read)) {
                continue;
            }
            String codec = codecBinary(dto.key);
            String other = owners.get(codec);
            if (other != null) {
                // Outer_Inner and Outer.Inner in one package fold to one codec
                // name, as do Page<a.Foo_Bar> and Page<a.Foo$Bar>, or Page<a.User>
                // and a class a.Page_a_User; generating both would let one
                // replace the other.
                ctx.error(dto.cls, source(dto.key) + " and " + source(other) + " would both "
                        + "get the JSON codec " + codec + ". Rename one of them.");
                continue;
            }
            owners.put(codec, dto.key);
            out.put(codec, codecSource(dto));
        }
        if (valuesUsed) {
            out.put(VALUES, valuesSource());
        }
        if (!adapters.isEmpty()) {
            out.put(ADAPTERS, adaptersSource());
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
        sb.append("        if (depth > ").append(CODEC).append(".maxDepth()) {\n");
        sb.append("            throw ").append(CODEC).append(".tooDeep(\"value\");\n");
        sb.append("        }\n");
        for (Dto dto : dtos.values()) {
            // A generic class used with its arguments has a codec per use, and a
            // value whose declared type says nothing does not say which.
            if (dto.problem != null || !dto.write || !dto.key.equals(dto.binary)) {
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

    /// `cn1app.JsonAdapters`: one instance of each application `JsonTypeAdapter`
    /// a codec calls, created directly, so the class is kept and found by no name.
    private String adaptersSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("package cn1app;\n\n");
        sb.append("// Generated: the JSON adapters the codecs call. Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class JsonAdapters {\n");
        for (Map.Entry<String, String> e : adapters.entrySet()) {
            String type = source(e.getKey());
            sb.append("    public static final ").append(type).append(' ').append(e.getValue())
              .append(" = new ").append(type).append("();\n");
        }
        sb.append("\n    private JsonAdapters() {\n    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String codecSource(Dto dto) {
        String pkg = packageOf(dto.binary);
        String simple = simpleOf(codecBinary(dto.key));
        String type = source(dto.key);
        StringBuilder sb = new StringBuilder();
        if (pkg.length() > 0) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("// Generated: the JSON form of ").append(type).append(". Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(simple).append(" {\n");
        sb.append("    private ").append(simple).append("() {\n    }\n");
        if (dto.write) {
            writerSource(sb, dto, type);
        }
        if (dto.read && dto.readProblem == null) {
            readerSource(sb, dto, type);
        }
        sb.append("}\n");
        return sb.toString();
    }

    private void writerSource(StringBuilder sb, Dto dto, String type) {
        sb.append("\n    public static void write(").append(type).append(" v, ").append(SINK)
          .append(" out, int depth) {\n");
        sb.append("        if (v == null) {\n");
        sb.append("            out.putAscii(\"null\");\n");
        sb.append("            return;\n");
        sb.append("        }\n");
        sb.append("        if (depth > ").append(CODEC).append(".maxDepth()) {\n");
        sb.append("            throw ").append(CODEC).append(".tooDeep(")
          .append(quote(simpleOf(dto.binary))).append(");\n");
        sb.append("        }\n");
        for (String sub : dto.subclasses) {
            String subSource = source(sub);
            sb.append("        if (v instanceof ").append(subSource).append(") {\n");
            sb.append("            ").append(codecBinary(sub)).append(".write((")
              .append(subSource).append(") v, out, depth);\n");
            sb.append("            return;\n");
            sb.append("        }\n");
        }
        if (dto.ownWriter) {
            sb.append("        v.writeTo(out);\n");
            sb.append("    }\n");
            return;
        }
        // A member left out when null makes whether a comma comes before the
        // next one a run-time question; a class without one keeps it static.
        boolean dynamic = false;
        for (Prop p : dto.props) {
            dynamic |= p.get != null && p.omitNull;
        }
        sb.append("        out.put('{');\n");
        if (dynamic) {
            sb.append("        boolean more = false;\n");
        }
        boolean first = true;
        for (Prop p : dto.props) {
            String get = p.get;
            if (get == null) {
                continue;
            }
            String ind = "        ";
            if (p.omitNull) {
                String t = fresh("o");
                sb.append(ind).append(source(strip(p.type))).append(' ').append(t).append(" = ")
                  .append(get).append(";\n");
                sb.append(ind).append("if (").append(t).append(" != null) {\n");
                get = t;
                ind = "            ";
            }
            if (dynamic) {
                sb.append(ind).append("if (more) {\n");
                sb.append(ind).append("    out.put(',');\n");
                sb.append(ind).append("}\n");
                sb.append(ind).append("more = true;\n");
            }
            boolean comma = !dynamic && !first;
            sb.append(ind);
            if (plainName(p.json)) {
                sb.append("out.putAscii(\"").append(comma ? "," : "").append("\\\"")
                  .append(p.json).append("\\\":\");\n");
            } else {
                if (comma) {
                    sb.append("out.put(',');\n").append(ind);
                }
                sb.append(JSON).append(".writeString(").append(quote(p.json))
                  .append(", out);\n").append(ind).append("out.put(':');\n");
            }
            writeProp(sb, p, get, ind);
            if (p.omitNull) {
                sb.append("        }\n");
            }
            first = false;
        }
        sb.append("        out.put('}');\n");
        sb.append("    }\n");
    }

    /// The value of property `p`, read off `v` by `get`, with the field's own
    /// adapter or format when it has one.
    private void writeProp(StringBuilder sb, Prop p, String get, String ind) {
        String t = strip(p.type);
        if (p.adapter != null) {
            writeAdapted(sb, p.adapter, get, "depth", ind);
        } else if (p.asString && "java.util.Date".equals(t)) {
            sb.append(ind).append(CODEC).append(".writeIsoDate(").append(get).append(", out);\n");
        } else if (p.asString && PRIMITIVES.contains(t)) {
            sb.append(ind).append(JSON).append(".writeString(String.valueOf(").append(get)
              .append("), out);\n");
        } else if (p.asString) {
            String b = fresh("n");
            sb.append(ind).append(t).append(' ').append(b).append(" = ").append(get).append(";\n");
            sb.append(ind).append("if (").append(b).append(" == null) {\n");
            sb.append(ind).append("    out.putAscii(\"null\");\n");
            sb.append(ind).append("} else {\n");
            sb.append(ind).append("    ").append(JSON).append(".writeString(").append(b)
              .append(".toString(), out);\n");
            sb.append(ind).append("}\n");
        } else {
            write(sb, t, get, "depth", ind);
        }
    }

    private void readerSource(StringBuilder sb, Dto dto, String type) {
        sb.append("\n    public static ").append(type).append(" read(Object json, ")
          .append(CODEC).append(".Path at, String name, int index, int depth) {\n");
        sb.append("        if (json == null) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        if (!(json instanceof java.util.Map) || depth > ").append(CODEC)
          .append(".maxDepth()) {\n");
        sb.append("            throw ").append(CODEC)
          .append(".mismatch(at, name, index, \"an object\", json);\n");
        sb.append("        }\n");
        sb.append("        java.util.Map m = (java.util.Map) json;\n");
        sb.append("        ").append(CODEC).append(".Path here = ").append(CODEC)
          .append(".enter(at, name, index);\n");
        if (dto.strict) {
            strictSource(sb, dto);
        }
        boolean record = dto.cls.isRecord();
        // A record, or a class read through its @JsonCreator, is built from one
        // local per member; an absent member leaves its local at the type's
        // default, as it leaves a field of a class.
        List<String> args = new ArrayList<String>();
        Set<String> created = new HashSet<String>();
        if (dto.creator != null) {
            for (Prop p : dto.creator.params) {
                String local = fresh("c");
                args.add(local);
                created.add(p.json);
                String t = strip(p.type);
                sb.append("        ").append(source(t)).append(' ').append(local)
                  .append(" = ").append(defaultOf(t)).append(";\n");
                readProp(sb, p, local + " = ", ";");
            }
            sb.append("        ").append(type).append(" v = ");
            if ("<init>".equals(dto.creator.method)) {
                sb.append("new ").append(type);
            } else {
                sb.append(source(dto.binary)).append('.').append(dto.creator.method);
            }
            sb.append('(').append(String.join(", ", args)).append(");\n");
        } else if (!record) {
            sb.append("        ").append(type).append(" v = new ").append(type).append("();\n");
        }
        for (Prop p : dto.props) {
            boolean direct = p.field != null;
            String local = null;
            if (record) {
                local = fresh("c");
                args.add(local);
                sb.append("        ").append(source(strip(p.type))).append(' ').append(local)
                  .append(" = ").append(defaultOf(strip(p.type))).append(";\n");
                if (!p.component) {
                    continue;        // @JsonIgnore: the default, never the body's value
                }
            } else if (!direct && p.setter == null || created.contains(p.json)) {
                continue;
            }
            readProp(sb, p, local != null ? local + " = " : direct ? "v." + p.field + " = "
                    : "v." + p.setter + "(", local != null || direct ? ";" : ");");
        }
        if (record) {
            sb.append("        return new ").append(type).append('(')
              .append(String.join(", ", args)).append(");\n");
        } else {
            sb.append("        return v;\n");
        }
        sb.append("    }\n");
    }

    /// Statements reading member `p` of `m` and storing it with `assign` and
    /// `end` around the value: its aliases, `required`, format and adapter.
    private void readProp(StringBuilder sb, Prop p, String assign, String end) {
        String t = strip(p.type);
        String j = fresh("j");
        String has = fresh("h");
        String key = quote(p.json);
        sb.append("        Object ").append(j).append(" = m.get(").append(key).append(");\n");
        boolean primitive = PRIMITIVES.contains(t);
        boolean track = !p.aliases.isEmpty() || p.required || !primitive;
        if (track) {
            sb.append("        boolean ").append(has).append(" = ").append(j)
              .append(" != null || m.containsKey(").append(key).append(");\n");
            String primary = key;
            if (!p.aliases.isEmpty()) {
                // Diagnostics name the member the body actually carried.
                key = fresh("k");
                sb.append("        String ").append(key).append(" = ").append(primary)
                  .append(";\n");
            }
            for (String alias : p.aliases) {
                String a = quote(alias);
                sb.append("        if (!").append(has).append(") {\n");
                sb.append("            ").append(key).append(" = ").append(a).append(";\n");
                sb.append("            ").append(j).append(" = m.get(").append(a).append(");\n");
                sb.append("            ").append(has).append(" = ").append(j)
                  .append(" != null || m.containsKey(").append(a).append(");\n");
                sb.append("        }\n");
            }
            if (p.required) {
                sb.append("        if (!").append(has).append(") {\n");
                sb.append("            throw ").append(CODEC).append(".missing(here, ").append(primary)
                  .append(");\n");
                sb.append("        }\n");
            }
        }
        if (p.asString && !"java.util.Date".equals(t)) {
            sb.append("        ").append(j).append(" = ").append(CODEC).append(".numberText(")
              .append(j).append(");\n");
        }
        if (primitive) {
            // null, like an absent member, leaves the field's default --
            // Jackson's answer for a primitive too -- unless the member is
            // required, when an explicit null is a value it cannot hold.
            String value = scalar(t, j, "here, " + key + ", -1");
            if (p.required) {
                sb.append("        if (").append(j).append(" == null) {\n");
                sb.append("            throw ").append(CODEC).append(".mismatch(here, ").append(key)
                  .append(", -1, \"a value\", null);\n");
                sb.append("        }\n");
            }
            sb.append("        if (").append(j).append(" != null) {\n");
            sb.append("            ").append(assign).append(value).append(end).append("\n");
            sb.append("        }\n");
            return;
        }
        String tmp = fresh("t");
        sb.append("        if (").append(has).append(") {\n");
        sb.append("            ").append(source(t)).append(' ').append(tmp).append(" = ")
          .append(defaultOf(t)).append(";\n");
        if (p.adapter != null) {
            readAdapted(sb, p.adapter, j, "here, " + key + ", -1", tmp, "            ");
        } else {
            read(sb, t, j, "here", key, "-1", "depth", tmp, "            ");
        }
        sb.append("            ").append(assign).append(tmp).append(end).append("\n");
        sb.append("        }\n");
    }

    /// Refuses a member of `m` that `dto` neither names nor aliases.
    private void strictSource(StringBuilder sb, Dto dto) {
        Set<String> known = new java.util.TreeSet<String>();
        for (Prop p : dto.props) {
            if (p.json != null) {
                known.add(p.json);
                known.addAll(p.aliases);
            }
        }
        if (dto.creator != null) {
            for (Prop p : dto.creator.params) {
                if (p.json == null) {
                    continue;
                }
                known.add(p.json);
                known.addAll(p.aliases);
            }
        }
        sb.append("        for (java.util.Iterator k = m.keySet().iterator(); k.hasNext();) {\n");
        sb.append("            String n = String.valueOf(k.next());\n");
        sb.append("            if (");
        if (known.isEmpty()) {
            sb.append("true");
        }
        String sep = "";
        for (String n : known) {
            sb.append(sep).append("!n.equals(").append(quote(n)).append(')');
            sep = "\n                    && ";
        }
        sb.append(") {\n");
        sb.append("                throw ").append(CODEC).append(".unknown(here, n);\n");
        sb.append("            }\n");
        sb.append("        }\n");
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

    /// The codec of a DTO key: `a.b.Outer$InnerCn1Json` is `a.b.Outer_InnerCn1Json`,
    /// and a generic class used with arguments names them, `a.Page<a.User>`
    /// becoming `a.Page_a_UserCn1Json`.
    private static String codecBinary(String key) {
        String binary = raw(key);
        String pkg = packageOf(binary);
        StringBuilder simple = new StringBuilder(binary.substring(
                pkg.length() == 0 ? 0 : pkg.length() + 1).replace('$', '_'));
        if (binary.length() < key.length()) {
            for (char c : key.substring(binary.length()).toCharArray()) {
                if (Character.isLetterOrDigit(c)) {
                    simple.append(c);
                } else if (c == '<' || c == ',' || c == '.' || c == '$') {
                    simple.append('_');
                } else if (c == '[') {
                    simple.append("Array");
                }
            }
        }
        simple.append("Cn1Json");
        return pkg.length() == 0 ? simple.toString() : pkg + "." + simple;
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

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

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.objectweb.asm.Type;

/// Server-side half of the `@RestClient` contract.
///
/// The SAME annotated interface that
/// [RestClientAnnotationProcessor] turns into a typed client is turned here into
/// the two things a server needs. One declaration, both ends, so a change to the
/// contract is a compile error on whichever side did not follow it -- which is the
/// entire point of sharing the interface rather than hand-writing a client against
/// a REST endpoint.
///
/// For `@RestClient interface GreeterApi` it emits, in the interface's package:
///
/// 1. `GreeterApiServer` -- a SYNCHRONOUS interface the backend implements. The
///    client's methods are asynchronous (they take an
///    `OnComplete<Response<T>>` and return void); a server handler has nothing to
///    do with a callback, so its method returns `T` directly and the callback
///    parameter is dropped. This is gRPC's shape: one definition, an async client
///    stub and a sync server base, rather than forcing one signature to serve both.
///    It is also what keeps `Response` off the server's classpath entirely -- its
///    constructor is package-private, so server code could not build one anyway.
/// 2. `GreeterApiDispatcher` -- routes `(method, path, body)` to the right handler
///    method, binding `@Path` segments and `@Query` parameters out of the request.
///
/// Unsupported bindings are rejected with an error rather than silently bound to
/// null: a parameter that quietly arrives empty at runtime is far more expensive
/// than a build that refuses to produce one.
public final class RestServerAnnotationProcessor extends AbstractAnnotationProcessor {

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(RestClientAnnotationProcessor.REST_CLIENT_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    /// Set -Dcn1.restServer=true (or the cn1.restServer property) to emit the
    /// server half. Off by default: every existing project carries @RestClient
    /// interfaces for its client, and generating server classes into those builds
    /// would grow every app for nothing.
    static boolean isEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("cn1.restServer", "false"));
    }

    private final TreeMap<String, Api> accepted = new TreeMap<String, Api>();

    /// DTO types reachable from an accepted contract, keyed by binary name. A
    /// TreeMap so codec emission order is stable across builds.
    private final TreeMap<String, AnnotatedClass> dtos = new TreeMap<String, AnnotatedClass>();

    static final class Api {
        String binaryName, simpleName, packageName, serverSimpleName, dispatcherSimpleName;
        final List<Op> ops = new ArrayList<Op>();
    }

    static final class Op {
        String name, verb, pathTemplate, returnType;
        final List<Param> params = new ArrayList<Param>();
    }

    static final class Param {
        String javaType, name, bindKind, bindName;
    }

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
        dtos.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (!isEnabled()) return;
        if (cls.isSynthetic()) return;
        if (cls.getClassAnnotation(RestClientAnnotationProcessor.REST_CLIENT_DESC) == null) return;
        // Shape errors (not an interface, not public) are already reported by the
        // client processor over the same class; repeating them would double every
        // message in the build log.
        if (!cls.isInterface() || !cls.isPublic()) return;

        Api api = new Api();
        api.binaryName = cls.getBinaryName();
        api.simpleName = RestClientAnnotationProcessor.simpleName(api.binaryName);
        api.packageName = RestClientAnnotationProcessor.packageOf(api.binaryName);
        api.serverSimpleName = api.simpleName + "Server";
        api.dispatcherSimpleName = api.simpleName + "Dispatcher";

        boolean anyError = false;
        for (MethodInfo m : cls.getMethods()) {
            if (m.isStatic() || m.isSynthetic() || m.isConstructor() || !m.isAbstract()) continue;
            if ((m.getAccess() & org.objectweb.asm.Opcodes.ACC_BRIDGE) != 0) continue;

            Op op = new Op();
            op.name = m.getName();

            AnnotationValues va;
            int verbCount = 0;
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.GET_DESC)) != null)    { op.verb = "GET";    op.pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.POST_DESC)) != null)   { op.verb = "POST";   op.pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.PUT_DESC)) != null)    { op.verb = "PUT";    op.pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.DELETE_DESC)) != null) { op.verb = "DELETE"; op.pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(RestClientAnnotationProcessor.PATCH_DESC)) != null)  { op.verb = "PATCH";  op.pathTemplate = va.getString("value"); verbCount++; }
            if (verbCount != 1) continue; // the client processor reports this
            if (op.pathTemplate == null) op.pathTemplate = "";

            Type[] paramTypes = Type.getArgumentTypes(m.getDescriptor());
            List<Map<String, AnnotationValues>> paramAnnotations = m.getParameterAnnotations();
            String[] genericSigs = RestClientAnnotationProcessor
                    .parseGenericParameterSignatures(m.getSignature(), paramTypes.length);

            op.returnType = "void";
            int bodyCount = 0;
            for (int i = 0; i < paramTypes.length; i++) {
                String descriptor = paramTypes[i].getDescriptor();
                String genericSig = genericSigs == null ? null : genericSigs[i];

                if (RestClientAnnotationProcessor.isCallbackType(descriptor)) {
                    // The callback is the client's result channel. On the server it
                    // becomes the return type and disappears from the signature.
                    String payload = RestClientAnnotationProcessor.extractResponsePayload(genericSig);
                    op.returnType = (payload == null || payload.length() == 0)
                            ? "java.lang.Object" : payload;
                    collectDtos(op.returnType, ctx);
                    continue;
                }

                Map<String, AnnotationValues> pa = i < paramAnnotations.size() ? paramAnnotations.get(i) : null;
                Param p = new Param();
                p.javaType = RestClientAnnotationProcessor.javaTypeFor(paramTypes[i], genericSig);
                AnnotationValues bind;
                if (pa != null && (bind = pa.get(RestClientAnnotationProcessor.PATH_DESC)) != null) {
                    p.bindKind = "path";
                    p.bindName = bind.getString("value");
                } else if (pa != null && (bind = pa.get(RestClientAnnotationProcessor.QUERY_DESC)) != null) {
                    p.bindKind = "query";
                    p.bindName = bind.getString("value");
                } else if (pa != null && (bind = pa.get(RestClientAnnotationProcessor.HEADER_DESC)) != null) {
                    p.bindKind = "header";
                    p.bindName = bind.getString("value");
                } else if (pa != null && (bind = pa.get(RestClientAnnotationProcessor.COOKIE_DESC)) != null) {
                    p.bindKind = "cookie";
                    p.bindName = bind.getString("value");
                } else if (pa != null && pa.get(RestClientAnnotationProcessor.BODY_DESC) != null) {
                    p.bindKind = "body";
                    p.bindName = "body";
                    bodyCount++;
                } else {
                    ctx.error(cls, "Parameter " + i + " of " + api.binaryName + "." + op.name
                            + " carries no REST binding annotation, so the dispatcher cannot "
                            + "supply a value for it");
                    anyError = true;
                    continue;
                }
                if (p.bindName == null || p.bindName.length() == 0) p.bindName = "p" + i;
                p.name = RestClientAnnotationProcessor.sanitizeIdentifier(
                        "body".equals(p.bindKind) ? "body" : p.bindName);
                if ("body".equals(p.bindKind)) {
                    collectDtos(p.javaType, ctx);
                } else if (!isBindableScalar(p.javaType)) {
                    // Path/query/header/cookie values arrive as text. Anything that
                    // is not convertible from a String has no defined binding, and
                    // guessing one would silently hand the handler a null.
                    ctx.error(cls, "Parameter " + i + " of " + api.binaryName + "." + op.name
                            + " is bound from the request as text but has type " + p.javaType
                            + ", which cannot be parsed from a string; use @Body for structured input");
                    anyError = true;
                    continue;
                }
                op.params.add(p);
            }
            if (bodyCount > 1) {
                ctx.error(cls, api.binaryName + "." + op.name
                        + " declares more than one @Body parameter; a request has one body");
                anyError = true;
            }
            // Every @Path has to name a placeholder that is actually in the template.
            // A typo bound null, or 0 for a primitive, and the route still matched --
            // so the handler ran with the wrong identifier and nothing said so.
            String[] template = splitTemplate(op.pathTemplate);
            for (int pi = 0; pi < op.params.size(); pi++) {
                Param p = op.params.get(pi);
                if ("path".equals(p.bindKind) && placeholderIndex(template, p.bindName) < 0) {
                    ctx.error(cls, api.binaryName + "." + op.name + " binds @Path(\""
                            + p.bindName + "\") but the route " + op.pathTemplate
                            + " has no {" + p.bindName + "} to bind it to");
                    anyError = true;
                }
            }
            // And the other direction, which is the half that was missing. A
            // placeholder nothing binds is worse than a typo: the CLIENT
            // substitutes the placeholder's own name, so it requests /users/id
            // literally, while the SERVER matches any value there and passes it
            // to nobody. Both halves compile, and the route they agree on is one
            // whose variable cannot be supplied or read.
            for (int ti = 0; ti < template.length; ti++) {
                String name = placeholderName(template[ti]);
                if (name == null) {
                    continue;
                }
                if (hasSecondPlaceholder(template[ti])) {
                    // Said out loud rather than matched approximately. "{a}-{b}"
                    // has no single reading -- where one value ends and the next
                    // begins is a guess -- and a server that guesses binds
                    // something the client never meant. The client half accepts
                    // this shape, so the developer is told where the disagreement
                    // is instead of meeting a route that never matches.
                    ctx.error(cls, api.binaryName + "." + op.name + " declares the route "
                            + op.pathTemplate + ", whose segment '" + template[ti]
                            + "' holds more than one placeholder. Where one value ends "
                            + "and the next begins cannot be decided from the path, so "
                            + "give each placeholder its own segment.");
                    anyError = true;
                    continue;
                }
                boolean bound = false;
                for (int pi = 0; pi < op.params.size(); pi++) {
                    Param p = op.params.get(pi);
                    if ("path".equals(p.bindKind) && name.equals(p.bindName)) {
                        bound = true;
                        break;
                    }
                }
                if (!bound) {
                    ctx.error(cls, api.binaryName + "." + op.name + " declares the route "
                            + op.pathTemplate + ", but nothing binds {" + name + "}. Add a "
                            + "parameter annotated @Path(\"" + name + "\"), or take the "
                            + "placeholder out of the path.");
                    anyError = true;
                }
            }
            api.ops.add(op);
        }
        // Two routes of the same verb and shape generate the same predicate, and
        // dispatch takes the first that matches -- so the second is unreachable
        // however it is called. The names differ; the SHAPE is what the router sees.
        Map<String, String> shapes = new LinkedHashMap<String, String>();
        for (Op op : api.ops) {
            String shape = op.verb + " " + placeholderShape(op.pathTemplate);
            String first = shapes.get(shape);
            if (first != null) {
                ctx.error(cls, api.binaryName + "." + op.name + " and " + first
                        + " are both " + shape + " once the placeholder names are"
                        + " taken out, so only the first can ever be reached");
                anyError = true;
                continue;
            }
            // Equality is not the only way two routes collide. "/a/{x}/c" and
            // "/a/b/{y}" are different shapes and BOTH answer /a/b/c: a placeholder
            // takes any value in its segment, so two dynamic patterns can overlap
            // without either being more specific. Literal-first ordering cannot
            // break that tie because neither is literal, and dispatch returns from
            // whichever it emits first, so the contract gives that request no
            // stable meaning.
            String clash = overlappingShape(shapes.keySet(), shape);
            // Unless one of the two is wholly literal. The dispatcher emits every
            // route without a placeholder before every route with one, so
            // "GET /users/me" beside "GET /users/{id}" is decided by that order:
            // the literal takes its own path and every other value falls through.
            // The comment above is right that literal-first cannot break a tie
            // between two DYNAMIC shapes -- and equally, it does break this one.
            if (clash != null && isLiteralShape(clash) != isLiteralShape(shape)) {
                clash = null;
            }
            if (clash != null) {
                ctx.error(cls, api.binaryName + "." + op.name + " answers " + shape
                        + ", which " + shapes.get(clash) + " also answers as " + clash
                        + ". A path satisfying both is dispatched to whichever comes "
                        + "first, so give them different paths.");
                anyError = true;
                continue;
            }
            shapes.put(shape, op.name);
        }
        if (!anyError && !api.ops.isEmpty()) {
            accepted.put(api.binaryName, api);
        }
    }

    /// A route with its placeholder NAMES removed, which is all the generated
    /// router matches on: "/pets/{id}" and "/pets/{name}" are one shape.
    /** The descriptor the scanner keys @Generated by. */
    private static final String GENERATED = "Lcom/codename1/backend/annotations/Generated;";

    private static String placeholderShape(String template) {
        String[] parts = splitTemplate(template);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            // The LITERALS around a placeholder are part of the shape. Collapsing
            // the whole segment to "{}" made "/{name}.json" and "/{name}.xml" the
            // same shape, so the duplicate check refused a pair that no single
            // request can satisfy -- and the matcher supports them now, which is
            // what makes the refusal wrong rather than conservative.
            if (isPlaceholder(parts[i])) {
                sb.append('/').append(placeholderPrefix(parts[i])).append("{}")
                  .append(placeholderSuffix(parts[i]));
            } else {
                sb.append('/').append(parts[i]);
            }
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    /// Records any application class reachable as a body or a result so a codec is
    /// emitted for it. `java.util.List<Foo>` contributes Foo, not List.
    /**
     * A DTO whose public fields the generated codec can actually round-trip.
     *
     * The encoder writes every public field; the decoder assigns them after a
     * no-argument construction, so a FINAL one is written and then silently not
     * read back. The handler gets the initializer and the client's value is gone,
     * with nothing failing at build time or at request time to say so. Refused
     * here instead: the contract cannot be honoured, so it should not compile.
     */
    private void requireAssignableFields(String binaryName, AnnotatedClass cls,
            ProcessorContext ctx) {
        for (FieldInfo f : transferredFields(cls, ctx)) {
            if (f.isFinal()) {
                ctx.error(cls, binaryName + "." + f.getName() + " is public and final, "
                        + "so the generated decoder cannot assign it: the field would "
                        + "be sent by the client and silently dropped on arrival. Drop "
                        + "the final, or keep the field out of the transferred shape.");
            }
        }
    }

    /**
     * Refuses to generate over a class the project already has.
     *
     * What is compiled here lands in the same output directory, so a name that
     * already exists is simply overwritten -- silently, because what is generated
     * compiles perfectly well. Every family generated here needs this, not just
     * the first one somebody thought of: the server interface, the dispatcher and
     * each DTO codec are all derived names a developer could have used.
     */
    private boolean wouldReplaceAnExistingClass(String binaryName, String what,
            ProcessorContext ctx) {
        AnnotatedClass existing = ctx.lookup(binaryName.replace('.', '/'));
        if (existing == null) {
            return false;
        }
        if (existing.getClassAnnotations().containsKey(GENERATED)) {
            // Our own output from an earlier pass. An incremental build scans
            // target/classes, so without this an unchanged contract could be
            // processed exactly once per clean -- the second run reported the
            // ApiServer, ApiDispatcher and every DTO codec as existing
            // application classes. A genuine collision carries no marker.
            return false;
        }
        ctx.error(binaryName + " already exists, and the " + what + " generated for "
                + "this API would replace it. Rename that class, or rename the "
                + "interface the name is derived from.");
        return true;
    }

    /**
     * Every public instance field a DTO carries, its superclasses included.
     *
     * AnnotatedClass.getFields() reads ONE class file, so an inherited field was
     * invisible to all four passes that use it: it was not validated, its type was
     * never collected, the encoder never wrote it and the decoder never read it. A
     * subclass went over the wire missing everything its base declared, silently
     * and on both ends. Walks up until the superclass is outside the index, which
     * is where the JDK begins; a field hidden by one of the same name in a subclass
     * is taken from the subclass, as Java resolves it.
     */
    /** The already-seen shape that a path could satisfy along with this one, or null. */
    private static String overlappingShape(Set<String> seen, String shape) {
        for (String other : seen) {
            if (shapesOverlap(other, shape)) {
                return other;
            }
        }
        return null;
    }

    /** Same verb, same segment count, and every pair of segments compatible. */
    private static boolean shapesOverlap(String left, String right) {
        int leftSpace = left.indexOf(' ');
        int rightSpace = right.indexOf(' ');
        if (leftSpace < 0 || rightSpace < 0
                || !left.substring(0, leftSpace).equals(right.substring(0, rightSpace))) {
            return false;
        }
        String[] a = left.substring(leftSpace + 1).split("/", -1);
        String[] b = right.substring(rightSpace + 1).split("/", -1);
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            // ONE implementation of this rule, in the controller processor. There
            // used to be two, and they disagreed: that one learned that "{}.json"
            // and "{}.xml" cannot both match while this one still called every pair
            // of variable-carrying segments a collision, so a contract the matcher
            // handles was refused here and accepted there. A rule copied is a rule
            // that will be fixed once.
            if (!RestControllerAnnotationProcessor.segmentsOverlap(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    private List<FieldInfo> transferredFields(AnnotatedClass cls, ProcessorContext ctx) {
        List<FieldInfo> out = new ArrayList<FieldInfo>();
        Set<String> seen = new LinkedHashSet<String>();
        AnnotatedClass at = cls;
        while (at != null) {
            for (FieldInfo f : at.getFields()) {
                if (f.isStatic() || !f.isPublic()) {
                    continue;
                }
                if ((f.getAccess() & org.objectweb.asm.Opcodes.ACC_SYNTHETIC) != 0) {
                    continue;
                }
                if (seen.add(f.getName())) {
                    out.add(f);
                }
            }
            String superName = at.getSuperInternalName();
            at = superName == null ? null : ctx.lookup(superName);
        }
        return out;
    }

    private void collectDtos(String javaType, ProcessorContext ctx) {
        if (javaType == null) return;
        String t = javaType.trim();
        int lt = t.indexOf('<');
        if (lt >= 0) {
            String outer = t.substring(0, lt);
            String inner = t.substring(lt + 1, t.length() - 1);
            if ("java.util.List".equals(outer) || "java.util.Set".equals(outer)
                    || "java.util.Collection".equals(outer)) {
                // A collection OF a collection of DTOs encodes wrongly and quietly:
                // fieldToJson applies the generated codec to the elements of the
                // outer collection only, and an element that is itself a collection
                // starts with "java." so it is handed to the writer untouched --
                // where each DTO inside becomes the JSON string of its toString().
                // Refused for the same reason a Map of DTOs is: the codec cannot
                // express the shape, and producing the wrong JSON is worse than
                // refusing to compile.
                if (inner.indexOf('<') >= 0 && namesADto(inner, ctx)) {
                    ctx.error("A transferred field or return typed " + t + " cannot be "
                            + "encoded: the generated codec reaches the elements of the "
                            + "outer collection only, so the DTOs inside " + inner
                            + " would be written as their toString(). Use a collection "
                            + "of a DTO that holds the inner collection.");
                    return;
                }
                collectDtos(inner, ctx);
            } else if ("java.util.Map".equals(outer)) {
                // A Map of JDK values round-trips; a Map whose values are a DTO
                // does not, and does so QUIETLY. Only List and Set recursed here,
                // so no codec was generated for the value type: the decoder does a
                // guarded Map cast and leaves a decoded Map in a field declared as
                // that DTO, and the encoder writes its toString() as a JSON string.
                // Both halves compile and neither works, so the shape is refused
                // rather than mistranslated. Generating conversions for it is a
                // feature, not a fix for this.
                // The KEY as well. A JSON object's member names are strings, always,
                // so Map<Integer,String> receives string keys under an integer-keyed
                // declaration: a get(Integer) finds nothing and iterating the entries
                // as Integer throws, while encoding turns the integers back into
                // strings. Only a String key round-trips.
                String key = mapKeyType(inner);
                String rawKey = key.indexOf('<') < 0 ? key : key.substring(0, key.indexOf('<'));
                if (!"java.lang.String".equals(rawKey) && !"java.lang.Object".equals(rawKey)) {
                    ctx.error("A transferred field or return typed " + t + " cannot be "
                            + "decoded: a JSON object's names are strings, so " + rawKey
                            + " keys arrive as String and neither lookup nor iteration "
                            + "works. Key the map by String.");
                    return;
                }
                String value = mapValueType(inner);
                // A Map's VALUES are handed over exactly as the parser made them:
                // unlike a List or a Set, nothing walks them applying the element
                // type. The parser answers Long for every JSON integer and Double
                // for every real, so Map<String,Integer> is a map of Long at
                // runtime -- the cast erases, and the handler's first read as an
                // Integer throws. Declaring Long or Double says what actually
                // arrives; the numeric types that need converting do not.
                String rawValue = value.indexOf('<') < 0 ? value
                        : value.substring(0, value.indexOf('<'));
                // The raw type is not the whole answer: Map<String,List<Integer>>
                // has an acceptable OUTER value and an Integer inside it that the
                // parser never produces. Nothing converts a map's values at any
                // depth, so every level has to be a type that arrives as itself.
                if (!namesADto(value, ctx) && !mapValueArrivesAsDeclared(value)) {
                    ctx.error("A transferred field or return typed " + t + " cannot be "
                            + "decoded: a map's values arrive as the parser built them, so "
                            + value + " would really be " + parserTypeFor(rawValue)
                            + " and reading it as " + rawValue + " throws. Use a Map of "
                            + "Long, Double, Boolean, String, Map or List, or a DTO.");
                    return;
                }
                if (namesADto(value, ctx)) {
                    ctx.error("A transferred field typed " + t + " cannot be encoded: "
                            + "the generated codec round-trips a Map of JDK values "
                            + "only, and " + value + " would be silently replaced by "
                            + "a plain Map on the way in. Use a list of a DTO that "
                            + "carries the key, or a Map with JDK value types.");
                }
            }
            return;
        }
        if (t.indexOf('.') < 0) return;                            // a primitive
        if (t.startsWith("java.")) {
            // Not every JDK type round-trips, and the ones that do not fail
            // SILENTLY in both directions. A field typed java.util.Date is the
            // plain case: the client sends a number, so the decoder's guarded
            // cast to Date never matches and the field arrives null, while the
            // encoder hands the Date to Json and gets its toString() -- the
            // contract compiles at both ends and the value survives neither.
            // The same is true of BigDecimal, UUID and every java.time type, so
            // the answer is the supported set rather than a case for Date.
            if (!CODEC_JDK_TYPES.contains(t)) {
                ctx.error("A transferred field or return typed " + t + " cannot be "
                        + "encoded: the generated codec handles the primitives and their "
                        + "boxes, String, byte[], and List, Set or Map of those. " + t
                        + " would arrive null and be written as its toString(). Carry it "
                        + "as a long of epoch milliseconds or as a String.");
            }
            return;
        }
        AnnotatedClass cls = ctx.lookup(t.replace('.', '/'));
        if (cls == null || cls.isInterface() || cls.isEnum()) return;
        if (dtos.containsKey(t)) return;
        requireAssignableFields(t, cls, ctx);
        dtos.put(t, cls);
        for (FieldInfo f : transferredFields(cls, ctx)) {
            collectDtos(fieldJavaType(f), ctx);
        }
    }

    /**
     * The JDK types a generated codec can convert in BOTH directions. Taken from
     * the branches of the conversion above, plus the containers handled by the
     * generic path and byte[]; a type added there belongs here too. Anything else
     * under java.* reaches the guarded cast, which cannot match a value the JSON
     * parser produced.
     */
    private static final Set<String> CODEC_JDK_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "java.lang.String", "java.lang.Integer", "java.lang.Long",
                    "java.lang.Double", "java.lang.Boolean", "java.lang.Float",
                    "java.lang.Short", "java.lang.Byte",
                    "java.util.List", "java.util.Set", "java.util.Collection",
                    "java.util.Map")));

    /** A shape with no placeholder at all, which the dispatcher emits first. */
    private static boolean isLiteralShape(String shape) {
        return shape.indexOf("{}") < 0;
    }

    /**
     * What a Map's values may be declared as, which is exactly what Json.parse
     * produces: it answers Long for every JSON integer and Double for every real,
     * regardless of how the field is declared, and nothing converts a map's
     * values afterwards the way collection elements are converted.
     */
    private static final Set<String> PARSED_MAP_VALUE_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "java.lang.Object", "java.lang.String", "java.lang.Long",
                    "java.lang.Double", "java.lang.Boolean",
                    // Map and List only. Set and Collection are NOT here even
                    // though a collection field elsewhere may be declared as
                    // either: a JSON array always arrives as a List, and the
                    // element conversion that turns one into a Set runs for
                    // FIELDS, never for a map's values -- so Map<String,Set<...>>
                    // hands the handler a List under a Set declaration and throws
                    // on first use. What a map's value may be declared as is
                    // exactly what the parser hands over, with nothing in between.
                    "java.util.Map", "java.util.List")));

    /** What the parser really answers where the declared type says otherwise. */
    private static String parserTypeFor(String declared) {
        if ("java.lang.Integer".equals(declared) || "java.lang.Short".equals(declared)
                || "java.lang.Byte".equals(declared)) {
            return "a Long";
        }
        if ("java.lang.Float".equals(declared)) {
            return "a Double";
        }
        return "something else";
    }

    /**
     * A declared collection this codec converts element by element. Collection
     * belongs with List and Set: the parser answers an ArrayList either way, so
     * a Collection<Note> that is NOT recognised here falls through to a guarded
     * cast, which erases -- the handler is then holding a collection of Map under
     * a Collection<Note> declaration and throws on its first element. The three
     * have to be listed everywhere any of them is, which is why this is one
     * method rather than three copies of the same disjunction.
     */
    private static boolean isCollectionShape(String javaType) {
        return javaType.startsWith("java.util.List<")
                || javaType.startsWith("java.util.Set<")
                || javaType.startsWith("java.util.Collection<");
    }

    /**
     * Whether a map value's declared type is what the parser really hands over,
     * all the way down. A map's values are never converted -- not at the top
     * level and not inside a nested container -- so each level must already be
     * what arrives: Map, List, String, Long, Double, Boolean or Object.
     */
    private static boolean mapValueArrivesAsDeclared(String javaType) {
        int lt = javaType.indexOf('<');
        String raw = lt < 0 ? javaType : javaType.substring(0, lt);
        if (!PARSED_MAP_VALUE_TYPES.contains(raw)) {
            return false;
        }
        if (lt < 0) {
            return true;
        }
        int end = javaType.lastIndexOf('>');
        if (end <= lt) {
            return true;
        }
        List<String> args = RestControllerAnnotationProcessor.splitTypeArguments(
                javaType.substring(lt + 1, end));
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("?")) {
                continue;
            }
            if (!mapValueArrivesAsDeclared(arg)) {
                return false;
            }
        }
        return true;
    }

    /** The key half of a Map's type arguments, honouring nested generics. */
    private static String mapKeyType(String inner) {
        int depth = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                return inner.substring(0, i).trim();
            }
        }
        return inner.trim();
    }

    /** The value half of a Map's type arguments, honouring nested generics. */
    private static String mapValueType(String inner) {
        int depth = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                return inner.substring(i + 1).trim();
            }
        }
        return inner.trim();
    }

    /** Whether a type, or anything inside its type arguments, is one of ours. */
    private static boolean namesADto(String javaType, ProcessorContext ctx) {
        if (javaType == null) {
            return false;
        }
        String[] tokens = javaType.split("[<>,]");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.length() == 0 || token.startsWith("java.") || token.indexOf('.') < 0) {
                continue;
            }
            AnnotatedClass cls = ctx.lookup(token.replace('.', '/'));
            if (cls != null && !cls.isInterface() && !cls.isEnum()) {
                return true;
            }
        }
        return false;
    }

    private static String fieldJavaType(FieldInfo f) {
        String sig = f.getSignature();
        if (sig != null && sig.length() > 0) {
            return RestClientAnnotationProcessor.jvmSignatureToJavaType(sig);
        }
        return RestClientAnnotationProcessor.jvmSignatureToJavaType(f.getDescriptor());
    }

    private static boolean isBindableScalar(String javaType) {
        return "java.lang.String".equals(javaType) || "int".equals(javaType) || "long".equals(javaType)
                || "boolean".equals(javaType) || "double".equals(javaType) || "float".equals(javaType)
                || "short".equals(javaType) || "byte".equals(javaType)
                || "java.lang.Integer".equals(javaType) || "java.lang.Long".equals(javaType)
                || "java.lang.Boolean".equals(javaType) || "java.lang.Double".equals(javaType)
                || "java.lang.Float".equals(javaType) || "java.lang.Short".equals(javaType)
                || "java.lang.Byte".equals(javaType);
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (!isEnabled() || ctx.hasErrors() || accepted.isEmpty()) return;
        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (Api api : accepted.values()) {
            String server = qualify(api.packageName, api.serverSimpleName);
            String dispatcher = qualify(api.packageName, api.dispatcherSimpleName);
            if (wouldReplaceAnExistingClass(server, "server interface", ctx)
                    || wouldReplaceAnExistingClass(dispatcher, "dispatcher", ctx)) {
                return;
            }
            sources.put(server, generateServerInterface(api));
            sources.put(dispatcher, generateDispatcher(api));
        }
        for (Map.Entry<String, AnnotatedClass> e : dtos.entrySet()) {
            String pkg = RestClientAnnotationProcessor.packageOf(e.getKey());
            String simple = RestClientAnnotationProcessor.simpleName(e.getKey()) + "Json";
            String codec = qualify(pkg, simple);
            if (wouldReplaceAnExistingClass(codec, "JSON codec", ctx)) {
                return;
            }
            sources.put(codec, generateDtoCodec(e.getKey(), e.getValue(), ctx));
        }
        try {
            List<java.io.File> cp = new ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated @RestClient server sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size() + " @RestClient server dispatcher(s) and "
                + dtos.size() + " DTO codec(s)");
    }

    private static String qualify(String pkg, String simple) {
        return pkg.length() == 0 ? simple : pkg + "." + simple;
    }

    private static String codecFor(String dtoBinaryName) {
        return qualify(RestClientAnnotationProcessor.packageOf(dtoBinaryName),
                RestClientAnnotationProcessor.simpleName(dtoBinaryName) + "Json");
    }

    private static String generateServerInterface(Api api) {
        StringBuilder sb = new StringBuilder(1024);
        if (api.packageName.length() > 0) sb.append("package ").append(api.packageName).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations from ").append(api.binaryName).append(". Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public interface ").append(api.serverSimpleName).append(" {\n");
        for (Op op : api.ops) {
            sb.append("    ").append(op.returnType).append(' ').append(op.name).append('(');
            for (int i = 0; i < op.params.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(op.params.get(i).javaType).append(' ').append(op.params.get(i).name);
            }
            sb.append(") throws Exception;\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Dispatcher
    // ----------------------------------------------------------------

    private static String generateDispatcher(Api api) {
        StringBuilder sb = new StringBuilder(8192);
        if (api.packageName.length() > 0) sb.append("package ").append(api.packageName).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations from ").append(api.binaryName).append(". Do not edit.\n");
        sb.append("//\n");
        sb.append("// References nothing outside java.*, on purpose: generated server code has to\n");
        sb.append("// link into a binary that has no Codename One implementation. JSON text is\n");
        sb.append("// parsed and written by the caller, so `body` arrives as an already-decoded\n");
        sb.append("// Map/List/String and the result goes back the same way.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(api.dispatcherSimpleName).append(" {\n");
        sb.append("    private final ").append(api.serverSimpleName).append(" impl;\n\n");
        sb.append("    public ").append(api.dispatcherSimpleName).append("(")
          .append(api.serverSimpleName).append(" impl) {\n        this.impl = impl;\n    }\n\n");

        sb.append("    /** True when this API has a route for the verb and path. */\n");
        sb.append("    public boolean hasRoute(String method, String rawPath) {\n");
        sb.append("        String path = stripQuery(rawPath);\n");
        sb.append("        String[] seg = split(path);\n");
        for (Op op : api.ops) {
            sb.append("        if(").append(routeCondition(op)).append(") return true;\n");
        }
        sb.append("        return false;\n");
        sb.append("    }\n\n");

        sb.append("    /**\n");
        sb.append("     * Invokes the handler for this request.\n");
        sb.append("     *\n");
        sb.append("     * headers may be null. body is the decoded JSON value (Map/List/String) or\n");
        sb.append("     * null. Returns a JSON-ready value; check hasRoute first, because a handler\n");
        sb.append("     * returning null and no route at all both come back as null.\n");
        sb.append("     */\n");
        sb.append("    public Object dispatch(String method, String rawPath, java.util.Map headers, Object body) throws Exception {\n");
        sb.append("        String path = stripQuery(rawPath);\n");
        sb.append("        String query = queryOf(rawPath);\n");
        sb.append("        String[] seg = split(path);\n");
        // Order matters HERE and nowhere else in this file: dispatch returns from
        // the first branch that matches, while hasRoute is an or and the interface
        // is only declarations. A route with a placeholder accepts any value in that
        // segment, so "GET /notes/{id}" declared before "GET /notes/latest" answered
        // /notes/latest itself and the literal method could never run. The generator
        // for @RestController already sorts for this; this half did not.
        List<Op> ordered = new ArrayList<Op>(api.ops);
        Collections.sort(ordered, new java.util.Comparator<Op>() {
            public int compare(Op a, Op b) {
                int byVerb = a.verb.compareTo(b.verb);
                if (byVerb != 0) {
                    return byVerb;
                }
                boolean aVar = placeholderShape(a.pathTemplate).indexOf("{}") >= 0;
                boolean bVar = placeholderShape(b.pathTemplate).indexOf("{}") >= 0;
                if (aVar != bVar) {
                    return aVar ? 1 : -1;
                }
                return b.pathTemplate.length() - a.pathTemplate.length();
            }
        });
        for (Op op : ordered) {
            emitRoute(sb, op);
        }
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        emitHelpers(sb);
        sb.append("}\n");
        return sb.toString();
    }

    private static String routeCondition(Op op) {
        String[] template = splitTemplate(op.pathTemplate);
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(op.verb).append("\".equals(method) && seg.length == ").append(template.length);
        for (int i = 0; i < template.length; i++) {
            if (!isPlaceholder(template[i])) {
                sb.append(" && \"").append(RestClientAnnotationProcessor.escape(template[i]))
                  .append("\".equals(seg[").append(i).append("])");
            } else {
                // A placeholder stands for a NON-EMPTY run within its segment, and
                // any literal text around it has to match too. /pets/ splits to the
                // same COUNT as /pets/{id}, so without the length test the route ran
                // with id set to the empty string rather than not matching -- a path
                // the contract does not describe. The overlap checker models a
                // placeholder as [^/]+ and the @RestController router refuses an
                // empty variable, so this is the rule the rest of the system already
                // applies.
                String prefix = placeholderPrefix(template[i]);
                String suffix = placeholderSuffix(template[i]);
                if (prefix.length() > 0) {
                    sb.append(" && seg[").append(i).append("].startsWith(\"")
                      .append(RestClientAnnotationProcessor.escape(prefix)).append("\")");
                }
                if (suffix.length() > 0) {
                    sb.append(" && seg[").append(i).append("].endsWith(\"")
                      .append(RestClientAnnotationProcessor.escape(suffix)).append("\")");
                }
                sb.append(" && seg[").append(i).append("].length() > ")
                  .append(prefix.length() + suffix.length());
            }
        }
        return sb.toString();
    }

    private static void emitRoute(StringBuilder sb, Op op) {
        String[] template = splitTemplate(op.pathTemplate);
        sb.append("        if(").append(routeCondition(op)).append(") {\n");
        // Locals are positional (_a0, _a1, ...) rather than the parameter's own name:
        // a @Body parameter called "body" would otherwise shadow dispatch()'s own
        // body argument and fail to compile. Generated identifiers must not be able
        // to collide with the generator's.
        for (int pi = 0; pi < op.params.size(); pi++) {
            Param p = op.params.get(pi);
            sb.append("            ").append(p.javaType).append(" _a").append(pi).append(" = ");
            if ("path".equals(p.bindKind)) {
                int idx = placeholderIndex(template, p.bindName);
                if (idx < 0) {
                    sb.append(fromText(p.javaType, "null"));
                } else {
                    // Only the part BETWEEN the literals is the value. The
                    // condition above has already proved both are present, so the
                    // arithmetic here cannot go out of range.
                    String slice = "seg[" + idx + "]";
                    int prefixLength = placeholderPrefix(template[idx]).length();
                    int suffixLength = placeholderSuffix(template[idx]).length();
                    if (prefixLength > 0 || suffixLength > 0) {
                        slice = slice + ".substring(" + prefixLength
                                + (suffixLength > 0 ? ", " + slice + ".length() - " + suffixLength : "")
                                + ")";
                    }
                    sb.append(fromText(p.javaType, "decodePath(" + slice + ")"));
                }
            } else if ("query".equals(p.bindKind)) {
                sb.append(fromText(p.javaType, "queryParam(query, \""
                        + RestClientAnnotationProcessor.escape(p.bindName) + "\")"));
            } else if ("header".equals(p.bindKind)) {
                sb.append(fromText(p.javaType, "header(headers, \""
                        + RestClientAnnotationProcessor.escape(p.bindName) + "\")"));
            } else if ("cookie".equals(p.bindKind)) {
                sb.append(fromText(p.javaType, "cookie(headers, \""
                        + RestClientAnnotationProcessor.escape(p.bindName) + "\")"));
            } else {
                sb.append(fromBody(p.javaType));
            }
            sb.append(";\n");
        }
        sb.append("            ");
        if (!"void".equals(op.returnType)) {
            sb.append(op.returnType).append(" _result = ");
        }
        sb.append("impl.").append(op.name).append('(');
        for (int i = 0; i < op.params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("_a").append(i);
        }
        sb.append(");\n");
        if ("void".equals(op.returnType)) {
            sb.append("            return \"\";\n");
        } else {
            sb.append("            return ").append(toJsonValue(op.returnType, "_result")).append(";\n");
        }
        sb.append("        }\n");
    }

    /// Wraps a String-valued expression in the conversion its target type needs.
    /// A null stays null for the boxed types rather than throwing, so an absent
    /// optional query parameter is not a 500.
    private static String fromText(String javaType, String expr) {
        if ("java.lang.String".equals(javaType)) return expr;
        if ("int".equals(javaType))     return "parseInt(" + expr + ")";
        if ("long".equals(javaType))    return "parseLong(" + expr + ")";
        if ("boolean".equals(javaType)) return "parseBool(" + expr + ")";
        if ("double".equals(javaType))  return "parseDouble(" + expr + ")";
        // Parsed AT the target width, not parsed wide and cast down. A cast
        // wraps: "40000" for a short became -25536 and "256" for a byte became
        // 0, so the handler ran on a number the client never sent, from a value
        // the client controls. The boxed forms below were always right about
        // this, because Short.valueOf throws -- only the primitives were cast.
        if ("float".equals(javaType))   return "parseFloat(" + expr + ")";
        if ("short".equals(javaType))   return "parseShort(" + expr + ")";
        if ("byte".equals(javaType))    return "parseByte(" + expr + ")";
        if ("java.lang.Integer".equals(javaType)) return "boxInt(" + expr + ")";
        if ("java.lang.Long".equals(javaType))    return "boxLong(" + expr + ")";
        if ("java.lang.Double".equals(javaType))  return "boxDouble(" + expr + ")";
        if ("java.lang.Float".equals(javaType))   return "boxFloat(" + expr + ")";
        if ("java.lang.Short".equals(javaType))   return "boxShort(" + expr + ")";
        if ("java.lang.Byte".equals(javaType))    return "boxByte(" + expr + ")";
        if ("java.lang.Boolean".equals(javaType)) return "boxBoolean(" + expr + ")";
        return expr;
    }

    /// The request body, converted to the handler's parameter type.
    ///
    /// The body is whatever the client sent, so its SHAPE is attacker controlled:
    /// a route declaring a DTO can be handed a string, a number or an array. None
    /// of these conversions may therefore rest on a cast. ParparVM's CHECKCAST is
    /// unchecked by default (see CLAUDE.md), so `(Map)body` over a String does not
    /// throw -- it reads a String's header as a Map's and the process dies, which
    /// on a server takes every in-flight connection with it. Every path below
    /// either tests with instanceof or converts through text.
    private static String fromBody(String javaType) {
        // The STRICT helper: a declared String body must have arrived as a JSON
        // string. The lenient one below exists for scalars, where converting
        // through text is the point.
        if ("java.lang.String".equals(javaType)) return "bodyAsString(body)";
        if (isCollectionShape(javaType)) {
            String element = javaType.substring(javaType.indexOf('<') + 1, javaType.length() - 1);
            // A Set parameter has to receive a Set. bodyAsList hands back an
            // ArrayList, and casting that to Set is exactly the cast the comment
            // above warns about: the JVM throws ClassCastException before the
            // handler runs, and the translated target does not check at all, so it
            // carries an ArrayList in a Set-typed field until something reads it as
            // one. setFromList converts instead of asserting.
            boolean isSet = javaType.startsWith("java.util.Set<");
            String decoded;
            if (element.startsWith("java.")) {
                // Converted element by element, not handed over raw. The JSON reader
                // produces Long for every integer and Double for every real, so a
                // List<Integer> arrives full of Longs: on the JVM the handler gets a
                // ClassCastException the first time it reads one, and on the
                // translated target the cast is unchecked, so it reads an Integer's
                // fields out of a Long and carries on. The DTO branch below already
                // converts; this one used not to.
                decoded = "listOfValues(bodyAsList(body), new FromValue() {\n"
                        + "                public Object convert(Object v) { return "
                        + fieldFromJson(element, "v") + "; }\n"
                        + "            })";
            } else {
                decoded = "listFromMaps(bodyAsList(body), new FromMap() {\n"
                        + "                public Object convert(java.util.Map m) { return "
                        + codecFor(element) + ".fromMap(m); }\n"
                        + "            })";
            }
            if (isSet) {
                decoded = "setFromList(" + decoded + ")";
            }
            return "(" + javaType + ")(Object)" + decoded;
        }
        // A primitive or boxed scalar goes through the same text conversion the
        // query and path parameters use, so a JSON number reaching an `int` body
        // behaves the same as one reaching an `int` query parameter.
        if (javaType.indexOf('.') < 0 || isBoxedScalar(javaType)) {
            return fromText(javaType, "bodyAsText(body)");
        }
        if (javaType.startsWith("java.")) {
            return guardedCast(javaType, "body");
        }
        return codecFor(javaType) + ".fromMap(bodyAsMap(body))";
    }

    private static boolean isBoxedScalar(String javaType) {
        return "java.lang.Integer".equals(javaType) || "java.lang.Long".equals(javaType)
                || "java.lang.Double".equals(javaType) || "java.lang.Float".equals(javaType)
                || "java.lang.Short".equals(javaType) || "java.lang.Byte".equals(javaType)
                || "java.lang.Boolean".equals(javaType);
    }

    /// `expr` narrowed to `javaType` when it already is one, and null when it is
    /// not -- an instanceof rather than a cast, for the reason in {@link #fromBody}.
    /// `expr` is evaluated twice, so it must stay side-effect free (it is always a
    /// local or a Map read).
    private static String guardedCast(String javaType, String expr) {
        String raw = javaType;
        int generic = raw.indexOf('<');
        if (generic > 0) raw = raw.substring(0, generic);
        return "(" + javaType + ")(Object)(" + expr + " instanceof " + raw
                + " ? " + expr + " : null)";
    }

    /// The handler's return value, converted to something the JSON writer accepts.
    private static String toJsonValue(String javaType, String expr) {
        if (isCollectionShape(javaType)) {
            String element = javaType.substring(javaType.indexOf('<') + 1, javaType.length() - 1);
            if (element.startsWith("java.")) {
                // Handed to the writer as it stands, Set included: Json.write emits any
                // Collection as an array. Converting a Set to a List here would fix this
                // one expression and leave a Set reached through a Map or a DTO field
                // still writing itself as a quoted toString(), so the writer is where
                // that belongs.
                return expr;
            }
            return "listToMaps(" + expr + ", new ToMap() {\n"
                    + "                public java.util.Map convert(Object o) { return "
                    + codecFor(element) + ".toMap((" + element + ")o); }\n"
                    + "            })";
        }
        if (javaType.startsWith("java.") || javaType.indexOf('.') < 0) {
            return expr;
        }
        return codecFor(javaType) + ".toMap(" + expr + ")";
    }

    private static void emitHelpers(StringBuilder sb) {
        sb.append("    /** Converts one element of a decoded JSON array into a DTO. */\n");
        sb.append("    private interface FromMap { Object convert(java.util.Map m); }\n");
        sb.append("    private interface ToMap { java.util.Map convert(Object o); }\n\n");
        emitValueCoercion(sb);
        sb.append("    /** Converts one element of a decoded JSON array to its declared type. */\n");
        sb.append("    private interface FromValue { Object convert(Object v); }\n\n");
        sb.append("    private static java.util.List listOfValues(java.util.List raw, FromValue f) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        for(int i = 0 ; i < raw.size() ; i++) {\n");
        sb.append("            out.add(f.convert(raw.get(i)));\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n\n");
        sb.append("    private static java.util.List listFromMaps(java.util.List raw, FromMap f) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        for(int i = 0 ; i < raw.size() ; i++) {\n");
        sb.append("            Object e = raw.get(i);\n");
        // A non-map element is the CLIENT being wrong, not a null. Substituting
        // null for it handed the handler a list with a hole in it -- and the
        // handler dereferences the DTO and answers 500, for input that should
        // have been a 400. A JSON null stays a null, because that is a value the
        // client really sent.
        sb.append("            if(e != null && !(e instanceof java.util.Map)) {\n");
        sb.append("                throw new IllegalArgumentException(\"element \" + i"
                + " + \" of the body is \" + e.getClass().getName()"
                + " + \", not an object\");\n");
        sb.append("            }\n");
        sb.append("            out.add(e == null ? null : f.convert((java.util.Map)e));\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n\n");
        sb.append("    private static java.util.List listToMaps(java.util.Collection raw, ToMap f) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        java.util.Iterator it = raw.iterator();\n");
        sb.append("        while(it.hasNext()) {\n");
        sb.append("            Object e = it.next();\n");
        sb.append("            out.add(e == null ? null : f.convert(e));\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n\n");
        sb.append("    /** The body as a JSON object, or a 400 -- never a cast. */\n");
        sb.append("    private static java.util.Map bodyAsMap(Object body) {\n");
        sb.append("        if(body == null || body instanceof java.util.Map) return (java.util.Map)body;\n");
        sb.append("        throw new IllegalArgumentException(\"a JSON object is required in the request body\");\n");
        sb.append("    }\n\n");
        sb.append("    /** The body as a JSON array, or a 400 -- never a cast. */\n");
        sb.append("    private static java.util.List bodyAsList(Object body) {\n");
        sb.append("        if(body == null || body instanceof java.util.List) return (java.util.List)body;\n");
        sb.append("        throw new IllegalArgumentException(\"a JSON array is required in the request body\");\n");
        sb.append("    }\n\n");
        // Declaring @Body String does not make the body a string. A client can
        // send the number 1 or an object, and String.valueOf turned those into
        // "1" and "{a=1}" as though they had been sent as JSON strings -- the
        // same coercion the DTO field path was fixed for, on the top-level body.
        sb.append("    private static String bodyAsString(Object body) {\n");
        sb.append("        if(body == null || body instanceof String) { return (String)body; }\n");
        sb.append("        throw new IllegalArgumentException(\"a JSON string is required in the "
                + "request body, not \" + body.getClass().getName());\n");
        sb.append("    }\n\n");
        // The lenient twin, and only for scalars: `@Body int` is fed by rendering
        // whatever arrived and parsing it, so that a JSON number reaching an int
        // body behaves like one reaching an int query parameter. Widening this to
        // String is what let an object arrive as "{a=1}".
        sb.append("    private static String bodyAsText(Object body) {\n");
        sb.append("        return body == null ? null : String.valueOf(body);\n");
        sb.append("    }\n\n");
        sb.append("    private static String stripQuery(String rawPath) {\n");
        sb.append("        if(rawPath == null) return \"\";\n");
        sb.append("        int q = rawPath.indexOf('?');\n");
        sb.append("        return q < 0 ? rawPath : rawPath.substring(0, q);\n");
        sb.append("    }\n\n");
        sb.append("    private static String queryOf(String rawPath) {\n");
        sb.append("        if(rawPath == null) return \"\";\n");
        sb.append("        int q = rawPath.indexOf('?');\n");
        sb.append("        return q < 0 ? \"\" : rawPath.substring(q + 1);\n");
        sb.append("    }\n\n");
        sb.append("    private static String[] split(String path) {\n");
        sb.append("        return splitOn(path, '/');\n");
        sb.append("    }\n\n");
        sb.append("    private static String queryParam(String query, String name) {\n");
        sb.append("        if(query == null || query.length() == 0) return null;\n");
        sb.append("        String[] pairs = splitOn(query, '&');\n");
        sb.append("        for(int i = 0 ; i < pairs.length ; i++) {\n");
        sb.append("            int eq = pairs[i].indexOf('=');\n");
        // The NAME is decoded before it is compared. A legal annotation name that has
        // to be encoded on the wire -- @Query("filter[name]") goes out as
        // filter%5Bname%5D, which is what the generated client sends -- otherwise
        // never matched the annotation text, and the two halves of one contract
        // failed to bind to each other.
        sb.append("            if(eq > 0 && decodeQuery(pairs[i].substring(0, eq)).equals(name)) return decodeQuery(pairs[i].substring(eq + 1));\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        sb.append("    /** Header lookup is case-insensitive: HTTP does not guarantee header case. */\n");
        sb.append("    private static String header(java.util.Map headers, String name) {\n");
        sb.append("        if(headers == null) return null;\n");
        sb.append("        Object direct = headers.get(name);\n");
        sb.append("        if(direct != null) return String.valueOf(direct);\n");
        sb.append("        java.util.Iterator it = headers.keySet().iterator();\n");
        sb.append("        while(it.hasNext()) {\n");
        sb.append("            Object k = it.next();\n");
        sb.append("            if(k != null && String.valueOf(k).equalsIgnoreCase(name)) {\n");
        sb.append("                Object v = headers.get(k);\n");
        sb.append("                return v == null ? null : String.valueOf(v);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        sb.append("    /** Cookies are not a header of their own; they are pairs inside Cookie. */\n");
        sb.append("    private static String cookie(java.util.Map headers, String name) {\n");
        sb.append("        String raw = header(headers, \"Cookie\");\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        String[] pairs = splitOn(raw, ';');\n");
        sb.append("        for(int i = 0 ; i < pairs.length ; i++) {\n");
        sb.append("            String pair = pairs[i].trim();\n");
        sb.append("            int eq = pair.indexOf('=');\n");
        sb.append("            if(eq > 0 && pair.substring(0, eq).trim().equals(name)) return decodeCookie(pair.substring(eq + 1));\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        sb.append("    private static String[] splitOn(String value, char sep) {\n");
        sb.append("        java.util.List parts = new java.util.ArrayList();\n");
        sb.append("        int pos = 0;\n");
        sb.append("        while(true) {\n");
        sb.append("            int next = value.indexOf(sep, pos);\n");
        sb.append("            if(next < 0) { parts.add(value.substring(pos)); break; }\n");
        sb.append("            parts.add(value.substring(pos, next));\n");
        sb.append("            pos = next + 1;\n");
        sb.append("        }\n");
        sb.append("        String[] out = new String[parts.size()];\n");
        sb.append("        for(int i = 0 ; i < out.length ; i++) out[i] = (String)parts.get(i);\n");
        sb.append("        return out;\n");
        sb.append("    }\n\n");
        sb.append("    /** Percent-decoding, plus '+' as space in query values. */\n");
        // '+' means a space only in application/x-www-form-urlencoded, which is what a
        // query string and a cookie are. In a PATH segment it is an ordinary character,
        // so /items/a+b names "a+b" and decoding it to "a b" hands the handler an id the
        // client never sent.
        sb.append("    /** A path segment. '+' is literal here, per RFC 3986. */\n");
        sb.append("    private static String decodePath(String value) { return decode(value, false); }\n\n");
        sb.append("    /** A query value, which is form-encoded: '+' is a space. */\n");
        sb.append("    private static String decodeQuery(String value) { return decode(value, true); }\n\n");
        sb.append("    /**\n");
        sb.append("     * A cookie value. Cookie syntax has no plus-to-space rule, and '+' is\n");
        sb.append("     * ordinary in the base64 that session tokens are made of, so folding it\n");
        sb.append("     * to a space corrupts the token and the session with it.\n");
        sb.append("     */\n");
        sb.append("    private static String decodeCookie(String value) { return decode(value, false); }\n\n");
        sb.append("    private static String decode(String value, boolean plusIsSpace) {\n");
        sb.append("        if(value == null) return null;\n");
        sb.append("        if(value.indexOf('%') < 0 && !(plusIsSpace && value.indexOf('+') >= 0)) return value;\n");
        // A run of escapes is one UTF-8 sequence, not one character each. Appending
        // %C3%A9 as two chars produced "\u00c3\u00a9" where the client sent one
        // accented letter, so consecutive escapes are gathered as bytes and decoded
        // together.
        sb.append("        StringBuilder out = new StringBuilder();\n");
        sb.append("        byte[] pending = new byte[value.length()];\n");
        sb.append("        int pendingLen = 0;\n");
        sb.append("        for(int i = 0 ; i < value.length() ; i++) {\n");
        sb.append("            char c = value.charAt(i);\n");
        sb.append("            if(c == '%' && i + 2 < value.length()) {\n");
        sb.append("                try {\n");
        sb.append("                    pending[pendingLen++] = (byte)Integer.parseInt(value.substring(i + 1, i + 3), 16);\n");
        sb.append("                    i += 2;\n");
        sb.append("                    continue;\n");
        sb.append("                } catch (NumberFormatException err) { }\n");
        sb.append("            }\n");
        sb.append("            if(pendingLen > 0) {\n");
        sb.append("                out.append(decodeUtf8(pending, pendingLen));\n");
        sb.append("                pendingLen = 0;\n");
        sb.append("            }\n");
        sb.append("            if(plusIsSpace && c == '+') { out.append(' '); continue; }\n");
        sb.append("            out.append(c);\n");
        sb.append("        }\n");
        sb.append("        if(pendingLen > 0) out.append(decodeUtf8(pending, pendingLen));\n");
        sb.append("        return out.toString();\n");
        sb.append("    }\n\n");
        sb.append("    /** The gathered escape bytes as text. Malformed input keeps its bytes rather than throwing. */\n");
        sb.append("    private static String decodeUtf8(byte[] bytes, int length) {\n");
        sb.append("        try {\n");
        sb.append("            return new String(bytes, 0, length, \"UTF-8\");\n");
        sb.append("        } catch (java.io.UnsupportedEncodingException err) {\n");
        sb.append("            return new String(bytes, 0, length);\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    // A missing text value binds to 0 / null rather than throwing: an absent\n");
        sb.append("    // optional query parameter is not a server error.\n");
        sb.append("    private static int parseInt(String v) { return v == null || v.length() == 0 ? 0 : Integer.parseInt(v.trim()); }\n");
        sb.append("    private static long parseLong(String v) { return v == null || v.length() == 0 ? 0L : Long.parseLong(v.trim()); }\n");
        // Double.parseDouble does not FAIL on a value too large for a double:
        // 1e999 comes back as infinity, so the handler ran on a number the client
        // never sent, and echoing it through Json writes null -- the client gets
        // back neither its value nor an error. The SPELLING decides whether an
        // infinity was meant, because the parsed value cannot tell 1e999 from
        // Infinity. Same test the controller processor's guards use.
        sb.append("    private static double parseDouble(String v) {\n");
        sb.append("        if (v == null || v.length() == 0) { return 0d; }\n");
        sb.append("        String t = v.trim();\n");
        sb.append("        double d = Double.parseDouble(t);\n");
        sb.append("        if (Double.isInfinite(d) && t.indexOf(\"Infinity\") < 0) {\n");
        sb.append("            throw new NumberFormatException(\"out of range for double: \" + v);\n");
        sb.append("        }\n");
        sb.append("        return d;\n");
        sb.append("    }\n");
        sb.append("    private static short parseShort(String v) { return v == null || v.length() == 0 ? (short)0 : Short.parseShort(v.trim()); }\n");
        sb.append("    private static byte parseByte(String v) { return v == null || v.length() == 0 ? (byte)0 : Byte.parseByte(v.trim()); }\n");
        // A double outside float range becomes INFINITY on the cast rather than
        // failing, so 1e100 reached the handler as an infinite amount. Rejected
        // the same way an unparseable number is, which the dispatcher already
        // answers 400 for.
        sb.append("    private static float parseFloat(String v) {\n");
        sb.append("        if (v == null || v.length() == 0) { return 0f; }\n");
        sb.append("        String t = v.trim();\n");
        sb.append("        double d = Double.parseDouble(t);\n");
        sb.append("        float f = (float)d;\n");
        // Was !Double.isInfinite(d), which is the wrong question: parseDouble
        // ("1e999") is ITSELF infinite, so every double-overflowing value read as
        // a deliberate infinity and went through. The text is what says it was
        // meant.
        sb.append("        if (Float.isInfinite(f) && t.indexOf(\"Infinity\") < 0) {\n");
        sb.append("            throw new NumberFormatException(\"out of range for float: \" + v);\n");
        sb.append("        }\n");
        sb.append("        return f;\n");
        sb.append("    }\n");
        sb.append("    private static Integer boxInt(String v) { return v == null || v.length() == 0 ? null : Integer.valueOf(v.trim()); }\n");
        sb.append("    private static Long boxLong(String v) { return v == null || v.length() == 0 ? null : Long.valueOf(v.trim()); }\n");
        // Through the guarded parsers, not valueOf: a boxed binding is the same
        // binding with a null for "absent", and it had no overflow check at all.
        sb.append("    private static Double boxDouble(String v) { return v == null || v.length() == 0 ? null : Double.valueOf(parseDouble(v)); }\n");
        sb.append("    private static Float boxFloat(String v) { return v == null || v.length() == 0 ? null : Float.valueOf(parseFloat(v)); }\n");
        sb.append("    private static Short boxShort(String v) { return v == null || v.length() == 0 ? null : Short.valueOf(v.trim()); }\n");
        sb.append("    private static Byte boxByte(String v) { return v == null || v.length() == 0 ? null : Byte.valueOf(v.trim()); }\n");
        // NOT Boolean.parseBoolean, which answers false for everything that is not
        // "true": "?enabled=treu" reached the handler as an explicit false and the
        // client was told nothing, while the same typo in a numeric binding throws
        // and comes back as a 400. A present value is either boolean or it is a
        // mistake worth reporting.
        sb.append("    private static boolean parseBool(String v) {\n");
        sb.append("        if (v == null || v.length() == 0) { return false; }\n");
        sb.append("        String t = v.trim();\n");
        sb.append("        if (t.equalsIgnoreCase(\"true\")) { return true; }\n");
        sb.append("        if (t.equalsIgnoreCase(\"false\")) { return false; }\n");
        sb.append("        throw new IllegalArgumentException(\"not a boolean: \" + v);\n");
        sb.append("    }\n");
        sb.append("    private static Boolean boxBoolean(String v) {\n");
        sb.append("        return v == null || v.length() == 0 ? null : Boolean.valueOf(parseBool(v));\n");
        sb.append("    }\n");
    }

    // ----------------------------------------------------------------
    // DTO codecs
    // ----------------------------------------------------------------

    /// Emits a Map<->DTO codec from the type's public instance fields. Field-based
    /// rather than reflective on purpose: ParparVM has no usable reflection and
    /// Codename One obfuscates, so a name lookup at runtime would fail in exactly
    /// the builds that matter.
    private String generateDtoCodec(String binaryName, AnnotatedClass cls,
            ProcessorContext ctx) {
        String pkg = RestClientAnnotationProcessor.packageOf(binaryName);
        String simple = RestClientAnnotationProcessor.simpleName(binaryName);
        StringBuilder sb = new StringBuilder(4096);
        if (pkg.length() > 0) sb.append("package ").append(pkg).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations for ").append(binaryName).append(". Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(simple).append("Json {\n");
        sb.append("    private ").append(simple).append("Json() { }\n\n");

        sb.append("    public static java.util.Map toMap(").append(binaryName).append(" o) {\n");
        sb.append("        if(o == null) return null;\n");
        sb.append("        java.util.Map m = new java.util.LinkedHashMap();\n");
        for (FieldInfo f : transferredFields(cls, ctx)) {
            String type = fieldJavaType(f);
            sb.append("        m.put(\"").append(RestClientAnnotationProcessor.escape(f.getName()))
              .append("\", ").append(fieldToJson(type, "o." + f.getName())).append(");\n");
        }
        sb.append("        return m;\n");
        sb.append("    }\n\n");

        sb.append("    public static ").append(binaryName).append(" fromMap(java.util.Map m) {\n");
        sb.append("        if(m == null) return null;\n");
        sb.append("        ").append(binaryName).append(" o = new ").append(binaryName).append("();\n");
        for (FieldInfo f : transferredFields(cls, ctx)) {
            if ((f.getAccess() & org.objectweb.asm.Opcodes.ACC_SYNTHETIC) != 0) continue;
            if (f.isFinal()) continue; // cannot be assigned after construction
            String type = fieldJavaType(f);
            sb.append("        o.").append(f.getName()).append(" = ")
              .append(fieldFromJson(type, "m.get(\"" + RestClientAnnotationProcessor.escape(f.getName()) + "\")"))
              .append(";\n");
        }
        sb.append("        return o;\n");
        sb.append("    }\n\n");
        emitCodecHelpers(sb);
        sb.append("}\n");
        return sb.toString();
    }

    /// NOTE: a direct-to-bytes writer was tried here and REVERTED. Emitting
    /// `toJson(T, com.codename1.backend.ByteSink)` beside `toMap` is worth a
    /// measured +29% on a JSON route (74% -> 94% of Go net/http), because it drops
    /// the per-request LinkedHashMap, the key hashing and the instanceof dispatch
    /// that walking a map costs.
    ///
    /// It cannot go here as things stand: generated sources are compiled against
    /// `ctx.getOutputClassDir()` and NOTHING ELSE (see the compile call above), so
    /// they cannot name a backend type. It happened to work for a backend app,
    /// whose own build puts com.codename1.backend in that same directory, and
    /// failed for every other project -- including this processor's own tests.
    ///
    /// To take the 29%, the generated code first needs a type it is allowed to
    /// name: either the project's compile classpath reaches the codec compile, or
    /// the sink interface lives somewhere generated code may always depend on.
    /// That is a deliberate decision about this processor's dependency contract,
    /// not a detail to slip in behind a performance patch.
    private static String fieldToJson(String type, String expr) {
        if (isCollectionShape(type)) {
            String element = type.substring(type.indexOf('<') + 1, type.length() - 1);
            if (element.startsWith("java.")) return "toValueList(" + expr + ")";
            // A nested DTO list has to become a list of MAPS; handing the writer
            // the DTOs themselves serialises them as toString().
            return "toMapList(" + expr + ", new ToMapFn() {\n"
                    + "            public java.util.Map convert(Object o) { return "
                    + codecFor(element) + ".toMap((" + element + ")o); }\n"
                    + "        })";
        }
        if (type.startsWith("java.") || type.indexOf('.') < 0) return expr;
        return codecFor(type) + ".toMap(" + expr + ")";
    }

    private static String fieldFromJson(String type, String expr) {
        if (isCollectionShape(type)) {
            String element = type.substring(type.indexOf('<') + 1, type.length() - 1);
            // Both branches below produce a List, so a Set-typed field has to be
            // converted rather than cast -- the same fix the request-body path
            // needed. Review named the java.* branch; the DTO branch had it too,
            // which is why this converts in one place at the end instead.
            boolean isSet = type.startsWith("java.util.Set<");
            if (element.startsWith("java.")) {
                // Each element converted, exactly as the DTO branch below does. The
                // parser produces Long for every integer, so a List<Integer> FIELD
                // arrived full of Longs -- the same defect the collection body had,
                // one level further in, and the same silent misread on a target whose
                // CHECKCAST does not check.
                String decoded = "fromValueList(" + expr + ", new FromValueFn() {\n"
                        + "            public Object convert(Object v) { return "
                        + fieldFromJson(element, "v") + "; }\n"
                        + "        })";
                if (isSet) {
                    decoded = "setFromList(" + decoded + ")";
                }
                return "(" + type + ")(Object)" + decoded;
            }
            // Each element is converted through the element codec. Returning the
            // decoded Maps as-is -- which this used to do -- gives the handler a
            // List whose elements are Maps typed as DTOs: a lie the JVM catches at
            // the first field read and ParparVM does not catch at all.
            String decodedDtos = "fromMapList(" + expr + ", new FromMapFn() {\n"
                    + "            public Object convert(java.util.Map m) { return "
                    + codecFor(element) + ".fromMap(m); }\n"
                    + "        })";
            if (isSet) {
                decodedDtos = "setFromList(" + decodedDtos + ")";
            }
            return "(" + type + ")(Object)" + decodedDtos;
        }
        if ("java.lang.String".equals(type)) return "asString(" + expr + ")";
        if ("int".equals(type))     return "asInt(" + expr + ")";
        if ("long".equals(type))    return "asLong(" + expr + ")";
        if ("double".equals(type))  return "asDouble(" + expr + ")";
        if ("float".equals(type))   return "asFloat(" + expr + ")";
        if ("short".equals(type))   return "asShort(" + expr + ")";
        if ("byte".equals(type))    return "asByte(" + expr + ")";
        if ("boolean".equals(type)) return "asBoolean(" + expr + ")";
        if ("java.lang.Integer".equals(type)) return "asBoxedInt(" + expr + ")";
        if ("java.lang.Long".equals(type))    return "asBoxedLong(" + expr + ")";
        if ("java.lang.Double".equals(type))  return "asBoxedDouble(" + expr + ")";
        if ("java.lang.Boolean".equals(type)) return "asBoxedBoolean(" + expr + ")";
        // Float, Short and Byte need the same treatment as the three above. The JSON
        // reader only ever produces Long or Double, so leaving them to guardedCast
        // meant an instanceof against the declared wrapper that never matched, and
        // the field silently arrived null with the client's value discarded.
        if ("java.lang.Float".equals(type))   return "asBoxedFloat(" + expr + ")";
        if ("java.lang.Short".equals(type))   return "asBoxedShort(" + expr + ")";
        if ("java.lang.Byte".equals(type))    return "asBoxedByte(" + expr + ")";
        // Anything else out of java.* is narrowed with instanceof rather than cast:
        // the value came from the wire, so its type is the client's choice.
        if (type.startsWith("java.")) return guardedCast(type, expr);
        // requireMap, not asMap: asMap answers null for anything that is not one,
        // so {"child":1} left the field null and the handler ran on input the
        // client did not send -- indistinguishable from an explicit JSON null.
        return codecFor(type) + ".fromMap(requireMap(" + expr + "))";
    }

    /**
     * The value coercions both generated classes need.
     *
     * Emitted into the dispatcher as well as the codec because the dispatcher
     * converts collection elements too: a List<Integer> body reaches it as a list of
     * Longs, and the conversion that fixes that is written in terms of these. They
     * were in the codec alone, so the dispatcher referred to helpers it did not have
     * and simply failed to compile.
     */
    private static void emitValueCoercion(StringBuilder sb) {
        sb.append("    // The JSON reader produces Long for integers and Double for reals, so every\n");
        sb.append("    // numeric read goes through Number rather than casting to the field's type.\n");
        // String.valueOf turns ANYTHING into a string, so a number arrived as
        // "1" and a whole object as "{x=1}" -- values the declared JSON shape
        // never allowed, handed to the handler as though the client had sent
        // them. A JSON string is a string; anything else is the client being
        // wrong, and null is still null.
        sb.append("    private static String asString(Object v) {\n");
        sb.append("        if (v == null || v instanceof String) { return (String)v; }\n");
        sb.append("        throw new IllegalArgumentException(\"a JSON string is required, not \""
                + " + v.getClass().getName());\n");
        sb.append("    }\n");
        // Range-checked, not narrowed. The parser answers a Long for any JSON
        // integer, and intValue() on 2147483648 is -2147483648 -- so an id, a count
        // or an amount reached the handler as a DIFFERENT number from the one the
        // client sent, with nothing raised. A value that does not fit is the
        // client's mistake and is reported as one.
        // Whole numbers only. The parser answers a Double for any JSON real, and
        // longValue() on 1.9 is 1 -- so a fractional id, count or amount reached
        // the handler as a DIFFERENT number from the one the client sent, and the
        // range check below never sees it because 1 is perfectly in range. A value
        // that is not integral is the client's mistake and is reported as one.
        sb.append("    private static long integral(Object v, String type) {\n");
        sb.append("        double d = ((Number)v).doubleValue();\n");
        sb.append("        if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.floor(d)) {\n");
        sb.append("            throw new IllegalArgumentException(\"not a whole number for \" + type + \": \" + v);\n");
        sb.append("        }\n");
        // Range too, and BEFORE the narrowing rather than after it. longValue()
        // SATURATES: 1e20 comes back as Long.MAX_VALUE instead of throwing, so an
        // id or an amount too large to represent arrived as a plausible number
        // that is not the one the client sent. Only a Double can be out of range
        // here -- a Long already is one -- so the bound is tested on the double.
        sb.append("        if (!(v instanceof Long) && (d < -9.223372036854776E18 || d >= 9.223372036854776E18)) {\n");
        sb.append("            throw new IllegalArgumentException(\"out of range for \" + type + \": \" + v);\n");
        sb.append("        }\n");
        sb.append("        return ((Number)v).longValue();\n");
        sb.append("    }\n");
        sb.append("    private static int asInt(Object v) {\n");
        sb.append("        if (v instanceof Number) {\n");
        sb.append("            long asLong = integral(v, \"int\");\n");
        sb.append("            if (asLong < Integer.MIN_VALUE || asLong > Integer.MAX_VALUE) {\n");
        sb.append("                throw new IllegalArgumentException(\"out of range for int: \" + v);\n");
        sb.append("            }\n");
        sb.append("            return (int)asLong;\n");
        sb.append("        }\n");
        // A JSON STRING is not a number. These helpers decode a value the parser
        // has already typed, so "123" where the contract declares an int is the
        // client disagreeing with the contract -- and parsing it anyway means the
        // handler cannot tell the two apart, while the equivalent request to the
        // generated CLIENT could never have produced it. The text bindings are a
        // different path on purpose: a query parameter really does arrive as text,
        // and fromText/parseInt still parse it.
        sb.append("        if (v != null) {\n");
        sb.append("            throw new IllegalArgumentException(\"a JSON number is required, not \"\n");
        sb.append("                    + v.getClass().getName() + \": \" + v);\n");
        sb.append("        }\n");
        sb.append("        return 0;\n");
        sb.append("    }\n");
        sb.append("    private static short asShort(Object v) {\n");
        sb.append("        int narrowed = asInt(v);\n");
        sb.append("        if (narrowed < Short.MIN_VALUE || narrowed > Short.MAX_VALUE) {\n");
        sb.append("            throw new IllegalArgumentException(\"out of range for short: \" + v);\n");
        sb.append("        }\n");
        sb.append("        return (short)narrowed;\n");
        sb.append("    }\n");
        sb.append("    private static byte asByte(Object v) {\n");
        sb.append("        int narrowed = asInt(v);\n");
        sb.append("        if (narrowed < Byte.MIN_VALUE || narrowed > Byte.MAX_VALUE) {\n");
        sb.append("            throw new IllegalArgumentException(\"out of range for byte: \" + v);\n");
        sb.append("        }\n");
        sb.append("        return (byte)narrowed;\n");
        sb.append("    }\n");
        // Same rule as asInt above, and for the same reason.
        sb.append("    private static long asLong(Object v) {\n");
        sb.append("        if (v instanceof Number) { return integral(v, \"long\"); }\n");
        sb.append("        if (v != null) {\n");
        sb.append("            throw new IllegalArgumentException(\"a JSON number is required, not \"\n");
        sb.append("                    + v.getClass().getName() + \": \" + v);\n");
        sb.append("        }\n");
        sb.append("        return 0L;\n");
        sb.append("    }\n");
        sb.append("    private static double asDouble(Object v) {\n");
        sb.append("        if (v instanceof Number) { return ((Number)v).doubleValue(); }\n");
        sb.append("        if (v != null) {\n");
        sb.append("            throw new IllegalArgumentException(\"a JSON number is required, not \"\n");
        sb.append("                    + v.getClass().getName() + \": \" + v);\n");
        sb.append("        }\n");
        sb.append("        return 0d;\n");
        sb.append("    }\n");
        // A cast to float SATURATES: a perfectly ordinary finite 1e100 becomes
        // infinity, which is not a number JSON can express and is not the one the
        // client sent. The scalar text path refuses it; a DTO field has to as
        // well, or the same value is accepted or rejected by where it appears.
        sb.append("    private static float asFloat(Object v) {\n");
        sb.append("        double d = asDouble(v);\n");
        sb.append("        float f = (float)d;\n");
        sb.append("        if (Float.isInfinite(f) && !Double.isInfinite(d)) {\n");
        sb.append("            throw new IllegalArgumentException(\"out of range for float: \" + v);\n");
        sb.append("        }\n");
        sb.append("        return f;\n");
        sb.append("    }\n");
        // Boolean.parseBoolean answers FALSE for everything that is not "true",
        // so {"good":1} and {"good":"invalid"} both reached the handler as an
        // explicit false the client never sent. This is a JSON body, where the
        // value has a real type -- unlike the text bindings, where several
        // spellings are a deliberate convention -- so anything that is not a
        // boolean is the client being wrong and is answered 400.
        sb.append("    private static boolean asBoolean(Object v) {\n");
        sb.append("        if (v instanceof Boolean) { return ((Boolean)v).booleanValue(); }\n");
        sb.append("        if (v == null) { return false; }\n");
        sb.append("        throw new IllegalArgumentException(\"not a boolean: \" + v);\n");
        sb.append("    }\n");
        sb.append("    private static Integer asBoxedInt(Object v) { return v == null ? null : Integer.valueOf(asInt(v)); }\n");
        sb.append("    private static Long asBoxedLong(Object v) { return v == null ? null : Long.valueOf(asLong(v)); }\n");
        sb.append("    private static Double asBoxedDouble(Object v) { return v == null ? null : Double.valueOf(asDouble(v)); }\n");
        sb.append("    private static Boolean asBoxedBoolean(Object v) { return v == null ? null : Boolean.valueOf(asBoolean(v)); }\n");
        sb.append("    private static Float asBoxedFloat(Object v) { return v == null ? null : Float.valueOf(asFloat(v)); }\n");
        sb.append("    private static Short asBoxedShort(Object v) { return v == null ? null : Short.valueOf(asShort(v)); }\n");
        sb.append("    private static Byte asBoxedByte(Object v) { return v == null ? null : Byte.valueOf(asByte(v)); }\n");
        sb.append("    /** A decoded value narrowed to a JSON object, or null -- never a cast. */\n");
        sb.append("    private static java.util.Map asMap(Object v) { return v instanceof java.util.Map ? (java.util.Map)v : null; }\n");
        // The difference between "the client sent null" and "the client sent
        // something that is not an object". The first is a value; the second is
        // a mistake, and answering 400 for it is the whole point of decoding.
        sb.append("    private static java.util.Map requireMap(Object v) {\n");
        sb.append("        if (v == null) { return null; }\n");
        sb.append("        if (!(v instanceof java.util.Map)) {\n");
        sb.append("            throw new IllegalArgumentException(\"a JSON object is required, not \""
                + " + v.getClass().getName());\n");
        sb.append("        }\n");
        sb.append("        return (java.util.Map)v;\n");
        sb.append("    }\n");
        sb.append("    private static java.util.List asList(Object v) { return v instanceof java.util.List ? (java.util.List)v : null; }\n");
        sb.append("    /** A decoded array as a Set, preserving the order it arrived in. */\n");
        sb.append("    private static java.util.Set setFromList(java.util.List v) {\n");
        sb.append("        return v == null ? null : new java.util.LinkedHashSet(v);\n");
        sb.append("    }\n");
    }

    private static void emitCodecHelpers(StringBuilder sb) {
        emitValueCoercion(sb);
        sb.append("    private interface ToMapFn { java.util.Map convert(Object o); }\n");
        sb.append("    private interface FromMapFn { Object convert(java.util.Map m); }\n");
        sb.append("    private interface FromValueFn { Object convert(Object v); }\n");
        sb.append("    /** Converts each element of a decoded array to the field's element type. */\n");
        sb.append("    private static java.util.List fromValueList(Object raw, FromValueFn f) {\n");
        // asList answers null for anything that is not one, so an object or a
        // scalar where an array was declared left the field null -- the client's
        // mistake made indistinguishable from an explicit JSON null.
        sb.append("        if(raw == null) return null;\n");
        sb.append("        if(!(raw instanceof java.util.List)) {\n");
        sb.append("            throw new IllegalArgumentException(\"a JSON array is required, not \""
                + " + raw.getClass().getName());\n");
        sb.append("        }\n");
        sb.append("        java.util.List in = (java.util.List)raw;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        for(int i = 0 ; i < in.size() ; i++) {\n");
        sb.append("            out.add(f.convert(in.get(i)));\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n");
        sb.append("    private static java.util.List toValueList(java.util.Collection raw) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        java.util.Iterator it = raw.iterator();\n");
        sb.append("        while(it.hasNext()) out.add(it.next());\n");
        sb.append("        return out;\n");
        sb.append("    }\n");
        sb.append("    private static java.util.List toMapList(java.util.Collection raw, ToMapFn f) {\n");
        sb.append("        if(raw == null) return null;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        java.util.Iterator it = raw.iterator();\n");
        sb.append("        while(it.hasNext()) { Object e = it.next(); out.add(e == null ? null : f.convert(e)); }\n");
        sb.append("        return out;\n");
        sb.append("    }\n");
        sb.append("    private static java.util.List fromMapList(Object raw, FromMapFn f) {\n");
        sb.append("        if(!(raw instanceof java.util.List)) return null;\n");
        sb.append("        java.util.List src = (java.util.List)raw;\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        sb.append("        for(int i = 0 ; i < src.size() ; i++) {\n");
        sb.append("            Object e = src.get(i);\n");
        // The same rule listFromMaps takes: a non-null element that is not an
        // object is the client being wrong, and substituting null for it hands
        // the handler a collection with a hole where a DTO should be.
        sb.append("            if(e != null && !(e instanceof java.util.Map)) {\n");
        sb.append("                throw new IllegalArgumentException(\"element \" + i"
                + " + \" is \" + e.getClass().getName() + \", not an object\");\n");
        sb.append("            }\n");
        sb.append("            out.add(e == null ? null : f.convert((java.util.Map)e));\n");
        sb.append("        }\n");
        sb.append("        return out;\n");
        sb.append("    }\n");
    }

    private static String[] splitTemplate(String template) {
        String t = template == null ? "" : template;
        // ORIGIN-FORM first. The runtime splits the incoming path with the same
        // algorithm, so "/notes" arrives as ["", "notes"] while a contract written
        // as @GET("notes") -- which the client resolves against a base URL ending
        // in "/" and requests as /notes -- split to ["notes"] and could never match
        // on length. The empty template had the same problem against "/".
        if (t.length() == 0 || t.charAt(0) != '/') {
            t = "/" + t;
        }
        List<String> parts = new ArrayList<String>();
        int pos = 0;
        while (pos <= t.length()) {
            int next = t.indexOf('/', pos);
            if (next < 0) { parts.add(t.substring(pos)); break; }
            parts.add(t.substring(pos, next));
            pos = next + 1;
        }
        return parts.toArray(new String[parts.size()]);
    }

    /**
     * Whether this segment carries a placeholder at all -- alone or with literal
     * text around it.
     *
     * The CLIENT generator has always substituted {name} anywhere in the template,
     * so /files/{name}.json produced a working client while this half saw no
     * placeholder, reported that the @Path was unbound, and refused the contract.
     * One annotation cannot mean two things in the two halves generated from it.
     */
    private static boolean isPlaceholder(String segment) {
        int open = segment.indexOf('{');
        return open >= 0 && segment.indexOf('}', open + 1) > open + 1;
    }

    /** The name inside this segment's placeholder, or null when it has none. */
    private static String placeholderName(String segment) {
        int open = segment.indexOf('{');
        if (open < 0) {
            return null;
        }
        int close = segment.indexOf('}', open + 1);
        return close > open + 1 ? segment.substring(open + 1, close) : null;
    }

    /** The literal text before this segment's placeholder. */
    private static String placeholderPrefix(String segment) {
        int open = segment.indexOf('{');
        return open < 0 ? "" : segment.substring(0, open);
    }

    /** The literal text after it. */
    private static String placeholderSuffix(String segment) {
        int open = segment.indexOf('{');
        if (open < 0) {
            return "";
        }
        int close = segment.indexOf('}', open + 1);
        return close < 0 ? "" : segment.substring(close + 1);
    }

    /**
     * Whether this segment holds more than one placeholder.
     *
     * Refused rather than matched: "{a}-{b}" has no single reading -- the split
     * point between the two values is a guess -- and guessing it here would make
     * the server bind something the client never meant. Named explicitly so the
     * developer is told, instead of the shape silently not matching.
     */
    private static boolean hasSecondPlaceholder(String segment) {
        int open = segment.indexOf('{');
        if (open < 0) {
            return false;
        }
        int close = segment.indexOf('}', open + 1);
        return close >= 0 && segment.indexOf('{', close + 1) >= 0;
    }

    private static int placeholderIndex(String[] template, String name) {
        for (int i = 0; i < template.length; i++) {
            if (name.equals(placeholderName(template[i]))) {
                return i;
            }
        }
        return -1;
    }
}

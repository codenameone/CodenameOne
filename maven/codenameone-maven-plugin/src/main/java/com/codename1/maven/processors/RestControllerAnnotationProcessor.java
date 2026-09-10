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
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.maven.annotations.ClassScanner;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.Type;

/**
 * Generates a router for every `@RestController`, and the `main` that serves them.
 *
 * The shape is Spring's on purpose -- `@RestController`, `@GetMapping`,
 * `@PathVariable`, `@RequestParam`, `@RequestBody`, `@ResponseStatus` mean here what
 * they mean there -- so that reading one is enough to read the other. What differs is
 * where the work happens: Spring resolves a route by walking a registry at request
 * time, and this resolves it at build time into code that compares the request's own
 * bytes.
 *
 * That is the reason to generate rather than reflect. A handwritten
 * `if ("/healthz".equals(request.getTarget()))` builds a String for the target,
 * hashes it and compares it, on every request and for every route it tests before the
 * one that matches; and it is wrong the moment the client appends a query string. The
 * generated form holds each route as a `byte[]` constant and asks the Request whether
 * its path bytes are those bytes -- no String, no hash, and the query cannot break it.
 * A route with no path variables therefore allocates nothing at all, which is what
 * keeps the collector out of the request path.
 *
 * Reflection is not an option regardless: this code is translated to C, and
 * `Class.forName` on an obfuscated name does not survive that. Generating source that
 * the same compiler sees is what makes the wiring visible to the dead-code pass.
 */
public final class RestControllerAnnotationProcessor extends AbstractAnnotationProcessor {

    private static final String PKG = "Lcom/codename1/backend/annotations/";
    private static final String CONTROLLER = PKG + "RestController;";
    private static final String REQUEST_MAPPING = PKG + "RequestMapping;";
    private static final String PATH_VARIABLE = PKG + "PathVariable;";
    private static final String REQUEST_PARAM = PKG + "RequestParam;";
    private static final String REQUEST_HEADER = PKG + "RequestHeader;";
    private static final String REQUEST_BODY = PKG + "RequestBody;";
    private static final String RESPONSE_STATUS = PKG + "ResponseStatus;";

    /** Mapping annotation to the HTTP method it stands for. */
    private static final Map<String, String> MAPPINGS;
    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put(PKG + "GetMapping;", "GET");
        m.put(PKG + "PostMapping;", "POST");
        m.put(PKG + "PutMapping;", "PUT");
        m.put(PKG + "DeleteMapping;", "DELETE");
        m.put(PKG + "PatchMapping;", "PATCH");
        MAPPINGS = Collections.unmodifiableMap(m);
    }

    private static final String REQUEST_TYPE = "com.codename1.backend.HttpServer.Request";
    private static final String RESPONSE_TYPE = "com.codename1.backend.HttpServer.Response";

    /**
     * The same class as the DESCRIPTOR spells it. A nested class is
     * Outer$Inner in bytecode, and the type derived from the descriptor keeps
     * that -- so comparing against the dotted source spelling alone never
     * matched, and both places that ask "is this a Response" were dead code:
     * emitRoute never took the branch that SENDS one, and the encodable check
     * never exempted it. Returning a Response from a controller, which the
     * refusal message itself offers as the way to take control of the reply,
     * did not work.
     */
    private static final String RESPONSE_TYPE_BINARY =
            "com.codename1.backend.HttpServer$Response";

    /** Either spelling of HttpServer.Response. */
    private static boolean isResponseType(String javaType) {
        return RESPONSE_TYPE.equals(javaType) || RESPONSE_TYPE_BINARY.equals(javaType);
    }

    /**
     * The verbs HttpServer routes. It compares them with equals and answers 501
     * to everything else before dispatch, so this list is the whole truth about
     * what a generated route can be reached by. Kept in the same order the
     * server declares it.
     */
    /**
     * The JDK types Json.writeValue has a branch for, and therefore the only ones
     * a handler may return without writing itself. Kept in the order that method
     * tests them so the two can be read side by side: String; the integral boxes;
     * the floating ones; Map; List; byte[] (handled as an array before this);
     * Collection, of which Set is the shape people actually return.
     */
    private static final Set<String> JSON_JDK_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "java.lang.String", "java.lang.Character",
                    "java.lang.Boolean", "java.lang.Integer", "java.lang.Long",
                    "java.lang.Short", "java.lang.Byte",
                    "java.lang.Double", "java.lang.Float",
                    "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap",
                    "java.util.TreeMap", "java.util.SortedMap",
                    "java.util.List", "java.util.ArrayList", "java.util.LinkedList",
                    "java.util.Collection", "java.util.Set", "java.util.HashSet",
                    "java.util.LinkedHashSet", "java.util.TreeSet", "java.util.SortedSet")));

    private static final List<String> ROUTABLE_METHODS = Collections.unmodifiableList(
            Arrays.asList("GET", "POST", "HEAD", "PUT", "DELETE", "PATCH", "OPTIONS"));

    /** Where the generated bootstrap's name is left for the packaging goal to read. */
    public static final String MAIN_CLASS_RESOURCE = "META-INF/cn1-backend-main";

    private final TreeMap<String, Controller> controllers = new TreeMap<String, Controller>();

    /**
     * Every route shape seen so far, across every controller, to the method that
     * claimed it. Kept beside `controllers` rather than inside one, because the
     * generated bootstrap chains the routers and returns the first non-null
     * response: two controllers colliding makes the later one unreachable in
     * exactly the way two methods in one controller do.
     */
    private final Map<String, String> routeShapes = new LinkedHashMap<String, String>();

    /** Which controller claimed each shape, so a clash names the other one. */
    private final Map<String, String> routeOwners = new LinkedHashMap<String, String>();

    private static final class Controller {
        String binaryName;
        String packageName;
        String simpleName;
        String routerSimpleName;
        List<String> basePaths = new ArrayList<String>();
        List<Route> routes = new ArrayList<Route>();
    }

    private static final class Route {
        String httpMethod;
        String pattern;
        String javaMethod;
        String returnJavaType;
        int status;
        List<Param> params = new ArrayList<Param>();
        /** The literal bytes before the first `{`; the whole pattern when there is none. */
        String prefix;
        /** For each variable, the literal that must follow it; "" when it runs to the end. */
        List<String> after = new ArrayList<String>();
    }

    private static final class Param {
        String kind;      // PATH, QUERY, HEADER, BODY, REQUEST
        String name;
        String javaType;
        /** The same type with its arguments, when the method carried a signature. */
        String genericJavaType;
        String defaultValue;
        /** From the annotation. A request missing a required binding is refused. */
        boolean required;
        /** Set when the body is decoded into a local before the call. */
        String local;
        int variableIndex = -1;
    }

    @Override
    public Set<String> getAnnotationDescriptors() {
        return Collections.singleton(CONTROLLER);
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.getClassAnnotation(CONTROLLER) == null) {
            return;
        }
        if (cls.isInterface() || cls.isAbstract()) {
            ctx.error(cls, "@RestController must be a concrete class: " + cls.getBinaryName());
            return;
        }
        Controller controller = new Controller();
        controller.binaryName = cls.getBinaryName();
        controller.packageName = RestClientAnnotationProcessor.packageOf(controller.binaryName);
        controller.simpleName = RestClientAnnotationProcessor.simpleName(controller.binaryName);
        controller.routerSimpleName = controller.simpleName + "Router";
        controller.basePaths.addAll(pathsOf(cls.getClassAnnotation(REQUEST_MAPPING)));
        if (controller.basePaths.isEmpty()) {
            controller.basePaths.add("");
        }
        if (!hasNoArgConstructor(cls)) {
            ctx.error(cls, "@RestController needs a public no-argument constructor so the "
                    + "generated bootstrap can create it: " + controller.binaryName);
            return;
        }

        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() || m.isSynthetic() || m.isStatic() || !m.isPublic()) {
                continue;
            }
            String httpMethod = null;
            List<String> paths = null;
            for (Map.Entry<String, String> e : MAPPINGS.entrySet()) {
                AnnotationValues values = m.getAnnotation(e.getKey());
                if (values != null) {
                    if (httpMethod != null) {
                        ctx.error(cls, "More than one mapping annotation on "
                                + controller.binaryName + "." + m.getName());
                        return;
                    }
                    httpMethod = e.getValue();
                    paths = pathsOf(values);
                }
            }
            AnnotationValues mapping = m.getAnnotation(REQUEST_MAPPING);
            if (mapping != null) {
                if (httpMethod != null) {
                    ctx.error(cls, "@RequestMapping and a shorthand mapping on the same "
                            + "method: " + controller.binaryName + "." + m.getName());
                    return;
                }
                httpMethod = mapping.getStringOrDefault("method", "GET");
                paths = pathsOf(mapping);
            }
            if (httpMethod == null) {
                continue;
            }
            // The verb is emitted into the router verbatim, and HttpServer
            // answers 501 to anything outside this set BEFORE dispatch -- so a
            // mistyped or unsupported one compiles into a branch no request can
            // ever reach, and both the build and the running server report
            // success while the endpoint simply does not exist. Case matters
            // for the same reason: the server compares with equals, so "get"
            // is not "GET".
            if (!ROUTABLE_METHODS.contains(httpMethod)) {
                ctx.error(cls, controller.binaryName + "." + m.getName() + " maps HTTP "
                        + "method \"" + httpMethod + "\", which the server does not route: "
                        + "the request would be answered 501 before reaching it. Use one of "
                        + ROUTABLE_METHODS + ", in upper case.");
                return;
            }
            if (paths.isEmpty()) {
                paths = Collections.singletonList("");
            }
            for (String base : controller.basePaths) {
                for (String path : paths) {
                    Route route = buildRoute(cls, m, httpMethod, join(base, path), ctx);
                    if (route == null) {
                        return;
                    }
                    controller.routes.add(route);
                }
            }
        }
        if (controller.routes.isEmpty()) {
            ctx.error(cls, "@RestController declares no mapped methods: " + controller.binaryName);
            return;
        }
        if (!routeShapesAreDistinct(cls, controller, ctx)) {
            return;
        }
        controllers.put(controller.binaryName, controller);
    }

    /**
     * Refuses two routes in one controller that no request can tell apart.
     *
     * A variable's NAME is not part of what the matcher sees, so `GET /notes/{id}`
     * and `GET /notes/{name}` are one shape. The generated router tests the
     * branches in order and returns from the first, which left the second method
     * permanently unreachable with nothing at build time or run time saying so.
     *
     * The shapes are held across controllers, not just within one: the bootstrap
     * chains the routers and takes the first non-null response, so a collision
     * between two controllers is unreachable in precisely the same way.
     */
    private boolean routeShapesAreDistinct(AnnotatedClass cls, Controller controller,
            ProcessorContext ctx) {
        for (int i = 0; i < controller.routes.size(); i++) {
            Route route = controller.routes.get(i);
            String shape = route.httpMethod + " " + route.pattern.replaceAll("\\{[^}]*\\}", "{}");
            String first = routeShapes.get(shape);
            if (first != null) {
                ctx.error(cls, controller.binaryName + "." + route.javaMethod + " and " + first
                        + " both answer " + shape + ". A path variable's NAME is not part of "
                        + "what a request carries, so nothing can tell them apart; the routers "
                        + "are tried in order and the second can never run. Give them different "
                        + "paths, or one method.");
                return false;
            }
            // Within one controller a literal route is emitted before any variable
            // route that would swallow it -- see the comparator in generateRouter.
            // ACROSS controllers nothing orders them: the bootstrap tries the
            // routers in turn and takes the first non-null, so a variable route in
            // an alphabetically earlier controller answers a literal route's own
            // path and that method never runs. There is no ordering to fix, since
            // a router is a set of routes rather than a single pattern, so the
            // ambiguity is reported instead of being resolved arbitrarily.
            String clash = crossControllerClash(controller, route, shape);
            if (clash != null) {
                ctx.error(cls, clash);
                return false;
            }
            routeShapes.put(shape, controller.binaryName + "." + route.javaMethod);
            routeOwners.put(shape, controller.binaryName);
        }
        return true;
    }

    /**
     * Whether a route from another controller and this one can answer each other's
     * paths, and the message saying so.
     *
     * Only ACROSS controllers: inside one, generateRouter's comparator already
     * emits the literal route first.
     */
    private String crossControllerClash(Controller controller, Route route, String shape) {
        String mine = controller.binaryName;
        for (Map.Entry<String, String> e : routeOwners.entrySet()) {
            // Same controller included. Skipping it assumed generateRouter's
            // literal-first comparator settled everything inside one class, and it
            // does not: two DYNAMIC patterns have no dominance, so "/a/{x}/c" and
            // "/a/b/{y}" both answer /a/b/c and whichever the sort happens to emit
            // first wins. That is the same ambiguity as across controllers, and it
            // has the same answer.
            String other = e.getKey();
            // Same verb, or they cannot collide at all -- except that a generated
            // GET block also answers HEAD, so a GET route and a HEAD route on
            // overlapping paths DO compete even though the verbs differ.
            int mySpace = shape.indexOf(' ');
            int otherSpace = other.indexOf(' ');
            if (mySpace < 0 || otherSpace < 0) {
                continue;
            }
            String myVerb = shape.substring(0, mySpace);
            String otherVerb = other.substring(0, otherSpace);
            boolean sameVerb = myVerb.equals(otherVerb);
            boolean getAndHead = isGetHeadPair(myVerb, otherVerb);
            if (!sameVerb && !getAndHead) {
                continue;
            }
            if (!overlaps(other.substring(otherSpace + 1), shape.substring(mySpace + 1))) {
                continue;
            }
            // Inside ONE controller, a wholly literal route and a dynamic one are
            // resolved by generateRouter's comparator: it emits every route with
            // no variables before every route with any, so /users/me is matched
            // before /users/{id} and /users/42 still falls through to it. That
            // pair is the single most ordinary thing to write, and it is what the
            // message below tells people to do -- refusing it left no way to
            // write it at all.
            // Only that pair. Two DYNAMIC shapes have no dominance in that
            // comparator, so "/a/{x}/c" against "/a/b/{y}" is still ambiguous,
            // and two literals that overlap are the same literal twice.
            // Within ONE controller a GET and a HEAD are ordered rather than
            // ambiguous: generateRouter's comparator emits HEAD's own block ahead
            // of GET's fallback, so the declared HEAD wins and the GET still
            // answers everything else. Across controllers there is no such order
            // -- the routers are tried in whatever sequence the bootstrap lists
            // them -- so that pair is exactly as ambiguous as two GETs.
            if (getAndHead && !sameVerb && mine.equals(e.getValue())) {
                continue;
            }
            if (mine.equals(e.getValue()) && isLiteralShape(other) != isLiteralShape(shape)) {
                continue;
            }
            return mine + "." + route.javaMethod + " answers " + shape + ", which "
                    + e.getValue() + " also answers as " + other + ". The routers are "
                    + "tried one after another, so whichever controller happens to "
                    + "come first takes the request and the other method never runs. "
                    + "Put both routes in one controller, where the more specific one "
                    + "is matched first, or give them different paths.";
        }
        return null;
    }

    /**
     * The class for an internal name as it appears on the COMPILE CLASSPATH,
     * whether that is a directory of classes or a jar, or null when it is on
     * neither. Read with ASM rather than loaded: a build must not run a
     * dependency's static initialisers to answer a question about its shape.
     */
    private static AnnotatedClass fromCompileClasspath(ProcessorContext ctx, String internalName) {
        String entryName = internalName + ".class";
        for (String element : ctx.getCompileClasspath()) {
            File file = new File(element);
            if (file.isDirectory()) {
                File candidate = new File(file, entryName.replace('/', File.separatorChar));
                if (candidate.isFile()) {
                    try {
                        return ClassScanner.readClass(candidate);
                    } catch (Exception err) {
                        return null;
                    }
                }
                continue;
            }
            if (!file.isFile()) {
                continue;
            }
            try {
                ZipFile zip = new ZipFile(file);
                try {
                    ZipEntry entry = zip.getEntry(entryName);
                    if (entry != null) {
                        InputStream in = zip.getInputStream(entry);
                        try {
                            return ClassScanner.readClass(in, file);
                        } finally {
                            in.close();
                        }
                    }
                } finally {
                    zip.close();
                }
            } catch (Exception err) {
                continue;             // an unreadable entry is not an answer
            }
        }
        return null;
    }

    /**
     * Whether these two verbs are the GET/HEAD pair, in either order.
     *
     * They are not the same verb, but they answer the same requests: a generated
     * GET block accepts HEAD, which is what makes a controller with only
     * @GetMapping usable by a health check.
     */
    private static boolean isGetHeadPair(String a, String b) {
        return ("GET".equals(a) && "HEAD".equals(b)) || ("HEAD".equals(a) && "GET".equals(b));
    }

    /** The descriptor the scanner keys @Generated by. */
    private static final String GENERATED = PKG + "Generated;";

    /**
     * Whether a class of this name exists AND is somebody else's.
     *
     * The name being taken is not enough. An incremental build -- process-classes
     * a second time, without a clean -- scans target/classes and finds the router
     * this processor wrote on the FIRST pass, so an unconditional lookup reported
     * the processor's own output as a class it would overwrite. Every project
     * using @RestController failed its second build, and only a clean fixed it.
     * Generated classes carry the marker for exactly this reason; a real
     * user-defined collision has no marker and is still refused.
     */
    private static boolean isNotOurOwnOutput(ProcessorContext ctx, String binaryName) {
        AnnotatedClass existing = ctx.lookup(binaryName.replace('.', '/'));
        return existing != null && !existing.getClassAnnotations().containsKey(GENERATED);
    }

    /** HEAD sorts ahead of everything, so its own block precedes GET's fallback. */
    private static int methodRank(String httpMethod) {
        return "HEAD".equals(httpMethod) ? 0 : 1;
    }

    /** A shape with no variables at all, which the router matches before any. */
    private static boolean isLiteralShape(String shape) {
        return shape.indexOf('{') < 0;
    }

    /**
     * Whether one path can satisfy both shapes.
     *
     * Two patterns that BOTH hold variables can still collide: "/a/{x}/c" and
     * "/a/b/{y}" are different shapes, and "/a/b/c" is answered by either. An
     * earlier version compared a variable pattern only against a literal one and
     * returned early whenever both had a variable, which is exactly the case this
     * misses. Segment by segment instead: a variable segment matches any single
     * segment, so two shapes overlap when they have the same number of segments
     * and every pair of segments is compatible.
     */
    private static boolean overlaps(String left, String right) {
        String[] a = left.split("/", -1);
        String[] b = right.split("/", -1);
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!segmentsOverlap(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether two single segments can be the same text.
     *
     * A segment is a literal, a whole variable, or a variable with literal text
     * around it ("{}.json"). Two segments that both contain a variable overlap
     * only when their fixed edges permit it; anything less certain errs toward
     * reporting an ambiguity rather than shipping one.
     */
    static boolean segmentsOverlap(String left, String right) {
        boolean leftVar = left.indexOf("{}") >= 0;
        boolean rightVar = right.indexOf("{}") >= 0;
        if (!leftVar && !rightVar) {
            return left.equals(right);
        }
        if (leftVar && rightVar) {
            // The fixed EDGES decide it. Both carry a variable, but "{}.json" and
            // "{}.xml" cannot both match one segment, and returning true here
            // refused a controller that could never be ambiguous -- while the
            // matcher this check guards handles literals around a variable
            // perfectly well. A segment matching both must start with both
            // prefixes and end with both suffixes, which is only possible when one
            // of each pair contains the other.
            String leftPrefix = left.substring(0, left.indexOf("{}"));
            String rightPrefix = right.substring(0, right.indexOf("{}"));
            String leftSuffix = left.substring(left.lastIndexOf("{}") + 2);
            String rightSuffix = right.substring(right.lastIndexOf("{}") + 2);
            boolean prefixesAgree = leftPrefix.startsWith(rightPrefix)
                    || rightPrefix.startsWith(leftPrefix);
            boolean suffixesAgree = leftSuffix.endsWith(rightSuffix)
                    || rightSuffix.endsWith(leftSuffix);
            return prefixesAgree && suffixesAgree;
        }
        String pattern = leftVar ? left : right;
        String literal = leftVar ? right : left;
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.startsWith("{}", i)) {
                regex.append("[^/]+");
                i++;
            } else {
                char c = pattern.charAt(i);
                if ("\\.[]{}()*+-?^$|".indexOf(c) >= 0) {
                    regex.append('\\');
                }
                regex.append(c);
            }
        }
        return literal.matches(regex.toString());
    }

    /**
     * Splits a route pattern into the parts the generated matcher needs.
     *
     * `/notes/{id}/tags` becomes prefix `/notes/` and one variable followed by
     * `/tags`. The prefix is what the byte compare rejects on, which is most
     * requests for most routes, and it is why the split happens here rather than at
     * request time.
     */
    private Route buildRoute(AnnotatedClass cls, MethodInfo m, String httpMethod, String pattern,
            ProcessorContext ctx) {
        Route route = new Route();
        route.httpMethod = httpMethod;
        route.pattern = pattern.length() == 0 ? "/" : pattern;
        route.javaMethod = m.getName();

        int firstVar = route.pattern.indexOf('{');
        route.prefix = firstVar < 0 ? route.pattern : route.pattern.substring(0, firstVar);
        List<String> variableNames = new ArrayList<String>();
        int pos = firstVar;
        while (pos >= 0) {
            int close = route.pattern.indexOf('}', pos);
            if (close < 0) {
                ctx.error(cls, "Unclosed '{' in route " + route.pattern + " on "
                        + cls.getBinaryName() + "." + m.getName());
                return null;
            }
            variableNames.add(route.pattern.substring(pos + 1, close));
            int next = route.pattern.indexOf('{', close);
            String following = next < 0 ? route.pattern.substring(close + 1)
                                        : route.pattern.substring(close + 1, next);
            // Two variables with nothing between them cannot be split. There is
            // no text to look for, so the matcher gives the first one everything
            // that is left and then fails because a second is still owed --
            // meaning the route compiles and then answers 404 to every request,
            // which is the worst way to be wrong. Nothing can bind it, so it is
            // refused where it is written.
            if (following.length() == 0 && next >= 0) {
                ctx.error(cls, cls.getBinaryName() + "." + m.getName() + " declares the "
                        + "route " + route.pattern + ", where two variables are adjacent. "
                        + "Nothing separates them, so no request could ever match it. Put a "
                        + "literal between them, such as a '/' or a '-'.");
                return null;
            }
            route.after.add(following);
            pos = next;
        }

        Type[] paramTypes = Type.getArgumentTypes(m.getDescriptor());
        String[] genericParams = RestClientAnnotationProcessor.parseGenericParameterSignatures(
                m.getSignature(), paramTypes.length);
        List<Map<String, AnnotationValues>> paramAnnotations = m.getParameterAnnotations();
        for (int i = 0; i < paramTypes.length; i++) {
            Param p = new Param();
            p.javaType = RestClientAnnotationProcessor.javaTypeFor(paramTypes[i], null);
            // The ERASED name drives code generation below, because every check
            // there compares against exact names like "java.util.Map". The
            // generic form is kept separately, for validation only: the
            // descriptor erases List<Note> to java.util.List, and accepting that
            // is how a body of DTOs got through.
            String genericType = genericParams == null || genericParams[i] == null
                    ? null
                    : RestClientAnnotationProcessor.javaTypeFor(paramTypes[i], genericParams[i]);
            Map<String, AnnotationValues> annotations = i < paramAnnotations.size()
                    ? paramAnnotations.get(i) : Collections.<String, AnnotationValues>emptyMap();
            AnnotationValues pathVariable = annotations.get(PATH_VARIABLE);
            AnnotationValues requestParam = annotations.get(REQUEST_PARAM);
            AnnotationValues requestHeader = annotations.get(REQUEST_HEADER);
            AnnotationValues requestBody = annotations.get(REQUEST_BODY);
            if (pathVariable != null) {
                p.kind = "PATH";
                p.name = pathVariable.getStringOrDefault("value", "");
                p.defaultValue = pathVariable.getStringOrDefault("defaultValue", "");
                p.variableIndex = variableNames.indexOf(p.name);
                if (p.name.length() == 0) {
                    // Parameter names are not in the class file unless javac was told to
                    // keep them, and a router that guessed would bind the wrong value in
                    // silence. Naming it is one word and removes the whole question.
                    ctx.error(cls, "@PathVariable needs the variable name, as "
                            + "@PathVariable(\"id\"): " + cls.getBinaryName() + "."
                            + m.getName());
                    return null;
                }
                if (p.variableIndex < 0) {
                    ctx.error(cls, "@PathVariable(\"" + p.name + "\") does not appear in the "
                            + "route " + route.pattern + " on " + cls.getBinaryName() + "."
                            + m.getName());
                    return null;
                }
            } else if (requestParam != null) {
                p.kind = "QUERY";
                p.name = requestParam.getStringOrDefault("value", "");
                p.defaultValue = requestParam.getStringOrDefault("defaultValue", "");
                p.required = requestParam.getBoolOrDefault("required", true);
                if (p.name.length() == 0) {
                    ctx.error(cls, "@RequestParam needs the parameter name: "
                            + cls.getBinaryName() + "." + m.getName());
                    return null;
                }
            } else if (requestHeader != null) {
                p.kind = "HEADER";
                p.name = requestHeader.getStringOrDefault("value", "");
                p.defaultValue = requestHeader.getStringOrDefault("defaultValue", "");
                p.required = requestHeader.getBoolOrDefault("required", true);
                if (p.name.length() == 0) {
                    ctx.error(cls, "@RequestHeader needs the header name: "
                            + cls.getBinaryName() + "." + m.getName());
                    return null;
                }
            } else if (requestBody != null) {
                p.kind = "BODY";
                p.required = requestBody.getBoolOrDefault("required", true);
            } else if (REQUEST_TYPE.equals(p.javaType)) {
                // The escape hatch: a handler that needs something this binding does not
                // model takes the Request itself, exactly as it would have before.
                p.kind = "REQUEST";
            } else {
                ctx.error(cls, "Parameter " + (i + 1) + " of " + cls.getBinaryName() + "."
                        + m.getName() + " has no binding annotation. Annotate it with "
                        + "@PathVariable, @RequestParam, @RequestHeader or @RequestBody, "
                        + "or declare it as HttpServer.Request");
                return null;
            }
            p.genericJavaType = genericType;
            String badKey = "BODY".equals(p.kind) ? unusableMapKey(genericType) : null;
            if (badKey != null) {
                // Separate from the element rule below, and with its own message,
                // because Long is a perfectly good body VALUE -- every JSON
                // integer arrives as one -- and only wrong as a KEY. The element
                // check therefore approves Map<Long,String>, and the emitted
                // shape check walks values() alone, so the map reached the
                // handler with keys that violate its own declaration.
                ctx.error(cls, "Cannot bind " + genericType + " from the body on "
                        + cls.getBinaryName() + "." + m.getName() + ". A JSON object's "
                        + "names are strings, so " + badKey + " keys arrive as String: "
                        + "iterating them as the declared type throws and get(" + badKey
                        + ") silently misses the value the client sent. Key the map by "
                        + "String.");
                return null;
            }
            if ("BODY".equals(p.kind) && !bodyElementsAreDecoded(genericType)) {
                ctx.error(cls, "Cannot bind " + genericType + " from the body on "
                        + cls.getBinaryName() + "." + m.getName() + ". A body is decoded "
                        + "by the JSON parser, which produces Map, List, String, Long, "
                        + "Double and Boolean -- so the elements arrive as Map and "
                        + "iterating them as the declared type throws, answering 500 "
                        + "from an endpoint that packaged cleanly. Take Map or "
                        + "List<Map> and convert, or use a @RestClient contract, which "
                        + "generates the codecs.");
                return null;
            }
            if (!"REQUEST".equals(p.kind) && !isBindable(p.javaType, p.kind)) {
                ctx.error(cls, "Cannot bind " + p.javaType + " from the request on "
                        + cls.getBinaryName() + "." + m.getName() + ". Path, query and "
                        + "header values bind to String and the primitive types; a body "
                        + "binds to String or java.util.Map");
                return null;
            }
            // A default that is not a value of the parameter's type. The generated
            // to<Type> answers its fallback for anything unparseable, so
            // defaultValue="oops" on an int became 0 -- and because the default is
            // NON-EMPTY the required-value guard is skipped too, so an absent
            // parameter called the handler with a number the controller never
            // wrote. This is the author's own configuration, not a client's input,
            // and it is wrong at build time or never.
            if (p.defaultValue != null && p.defaultValue.length() > 0
                    && !defaultParsesAs(p.javaType, p.defaultValue)) {
                ctx.error(cls, cls.getBinaryName() + "." + m.getName() + " declares "
                        + "defaultValue=\"" + p.defaultValue + "\" for a " + p.javaType
                        + " parameter, which is not a " + p.javaType + ". It would be "
                        + "silently replaced by zero, and the handler would run on a "
                        + "value nobody wrote.");
                return null;
            }
            route.params.add(p);
        }

        // The generic signature, not just the descriptor: the descriptor erases
        // List<Note> to java.util.List, and the check below would then approve
        // the container without ever looking at what is IN it.
        route.returnJavaType = RestClientAnnotationProcessor.javaTypeFor(
                Type.getReturnType(m.getDescriptor()), returnSignature(m.getSignature()));
        // A return type this router can actually turn into JSON. Anything else
        // reached Json.write as an unknown object and came out as the QUOTED
        // result of its toString() -- "com.example.Note@1a2b3c" where the caller
        // expected an object -- while the build and the request both reported
        // success. This processor has no DTO codec generation (the @RestClient
        // half does), so the honest answer today is to refuse the shape rather
        // than emit JSON nobody can use.
        if (!isEncodableReturn(route.returnJavaType, ctx)) {
            ctx.error(cls, cls.getBinaryName() + "." + m.getName() + " returns "
                    + route.returnJavaType + ", which the generated router cannot encode: "
                    + "it would be written as the JSON string of its toString(). Return a "
                    + "Map, a List, a Set, a String, a primitive, an HttpServer.Response, "
                    + "or make the type implement com.codename1.backend.Json.Writable.");
            return null;
        }
        AnnotationValues status = m.getAnnotation(RESPONSE_STATUS);
        // ResponseStatus documents that a value-returning method answers 200 and a
        // void one answers 204. Defaulting to 200 for both made the annotation's
        // own javadoc wrong about the case it exists to describe.
        int implied = "void".equals(route.returnJavaType) ? 204 : 200;
        route.status = status == null ? implied : status.getIntOrDefault("value", implied);
        // A typo here is copied straight into the generated router, and neither
        // writer questions it: HTTP/1 emits it as the status line and HTTP/2
        // submits it as :status, so a handler that worked perfectly answers with
        // something the client rejects or cannot frame. Three digits is the whole
        // of what HTTP defines.
        if (route.status < 200 || route.status > 599) {
            ctx.error(cls, cls.getBinaryName() + "." + m.getName() + " declares "
                    + "@ResponseStatus(" + route.status + "), which cannot be a handler's "
                    + "answer: a generated route sends ONE response, so it has to be a "
                    + "final status between 200 and 599. A 1xx is interim -- the client "
                    + "would go on waiting for the final response, and 101 is not legal "
                    + "over HTTP/2 at all.");
            return null;
        }
        return route;
    }

    /**
     * Whether every type argument of a body type is something the JSON parser
     * actually produces. It answers Map for an object, List for an array, and
     * String/Long/Double/Boolean for the scalars -- so a List<Note> is a list of
     * Map at runtime, and the first use of an element as a Note throws.
     */
    /* package-private, not private: RestServerAnnotationProcessor decodes bodies
       with the same parser and therefore needs the identical rule. One copy, so
       the two halves cannot drift into disagreeing about what a body may hold. */
    static boolean bodyElementsAreDecoded(String javaType) {
        if (javaType == null) {
            return true;
        }
        int lt = javaType.indexOf('<');
        if (lt < 0) {
            return true;                      // raw, so nothing was claimed
        }
        int end = javaType.lastIndexOf('>');
        if (end <= lt) {
            return true;
        }
        List<String> args = splitTypeArguments(javaType.substring(lt + 1, end));
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("?")) {
                continue;                     // a wildcard claims nothing either
            }
            if (!PARSED_JSON_TYPES.contains(arg) && !bodyElementsAreDecoded(arg)) {
                return false;
            }
            int inner = arg.indexOf('<');
            String rawArg = inner < 0 ? arg : arg.substring(0, inner);
            if (!PARSED_JSON_TYPES.contains(rawArg)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The first map key type in this declaration that a JSON body cannot produce,
     * or null when every one of them is usable.
     *
     * Recursive, because the map need not be the outer type: List<Map<Long,?>> has
     * the same problem one level down. Object and a wildcard claim nothing, so
     * they are fine; String is what actually arrives.
     */
    static String unusableMapKey(String javaType) {
        if (javaType == null) {
            return null;
        }
        int lt = javaType.indexOf('<');
        int end = javaType.lastIndexOf('>');
        if (lt < 0 || end <= lt) {
            return null;
        }
        List<String> args = splitTypeArguments(javaType.substring(lt + 1, end));
        if ("java.util.Map".equals(javaType.substring(0, lt)) && args.size() == 2) {
            // A BOUNDED wildcard is not the same as an unbounded one. Exempting
            // anything beginning with '?' let Map<? extends Long, V> through,
            // and that bound is still a promise that every key is a Long -- so
            // `for (Long key : body.keySet())` compiles and then fails on the
            // Strings a JSON object really produces. Only the unbounded ? claims
            // nothing; a bound is judged exactly like a spelled-out type.
            String key = args.get(0).trim();
            if (!"?".equals(key)) {
                // Already normalised by splitTypeArguments, so "? extends Long"
                // arrives here as Long and only a genuinely unbounded wildcard
                // is still spelled "?". One rule, in one place.
                int inner = key.indexOf('<');
                String rawKey = inner < 0 ? key : key.substring(0, inner);
                if (!"java.lang.String".equals(rawKey)
                        && !"java.lang.Object".equals(rawKey)) {
                    return rawKey;
                }
            }
        }
        for (int i = 0; i < args.size(); i++) {
            String nested = unusableMapKey(args.get(i));
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    /** What Json.parse produces, and therefore all a body can be made of. */
    private static final Set<String> PARSED_JSON_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "java.lang.Object", "java.lang.String", "java.lang.Long",
                    "java.lang.Double", "java.lang.Boolean",
                    "java.util.Map", "java.util.List")));

    private static boolean isBindable(String javaType, String kind) {
        if ("BODY".equals(kind)) {
            return "java.lang.String".equals(javaType) || "java.util.Map".equals(javaType)
                    || "java.util.List".equals(javaType);
        }
        return "java.lang.String".equals(javaType) || "int".equals(javaType)
                || "long".equals(javaType) || "boolean".equals(javaType)
                || "double".equals(javaType) || "float".equals(javaType)
                || "short".equals(javaType) || "byte".equals(javaType);
    }

    private static boolean hasNoArgConstructor(AnnotatedClass cls) {
        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() && m.isPublic()
                    && Type.getArgumentTypes(m.getDescriptor()).length == 0) {
                return true;
            }
        }
        return false;
    }

    private static List<String> pathsOf(AnnotationValues values) {
        List<String> out = new ArrayList<String>();
        if (values == null) {
            return out;
        }
        Object value = values.get("value");
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                out.add(String.valueOf(item));
            }
        } else if (value instanceof Object[]) {
            for (Object item : (Object[]) value) {
                out.add(String.valueOf(item));
            }
        } else if (value != null) {
            out.add(String.valueOf(value));
        }
        return out;
    }

    /** Joins a class-level base with a method-level path, without doubling the slash. */
    private static String join(String base, String path) {
        String left = base == null ? "" : base.trim();
        String right = path == null ? "" : path.trim();
        while (left.endsWith("/")) {
            left = left.substring(0, left.length() - 1);
        }
        // Every request target begins with "/", so a RELATIVE class prefix built
        // a route no request could ever equal: @RequestMapping("api") plus
        // "/users" produced "api/users", and /api/users answered 404 from an
        // endpoint that packaged perfectly. The method-level half was normalised
        // this way already; the class-level half was not.
        if (left.length() > 0 && !left.startsWith("/")) {
            left = "/" + left;
        }
        if (right.length() == 0) {
            return left.length() == 0 ? "/" : left;
        }
        if (!right.startsWith("/")) {
            right = "/" + right;
        }
        return left + right;
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors() || controllers.isEmpty()) {
            return;
        }
        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (Controller c : controllers.values()) {
            String router = qualify(c.packageName, c.routerSimpleName);
            // The same check the bootstrap gets below, for the same reason: a class
            // of this name already in that package is overwritten in the output
            // directory by the one compiled here, silently, because what is
            // generated compiles perfectly well. Guarding only the bootstrap left
            // every <Controller>Router able to replace a real class.
            if (isNotOurOwnOutput(ctx, router)) {
                ctx.error(router + " already exists, and the router generated for "
                        + c.binaryName + " would replace it. Rename that class, or "
                        + "rename the controller.");
                return;
            }
            sources.put(router, generateRouter(c));
        }
        Controller first = controllers.values().iterator().next();
        String bootstrap = qualify(first.packageName, "BackendApplication");
        // A class of this name already in that package would be OVERWRITTEN in the
        // output directory by the one compiled below -- silently, because the
        // generated source compiles perfectly well. The packaged application then
        // runs this bootstrap instead of the developer's own, dropping whatever
        // startup it did: TLS, middleware, pooling. Refusing is the only safe
        // answer, since there is no way to tell which one they meant.
        if (isNotOurOwnOutput(ctx, bootstrap)) {
            ctx.error(first.packageName + ".BackendApplication already "
                    + "exists, and the generated entry point would replace it. Rename "
                    + "that class, or move the controllers into another package.");
            return;
        }
        sources.put(bootstrap, generateBootstrap(first.packageName));
        try {
            List<File> cp = new ArrayList<File>();
            cp.add(ctx.getOutputClassDir());
            for (String element : ctx.getCompileClasspath()) {
                cp.add(new File(element));
            }
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
            ctx.emitResource(MAIN_CLASS_RESOURCE, asciiBytes(bootstrap));
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile the generated @RestController "
                    + "sources: " + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + controllers.size() + " @RestController router(s) and "
                + bootstrap);
    }

    private static byte[] asciiBytes(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required of every VM", err);
        }
    }

    private static String qualify(String pkg, String simple) {
        return pkg.length() == 0 ? simple : pkg + "." + simple;
    }

    /**
     * The router for one controller.
     *
     * Routes are emitted grouped by HTTP method and, within a group, static routes
     * before dynamic ones. A static route is a single byte compare; a dynamic one
     * pays for a prefix compare first, so an unrelated request leaves without
     * allocating anything.
     */
    private static String generateRouter(Controller c) {
        StringBuilder sb = new StringBuilder();
        if (c.packageName.length() > 0) {
            sb.append("package ").append(c.packageName).append(";\n\n");
        }
        sb.append("// Generated from @RestController on ").append(c.binaryName)
          .append(". Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(c.routerSimpleName)
          .append(" implements com.codename1.backend.HttpServer.Handler {\n\n");

        List<Route> ordered = new ArrayList<Route>(c.routes);
        Collections.sort(ordered, new java.util.Comparator<Route>() {
            public int compare(Route a, Route b) {
                // HEAD before GET, because a GET block also answers HEAD and
                // dispatch returns on the first block that matches. Alphabetically
                // GET comes first, which would have made a controller's explicit
                // HEAD route unreachable the moment the GET fallback was added --
                // the fallback swallowing the specific case it defers to.
                int byMethod = methodRank(a.httpMethod) - methodRank(b.httpMethod);
                if (byMethod == 0) {
                    byMethod = a.httpMethod.compareTo(b.httpMethod);
                }
                if (byMethod != 0) {
                    return byMethod;
                }
                // Static routes first: they answer without touching the heap, and a
                // dynamic route whose prefix also matches must not take the request
                // from one that matches exactly.
                int byKind = (a.after.isEmpty() ? 0 : 1) - (b.after.isEmpty() ? 0 : 1);
                if (byKind != 0) {
                    return byKind;
                }
                return b.pattern.length() - a.pattern.length();
            }
        });

        for (int i = 0; i < ordered.size(); i++) {
            Route route = ordered.get(i);
            sb.append("    private static final byte[] P").append(i).append(" = ")
              .append(byteArrayLiteral(route.after.isEmpty() ? route.pattern : route.prefix))
              .append("; // ").append(route.httpMethod).append(' ').append(route.pattern)
              .append('\n');
            if (!route.after.isEmpty()) {
                sb.append("    private static final String[] A").append(i).append(" = ")
                  .append(stringArrayLiteral(route.after)).append(";\n");
            }
        }

        sb.append("\n    private final ").append(c.simpleName).append(" impl;\n\n");
        sb.append("    public ").append(c.routerSimpleName).append("(").append(c.simpleName)
          .append(" impl) {\n        this.impl = impl;\n    }\n\n");

        // throws Exception because Handler does: a controller method that reads a
        // database throws IOException, and a router that could not pass it on would
        // force every handler to swallow its own errors.
        sb.append("    public com.codename1.backend.HttpServer.Response handle(\n")
          .append("            com.codename1.backend.HttpServer.Request request) throws Exception {\n");
        sb.append("        String httpMethod = request.getMethod();\n");
        // Checked ONCE, here, rather than inside the matcher: bindFrom answers a
        // boolean, so a decoder that refused would only turn a malformed escape
        // into "no route" -- a 404 for what is a syntax error the client can fix.
        sb.append("        if (!wellFormedEscapes(request.getTarget())) {\n");
        sb.append("            return request.respond(400, \"text/plain; charset=utf-8\",\n");
        sb.append("                    utf8(\"malformed percent-escape in the request target\"));\n");
        sb.append("        }\n");

        String current = null;
        boolean open = false;
        for (int i = 0; i < ordered.size(); i++) {
            Route route = ordered.get(i);
            if (!route.httpMethod.equals(current)) {
                if (open) {
                    sb.append("        }\n");
                }
                sb.append("        if (\"").append(route.httpMethod)
                  .append("\".equals(httpMethod)");
                if ("GET".equals(route.httpMethod)) {
                    // A HEAD asks what a GET would answer, so a GET route is the
                    // route for it -- the server routes HEAD and its writer already
                    // suppresses the body and reports the length a GET would have
                    // sent. Without this, a controller declaring only @GetMapping
                    // answered 404 to every HEAD, which breaks the health checks and
                    // cache probes that use it, and disagrees with the Spring-style
                    // semantics these annotations borrow. An explicit @RequestMapping
                    // for HEAD still wins: its own block is emitted separately and
                    // dispatch returns on the first that matches.
                    sb.append(" || \"HEAD\".equals(httpMethod)");
                }
                sb.append(") {\n");
                current = route.httpMethod;
                open = true;
            }
            emitRoute(sb, route, i, c);
        }
        if (open) {
            sb.append("        }\n");
        }
        sb.append("        // No route here. Returning null lets the server answer 404, and\n");
        sb.append("        // lets another router be tried first when several are chained.\n");
        sb.append("        return null;\n    }\n\n");
        emitRouterHelpers(sb);
        sb.append("}\n");
        return sb.toString();
    }

    private static void emitRoute(StringBuilder sb, Route route, int index, Controller c) {
        String pad = "                ";
        if (route.after.isEmpty()) {
            sb.append("            if (request.pathIs(P").append(index).append(")) {\n");
        } else {
            sb.append("            if (request.pathStartsWith(P").append(index).append(")) {\n");
            sb.append(pad).append("String[] bound = bindPath(request.pathFrom(P").append(index)
              .append(".length), A").append(index).append(");\n");
            sb.append(pad).append("if (bound != null) {\n");
            pad = "                    ";
        }

        emitRequiredGuards(sb, route, pad);
        emitScalarGuards(sb, route, pad);
        emitBodyLocals(sb, route, pad);

        StringBuilder args = new StringBuilder();
        for (int i = 0; i < route.params.size(); i++) {
            Param p = route.params.get(i);
            if (i > 0) {
                args.append(", ");
            }
            args.append(argumentExpression(p));
        }

        String call = "impl." + route.javaMethod + "(" + args + ")";
        if ("void".equals(route.returnJavaType)) {
            sb.append(pad).append(call).append(";\n");
            sb.append(pad).append("return request.respond(").append(route.status)
              .append(", \"text/plain\", EMPTY);\n");
        } else if (isResponseType(route.returnJavaType)) {
            // The handler built its own Response; a status annotation would be a lie
            // about something this router no longer controls.
            sb.append(pad).append("return ").append(call).append(";\n");
        } else if ("java.lang.String".equals(route.returnJavaType)) {
            sb.append(pad).append("String result = ").append(call).append(";\n");
            sb.append(pad).append("return result == null ? request.respond(404, \"text/plain\", EMPTY)\n");
            sb.append(pad).append("        : request.respond(").append(route.status)
              .append(", \"text/plain; charset=utf-8\", utf8(result));\n");
        } else {
            // Everything else is JSON. respondJson writes into the connection's own
            // Response, so a route that returns a value still allocates only that value.
            sb.append(pad).append("Object result = ").append(call).append(";\n");
            sb.append(pad).append("return result == null ? request.respond(404, \"text/plain\", EMPTY)\n");
            sb.append(pad).append("        : request.respondJson(").append(route.status)
              .append(", result);\n");
        }

        if (!route.after.isEmpty()) {
            sb.append("                }\n");
        }
        sb.append("            }\n");
    }

    /**
     * Refuses a request that omits a binding declared required.
     *
     * Without this the `required` element of RequestParam, RequestHeader and
     * RequestBody was read by nobody: an absent value simply converted to null,
     * or to a primitive zero, and the handler ran as though the client had sent
     * one. Both settings behaved identically, so the annotation documented a
     * check that did not exist. A declared default supplies the value instead,
     * so it makes the parameter satisfiable and no guard is emitted.
     */
    private static void emitRequiredGuards(StringBuilder sb, Route route, String pad) {
        for (int i = 0; i < route.params.size(); i++) {
            Param p = route.params.get(i);
            if (!p.required || (p.defaultValue != null && p.defaultValue.length() > 0)) {
                continue;
            }
            String test;
            String what;
            if ("QUERY".equals(p.kind)) {
                test = "request.queryParam(" + quote(p.name) + ") == null";
                what = "query parameter " + p.name;
            } else if ("HEADER".equals(p.kind)) {
                test = "request.getHeader(" + quote(p.name) + ") == null";
                what = "header " + p.name;
            } else if ("BODY".equals(p.kind)) {
                test = "request.getBody() == null || request.getBody().length() == 0";
                what = "request body";
            } else {
                // A path variable cannot be absent: the route only matched because
                // the segment was there.
                continue;
            }
            sb.append(pad).append("if (").append(test).append(") {\n");
            sb.append(pad).append("    return request.respond(400, \"text/plain; charset=utf-8\",\n");
            sb.append(pad).append("            utf8(").append(quote("Missing required " + what))
              .append("));\n");
            sb.append(pad).append("}\n");
        }
    }

    /**
     * Decodes a structured body into a local, and refuses one that will not parse.
     *
     * bodyAsMap/bodyAsList answer null both for "there was no body" and for "the
     * body was not JSON", and the call site could not tell those apart: malformed
     * client JSON was handed to the controller as a null argument, so it surfaced
     * as a 404, as a 500 from dereferencing it, or as a side effect performed with
     * an argument the client never sent. Only the second case is a 400, so the
     * emptiness test comes first and an absent body stays null for a binding that
     * allows it.
     */
    /**
     * Refuses a scalar binding whose value is present but is not that type.
     *
     * "page=zz" for an int used to bind the annotation's defaultValue, so a client
     * sending a typo got a different page rather than an error, and the handler
     * could not tell the two apart. The annotation says defaultValue is "used when
     * the request omits it", and a malformed value is not an omission -- the same
     * shape of bug as an annotation element nothing reads.
     *
     * Numeric types only. toBoolean maps several spellings to true and everything
     * else to false, which is a convention rather than a parse that can fail.
     */
    private static void emitScalarGuards(StringBuilder sb, Route route, String pad) {
        for (int i = 0; i < route.params.size(); i++) {
            Param p = route.params.get(i);
            String checker = numericChecker(p.javaType);
            if (checker == null) {
                continue;
            }
            String raw;
            String what;
            if ("PATH".equals(p.kind)) {
                raw = "bound[" + p.variableIndex + "]";
                what = "path variable " + p.name;
            } else if ("QUERY".equals(p.kind)) {
                raw = "request.queryParam(" + quote(p.name) + ")";
                what = "query parameter " + p.name;
            } else if ("HEADER".equals(p.kind)) {
                raw = "request.getHeader(" + quote(p.name) + ")";
                what = "header " + p.name;
            } else {
                continue;
            }
            sb.append(pad).append("if (!").append(checker).append('(').append(raw)
              .append(")) {\n");
            sb.append(pad).append("    return request.respond(400, \"text/plain; charset=utf-8\",\n");
            sb.append(pad).append("            utf8(").append(quote("The " + what + " is not a valid "
                    + p.javaType)).append("));\n");
            sb.append(pad).append("}\n");
        }
    }

    /** Whether Json.write turns this return type into something other than toString(). */
    private static boolean isEncodableReturn(String javaType, ProcessorContext ctx) {
        return isEncodableReturn(javaType, ctx, true);
    }

    /**
     * @param top whether this is the RETURN type itself rather than something
     *            inside it. void and HttpServer.Response are answers a route can
     *            give; they are not values Json can write. emitRoute handles a
     *            directly returned Response by sending it, so the exemption is
     *            real at the top and false anywhere else -- a
     *            List&lt;Response&gt; reaches the writer's fallback and each
     *            element is emitted as the quoted result of its toString().
     */
    private static boolean isEncodableReturn(String javaType, ProcessorContext ctx,
                                             boolean top) {
        if (javaType == null) {
            return true;
        }
        if ("void".equals(javaType) || isResponseType(javaType)) {
            return top;
        }
        // Arrays before anything else, because both tests below wave them
        // through: a primitive array's name has no dot and a JDK array's name
        // begins with "java.". Json writes byte[] as base64 and has no handling
        // for any other array at all, so int[] or String[] reaches
        // String.valueOf and is written as the JSON STRING "[I@1a2b3c" -- the
        // array's identity, not its contents.
        if (javaType.endsWith("[]")) {
            return "byte[]".equals(javaType);
        }
        String raw = javaType;
        int lt = raw.indexOf('<');
        if (lt >= 0) {
            raw = raw.substring(0, lt);
            // What Json actually writes is the ELEMENTS, so a container is only
            // encodable when they are. java.util.List passes the raw check
            // below on its own name, while every Note inside it comes out as
            // the quoted result of its toString().
            int end = javaType.lastIndexOf('>');
            if (end > lt) {
                List<String> args = splitTypeArguments(javaType.substring(lt + 1, end));
                // A map's KEY is not written the way its values are. Json.writeValue
                // calls String.valueOf on every key whatever its type, so
                // Map<byte[], String> comes back with keys spelled "[B@1a2b3c" and
                // a Map<Note, String> with "com.example.Note@1a2b3c" -- object
                // identity, not data, and different on every run. Checking the key
                // as though it were a value approved both: byte[] and a writable
                // DTO are perfectly good VALUES. This is the return-side twin of
                // the rule that a JSON object's names arrive as strings.
                if ("java.util.Map".equals(raw) && args.size() == 2) {
                    String key = args.get(0);
                    int keyLt = key.indexOf('<');
                    String rawKey = keyLt < 0 ? key : key.substring(0, keyLt);
                    if (!"java.lang.String".equals(rawKey)) {
                        return false;
                    }
                }
                for (int i = 0; i < args.size(); i++) {
                    if (!isEncodableReturn(args.get(i), ctx, false)) {
                        return false;
                    }
                }
            }
        }
        // Only the JDK shapes Json ACTUALLY writes. "Anything under java." was too
        // generous by a wide margin: java.util.Date reaches Json's final branch
        // and comes back as a quoted, implementation-formatted toString(), and
        // java.lang.Object holding a DTO comes back as "com.example.Note@1a2b3c"
        // -- the same defect the DTO check exists to stop, arriving through a
        // wider declared type. This list mirrors the branches of Json.writeValue
        // in order; a type added there belongs here too.
        if ("?".equals(raw)) {
            // A wildcard is UNKNOWN, not primitive, and it has no dot -- so it fell
            // into the branch below and was approved as though it were an int. The
            // handler can then return a Date or a DTO inside a List<?> and Json
            // writes the quoted toString(), which is exactly what this validation
            // refuses when the same thing is declared as List<Object>. Note the
            // asymmetry with a BODY: an unknown element arriving is the client's
            // to shape, while an unknown element leaving is ours to serialise.
            return false;
        }
        if (raw.indexOf('.') < 0) {
            return true;              // a primitive, which is always written as one
        }
        if (JSON_JDK_TYPES.contains(raw)) {
            return true;
        }
        if (raw.startsWith("java.")) {
            return false;             // some other JDK type Json would toString()
        }
        String internal = raw.replace('.', '/');
        AnnotatedClass cls = ctx.lookup(internal);
        if (cls == null) {
            // Not in the index because the index holds only what this project
            // compiles -- so a DTO from a DEPENDENCY landed here and was waved
            // through, and Json wrote it as the quoted result of its toString().
            // The compile classpath is where such a type actually lives, and it
            // is read the same way the index was built: with ASM, so nothing is
            // loaded and no static initialiser runs.
            cls = fromCompileClasspath(ctx, internal);
        }
        if (cls == null) {
            return false;             // cannot be inspected, so cannot be trusted
        }
        for (String itf : cls.getInterfaceInternalNames()) {
            if ("com/codename1/backend/Json$Writable".equals(itf)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The return portion of a generic method signature, or null when the method
     * carries none. Only the descriptor is guaranteed to exist, and it is the
     * erased form.
     */
    private static String returnSignature(String signature) {
        if (signature == null) {
            return null;
        }
        int close = signature.lastIndexOf(')');
        if (close < 0 || close + 1 >= signature.length()) {
            return null;
        }
        return signature.substring(close + 1);
    }

    /**
     * Splits type arguments on their TOP-LEVEL commas, so the two arguments of
     * Map&lt;String, List&lt;Note&gt;&gt; come back whole rather than being cut
     * inside the nested one.
     */
    /* package-private for the same reason as bodyElementsAreDecoded above. */
    static List<String> splitTypeArguments(String args) {
        List<String> out = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                out.add(withoutWildcard(args.substring(start, i).trim()));
                start = i + 1;
            }
        }
        String last = args.substring(start).trim();
        if (last.length() > 0) {
            out.add(withoutWildcard(last));
        }
        return out;
    }

    /**
     * A type argument reduced to what it actually PROMISES about the value.
     *
     * Normalised here, at the one place type arguments are produced, rather than
     * at each of the six consumers -- validation, the element type, the map value
     * type, the encodability walk and the emitted instanceof checks all ask the
     * same question, and a bounded wildcard was being read as "claims nothing" by
     * every one of them. `List<? extends String>` was accepted with no runtime
     * check at all, so `[1]` reached the handler as a list holding a Long and the
     * first typed read answered 500 where a 400 was owed.
     *
     * `? extends T` promises T. `? super T` does NOT: the value may be T or any
     * supertype of it, so the only honest reading is the unbounded one, and a
     * check against T there would reject values the declaration allows.
     */
    private static String withoutWildcard(String arg) {
        if (arg.startsWith("? extends ")) {
            return arg.substring("? extends ".length()).trim();
        }
        if (arg.startsWith("? super ")) {
            return "?";
        }
        return arg;
    }

    /** Whether an annotation's declared default really is a value of that type. */
    private static boolean defaultParsesAs(String javaType, String value) {
        String v = value.trim();
        try {
            if ("int".equals(javaType)) {
                Integer.parseInt(v);
            } else if ("long".equals(javaType)) {
                Long.parseLong(v);
            } else if ("short".equals(javaType)) {
                Short.parseShort(v);
            } else if ("byte".equals(javaType)) {
                Byte.parseByte(v);
            } else if ("double".equals(javaType)) {
                // parseDouble answers infinity for 1e999 rather than throwing, so
                // this branch used to approve a default the RUNTIME guard rejects
                // in the identical spelling: omit the value and the generated
                // converter hands the controller an infinity, send it and the
                // request is a 400. The same rule as the request path, then, and
                // the same test -- the SPELLING decides whether an infinity was
                // meant, because the parsed value cannot tell 1e999 from Infinity.
                double d = Double.parseDouble(v);
                return !Double.isInfinite(d) || spellsInfinity(v);
            } else if ("float".equals(javaType)) {
                // Same rule, and it was wrong here in the other direction:
                // Double.isInfinite was the "did they mean it" test, and
                // Double.parseDouble("1e999") is itself infinite, so every
                // double-overflowing default was read as a deliberate infinity.
                double d = Double.parseDouble(v);
                return !Float.isInfinite((float) d) || spellsInfinity(v);
            } else if ("boolean".equals(javaType)) {
                // The binder accepts only these two, so a default of "yes" would
                // bind false and read as a deliberate choice.
                return "true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v);
            }
            return true;                  // String and anything else: no parsing
        } catch (NumberFormatException err) {
            return false;
        }
    }

    /**
     * Whether this text asks for an infinity, rather than merely producing one.
     *
     * The same test the generated guards use, deliberately: parseDouble answers
     * infinity for both "Infinity" and "1e999", so only the text tells the two
     * apart, and a default and a request value that disagreed about which is
     * which is exactly the divergence this pair of rules exists to prevent.
     */
    private static boolean spellsInfinity(String value) {
        return value.trim().indexOf("Infinity") >= 0;
    }

    private static String numericChecker(String javaType) {
        if ("boolean".equals(javaType)) return "parsesBoolean";
        if ("int".equals(javaType)) return "parsesInt";
        if ("long".equals(javaType)) return "parsesLong";
        if ("double".equals(javaType)) return "parsesDouble";
        if ("float".equals(javaType)) return "parsesFloat";
        if ("short".equals(javaType)) return "parsesShort";
        if ("byte".equals(javaType)) return "parsesByte";
        return null;
    }

    private static void emitBodyLocals(StringBuilder sb, Route route, String pad) {
        for (int i = 0; i < route.params.size(); i++) {
            Param p = route.params.get(i);
            if (!"BODY".equals(p.kind) || "java.lang.String".equals(p.javaType)) {
                continue;
            }
            boolean map = "java.util.Map".equals(p.javaType);
            String type = map ? "java.util.Map" : "java.util.List";
            String decoder = map ? "bodyAsMap" : "bodyAsList";
            p.local = "body" + i;
            sb.append(pad).append(type).append(' ').append(p.local).append(" = null;\n");
            sb.append(pad).append("if (request.getBody() != null && request.getBody().length() > 0) {\n");
            sb.append(pad).append("    ").append(p.local).append(" = ").append(decoder)
              .append("(request.getBody());\n");
            sb.append(pad).append("    if (").append(p.local).append(" == null) {\n");
            sb.append(pad).append("        return request.respond(400, \"text/plain; charset=utf-8\",\n");
            sb.append(pad).append("                utf8(\"The request body is not valid JSON\"));\n");
            sb.append(pad).append("    }\n");
            // Declaring List<String> does not make the ELEMENTS strings. The
            // build-time check says the declared element type is one the parser
            // can produce; what it actually produced depends on what the client
            // sent, so "[1]" fills a List<String> with a Long and the handler's
            // first read of it throws -- turning a malformed request into a 500
            // instead of the 400 it is. Checked with instanceof, never a cast:
            // a failed cast does not throw in the packaged runtime at all.
            // Maps as well as lists. A Map<String,String> that receives
            // {"value":1} holds a Long under a String declaration, and the
            // handler's first typed read throws -- the same 500-for-a-400 the
            // list case had, skipped only because the check was written for
            // lists and the map branch went past it.
            emitShapeChecks(sb, pad + "    ", p.local, p.genericJavaType, 0);
            sb.append(pad).append("}\n");
        }
    }

    /**
     * Emits the runtime element checks for one declared container, and for
     * whatever its elements are declared to contain, to whatever depth the
     * declaration goes. Each level is a loop; the innermost is an instanceof.
     *
     * instanceof rather than a cast at every level, because a failed cast does
     * not throw in the packaged runtime -- the wrong object is simply handed on.
     */
    private static void emitShapeChecks(StringBuilder sb, String pad, String expr,
                                       String genericJavaType, int depth) {
        // No depth cutoff. One used to stop emitting below the fifth level while
        // build-time validation accepted the whole shape, so a declaration nested
        // deeper than that was checked partway and the rest reached the handler
        // unverified -- a 500 for what is a 400, at exactly the depth nobody
        // looks. The declaration is finite, so the recursion is too.
        if (genericJavaType != null && genericJavaType.startsWith("java.util.Map<")) {
            String value = mapBodyValueType(genericJavaType);
            if (value != null) {
                String raw = value.indexOf('<') < 0 ? value
                        : value.substring(0, value.indexOf('<'));
                String var = "v" + depth + "$";
                sb.append(pad).append("for (java.util.Iterator it").append(depth)
                  .append("$ = ").append(expr).append(".values().iterator(); it")
                  .append(depth).append("$.hasNext();) {\n");
                sb.append(pad).append("    Object ").append(var).append(" = it")
                  .append(depth).append("$.next();\n");
                sb.append(pad).append("    if (").append(var).append(" != null && !(")
                  .append(var).append(" instanceof ").append(raw).append(")) {\n");
                sb.append(pad).append("        return request.respond(400, "
                        + "\"text/plain; charset=utf-8\",\n");
                sb.append(pad).append("                utf8(")
                  .append(quote("A value of the request body is not a " + raw))
                  .append("));\n");
                sb.append(pad).append("    }\n");
                if (value.indexOf('<') >= 0 && depth < 32) {
                    sb.append(pad).append("    if (").append(var).append(" != null) {\n");
                    emitShapeChecks(sb, pad + "        ", "((" + raw + ")" + var + ")",
                            value, depth + 1);
                    sb.append(pad).append("    }\n");
                }
                sb.append(pad).append("}\n");
            }
            return;
        }
        emitElementChecks(sb, pad, expr, genericJavaType, depth);
    }

    /** A map body's declared value type, when it is one worth asserting. */
    private static String mapBodyValueType(String genericJavaType) {
        int lt = genericJavaType.indexOf('<');
        int end = genericJavaType.lastIndexOf('>');
        if (lt < 0 || end <= lt) {
            return null;
        }
        List<String> args = splitTypeArguments(genericJavaType.substring(lt + 1, end));
        if (args.size() != 2) {
            return null;
        }
        String value = args.get(1);
        String raw = value.indexOf('<') < 0 ? value : value.substring(0, value.indexOf('<'));
        return PARSED_JSON_TYPES.contains(raw) && !"java.lang.Object".equals(raw)
                ? value : null;
    }

    private static void emitElementChecks(StringBuilder sb, String pad, String expr,
                                          String genericJavaType, int depth) {
        String element = bodyElementType(genericJavaType);
        if (element == null) {
            return;                       // nothing declared to check
        }
        String var = "e" + depth + "$";
        String index = "i" + depth + "$";
        sb.append(pad).append("for (int ").append(index).append(" = 0; ").append(index)
          .append(" < ").append(expr).append(".size(); ").append(index).append("++) {\n");
        sb.append(pad).append("    Object ").append(var).append(" = ").append(expr)
          .append(".get(").append(index).append(");\n");
        String raw = element.indexOf('<') < 0 ? element
                : element.substring(0, element.indexOf('<'));
        sb.append(pad).append("    if (").append(var).append(" != null && !(").append(var)
          .append(" instanceof ").append(raw).append(")) {\n");
        sb.append(pad).append("        return request.respond(400, "
                + "\"text/plain; charset=utf-8\",\n");
        sb.append(pad).append("                utf8(")
          .append(quote("An element of the request body is not a " + raw)).append("));\n");
        sb.append(pad).append("    }\n");
        if (element.indexOf('<') >= 0 && depth < 32) {
            sb.append(pad).append("    if (").append(var).append(" != null) {\n");
            emitShapeChecks(sb, pad + "        ", "((" + raw + ")" + var + ")",
                    element, depth + 1);
            sb.append(pad).append("    }\n");
        }
        sb.append(pad).append("}\n");
    }

    /**
     * The element type of a declared List or Set body, when it is one the runtime
     * check can assert -- a type the parser produces, or another container whose
     * own elements can then be checked. Null for a raw container, a wildcard, or
     * anything else, where there is nothing to assert.
     */
    private static String bodyElementType(String genericJavaType) {
        if (genericJavaType == null) {
            return null;
        }
        int lt = genericJavaType.indexOf('<');
        int end = genericJavaType.lastIndexOf('>');
        if (lt < 0 || end <= lt) {
            return null;
        }
        String raw = genericJavaType.substring(0, lt);
        if (!"java.util.List".equals(raw) && !"java.util.Set".equals(raw)
                && !"java.util.Collection".equals(raw)) {
            return null;
        }
        List<String> args = splitTypeArguments(genericJavaType.substring(lt + 1, end));
        if (args.size() != 1) {
            return null;
        }
        String arg = args.get(0);
        // A nested container counts: its own elements are checked one level in.
        String argRaw = arg.indexOf('<') < 0 ? arg : arg.substring(0, arg.indexOf('<'));
        return PARSED_JSON_TYPES.contains(argRaw) && !"java.lang.Object".equals(argRaw)
                ? arg : null;
    }

    private static String argumentExpression(Param p) {
        if ("REQUEST".equals(p.kind)) {
            return "request";
        }
        String raw;
        if ("PATH".equals(p.kind)) {
            raw = "bound[" + p.variableIndex + "]";
        } else if ("QUERY".equals(p.kind)) {
            raw = "request.queryParam(" + quote(p.name) + ")";
        } else if ("HEADER".equals(p.kind)) {
            raw = "request.getHeader(" + quote(p.name) + ")";
        } else {
            return bodyExpression(p);
        }
        return convert(p.javaType, raw, p.defaultValue);
    }

    private static String bodyExpression(Param p) {
        if ("java.lang.String".equals(p.javaType)) {
            return "request.getBody()";
        }
        if (p.local != null) {
            return p.local;
        }
        if ("java.util.Map".equals(p.javaType)) {
            return "bodyAsMap(request.getBody())";
        }
        return "bodyAsList(request.getBody())";
    }

    /**
     * Converts a raw request value to the parameter's type.
     *
     * An absent value takes the annotation's default rather than throwing, which is
     * what Spring does and what a caller expects from `defaultValue`.
     */
    private static String convert(String javaType, String raw, String defaultValue) {
        String fallback = defaultValue == null ? "" : defaultValue;
        if ("java.lang.String".equals(javaType)) {
            return fallback.length() == 0 ? raw
                    : "orDefault(" + raw + ", " + quote(fallback) + ")";
        }
        String zero = "boolean".equals(javaType) ? "false" : "0";
        String literalDefault = fallback.length() == 0 ? zero
                : "to" + capitalize(javaType) + "(" + quote(fallback) + ", " + zero + ")";
        return "to" + capitalize(javaType) + "(" + raw + ", " + literalDefault + ")";
    }

    private static String capitalize(String javaType) {
        return Character.toUpperCase(javaType.charAt(0)) + javaType.substring(1);
    }

    /**
     * The helpers every router shares. Emitted into each router rather than into a
     * runtime class so that a module with no controllers links none of it, and so
     * the dead-code pass can drop whichever ones this controller never calls.
     */
    private static void emitRouterHelpers(StringBuilder sb) {
        sb.append("    private static final byte[] EMPTY = new byte[0];\n\n");
        sb.append("    /**\n");
        sb.append("     * Binds the path variables, or returns null when the rest of the path is\n");
        sb.append("     * not this route after all. `after[i]` is the literal that follows\n");
        sb.append("     * variable i, empty when the variable runs to the end.\n");
        sb.append("     */\n");
        sb.append("    private static String[] bindPath(String rest, String[] after) {\n");
        sb.append("        String[] out = new String[after.length];\n");
        sb.append("        return bindFrom(rest, after, 0, 0, out) ? out : null;\n");
        sb.append("    }\n\n");

        sb.append("    /**\n");
        sb.append("     * Tries every placement of the remaining literals, not just the first.\n");
        sb.append("     *\n");
        sb.append("     * A variable's value may contain the literal that follows it: matching\n");
        sb.append("     * /download/{name}.json against \"foo.json.json\" has to bind name to\n");
        sb.append("     * \"foo.json\", and taking the first occurrence bound it to \"foo\", left\n");
        sb.append("     * \".json\" unconsumed and rejected a request the route does match.\n");
        sb.append("     */\n");
        sb.append("    private static boolean bindFrom(String rest, String[] after, int i, int pos,\n");
        sb.append("            String[] out) {\n");
        sb.append("        if (i == after.length) {\n");
        sb.append("            return pos == rest.length();\n");
        sb.append("        }\n");
        sb.append("        String literal = after[i];\n");
        sb.append("        if (literal.length() == 0) {\n");
        sb.append("            String value = rest.substring(pos);\n");
        sb.append("            if (value.length() == 0 || value.indexOf('/') >= 0) {\n");
        sb.append("                return false;\n");
        sb.append("            }\n");
        sb.append("            out[i] = decode(value);\n");
        sb.append("            return i + 1 == after.length;\n");
        sb.append("        }\n");
        sb.append("        for (int at = rest.indexOf(literal, pos) ; at >= 0 ;\n");
        sb.append("                at = rest.indexOf(literal, at + 1)) {\n");
        sb.append("            String value = rest.substring(pos, at);\n");
        sb.append("            if (value.length() == 0) {\n");
        sb.append("                continue;\n");
        sb.append("            }\n");
        sb.append("            // A variable is one segment. Without this, /notes/{id} would\n");
        sb.append("            // match /notes/1/2 and hand the method \"1/2\" as the id. Every\n");
        sb.append("            // later occurrence spans this slash too, so stop rather than\n");
        sb.append("            // continue.\n");
        sb.append("            if (value.indexOf('/') >= 0) {\n");
        sb.append("                break;\n");
        sb.append("            }\n");
        sb.append("            out[i] = decode(value);\n");
        sb.append("            if (bindFrom(rest, after, i + 1, at + literal.length(), out)) {\n");
        sb.append("                return true;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return false;\n");
        sb.append("    }\n\n");

        sb.append("    /**\n");
        sb.append("     * Percent-decodes one path segment. The octets are gathered and decoded\n");
        sb.append("     * as a run: an escape carries one byte of UTF-8, and decoding them one\n");
        sb.append("     * at a time turns every non-ASCII value into mojibake.\n");
        sb.append("     */\n");
        sb.append("    private static String decode(String value) {\n");
        sb.append("        if (value.indexOf('%') < 0) {\n");
        sb.append("            return value;\n");
        sb.append("        }\n");
        sb.append("        byte[] out = new byte[value.length()];\n");
        sb.append("        int length = 0;\n");
        sb.append("        for (int i = 0 ; i < value.length() ; i++) {\n");
        sb.append("            char c = value.charAt(i);\n");
        sb.append("            if (c == '%' && i + 2 < value.length()) {\n");
        sb.append("                int hi = hex(value.charAt(i + 1));\n");
        sb.append("                int lo = hex(value.charAt(i + 2));\n");
        sb.append("                if (hi >= 0 && lo >= 0) {\n");
        sb.append("                    out[length++] = (byte)((hi << 4) | lo);\n");
        sb.append("                    i += 2;\n");
        sb.append("                    continue;\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("            out[length++] = (byte)c;\n");
        sb.append("        }\n");
        sb.append("        try {\n");
        sb.append("            return new String(out, 0, length, \"UTF-8\");\n");
        sb.append("        } catch (java.io.UnsupportedEncodingException err) {\n");
        sb.append("            return new String(out, 0, length);\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    /**\n");
        sb.append("     * Every % in a target must introduce two hex digits.\n");
        sb.append("     *\n");
        sb.append("     * A malformed escape used to decode as literal text, so /users/%ZZ\n");
        sb.append("     * reached the handler as those four characters -- and /users/%252F,\n");
        sb.append("     * a correctly escaped %2F, arrived as whatever a raw %2F would.\n");
        sb.append("     * Aliasing like that is how a check in front of a handler is passed\n");
        sb.append("     * by one spelling and defeated by another.\n");
        sb.append("     */\n");
        sb.append("    private static boolean wellFormedEscapes(String value) {\n");
        sb.append("        if (value == null) { return true; }\n");
        sb.append("        for (int i = 0 ; i < value.length() ; i++) {\n");
        sb.append("            if (value.charAt(i) != '%') { continue; }\n");
        sb.append("            if (i + 2 >= value.length()) { return false; }\n");
        sb.append("            if (hex(value.charAt(i + 1)) < 0 || hex(value.charAt(i + 2)) < 0) {\n");
        sb.append("                return false;\n");
        sb.append("            }\n");
        sb.append("            i += 2;\n");
        sb.append("        }\n");
        sb.append("        return true;\n");
        sb.append("    }\n\n");
        sb.append("    private static int hex(char c) {\n");
        sb.append("        if (c >= '0' && c <= '9') { return c - '0'; }\n");
        sb.append("        if (c >= 'a' && c <= 'f') { return c - 'a' + 10; }\n");
        sb.append("        if (c >= 'A' && c <= 'F') { return c - 'A' + 10; }\n");
        sb.append("        return -1;\n");
        sb.append("    }\n\n");

        sb.append("    private static byte[] utf8(String value) {\n");
        sb.append("        try {\n");
        sb.append("            return value.getBytes(\"UTF-8\");\n");
        sb.append("        } catch (java.io.UnsupportedEncodingException err) {\n");
        sb.append("            return value.getBytes();\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private static String orDefault(String value, String fallback) {\n");
        sb.append("        return value == null ? fallback : value;\n");
        sb.append("    }\n\n");

        // The numeric binders. A malformed value takes the default rather than
        // failing the request: a query string is user input, and 400 for "?page=x"
        // is a choice the handler should get to make.
        String[][] numeric = {
            {"Int", "int", "Integer.parseInt"},
            {"Long", "long", "Long.parseLong"},
            {"Double", "double", "Double.parseDouble"},
            {"Float", "float", "Float.parseFloat"},
            {"Short", "short", "Short.parseShort"},
            {"Byte", "byte", "Byte.parseByte"},
        };
        for (int i = 0; i < numeric.length; i++) {
            sb.append("    private static ").append(numeric[i][1]).append(" to")
              .append(numeric[i][0]).append("(String value, ").append(numeric[i][1])
              .append(" fallback) {\n");
            sb.append("        if (value == null || value.length() == 0) {\n");
            sb.append("            return fallback;\n");
            sb.append("        }\n");
            sb.append("        try {\n");
            sb.append("            return ").append(numeric[i][2]).append("(value.trim());\n");
            sb.append("        } catch (NumberFormatException err) {\n");
            sb.append("            return fallback;\n");
            sb.append("        }\n");
            sb.append("    }\n\n");
            // The companion the guard uses. to<Type> answers the fallback for a
            // value that is absent AND for one that is malformed, which is exactly
            // the distinction a caller needs to make: defaultValue is documented as
            // "used when the request omits it", and "zz" is not an omission.
            sb.append("    private static boolean parses").append(numeric[i][0])
              .append("(String value) {\n");
            // ABSENT is fine; present and EMPTY is not. "?count=" is a parameter
            // the client sent, and queryParam distinguishes it from one that was
            // omitted -- so treating the two alike let an empty value take the
            // default or zero and call the handler with a number nobody sent,
            // which is the same defect as accepting "zz". A default is documented
            // as "used when the request omits it", and this is not an omission.
            sb.append("        if (value == null) {\n");
            sb.append("            return true;\n");
            sb.append("        }\n");
            sb.append("        if (value.length() == 0) {\n");
            sb.append("            return false;\n");
            sb.append("        }\n");
            sb.append("        try {\n");
            if ("Double".equals(numeric[i][0])) {
                // parseDouble does not fail on a value too large for a double
                // either: 1e999 comes back as infinity. The handler then runs on
                // an infinite amount, and if it is written back out Json turns it
                // into null, so the client is answered with neither its value nor
                // an error. Float was fixed for this and Double left, which is the
                // same defect one type over.
                sb.append("            double asDouble = Double.parseDouble(value.trim());\n");
                sb.append("            return !Double.isInfinite(asDouble)"
                        + " || value.trim().indexOf(\"Infinity\") >= 0;\n");
            } else if ("Float".equals(numeric[i][0])) {
                // Float.parseFloat does not FAIL on a value too large for a
                // float: it answers infinity, so 1e100 passed this guard and the
                // handler ran on a number the client never sent. Every other
                // width throws. An input that really spells an infinity is still
                // accepted, which is what parseFloat means by it.
                // The SPELLING decides whether an infinity was meant, not the
                // parsed value: Double.parseDouble("1e999") is itself infinite,
                // so testing the parsed double declared every double-overflowing
                // value to be a deliberate infinity and handed it on. The double
                // guard above already tests the text; these two now agree.
                sb.append("            double asDouble = Double.parseDouble(value.trim());\n");
                sb.append("            return !Float.isInfinite((float)asDouble)"
                        + " || value.trim().indexOf(\"Infinity\") >= 0;\n");
            } else {
                sb.append("            ").append(numeric[i][2]).append("(value.trim());\n");
                sb.append("            return true;\n");
            }
            sb.append("        } catch (NumberFormatException err) {\n");
            sb.append("            return false;\n");
            sb.append("        }\n");
            sb.append("    }\n\n");
        }
        sb.append("    private static boolean toBoolean(String value, boolean fallback) {\n");
        sb.append("        if (value == null || value.length() == 0) {\n");
        sb.append("            return fallback;\n");
        sb.append("        }\n");
        sb.append("        // Case folding a token with toLowerCase() is locale sensitive and\n");
        sb.append("        // wrong on a Turkish device; equalsIgnoreCase is not.\n");
        sb.append("        return value.equalsIgnoreCase(\"true\") || value.equals(\"1\")\n");
        sb.append("                || value.equalsIgnoreCase(\"yes\") || value.equalsIgnoreCase(\"on\");\n");
        sb.append("    }\n\n");

        // toBoolean answers false for everything it does not recognise, so
        // "?enabled=treu" reached the handler as an explicit false and neither
        // side could tell -- while the same typo in a numeric binding is a 400.
        // The permissive spellings stay; what is refused is a value that is
        // neither true nor false in any of them. The @RestClient half refuses
        // its own malformed booleans, and the two generators disagreeing about
        // the same request is its own bug.
        // Empty is not false, for the reason the numeric guards already give:
        // "?enabled=" is a parameter the client SENT, and binding it to false
        // hands the controller a decision nobody made. Only an absent value
        // takes the default.
        sb.append("    private static boolean parsesBoolean(String value) {\n");
        sb.append("        if (value == null) {\n");
        sb.append("            return true;\n");
        sb.append("        }\n");
        sb.append("        if (value.length() == 0) {\n");
        sb.append("            return false;\n");
        sb.append("        }\n");
        sb.append("        return value.equalsIgnoreCase(\"true\") || value.equals(\"1\")\n");
        sb.append("                || value.equalsIgnoreCase(\"yes\") || value.equalsIgnoreCase(\"on\")\n");
        sb.append("                || value.equalsIgnoreCase(\"false\") || value.equals(\"0\")\n");
        sb.append("                || value.equalsIgnoreCase(\"no\") || value.equalsIgnoreCase(\"off\");\n");
        sb.append("    }\n\n");

        sb.append("    private static java.util.Map bodyAsMap(String body) {\n");
        sb.append("        if (body == null || body.length() == 0) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        try {\n");
        sb.append("            return com.codename1.backend.Json.parseObject(body);\n");
        sb.append("        } catch (java.io.IOException err) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        sb.append("    private static java.util.List bodyAsList(String body) {\n");
        sb.append("        if (body == null || body.length() == 0) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        try {\n");
        sb.append("            Object parsed = com.codename1.backend.Json.parse(body);\n");
        sb.append("            // Never a cast: ParparVM's CHECKCAST is unchecked, so a wrong\n");
        sb.append("            // one reads the next instruction's fields out of the wrong\n");
        sb.append("            // object instead of throwing.\n");
        sb.append("            return parsed instanceof java.util.List ? (java.util.List)parsed : null;\n");
        sb.append("        } catch (java.io.IOException err) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("    }\n");
    }

    /**
     * The `main` the developer no longer writes.
     *
     * This is the half of the old API that was an implementation detail wearing a
     * user's clothes: every server opened with the same twenty lines -- read PORT,
     * start, install a shutdown handler, await termination -- and getting any of
     * them wrong produced a server that leaked connections on SIGTERM or exited
     * silently the moment main returned. Generating it means the controller is the
     * only thing anyone writes, and the lifecycle is the same in every project.
     */
    private String generateBootstrap(String packageName) {
        StringBuilder sb = new StringBuilder();
        if (packageName.length() > 0) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
        sb.append("// Generated from the @RestController classes in this module. Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class BackendApplication {\n\n");
        sb.append("    private BackendApplication() {\n    }\n\n");
        sb.append("    public static void main(String[] args) throws Exception {\n");
        sb.append("        com.codename1.backend.Signals.installShutdownHandler();\n");
        sb.append("        int port = 8080;\n");
        sb.append("        String configured = System.getenv(\"PORT\");\n");
        sb.append("        if (configured != null && configured.length() > 0) {\n");
        sb.append("            try {\n");
        sb.append("                port = Integer.parseInt(configured.trim());\n");
        sb.append("            } catch (NumberFormatException err) {\n");
        sb.append("                throw new IllegalStateException(\"PORT is not a number: \"\n");
        sb.append("                        + configured);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        final com.codename1.backend.HttpServer.Handler[] routers =\n");
        sb.append("                new com.codename1.backend.HttpServer.Handler[] {\n");
        int index = 0;
        for (Controller c : controllers.values()) {
            sb.append("                    new ").append(qualify(c.packageName, c.routerSimpleName))
              .append("(new ").append(c.binaryName).append("())");
            sb.append(++index < controllers.size() ? ",\n" : "\n");
        }
        sb.append("                };\n");
        sb.append("        final com.codename1.backend.HttpServer server =\n");
        sb.append("                com.codename1.backend.HttpServer.start(null, port, 512, 16,\n");
        sb.append("                new com.codename1.backend.HttpServer.Handler() {\n");
        sb.append("            public com.codename1.backend.HttpServer.Response handle(\n");
        sb.append("                    com.codename1.backend.HttpServer.Request request)\n");
        sb.append("                    throws Exception {\n");
        sb.append("                for (int i = 0 ; i < routers.length ; i++) {\n");
        sb.append("                    com.codename1.backend.HttpServer.Response response =\n");
        sb.append("                            routers[i].handle(request);\n");
        sb.append("                    if (response != null) {\n");
        sb.append("                        return response;\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("                return null;\n");
        sb.append("            }\n");
        sb.append("        }, null);\n");
        sb.append("        com.codename1.backend.Signals.onShutdown(new Runnable() {\n");
        sb.append("            public void run() {\n");
        sb.append("                // Stop accepting and let what is in flight finish.\n");
        sb.append("                // Signals ends the process; exiting from here would\n");
        sb.append("                // deadlock the JVM shutdown hook this runs from.\n");
        sb.append("                server.stop(10000);\n");
        sb.append("            }\n");
        sb.append("        });\n");
        sb.append("        // Required: the host threads are detached, so a main that returned\n");
        sb.append("        // would end the process without a word.\n");
        sb.append("        server.awaitTermination();\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /** A route as a byte[] constant, which is what the request is compared against. */
    private static String byteArrayLiteral(String value) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7f) {
                // A route pattern is written in source, and a non-ASCII one would have
                // to be compared against its percent-encoded form on the wire.
                throw new IllegalArgumentException("Route patterns must be ASCII: " + value);
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append((int) c);
        }
        return sb.append("}").toString();
    }

    private static String stringArrayLiteral(List<String> values) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(values.get(i)));
        }
        return sb.append("}").toString();
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c < 0x20 || c > 0x7e) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}

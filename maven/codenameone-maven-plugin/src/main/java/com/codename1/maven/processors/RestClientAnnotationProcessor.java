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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.objectweb.asm.Type;

/// Build-time `@RestClient` processor. Scans the project's compiled classes
/// for `@RestClient`-annotated interfaces, validates them (interface, public,
/// every abstract method carries exactly one HTTP-verb annotation, each
/// parameter carries at most one binding annotation), and emits:
///
/// 1. One `<SimpleName>Impl` Java class per `@RestClient` interface in the
///    **same package** as the source interface. The impl chains
///    `com.codename1.io.rest.Rest.<verb>(baseUrl + path)` with query / header
///    / body builders and finishes with `fetchAsMapped` /
///    `fetchAsMappedList` / `fetchAsString` based on the
///    `OnComplete<Response<T>>` callback parameter's generic type.
/// 2. A single `cn1app.RestClientBootstrap` whose no-arg constructor calls
///    `com.codename1.io.rest.RestClients.register(FooApi.class, new
///    Factory<FooApi>(){...})` for every accepted interface. The build server
///    probes the project zip for this class and splices
///    `new cn1app.RestClientBootstrap();` into the iOS / Android per-build
///    application stub before `Display.init`, just like the existing
///    `cn1app.MapperBootstrap`.
public final class RestClientAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String REST_CLIENT_DESC = "Lcom/codename1/annotations/rest/RestClient;";

    public static final String GET_DESC = "Lcom/codename1/annotations/rest/GET;";
    public static final String POST_DESC = "Lcom/codename1/annotations/rest/POST;";
    public static final String PUT_DESC = "Lcom/codename1/annotations/rest/PUT;";
    public static final String DELETE_DESC = "Lcom/codename1/annotations/rest/DELETE;";
    public static final String PATCH_DESC = "Lcom/codename1/annotations/rest/PATCH;";

    public static final String PATH_DESC = "Lcom/codename1/annotations/rest/Path;";
    public static final String QUERY_DESC = "Lcom/codename1/annotations/rest/Query;";
    public static final String HEADER_DESC = "Lcom/codename1/annotations/rest/Header;";
    public static final String COOKIE_DESC = "Lcom/codename1/annotations/rest/Cookie;";
    public static final String BODY_DESC = "Lcom/codename1/annotations/rest/Body;";

    static final String BOOTSTRAP_BINARY = "cn1app.RestClientBootstrap";
    static final String BOOTSTRAP_SIMPLE = "RestClientBootstrap";
    static final String BOOTSTRAP_PACKAGE = "cn1app";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(REST_CLIENT_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    /// Accepted interfaces keyed by binary name. TreeMap so the emitted
    /// bootstrap registration order is deterministic regardless of scan order.
    private final TreeMap<String, RestApi> accepted = new TreeMap<String, RestApi>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.isSynthetic()) return;
        if (cls.getClassAnnotation(REST_CLIENT_DESC) == null) return;
        if (!cls.isInterface()) {
            ctx.error(cls, "@RestClient requires an interface; "
                    + cls.getBinaryName() + " is not an interface");
            return;
        }
        if (!cls.isPublic()) {
            ctx.error(cls, "@RestClient interface " + cls.getBinaryName()
                    + " must be public");
            return;
        }

        RestApi api = new RestApi();
        api.binaryName = cls.getBinaryName();
        api.simpleName = simpleName(api.binaryName);
        api.packageName = packageOf(api.binaryName);
        api.implSimpleName = api.simpleName + "Impl";
        api.implBinaryName = api.packageName.length() == 0
                ? api.implSimpleName
                : api.packageName + "." + api.implSimpleName;

        boolean anyError = false;
        for (MethodInfo m : cls.getMethods()) {
            if (m.isStatic()) continue;        // static `of()` factory
            if (m.isSynthetic()) continue;
            if (m.isConstructor()) continue;   // can't happen on interface, defensive
            if ((m.getAccess() & org.objectweb.asm.Opcodes.ACC_BRIDGE) != 0) continue;
            // Default methods on the interface aren't ours to implement.
            if (!m.isAbstract()) continue;

            RestMethod rm = new RestMethod();
            rm.name = m.getName();
            rm.descriptor = m.getDescriptor();
            rm.signature = m.getSignature();

            String verb = null;
            String pathTemplate = null;
            int verbCount = 0;
            AnnotationValues va;
            if ((va = m.getAnnotation(GET_DESC)) != null) { verb = "get";    pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(POST_DESC)) != null) { verb = "post";   pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(PUT_DESC)) != null) { verb = "put";    pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(DELETE_DESC)) != null) { verb = "delete"; pathTemplate = va.getString("value"); verbCount++; }
            if ((va = m.getAnnotation(PATCH_DESC)) != null) { verb = "patch";  pathTemplate = va.getString("value"); verbCount++; }
            if (verbCount == 0) {
                ctx.error(cls, "@RestClient method " + api.binaryName + "." + rm.name
                        + " must carry exactly one of @GET/@POST/@PUT/@DELETE/@PATCH");
                anyError = true;
                continue;
            }
            if (verbCount > 1) {
                ctx.error(cls, "@RestClient method " + api.binaryName + "." + rm.name
                        + " carries multiple HTTP-verb annotations; pick one");
                anyError = true;
                continue;
            }
            if (pathTemplate == null) pathTemplate = "";
            rm.verb = verb;
            rm.pathTemplate = pathTemplate;

            Type[] paramTypes = Type.getArgumentTypes(rm.descriptor);
            List<Map<String, AnnotationValues>> paramAnnotations = m.getParameterAnnotations();
            int paramCount = paramTypes.length;
            // Parse generic param signature for the callback's response payload.
            String[] genericParamSigs = parseGenericParameterSignatures(rm.signature, paramCount);

            int bodyCount = 0;
            int callbackIndex = -1;
            boolean methodHasError = false;

            for (int i = 0; i < paramCount; i++) {
                RestParam rp = new RestParam();
                rp.index = i;
                rp.descriptor = paramTypes[i].getDescriptor();
                rp.javaType = javaTypeFor(paramTypes[i], genericParamSigs == null ? null : genericParamSigs[i]);
                rp.name = "p" + i;

                Map<String, AnnotationValues> pa = i < paramAnnotations.size()
                        ? paramAnnotations.get(i) : null;

                int annoCount = 0;
                AnnotationValues path = null, query = null, header = null, cookie = null;
                boolean body = false;
                if (pa != null) {
                    if ((path = pa.get(PATH_DESC)) != null) annoCount++;
                    if ((query = pa.get(QUERY_DESC)) != null) annoCount++;
                    if ((header = pa.get(HEADER_DESC)) != null) annoCount++;
                    if ((cookie = pa.get(COOKIE_DESC)) != null) annoCount++;
                    if (pa.get(BODY_DESC) != null) { body = true; annoCount++; }
                }
                if (annoCount > 1) {
                    ctx.error(cls, "Parameter " + i + " of "
                            + api.binaryName + "." + rm.name
                            + " carries multiple REST binding annotations; pick one");
                    anyError = true;
                    methodHasError = true;
                    break;
                }

                if (path != null) {
                    rp.kind = ParamKind.PATH;
                    rp.bindingName = path.getString("value");
                    if (rp.bindingName == null) rp.bindingName = rp.name;
                    rp.name = sanitizeIdentifier(rp.bindingName);
                } else if (query != null) {
                    rp.kind = ParamKind.QUERY;
                    rp.bindingName = query.getString("value");
                    if (rp.bindingName == null) rp.bindingName = "p" + i;
                    rp.name = sanitizeIdentifier(rp.bindingName);
                } else if (header != null) {
                    rp.kind = ParamKind.HEADER;
                    rp.bindingName = header.getString("value");
                    if (rp.bindingName == null) rp.bindingName = "p" + i;
                    rp.name = sanitizeIdentifier(rp.bindingName + "Header");
                } else if (cookie != null) {
                    rp.kind = ParamKind.COOKIE;
                    rp.bindingName = cookie.getString("value");
                    if (rp.bindingName == null) rp.bindingName = "p" + i;
                    rp.name = sanitizeIdentifier(rp.bindingName + "Cookie");
                } else if (body) {
                    rp.kind = ParamKind.BODY;
                    rp.name = "body";
                    bodyCount++;
                } else if (isCallbackType(rp.descriptor)) {
                    rp.kind = ParamKind.CALLBACK;
                    rp.name = "callback";
                    callbackIndex = i;
                    rp.javaType = "com.codename1.util.OnComplete<com.codename1.io.rest.Response<"
                            + boxIfPrimitive(extractResponsePayload(genericParamSigs == null ? null : genericParamSigs[i]))
                            + ">>";
                } else {
                    ctx.error(cls, "Parameter " + i + " of "
                            + api.binaryName + "." + rm.name
                            + " has no REST binding annotation and is not an OnComplete callback");
                    anyError = true;
                    methodHasError = true;
                    break;
                }

                rm.params.add(rp);
            }
            if (methodHasError) continue;
            if (bodyCount > 1) {
                ctx.error(cls, "@RestClient method " + api.binaryName + "." + rm.name
                        + " declares more than one @Body parameter");
                anyError = true;
                continue;
            }
            rm.callbackIndex = callbackIndex;

            // Decide payload-fetch shape.
            if (callbackIndex >= 0) {
                String payloadSig = genericParamSigs == null ? null : genericParamSigs[callbackIndex];
                String payload = extractResponsePayload(payloadSig);
                if (payload.startsWith("java.util.List<")) {
                    rm.fetchKind = FetchKind.MAPPED_LIST;
                    rm.payloadElementBinaryName = stripGeneric(payload.substring("java.util.List<".length(),
                            payload.length() - 1));
                } else if ("java.lang.String".equals(payload)) {
                    rm.fetchKind = FetchKind.STRING;
                } else {
                    rm.fetchKind = FetchKind.MAPPED;
                    rm.payloadBinaryName = stripGeneric(payload);
                }
            } else {
                rm.fetchKind = FetchKind.STRING; // void w/ no callback
            }

            api.methods.add(rm);
        }

        if (!anyError) {
            accepted.put(api.binaryName, api);
        }
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) return;
        if (accepted.isEmpty()) return;

        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (RestApi api : accepted.values()) {
            sources.put(api.implBinaryName, generateImplSource(api));
        }
        sources.put(BOOTSTRAP_BINARY, generateBootstrapSource(accepted.values()));

        try {
            List<java.io.File> cp = new ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated @RestClient sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size()
                + " @RestClient impl(s) + " + BOOTSTRAP_BINARY);
    }

    // ----------------------------------------------------------------
    // Source generation
    // ----------------------------------------------------------------

    private static String generateImplSource(RestApi api) {
        StringBuilder sb = new StringBuilder(4096);
        if (api.packageName.length() > 0) {
            sb.append("package ").append(api.packageName).append(";\n\n");
        }
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(api.implSimpleName)
                .append(" implements ").append(api.binaryName).append(" {\n\n");
        sb.append("    private final String baseUrl;\n\n");
        sb.append("    public ").append(api.implSimpleName).append("(String baseUrl) {\n");
        sb.append("        this.baseUrl = baseUrl;\n");
        sb.append("    }\n\n");

        for (RestMethod rm : api.methods) {
            emitMethod(sb, rm);
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static void emitMethod(StringBuilder sb, RestMethod rm) {
        // Signature
        sb.append("    public void ").append(rm.name).append("(");
        boolean first = true;
        for (RestParam p : rm.params) {
            if (!first) sb.append(", ");
            sb.append(p.javaType).append(" ").append(p.name);
            first = false;
        }
        sb.append(") {\n");

        // URL: path-template substitution.
        sb.append("        String _url = baseUrl + ");
        appendPathExpression(sb, rm);
        sb.append(";\n");

        sb.append("        com.codename1.io.rest.RequestBuilder _rb = com.codename1.io.rest.Rest.")
                .append(rm.verb).append("(_url);\n");
        emitExceptionHandler(sb, rm);

        // Query params.
        for (RestParam p : rm.params) {
            if (p.kind != ParamKind.QUERY) continue;
            sb.append("        if (").append(p.name).append(" != null) _rb.queryParam(\"")
                    .append(escape(p.bindingName)).append("\", String.valueOf(")
                    .append(p.name).append("));\n");
        }
        // Header params.
        for (RestParam p : rm.params) {
            if (p.kind != ParamKind.HEADER) continue;
            sb.append("        if (").append(p.name).append(" != null) _rb.header(\"")
                    .append(escape(p.bindingName)).append("\", ").append(p.name).append(");\n");
        }
        // Cookie params: collapsed into a single `Cookie: a=1; b=2` header.
        boolean anyCookie = false;
        for (RestParam p : rm.params) {
            if (p.kind == ParamKind.COOKIE) { anyCookie = true; break; }
        }
        if (anyCookie) {
            sb.append("        StringBuilder _ck = new StringBuilder();\n");
            for (RestParam p : rm.params) {
                if (p.kind != ParamKind.COOKIE) continue;
                sb.append("        if (").append(p.name).append(" != null) {\n");
                sb.append("            if (_ck.length() > 0) _ck.append(\"; \");\n");
                sb.append("            _ck.append(\"").append(escape(p.bindingName))
                        .append("=\").append(com.codename1.io.Util.encodeUrl(String.valueOf(")
                        .append(p.name).append(")));\n");
                sb.append("        }\n");
            }
            sb.append("        if (_ck.length() > 0) _rb.header(\"Cookie\", _ck.toString());\n");
        }
        // Body.
        for (RestParam p : rm.params) {
            if (p.kind != ParamKind.BODY) continue;
            sb.append("        _rb.contentType(\"application/json\").body(com.codename1.mapping.Mappers.toJson(")
                    .append(p.name).append("));\n");
        }

        // Fetch.
        emitErrorCodeHandler(sb, rm);
        switch (rm.fetchKind) {
            case MAPPED:
                sb.append("        _rb.fetchAsMapped(").append(rm.payloadBinaryName).append(".class, callback);\n");
                break;
            case MAPPED_LIST:
                sb.append("        _rb.fetchAsMappedList(").append(rm.payloadElementBinaryName).append(".class, callback);\n");
                break;
            case STRING:
                if (rm.callbackIndex >= 0) {
                    sb.append("        _rb.fetchAsString(callback);\n");
                } else {
                    sb.append("        _rb.fetchAsString(new com.codename1.util.OnComplete<com.codename1.io.rest.Response<String>>() {\n");
                    sb.append("            public void completed(com.codename1.io.rest.Response<String> _r) { }\n");
                    sb.append("        });\n");
                }
                break;
        }
        sb.append("    }\n\n");
    }

    private static void emitErrorCodeHandler(StringBuilder sb, RestMethod rm) {
        if (rm.callbackIndex >= 0) {
            sb.append("        _rb.onErrorCodeString(new com.codename1.io.rest.ErrorCodeHandler<String>() {\n");
            sb.append("            public void onError(com.codename1.io.rest.Response<String> _r) {\n");
            sb.append("                callback.completed((com.codename1.io.rest.Response)_r);\n");
            sb.append("            }\n");
            sb.append("        });\n");
            return;
        }
        sb.append("        _rb.onErrorCodeString(new com.codename1.io.rest.ErrorCodeHandler<String>() {\n");
        sb.append("            public void onError(com.codename1.io.rest.Response<String> _r) { }\n");
        sb.append("        });\n");
    }

    private static void emitExceptionHandler(StringBuilder sb, RestMethod rm) {
        if (rm.callbackIndex >= 0) {
            sb.append("        _rb.onError(new com.codename1.ui.events.ActionListener<com.codename1.io.NetworkEvent>() {\n");
            sb.append("            public void actionPerformed(com.codename1.io.NetworkEvent _evt) {\n");
            sb.append("                _evt.consume();\n");
            sb.append("                callback.completed(null);\n");
            sb.append("            }\n");
            sb.append("        }, false);\n");
            return;
        }
        sb.append("        _rb.onError(new com.codename1.ui.events.ActionListener<com.codename1.io.NetworkEvent>() {\n");
        sb.append("            public void actionPerformed(com.codename1.io.NetworkEvent _evt) { _evt.consume(); }\n");
        sb.append("        }, false);\n");
    }

    /// Builds the Java expression that resolves the URL path with `{name}`
    /// placeholders replaced by the matching `@Path` parameter values
    /// (URL-encoded). The result is concatenated onto `baseUrl`.
    private static void appendPathExpression(StringBuilder sb, RestMethod rm) {
        String path = rm.pathTemplate;
        if (path == null) path = "";
        // Split on {name} placeholders and replace each with a concat against the
        // matching @Path parameter.
        StringBuilder cur = new StringBuilder();
        List<String> parts = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        int i = 0;
        while (i < path.length()) {
            char c = path.charAt(i);
            if (c == '{') {
                int end = path.indexOf('}', i);
                if (end < 0) { cur.append(c); i++; continue; }
                parts.add(cur.toString()); cur.setLength(0);
                names.add(path.substring(i + 1, end));
                i = end + 1;
            } else {
                cur.append(c);
                i++;
            }
        }
        parts.add(cur.toString());

        if (names.isEmpty()) {
            sb.append('"').append(escape(parts.get(0))).append('"');
            return;
        }
        boolean first = true;
        for (int p = 0; p < parts.size(); p++) {
            String literal = parts.get(p);
            if (literal.length() > 0 || (first && p == 0)) {
                if (!first) sb.append(" + ");
                sb.append('"').append(escape(literal)).append('"');
                first = false;
            }
            if (p < names.size()) {
                String placeholder = names.get(p);
                String paramName = findPathParamName(rm, placeholder);
                if (!first) sb.append(" + ");
                sb.append("String.valueOf(").append(paramName).append(")");
                first = false;
            }
        }
    }

    private static String findPathParamName(RestMethod rm, String placeholder) {
        for (RestParam p : rm.params) {
            if (p.kind == ParamKind.PATH && placeholder.equals(p.bindingName)) {
                return p.name;
            }
        }
        // Fall back to a no-op constant so generated source still compiles --
        // upstream validation should already have caught the missing @Path.
        return "\"" + escape(placeholder) + "\"";
    }

    private static String generateBootstrapSource(Iterable<RestApi> apis) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(BOOTSTRAP_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("///\n");
        sb.append("/// REST-client bootstrap. The iOS / Android per-build application\n");
        sb.append("/// stub instantiates this class before Display.init (the build\n");
        sb.append("/// server probes the project zip for it and emits the install\n");
        sb.append("/// line conditionally); JavaSEPort.postInit picks it up via\n");
        sb.append("/// Class.forName for the simulator and desktop runs.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(BOOTSTRAP_SIMPLE).append(" {\n");
        sb.append("    public ").append(BOOTSTRAP_SIMPLE).append("() {\n");
        for (RestApi api : apis) {
            sb.append("        com.codename1.io.rest.RestClients.register(")
                    .append(api.binaryName).append(".class, new com.codename1.io.rest.RestClients.Factory<")
                    .append(api.binaryName).append(">() {\n");
            sb.append("            public ").append(api.binaryName).append(" create(String baseUrl) {\n");
            sb.append("                return new ").append(api.implBinaryName).append("(baseUrl);\n");
            sb.append("            }\n");
            sb.append("        });\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Signature parsing
    // ----------------------------------------------------------------

    /// Splits the parameter portion of a JVM generic method signature into one
    /// substring per parameter. Returns null when no signature is available
    /// (raw method) so callers fall back to the descriptor.
    static String[] parseGenericParameterSignatures(String signature, int expectedCount) {
        if (signature == null) return null;
        int open = signature.indexOf('(');
        int close = matchingParen(signature, open);
        if (open < 0 || close < 0) return null;
        String params = signature.substring(open + 1, close);
        List<String> out = new ArrayList<String>();
        int i = 0;
        while (i < params.length()) {
            int start = i;
            char c = params.charAt(i);
            // skip array prefix
            while (c == '[') { i++; if (i >= params.length()) break; c = params.charAt(i); }
            if (c == 'L' || c == 'T') {
                i = skipReferenceTypeSignature(params, i);
            } else {
                // primitive: single char
                i++;
            }
            out.add(params.substring(start, i));
        }
        if (out.size() != expectedCount) return null;
        return out.toArray(new String[0]);
    }

    private static int skipReferenceTypeSignature(String s, int i) {
        // L<name>(...generic args...);  OR  T<name>;
        char c = s.charAt(i);
        if (c == 'T') {
            int semi = s.indexOf(';', i);
            return semi < 0 ? s.length() : semi + 1;
        }
        // Walk forward counting < > nesting so we find the matching ';' on
        // the *outer* type. JVM signatures spell nested generics inline so
        // `Lcom/foo/Bar<TT;Ljava/util/List<Ljava/lang/String;>;>;` is one
        // signature; tracking the depth keeps us from stopping at the inner
        // `;` characters.
        int depth = 0;
        i++; // past 'L'
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (ch == '<') depth++;
            else if (ch == '>') depth--;
            else if (ch == ';' && depth == 0) return i + 1;
            i++;
        }
        return s.length();
    }

    private static int matchingParen(String s, int open) {
        if (open < 0) return -1;
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    /// Returns the Java (binary) form of the payload type carried by the
    /// callback parameter. Given the parameter's generic signature
    /// `Lcom/codename1/util/OnComplete<Lcom/codename1/io/rest/Response<Lpet/Pet;>;>;`
    /// this returns `pet.Pet`. Returns `java.lang.Object` when the signature
    /// can't be parsed.
    static String extractResponsePayload(String paramSignature) {
        if (paramSignature == null) return "java.lang.Object";
        // Find the OnComplete generic arg.
        int lt = paramSignature.indexOf('<');
        int gt = paramSignature.lastIndexOf('>');
        if (lt < 0 || gt < 0 || lt > gt) return "java.lang.Object";
        String inner = paramSignature.substring(lt + 1, gt); // Lcom/codename1/io/rest/Response<...>;
        // Now find the Response's inner type arg.
        int innerLt = inner.indexOf('<');
        int innerGt = inner.lastIndexOf('>');
        if (innerLt < 0 || innerGt < 0 || innerLt > innerGt) return "java.lang.Object";
        String payload = inner.substring(innerLt + 1, innerGt);
        return jvmSignatureToJavaType(payload);
    }

    private static String jvmSignatureToJavaType(String sig) {
        if (sig == null || sig.length() == 0) return "java.lang.Object";
        char c = sig.charAt(0);
        switch (c) {
            case 'V': return "void";
            case 'B': return "byte";
            case 'C': return "char";
            case 'D': return "double";
            case 'F': return "float";
            case 'I': return "int";
            case 'J': return "long";
            case 'S': return "short";
            case 'Z': return "boolean";
            case '[':
                return jvmSignatureToJavaType(sig.substring(1)) + "[]";
            case 'L':
                // Lpkg/Class<...args>;
                int end = sig.indexOf('<');
                int semi = sig.indexOf(';');
                if (end < 0 || (semi >= 0 && semi < end)) {
                    // No generic args.
                    String binary = sig.substring(1, semi >= 0 ? semi : sig.length() - 1).replace('/', '.');
                    return binary;
                }
                String rawBin = sig.substring(1, end).replace('/', '.');
                // Find matching '>'
                int depth = 0;
                int gt = -1;
                for (int i = end; i < sig.length(); i++) {
                    char ch = sig.charAt(i);
                    if (ch == '<') depth++;
                    else if (ch == '>') { depth--; if (depth == 0) { gt = i; break; } }
                }
                if (gt < 0) return rawBin;
                String args = sig.substring(end + 1, gt);
                // Split args at top level.
                List<String> argList = splitTopLevelArgs(args);
                StringBuilder sb = new StringBuilder(rawBin);
                sb.append('<');
                for (int i = 0; i < argList.size(); i++) {
                    if (i > 0) sb.append(", ");
                    String a = argList.get(i);
                    if (a.startsWith("*")) {
                        sb.append("?");
                    } else if (a.startsWith("+")) {
                        sb.append("? extends ").append(boxIfPrimitive(jvmSignatureToJavaType(a.substring(1))));
                    } else if (a.startsWith("-")) {
                        sb.append("? super ").append(boxIfPrimitive(jvmSignatureToJavaType(a.substring(1))));
                    } else {
                        sb.append(boxIfPrimitive(jvmSignatureToJavaType(a)));
                    }
                }
                sb.append('>');
                return sb.toString();
            case 'T':
                // Type variable -- erased to its name; we treat as Object.
                return "java.lang.Object";
            default:
                return "java.lang.Object";
        }
    }

    private static List<String> splitTopLevelArgs(String args) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        while (i < args.length()) {
            int start = i;
            char c = args.charAt(i);
            if (c == '*') { out.add("*"); i++; continue; }
            if (c == '+' || c == '-') { i++; if (i >= args.length()) break; c = args.charAt(i); }
            while (c == '[') { i++; if (i >= args.length()) break; c = args.charAt(i); }
            if (c == 'L' || c == 'T') {
                i = skipReferenceTypeSignature(args, i);
            } else {
                i++;
            }
            out.add(args.substring(start, i));
        }
        return out;
    }

    /// Strips top-level generic parameters from a Java type name so it can be
    /// used as a `Class<T>` literal. `List<Pet>` -> `List`.
    private static String stripGeneric(String javaType) {
        if (javaType == null) return "java.lang.Object";
        int lt = javaType.indexOf('<');
        return lt < 0 ? javaType : javaType.substring(0, lt);
    }

    private static boolean isCallbackType(String descriptor) {
        return "Lcom/codename1/util/OnComplete;".equals(descriptor);
    }

    /// Returns the Java type name for a parameter, preferring the generic
    /// signature when available so `List<Pet>` survives instead of erasing to
    /// `List`.
    private static String javaTypeFor(Type asmType, String genericSig) {
        if (genericSig != null && genericSig.length() > 0) {
            return jvmSignatureToJavaType(genericSig);
        }
        // Erase to the descriptor form.
        return jvmSignatureToJavaType(asmType.getDescriptor());
    }

    private static String boxIfPrimitive(String type) {
        if (type == null) return "java.lang.Object";
        if (type.equals("int")) return "java.lang.Integer";
        if (type.equals("long")) return "java.lang.Long";
        if (type.equals("double")) return "java.lang.Double";
        if (type.equals("float")) return "java.lang.Float";
        if (type.equals("boolean")) return "java.lang.Boolean";
        if (type.equals("byte")) return "java.lang.Byte";
        if (type.equals("short")) return "java.lang.Short";
        if (type.equals("char")) return "java.lang.Character";
        return type;
    }

    // ----------------------------------------------------------------
    // Misc
    // ----------------------------------------------------------------

    private static String packageOf(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? "" : binary.substring(0, dot);
    }

    private static String simpleName(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? binary : binary.substring(dot + 1);
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.toString();
    }

    private static String sanitizeIdentifier(String s) {
        if (s == null || s.length() == 0) return "p";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == 0 ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c)) {
                b.append(c);
            }
        }
        if (b.length() == 0) return "p";
        return b.toString();
    }

    // ----------------------------------------------------------------
    // Accumulators
    // ----------------------------------------------------------------

    enum ParamKind { PATH, QUERY, HEADER, COOKIE, BODY, CALLBACK }

    enum FetchKind { MAPPED, MAPPED_LIST, STRING }

    static final class RestApi {
        String binaryName;
        String packageName;
        String simpleName;
        String implBinaryName;
        String implSimpleName;
        final List<RestMethod> methods = new ArrayList<RestMethod>();
    }

    static final class RestMethod {
        String name;
        String descriptor;
        String signature;
        String verb;
        String pathTemplate;
        int callbackIndex = -1;
        FetchKind fetchKind;
        String payloadBinaryName;          // FetchKind.MAPPED
        String payloadElementBinaryName;   // FetchKind.MAPPED_LIST
        final List<RestParam> params = new ArrayList<RestParam>();
    }

    static final class RestParam {
        int index;
        String name;
        String descriptor;
        String javaType;
        ParamKind kind;
        String bindingName; // path / query / header name
    }
}

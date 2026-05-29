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

/// Build-time `@GrpcClient` processor. Scans the project's compiled
/// classes for `@GrpcClient`-annotated interfaces, validates them,
/// and emits:
///
/// 1. One `<SimpleName>Impl` per `@GrpcClient` interface in the
///    same package. The impl chains
///    `com.codename1.io.grpc.GrpcWeb.invokeUnary(baseUrl, service,
///    method, bearerToken, request, RequestProtoCodec.INSTANCE,
///    ResponseProtoCodec.INSTANCE, callback)` for each
///    `@Rpc`-annotated method.
/// 2. A single `cn1app.GrpcClientBootstrap` that registers every
///    accepted interface with `com.codename1.io.grpc.GrpcClients`.
///
/// Mirrors [RestClientAnnotationProcessor] in structure.
public final class GrpcClientAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String GRPC_CLIENT_DESC = "Lcom/codename1/annotations/grpc/GrpcClient;";
    public static final String RPC_DESC = "Lcom/codename1/annotations/grpc/Rpc;";
    public static final String HEADER_DESC = "Lcom/codename1/annotations/rest/Header;";

    static final String BOOTSTRAP_BINARY = "cn1app.GrpcClientBootstrap";
    static final String BOOTSTRAP_SIMPLE = "GrpcClientBootstrap";
    static final String BOOTSTRAP_PACKAGE = "cn1app";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(GRPC_CLIENT_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    private final TreeMap<String, GrpcApi> accepted = new TreeMap<String, GrpcApi>();

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
        AnnotationValues clientAnno = cls.getClassAnnotation(GRPC_CLIENT_DESC);
        if (clientAnno == null) return;
        if (!cls.isInterface()) {
            ctx.error(cls, "@GrpcClient requires an interface; "
                    + cls.getBinaryName() + " is not an interface");
            return;
        }
        if (!cls.isPublic()) {
            ctx.error(cls, "@GrpcClient interface " + cls.getBinaryName()
                    + " must be public");
            return;
        }

        GrpcApi api = new GrpcApi();
        api.binaryName = cls.getBinaryName();
        api.simpleName = simpleName(api.binaryName);
        api.packageName = packageOf(api.binaryName);
        api.implSimpleName = api.simpleName + "Impl";
        api.implBinaryName = api.packageName.length() == 0
                ? api.implSimpleName
                : api.packageName + "." + api.implSimpleName;
        api.defaultService = clientAnno.getStringOrDefault("value", "");

        boolean anyError = false;
        for (MethodInfo m : cls.getMethods()) {
            if (m.isStatic()) continue;
            if (m.isSynthetic()) continue;
            if (m.isConstructor()) continue;
            if ((m.getAccess() & org.objectweb.asm.Opcodes.ACC_BRIDGE) != 0) continue;
            if (!m.isAbstract()) continue;

            AnnotationValues rpc = m.getAnnotation(RPC_DESC);
            if (rpc == null) {
                ctx.error(cls, "@GrpcClient method " + api.binaryName + "." + m.getName()
                        + " must carry an @Rpc annotation");
                anyError = true;
                continue;
            }
            String rpcMethod = rpc.getString("value");
            String rpcServiceOverride = rpc.getStringOrDefault("service", "");
            String serviceFqn = rpcServiceOverride.length() > 0 ? rpcServiceOverride : api.defaultService;
            if (serviceFqn == null || serviceFqn.length() == 0) {
                ctx.error(cls, "@Rpc on " + api.binaryName + "." + m.getName()
                        + " has no service path -- set @GrpcClient(\"<service>\") "
                        + "on the interface or @Rpc(service=\"<service>\", value=\"<method>\")");
                anyError = true;
                continue;
            }
            if (rpcMethod == null || rpcMethod.length() == 0) {
                ctx.error(cls, "@Rpc on " + api.binaryName + "." + m.getName()
                        + " requires a method name (value)");
                anyError = true;
                continue;
            }

            GrpcMethod gm = new GrpcMethod();
            gm.name = m.getName();
            gm.descriptor = m.getDescriptor();
            gm.signature = m.getSignature();
            gm.rpcMethod = rpcMethod;
            gm.service = serviceFqn;

            Type[] paramTypes = Type.getArgumentTypes(gm.descriptor);
            List<Map<String, AnnotationValues>> paramAnnotations = m.getParameterAnnotations();
            String[] genericSigs = RestClientAnnotationProcessor.parseGenericParameterSignatures(
                    gm.signature, paramTypes.length);

            int requestIndex = -1;
            int bearerIndex = -1;
            int callbackIndex = -1;
            boolean methodHasError = false;

            for (int i = 0; i < paramTypes.length; i++) {
                String descriptor = paramTypes[i].getDescriptor();
                Map<String, AnnotationValues> pa = (paramAnnotations != null && i < paramAnnotations.size())
                        ? paramAnnotations.get(i) : null;
                AnnotationValues hdr = pa == null ? null : pa.get(HEADER_DESC);

                if ("Lcom/codename1/util/OnComplete;".equals(descriptor)) {
                    if (callbackIndex >= 0) {
                        ctx.error(cls, "@GrpcClient method " + api.binaryName + "." + gm.name
                                + " declares more than one OnComplete callback");
                        methodHasError = true;
                        break;
                    }
                    callbackIndex = i;
                    String payloadSig = genericSigs == null ? null : genericSigs[i];
                    gm.responseBinaryName = extractGrpcResponsePayload(payloadSig);
                    continue;
                }
                if (hdr != null) {
                    String hv = hdr.getStringOrDefault("value", "");
                    if ("Authorization".equalsIgnoreCase(hv)) {
                        bearerIndex = i;
                        continue;
                    }
                    ctx.error(cls, "@GrpcClient method " + api.binaryName + "." + gm.name
                            + " carries @Header(\"" + hv + "\") -- only @Header(\"Authorization\") "
                            + "is honoured for gRPC clients in this release");
                    methodHasError = true;
                    break;
                }
                if (requestIndex < 0) {
                    requestIndex = i;
                    gm.requestBinaryName = descriptor.startsWith("L") && descriptor.endsWith(";")
                            ? descriptor.substring(1, descriptor.length() - 1).replace('/', '.')
                            : "java.lang.Object";
                    continue;
                }
                ctx.error(cls, "@GrpcClient method " + api.binaryName + "." + gm.name
                        + " has an unrecognised parameter at position " + i
                        + " (descriptor " + descriptor + "); expected: "
                        + "(<RequestMessage>, [@Header(\"Authorization\") String], OnComplete<GrpcResponse<...>>)");
                methodHasError = true;
                break;
            }

            if (methodHasError) {
                anyError = true;
                continue;
            }
            if (requestIndex < 0) {
                ctx.error(cls, "@GrpcClient method " + api.binaryName + "." + gm.name
                        + " must declare a request-message parameter before the callback");
                anyError = true;
                continue;
            }
            if (callbackIndex < 0) {
                ctx.error(cls, "@GrpcClient method " + api.binaryName + "." + gm.name
                        + " must end with an OnComplete<GrpcResponse<...>> callback");
                anyError = true;
                continue;
            }
            gm.requestParamIndex = requestIndex;
            gm.bearerParamIndex = bearerIndex;
            gm.callbackParamIndex = callbackIndex;
            gm.paramTypes = paramTypes;
            api.methods.add(gm);
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
        for (GrpcApi api : accepted.values()) {
            sources.put(api.implBinaryName, generateImplSource(api));
        }
        sources.put(BOOTSTRAP_BINARY, generateBootstrapSource(accepted.values()));

        try {
            List<java.io.File> cp = new ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated @GrpcClient sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size()
                + " @GrpcClient impl(s) + " + BOOTSTRAP_BINARY);
    }

    // ----------------------------------------------------------------
    // Source generation
    // ----------------------------------------------------------------

    private static String generateImplSource(GrpcApi api) {
        StringBuilder sb = new StringBuilder(2048);
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
        for (GrpcMethod gm : api.methods) {
            emitMethod(sb, gm);
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static void emitMethod(StringBuilder sb, GrpcMethod gm) {
        sb.append("    public void ").append(gm.name).append("(");
        for (int i = 0; i < gm.paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramJavaType(gm, i)).append(" p").append(i);
        }
        sb.append(") {\n");

        String requestExpr = "p" + gm.requestParamIndex;
        String bearerExpr = gm.bearerParamIndex < 0 ? "null" : "p" + gm.bearerParamIndex;
        String callbackExpr = "p" + gm.callbackParamIndex;
        String reqCodec = gm.requestBinaryName + "ProtoCodec.INSTANCE";
        String respCodec = gm.responseBinaryName + "ProtoCodec.INSTANCE";

        sb.append("        com.codename1.io.grpc.GrpcWeb.invokeUnary(\n")
                .append("            baseUrl, \"").append(escape(gm.service)).append("\", \"")
                .append(escape(gm.rpcMethod)).append("\",\n")
                .append("            ").append(bearerExpr).append(",\n")
                .append("            ").append(requestExpr).append(",\n")
                .append("            ").append(reqCodec).append(",\n")
                .append("            ").append(respCodec).append(",\n")
                .append("            ").append(callbackExpr).append(");\n");
        sb.append("    }\n\n");
    }

    /// Returns the Java type literal for the impl-method parameter at
    /// position `i`. The callback re-uses the generic signature so the
    /// impl's method signature stays parameterized with `GrpcResponse<X>`
    /// the way the user declared it.
    private static String paramJavaType(GrpcMethod gm, int i) {
        if (i == gm.callbackParamIndex) {
            return "com.codename1.util.OnComplete<com.codename1.io.grpc.GrpcResponse<"
                    + gm.responseBinaryName + ">>";
        }
        String descriptor = gm.paramTypes[i].getDescriptor();
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
        }
        // Should never happen for gRPC client methods (request type is
        // always a reference, bearerToken is String) but cover the
        // edge case rather than emit invalid source.
        return primitiveJava(descriptor);
    }

    private static String primitiveJava(String descriptor) {
        switch (descriptor) {
            case "I": return "int";
            case "J": return "long";
            case "F": return "float";
            case "D": return "double";
            case "Z": return "boolean";
            case "B": return "byte";
            case "S": return "short";
            case "C": return "char";
            default: return "java.lang.Object";
        }
    }

    private static String generateBootstrapSource(Iterable<GrpcApi> apis) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(BOOTSTRAP_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("///\n");
        sb.append("/// gRPC-client bootstrap. The iOS / Android per-build application\n");
        sb.append("/// stub instantiates this class before Display.init (the build\n");
        sb.append("/// server probes the project zip for it and emits the install\n");
        sb.append("/// line conditionally); JavaSEPort picks it up via Class.forName\n");
        sb.append("/// for the simulator and desktop runs.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(BOOTSTRAP_SIMPLE).append(" {\n");
        sb.append("    public ").append(BOOTSTRAP_SIMPLE).append("() {\n");
        for (GrpcApi api : apis) {
            sb.append("        com.codename1.io.grpc.GrpcClients.register(")
                    .append(api.binaryName)
                    .append(".class, new com.codename1.io.grpc.GrpcClients.Factory<")
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
    // Signature helpers
    // ----------------------------------------------------------------

    /// Extracts the response payload type from the callback
    /// parameter's generic signature
    /// `Lcom/codename1/util/OnComplete<Lcom/codename1/io/grpc/GrpcResponse<Lhello/HelloReply;>;>;`.
    /// Returns `java.lang.Object` when the signature is missing.
    static String extractGrpcResponsePayload(String paramSignature) {
        if (paramSignature == null) return "java.lang.Object";
        int lt = paramSignature.indexOf('<');
        int gt = paramSignature.lastIndexOf('>');
        if (lt < 0 || gt < 0 || lt > gt) return "java.lang.Object";
        String inner = paramSignature.substring(lt + 1, gt);
        int innerLt = inner.indexOf('<');
        int innerGt = inner.lastIndexOf('>');
        if (innerLt < 0 || innerGt < 0 || innerLt > innerGt) return "java.lang.Object";
        String payload = inner.substring(innerLt + 1, innerGt);
        if (payload.startsWith("L") && payload.endsWith(";")) {
            return payload.substring(1, payload.length() - 1).replace('/', '.');
        }
        return "java.lang.Object";
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

    // ----------------------------------------------------------------
    // Accumulators
    // ----------------------------------------------------------------

    static final class GrpcApi {
        String binaryName;
        String packageName;
        String simpleName;
        String implBinaryName;
        String implSimpleName;
        String defaultService;
        final List<GrpcMethod> methods = new ArrayList<GrpcMethod>();
    }

    static final class GrpcMethod {
        String name;
        String descriptor;
        String signature;
        String rpcMethod;
        String service;
        int requestParamIndex = -1;
        int bearerParamIndex = -1;
        int callbackParamIndex = -1;
        String requestBinaryName;
        String responseBinaryName;
        Type[] paramTypes;
    }
}

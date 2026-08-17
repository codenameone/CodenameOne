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
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/// Turns `AppIntent` and `IntentEntity` declarations into the two things the
/// framework needs: a reflection-free Java dispatch table, and an `intents.json`
/// manifest the native builders read to generate each platform's declarations.
///
/// #### Why everything is generated rather than looked up
///
/// On a translated iOS build there is no reflection to fall back on --
/// `Class.getAnnotation` returns null and `Class.forName` is banned in the
/// framework -- and the translator's dead-code eliminator strips any method
/// with no Java caller. So the generated registry calls every handler and every
/// entity query by **direct static invocation**. That is simultaneously what
/// keeps the methods alive through DCE and what lets Android's obfuscator rename
/// the call site and the target together.
///
/// #### Why the generated coercion never casts
///
/// Parameter values arrive from a platform payload as whatever the wire format
/// carried. ParparVM's `CHECKCAST` expands to nothing, so a bad cast does not
/// throw -- it hands the wrong object to the next instruction and crashes
/// natively somewhere else. Every generated conversion is therefore
/// `instanceof`-guarded with an explicit fallback, and entity parameters are
/// never cast at all: they arrive as an id string and become objects only by
/// calling the entity's own `BY_ID` query.
///
/// Validation surfaces every offending declaration in one build run through
/// `ProcessorContext#error`; nothing is generated while an error is pending.
public final class AppIntentAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String APP_INTENT_DESC = "Lcom/codename1/annotations/AppIntent;";
    public static final String INTENT_PARAM_DESC = "Lcom/codename1/annotations/IntentParam;";
    public static final String INTENT_ENTITY_DESC = "Lcom/codename1/annotations/IntentEntity;";
    public static final String ENTITY_ID_DESC = "Lcom/codename1/annotations/EntityId;";
    public static final String ENTITY_TITLE_DESC = "Lcom/codename1/annotations/EntityTitle;";
    public static final String ENTITY_SUBTITLE_DESC = "Lcom/codename1/annotations/EntitySubtitle;";
    public static final String ENTITY_IMAGE_DESC = "Lcom/codename1/annotations/EntityImage;";
    public static final String ENTITY_QUERY_DESC = "Lcom/codename1/annotations/EntityQuery;";

    static final String REGISTRY_PACKAGE = "com.codename1.intents.generated";
    static final String REGISTRY_SIMPLE = "IntentRegistry";
    static final String BOOTSTRAP_PACKAGE = "cn1app";
    static final String BOOTSTRAP_SIMPLE = "IntentBootstrap";
    static final String MANIFEST_RESOURCE = "intents.json";

    private static final String INTENT_RESULT_BINARY = "com.codename1.intents.IntentResult";
    private static final String INTENT_CONTEXT_DESC = "Lcom/codename1/intents/IntentContext;";
    private static final String DATE_DESC = "Ljava/util/Date;";
    private static final String STRING_DESC = "Ljava/lang/String;";
    private static final String ENCODED_IMAGE_DESC = "Lcom/codename1/ui/EncodedImage;";

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_]{2,63}");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");
    private static final Pattern ROUTE_PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_]+)\\}");

    /// Apple rejects an App Shortcut phrase that does not name the app.
    private static final String APP_NAME_TOKEN = "${applicationName}";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(APP_INTENT_DESC);
        s.add(INTENT_ENTITY_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    /// TreeMaps so the emitted source and manifest are byte-stable regardless of
    /// the order the class scan happened to walk the tree in.
    private final TreeMap<String, IntentDef> intents = new TreeMap<String, IntentDef>();
    private final TreeMap<String, EntityDef> entities = new TreeMap<String, EntityDef>();
    private final List<String> routePatterns = new ArrayList<String>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        intents.clear();
        entities.clear();
        routePatterns.clear();
        collectRoutePatterns(ctx);
    }

    /// Independently re-collects the project's `Route` patterns so `opensRoute`
    /// can be validated against them.
    ///
    /// The processor SPI has no way to express "run after the route processor",
    /// and inventing one for a single cross-check would be a heavier change than
    /// re-reading the annotations. This is a scan of an index that is already in
    /// memory.
    private void collectRoutePatterns(ProcessorContext ctx) {
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            addRoutePatterns(cls.getClassAnnotation(RouteAnnotationProcessor.ROUTE_DESC));
            addRouteContainer(cls.getClassAnnotation(RouteAnnotationProcessor.ROUTES_DESC));
            for (MethodInfo m : cls.getMethods()) {
                addRoutePatterns(m.getAnnotation(RouteAnnotationProcessor.ROUTE_DESC));
                addRouteContainer(m.getAnnotation(RouteAnnotationProcessor.ROUTES_DESC));
            }
        }
    }

    private void addRoutePatterns(AnnotationValues route) {
        if (route == null) {
            return;
        }
        String v = route.getString("value");
        if (v != null) {
            routePatterns.add(v);
        }
    }

    private void addRouteContainer(AnnotationValues container) {
        if (container == null) {
            return;
        }
        for (Object o : asList(container.get("value"))) {
            if (o instanceof AnnotationValues) {
                addRoutePatterns((AnnotationValues) o);
            }
        }
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.isSynthetic()) {
            return;
        }
        AnnotationValues entity = cls.getClassAnnotation(INTENT_ENTITY_DESC);
        if (entity != null) {
            processEntity(cls, entity, ctx);
        }
        for (MethodInfo m : cls.getMethods()) {
            AnnotationValues intent = m.getAnnotation(APP_INTENT_DESC);
            if (intent != null) {
                processIntent(cls, m, intent, ctx);
            }
        }
    }

    // ------------------------------------------------------------------
    // Entities
    // ------------------------------------------------------------------

    private void processEntity(AnnotatedClass cls, AnnotationValues ann, ProcessorContext ctx) {
        String type = ann.getString("value");
        if (type == null || !ID_PATTERN.matcher(type).matches()) {
            ctx.error(cls, "@IntentEntity id \"" + type + "\" must match "
                    + ID_PATTERN.pattern());
            return;
        }
        if (entities.containsKey(type)) {
            ctx.error(cls, "@IntentEntity id \"" + type + "\" is already declared by "
                    + entities.get(type).binaryName);
            return;
        }
        if (cls.isInterface() || cls.isAbstract()) {
            ctx.error(cls, "@IntentEntity requires a concrete class; "
                    + cls.getBinaryName() + " is abstract or an interface");
            return;
        }

        EntityDef def = new EntityDef();
        def.type = type;
        def.binaryName = cls.getBinaryName();
        def.title = ann.getStringOrDefault("title", type);
        def.indexed = ann.getBoolOrDefault("indexed", false);

        def.idAccessor = findAccessor(cls, ENTITY_ID_DESC, ctx, "@EntityId");
        def.titleAccessor = findAccessor(cls, ENTITY_TITLE_DESC, ctx, "@EntityTitle");
        def.subtitleAccessor = findAccessor(cls, ENTITY_SUBTITLE_DESC, ctx, "@EntitySubtitle");
        def.imageAccessor = findAccessor(cls, ENTITY_IMAGE_DESC, ctx, "@EntityImage");

        if (def.idAccessor == null) {
            ctx.error(cls, "@IntentEntity " + cls.getBinaryName()
                    + " must declare exactly one @EntityId member returning String");
        } else if (!STRING_DESC.equals(def.idAccessor.typeDescriptor)) {
            ctx.error(cls, "@EntityId on " + cls.getBinaryName() + " must return String, not "
                    + readable(def.idAccessor.typeDescriptor));
        }
        if (def.indexed && def.titleAccessor == null) {
            ctx.error(cls, "@IntentEntity " + cls.getBinaryName()
                    + " is indexed, so it must declare an @EntityTitle to display");
        }
        if (def.imageAccessor != null
                && !ENCODED_IMAGE_DESC.equals(def.imageAccessor.typeDescriptor)) {
            ctx.error(cls, "@EntityImage on " + cls.getBinaryName()
                    + " must return EncodedImage, not "
                    + readable(def.imageAccessor.typeDescriptor));
        }

        for (MethodInfo m : cls.getMethods()) {
            AnnotationValues q = m.getAnnotation(ENTITY_QUERY_DESC);
            if (q == null) {
                continue;
            }
            String kind = enumConstant(q.get("value"));
            if (kind == null) {
                ctx.error(cls, "@EntityQuery on " + cls.getBinaryName() + "." + m.getName()
                        + " must name a Kind");
                continue;
            }
            if (!m.isStatic() || !m.isPublic()) {
                ctx.error(cls, "@EntityQuery " + cls.getBinaryName() + "." + m.getName()
                        + " must be public static -- the platform calls it directly, with no"
                        + " instance of your class in existence");
                continue;
            }
            def.queries.put(kind, m.getName());
        }

        if (!def.queries.containsKey("BY_ID")) {
            ctx.error(cls, "@IntentEntity " + cls.getBinaryName()
                    + " must declare an @EntityQuery(BY_ID) method: an entity crosses to the"
                    + " platform as its id, so resolving that id back is the one lookup the"
                    + " framework cannot do without");
        }
        entities.put(type, def);
    }

    /// Finds the single member carrying `descriptor`, reporting a clear error
    /// when more than one does.
    private Accessor findAccessor(AnnotatedClass cls, String descriptor, ProcessorContext ctx,
                                   String label) {
        Accessor found = null;
        for (MethodInfo m : cls.getMethods()) {
            if (m.getAnnotation(descriptor) == null) {
                continue;
            }
            if (found != null) {
                ctx.error(cls, label + " appears more than once on " + cls.getBinaryName());
                return found;
            }
            if (!m.isPublic() || m.isStatic()
                    || Type.getArgumentTypes(m.getDescriptor()).length != 0) {
                ctx.error(cls, label + " on " + cls.getBinaryName() + "." + m.getName()
                        + " must be a public no-argument instance method");
                continue;
            }
            found = new Accessor(m.getName(), true,
                    Type.getReturnType(m.getDescriptor()).getDescriptor());
        }
        for (FieldInfo f : cls.getFields()) {
            if (f.getAnnotation(descriptor) == null) {
                continue;
            }
            if (found != null) {
                ctx.error(cls, label + " appears more than once on " + cls.getBinaryName());
                return found;
            }
            if (!f.isPublic() || f.isStatic()) {
                ctx.error(cls, label + " on " + cls.getBinaryName() + "." + f.getName()
                        + " must be a public instance field");
                continue;
            }
            found = new Accessor(f.getName(), false, f.getDescriptor());
        }
        return found;
    }

    // ------------------------------------------------------------------
    // Intents
    // ------------------------------------------------------------------

    private void processIntent(AnnotatedClass cls, MethodInfo m, AnnotationValues ann,
                                ProcessorContext ctx) {
        String id = ann.getString("value");
        String where = cls.getBinaryName() + "." + m.getName();
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            ctx.error(cls, "@AppIntent id \"" + id + "\" on " + where + " must match "
                    + ID_PATTERN.pattern());
            return;
        }
        if (intents.containsKey(id)) {
            ctx.error(cls, "@AppIntent id \"" + id + "\" is already declared by "
                    + intents.get(id).where);
            return;
        }
        if (!m.isStatic() || !m.isPublic()) {
            ctx.error(cls, "@AppIntent " + where + " must be public static: the build emits a"
                    + " direct call to it, and it can be asked to run in a process where no"
                    + " instance of your class exists");
            return;
        }

        Type returnType = Type.getReturnType(m.getDescriptor());
        boolean returnsResult = INTENT_RESULT_BINARY.equals(returnType.getClassName());
        if (!returnsResult && returnType.getSort() != Type.VOID) {
            ctx.error(cls, "@AppIntent " + where + " must return IntentResult or void, not "
                    + returnType.getClassName());
            return;
        }

        IntentDef def = new IntentDef();
        def.id = id;
        def.where = where;
        def.ownerBinary = cls.getBinaryName();
        def.methodName = m.getName();
        def.returnsResult = returnsResult;
        def.title = ann.getStringOrDefault("title", id);
        def.description = ann.getStringOrDefault("description", "");
        def.headless = ann.getBoolOrDefault("headless", false);
        def.discoverable = ann.getBoolOrDefault("discoverable", true);
        def.destructive = ann.getBoolOrDefault("destructive", false);
        def.opensRoute = ann.getStringOrDefault("opensRoute", "");
        def.timeoutSeconds = ann.getIntOrDefault("timeoutSeconds", 20);
        for (Object o : asList(ann.get("phrases"))) {
            if (o instanceof String) {
                def.phrases.add((String) o);
            }
        }
        for (Object o : asList(ann.get("exposure"))) {
            String e = enumConstant(o);
            if (e != null) {
                def.exposure.add(e);
            }
        }
        if (def.exposure.isEmpty()) {
            def.exposure.add("ASSISTANT");
        }

        readParameters(cls, m, def, ctx);

        for (String phrase : def.phrases) {
            if (phrase.indexOf(APP_NAME_TOKEN) < 0) {
                ctx.error(cls, "@AppIntent " + where + " phrase \"" + phrase
                        + "\" must contain " + APP_NAME_TOKEN
                        + " -- Apple rejects App Shortcut phrases that omit the app name");
            }
            checkPlaceholders(cls, ctx, where, PLACEHOLDER, phrase, def, "phrase");
        }
        if (def.opensRoute.length() > 0) {
            checkPlaceholders(cls, ctx, where, ROUTE_PLACEHOLDER, def.opensRoute, def,
                    "opensRoute");
            if (!routeDeclared(def.opensRoute)) {
                ctx.error(cls, "@AppIntent " + where + " opensRoute \"" + def.opensRoute
                        + "\" does not match any @Route in this project");
            }
        }
        intents.put(id, def);
    }

    private void readParameters(AnnotatedClass cls, MethodInfo m, IntentDef def,
                                 ProcessorContext ctx) {
        Type[] args = Type.getArgumentTypes(m.getDescriptor());
        List<Map<String, AnnotationValues>> paramAnnotations = m.getParameterAnnotations();
        for (int i = 0; i < args.length; i++) {
            String desc = args[i].getDescriptor();
            Map<String, AnnotationValues> onThis = i < paramAnnotations.size()
                    ? paramAnnotations.get(i)
                    : Collections.<String, AnnotationValues>emptyMap();
            AnnotationValues p = onThis.get(INTENT_PARAM_DESC);

            if (p == null) {
                // A leading IntentContext is the one parameter that carries no
                // annotation, because it is supplied by the framework rather
                // than by the caller.
                if (i == 0 && INTENT_CONTEXT_DESC.equals(desc)) {
                    def.takesContext = true;
                    continue;
                }
                ctx.error(cls, "@AppIntent " + def.where + " parameter " + i
                        + " needs @IntentParam (only a leading IntentContext may omit it)");
                continue;
            }
            if (INTENT_CONTEXT_DESC.equals(desc)) {
                ctx.error(cls, "@AppIntent " + def.where
                        + " IntentContext must be the first parameter and carry no @IntentParam");
                continue;
            }

            ParamDef pd = new ParamDef();
            pd.name = p.getString("value");
            pd.title = p.getStringOrDefault("title", pd.name == null ? "" : pd.name);
            pd.required = p.getBoolOrDefault("required", true);
            pd.defaultValue = p.getStringOrDefault("defaultValue", "");
            pd.descriptor = desc;
            for (Object o : asList(p.get("options"))) {
                if (o instanceof String) {
                    pd.options.add((String) o);
                }
            }
            if (pd.name == null || pd.name.length() == 0) {
                ctx.error(cls, "@IntentParam on " + def.where + " parameter " + i
                        + " needs a name");
                continue;
            }
            pd.kind = kindOf(desc);
            if (pd.kind == null) {
                // Not a primitive we know: it has to be a declared entity, and
                // that is checked in finish() once every class has been seen.
                pd.kind = "entity";
                pd.entityBinary = args[i].getClassName();
            }
            def.params.add(pd);
        }
    }

    private void checkPlaceholders(AnnotatedClass cls, ProcessorContext ctx, String where,
                                    Pattern pattern, String text, IntentDef def, String label) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1);
            if ("applicationName".equals(name)) {
                continue;
            }
            if (def.param(name) == null) {
                ctx.error(cls, "@AppIntent " + where + " " + label + " references ${" + name
                        + "} but declares no parameter with that name");
            }
        }
    }

    /// True when some `@Route` pattern could produce this URL. Compared
    /// structurally by segment count and literal segments, because the route may
    /// use `:id` where the intent uses `{id}`.
    private boolean routeDeclared(String opensRoute) {
        String path = opensRoute;
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        String[] want = split(path);
        for (String pattern : routePatterns) {
            if (matches(split(pattern), want)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String[] pattern, String[] want) {
        for (int i = 0; i < pattern.length; i++) {
            if ("**".equals(pattern[i])) {
                return true;
            }
            if (i >= want.length) {
                return false;
            }
            String p = pattern[i];
            boolean wildcard = "*".equals(p) || p.startsWith(":");
            boolean variable = want[i].startsWith("{") && want[i].endsWith("}");
            if (!wildcard && !variable && !p.equals(want[i])) {
                return false;
            }
        }
        return pattern.length == want.length;
    }

    private static String[] split(String path) {
        List<String> out = new ArrayList<String>();
        for (String s : path.split("/")) {
            if (s.length() > 0) {
                out.add(s);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    // ------------------------------------------------------------------
    // Emission
    // ------------------------------------------------------------------

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        resolveEntityParameters(ctx);
        if (ctx.hasErrors()) {
            return;
        }
        if (intents.isEmpty() && entities.isEmpty()) {
            return;
        }

        List<IntentDef> defs = new ArrayList<IntentDef>(intents.values());
        List<EntityDef> ents = new ArrayList<EntityDef>(entities.values());

        String registry = generateRegistry(defs, ents);
        String bootstrap = generateBootstrap();
        File outDir = ctx.getOutputClassDir();
        try {
            Map<String, String> srcs = new LinkedHashMap<String, String>();
            srcs.put(REGISTRY_PACKAGE + "." + REGISTRY_SIMPLE, registry);
            srcs.put(BOOTSTRAP_PACKAGE + "." + BOOTSTRAP_SIMPLE, bootstrap);
            List<File> cp = new ArrayList<File>();
            cp.add(outDir);
            JavaSourceCompiler.compile(srcs, outDir, cp);
        } catch (IOException e) {
            throw new ProcessingException("Failed to compile the generated intent registry: "
                    + e.getMessage(), e);
        }

        try {
            ctx.emitResource(MANIFEST_RESOURCE, generateManifest(defs, ents).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ProcessingException("UTF-8 unavailable", e);
        }

        ctx.getLog().info("cn1: generated " + REGISTRY_PACKAGE + "." + REGISTRY_SIMPLE
                + " with " + defs.size() + " intent(s) and " + ents.size() + " entity type(s)");
    }

    /// Binds every entity-typed parameter to a declared entity. Deferred to
    /// `finish` because an intent may be scanned before the entity it names.
    private void resolveEntityParameters(ProcessorContext ctx) {
        for (IntentDef def : intents.values()) {
            for (ParamDef p : def.params) {
                if (!"entity".equals(p.kind)) {
                    continue;
                }
                EntityDef match = null;
                for (EntityDef e : entities.values()) {
                    if (e.binaryName.equals(p.entityBinary)) {
                        match = e;
                        break;
                    }
                }
                if (match == null) {
                    ctx.error("@AppIntent " + def.where + " parameter \"" + p.name
                            + "\" has type " + p.entityBinary
                            + ", which is not annotated @IntentEntity."
                            + " Supported parameter types are String, int, long, float, double,"
                            + " boolean, java.util.Date and @IntentEntity classes.");
                    continue;
                }
                p.entityType = match.type;
            }
        }
    }

    private String generateBootstrap() {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(BOOTSTRAP_PACKAGE).append(";\n\n");
        sb.append("/** Generated by Codename One. Installs the intent registry at startup. */\n");
        sb.append("public final class ").append(BOOTSTRAP_SIMPLE).append(" {\n");
        sb.append("    public ").append(BOOTSTRAP_SIMPLE).append("() {\n");
        // A direct `new`, not a reflective lookup: this is the reference that
        // keeps the registry (and through it every handler) alive through the
        // iOS translator's dead-code elimination, and that lets R8 rename the
        // call site and the class together.
        sb.append("        com.codename1.intents.Intents.setDispatcher(new ")
                .append(REGISTRY_PACKAGE).append(".").append(REGISTRY_SIMPLE).append("());\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String generateRegistry(List<IntentDef> defs, List<EntityDef> ents) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(REGISTRY_PACKAGE).append(";\n\n");
        sb.append("import com.codename1.intents.AppEntity;\n");
        sb.append("import com.codename1.intents.Exposure;\n");
        sb.append("import com.codename1.intents.IntentContext;\n");
        sb.append("import com.codename1.intents.IntentDeclaration;\n");
        sb.append("import com.codename1.intents.IntentDispatcher;\n");
        sb.append("import com.codename1.intents.IntentParameterInfo;\n");
        sb.append("import com.codename1.intents.IntentParameterType;\n");
        sb.append("import com.codename1.intents.IntentResult;\n");
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.Arrays;\n");
        sb.append("import java.util.Collections;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("/** Generated by Codename One from @AppIntent / @IntentEntity. Do not edit. */\n");
        sb.append("public final class ").append(REGISTRY_SIMPLE)
                .append(" implements IntentDispatcher {\n\n");

        generateDescribe(sb, defs);
        generateInvoke(sb, defs);
        generateQueryEntities(sb, ents);
        generateAdapters(sb, ents);
        generateCoercion(sb);

        sb.append("}\n");
        return sb.toString();
    }

    private void generateDescribe(StringBuilder sb, List<IntentDef> defs) {
        sb.append("    private static final List<IntentDeclaration> DECLARATIONS = build();\n\n");
        sb.append("    public List<IntentDeclaration> describe() {\n");
        sb.append("        return DECLARATIONS;\n");
        sb.append("    }\n\n");
        sb.append("    private static List<IntentDeclaration> build() {\n");
        sb.append("        List<IntentDeclaration> out = new ArrayList<IntentDeclaration>();\n");
        for (IntentDef d : defs) {
            sb.append("        {\n");
            sb.append("            List<IntentParameterInfo> p = new ArrayList<IntentParameterInfo>();\n");
            for (ParamDef p : d.params) {
                sb.append("            p.add(new IntentParameterInfo(")
                        .append(quote(p.name)).append(", ").append(quote(p.title)).append(", ")
                        .append("IntentParameterType.").append(parameterTypeConstant(p))
                        .append(", ").append(p.required).append(", ")
                        .append(p.entityType == null ? "null" : quote(p.entityType)).append(", ")
                        .append(p.defaultValue.length() == 0 ? "null" : quote(p.defaultValue))
                        .append(", ").append(stringListExpr(p.options)).append("));\n");
            }
            sb.append("            out.add(new IntentDeclaration(")
                    .append(quote(d.id)).append(", ").append(quote(d.title)).append(", ")
                    .append(quote(d.description)).append(", ")
                    .append(d.headless).append(", ").append(d.discoverable).append(", ")
                    .append(d.destructive).append(", ").append(quote(d.opensRoute)).append(", ")
                    .append(d.timeoutSeconds).append(", ")
                    .append(stringListExpr(d.phrases)).append(", p, ")
                    .append(exposureListExpr(d.exposure)).append("));\n");
            sb.append("        }\n");
        }
        sb.append("        return Collections.unmodifiableList(out);\n");
        sb.append("    }\n\n");
    }

    private void generateInvoke(StringBuilder sb, List<IntentDef> defs) {
        sb.append("    public IntentResult invoke(String intentId, Map<String, Object> params,\n");
        sb.append("                                IntentContext ctx) {\n");
        for (IntentDef d : defs) {
            sb.append("        if (").append(quote(d.id)).append(".equals(intentId)) {\n");
            StringBuilder call = new StringBuilder();
            call.append(sourceName(d.ownerBinary)).append(".").append(d.methodName).append("(");
            boolean first = true;
            if (d.takesContext) {
                call.append("ctx");
                first = false;
            }
            for (ParamDef p : d.params) {
                if (!first) {
                    call.append(", ");
                }
                first = false;
                call.append(argumentExpression(p));
            }
            call.append(")");
            if (d.returnsResult) {
                sb.append("            return ").append(call).append(";\n");
            } else {
                sb.append("            ").append(call).append(";\n");
                sb.append("            return IntentResult.ok();\n");
            }
            sb.append("        }\n");
        }
        sb.append("        return null;\n");
        sb.append("    }\n\n");
    }

    private String argumentExpression(ParamDef p) {
        String key = quote(p.name);
        if (!p.options.isEmpty() && "string".equals(p.kind)) {
            // The declaration promises a closed vocabulary, so the framework has to enforce it
            // rather than trusting whatever the platform or an in-app caller supplied.
            return "oneOf(params, " + key + ", "
                    + (p.defaultValue.length() == 0 ? "null" : quote(p.defaultValue)) + ", "
                    + stringArrayExpr(p.options) + ")";
        }
        if ("entity".equals(p.kind)) {
            return "entity_" + p.entityType + "(params, " + key + ")";
        }
        String def = p.defaultValue.length() == 0 ? null : p.defaultValue;
        if ("string".equals(p.kind)) {
            return "asString(params, " + key + ", " + (def == null ? "null" : quote(def)) + ")";
        }
        if ("date".equals(p.kind)) {
            return "asDate(params, " + key + ")";
        }
        if ("boolean".equals(p.kind)) {
            return "asBoolean(params, " + key + ", " + Boolean.parseBoolean(p.defaultValue) + ")";
        }
        String fallback = def == null ? "0" : def;
        if ("int".equals(p.kind)) {
            return "asInt(params, " + key + ", " + numeric(fallback, "0") + ")";
        }
        if ("long".equals(p.kind)) {
            return "asLong(params, " + key + ", " + numeric(fallback, "0") + "L)";
        }
        if ("float".equals(p.kind)) {
            return "(float) asDouble(params, " + key + ", " + numeric(fallback, "0") + "d)";
        }
        return "asDouble(params, " + key + ", " + numeric(fallback, "0") + "d)";
    }

    private void generateQueryEntities(StringBuilder sb, List<EntityDef> ents) {
        sb.append("    public List<AppEntity> queryEntities(String entityType, String kind,\n");
        sb.append("                                       String argument) {\n");
        for (EntityDef e : ents) {
            sb.append("        if (").append(quote(e.type)).append(".equals(entityType)) {\n");
            String byId = e.queries.get("BY_ID");
            if (byId != null) {
                sb.append("            if (\"byId\".equals(kind)) {\n");
                sb.append("                List<AppEntity> out = new ArrayList<AppEntity>();\n");
                sb.append("                AppEntity one = adapt_").append(e.type).append("(")
                        .append(sourceName(e.binaryName)).append(".").append(byId)
                        .append("(argument));\n");
                sb.append("                if (one != null) { out.add(one); }\n");
                sb.append("                return out;\n");
                sb.append("            }\n");
            }
            appendListQuery(sb, e, "SUGGESTED", "suggested", false);
            appendListQuery(sb, e, "SEARCH", "search", true);
            sb.append("            return Collections.emptyList();\n");
            sb.append("        }\n");
        }
        sb.append("        return Collections.emptyList();\n");
        sb.append("    }\n\n");
    }

    private void appendListQuery(StringBuilder sb, EntityDef e, String kindKey, String wireKind,
                                  boolean takesArgument) {
        String method = e.queries.get(kindKey);
        if (method == null) {
            return;
        }
        sb.append("            if (\"").append(wireKind).append("\".equals(kind)) {\n");
        sb.append("                return adaptAll_").append(e.type).append("(")
                .append(sourceName(e.binaryName)).append(".").append(method)
                .append(takesArgument ? "(argument)" : "()").append(");\n");
        sb.append("            }\n");
    }

    private void generateAdapters(StringBuilder sb, List<EntityDef> ents) {
        for (EntityDef e : ents) {
            sb.append("    private static AppEntity adapt_").append(e.type).append("(")
                    .append(sourceName(e.binaryName)).append(" o) {\n");
            sb.append("        if (o == null) { return null; }\n");
            sb.append("        AppEntity e = new AppEntity(").append(quote(e.type)).append(", ")
                    .append(read(e.idAccessor)).append(");\n");
            if (e.titleAccessor != null) {
                sb.append("        e.setTitle(").append(readAsString(e.titleAccessor)).append(");\n");
            }
            if (e.subtitleAccessor != null) {
                sb.append("        e.setSubtitle(").append(readAsString(e.subtitleAccessor))
                        .append(");\n");
            }
            if (e.imageAccessor != null) {
                sb.append("        e.setImage(").append(read(e.imageAccessor)).append(");\n");
            }
            sb.append("        return e;\n");
            sb.append("    }\n\n");

            sb.append("    private static List<AppEntity> adaptAll_").append(e.type)
                    .append("(List<").append(sourceName(e.binaryName)).append("> in) {\n");
            sb.append("        List<AppEntity> out = new ArrayList<AppEntity>();\n");
            sb.append("        if (in != null) {\n");
            sb.append("            for (").append(sourceName(e.binaryName)).append(" o : in) {\n");
            sb.append("                AppEntity a = adapt_").append(e.type).append("(o);\n");
            sb.append("                if (a != null) { out.add(a); }\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("        return out;\n");
            sb.append("    }\n\n");

            sb.append("    private static ").append(sourceName(e.binaryName)).append(" entity_")
                    .append(e.type).append("(Map<String, Object> params, String key) {\n");
            sb.append("        String id = asString(params, key, null);\n");
            sb.append("        if (id == null) { return null; }\n");
            // Entities are never cast out of the payload: they arrive as an id
            // and become objects only through the declared BY_ID query.
            sb.append("        return ").append(sourceName(e.binaryName)).append(".")
                    .append(e.queries.get("BY_ID")).append("(id);\n");
            sb.append("    }\n\n");
        }
    }

    private static String read(Accessor a) {
        return a.method ? "o." + a.name + "()" : "o." + a.name;
    }

    private static String readAsString(Accessor a) {
        String expr = read(a);
        if (STRING_DESC.equals(a.typeDescriptor)) {
            return expr;
        }
        return "String.valueOf(" + expr + ")";
    }

    /// The conversion helpers every generated call site uses.
    ///
    /// Not one of them casts an incoming payload value. ParparVM's `CHECKCAST`
    /// expands to nothing, so a cast that fails does not throw -- it hands the
    /// wrong object onward and crashes natively later, somewhere unrelated.
    /// Every branch here is `instanceof`-guarded with an explicit fallback.
    private void generateCoercion(StringBuilder sb) {
        sb.append("    private static String asString(Map<String, Object> p, String k, String def) {\n");
        sb.append("        Object o = p == null ? null : p.get(k);\n");
        sb.append("        if (o instanceof String) { return (String) o; }\n");
        sb.append("        if (o != null) { return String.valueOf(o); }\n");
        sb.append("        return def;\n");
        sb.append("    }\n\n");

        sb.append("    private static long asLong(Map<String, Object> p, String k, long def) {\n");
        sb.append("        Object o = p == null ? null : p.get(k);\n");
        sb.append("        if (o instanceof Number) { return ((Number) o).longValue(); }\n");
        sb.append("        if (o instanceof String) {\n");
        sb.append("            try { return Long.parseLong(((String) o).trim()); }\n");
        sb.append("            catch (NumberFormatException e) { return def; }\n");
        sb.append("        }\n");
        sb.append("        return def;\n");
        sb.append("    }\n\n");

        sb.append("    private static int asInt(Map<String, Object> p, String k, int def) {\n");
        sb.append("        return (int) asLong(p, k, def);\n");
        sb.append("    }\n\n");

        sb.append("    private static double asDouble(Map<String, Object> p, String k, double def) {\n");
        sb.append("        Object o = p == null ? null : p.get(k);\n");
        sb.append("        if (o instanceof Number) { return ((Number) o).doubleValue(); }\n");
        sb.append("        if (o instanceof String) {\n");
        sb.append("            try { return Double.parseDouble(((String) o).trim()); }\n");
        sb.append("            catch (NumberFormatException e) { return def; }\n");
        sb.append("        }\n");
        sb.append("        return def;\n");
        sb.append("    }\n\n");

        sb.append("    private static boolean asBoolean(Map<String, Object> p, String k, boolean def) {\n");
        sb.append("        Object o = p == null ? null : p.get(k);\n");
        sb.append("        if (o instanceof Boolean) { return ((Boolean) o).booleanValue(); }\n");
        sb.append("        if (o instanceof String) { return \"true\".equalsIgnoreCase(((String) o).trim()); }\n");
        sb.append("        if (o instanceof Number) { return ((Number) o).intValue() != 0; }\n");
        sb.append("        return def;\n");
        sb.append("    }\n\n");

        sb.append("    private static String oneOf(Map<String, Object> p, String k, String def,\n");
        sb.append("                                String[] options) {\n");
        sb.append("        String v = asString(p, k, def);\n");
        sb.append("        if (v == null) { return null; }\n");
        sb.append("        for (int i = 0; i < options.length; i++) {\n");
        sb.append("            if (options[i].equals(v)) { return v; }\n");
        sb.append("        }\n");
        // A value outside the vocabulary is not silently coerced to the default: that would run
        // the handler with something the caller did not ask for.
        sb.append("        throw new IllegalArgumentException(\"\\\"\" + v\n");
        sb.append("                + \"\\\" is not an accepted value for \" + k);\n");
        sb.append("    }\n\n");

        sb.append("    private static java.util.Date asDate(Map<String, Object> p, String k) {\n");
        sb.append("        Object o = p == null ? null : p.get(k);\n");
        sb.append("        if (o instanceof java.util.Date) { return (java.util.Date) o; }\n");
        sb.append("        if (o instanceof Number) { return new java.util.Date(((Number) o).longValue()); }\n");
        sb.append("        if (o instanceof String) {\n");
        sb.append("            try { return new java.util.Date(Long.parseLong(((String) o).trim())); }\n");
        sb.append("            catch (NumberFormatException e) { return null; }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n");
    }

    // ------------------------------------------------------------------
    // Manifest
    // ------------------------------------------------------------------

    /// The document the native builders read. Deliberately the same shape the
    /// runtime publishes over the bridge, so what the build compiles into the
    /// app and what the app reports at runtime cannot describe different things.
    private String generateManifest(List<IntentDef> defs, List<EntityDef> ents) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"schema\": 1,\n  \"intents\": [");
        for (int i = 0; i < defs.size(); i++) {
            IntentDef d = defs.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\"id\": ").append(json(d.id));
            sb.append(", \"title\": ").append(json(d.title));
            sb.append(", \"description\": ").append(json(d.description));
            sb.append(", \"headless\": ").append(d.headless);
            sb.append(", \"discoverable\": ").append(d.discoverable);
            sb.append(", \"destructive\": ").append(d.destructive);
            sb.append(", \"opensRoute\": ").append(json(d.opensRoute));
            sb.append(", \"timeoutSeconds\": ").append(d.timeoutSeconds);
            sb.append(", \"phrases\": ").append(jsonArray(d.phrases));
            // The native builders need this: without it a MODEL-only intent still becomes an
            // App Intent and a launcher shortcut, which is the opposite of what it declared.
            sb.append(", \"exposure\": ").append(jsonArray(d.exposure));
            sb.append(", \"params\": [");
            for (int j = 0; j < d.params.size(); j++) {
                ParamDef p = d.params.get(j);
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append("{\"name\": ").append(json(p.name));
                sb.append(", \"title\": ").append(json(p.title));
                sb.append(", \"type\": ").append(json(p.kind));
                sb.append(", \"required\": ").append(p.required);
                if (p.entityType != null) {
                    sb.append(", \"entityType\": ").append(json(p.entityType));
                }
                if (p.defaultValue.length() > 0) {
                    sb.append(", \"default\": ").append(json(p.defaultValue));
                }
                if (!p.options.isEmpty()) {
                    sb.append(", \"options\": ").append(jsonArray(p.options));
                }
                sb.append("}");
            }
            sb.append("]}");
        }
        sb.append(defs.isEmpty() ? "" : "\n  ").append("],\n  \"entities\": [");
        for (int i = 0; i < ents.size(); i++) {
            EntityDef e = ents.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\"type\": ").append(json(e.type));
            sb.append(", \"title\": ").append(json(e.title));
            sb.append(", \"indexed\": ").append(e.indexed);
            sb.append(", \"queries\": ").append(jsonArray(new ArrayList<String>(e.queries.keySet())));
            sb.append("}");
        }
        sb.append(ents.isEmpty() ? "" : "\n  ").append("]\n}\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String kindOf(String descriptor) {
        if (STRING_DESC.equals(descriptor)) return "string";
        if ("I".equals(descriptor) || "Ljava/lang/Integer;".equals(descriptor)) return "int";
        if ("J".equals(descriptor) || "Ljava/lang/Long;".equals(descriptor)) return "long";
        if ("F".equals(descriptor) || "Ljava/lang/Float;".equals(descriptor)) return "float";
        if ("D".equals(descriptor) || "Ljava/lang/Double;".equals(descriptor)) return "double";
        if ("Z".equals(descriptor) || "Ljava/lang/Boolean;".equals(descriptor)) return "boolean";
        if (DATE_DESC.equals(descriptor)) return "date";
        return null;
    }

    private static String parameterTypeConstant(ParamDef p) {
        if ("entity".equals(p.kind)) return "ENTITY";
        if ("string".equals(p.kind)) return "STRING";
        if ("date".equals(p.kind)) return "DATE";
        if ("boolean".equals(p.kind)) return "BOOLEAN";
        if ("int".equals(p.kind) || "long".equals(p.kind)) return "INTEGER";
        return "NUMBER";
    }

    private static String numeric(String value, String fallback) {
        try {
            Double.parseDouble(value.trim());
            return value.trim();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /// Turns a JVM binary name into one that is legal in generated Java source.
    ///
    /// A nested class is `Outer$Inner` in binary form and `Outer.Inner` in source, and nested
    /// handlers and entities are the common case rather than the exception -- an application
    /// naturally declares its entity as a static nested class next to the code that uses it.
    /// Emitting the binary form produces source that does not compile.
    private static String sourceName(String binaryName) {
        return binaryName == null ? null : binaryName.replace('$', '.');
    }

    private static String readable(String descriptor) {
        return Type.getType(descriptor).getClassName();
    }

    /// ASM hands an enum constant back as `{internalName, constantName}`.
    private static String enumConstant(Object value) {
        if (value instanceof String[]) {
            String[] pair = (String[]) value;
            return pair.length > 1 ? pair[1] : null;
        }
        return null;
    }

    private static List<Object> asList(Object value) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return Collections.emptyList();
    }

    private static String stringArrayExpr(List<String> values) {
        StringBuilder sb = new StringBuilder("new String[]{");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(values.get(i)));
        }
        return sb.append("}").toString();
    }

    private static String stringListExpr(List<String> values) {
        if (values.isEmpty()) {
            return "Collections.<String>emptyList()";
        }
        StringBuilder sb = new StringBuilder("Arrays.asList(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(values.get(i)));
        }
        return sb.append(")").toString();
    }

    private static String exposureListExpr(List<String> values) {
        StringBuilder sb = new StringBuilder("Arrays.asList(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("Exposure.").append(values.get(i));
        }
        return sb.append(")").toString();
    }

    private static String quote(String s) {
        return s == null ? "null" : json(s);
    }

    private static String json(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append("\"").toString();
    }

    private static String jsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(json(values.get(i)));
        }
        return sb.append("]").toString();
    }

    // ------------------------------------------------------------------
    // Models
    // ------------------------------------------------------------------

    private static final class IntentDef {
        private String id;
        private String where;
        private String ownerBinary;
        private String methodName;
        private String title;
        private String description;
        private boolean headless;
        private boolean discoverable;
        private boolean destructive;
        private String opensRoute = "";
        private int timeoutSeconds = 20;
        private boolean takesContext;
        private boolean returnsResult;
        private final List<String> phrases = new ArrayList<String>();
        private final List<String> exposure = new ArrayList<String>();
        private final List<ParamDef> params = new ArrayList<ParamDef>();

        ParamDef param(String name) {
            for (ParamDef p : params) {
                if (p.name != null && p.name.equals(name)) {
                    return p;
                }
            }
            return null;
        }
    }

    private static final class ParamDef {
        private String name;
        private String title;
        private String kind;
        private String descriptor;
        private String entityBinary;
        private String entityType;
        private boolean required = true;
        private String defaultValue = "";
        private final List<String> options = new ArrayList<String>();
    }

    private static final class EntityDef {
        private String type;
        private String binaryName;
        private String title;
        private boolean indexed;
        private Accessor idAccessor;
        private Accessor titleAccessor;
        private Accessor subtitleAccessor;
        private Accessor imageAccessor;
        private final TreeMap<String, String> queries = new TreeMap<String, String>();
    }

    private static final class Accessor {
        private final String name;
        private final boolean method;
        private final String typeDescriptor;

        Accessor(String name, boolean method, String typeDescriptor) {
            this.name = name;
            this.method = method;
            this.typeDescriptor = typeDescriptor;
        }
    }
}

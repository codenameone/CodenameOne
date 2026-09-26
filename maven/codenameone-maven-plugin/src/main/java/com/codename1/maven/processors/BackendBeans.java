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
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// The backend's beans, resolved at build time.
///
/// Every class carrying a stereotype -- `@Component`, `@Service`, `@Repository`,
/// `@Configuration`, `@RestController`, `@WebSocketMapping` -- and every `@Bean`
/// method is a bean. This works out, for each one, which constructor the
/// generated entry point calls and what it passes, which bean every `@Autowired`
/// field and setter receives, in what order they are built, and which of their
/// methods are scheduled, published as MCP tools or managed. Everything that
/// cannot be satisfied is a build error naming the injection point.
///
/// The result is written down as code by [BackendWiringWriter]: `new` calls,
/// setter calls and direct method calls, with no registry, lookup or reflection
/// left for run time. The same pass rewrites the compiled classes -- see
/// [BackendWeaver] -- and generates, as Java source, the small classes the
/// aspects, scopes and adapters need.
///
/// Computed once per build and shared through the processor context, because two
/// processors need it: the one that owns these annotations, which must rewrite
/// the classes even in a module with no controller, and the one that writes the
/// entry point.
final class BackendBeans {
    static final String ATTRIBUTE = "cn1.backend.beans";

    static final String PKG = "Lcom/codename1/backend/annotations/";
    static final String COMPONENT = PKG + "Component;";
    static final String SERVICE = PKG + "Service;";
    static final String REPOSITORY = PKG + "Repository;";
    static final String CONFIGURATION = PKG + "Configuration;";
    static final String BEAN = PKG + "Bean;";
    static final String AUTOWIRED = PKG + "Autowired;";
    static final String QUALIFIER = PKG + "Qualifier;";
    static final String PRIMARY = PKG + "Primary;";
    static final String LAZY = PKG + "Lazy;";
    static final String VALUE = PKG + "Value;";
    static final String CONFIG_PROPERTIES = PKG + "ConfigurationProperties;";
    static final String SCOPE = PKG + "Scope;";
    static final String REQUEST_SCOPE = PKG + "RequestScope;";
    static final String SESSION_SCOPE = PKG + "SessionScope;";
    static final String PROFILE = PKG + "Profile;";
    static final String ON_PROPERTY = PKG + "ConditionalOnProperty;";
    static final String ON_MISSING = PKG + "ConditionalOnMissingBean;";
    static final String POST_CONSTRUCT = PKG + "PostConstruct;";
    static final String PRE_DESTROY = PKG + "PreDestroy;";
    static final String TRANSACTIONAL = PKG + "Transactional;";
    static final String ASYNC = PKG + "Async;";
    static final String SCHEDULED = PKG + "Scheduled;";
    static final String MANAGED_RESOURCE = PKG + "ManagedResource;";
    static final String MANAGED_ATTRIBUTE = PKG + "ManagedAttribute;";
    static final String MANAGED_OPERATION = PKG + "ManagedOperation;";
    static final String TIMED = PKG + "Timed;";
    static final String COUNTED = PKG + "Counted;";
    static final String MCP_TOOL = PKG + "McpTool;";
    static final String MCP_PARAM = PKG + "McpParam;";
    static final String REST_CONTROLLER = PKG + "RestController;";
    static final String WEBSOCKET_MAPPING = PKG + "WebSocketMapping;";
    static final String GENERATED = PKG + "Generated;";

    /// Every annotation this pass reads, for the processor's declared interest.
    static final Set<String> DESCRIPTORS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(java.util.Arrays.asList(COMPONENT, SERVICE, REPOSITORY,
                    CONFIGURATION, BEAN, AUTOWIRED, VALUE, CONFIG_PROPERTIES, SCOPE,
                    REQUEST_SCOPE, SESSION_SCOPE, PROFILE, ON_PROPERTY, ON_MISSING,
                    POST_CONSTRUCT, PRE_DESTROY, TRANSACTIONAL, ASYNC, SCHEDULED,
                    MANAGED_RESOURCE, MANAGED_ATTRIBUTE, MANAGED_OPERATION, TIMED, COUNTED,
                    MCP_TOOL, REST_CONTROLLER, WEBSOCKET_MAPPING)));

    private static final String[] STEREOTYPES = {COMPONENT, SERVICE, REPOSITORY, CONFIGURATION,
            REST_CONTROLLER, WEBSOCKET_MAPPING};

    static final String CONFIG_TYPE = "com/codename1/backend/Config";
    static final String DATASOURCE_TYPE = "com/codename1/backend/DataSource";
    static final String ENTITIES_TYPE = "com/codename1/backend/orm/EntityManager";
    static final String SESSION_TYPE = "com/codename1/orm/session/Session";
    static final String REQUEST_TYPE = "com/codename1/backend/HttpServer$Request";
    static final String HTTP_SESSION_TYPE = "com/codename1/backend/HttpSession";

    static final String SINGLETON = "singleton";
    static final String PROTOTYPE = "prototype";
    static final String REQUEST = "request";
    static final String SESSION = "session";

    // ------------------------------------------------------------------ model

    /// One injection point: a constructor or method parameter, or a field.
    static final class Point {
        final String where;
        final Type type;
        /// The generic signature of the point's type, when there is one.
        final String genericType;
        String qualifier;
        boolean required = true;
        /// The @Value expression, or null.
        String value;
        /// The bean or beans it receives, once resolved.
        final List<Bean> candidates = new ArrayList<Bean>();
        /// A built-in: config, dataSource, entities, session, request, httpSession.
        String builtin;
        /// Whether the point takes every bean of its element type, as a List.
        boolean list;
        /// Whether several conditional candidates are chosen between at start-up.
        boolean choice;

        Point(String where, Type type, String genericType) {
            this.where = where;
            this.type = type;
            this.genericType = genericType;
        }
    }

    /// A method called with injected arguments: an `@Autowired` setter.
    static final class Call {
        final MethodInfo method;
        final List<Point> points = new ArrayList<Point>();

        Call(MethodInfo method) {
            this.method = method;
        }
    }

    /// One `@Scheduled` method.
    static final class Job {
        final MethodInfo method;
        final String name;
        String cron;
        CronCompiler masks;
        String zone = "";
        long fixedRate = -1;
        long fixedDelay = -1;
        long initialDelay = -1;
        String fixedRateText;
        String fixedDelayText;
        String initialDelayText;
        String thread = "PLATFORM";
        String executor = "";
        String lock = "";
        long lockAtMostFor = -1;

        Job(MethodInfo method, String name) {
            this.method = method;
            this.name = name;
        }
    }

    /// One `@McpTool` method.
    static final class Tool {
        final MethodInfo method;
        final String name;
        final String description;
        final List<String> paramNames = new ArrayList<String>();
        final List<String> paramDescriptions = new ArrayList<String>();
        final List<Boolean> paramRequired = new ArrayList<Boolean>();
        String adapterBinary;

        Tool(MethodInfo method, String name, String description) {
            this.method = method;
            this.name = name;
            this.description = description;
        }
    }

    /// A `@ManagedResource` bean's attributes and operations.
    static final class Managed {
        String objectName;
        String description;
        final List<MethodInfo> attributes = new ArrayList<MethodInfo>();
        final List<String> attributeNames = new ArrayList<String>();
        final List<String> attributeDescriptions = new ArrayList<String>();
        final List<String> attributeUnits = new ArrayList<String>();
        final List<MethodInfo> operations = new ArrayList<MethodInfo>();
        final List<String> operationDescriptions = new ArrayList<String>();
        final List<List<String>> operationParams = new ArrayList<List<String>>();
        String adapterBinary;
    }

    /// One bean.
    static final class Bean {
        String name;
        /// Internal name of the bean's type: the class, or a factory's return type.
        String type;
        /// The class, when it is in the project or on the classpath.
        AnnotatedClass cls;
        /// For a factory bean: the configuration bean and method, and the class
        /// that declares the method -- all a static factory needs.
        Bean owner;
        MethodInfo factory;
        AnnotatedClass factoryOwnerClass;
        String scope = SINGLETON;
        boolean primary;
        boolean lazy;
        boolean onMissing;
        String[] profiles;
        final List<String[]> propertyConditions = new ArrayList<String[]>();
        MethodInfo constructor;
        final List<Point> constructorPoints = new ArrayList<Point>();
        final Map<FieldInfo, Point> fields = new LinkedHashMap<FieldInfo, Point>();
        final List<Call> setters = new ArrayList<Call>();
        final List<MethodInfo> postConstruct = new ArrayList<MethodInfo>();
        final List<MethodInfo> preDestroy = new ArrayList<MethodInfo>();
        String initMethod;
        String destroyMethod;
        String propertiesPrefix;
        final List<MethodInfo> propertySetters = new ArrayList<MethodInfo>();
        final Set<String> types = new LinkedHashSet<String>();
        String var;
        boolean controller;
        boolean webSocket;
        final List<Job> jobs = new ArrayList<Job>();
        final List<Tool> tools = new ArrayList<Tool>();
        Managed managed;
        /// Request, session and lazy beans: the number the build gives each.
        int slot = -1;
        /// The generated stand-in class, for a request, session or lazy bean.
        String proxyBinary;

        boolean isConditional() {
            return profiles != null || !propertyConditions.isEmpty();
        }

        /// Whether it is reached through a generated stand-in rather than held.
        boolean isProxied() {
            return REQUEST.equals(scope) || SESSION.equals(scope) || lazy;
        }

        boolean isEager() {
            return SINGLETON.equals(scope) && !lazy;
        }

        String describe() {
            return name + " (" + type.replace('/', '.') + ")";
        }
    }

    /// One class's aspects, for the helper class and the weaver.
    static final class Aspects {
        final AnnotatedClass cls;
        final String helperBinary;
        final List<Aspect> methods = new ArrayList<Aspect>();

        Aspects(AnnotatedClass cls, String helperBinary) {
            this.cls = cls;
            this.helperBinary = helperBinary;
        }
    }

    /// The aspects of one method.
    static final class Aspect {
        final MethodInfo method;
        AnnotationValues transactional;
        AnnotationValues async;
        AnnotationValues timed;
        AnnotationValues counted;
        String asyncTaskBinary;

        Aspect(MethodInfo method) {
            this.method = method;
        }
    }

    // ------------------------------------------------------------------ state

    final ProcessorContext ctx;
    final List<Bean> beans = new ArrayList<Bean>();
    final Map<String, Bean> byName = new LinkedHashMap<String, Bean>();
    final Map<String, Aspects> aspects = new TreeMap<String, Aspects>();
    final Map<String, BackendWeaver.Plan> plans = new TreeMap<String, BackendWeaver.Plan>();
    /// Eager singletons (and the prototypes they need) in construction order.
    final List<Bean> order = new ArrayList<Bean>();
    boolean needsDatabase;
    boolean needsEntities;
    boolean needsSession;
    int requestSlots;
    int sessionSlots;
    int lazySlots;
    /// The package the entry point is written into.
    String entryPackage;
    /// Sources this pass compiled, by binary name, so tests can read them.
    final Map<String, String> sources = new LinkedHashMap<String, String>();

    private BackendBeans(ProcessorContext ctx) {
        this.ctx = ctx;
    }

    /// The beans of this build: computed, validated, woven and their support
    /// classes compiled on the first call, and the same object after that.
    static BackendBeans prepare(ProcessorContext ctx) throws ProcessingException {
        Object cached = ctx.getAttribute(ATTRIBUTE);
        if (cached instanceof BackendBeans) {
            return (BackendBeans) cached;
        }
        BackendBeans out = new BackendBeans(ctx);
        ctx.setAttribute(ATTRIBUTE, out);
        if (!usesAnyAnnotation(ctx)) {
            // Every project runs this processor -- an app's client module too --
            // and nearly all of them use none of this. Settled in memory, before
            // any source file is looked at.
            return out;
        }
        out.discover();
        if (!ctx.hasErrors()) {
            out.resolve();
        }
        if (!ctx.hasErrors()) {
            out.collectAspects();
        }
        if (!ctx.hasErrors()) {
            out.plan();
        }
        if (!ctx.hasErrors()) {
            out.emit();
        }
        return out;
    }

    private static boolean usesAnyAnnotation(ProcessorContext ctx) {
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            for (String d : cls.getAllAnnotationDescriptors()) {
                if (DESCRIPTORS.contains(d)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Whether this build has anything for the entry point to wire beyond what
    /// the controllers and endpoints themselves need.
    boolean hasApplicationBeans() {
        for (Bean b : beans) {
            if (!b.controller && !b.webSocket) {
                return true;
            }
        }
        return false;
    }

    boolean hasJobs() {
        for (Bean b : beans) {
            if (!b.jobs.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    boolean hasTools() {
        for (Bean b : beans) {
            if (!b.tools.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /// The bean for a controller or endpoint class, or null.
    Bean beanOfClass(String binaryName) {
        String internal = binaryName.replace('.', '/');
        for (Bean b : beans) {
            if (b.factory == null && b.type.equals(internal)) {
                return b;
            }
        }
        return null;
    }

    // --------------------------------------------------------------- discovery

    private void discover() {
        List<AnnotatedClass> classes = new ArrayList<AnnotatedClass>(ctx.getClassIndex().values());
        Collections.sort(classes, new java.util.Comparator<AnnotatedClass>() {
            public int compare(AnnotatedClass a, AnnotatedClass b) {
                return a.getInternalName().compareTo(b.getInternalName());
            }
        });
        for (AnnotatedClass cls : classes) {
            if (!concerns(cls)) {
                continue;
            }
            if (!isStereotyped(cls)) {
                continue;
            }
            if (cls.isInterface() || cls.isAbstract()) {
                ctx.error(cls, "A bean must be a concrete class; " + cls.getBinaryName()
                        + " is " + (cls.isInterface() ? "an interface" : "abstract")
                        + ". Annotate its implementation instead.");
                continue;
            }
            if (isInnerClass(cls)) {
                ctx.error(cls, cls.getSourceName() + " is an inner class, so it cannot be "
                        + "built without an instance of the class around it. Make it static.");
                continue;
            }
            Bean bean = classBean(cls);
            if (bean != null) {
                add(bean);
            }
        }
        // Factory methods second, so their owners exist.
        for (Bean owner : new ArrayList<Bean>(beans)) {
            if (owner.cls == null) {
                continue;
            }
            for (MethodInfo m : owner.cls.getMethods()) {
                if (m.getAnnotation(BEAN) != null) {
                    Bean bean = factoryBean(owner, m);
                    if (bean != null) {
                        add(bean);
                    }
                }
            }
        }
        dropSteppedAside();
        pickEntryPackage();
    }

    /// Whether the class belongs to this build: a source still backs it, and it
    /// is not one of the classes the processors generate.
    private boolean concerns(AnnotatedClass cls) {
        if (cls.isSynthetic() || cls.getClassAnnotation(GENERATED) != null) {
            return false;
        }
        return BuildHintAnnotationProcessor.hasBackingSource(cls, ctx.getCompileSourceRoots(),
                ctx.getSourceEncoding());
    }

    private static boolean isStereotyped(AnnotatedClass cls) {
        for (String s : STEREOTYPES) {
            if (cls.getClassAnnotation(s) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInnerClass(AnnotatedClass cls) {
        for (FieldInfo f : cls.getFields()) {
            if (f.getName().startsWith("this$") && (f.getAccess() & Opcodes.ACC_SYNTHETIC) != 0) {
                return true;
            }
        }
        return false;
    }

    private void add(Bean bean) {
        Bean clash = byName.get(bean.name);
        if (clash != null) {
            ctx.error(bean.cls != null ? bean.cls : bean.owner.cls, "Two beans are named \""
                    + bean.name + "\": " + clash.describe() + " and " + bean.describe()
                    + ". Give one a name of its own in its annotation.");
            return;
        }
        byName.put(bean.name, bean);
        beans.add(bean);
    }

    private Bean classBean(AnnotatedClass cls) {
        Bean bean = new Bean();
        bean.cls = cls;
        bean.type = cls.getInternalName();
        bean.name = explicitName(cls);
        if (bean.name == null || bean.name.length() == 0) {
            bean.name = decapitalize(RestClientAnnotationProcessor.simpleName(
                    cls.getBinaryName().replace('$', '.')));
        }
        bean.controller = cls.getClassAnnotation(REST_CONTROLLER) != null;
        bean.webSocket = cls.getClassAnnotation(WEBSOCKET_MAPPING) != null;
        readModifiers(bean, cls.getClassAnnotations(), cls);
        bean.types.addAll(assignableTypes(cls.getInternalName()));
        if (!chooseConstructor(bean)) {
            return null;
        }
        for (FieldInfo f : cls.getFields()) {
            boolean autowired = f.getAnnotation(AUTOWIRED) != null;
            AnnotationValues value = f.getAnnotation(VALUE);
            if (!autowired && value == null) {
                continue;
            }
            String where = "field " + f.getName() + " of " + cls.getSourceName();
            if (f.isStatic()) {
                ctx.error(cls, "The build injects instances, and " + where + " is static. "
                        + "Make it an instance field, or give the value to an instance.");
                continue;
            }
            if (f.isFinal()) {
                ctx.error(cls, where + " is final, so it can only be set by the constructor. "
                        + "Take it as a constructor parameter, or drop the final.");
                continue;
            }
            Point p = new Point(where, Type.getType(f.getDescriptor()), f.getSignature());
            readPoint(p, f.getAnnotations());
            bean.fields.put(f, p);
        }
        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor()) {
                continue;
            }
            String where = cls.getSourceName() + "." + m.getName();
            if (m.getAnnotation(AUTOWIRED) != null) {
                if (m.isStatic()) {
                    ctx.error(cls, "@Autowired method " + where + " is static; the build "
                            + "injects instances.");
                    continue;
                }
                Call call = new Call(m);
                Type[] args = Type.getArgumentTypes(m.getDescriptor());
                String[] generics = parameterSignatures(m);
                for (int i = 0; i < args.length; i++) {
                    Point p = new Point("parameter " + (i + 1) + " of " + where, args[i],
                            generics == null ? null : generics[i]);
                    readPoint(p, parameterAnnotations(m, i));
                    call.points.add(p);
                }
                bean.setters.add(call);
            }
            if (m.getAnnotation(POST_CONSTRUCT) != null && lifecycle(cls, m, "@PostConstruct")) {
                bean.postConstruct.add(m);
            }
            if (m.getAnnotation(PRE_DESTROY) != null && lifecycle(cls, m, "@PreDestroy")) {
                bean.preDestroy.add(m);
            }
        }
        AnnotationValues props = cls.getClassAnnotation(CONFIG_PROPERTIES);
        if (props != null) {
            bindProperties(bean, props, cls);
        }
        collectJobs(bean);
        collectTools(bean);
        collectManaged(bean);
        return bean;
    }

    private boolean lifecycle(AnnotatedClass cls, MethodInfo m, String what) {
        if (m.isStatic() || Type.getArgumentTypes(m.getDescriptor()).length != 0) {
            ctx.error(cls, what + " method " + cls.getSourceName() + "." + m.getName()
                    + " must be an instance method that takes no arguments.");
            return false;
        }
        return true;
    }

    private static String explicitName(AnnotatedClass cls) {
        for (String s : STEREOTYPES) {
            AnnotationValues v = cls.getClassAnnotation(s);
            if (v != null && v.getString("value") != null && !REST_CONTROLLER.equals(s)
                    && !WEBSOCKET_MAPPING.equals(s)) {
                String name = v.getString("value").trim();
                if (name.length() > 0) {
                    return name;
                }
            }
        }
        return null;
    }

    /// Scope, primary, lazy and the conditions, from a class's or a factory
    /// method's annotations.
    private void readModifiers(Bean bean, Map<String, AnnotationValues> annotations,
                               AnnotatedClass where) {
        AnnotationValues scope = annotations.get(SCOPE);
        if (scope != null) {
            String s = scope.getStringOrDefault("value", SINGLETON).trim();
            if (!SINGLETON.equals(s) && !PROTOTYPE.equals(s) && !REQUEST.equals(s)
                    && !SESSION.equals(s)) {
                ctx.error(where, "@Scope(\"" + s + "\") on " + bean.name + " names no scope "
                        + "this runtime has; use singleton, prototype, request or session.");
            }
            bean.scope = s;
        }
        if (annotations.get(REQUEST_SCOPE) != null) {
            bean.scope = REQUEST;
        }
        if (annotations.get(SESSION_SCOPE) != null) {
            bean.scope = SESSION;
        }
        bean.primary = annotations.get(PRIMARY) != null;
        AnnotationValues lazy = annotations.get(LAZY);
        bean.lazy = lazy != null && lazy.getBoolOrDefault("value", true);
        if (bean.lazy && !SINGLETON.equals(bean.scope)) {
            // A request, session or prototype bean is already built on demand.
            bean.lazy = false;
        }
        bean.onMissing = annotations.get(ON_MISSING) != null;
        AnnotationValues profile = annotations.get(PROFILE);
        if (profile != null) {
            List<String> values = strings(profile.get("value"));
            if (values.isEmpty()) {
                ctx.error(where, "@Profile on " + bean.name + " names no profile.");
            }
            bean.profiles = values.toArray(new String[values.size()]);
        }
        AnnotationValues prop = annotations.get(ON_PROPERTY);
        if (prop != null) {
            List<String> keys = strings(prop.get("value"));
            keys.addAll(strings(prop.get("name")));
            String prefix = prop.getStringOrDefault("prefix", "");
            if (keys.isEmpty()) {
                ctx.error(where, "@ConditionalOnProperty on " + bean.name + " names no key.");
            }
            for (String k : keys) {
                String key = prefix.length() == 0 ? k : prefix + "." + k;
                bean.propertyConditions.add(new String[] {key,
                        prop.getStringOrDefault("havingValue", ""),
                        String.valueOf(prop.getBoolOrDefault("matchIfMissing", false))});
            }
        }
    }

    private boolean chooseConstructor(Bean bean) {
        AnnotatedClass cls = bean.cls;
        List<MethodInfo> constructors = new ArrayList<MethodInfo>();
        List<MethodInfo> marked = new ArrayList<MethodInfo>();
        MethodInfo noArg = null;
        for (MethodInfo m : cls.getMethods()) {
            if (!m.isConstructor() || m.isSynthetic()) {
                continue;
            }
            constructors.add(m);
            if (m.getAnnotation(AUTOWIRED) != null) {
                marked.add(m);
            }
            if (Type.getArgumentTypes(m.getDescriptor()).length == 0) {
                noArg = m;
            }
        }
        MethodInfo chosen;
        if (marked.size() > 1) {
            ctx.error(cls, cls.getSourceName() + " marks " + marked.size() + " constructors "
                    + "@Autowired; the build can call only one. Mark one.");
            return false;
        } else if (marked.size() == 1) {
            chosen = marked.get(0);
        } else if (constructors.size() == 1) {
            chosen = constructors.get(0);
        } else if (noArg != null) {
            chosen = noArg;
        } else {
            ctx.error(cls, cls.getSourceName() + " has " + constructors.size()
                    + " constructors and none is marked @Autowired or takes no arguments, so "
                    + "the build cannot tell which one to call. Mark one @Autowired.");
            return false;
        }
        bean.constructor = chosen;
        Type[] args = Type.getArgumentTypes(chosen.getDescriptor());
        String[] generics = parameterSignatures(chosen);
        for (int i = 0; i < args.length; i++) {
            Point p = new Point("constructor parameter " + (i + 1) + " of " + cls.getSourceName(),
                    args[i], generics == null ? null : generics[i]);
            readPoint(p, parameterAnnotations(chosen, i));
            bean.constructorPoints.add(p);
        }
        return true;
    }

    private Bean factoryBean(Bean owner, MethodInfo m) {
        AnnotatedClass cls = owner.cls;
        String where = cls.getSourceName() + "." + m.getName();
        Type ret = Type.getReturnType(m.getDescriptor());
        if (ret.getSort() != Type.OBJECT && ret.getSort() != Type.ARRAY) {
            ctx.error(cls, "@Bean method " + where + " returns " + ret.getClassName()
                    + "; a bean is an object.");
            return null;
        }
        if (ret.getSort() == Type.ARRAY) {
            ctx.error(cls, "@Bean method " + where + " returns an array; return a List or "
                    + "an object holding it.");
            return null;
        }
        if (m.isAbstract()) {
            ctx.error(cls, "@Bean method " + where + " is abstract.");
            return null;
        }
        Bean bean = new Bean();
        AnnotationValues values = m.getAnnotation(BEAN);
        String name = values.getStringOrDefault("value", "").trim();
        bean.name = name.length() > 0 ? name : m.getName();
        bean.type = ret.getInternalName();
        bean.owner = m.isStatic() ? null : owner;
        bean.factory = m;
        bean.cls = RestControllerAnnotationProcessor.resolveClass(ctx, bean.type);
        bean.initMethod = emptyToNull(values.getString("initMethod"));
        bean.destroyMethod = emptyToNull(values.getString("destroyMethod"));
        readModifiers(bean, m.getAnnotations(), cls);
        bean.types.addAll(assignableTypes(bean.type));
        Type[] args = Type.getArgumentTypes(m.getDescriptor());
        String[] generics = parameterSignatures(m);
        for (int i = 0; i < args.length; i++) {
            Point p = new Point("parameter " + (i + 1) + " of @Bean " + where, args[i],
                    generics == null ? null : generics[i]);
            readPoint(p, parameterAnnotations(m, i));
            bean.constructorPoints.add(p);
        }
        if (bean.cls != null && ctx.lookup(bean.type) != null) {
            for (MethodInfo lm : bean.cls.getMethods()) {
                if (lm.getAnnotation(POST_CONSTRUCT) != null
                        && lifecycle(bean.cls, lm, "@PostConstruct")) {
                    bean.postConstruct.add(lm);
                }
                if (lm.getAnnotation(PRE_DESTROY) != null
                        && lifecycle(bean.cls, lm, "@PreDestroy")) {
                    bean.preDestroy.add(lm);
                }
            }
        }
        AnnotationValues props = m.getAnnotation(CONFIG_PROPERTIES);
        if (props != null) {
            if (bean.cls == null) {
                ctx.error(cls, "@ConfigurationProperties on " + where + " needs the returned "
                        + "class's setters, and " + bean.type.replace('/', '.')
                        + " is not on the build's classpath.");
            } else {
                bindProperties(bean, props, cls);
            }
        }
        // The declaring class: a static @Bean method needs no instance of it,
        // but it still lives there.
        bean.factoryOwnerClass = cls;
        return bean;
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().length() == 0 ? null : s.trim();
    }

    private void readPoint(Point p, Map<String, AnnotationValues> annotations) {
        if (annotations == null) {
            return;
        }
        AnnotationValues q = annotations.get(QUALIFIER);
        if (q != null) {
            p.qualifier = q.getString("value");
        }
        AnnotationValues a = annotations.get(AUTOWIRED);
        if (a != null) {
            p.required = a.getBoolOrDefault("required", true);
        }
        AnnotationValues v = annotations.get(VALUE);
        if (v != null) {
            p.value = v.getString("value");
        }
    }

    private void bindProperties(Bean bean, AnnotationValues props, AnnotatedClass where) {
        String prefix = props.getStringOrDefault("value", "");
        if (prefix.length() == 0) {
            prefix = props.getStringOrDefault("prefix", "");
        }
        while (prefix.endsWith(".")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        bean.propertiesPrefix = prefix;
        Set<String> seen = new LinkedHashSet<String>();
        AnnotatedClass c = bean.cls;
        while (c != null) {
            for (MethodInfo m : c.getMethods()) {
                if (!m.isPublic() || m.isStatic() || !m.getName().startsWith("set")
                        || m.getName().length() < 4) {
                    continue;
                }
                Type[] args = Type.getArgumentTypes(m.getDescriptor());
                if (args.length != 1 || !bindable(args[0])) {
                    continue;
                }
                if (seen.add(m.getName() + m.getDescriptor())) {
                    bean.propertySetters.add(m);
                }
            }
            String parent = c.getSuperInternalName();
            c = parent == null || "java/lang/Object".equals(parent) ? null : ctx.lookup(parent);
        }
        if (bean.propertySetters.isEmpty()) {
            ctx.error(where, "@ConfigurationProperties(\"" + prefix + "\") on " + bean.name
                    + " binds nothing: " + bean.type.replace('/', '.') + " has no public "
                    + "setter taking a String, a number, a boolean or an enum.");
        }
    }

    /// Whether a configuration value can be converted to this type.
    boolean bindable(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.LONG:
            case Type.FLOAT:
            case Type.DOUBLE:
                return true;
            case Type.OBJECT:
                String n = t.getInternalName();
                if (n.equals("java/lang/String") || n.equals("java/lang/Integer")
                        || n.equals("java/lang/Long") || n.equals("java/lang/Boolean")
                        || n.equals("java/lang/Double") || n.equals("java/lang/Float")
                        || n.equals("java/lang/Short") || n.equals("java/lang/Byte")
                        || n.equals("java/lang/Character")) {
                    return true;
                }
                AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(ctx, n);
                return c != null && c.isEnum();
            default:
                return false;
        }
    }

    /// A `@ConditionalOnMissingBean` bean steps aside for any other bean of its type.
    private void dropSteppedAside() {
        List<Bean> drop = new ArrayList<Bean>();
        for (Bean b : beans) {
            if (!b.onMissing) {
                continue;
            }
            for (Bean other : beans) {
                if (other != b && !other.onMissing && other.types.contains(b.type)) {
                    drop.add(b);
                    break;
                }
            }
        }
        for (Bean b : drop) {
            beans.remove(b);
            byName.remove(b.name);
            ctx.getLog().info("cn1: " + b.describe() + " steps aside: another bean has its type");
        }
    }

    /// Where the entry point goes: the first controller's package, as it always
    /// was, then the first endpoint's, then the first bean's.
    private void pickEntryPackage() {
        String best = null;
        for (int pass = 0; pass < 3 && best == null; pass++) {
            String first = null;
            for (Bean b : beans) {
                if (b.factory != null) {
                    continue;
                }
                boolean eligible = pass == 0 ? b.controller : pass == 1 ? b.webSocket : true;
                String binary = b.type.replace('/', '.');
                if (eligible && (first == null || binary.compareTo(first) < 0)) {
                    first = binary;
                }
            }
            if (first != null) {
                best = RestClientAnnotationProcessor.packageOf(first);
            }
        }
        entryPackage = best;
    }

    /// Scheduled, tool and managed methods, for a bean class.
    private void collectJobs(Bean bean) {
        AnnotatedClass cls = bean.cls;
        for (MethodInfo m : cls.getMethods()) {
            AnnotationValues s = m.getAnnotation(SCHEDULED);
            if (s == null) {
                continue;
            }
            String where = cls.getSourceName() + "." + m.getName();
            if (m.isStatic() || Type.getArgumentTypes(m.getDescriptor()).length != 0) {
                ctx.error(cls, "@Scheduled method " + where + " must be an instance method "
                        + "that takes no arguments.");
                continue;
            }
            Job job = new Job(m, RestClientAnnotationProcessor.simpleName(
                    cls.getBinaryName().replace('$', '.')) + "." + m.getName());
            job.cron = emptyToNull(s.getString("cron"));
            job.zone = s.getStringOrDefault("zone", "").trim();
            job.fixedRate = longOf(s.get("fixedRate"));
            job.fixedDelay = longOf(s.get("fixedDelay"));
            job.initialDelay = longOf(s.get("initialDelay"));
            job.fixedRateText = emptyToNull(s.getString("fixedRateString"));
            job.fixedDelayText = emptyToNull(s.getString("fixedDelayString"));
            job.initialDelayText = emptyToNull(s.getString("initialDelayString"));
            job.thread = enumName(s.get("thread"), "PLATFORM");
            job.executor = s.getStringOrDefault("executor", "");
            job.lock = s.getStringOrDefault("lock", "");
            job.lockAtMostFor = longOf(s.get("lockAtMostFor"));
            int kinds = (job.cron != null ? 1 : 0)
                    + (job.fixedRate > 0 || job.fixedRateText != null ? 1 : 0)
                    + (job.fixedDelay > 0 || job.fixedDelayText != null ? 1 : 0);
            if (kinds != 1) {
                ctx.error(cls, "@Scheduled on " + where + " must give exactly one of cron, "
                        + "fixedRate and fixedDelay; it gives " + kinds + ".");
                continue;
            }
            if (job.fixedRateText != null && job.fixedRate > 0
                    || job.fixedDelayText != null && job.fixedDelay > 0) {
                ctx.error(cls, "@Scheduled on " + where + " gives a period both as a number "
                        + "and as text; keep one.");
                continue;
            }
            if (job.cron != null && job.cron.indexOf("${") < 0) {
                try {
                    job.masks = CronCompiler.compile(job.cron);
                } catch (IllegalArgumentException err) {
                    ctx.error(cls, "@Scheduled on " + where + ": " + err.getMessage() + ".");
                    continue;
                }
            }
            if (job.cron == null && job.zone.length() > 0) {
                ctx.error(cls, "@Scheduled on " + where + " sets a zone, which only a cron "
                        + "expression uses.");
                continue;
            }
            if (!CronCompiler.knownZone(job.zone)) {
                ctx.error(cls, "@Scheduled on " + where + " names time zone \"" + job.zone
                        + "\", which is not a zone ID, UTC, or an offset such as +02:00.");
                continue;
            }
            if (job.lock.length() > 0) {
                needsDatabase = true;
            }
            bean.jobs.add(job);
        }
    }

    private void collectTools(Bean bean) {
        AnnotatedClass cls = bean.cls;
        for (MethodInfo m : cls.getMethods()) {
            AnnotationValues t = m.getAnnotation(MCP_TOOL);
            if (t == null) {
                continue;
            }
            String where = cls.getSourceName() + "." + m.getName();
            if (m.isStatic()) {
                ctx.error(cls, "@McpTool method " + where + " must be an instance method.");
                continue;
            }
            String name = t.getStringOrDefault("name", "").trim();
            Tool tool = new Tool(m, name.length() > 0 ? name : m.getName(),
                    t.getStringOrDefault("description", ""));
            if (!tool.name.matches("[A-Za-z0-9_.-]{1,128}")) {
                ctx.error(cls, "@McpTool on " + where + " is named \"" + tool.name + "\"; a "
                        + "tool name is letters, digits, _, - and . only.");
                continue;
            }
            Type[] args = Type.getArgumentTypes(m.getDescriptor());
            boolean ok = true;
            for (int i = 0; i < args.length; i++) {
                AnnotationValues p = parameterAnnotations(m, i).get(MCP_PARAM);
                if (p == null) {
                    ctx.error(cls, "Parameter " + (i + 1) + " of @McpTool " + where + " has no "
                            + "@McpParam. A Java parameter name does not survive compilation, "
                            + "so the tool's argument needs one to be called by.");
                    ok = false;
                    continue;
                }
                if (!toolArgument(args[i])) {
                    ctx.error(cls, "Parameter " + (i + 1) + " of @McpTool " + where + " is a "
                            + args[i].getClassName() + "; a tool argument is a String, a "
                            + "number, a boolean, an enum, a Map or a List.");
                    ok = false;
                    continue;
                }
                tool.paramNames.add(p.getString("value"));
                tool.paramDescriptions.add(p.getStringOrDefault("description", ""));
                tool.paramRequired.add(Boolean.valueOf(p.getBoolOrDefault("required", true)));
            }
            if (ok) {
                bean.tools.add(tool);
            }
        }
    }

    boolean toolArgument(Type t) {
        if (bindable(t)) {
            return true;
        }
        if (t.getSort() != Type.OBJECT) {
            return false;
        }
        String n = t.getInternalName();
        return n.equals("java/util/Map") || n.equals("java/util/List")
                || n.equals("java/util/Collection") || n.equals("java/lang/Object");
    }

    private void collectManaged(Bean bean) {
        AnnotatedClass cls = bean.cls;
        AnnotationValues resource = cls.getClassAnnotation(MANAGED_RESOURCE);
        boolean any = resource != null;
        for (MethodInfo m : cls.getMethods()) {
            if (m.getAnnotation(MANAGED_ATTRIBUTE) != null
                    || m.getAnnotation(MANAGED_OPERATION) != null) {
                if (resource == null) {
                    ctx.error(cls, cls.getSourceName() + "." + m.getName() + " is managed, but "
                            + "its class has no @ManagedResource.");
                    return;
                }
            }
        }
        if (!any) {
            return;
        }
        Managed managed = new Managed();
        String objectName = resource.getStringOrDefault("objectName", "").trim();
        managed.objectName = objectName.length() > 0 ? objectName
                : RestClientAnnotationProcessor.simpleName(cls.getBinaryName().replace('$', '.'));
        managed.description = resource.getStringOrDefault("description", "");
        for (MethodInfo m : cls.getMethods()) {
            String where = cls.getSourceName() + "." + m.getName();
            AnnotationValues attr = m.getAnnotation(MANAGED_ATTRIBUTE);
            if (attr != null) {
                Type ret = Type.getReturnType(m.getDescriptor());
                if (m.isStatic() || Type.getArgumentTypes(m.getDescriptor()).length != 0
                        || ret.getSort() == Type.VOID || ret.getSort() == Type.ARRAY) {
                    ctx.error(cls, "@ManagedAttribute " + where + " must be an instance getter "
                            + "that takes no arguments and returns a value.");
                    continue;
                }
                managed.attributes.add(m);
                managed.attributeNames.add(attributeName(m.getName()));
                managed.attributeDescriptions.add(attr.getStringOrDefault("description", ""));
                managed.attributeUnits.add(attr.getStringOrDefault("unit", ""));
            }
            AnnotationValues op = m.getAnnotation(MANAGED_OPERATION);
            if (op != null) {
                if (m.isStatic()) {
                    ctx.error(cls, "@ManagedOperation " + where + " must be an instance method.");
                    continue;
                }
                Type[] args = Type.getArgumentTypes(m.getDescriptor());
                List<String> names = new ArrayList<String>();
                boolean ok = true;
                for (int i = 0; i < args.length; i++) {
                    if (!bindable(args[i])) {
                        ctx.error(cls, "Parameter " + (i + 1) + " of @ManagedOperation " + where
                                + " is a " + args[i].getClassName() + "; an operation takes "
                                + "strings, numbers, booleans and enums.");
                        ok = false;
                    }
                    AnnotationValues named = parameterAnnotations(m, i).get(MCP_PARAM);
                    names.add(named != null ? named.getString("value") : "arg" + i);
                }
                if (ok) {
                    managed.operations.add(m);
                    managed.operationDescriptions.add(op.getStringOrDefault("description", ""));
                    managed.operationParams.add(names);
                }
            }
        }
        bean.managed = managed;
    }

    static String attributeName(String getter) {
        String base = getter;
        if (getter.startsWith("get") && getter.length() > 3) {
            base = getter.substring(3);
        } else if (getter.startsWith("is") && getter.length() > 2) {
            base = getter.substring(2);
        }
        return decapitalize(base);
    }

    // ---------------------------------------------------------------- resolve

    private void resolve() {
        // Names for the generated code, once the set of beans is final.
        Set<String> used = new LinkedHashSet<String>();
        for (Bean b : beans) {
            String base = "b_" + identifier(b.name);
            String var = base;
            int n = 2;
            while (!used.add(var)) {
                var = base + n++;
            }
            b.var = var;
            if (REQUEST.equals(b.scope)) {
                b.slot = requestSlots++;
            } else if (SESSION.equals(b.scope)) {
                b.slot = sessionSlots++;
            } else if (b.lazy) {
                b.slot = lazySlots++;
            }
        }
        for (Bean b : beans) {
            checkAccessibility(b);
            AnnotatedClass where = b.cls != null && ctx.lookup(b.type) != null ? b.cls
                    : b.factoryOwnerClass;
            for (Point p : b.constructorPoints) {
                resolvePoint(b, p, where);
            }
            for (Point p : b.fields.values()) {
                resolvePoint(b, p, where);
            }
            for (Call c : b.setters) {
                for (Point p : c.points) {
                    resolvePoint(b, p, where);
                }
            }
            if (b.isProxied()) {
                checkProxyable(b);
            }
            if (b.webSocket && !SINGLETON.equals(b.scope)) {
                ctx.error(b.cls, "Websocket endpoint " + b.describe() + " is "
                        + b.scope + "-scoped; an endpoint serves many connections for the "
                        + "life of the server, so it must be a singleton.");
            }
            if (!b.jobs.isEmpty() && (REQUEST.equals(b.scope) || SESSION.equals(b.scope))) {
                ctx.error(b.cls, b.describe() + " has @Scheduled methods but is " + b.scope
                        + "-scoped; a job runs outside any request.");
            }
        }
        if (!ctx.hasErrors()) {
            orderConstruction();
        }
    }

    /// The generated wiring names the bean's type, so the type must be visible
    /// from the entry package.
    private void checkAccessibility(Bean b) {
        if (b.cls == null || entryPackage == null) {
            return;
        }
        String pkg = RestClientAnnotationProcessor.packageOf(b.type.replace('/', '.'));
        if (!b.cls.isAccessibleFromAnywhere() && !pkg.equals(entryPackage)) {
            AnnotatedClass where = ctx.lookup(b.type) != null ? b.cls : b.factoryOwnerClass;
            ctx.error(where, "Bean " + b.describe() + " is not public, and the generated "
                    + "entry point is in package " + (entryPackage.length() == 0 ? "(default)"
                    : entryPackage) + ", where it cannot be named. Make the class public"
                    + (b.type.indexOf('$') >= 0 ? ", along with the classes it is nested in"
                    : "") + ".");
        }
    }

    private void resolvePoint(Bean owner, Point p, AnnotatedClass where) {
        if (p.value != null) {
            if (!bindable(p.type)) {
                ctx.error(where, "@Value on " + p.where + " cannot convert text to "
                        + p.type.getClassName() + "; use a String, a number, a boolean or an "
                        + "enum.");
            }
            warnIfUnset(p, where);
            return;
        }
        if (p.type.getSort() != Type.OBJECT) {
            ctx.error(where, p.where + " is a " + p.type.getClassName() + ", which no bean "
                    + "can be. Give it @Value(\"${some.key}\") to read it from configuration.");
            return;
        }
        String type = p.type.getInternalName();
        if (CONFIG_TYPE.equals(type)) {
            p.builtin = "config";
            return;
        }
        if (DATASOURCE_TYPE.equals(type)) {
            p.builtin = "dataSource";
            needsDatabase = true;
            return;
        }
        if (ENTITIES_TYPE.equals(type)) {
            p.builtin = "entities";
            needsDatabase = true;
            needsEntities = true;
            return;
        }
        if (SESSION_TYPE.equals(type)) {
            p.builtin = "session";
            needsDatabase = true;
            needsEntities = true;
            needsSession = true;
            return;
        }
        if (REQUEST_TYPE.equals(type) || HTTP_SESSION_TYPE.equals(type)) {
            if (!REQUEST.equals(owner.scope) && !SESSION.equals(owner.scope)) {
                ctx.error(where, p.where + " asks for the current "
                        + (REQUEST_TYPE.equals(type) ? "request" : "session") + ", which only "
                        + "a @RequestScope or @SessionScope bean has. Take it as a parameter of "
                        + "the handler method instead, or scope the bean.");
                return;
            }
            if (SESSION.equals(owner.scope) && REQUEST_TYPE.equals(type)) {
                ctx.error(where, p.where + " asks for the request, but a session bean outlives "
                        + "the request that built it. Take the HttpSession instead.");
                return;
            }
            p.builtin = REQUEST_TYPE.equals(type) ? "request" : "httpSession";
            return;
        }
        if (("java/util/List".equals(type) || "java/util/Collection".equals(type))
                && p.genericType != null) {
            String element = elementType(p.genericType);
            if (element != null) {
                p.list = true;
                for (Bean b : beans) {
                    if (b != owner && b.types.contains(element)
                            && (p.qualifier == null || p.qualifier.equals(b.name))) {
                        p.candidates.add(b);
                    }
                }
                return;
            }
        }
        List<Bean> matches = new ArrayList<Bean>();
        for (Bean b : beans) {
            if (b != owner && b.types.contains(type)) {
                matches.add(b);
            }
        }
        if (p.qualifier != null) {
            List<Bean> named = new ArrayList<Bean>();
            for (Bean b : matches) {
                if (b.name.equals(p.qualifier)) {
                    named.add(b);
                }
            }
            if (named.isEmpty() && p.required) {
                ctx.error(where, p.where + " asks for the bean named \"" + p.qualifier
                        + "\" of type " + p.type.getClassName() + ", and there is none"
                        + (matches.isEmpty() ? "" : "; the beans of that type are "
                        + names(matches)) + ".");
                return;
            }
            matches = named;
        }
        if (matches.isEmpty()) {
            if (p.required) {
                ctx.error(where, p.where + " needs a " + p.type.getClassName() + ", and no "
                        + "bean has that type. Annotate the implementing class @Component, "
                        + "@Service or @Repository, or declare a @Bean method returning one.");
            }
            return;
        }
        if (matches.size() > 1) {
            List<Bean> primaries = new ArrayList<Bean>();
            for (Bean b : matches) {
                if (b.primary) {
                    primaries.add(b);
                }
            }
            if (primaries.size() == 1) {
                matches = primaries;
            } else if (primaries.size() > 1) {
                ctx.error(where, p.where + " could receive any of " + names(primaries)
                        + ", which are all @Primary. Keep one, or name one with @Qualifier.");
                return;
            } else {
                boolean allConditional = true;
                for (Bean b : matches) {
                    allConditional &= b.isConditional();
                }
                if (!allConditional) {
                    ctx.error(where, p.where + " could receive any of " + names(matches)
                            + ". Mark one @Primary, or name one with @Qualifier.");
                    return;
                }
                p.choice = true;
            }
        }
        p.candidates.addAll(matches);
    }

    private void warnIfUnset(Point p, AnnotatedClass where) {
        String expression = p.value;
        int open = expression.indexOf("${");
        while (open >= 0) {
            int close = expression.indexOf('}', open);
            if (close < 0) {
                ctx.error(where, "@Value on " + p.where + " has an unterminated ${ in \""
                        + expression + "\".");
                return;
            }
            String inner = expression.substring(open + 2, close);
            if (inner.indexOf(':') < 0 && !RestControllerAnnotationProcessor
                    .applicationPropertyKnown(ctx, inner.trim())) {
                ctx.getLog().warn("cn1: @Value on " + p.where + " reads \"" + inner.trim()
                        + "\", which application.properties does not set and which has no "
                        + "fallback; the server refuses to start unless the environment "
                        + "sets it.");
            }
            open = expression.indexOf("${", close);
        }
    }

    private static String names(List<Bean> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(list.get(i).describe());
        }
        return sb.toString();
    }

    private void checkProxyable(Bean b) {
        String what = b.lazy ? "@Lazy" : "@" + (REQUEST.equals(b.scope) ? "Request" : "Session")
                + "Scope";
        AnnotatedClass where = ctx.lookup(b.type) != null ? b.cls : b.factoryOwnerClass;
        if (b.cls == null) {
            ctx.error(where, what + " bean " + b.describe() + " is reached through a class "
                    + "the build generates to stand in for it, which extends its type, and "
                    + "that type is not on the build's classpath.");
            return;
        }
        if (b.cls.isFinal() || b.cls.isInterface()) {
            ctx.error(where, what + " bean " + b.describe() + " is reached through a class "
                    + "the build generates to stand in for it, which extends it -- so it "
                    + "cannot be " + (b.cls.isFinal() ? "final" : "an interface") + ".");
            return;
        }
        MethodInfo noArg = null;
        for (MethodInfo m : b.cls.getMethods()) {
            if (m.isConstructor() && Type.getArgumentTypes(m.getDescriptor()).length == 0
                    && !m.isPrivate()) {
                noArg = m;
            }
        }
        if (noArg == null) {
            ctx.error(where, what + " bean " + b.describe() + " is reached through a class "
                    + "the build generates to stand in for it, which extends it and so must "
                    + "call one of its constructors. Give it a constructor that takes no "
                    + "arguments (it may be protected), and inject the rest with fields.");
            return;
        }
        for (MethodInfo m : proxiedMethods(b.cls, true)) {
            if (m.isFinal()) {
                ctx.error(where, what + " bean " + b.describe() + " has final method "
                        + m.getName() + ", which the stand-in cannot forward: a call to it "
                        + "would run on the stand-in rather than the bean. Drop the final.");
            }
        }
        String pkg = RestClientAnnotationProcessor.packageOf(b.type.replace('/', '.'));
        b.proxyBinary = qualify(pkg, baseName(b.type) + (b.lazy ? "Cn1Lazy" : "Cn1Scoped"));
    }

    /// The overridable methods of a class and its project superclasses.
    List<MethodInfo> proxiedMethods(AnnotatedClass cls, boolean includeFinal) {
        List<MethodInfo> out = new ArrayList<MethodInfo>();
        Set<String> seen = new LinkedHashSet<String>();
        AnnotatedClass c = cls;
        String pkg = RestClientAnnotationProcessor.packageOf(cls.getBinaryName());
        while (c != null) {
            boolean samePackage = RestClientAnnotationProcessor.packageOf(c.getBinaryName())
                    .equals(pkg);
            for (MethodInfo m : c.getMethods()) {
                if (m.isConstructor() || m.isStatic() || m.isPrivate() || m.isSynthetic()
                        || "<clinit>".equals(m.getName())
                        || m.getName().endsWith(BackendWeaver.BODY_SUFFIX)
                        || m.getName().startsWith(BackendWeaver.BRIDGE_PREFIX)) {
                    continue;
                }
                boolean packagePrivate = (m.getAccess() & (Opcodes.ACC_PUBLIC
                        | Opcodes.ACC_PROTECTED)) == 0;
                if (packagePrivate && !samePackage) {
                    continue;
                }
                if (!seen.add(m.getName() + m.getDescriptor().substring(0,
                        m.getDescriptor().indexOf(')') + 1))) {
                    continue;
                }
                if (m.isFinal() && !includeFinal) {
                    continue;
                }
                out.add(m);
            }
            String parent = c.getSuperInternalName();
            c = parent == null || "java/lang/Object".equals(parent) ? null : ctx.lookup(parent);
        }
        return out;
    }

    /// The eager singletons in dependency order, through constructor and factory
    /// dependencies; prototypes are placed so what they need is built first.
    private void orderConstruction() {
        Map<Bean, Integer> state = new LinkedHashMap<Bean, Integer>();
        for (Bean b : beans) {
            if (b.isEager() || PROTOTYPE.equals(b.scope)) {
                visit(b, state, new ArrayList<Bean>());
                if (ctx.hasErrors()) {
                    return;
                }
            }
        }
    }

    private void visit(Bean b, Map<Bean, Integer> state, List<Bean> path) {
        Integer s = state.get(b);
        if (s != null && s.intValue() == 2) {
            return;
        }
        if (s != null && s.intValue() == 1) {
            StringBuilder cycle = new StringBuilder();
            int from = path.indexOf(b);
            for (int i = from; i < path.size(); i++) {
                cycle.append(path.get(i).name).append(" -> ");
            }
            cycle.append(b.name);
            AnnotatedClass where = b.cls != null && ctx.lookup(b.type) != null ? b.cls
                    : b.factoryOwnerClass;
            ctx.error(where, "The constructors form a cycle: " + cycle + ". Nothing can be "
                    + "built first. Inject one of them through an @Autowired field or setter, "
                    + "which the build sets after every bean is constructed.");
            return;
        }
        state.put(b, Integer.valueOf(1));
        path.add(b);
        List<Bean> deps = new ArrayList<Bean>();
        if (b.owner != null) {
            deps.add(b.owner);
        }
        for (Point p : b.constructorPoints) {
            deps.addAll(p.candidates);
        }
        if (PROTOTYPE.equals(b.scope)) {
            // Built at each injection point, fields included, so everything it
            // injects has to exist when the first of them is built.
            for (Point p : b.fields.values()) {
                deps.addAll(p.candidates);
            }
            for (Call c : b.setters) {
                for (Point p : c.points) {
                    deps.addAll(p.candidates);
                }
            }
        }
        for (Bean d : deps) {
            if (d.isEager() || PROTOTYPE.equals(d.scope)) {
                visit(d, state, path);
                if (ctx.hasErrors()) {
                    return;
                }
            }
        }
        path.remove(path.size() - 1);
        state.put(b, Integer.valueOf(2));
        order.add(b);
    }

    // ---------------------------------------------------------------- aspects

    /// Every project class with a transactional, async, timed or counted method:
    /// beans or not, since the rewrite applies to `new` as much as to injection.
    private void collectAspects() {
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            if (!concerns(cls) || cls.isInterface()) {
                continue;
            }
            AnnotationValues classTx = cls.getClassAnnotation(TRANSACTIONAL);
            AnnotationValues classAsync = cls.getClassAnnotation(ASYNC);
            Aspects found = null;
            for (MethodInfo m : cls.getMethods()) {
                if (m.isConstructor() || m.isSynthetic() || "<clinit>".equals(m.getName())
                        || m.getName().endsWith(BackendWeaver.BODY_SUFFIX)
                        || m.getName().startsWith(BackendWeaver.BRIDGE_PREFIX)) {
                    continue;
                }
                AnnotationValues tx = m.getAnnotation(TRANSACTIONAL);
                AnnotationValues async = m.getAnnotation(ASYNC);
                if (tx == null && classTx != null && m.isPublic() && !m.isStatic()) {
                    tx = classTx;
                }
                if (async == null && classAsync != null && m.isPublic() && !m.isStatic()) {
                    async = classAsync;
                }
                AnnotationValues timed = m.getAnnotation(TIMED);
                AnnotationValues counted = m.getAnnotation(COUNTED);
                if (tx == null && async == null && timed == null && counted == null) {
                    continue;
                }
                String where = cls.getSourceName() + "." + m.getName();
                if (m.isAbstract() || (m.getAccess() & Opcodes.ACC_NATIVE) != 0) {
                    ctx.error(cls, where + " is " + (m.isAbstract() ? "abstract" : "native")
                            + ", so there is no body to wrap. Annotate the implementation.");
                    continue;
                }
                if (async != null) {
                    Type ret = Type.getReturnType(m.getDescriptor());
                    if (ret.getSort() != Type.VOID && !(ret.getSort() == Type.OBJECT
                            && "java/util/concurrent/Future".equals(ret.getInternalName()))) {
                        ctx.error(cls, "@Async method " + where + " returns "
                                + ret.getClassName() + "; its caller returns before the work "
                                + "is done, so it can only receive nothing or a "
                                + "java.util.concurrent.Future. Return AsyncResult.of(value) "
                                + "from a method declared to return Future.");
                        continue;
                    }
                }
                if (found == null) {
                    String pkg = RestClientAnnotationProcessor.packageOf(cls.getBinaryName());
                    found = new Aspects(cls, qualify(pkg, baseName(cls.getInternalName())
                            + "Cn1Aspects"));
                    aspects.put(cls.getInternalName(), found);
                }
                Aspect a = new Aspect(m);
                a.transactional = tx;
                a.async = async;
                a.timed = timed;
                a.counted = counted;
                if (async != null) {
                    String pkg = RestClientAnnotationProcessor.packageOf(cls.getBinaryName());
                    a.asyncTaskBinary = qualify(pkg, baseName(cls.getInternalName())
                            + "Cn1Async" + found.methods.size());
                }
                found.methods.add(a);
            }
        }
    }

    // ------------------------------------------------------------------- plan

    /// What the weaver does to each class.
    private void plan() {
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            if (!concerns(cls)) {
                continue;
            }
            BackendWeaver.Plan plan = null;
            Aspects a = aspects.get(cls.getInternalName());
            String helper = a == null ? null : a.helperBinary.replace('.', '/');
            for (FieldInfo f : cls.getFields()) {
                if ((f.getAnnotation(AUTOWIRED) != null || f.getAnnotation(VALUE) != null)
                        && !f.isStatic() && !f.isFinal()) {
                    plan = plan(plan, cls, helper);
                    plan.injectFields.put(f.getName(), f.getDescriptor());
                }
            }
            boolean stereotyped = isStereotyped(cls);
            for (MethodInfo m : cls.getMethods()) {
                if (m.isSynthetic()) {
                    continue;
                }
                if (m.isConstructor()) {
                    if (stereotyped && !m.isPublic()) {
                        plan = plan(plan, cls, helper);
                        plan.constructors.add(m.getDescriptor());
                    }
                    continue;
                }
                if (!m.isPublic() && callsFromWiring(m)) {
                    plan = plan(plan, cls, helper);
                    String key = m.getName() + m.getDescriptor();
                    plan.bridges.add(key);
                    if (m.isStatic()) {
                        plan.staticBridges.add(key);
                    }
                    if (m.isPrivate()) {
                        plan.privateBridges.add(key);
                    }
                }
            }
            if (a != null) {
                plan = plan(plan, cls, helper);
                for (Aspect aspect : a.methods) {
                    plan.aspects.add(aspect.method.getName() + aspect.method.getDescriptor());
                }
            }
            if (plan != null) {
                plans.put(cls.getInternalName(), plan);
            }
        }
    }

    private static BackendWeaver.Plan plan(BackendWeaver.Plan existing, AnnotatedClass cls,
                                           String helper) {
        return existing != null ? existing : new BackendWeaver.Plan(cls.getInternalName(),
                helper);
    }

    /// Whether generated code outside the class calls this method.
    private static boolean callsFromWiring(MethodInfo m) {
        return m.getAnnotation(AUTOWIRED) != null || m.getAnnotation(POST_CONSTRUCT) != null
                || m.getAnnotation(PRE_DESTROY) != null || m.getAnnotation(BEAN) != null
                || m.getAnnotation(SCHEDULED) != null || m.getAnnotation(MCP_TOOL) != null
                || m.getAnnotation(MANAGED_ATTRIBUTE) != null
                || m.getAnnotation(MANAGED_OPERATION) != null;
    }

    /// Whether code in another class calls `m` through its bridge.
    static boolean bridged(MethodInfo m) {
        return !m.isPublic() && callsFromWiring(m);
    }

    // ------------------------------------------------------------------- emit

    /// Weaves the classes, then compiles the classes generated beside them.
    private void emit() throws ProcessingException {
        File out = ctx.getOutputClassDir();
        int woven = 0;
        try {
            for (BackendWeaver.Plan plan : plans.values()) {
                if (BackendWeaver.weave(out, plan)) {
                    woven++;
                }
            }
        } catch (IOException err) {
            throw new ProcessingException("Could not rewrite the backend classes: "
                    + err.getMessage(), err);
        }
        BackendSources writer = new BackendSources(this);
        for (Aspects a : aspects.values()) {
            writer.aspects(a, sources);
        }
        for (Bean b : beans) {
            if (b.proxyBinary != null) {
                sources.put(b.proxyBinary, writer.proxy(b));
            }
            for (int i = 0; i < b.tools.size(); i++) {
                Tool t = b.tools.get(i);
                String pkg = RestClientAnnotationProcessor.packageOf(b.type.replace('/', '.'));
                t.adapterBinary = qualify(pkg, baseName(b.type) + "Cn1Tool" + i);
                sources.put(t.adapterBinary, writer.tool(b, t));
            }
            if (b.managed != null) {
                String pkg = RestClientAnnotationProcessor.packageOf(b.type.replace('/', '.'));
                b.managed.adapterBinary = qualify(pkg, baseName(b.type) + "Cn1Managed");
                sources.put(b.managed.adapterBinary, writer.managed(b));
            }
        }
        if (sources.isEmpty()) {
            if (woven > 0) {
                ctx.getLog().info("cn1: rewrote " + woven + " backend class(es)");
            }
            return;
        }
        try {
            List<File> cp = new ArrayList<File>();
            cp.add(out);
            for (String element : ctx.getCompileClasspath()) {
                cp.add(new File(element));
            }
            JavaSourceCompiler.compile(sources, out, cp);
        } catch (IOException err) {
            throw new ProcessingException("Could not compile the classes generated for the "
                    + "backend's beans: " + err.getMessage(), err);
        }
        ctx.getLog().info("cn1: rewrote " + woven + " backend class(es) and generated "
                + sources.size() + " support class(es)");
    }

    // ---------------------------------------------------------------- helpers

    /// Every type a value of `internal` can be assigned to: itself, its
    /// superclasses and its interfaces, as far as the build can see them.
    Set<String> assignableTypes(String internal) {
        Set<String> out = new LinkedHashSet<String>();
        List<String> queue = new ArrayList<String>();
        queue.add(internal);
        while (!queue.isEmpty()) {
            String next = queue.remove(0);
            if (next == null || "java/lang/Object".equals(next) || !out.add(next)) {
                continue;
            }
            AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(ctx, next);
            if (c == null) {
                continue;
            }
            queue.add(c.getSuperInternalName());
            queue.addAll(c.getInterfaceInternalNames());
        }
        return out;
    }

    /// The erased element type of `List<E>` or `Collection<E>`, from a field or
    /// parameter signature.
    static String elementType(String signature) {
        int lt = signature.indexOf('<');
        int gt = signature.lastIndexOf('>');
        if (lt < 0 || gt < lt) {
            return null;
        }
        String arg = signature.substring(lt + 1, gt);
        if (arg.startsWith("+")) {
            arg = arg.substring(1);
        }
        if (!arg.startsWith("L")) {
            return null;
        }
        int end = arg.indexOf('<');
        if (end < 0) {
            end = arg.indexOf(';');
        }
        return end < 0 ? null : arg.substring(1, end);
    }

    /// Each parameter's generic signature, or null when the method has none.
    static String[] parameterSignatures(MethodInfo m) {
        String sig = m.getSignature();
        if (sig == null) {
            return null;
        }
        int open = sig.indexOf('(');
        int close = sig.lastIndexOf(')');
        if (open < 0 || close < open) {
            return null;
        }
        List<String> out = new ArrayList<String>();
        String params = sig.substring(open + 1, close);
        int i = 0;
        while (i < params.length()) {
            int start = i;
            while (params.charAt(i) == '[') {
                i++;
            }
            char c = params.charAt(i);
            if (c == 'L' || c == 'T') {
                int depth = 0;
                while (i < params.length()) {
                    char d = params.charAt(i);
                    if (d == '<') {
                        depth++;
                    } else if (d == '>') {
                        depth--;
                    } else if (d == ';' && depth == 0) {
                        break;
                    }
                    i++;
                }
            }
            i++;
            out.add(params.substring(start, i));
        }
        int count = Type.getArgumentTypes(m.getDescriptor()).length;
        if (out.size() != count) {
            // A signature that skips synthetic parameters: the positions would
            // not line up, and a wrong element type is worse than none.
            return null;
        }
        return out.toArray(new String[out.size()]);
    }

    static Map<String, AnnotationValues> parameterAnnotations(MethodInfo m, int index) {
        List<Map<String, AnnotationValues>> all = m.getParameterAnnotations();
        if (index < all.size()) {
            return all.get(index);
        }
        return Collections.<String, AnnotationValues>emptyMap();
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<String>();
        if (value instanceof List) {
            for (Object o : (List<?>) value) {
                if (o instanceof String && ((String) o).trim().length() > 0) {
                    out.add(((String) o).trim());
                }
            }
        } else if (value instanceof String && ((String) value).trim().length() > 0) {
            out.add(((String) value).trim());
        }
        return out;
    }

    private static long longOf(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : -1;
    }

    static String enumName(Object value, String fallback) {
        if (value instanceof String[] && ((String[]) value).length == 2) {
            return ((String[]) value)[1];
        }
        return fallback;
    }

    /// Spring's rule, which is java.beans.Introspector's: the first letter
    /// lower-cased, unless the first two are both capitals (URLService stays).
    static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(0))
                && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    static String identifier(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            sb.append((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' ? c : '_');
        }
        return sb.toString();
    }

    /// A class's name within its package with `$` turned into `_`: the base the
    /// generated classes beside it are named from.
    static String baseName(String internal) {
        int slash = internal.lastIndexOf('/');
        return internal.substring(slash + 1).replace('$', '_');
    }

    static String qualify(String pkg, String simple) {
        return pkg == null || pkg.length() == 0 ? simple : pkg + "." + simple;
    }
}

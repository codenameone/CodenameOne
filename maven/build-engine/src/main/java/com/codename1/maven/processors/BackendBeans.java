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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
/// Every class carrying a stereotype -- `@Component`, `@Configuration`,
/// `@RestController`, `@WebSocketMapping` -- and every `@Bean`
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
    static final String CONFIGURATION = PKG + "Configuration;";
    static final String BEAN = PKG + "Bean;";
    static final String AUTOWIRED = PKG + "Autowired;";
    static final String QUALIFIER = PKG + "Qualifier;";
    static final String PRIMARY = PKG + "Primary;";
    static final String ORDER = PKG + "Order;";
    static final String LAZY = PKG + "Lazy;";
    static final String VALUE = PKG + "Value;";
    static final String CONFIG_PROPERTIES = PKG + "ConfigurationProperties;";
    static final String SCOPE = PKG + "Scope;";
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
    static final String PRE_AUTHORIZE = PKG + "PreAuthorize;";
    static final String SECURED = PKG + "Secured;";
    static final String ROLES_ALLOWED = PKG + "RolesAllowed;";
    static final String PERMIT_ALL = PKG + "PermitAll;";
    static final String DENY_ALL = PKG + "DenyAll;";
    static final String P = PKG + "P;";
    /// The method-security annotations, and how each is written.
    static final String[] METHOD_SECURITY = {PRE_AUTHORIZE, SECURED, ROLES_ALLOWED, PERMIT_ALL,
        DENY_ALL};
    static final String[] METHOD_SECURITY_SHOWN = {"@PreAuthorize", "@Secured", "@RolesAllowed",
        "@PermitAll", "@DenyAll"};
    /// Spring Security's annotations that act on what a method returns or is
    /// given, by simple name: whichever package one comes from, the build has
    /// nothing that applies it.
    private static final String[] UNSUPPORTED_SECURITY = {"PostAuthorize", "PreFilter",
        "PostFilter"};

    /// Every annotation this pass reads, for the processor's declared interest.
    static final Set<String> DESCRIPTORS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(java.util.Arrays.asList(COMPONENT,
                    CONFIGURATION, BEAN, AUTOWIRED, VALUE, CONFIG_PROPERTIES, SCOPE,
                    PROFILE, ON_PROPERTY, ON_MISSING,
                    POST_CONSTRUCT, PRE_DESTROY, TRANSACTIONAL, ASYNC, SCHEDULED,
                    MANAGED_RESOURCE, MANAGED_ATTRIBUTE, MANAGED_OPERATION, TIMED, COUNTED,
                    MCP_TOOL, REST_CONTROLLER, WEBSOCKET_MAPPING, PRE_AUTHORIZE, SECURED,
                    ROLES_ALLOWED, PERMIT_ALL, DENY_ALL)));

    private static final String[] STEREOTYPES = {COMPONENT, CONFIGURATION,
            REST_CONTROLLER, WEBSOCKET_MAPPING};

    static final String CONFIG_TYPE = "com/codename1/backend/Config";
    static final String DATASOURCE_TYPE = "com/codename1/backend/DataSource";
    static final String ENTITIES_TYPE = "com/codename1/backend/orm/EntityManager";
    static final String SESSION_TYPE = "com/codename1/orm/session/Session";
    static final String REQUEST_TYPE = "com/codename1/backend/HttpServer$Request";
    static final String HTTP_SESSION_TYPE = "com/codename1/backend/HttpSession";
    static final String SECURITY_PKG = "com/codename1/backend/security/";
    static final String SECURITY_CHAIN_TYPE = SECURITY_PKG + "SecurityFilterChain";
    static final String HTTP_SECURITY_TYPE = SECURITY_PKG + "HttpSecurity";
    /// The bean types an HttpSecurity picks its collaborators from: every bean
    /// of one of these is handed to it, and it chooses by type. A sign-in
    /// mechanism that needs another kind of bean adds its type here.
    static final String[] SECURITY_SHARED_TYPES = {
        SECURITY_PKG + "core/userdetails/UserDetailsService",
        SECURITY_PKG + "core/userdetails/UserDetailsPasswordService",
        SECURITY_PKG + "crypto/PasswordEncoder",
        SECURITY_PKG + "AuthenticationProvider",
        SECURITY_PKG + "AuthenticationManager",
        // What oauth2ResourceServer() uses when the chain names none of its own.
        SECURITY_PKG + "oauth2/jwt/JwtDecoder",
        SECURITY_PKG + "oauth2/server/resource/JwtAuthenticationConverter",
        SECURITY_PKG + "oauth2/server/resource/BearerTokenResolver",
        // What apiKey() looks keys up in.
        SECURITY_PKG + "apikey/ApiKeyRepository",
        // What rateLimit() counts with, when the rule names none.
        SECURITY_PKG + "ratelimit/RateLimiter",
    };

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
        /// The first candidate is a CONDITIONAL @Primary: used when it exists, the
        /// others considered only when its condition leaves it out.
        boolean preferFirst;

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
        /// The class's simple name and the method; qualified when that clashes.
        String name;
        /// The class the method is in, for qualifying a clashing name.
        String ownerBinary;
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
        /// Per parameter: the statements reading it through the JSON codecs into
        /// `target`, or null for a plain scalar McpArgs converts.
        final List<String> paramRead = new ArrayList<String>();
        /// Per parameter: its declared type with arguments, when read by a codec.
        final List<String> paramTypes = new ArrayList<String>();
        /// Per parameter: the JSON Schema type a codec-read one is described with.
        final List<String> paramSchema = new ArrayList<String>();
        /// The return type, when the result is written by a codec; null for
        /// void and String, which are sent as they are.
        String returnType;
        /// The statements writing `result` into `out`, with [#returnType].
        String returnWrite;
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
        /// Per operation: its declared return type and the statements writing
        /// `result` through the JSON codecs, or nulls when the value is sent as
        /// it is -- nothing, a String, a primitive or its box.
        final List<String> operationReturnTypes = new ArrayList<String>();
        final List<String> operationWrites = new ArrayList<String>();
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
        /// @Order's value; a bean without one comes last, as in Spring.
        int order = Integer.MAX_VALUE;
        boolean lazy;
        boolean onMissing;
        /// @ConditionalOnMissingBean's explicit types, empty for the default.
        final List<String> missingTypes = new ArrayList<String>();
        /// Each entry is one @Profile's names, of which one must be active; every
        /// entry must hold. More than one only for a factory bean, which also
        /// carries its configuration class's @Profile.
        final List<String[]> profiles = new ArrayList<String[]>();
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
        /// For a test's `@MockitoBean`: the internal name of the type mocked. Such a
        /// bean has no class of its own to construct; the test wiring asks Mockito.
        String mockType;

        boolean isConditional() {
            return !profiles.isEmpty() || !propertyConditions.isEmpty();
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
        /// The method's authorization check as a Java boolean expression, or
        /// null when it has none; see [MethodSecurityCompiler].
        String security;
        /// Bean name -> Java type, for the beans [#security] calls.
        Map<String, String> securityBeans = Collections.<String, String>emptyMap();

        Aspect(MethodInfo method) {
            this.method = method;
        }
    }

    // ------------------------------------------------------------------ state

    final ProcessorContext ctx;
    final List<Bean> beans = new ArrayList<Bean>();
    final Map<String, Bean> byName = new LinkedHashMap<String, Bean>();
    final Map<String, Aspects> aspects = new TreeMap<String, Aspects>();
    /// The beans authorization expressions call, by name: the wiring registers
    /// each so the woven check can reach the one of its own server.
    final Set<String> namedBeans = new java.util.TreeSet<String>();
    /// Metric name -> "histogram" or "counter", with where it was declared, for
    /// the @Timed and @Counted instruments the woven code registers.
    private final Map<String, String[]> aspectMetrics = new LinkedHashMap<String, String[]>();
    /// Prometheus series name -> the aspect metric exporting it, and where.
    private final Map<String, String[]> aspectSeries = new LinkedHashMap<String, String[]>();

    /// The instruments the server registers itself (Metrics.enableServer), and
    /// their kinds; keep in step with it. Claimed up front, so an aspect cannot
    /// take one of these names -- or one that exports to the same Prometheus
    /// series. At run time that clash is refused in the woven finally, AFTER the
    /// body's work (and any transaction around it) has committed, so the caller
    /// sees a failure for work that succeeded and may retry it.
    private static final String[][] BUILT_IN_METRICS = {
        {"http.server.request.duration", "histogram"},
        {"cn1.scheduler.run.duration", "histogram"},
        {"http.server.active_requests", "gauge"},
        {"http.server.open_connections", "gauge"},
        {"cn1.server.websocket_connections", "gauge"},
        {"cn1.server.requests_served", "gauge"},
        {"cn1.server.connections_refused", "gauge"},
        {"db.client.connection.count", "gauge"},
        {"db.client.connection.idle", "gauge"},
        {"cn1.task.queue_depth", "gauge"},
        {"process.runtime.memory.used", "gauge"},
        {"process.uptime", "gauge"},
    };

    {
        String by = "the server's own instruments";
        for (String[] builtIn : BUILT_IN_METRICS) {
            aspectMetrics.put(builtIn[0], new String[] {"built-in", by});
            String base = promName(builtIn[0]);
            String[] series = "histogram".equals(builtIn[1])
                    ? new String[] {base, base + "_bucket", base + "_sum", base + "_count"}
                    : new String[] {base};
            for (String element : series) {
                aspectSeries.put(element, new String[] {builtIn[0], by});
            }
        }
    }
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

    /// Test mode, set by [#forTests] and null in a normal build: the classes that
    /// may become beans (the application's and the context's test
    /// configurations), the test configurations themselves, the mocks as
    /// {field, internal type}, and the classes this pass may rewrite.
    private Set<String> testScope;
    private Set<String> testConfigurations;
    private List<String[]> testMocks;
    private Set<String> testWeaveable;
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
            out.checkExecutorKinds();
        }
        if (!ctx.hasErrors()) {
            out.plan();
        }
        if (!ctx.hasErrors()) {
            out.emit();
        }
        return out;
    }

    /// The beans of one test context: the application's, the context's
    /// `@TestConfiguration` beans, and its mocks in place of the beans they
    /// replace -- resolved like a build's, but woven only in `weaveable` (the
    /// test classes) and with nothing generated beside them. The application's
    /// classes were woven, and their stand-ins, adapters and aspects compiled, by
    /// the main build; the names computed here are those same names, which the
    /// rules below keep stable.
    ///
    /// @param entryPackage the main build's, from its wiring record
    static BackendBeans forTests(ProcessorContext ctx, Set<String> scope,
                                 Set<String> configurations, List<String[]> mocks,
                                 Set<String> weaveable, String entryPackage)
            throws ProcessingException {
        BackendBeans out = new BackendBeans(ctx);
        out.testScope = scope;
        out.testConfigurations = configurations;
        out.testMocks = mocks;
        out.testWeaveable = weaveable;
        out.discover();
        if (entryPackage != null) {
            out.entryPackage = entryPackage;
        }
        if (!ctx.hasErrors()) {
            out.checkTestBeans();
        }
        if (!ctx.hasErrors()) {
            out.applyMocks();
        }
        if (!ctx.hasErrors()) {
            out.resolve();
        }
        if (!ctx.hasErrors()) {
            out.collectNamedBeans();
        }
        if (!ctx.hasErrors()) {
            out.plan();
        }
        if (!ctx.hasErrors()) {
            out.nameAdapters();
        }
        return out;
    }

    /// Test beans are plain singletons. A scoped or lazy one would take a slot
    /// and shift the application's, which the stand-ins compiled by the main
    /// build have baked in; a woven one would need an aspect class nothing
    /// generates in a test run.
    private void checkTestBeans() {
        for (Bean b : beans) {
            AnnotatedClass declaring = b.factory != null ? b.factoryOwnerClass : b.cls;
            if (declaring == null || !testConfigurations.contains(declaring.getInternalName())) {
                continue;
            }
            if (!SINGLETON.equals(b.scope) || b.lazy) {
                ctx.error(declaring, "Test bean " + b.describe() + " is " + (b.lazy ? "lazy"
                        : "scoped " + b.scope) + "; a test bean is a plain singleton.");
            }
        }
        for (String name : testConfigurations) {
            AnnotatedClass cls = ctx.lookup(name);
            if (cls == null) {
                continue;
            }
            for (MethodInfo m : cls.getMethods()) {
                if (m.getAnnotation(TRANSACTIONAL) != null || m.getAnnotation(ASYNC) != null
                        || m.getAnnotation(TIMED) != null || m.getAnnotation(COUNTED) != null
                        || m.getAnnotation(SCHEDULED) != null
                        || methodSecurity(m.getAnnotations(), null, null) != null) {
                    ctx.error(cls, "@TestConfiguration " + cls.getSourceName() + "."
                            + m.getName() + " carries an annotation the build weaves; a test "
                            + "configuration declares beans and nothing else.");
                }
            }
        }
    }

    /// Replaces every bean assignable to a mocked type with one mock of it.
    private void applyMocks() {
        for (String[] mock : testMocks) {
            String type = mock[1];
            List<Bean> replaced = new ArrayList<Bean>();
            for (Bean b : beans) {
                if (b.mockType == null && b.types.contains(type)) {
                    replaced.add(b);
                }
            }
            for (Bean b : replaced) {
                if (b.controller || b.webSocket) {
                    ctx.error("@MockitoBean " + type.replace('/', '.') + " would replace "
                            + b.describe() + ", which serves requests; mock what it calls "
                            + "instead.");
                    return;
                }
                if (!SINGLETON.equals(b.scope) || b.lazy) {
                    ctx.error("@MockitoBean " + type.replace('/', '.') + " would replace "
                            + b.describe() + ", which is " + (b.lazy ? "lazy" : "scoped "
                            + b.scope) + "; only a singleton can be mocked.");
                    return;
                }
            }
            for (Bean b : replaced) {
                beans.remove(b);
                byName.remove(b.name);
            }
            // Factory beans of a configuration that was mocked away go with it.
            for (int i = beans.size() - 1; i >= 0; i--) {
                Bean b = beans.get(i);
                if (b.owner != null && replaced.contains(b.owner)) {
                    beans.remove(i);
                    byName.remove(b.name);
                }
            }
            Bean m = new Bean();
            m.mockType = type;
            m.type = type;
            m.cls = ctx.lookup(type);
            m.name = replaced.size() == 1 ? replaced.get(0).name : mock[0];
            m.types.addAll(assignableTypes(type));
            m.types.add(type);
            add(m);
        }
    }

    /// The adapter class names the wiring registers, as the main build named them.
    private void nameAdapters() {
        for (Bean b : beans) {
            String pkg = RestClientAnnotationProcessor.packageOf(b.type.replace('/', '.'));
            for (int i = 0; i < b.tools.size(); i++) {
                b.tools.get(i).adapterBinary = qualify(pkg, baseName(b.type) + "Cn1Tool" + i);
            }
            if (b.managed != null) {
                b.managed.adapterBinary = qualify(pkg, baseName(b.type) + "Cn1Managed");
            }
        }
    }

    /// Weaves the test classes this pass planned, and nothing else, each where its
    /// class file is: a mixed Java and Kotlin test set has two output directories.
    int weaveTests(File out, Map<String, AnnotatedClass> index) throws ProcessingException {
        int woven = 0;
        try {
            for (BackendWeaver.Plan plan : plans.values()) {
                AnnotatedClass cls = index.get(plan.internalName);
                File file = cls != null && cls.getClassFile() != null ? cls.getClassFile()
                        : new File(out, plan.internalName + ".class");
                if (BackendWeaver.weaveFile(file, plan)) {
                    woven++;
                }
            }
        } catch (IOException err) {
            throw new ProcessingException("Could not rewrite the test classes: "
                    + err.getMessage(), err);
        }
        return woven;
    }

    private static boolean usesAnyAnnotation(ProcessorContext ctx) {
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            for (String d : cls.getAllAnnotationDescriptors()) {
                if (DESCRIPTORS.contains(d) || unsupportedSecurity(d) != null) {
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

    boolean hasManaged() {
        for (Bean b : beans) {
            if (b.managed != null) {
                return true;
            }
        }
        return false;
    }

    /// Whether any bean is a SecurityFilterChain: what links the security layer
    /// into the server.
    boolean hasSecurityChains() {
        for (Bean b : beans) {
            if (isSecurityChain(b)) {
                return true;
            }
        }
        return false;
    }

    static boolean isSecurityChain(Bean b) {
        return b.types.contains(SECURITY_CHAIN_TYPE);
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
            if (testScope != null && !testScope.contains(cls.getInternalName())) {
                continue;
            }
            if (!isStereotyped(cls) && !(testConfigurations != null
                    && testConfigurations.contains(cls.getInternalName()))) {
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
        checkUniqueNames();
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
        hierarchy(bean, cls, false);
        AnnotationValues props = cls.getClassAnnotation(CONFIG_PROPERTIES);
        if (props != null) {
            bindProperties(bean, props, cls);
        }
        if (REQUEST.equals(bean.scope) || SESSION.equals(bean.scope)) {
            // Inherited ones too: an @Async method a superclass declares queues a
            // task holding the scoped instance just the same.
            for (MethodInfo m : inheritedMembers(cls)) {
                if (wovenAsync(cls, m)) {
                    // The task would hold the scoped instance after its request or
                    // session ended and destroyed it. A warning, as Spring runs it:
                    // the task calls the instance itself, destroyed or not.
                    String scope = REQUEST.equals(bean.scope) ? "request" : "session";
                    ctx.getLog().warn("cn1: @Async method " + cls.getSourceName() + "." + m.getName()
                            + " is on a @Scope(\"" + scope + "\") bean, which is destroyed when "
                            + "its " + scope + " ends -- "
                            + "possibly before the task runs. Move the method to a singleton "
                            + "and pass it what it needs.");
                    break;
                }
            }
        }
        collectJobs(bean);
        collectTools(bean);
        collectManaged(bean);
        return bean;
    }

    /// Walks `cls` and its superclasses, top first, collecting what each declares.
    ///
    /// @param lifecycleOnly only the @PostConstruct and @PreDestroy methods -- for
    ///        the object a @Bean method returns, which the build does not inject
    private void hierarchy(Bean bean, AnnotatedClass cls, boolean lifecycleOnly) {
        // Superclasses first, as Spring injects and initializes them: a field an
        // abstract base declares @Autowired is as much a dependency as one the
        // bean declares, and AnnotatedClass lists only DECLARED members.
        List<AnnotatedClass> chain = new ArrayList<AnnotatedClass>();
        for (AnnotatedClass c = cls; c != null && chain.size() < 64; ) {
            chain.add(c);
            String sup = c.getSuperInternalName();
            c = sup == null || "java/lang/Object".equals(sup) ? null
                    : RestControllerAnnotationProcessor.resolveClass(ctx, sup);
        }
        Set<String> fieldNames = new HashSet<String>();
        for (int level = chain.size() - 1; level >= 0; level--) {
            Set<String> overridden = new HashSet<String>();
            Set<String> declaredBelow = new HashSet<String>();
            for (int below = 0; below < level; below++) {
                for (MethodInfo m : chain.get(below).getMethods()) {
                    if (m.isConstructor()) {
                        continue;
                    }
                    declaredBelow.add(m.getName() + m.getDescriptor());
                    if (!m.isPrivate() && !m.isStatic()) {
                        overridden.add(m.getName() + m.getDescriptor());
                    }
                }
            }
            members(bean, cls, chain.get(level), level > 0 && !concerns(chain.get(level)),
                    overridden, declaredBelow, fieldNames, lifecycleOnly);
        }
    }

    /// The injection points and lifecycle methods one class of a bean's
    /// hierarchy declares.
    ///
    /// @param unwoven the class is not rewritten by this build, so nothing that
    ///        needs a woven setter or bridge can be used from it
    /// @param overridden methods a subclass redeclares, which are skipped here
    /// @param declaredBelow every method a subclass declares, private ones too
    private void members(Bean bean, AnnotatedClass cls, AnnotatedClass declaring,
                         boolean unwoven, Set<String> overridden, Set<String> declaredBelow,
                         Set<String> fieldNames, boolean lifecycleOnly) {
        for (FieldInfo f : lifecycleOnly ? new ArrayList<FieldInfo>() : declaring.getFields()) {
            boolean autowired = f.getAnnotation(AUTOWIRED) != null;
            AnnotationValues value = f.getAnnotation(VALUE);
            if (!autowired && value == null) {
                continue;
            }
            String where = "field " + f.getName() + " of " + declaring.getSourceName();
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
            if (!fieldNames.add(f.getName())) {
                ctx.error(cls, where + " has the name of another injected field of "
                        + cls.getSourceName() + "'s class hierarchy, and the build injects a "
                        + "private field through a setter named after it. Rename one.");
                continue;
            }
            if (unwoven) {
                ctx.error(cls, cls.getSourceName() + " inherits " + where + ", which the build "
                        + "cannot inject: its class is not compiled from this project's "
                        + "sources. Take the dependency in " + cls.getSourceName()
                        + " instead.");
                continue;
            }
            Point p = new Point(where, Type.getType(f.getDescriptor()), f.getSignature());
            readPoint(p, f.getAnnotations());
            bean.fields.put(f, p);
        }
        for (MethodInfo m : declaring.getMethods()) {
            if (m.isConstructor() || overridden.contains(m.getName() + m.getDescriptor())) {
                // An overriding method is the one that runs, and it carries its
                // own annotations or none, as in Spring.
                continue;
            }
            if (m.isPrivate() && callsFromWiring(m)
                    && declaredBelow.contains(m.getName() + m.getDescriptor())) {
                // Both classes get a public bridge of the same signature, and the
                // subclass's would override the base's -- running its own method
                // twice and the base's never.
                ctx.error(cls, declaring.getSourceName() + "." + m.getName() + " is private and "
                        + "a subclass in " + cls.getSourceName() + "'s hierarchy declares a "
                        + "method of the same signature, so the build cannot call both. "
                        + "Rename one.");
                continue;
            }
            if (unwoven && !m.isPublic() && callsFromWiring(m)) {
                ctx.error(cls, cls.getSourceName() + " inherits " + declaring.getSourceName()
                        + "." + m.getName() + ", which is not public and so needs a bridge "
                        + "the build can only add to a class compiled from this project's "
                        + "sources. Make it public.");
                continue;
            }
            String where = declaring.getSourceName() + "." + m.getName();
            if (!lifecycleOnly && m.getAnnotation(AUTOWIRED) != null) {
                if (m.isStatic()) {
                    ctx.error(cls, "@Autowired method " + where + " is static; the build "
                            + "injects instances.");
                    continue;
                }
                Call call = new Call(m);
                Type[] args = Type.getArgumentTypes(m.getDescriptor());
                String[] generics = parameterSignatures(m);
                // @Autowired(required = false) on the METHOD makes its arguments
                // optional, as in Spring: the method is then not called unless
                // they resolve. A parameter's own @Autowired still decides for it.
                boolean methodRequired = m.getAnnotation(AUTOWIRED)
                        .getBoolOrDefault("required", true);
                for (int i = 0; i < args.length; i++) {
                    Point p = new Point("parameter " + (i + 1) + " of " + where, args[i],
                            generics == null ? null : generics[i]);
                    p.required = methodRequired;
                    readPoint(p, parameterAnnotations(m, i));
                    call.points.add(p);
                }
                bean.setters.add(call);
            }
            if (m.getAnnotation(POST_CONSTRUCT) != null
                    && lifecycle(declaring, m, "@PostConstruct")) {
                bean.postConstruct.add(m);
            }
            if (m.getAnnotation(PRE_DESTROY) != null && lifecycle(declaring, m, "@PreDestroy")) {
                bean.preDestroy.add(m);
            }
        }
    }

    private boolean lifecycle(AnnotatedClass cls, MethodInfo m, String what) {
        if (m.isStatic() || Type.getArgumentTypes(m.getDescriptor()).length != 0) {
            ctx.error(cls, what + " method " + cls.getSourceName() + "." + m.getName()
                    + " must be an instance method that takes no arguments.");
            return false;
        }
        if (returnsFuture(m)) {
            // Spring ignores a lifecycle method's return value, and so does this --
            // but a Future means work still running when the call returns.
            ctx.getLog().warn("cn1: " + what + " method " + cls.getSourceName() + "."
                    + m.getName() + " returns a Future, which is ignored: the server goes "
                    + "on -- " + ("@PostConstruct".equals(what) ? "to serve requests"
                    : "to tear down beans and close the database") + " -- when the method "
                    + "returns, not when that work finishes.");
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
        bean.primary = annotations.get(PRIMARY) != null;
        AnnotationValues order = annotations.get(ORDER);
        if (order != null) {
            bean.order = order.getIntOrDefault("value", Integer.MAX_VALUE);
        }
        AnnotationValues lazy = annotations.get(LAZY);
        bean.lazy = lazy != null && lazy.getBoolOrDefault("value", true);
        if (bean.lazy && !SINGLETON.equals(bean.scope)) {
            // A request, session or prototype bean is already built on demand.
            bean.lazy = false;
        }
        AnnotationValues missing = annotations.get(ON_MISSING);
        bean.onMissing = missing != null;
        if (missing != null && missing.get("value") instanceof List) {
            for (Object o : (List<?>) missing.get("value")) {
                if (o instanceof Type) {
                    bean.missingTypes.add(((Type) o).getInternalName());
                }
            }
        }
        AnnotationValues profile = annotations.get(PROFILE);
        if (profile != null) {
            List<String> values = strings(profile.get("value"));
            if (values.isEmpty()) {
                ctx.error(where, "@Profile on " + bean.name + " names no profile.");
            }
            for (String v : values) {
                String name = v.trim();
                if (name.startsWith("!")) {
                    name = name.substring(1).trim();
                }
                if (name.length() == 0) {
                    // "!" negates nothing: compared with the active profile, the
                    // empty name never matches, so the bean was on under EVERY
                    // profile, production included.
                    ctx.error(where, "@Profile on " + bean.name + " has \"" + v
                            + "\", which names no profile.");
                }
            }
            bean.profiles.add(values.toArray(new String[values.size()]));
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
        boolean scopedOwner = REQUEST.equals(owner.scope) || SESSION.equals(owner.scope);
        if (!m.isStatic() && scopedOwner && !REQUEST.equals(bean.scope)
                && !SESSION.equals(bean.scope)) {
            // The factory is called through the owner, which is a request- or
            // session-scoped stand-in -- and a bean that is not scoped itself is
            // built at start-up, when there is no request or session to find the
            // owner in. The server would refuse to start.
            ctx.error(cls, "@Bean method " + where + " builds a " + bean.scope + " bean, but "
                    + "its configuration " + owner.describe() + " is " + owner.scope
                    + "-scoped, so there is no instance to call it on when the bean is built. "
                    + "Make the method static, or give the bean the configuration's scope.");
            return null;
        }
        // A factory bean exists only when its configuration class does, as in
        // Spring: otherwise a @Profile("prod") configuration's @Bean would be
        // built on every profile -- through a null owner if the method is not
        // static, and against configuration a static one was never meant to see.
        bean.profiles.addAll(owner.profiles);
        bean.propertyConditions.addAll(owner.propertyConditions);
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
            // The whole hierarchy, as for a class bean: an initializer or
            // destructor the returned class inherits is as much its own.
            hierarchy(bean, bean.cls, true);
            // And its operational surface: a project class a factory builds is
            // a bean like any other, so its @Scheduled jobs, @McpTool methods
            // and @ManagedResource attributes must not silently disappear just
            // because @Bean rather than a stereotype created it.
            collectJobs(bean);
            collectTools(bean);
            collectManaged(bean);
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
        // ONE setter per property, as Spring Boot's binder picks one: the
        // overload whose parameter is the getter's type, otherwise the first
        // found (the subclass's before its base's). Every overload used to bind
        // from the same key, one after the other, and the last to run decided.
        Map<String, MethodInfo> byProperty = new LinkedHashMap<String, MethodInfo>();
        Set<String> overridden = new HashSet<String>();
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
                if (!overridden.add(m.getName() + m.getDescriptor())) {
                    continue;                      // overridden below: already seen
                }
                MethodInfo current = byProperty.get(m.getName());
                if (current == null || (!matchesGetter(bean.cls, current)
                        && matchesGetter(bean.cls, m))) {
                    byProperty.put(m.getName(), m);
                }
            }
            // Setters a library base class declares bind as well.
            String parent = c.getSuperInternalName();
            c = parent == null || "java/lang/Object".equals(parent) ? null
                    : RestControllerAnnotationProcessor.resolveClass(ctx, parent);
        }
        bean.propertySetters.addAll(byProperty.values());
        if (bean.propertySetters.isEmpty()) {
            ctx.error(where, "@ConfigurationProperties(\"" + prefix + "\") on " + bean.name
                    + " binds nothing: " + bean.type.replace('/', '.') + " has no public "
                    + "setter taking a String, a number, a boolean or an enum.");
        }
    }

    /// Whether `setter`'s parameter is the type its property's getter returns,
    /// looking through `cls`'s superclasses.
    private boolean matchesGetter(AnnotatedClass cls, MethodInfo setter) {
        String property = setter.getName().substring(3);
        Type arg = Type.getArgumentTypes(setter.getDescriptor())[0];
        AnnotatedClass c = cls;
        for (int depth = 0; c != null && depth < 64; depth++) {
            for (MethodInfo m : c.getMethods()) {
                if (m.isStatic() || Type.getArgumentTypes(m.getDescriptor()).length != 0) {
                    continue;
                }
                if (("get" + property).equals(m.getName())
                        || ("is" + property).equals(m.getName())) {
                    return Type.getReturnType(m.getDescriptor()).equals(arg);
                }
            }
            String parent = c.getSuperInternalName();
            c = parent == null || "java/lang/Object".equals(parent) ? null
                    : RestControllerAnnotationProcessor.resolveClass(ctx, parent);
        }
        return false;
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
            List<String> wanted = stepAsideTypes(b);
            for (Bean other : beans) {
                if (other == b || other.onMissing) {
                    continue;
                }
                boolean replaces = false;
                for (String t : wanted) {
                    if (other.types.contains(t)) {
                        replaces = true;
                        break;
                    }
                }
                if (replaces) {
                    drop.add(b);
                    break;
                }
            }
        }
        // A configuration that steps aside takes its @Bean methods with it,
        // static ones included, as its @Profile and @ConditionalOnProperty
        // already do: they are its declarations, and a non-static one would
        // otherwise be built through an owner that no longer exists.
        List<Bean> withFactories = new ArrayList<Bean>(drop);
        for (Bean b : beans) {
            if (b.factory == null || withFactories.contains(b)) {
                continue;
            }
            for (Bean gone : drop) {
                if (gone.cls != null && b.factoryOwnerClass == gone.cls) {
                    withFactories.add(b);
                    break;
                }
            }
        }
        for (Bean b : withFactories) {
            beans.remove(b);
            byName.remove(b.name);
            ctx.getLog().info("cn1: " + b.describe() + (drop.contains(b)
                    ? " steps aside: another bean has its type"
                    : " steps aside with its configuration class"));
        }
    }

    /// MCP tools and managed resources are found by name alone, so two with one
    /// name leave one unreachable -- which one depending on registration order.
    /// Two that are always built are refused here; two conditional ones may be
    /// meant for different profiles, and the server refuses them at start-up if
    /// both turn out active.
    private void checkUniqueNames() {
        // Jobs are named after their class's simple name, which two packages can
        // share -- and a manual trigger, the job listing and the metric label all
        // pick a job by name. Clashing ones are named by the qualified class.
        Map<String, List<Job>> jobs = new LinkedHashMap<String, List<Job>>();
        for (Bean b : beans) {
            for (Job j : b.jobs) {
                List<Job> same = jobs.get(j.name);
                if (same == null) {
                    same = new ArrayList<Job>();
                    jobs.put(j.name, same);
                }
                same.add(j);
            }
        }
        for (List<Job> same : jobs.values()) {
            if (same.size() > 1) {
                for (Job j : same) {
                    j.name = j.ownerBinary.replace('$', '.') + "." + j.method.getName();
                }
            }
        }
        // Still clashing: one class, two beans -- two @Bean factories of it -- and
        // each schedules its own, as in Spring. Named by the bean then, or the
        // second registration refused the start.
        Map<String, Integer> named = new LinkedHashMap<String, Integer>();
        for (Bean b : beans) {
            for (Job j : b.jobs) {
                Integer n = named.get(j.name);
                named.put(j.name, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
            }
        }
        for (Bean b : beans) {
            for (Job j : b.jobs) {
                if (named.get(j.name).intValue() > 1) {
                    j.name = b.name + "." + j.method.getName();
                }
            }
        }
        Map<String, Bean> tools = new LinkedHashMap<String, Bean>();
        Map<String, Bean> managed = new LinkedHashMap<String, Bean>();
        for (Bean b : beans) {
            for (Tool t : b.tools) {
                Bean other = tools.get(t.name);
                // The same bean twice is always a clash; two beans only when
                // they cannot be told apart by their conditions.
                if (other != null && (other == b || !(other.isConditional()
                        && b.isConditional()))) {
                    ctx.error(b.cls, "Two @McpTool methods are named \"" + t.name + "\" -- in "
                            + other.describe() + " and " + b.describe() + " -- and a tool is "
                            + "called by name. Give one a distinct name with "
                            + "@McpTool(name = ...).");
                } else if (other == null) {
                    tools.put(t.name, b);
                }
            }
            if (b.managed != null) {
                Bean other = managed.get(b.managed.objectName);
                if (other != null && !(other.isConditional() && b.isConditional())) {
                    ctx.error(b.cls, "Two @ManagedResource beans are named \""
                            + b.managed.objectName + "\" -- " + other.describe() + " and "
                            + b.describe() + " -- and operations are invoked by that name. "
                            + "Set objectName on one.");
                } else if (other == null) {
                    managed.put(b.managed.objectName, b);
                }
            }
        }
    }

    /// The types another bean must have for a @ConditionalOnMissingBean one to
    /// step aside. By default every type it is injected as except the JDK's --
    /// the concrete class alone would never match the application's own
    /// implementation of the interface the default provides, leaving both and
    /// an ambiguous injection -- and java.* and javax.* types are left out so a
    /// default that happens to be Closeable does not yield to every Closeable.
    private static List<String> stepAsideTypes(Bean b) {
        if (!b.missingTypes.isEmpty()) {
            return b.missingTypes;
        }
        List<String> out = new ArrayList<String>();
        for (String t : b.types) {
            if (!t.startsWith("java/") && !t.startsWith("javax/")) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            // A JDK type returned by a @Bean method: its own type is all it has.
            out.add(b.type);
        }
        return out;
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
    /**
     * The methods of {@code cls} and of the superclasses it inherits them from,
     * minus the ones a subclass overrides: a bean's @Scheduled, @McpTool and
     * managed methods are its own whether it declares or inherits them, as its
     * injection points and lifecycle methods already are. A non-public one in a
     * class this build does not compile needs a bridge it cannot add, and is an
     * error.
     */
    private final Set<String> reportedUnwoven = new HashSet<String>();

    /// Whether the bean has a method the build runs on an executor: `@Async` on
    /// the method, or on the class for its public instance methods.
    private boolean hasAsync(Bean b) {
        if (b.cls == null || b.mockType != null) {
            return false;
        }
        boolean onClass = b.cls.getClassAnnotation(ASYNC) != null;
        for (MethodInfo m : inheritedMembers(b.cls)) {
            if (m.getAnnotation(ASYNC) != null || (onClass && m.isPublic() && !m.isStatic()
                    && !m.isConstructor())) {
                return true;
            }
        }
        return false;
    }

    private List<MethodInfo> inheritedMembers(AnnotatedClass cls) {
        List<MethodInfo> out = new ArrayList<MethodInfo>();
        Set<String> seen = new HashSet<String>();
        AnnotatedClass c = cls;
        for (int depth = 0; c != null && depth < 64; depth++) {
            boolean unwoven = c != cls && !concerns(c);
            for (MethodInfo m : c.getMethods()) {
                if (m.isConstructor() || m.isStatic() && c != cls) {
                    continue;
                }
                String key = m.getName() + m.getDescriptor();
                if (!m.isPrivate() || c == cls) {
                    if (!seen.add(key)) {
                        continue;                  // overridden below
                    }
                } else if (seen.contains(key)) {
                    continue;
                }
                if (c != cls && unwoven && !m.isPublic() && callsFromWiring(m)) {
                    if (!reportedUnwoven.add(cls.getInternalName() + " " + key)) {
                        continue;                  // said once, not per collector
                    }
                    ctx.error(cls, cls.getSourceName() + " inherits " + c.getSourceName() + "."
                            + m.getName() + ", which is not public and so needs a bridge the "
                            + "build can only add to a class compiled from this project's "
                            + "sources. Make it public.");
                    continue;
                }
                out.add(m);
            }
            String sup = c.getSuperInternalName();
            c = sup == null || "java/lang/Object".equals(sup) ? null
                    : RestControllerAnnotationProcessor.resolveClass(ctx, sup);
        }
        return out;
    }

    /// Whether `m`, a member of `cls` or inherited by it, is woven to run as
    /// an @Async task: annotated itself, or a public instance method of a class
    /// annotated @Async. By the class that DECLARES it, exactly as
    /// collectAspects() weaves -- a class-level annotation reaches only that
    /// class's own methods, so an inherited method stays synchronous under a
    /// subclass's @Async, and reporting it as asynchronous refused a valid bean.
    private boolean wovenAsync(AnnotatedClass cls, MethodInfo m) {
        if (m.getAnnotation(ASYNC) != null) {
            return true;
        }
        if (!m.isPublic() || m.isStatic() || m.isConstructor() || m.isSynthetic()) {
            return false;
        }
        AnnotatedClass declaring = declaringClass(cls, m);
        return declaring != null && !declaring.isInterface()
                && declaring.getClassAnnotation(ASYNC) != null;
    }

    /// The class in `cls`'s superclass chain whose own methods include `m`, or
    /// null when none does.
    private AnnotatedClass declaringClass(AnnotatedClass cls, MethodInfo m) {
        AnnotatedClass c = cls;
        for (int depth = 0; c != null && depth < 64; depth++) {
            for (MethodInfo own : c.getMethods()) {
                if (own == m) { //NOPMD CompareObjectsWithEquals - the same parsed method, by identity
                    return c;
                }
            }
            String sup = c.getSuperInternalName();
            c = sup == null || "java/lang/Object".equals(sup) ? null
                    : RestControllerAnnotationProcessor.resolveClass(ctx, sup);
        }
        return null;
    }

    private void collectJobs(Bean bean) {
        AnnotatedClass cls = bean.cls;
        for (MethodInfo m : inheritedMembers(cls)) {
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
            if (wovenAsync(cls, m)) {
                // The scheduler would call the stub, which returns once the body
                // is queued: the run would count as over -- and a lock be
                // released -- while it is still going, so runs could overlap.
                ctx.error(cls, "@Scheduled method " + where + " is also @Async. A scheduled "
                        + "run already happens off the request thread, on the executor "
                        + "@Scheduled names, and it must end when its work does; drop "
                        + "@Async and pick the thread with @Scheduled(thread = ...).");
                continue;
            }
            if (returnsFuture(m)) {
                // Spring ignores a scheduled method's return value, and so does
                // this -- but a Future means work that outlives the run, so the
                // run ends, and its lock is released, while that work goes on.
                ctx.getLog().warn("cn1: @Scheduled method " + where + " returns a Future, "
                        + "which is ignored: the run ends -- and its lock is released -- "
                        + "when the method returns, not when that work does.");
            }
            Job job = new Job(m, RestClientAnnotationProcessor.simpleName(
                    cls.getBinaryName().replace('$', '.')) + "." + m.getName());
            job.ownerBinary = cls.getBinaryName();
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
        for (MethodInfo m : inheritedMembers(cls)) {
            AnnotationValues t = m.getAnnotation(MCP_TOOL);
            if (t == null) {
                continue;
            }
            String where = cls.getSourceName() + "." + m.getName();
            if (m.isStatic()) {
                ctx.error(cls, "@McpTool method " + where + " must be an instance method.");
                continue;
            }
            if (wovenAsync(cls, m)) {
                // The adapter would call the stub and receive the queued task at
                // once, and the agent would get neither the value nor the failure.
                ctx.error(cls, "@McpTool method " + where + " is also @Async. A tool call "
                        + "answers with what the method returns, so it must run to the "
                        + "end; drop @Async, or have the tool start the work and return an "
                        + "id to ask about it by.");
                continue;
            }
            if (returnsFuture(m)) {
                // Not only a woven stub's: a Future from any executor or API is
                // written as its toString() while it is still pending, a success
                // with a bogus body and its failure lost -- as a route refuses.
                ctx.error(cls, "@McpTool method " + where + " returns a Future. A tool call "
                        + "answers with what the method returns, so it would send the "
                        + "pending task, not its result; return the value, or start the work "
                        + "and return an id to ask about it by.");
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
            // The GENERIC types: the descriptor erases List<Integer> to List, and
            // an adapter that handed over the parser's list would give the tool
            // Longs where it declared Integers -- a ClassCastException on its
            // first read, for a call that was perfectly valid.
            String[] generic = RestClientAnnotationProcessor.parseGenericParameterSignatures(
                    m.getSignature(), args.length);
            BackendJsonCodecs codecs = BackendJsonCodecs.of(ctx);
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
                String declared = null;
                if (!bindable(args[i])) {
                    // Anything but a scalar is read the way a request body is,
                    // through the codecs -- as Spring AI reads a tool's arguments
                    // through Jackson -- so collections are filled with what they
                    // declare and a class of the application's own is built.
                    declared = RestClientAnnotationProcessor.javaTypeFor(args[i],
                            generic == null ? null : generic[i]);
                    String why = codecs.checkRead(declared);
                    if (why != null) {
                        ctx.error(cls, "Parameter " + (i + 1) + " of @McpTool " + where + " is "
                                + declared + ", which a tool call's arguments cannot be read "
                                + "into: it holds " + why + ".");
                        ok = false;
                        continue;
                    }
                }
                String paramName = p.getString("value");
                if (paramName == null || paramName.trim().length() == 0
                        || tool.paramNames.contains(paramName)) {
                    // One JSON property per name: a second with the same name
                    // would overwrite the first in the schema and be read for both.
                    ctx.error(cls, "Parameter " + (i + 1) + " of @McpTool " + where + " is "
                            + (paramName == null || paramName.trim().length() == 0
                            ? "named nothing" : "named \"" + paramName + "\" like another")
                            + "; each argument needs a name of its own.");
                    ok = false;
                    continue;
                }
                tool.paramNames.add(paramName);
                tool.paramDescriptions.add(p.getStringOrDefault("description", ""));
                tool.paramRequired.add(Boolean.valueOf(p.getBoolOrDefault("required", true)));
                tool.paramTypes.add(declared);
                tool.paramSchema.add(declared == null ? null : codecs.schemaType(declared));
                tool.paramRead.add(declared == null ? null : codecs.readStatements(declared,
                        "j", "com.codename1.impl.backend.JsonCodec.Path.ROOT",
                        BackendJsonCodecs.javaString(paramName), "-1", "0", "target", ""));
            }
            Type returned = Type.getReturnType(m.getDescriptor());
            if (ok && returned.getSort() != Type.VOID
                    && !"java/lang/String".equals(returned.getSort() == Type.OBJECT
                    ? returned.getInternalName() : "")) {
                // Written by a codec, as a route's result is: Json.write knows no
                // class of the application's own and would send its toString().
                String sig = m.getSignature();
                String declared = RestClientAnnotationProcessor.javaTypeFor(returned,
                        sig == null ? null : sig.substring(sig.lastIndexOf(')') + 1));
                String why = codecs.checkWrite(declared);
                if (why != null) {
                    ctx.error(cls, "@McpTool " + where + " returns " + declared + ", which "
                            + "cannot be written as JSON: it holds " + why + ".");
                    ok = false;
                } else {
                    tool.returnType = declared;
                    tool.returnWrite = codecs.writeStatements(declared, "result", "0", "");
                }
            }
            if (ok) {
                bean.tools.add(tool);
            }
        }
    }

    private void collectManaged(Bean bean) {
        AnnotatedClass cls = bean.cls;
        AnnotationValues resource = cls.getClassAnnotation(MANAGED_RESOURCE);
        boolean any = resource != null;
        for (MethodInfo m : inheritedMembers(cls)) {
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
        if (!SINGLETON.equals(bean.scope)) {
            // Its gauges are read by the metrics exporter and its operations by
            // the management endpoint, on threads with no request or session to
            // find a scoped instance in -- every read would fail and the gauge
            // would silently export nothing. A prototype has no one instance.
            ctx.error(cls, cls.getSourceName() + " is a @ManagedResource with scope "
                    + bean.scope + ". A managed resource is read outside any request, so it "
                    + "must be a singleton; keep the per-" + bean.scope + " state in a "
                    + "singleton it reports on.");
            return;
        }
        Managed managed = new Managed();
        String objectName = resource.getStringOrDefault("objectName", "").trim();
        managed.objectName = objectName.length() > 0 ? objectName
                : RestClientAnnotationProcessor.simpleName(cls.getBinaryName().replace('$', '.'));
        managed.description = resource.getStringOrDefault("description", "");
        if (!managed.objectName.matches("[A-Za-z0-9_.-]{1,128}")) {
            // It is one segment of /manage/managed/{bean}/{operation}, matched
            // without decoding: a '/' makes two segments, and anything escaped
            // never matches -- listed, and never invocable.
            ctx.error(cls, "@ManagedResource on " + cls.getSourceName() + " is named \""
                    + managed.objectName + "\"; an objectName is letters, digits, _, - and . "
                    + "only, since it is a segment of the management URL.");
            return;
        }
        for (MethodInfo m : inheritedMembers(cls)) {
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
                if (returnsFuture(m) || wovenAsync(cls, m)) {
                    // Read and written as its value: a pending task -- a woven
                    // @Async getter's, or any Future -- came out as the task's
                    // toString(), a successful reading of nothing, its failure lost.
                    ctx.error(cls, "@ManagedAttribute " + where + " returns a Future or is "
                            + "@Async. An attribute is read as the value its getter returns, so "
                            + "the reader would get a pending task; return the value itself.");
                    continue;
                }
                if (BackendSources.gaugeRead(ret, "v") == null) {
                    // Published as a gauge, so a number or a boolean -- what the
                    // annotation promises. Anything else was listed under
                    // /manage/managed but silently missing from the metrics.
                    ctx.error(cls, "@ManagedAttribute " + where + " returns "
                            + ret.getClassName() + "; an attribute is a gauge, so it returns "
                            + "a number or a boolean, primitive or boxed.");
                    continue;
                }
                if (managed.attributeNames.contains(attributeName(m.getName()))) {
                    ctx.error(cls, "@ManagedAttribute " + where + " has the attribute name "
                            + attributeName(m.getName()) + ", which another getter of "
                            + cls.getSourceName() + " already reports under.");
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
                if (returnsFuture(m)) {
                    // The management endpoint answers with the result, so an @Async
                    // one's queued task -- or any pending Future -- was sent as its
                    // toString(), a success while the work still ran.
                    ctx.error(cls, "@ManagedOperation " + where + " returns a Future. An "
                            + "operation answers with what the method returns, so the caller "
                            + "would get the pending task, not its result; return the value, "
                            + "or return nothing and let @Async run it in the background.");
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
                    String paramName = named != null ? named.getString("value") : "arg" + i;
                    if (paramName == null || paramName.trim().length() == 0
                            || names.contains(paramName)) {
                        ctx.error(cls, "Parameter " + (i + 1) + " of @ManagedOperation "
                                + where + " needs a name of its own: arguments are passed "
                                + "by name.");
                        ok = false;
                    }
                    names.add(paramName);
                }
                for (MethodInfo other : managed.operations) {
                    if (other.getName().equals(m.getName())) {
                        // Management and MCP name an operation by its method
                        // name alone, so an overload could never be reached.
                        ctx.error(cls, "@ManagedOperation " + where + " is overloaded; an "
                                + "operation is invoked by name, so only one method of "
                                + "that name can be one. Rename the others.");
                        ok = false;
                        break;
                    }
                }
                // The result goes out as JSON. Json.write knows no class of the
                // application's own and would send its toString(), so anything
                // beyond a scalar is written by the codecs -- or refused here.
                String returnType = null;
                String returnWrite = null;
                Type returned = Type.getReturnType(m.getDescriptor());
                if (ok && returned.getSort() != Type.VOID && !bindable(returned)) {
                    String sig = m.getSignature();
                    returnType = RestClientAnnotationProcessor.javaTypeFor(returned,
                            sig == null ? null : sig.substring(sig.lastIndexOf(')') + 1));
                    String why = BackendJsonCodecs.of(ctx).checkWrite(returnType);
                    if (why != null) {
                        ctx.error(cls, "@ManagedOperation " + where + " returns " + returnType
                                + ", which cannot be written as JSON: it holds " + why + ".");
                        ok = false;
                    } else {
                        returnWrite = BackendJsonCodecs.of(ctx).writeStatements(returnType,
                                "result", "0", "");
                    }
                }
                if (ok) {
                    managed.operations.add(m);
                    managed.operationDescriptions.add(op.getStringOrDefault("description", ""));
                    managed.operationParams.add(names);
                    managed.operationReturnTypes.add(returnWrite == null ? null : returnType);
                    managed.operationWrites.add(returnWrite);
                }
            }
        }
        bean.managed = managed;
    }

    /// Future types the JDK and the backend provide, which the class index
    /// cannot look inside.
    private static final Set<String> FUTURE_TYPES = new HashSet<String>(Arrays.asList(
            "java/util/concurrent/Future", "java/util/concurrent/RunnableFuture",
            "java/util/concurrent/ScheduledFuture", "java/util/concurrent/RunnableScheduledFuture",
            "java/util/concurrent/FutureTask", "java/util/concurrent/CompletableFuture",
            "java/util/concurrent/ForkJoinTask", "com/codename1/backend/AsyncResult",
            "com/codename1/impl/backend/AsyncTask"));

    /// Whether `m` returns a Future: declared as one, or a type implementing it.
    private boolean returnsFuture(MethodInfo m) {
        Type ret = Type.getReturnType(m.getDescriptor());
        return ret.getSort() == Type.OBJECT
                && isFutureType(ret.getInternalName(), new HashSet<String>());
    }

    private boolean isFutureType(String internal, Set<String> seen) {
        if (internal == null || !seen.add(internal)) {
            return false;
        }
        if (FUTURE_TYPES.contains(internal)) {
            return true;
        }
        AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(ctx, internal);
        if (c == null) {
            return false;
        }
        for (String i : c.getInterfaceInternalNames()) {
            if (isFutureType(i, seen)) {
                return true;
            }
        }
        return isFutureType(c.getSuperInternalName(), seen);
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
            if (isSecurityChain(b) && !b.isEager()) {
                // The chains are handed to the server once, when it starts: one
                // built later, or once per request, would guard nothing.
                ctx.error(where, "SecurityFilterChain " + b.describe() + " is "
                        + (b.lazy ? "@Lazy" : b.scope + "-scoped") + "; the server takes its "
                        + "chains when it starts, so a chain must be an ordinary singleton.");
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
            if (!b.jobs.isEmpty() && PROTOTYPE.equals(b.scope)) {
                // A warning, as Spring runs it: its post-processor schedules the
                // instance it is given and never destroys a prototype either. But
                // not silent -- the one instance built for the jobs runs them for
                // the life of the server, like a singleton, and its @PreDestroy or
                // destroy method is never called.
                ctx.getLog().warn("cn1: " + b.describe() + " has @Scheduled methods but is "
                        + "prototype-scoped. One instance is built to run its jobs and keeps "
                        + "running them for the life of the server, and a prototype is never "
                        + "destroyed, so its @PreDestroy never runs. Make it a singleton if "
                        + "that is what it is.");
            }
        }
        // Every candidate is resolved now, so what runs outside any HTTP request
        // can be checked through the whole graph: a websocket callback, a
        // scheduled run and a managed-attribute read all happen on threads with no
        // request current, and a request- or session-scoped stand-in reached from
        // there -- directly, or through a singleton that injects it -- throws on
        // first use.
        //
        // A WARNING, as Spring has it: Spring starts such an application and
        // the scoped proxy throws IllegalStateException only when it is really
        // used with no request current, which the generated stand-in does too.
        // Reachability through the graph is an over-approximation -- the job may
        // never call the method that touches the scoped bean -- so refusing the
        // build would reject programs Spring runs correctly.
        for (Bean b : beans) {
            boolean async = hasAsync(b);
            String role = b.webSocket ? "Websocket endpoint"
                    : !b.jobs.isEmpty() ? "Bean with @Scheduled methods"
                    : b.managed != null ? "@ManagedResource bean"
                    : async ? "Bean with @Async methods" : null;
            if (role == null || b.cls == null) {
                continue;
            }
            List<Bean> path = scopedReach(b, new ArrayList<Bean>(), new HashSet<Bean>());
            if (path == null) {
                continue;
            }
            Bean d = path.get(path.size() - 1);
            StringBuilder via = new StringBuilder();
            for (int i = 1; i < path.size() - 1; i++) {
                via.append(i == 1 ? " through " : " -> ").append(path.get(i).describe());
            }
            String outside = b.webSocket ? "A websocket callback runs"
                    : !b.jobs.isEmpty() ? "A scheduled job runs"
                    : b.managed != null ? "A managed attribute is read"
                    : "An @Async call runs on an executor, even one a request made,";
            ctx.getLog().warn("cn1: " + role + " " + b.describe() + " reaches " + d.describe()
                    + via + ", which is " + d.scope + "-scoped. " + outside + " outside any "
                    + "HTTP request, where there is no " + d.scope + " to find it in, and using "
                    + "it there throws IllegalStateException. Inject a singleton instead"
                    + (b.webSocket ? ", and keep per-connection state in the "
                    + "WebSocketSession's attachment" : "") + ", unless that path never "
                    + "touches it.");
        }
        if (!ctx.hasErrors()) {
            orderConstruction();
        }
    }

    /// The path from `from` to the first request- or session-scoped bean it
    /// injects, directly or through other beans; null when there is none.
    private List<Bean> scopedReach(Bean from, List<Bean> path, Set<Bean> seen) {
        if (!seen.add(from)) {
            return null;
        }
        path.add(from);
        if (path.size() > 1 && (REQUEST.equals(from.scope) || SESSION.equals(from.scope))) {
            return path;
        }
        List<Point> all = new ArrayList<Point>(from.constructorPoints);
        all.addAll(from.fields.values());
        for (Call c : from.setters) {
            all.addAll(c.points);
        }
        for (Point p : all) {
            for (Bean d : p.candidates) {
                List<Bean> found = scopedReach(d, path, seen);
                if (found != null) {
                    return found;
                }
            }
        }
        path.remove(path.size() - 1);
        return null;
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
        if (HTTP_SECURITY_TYPE.equals(type)) {
            // A new one at every injection point, like a prototype: each chain
            // method configures and builds its own. It is given the beans it may
            // pick a user store, a password encoder and providers from, which
            // also puts them before the chain in construction order.
            p.builtin = "httpSecurity";
            for (Bean b : beans) {
                if (b == owner || b == owner.owner || p.candidates.contains(b)) {
                    continue;
                }
                for (String shared : SECURITY_SHARED_TYPES) {
                    if (b.types.contains(shared)) {
                        p.candidates.add(b);
                        break;
                    }
                }
            }
            return;
        }
        if (REQUEST_TYPE.equals(type) || HTTP_SESSION_TYPE.equals(type)) {
            if (!REQUEST.equals(owner.scope) && !SESSION.equals(owner.scope)) {
                ctx.error(where, p.where + " asks for the current "
                        + (REQUEST_TYPE.equals(type) ? "request" : "session") + ", which only "
                        + "a @Scope(\"request\") or @Scope(\"session\") bean has. Take it as a parameter of "
                        + "the handler method instead, or scope the bean.");
                return;
            }
            if (SESSION.equals(owner.scope)) {
                // Both are the objects of the request that BUILT the bean. The
                // request is gone when it ends; the session is a per-request copy
                // with the database store, so the bean would read stale
                // attributes and write to a copy nobody saves.
                ctx.error(where, p.where + " asks for the " + (REQUEST_TYPE.equals(type)
                        ? "request" : "session") + ", but a session bean outlives the request "
                        + "that built it and the copy of the session that request loaded. "
                        + "Read the current one when it is needed: "
                        + "Backend.currentRequest().getSession(true).");
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
                sortByOrder(p.candidates);
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
                        + "or declare a @Bean method returning one.");
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
            if (primaries.size() == 1 && primaries.get(0).isConditional()) {
                // A conditional @Primary may not exist at run time -- a prod-only
                // primary on a dev profile -- and then the others are the answer.
                // Discarding them here made that profile fail to start.
                Bean primary = primaries.get(0);
                List<Bean> ordered = new ArrayList<Bean>();
                ordered.add(primary);
                int unconditional = 0;
                for (Bean b : matches) {
                    if (b != primary) {
                        ordered.add(b);
                        if (!b.isConditional()) {
                            unconditional++;
                        }
                    }
                }
                if (unconditional > 1) {
                    ctx.error(where, p.where + " could receive any of " + names(ordered)
                            + " whenever the @Primary " + primary.describe() + " is not "
                            + "active. Mark the rest conditional, or name one with @Qualifier.");
                    return;
                }
                matches = ordered;
                p.choice = true;
                p.preferFirst = true;
            } else if (primaries.size() == 1) {
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
        // Candidates that are all conditional are accepted here and checked at
        // START, as Spring checks them: whether @Profile or @ConditionalOnProperty
        // holds is a fact of the deployment, not of the build, and
        // Wiring.single() refuses the start naming this injection point when none
        // is on. Proving at build time that every combination of profiles and
        // properties is covered is not attempted -- Spring does not either.
        p.candidates.addAll(matches);
    }

    /// Orders `list` by @Order, lowest first. Stable, so beans with the same
    /// value -- every bean that has none, usually -- keep the order the build
    /// found them in, which is the order such a list always had.
    static void sortByOrder(List<Bean> list) {
        Collections.sort(list, new java.util.Comparator<Bean>() {
            @Override
            public int compare(Bean a, Bean b) {
                return a.order < b.order ? -1 : a.order == b.order ? 0 : 1;
            }
        });
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
        String what = b.lazy ? "@Lazy" : "@Scope(\"" + (REQUEST.equals(b.scope) ? "request"
                : "session") + "\")";
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
        String unreadable = unreadableAncestor(b.cls);
        if (unreadable != null) {
            ctx.error(where, what + " bean " + b.describe() + " extends " + unreadable
                    + ", which is not on the build's classpath, so the stand-in the build "
                    + "generates cannot forward the methods it inherits from it.");
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
        String name = qualify(pkg, baseName(b.type) + (b.lazy ? "Cn1Lazy" : "Cn1Scoped"));
        // Per bean, not per type: two @Bean methods of one class, both scoped or
        // lazy, each need their own stand-in -- sharing the name, the second
        // source replaced the first and both injections built the second bean.
        String unique = name;
        for (int n = 2 ; !proxyNames.add(unique) ; n++) {
            unique = name + n;
        }
        b.proxyBinary = unique;
    }

    /// Stand-in class names already given out, so two beans never share one.
    private final Set<String> proxyNames = new HashSet<String>();

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
                        || BackendWeaver.isBody(m.getName())
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
            // On through the compile classpath, not just this project's classes:
            // a public method a library base class declares is called on the
            // stand-in too, and unforwarded it would run on the stand-in's own,
            // never-initialized state instead of the scoped bean's.
            String parent = c.getSuperInternalName();
            c = parent == null || "java/lang/Object".equals(parent) ? null
                    : RestControllerAnnotationProcessor.resolveClass(ctx, parent);
        }
        return out;
    }

    /// The first superclass of `cls` the build cannot read, or null.
    private String unreadableAncestor(AnnotatedClass cls) {
        AnnotatedClass c = cls;
        while (c != null) {
            String parent = c.getSuperInternalName();
            if (parent == null || "java/lang/Object".equals(parent)) {
                return null;
            }
            AnnotatedClass next = RestControllerAnnotationProcessor.resolveClass(ctx, parent);
            if (next == null) {
                return parent.replace('/', '.');
            }
            c = next;
        }
        return null;
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

    /// A class-level @Transactional or @Async covers the public methods the class
    /// DECLARES, as in Spring, whose reference says an inherited method has to be
    /// redeclared to take part: the weaver rewrites bodies in place, and an
    /// inherited body lives in another class. Silence would let such a method
    /// run outside the transaction or on the caller's thread unnoticed, so the
    /// build names each one.
    /// Refuses @Transactional, @Async, @Timed and @Counted on a project interface.
    /// The build weaves classes, not interfaces, so such an annotation -- on a
    /// default method, an abstract one, or the interface itself -- is applied to
    /// nothing: a transactional default method committed each write on its own.
    /// Spring's runtime proxies would honour it, so ignoring it silently is the
    /// one answer that cannot be right.
    private void refuseInterfaceAspects(AnnotatedClass cls) {
        String[] names = {TRANSACTIONAL, ASYNC, TIMED, COUNTED, PRE_AUTHORIZE, SECURED,
            ROLES_ALLOWED, PERMIT_ALL, DENY_ALL};
        String[] shown = {"@Transactional", "@Async", "@Timed", "@Counted", "@PreAuthorize",
            "@Secured", "@RolesAllowed", "@PermitAll", "@DenyAll"};
        for (int i = 0; i < names.length; i++) {
            if (cls.getClassAnnotation(names[i]) != null) {
                ctx.error(cls, shown[i] + " on interface " + cls.getSourceName() + " is not "
                        + "applied: the build weaves classes, not interfaces. Put it on the "
                        + "implementing class.");
            }
        }
        for (MethodInfo m : cls.getMethods()) {
            for (int i = 0; i < names.length; i++) {
                if (m.getAnnotation(names[i]) != null) {
                    ctx.error(cls, shown[i] + " on " + cls.getSourceName() + "."
                            + m.getName() + " is not applied: the build weaves classes, not "
                            + "interfaces, so neither " + (m.isAbstract() ? "an implementation"
                            : "this default method") + " would get it. Put it on the "
                            + "implementing class's method.");
                }
            }
        }
    }

    private void warnInheritedOutsideClassAspect(AnnotatedClass cls, String what) {
        Set<String> declared = new HashSet<String>();
        for (MethodInfo m : cls.getMethods()) {
            declared.add(m.getName() + m.getDescriptor());
        }
        String sup = cls.getSuperInternalName();
        AnnotatedClass c = sup == null || "java/lang/Object".equals(sup) ? null
                : RestControllerAnnotationProcessor.resolveClass(ctx, sup);
        for (int depth = 0; c != null && depth < 64; depth++) {
            for (MethodInfo m : c.getMethods()) {
                String key = m.getName() + m.getDescriptor();
                if (m.isPublic() && !m.isStatic() && !m.isConstructor() && !m.isSynthetic()
                        && declared.add(key)) {
                    ctx.getLog().warn("cn1: " + cls.getSourceName() + " is " + what
                            + ", which covers the methods it declares; " + m.getName()
                            + " is inherited from " + c.getSourceName() + " and runs without "
                            + "it. Override it in " + cls.getSourceName() + " to include it.");
                }
            }
            sup = c.getSuperInternalName();
            c = sup == null || "java/lang/Object".equals(sup) ? null
                    : RestControllerAnnotationProcessor.resolveClass(ctx, sup);
        }
    }

    /// Every project class with a transactional, async, timed or counted method:
    /// beans or not, since the rewrite applies to `new` as much as to injection.
    /// Refuses one named executor asked for two kinds of thread. An executor is
    /// created once, by whichever declaration runs first, so the other's kind
    /// would be ignored depending on call order -- a PLATFORM method written to
    /// block in SQLite could end up on the virtual hosts. AUTO agrees with either;
    /// unnamed executors are already named by their kind.
    ///
    /// There is deliberately no warning for a VIRTUAL method that injects a
    /// DataSource. A PostgreSQL or MySQL query parks its virtual thread as any
    /// socket wait does; only SQLite blocks the host, and which engine a
    /// DataSource reaches is cn1.datasource.url, read at start-up from a
    /// deployment's environment -- so the build cannot tell, and warning on every
    /// such method would be wrong for the engines a server is usually deployed
    /// against. Spring does not warn here either.
    private void checkExecutorKinds() {
        Map<String, String[]> kinds = new TreeMap<String, String[]>();
        for (Aspects owner : aspects.values()) {
            for (Aspect a : owner.methods) {
                if (a.async != null) {
                    noteExecutorKind(kinds, a.async.getStringOrDefault("value", ""),
                            enumName(a.async.get("thread"), "PLATFORM"), owner.cls,
                            "@Async " + owner.cls.getSourceName() + "." + a.method.getName());
                }
            }
        }
        for (Bean b : beans) {
            for (Job j : b.jobs) {
                noteExecutorKind(kinds, j.executor, j.thread, b.cls,
                        "@Scheduled " + j.method.getName() + " of " + b.describe());
            }
        }
    }

    private void noteExecutorKind(Map<String, String[]> kinds, String executor, String thread,
                                  AnnotatedClass where, String who) {
        if (executor == null || executor.trim().length() == 0 || "AUTO".equals(thread)) {
            return;
        }
        String name = executor.trim();
        String[] seen = kinds.get(name);
        if (seen == null) {
            kinds.put(name, new String[] {thread, who});
        } else if (!seen[0].equals(thread)) {
            ctx.error(where, "Executor \"" + name + "\" is asked for " + seen[0] + " threads by "
                    + seen[1] + " and for " + thread + " threads by " + who + ". One executor "
                    + "has one kind of thread, and the first to run would decide; give them "
                    + "different executor names, or the same thread kind.");
        }
    }

    /// Claims the instruments `m`'s @Timed and @Counted register -- named as
    /// BackendSources names them -- and refuses a name used for both kinds. At
    /// run time the second registration throws, and it happens in the woven
    /// finally, AFTER the body ran: a call whose work succeeded would be reported
    /// as failed, and possibly retried. Metrics are the process's, so a clash
    /// between two methods counts as much as one within a method.
    private void claimAspectMetrics(AnnotatedClass cls, MethodInfo m, AnnotationValues timed,
                                    AnnotationValues counted, String where) {
        String base = cls.getBinaryName() + "." + m.getName();
        if (timed != null) {
            String name = timed.getStringOrDefault("value", "");
            claimAspectMetric(cls, name.length() > 0 ? name : base + ".duration", "histogram",
                    "@Timed on " + where);
        }
        if (counted != null) {
            String name = counted.getStringOrDefault("value", "");
            String calls = name.length() > 0 ? name : base + ".calls";
            claimAspectMetric(cls, calls, "counter", "@Counted on " + where);
            claimAspectMetric(cls, calls + ".failures", "counter", "@Counted on " + where);
        }
    }

    private void claimAspectMetric(AnnotatedClass cls, String name, String kind, String by) {
        String[] earlier = aspectMetrics.get(name);
        if (earlier != null) {
            if ("built-in".equals(earlier[0])) {
                ctx.error(cls, "Metric " + name + " for " + by + " is one of the server's own "
                        + "instruments; give it another name.");
                return;
            }
            if (!earlier[0].equals(kind)) {
                ctx.error(cls, "Metric " + name + " is a " + kind + " for " + by + " and a "
                        + earlier[0] + " for " + earlier[1] + ". One name cannot be both: "
                        + "give one of them another name.");
            }
            return;
        }
        aspectMetrics.put(name, new String[] {kind, by});
        // And the Prometheus series it exports, as Metrics.claimPrometheusNames
        // claims them at run time: two DIFFERENT names that fold alike --
        // latency.ms and latency_ms, or a counter's _total and a histogram's
        // series -- are refused there, in the woven finally, after the body ran.
        String base = promName(name);
        String[] series = "counter".equals(kind) ? new String[] {base + "_total"}
                : new String[] {base, base + "_bucket", base + "_sum", base + "_count"};
        for (String element : series) {
            String[] owner = aspectSeries.get(element);
            if (owner != null && !owner[0].equals(name)) {
                ctx.error(cls, "Metric " + name + " (" + by + ") would be exported to "
                        + "Prometheus as " + element + ", which metric " + owner[0] + " ("
                        + owner[1] + ") already is; rename one.");
                return;
            }
        }
        for (String element : series) {
            aspectSeries.put(element, new String[] {name, by});
        }
    }

    /// A metric name in Prometheus's alphabet -- the same fold Metrics.promName
    /// applies at run time, which this must match exactly.
    static String promName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
                    || c == ':' || (iter > 0 && c >= '0' && c <= '9');
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }

    private void collectAspects() {
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            if (concerns(cls) && cls.isInterface()) {
                refuseInterfaceAspects(cls);
                continue;
            }
            if (!concerns(cls)) {
                continue;
            }
            AnnotationValues classTx = cls.getClassAnnotation(TRANSACTIONAL);
            AnnotationValues classAsync = cls.getClassAnnotation(ASYNC);
            if (classTx != null || classAsync != null) {
                warnInheritedOutsideClassAspect(cls, classTx != null ? "@Transactional"
                        : "@Async");
            }
            refuseUnsupportedSecurity(cls);
            String[] classSecurityShown = new String[1];
            AnnotationValues classSecurity = methodSecurity(cls.getClassAnnotations(), cls,
                    classSecurityShown);
            if (classSecurity != null && !PERMIT_ALL.equals(classSecurity.getDescriptor())) {
                // The one that matters most to hear about: an inherited method of
                // a guarded class is open.
                warnInheritedOutsideClassAspect(cls, classSecurityShown[0]);
            }
            boolean usesSecurity = classSecurity != null;
            Aspects found = null;
            for (MethodInfo m : cls.getMethods()) {
                if (m.isConstructor() || m.isSynthetic() || "<clinit>".equals(m.getName())
                        || BackendWeaver.isBody(m.getName())
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
                if (async != null && (m.getAnnotation(POST_CONSTRUCT) != null
                        || m.getAnnotation(PRE_DESTROY) != null)) {
                    // Spring calls a lifecycle method on the bean itself, not
                    // through its proxy, so @Async never applies to it -- and here
                    // it would be worse than ignored: @PostConstruct would return
                    // before initialising, and @PreDestroy runs after the executors
                    // stop, so its body would never run at all.
                    ctx.getLog().warn("cn1: " + cls.getSourceName() + "." + m.getName()
                            + " is a lifecycle method, which runs synchronously whatever "
                            + "@Async says -- as Spring runs it.");
                    async = null;
                }
                AnnotationValues timed = m.getAnnotation(TIMED);
                AnnotationValues counted = m.getAnnotation(COUNTED);
                String where = cls.getSourceName() + "." + m.getName();
                AnnotationValues security = methodSecurity(m.getAnnotations(), cls, null);
                boolean lifecycle = m.getAnnotation(POST_CONSTRUCT) != null
                        || m.getAnnotation(PRE_DESTROY) != null || m.getAnnotation(BEAN) != null
                        || m.getAnnotation(AUTOWIRED) != null;
                if (security != null) {
                    usesSecurity = true;
                    if (lifecycle && !PERMIT_ALL.equals(security.getDescriptor())) {
                        ctx.error(cls, where + " is called by the server while it builds and "
                                + "destroys its beans, with nobody signed in, so an "
                                + "authorization annotation on it could only ever refuse. "
                                + "Remove it.");
                        continue;
                    }
                } else if (classSecurity != null && m.isPublic() && !m.isStatic() && !lifecycle) {
                    // The class's default covers the public methods it declares,
                    // but not the ones the server itself calls on the bean -- as
                    // Spring calls those on the object, not through its proxy.
                    security = classSecurity;
                }
                if (security != null && PERMIT_ALL.equals(security.getDescriptor())) {
                    security = null;
                }
                if (tx == null && async == null && timed == null && counted == null
                        && security == null) {
                    continue;
                }
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
                if (security != null) {
                    compileSecurity(cls, m, security, a, where);
                }
                a.transactional = tx;
                a.async = async;
                a.timed = timed;
                a.counted = counted;
                claimAspectMetrics(cls, m, timed, counted, where);
                if (async != null) {
                    String pkg = RestClientAnnotationProcessor.packageOf(cls.getBinaryName());
                    a.asyncTaskBinary = qualify(pkg, baseName(cls.getInternalName())
                            + "Cn1Async" + found.methods.size());
                }
                found.methods.add(a);
            }
            if (usesSecurity && !hasSecurityChains()) {
                ctx.error(cls, cls.getSourceName() + " uses method security, and this module "
                        + "has no SecurityFilterChain bean: without a chain nobody ever signs "
                        + "in, so every guarded method would refuse every caller. Declare "
                        + "one -- a @Bean method that takes an HttpSecurity and returns "
                        + "http.build() -- or remove the annotations.");
            }
        }
    }

    /// The simple name of a Spring Security annotation the build has nothing
    /// for, or null.
    private static String unsupportedSecurity(String descriptor) {
        for (String name : UNSUPPORTED_SECURITY) {
            if (descriptor.endsWith("/" + name + ";")) {
                return name;
            }
        }
        return null;
    }

    private void refuseUnsupportedSecurity(AnnotatedClass cls) {
        for (String d : cls.getClassAnnotations().keySet()) {
            refuseUnsupportedSecurity(cls, d, cls.getSourceName());
        }
        for (MethodInfo m : cls.getMethods()) {
            for (String d : m.getAnnotations().keySet()) {
                refuseUnsupportedSecurity(cls, d, cls.getSourceName() + "." + m.getName());
            }
        }
    }

    private void refuseUnsupportedSecurity(AnnotatedClass cls, String descriptor, String where) {
        String name = unsupportedSecurity(descriptor);
        if (name == null) {
            return;
        }
        ctx.error(cls, "@" + name + " on " + where + " is not supported: " + ("PostAuthorize"
                .equals(name) ? "the check is compiled in before the method runs and never "
                + "sees what it returns. Decide from the arguments with @PreAuthorize, or "
                + "check the result in the method and throw AccessDeniedException."
                : "the build does not rewrite a method's collections. Filter in the method, "
                + "or in the query that loads them."));
    }

    /// The one method-security annotation among `annotations`, or null. More
    /// than one is an error when `where` is given: which of two rules wins is
    /// not something to leave to an order nobody wrote down.
    private AnnotationValues methodSecurity(Map<String, AnnotationValues> annotations,
                                            AnnotatedClass where, String[] shown) {
        AnnotationValues found = null;
        String foundShown = null;
        for (int i = 0; i < METHOD_SECURITY.length; i++) {
            AnnotationValues v = annotations.get(METHOD_SECURITY[i]);
            if (v == null) {
                continue;
            }
            if (found != null && where != null) {
                ctx.error(where, where.getSourceName() + " carries both " + foundShown + " and "
                        + METHOD_SECURITY_SHOWN[i] + " on one element; one rule decides a "
                        + "call. Combine them in one @PreAuthorize expression.");
                return found;
            }
            if (found == null) {
                found = v;
                foundShown = METHOD_SECURITY_SHOWN[i];
            }
        }
        if (shown != null) {
            shown[0] = foundShown;
        }
        return found;
    }

    /// Turns `security` into the Java expression `a`'s woven check evaluates.
    private void compileSecurity(AnnotatedClass cls, MethodInfo m, AnnotationValues security,
                                 Aspect a, String where) {
        String d = security.getDescriptor();
        if (DENY_ALL.equals(d)) {
            a.security = "false";
            return;
        }
        if (SECURED.equals(d) || ROLES_ALLOWED.equals(d)) {
            boolean roles = ROLES_ALLOWED.equals(d);
            List<String> names = new ArrayList<String>();
            for (String name : strings(security.get("value"))) {
                String trimmed = name.trim();
                if (trimmed.length() > 0) {
                    names.add(roles && !trimmed.startsWith("ROLE_") ? "ROLE_" + trimmed : trimmed);
                }
            }
            if (names.isEmpty()) {
                ctx.error(cls, (roles ? "@RolesAllowed" : "@Secured") + " on " + where
                        + " names nothing, so nobody could call it; use @DenyAll to say that.");
                return;
            }
            a.security = MethodSecurityCompiler.anyAuthority(names);
            return;
        }
        try {
            MethodSecurityCompiler.Result compiled = new MethodSecurityCompiler(this, cls, m)
                    .compile(security.getStringOrDefault("value", ""));
            a.security = compiled.java;
            a.securityBeans = compiled.beans;
            namedBeans.addAll(compiled.beans.keySet());
        } catch (MethodSecurityCompiler.Refused refused) {
            ctx.error(cls, "@PreAuthorize(\"" + security.getStringOrDefault("value", "")
                    + "\") on " + where + ": " + refused.getMessage() + ".");
        }
    }

    /// For a test's wiring: the beans the application's expressions call, which
    /// the main build compiled the checks against and this wiring must register
    /// under the same names.
    private void collectNamedBeans() {
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            AnnotationValues onClass = cls.getClassAnnotation(PRE_AUTHORIZE);
            if (onClass != null) {
                namedBeans.addAll(MethodSecurityCompiler.beanNames(
                        onClass.getStringOrDefault("value", "")));
            }
            for (MethodInfo m : cls.getMethods()) {
                AnnotationValues onMethod = m.getAnnotation(PRE_AUTHORIZE);
                if (onMethod != null) {
                    namedBeans.addAll(MethodSecurityCompiler.beanNames(
                            onMethod.getStringOrDefault("value", "")));
                }
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
            if (testWeaveable != null && !testWeaveable.contains(cls.getInternalName())) {
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
            boolean stereotyped = isStereotyped(cls) || (testConfigurations != null
                    && testConfigurations.contains(cls.getInternalName()));
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
        // The codecs the tools use, compiled with them: the adapters name them.
        // The router processor compiles the same set again later, grown by its
        // own routes, over these.
        boolean toolCodecs = false;
        for (Bean b : beans) {
            for (Tool t : b.tools) {
                toolCodecs |= t.returnType != null || t.paramTypes.size()
                        > Collections.frequency(t.paramTypes, null);
            }
            if (b.managed != null) {
                toolCodecs |= b.managed.operationWrites.size()
                        > Collections.frequency(b.managed.operationWrites, null);
            }
        }
        if (toolCodecs) {
            sources.putAll(BackendJsonCodecs.of(ctx).sources());
            if (ctx.hasErrors()) {
                return;
            }
        }
        // EVERY name about to be generated -- aspects, proxies, tool and managed
        // adapters, codecs -- checked against the project's own classes. A class
        // of the application's that happened to have one of these names would be
        // overwritten in the output directory by what is compiled here, silently,
        // and code compiled against the original would fail at run time.
        for (String generated : sources.keySet()) {
            AnnotatedClass existing = ctx.lookup(generated.replace('.', '/'));
            if (existing != null && !existing.getClassAnnotations().containsKey(
                    "Lcom/codename1/backend/annotations/Generated;")) {
                ctx.error(existing, generated + " already exists, and the class the build "
                        + "generates under that name would replace it. Rename that class.");
                return;
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
    /// The class's name in its package, as a Java identifier for the support
    /// classes named after it. Injective: `_` becomes `__` and `$` becomes `_S`,
    /// so every escape is `_` plus a character saying which, and no two names
    /// can meet -- Outer_Inner and Outer$Inner, or A$_B and A_$B, each folded to
    /// one name under a plainer scheme, and one helper replaced the other.
    static String baseName(String internal) {
        String simple = internal.substring(internal.lastIndexOf('/') + 1);
        StringBuilder sb = new StringBuilder(simple.length() + 4);
        for (int iter = 0 ; iter < simple.length() ; iter++) {
            char c = simple.charAt(iter);
            if (c == '_') {
                sb.append("__");
            } else if (c == '$') {
                sb.append("_S");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static String qualify(String pkg, String simple) {
        return pkg == null || pkg.length() == 0 ? simple : pkg + "." + simple;
    }
}

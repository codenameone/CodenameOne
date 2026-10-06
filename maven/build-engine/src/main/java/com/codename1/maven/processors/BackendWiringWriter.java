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

import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.MethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

/// Writes `BackendWiring`: the whole dependency injection of a backend, as the
/// code a person would write by hand if they had the patience.
///
/// ```java
/// b_userRepo = new com.example.UserRepo(Backend.requireDataSource(dataSource, ...));
/// b_userService = new com.example.UserService(b_userRepo);
/// b_userService.cn1$inject$mailer(b_mailer);
/// b_userService.init();
/// handlers.add(new com.example.UserControllerRouter(b_userController));
/// ```
///
/// Beans are fields rather than locals only because the server calls back into
/// this object later -- to register websockets, start the scheduled jobs and
/// run the destroy methods -- and those need the same instances. There is no
/// map, no lookup by type or name, and no reflection: every decision was made by
/// [BackendBeans] and is written down here as a constant.
final class BackendWiringWriter {
    static final String CLASS_NAME = "BackendWiring";

    private final BackendBeans model;
    private final BackendSources types;

    BackendWiringWriter(BackendBeans model) {
        this.model = model;
        this.types = new BackendSources(model);
    }

    /// One controller's router, for the handler list.
    static final class Router {
        final String controllerBinary;
        final String routerBinary;

        Router(String controllerBinary, String routerBinary) {
            this.controllerBinary = controllerBinary;
            this.routerBinary = routerBinary;
        }
    }

    /// The source of `BackendWiring` in `pkg`.
    ///
    /// @param routers the controllers, with their generated routers, in order
    /// @param sockets path -> endpoint binary name
    /// @param routes method, path, handler and owning class of every route, for
    /// the listing
    String write(String pkg, List<Router> routers, Map<String, String> sockets,
                 List<String[]> routes) {
        return write(pkg, CLASS_NAME, routers, sockets, routes, false);
    }

    /// The class being written; [#CLASS_NAME] unless a test wiring is.
    private String className = CLASS_NAME;

    /// The same, under another name, and for a test also implementing
    /// `com.codename1.impl.backend.test.TestBeans` so the test's fields can be
    /// filled by name. The production wiring never has that lookup.
    String write(String pkg, String name, List<Router> routers, Map<String, String> sockets,
                 List<String[]> routes, boolean testBeans) {
        className = name;
        StringBuilder sb = new StringBuilder();
        if (pkg.length() > 0) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("// Generated from the beans of this module. Do not edit.\n");
        sb.append("@com.codename1.backend.annotations.Generated\n");
        sb.append("public final class ").append(className)
          .append(" implements com.codename1.impl.backend.BackendApplication")
          .append(testBeans ? ", com.codename1.impl.backend.test.TestBeans" : "").append(" {\n");
        fields(sb);
        create(sb, routers);
        webSockets(sb, sockets);
        started(sb);
        sb.append("    public void stopping() {\n");
        sb.append("        if (scheduler != null) {\n            scheduler.stop(0);\n        }\n");
        sb.append("    }\n\n");
        stopped(sb);
        sb.append("    public boolean tracksCurrentRequest() {\n        return ")
          .append(model.requestSlots + model.sessionSlots > 0).append(";\n    }\n\n");
        requestEnded(sb);
        sb.append("    public com.codename1.impl.backend.Scheduler getScheduler() {\n")
          .append("        return scheduler;\n    }\n\n");
        describeBeans(sb);
        describeRoutes(sb, routes);
        scopes(sb);
        prototypes(sb);
        if (testBeans) {
            testBeans(sb);
        }
        sb.append("}\n");
        return sb.toString();
    }

    /// `bean(name)` over the fields, for a test's injections. A prototype is built
    /// anew, as an injection point would get it.
    private void testBeans(StringBuilder sb) {
        sb.append("    public Object bean(String name) {\n");
        for (BackendBeans.Bean b : model.beans) {
            sb.append("        if (").append(BackendSources.quote(b.name))
              .append(".equals(name)) {\n            return ").append(reference(b)).append(";\n        }\n");
        }
        sb.append("        return null;\n    }\n\n");
    }

    // ----------------------------------------------------------------- fields

    private void fields(StringBuilder sb) {
        sb.append("    private com.codename1.backend.Config config;\n");
        sb.append("    private com.codename1.backend.DataSource dataSource;\n");
        sb.append("    private com.codename1.backend.orm.EntityManager entities;\n");
        sb.append("    private com.codename1.impl.backend.TransactionSession transactionSession;\n");
        sb.append("    private com.codename1.impl.backend.Scheduler scheduler;\n");
        // The controllers and endpoints this start registered, by binary name,
        // for describeRoutes(): one whose condition is off serves nothing, and
        // listing its routes anyway sent an agent to endpoints that answer 404.
        sb.append("    private final java.util.Set active = new java.util.HashSet();\n");
        for (BackendBeans.Bean b : model.beans) {
            if (BackendBeans.PROTOTYPE.equals(b.scope)) {
                continue;
            }
            sb.append("    private ").append(typeOf(b)).append(' ').append(b.var).append(";\n");
            if (b.lazy) {
                sb.append("    private ").append(typeOf(b)).append(' ').append(b.var)
                  .append("Real;\n");
            }
        }
        if (model.requestSlots > 0) {
            scopeField(sb, "requestScope", "requestBean");
        }
        if (model.sessionSlots > 0) {
            scopeField(sb, "sessionScope", "sessionBean");
        }
        if (model.lazySlots > 0) {
            scopeField(sb, "lazyScope", "lazyBean");
        }
        sb.append("\n    public ").append(className).append("() {\n    }\n\n");
    }

    private static void scopeField(StringBuilder sb, String field, String method) {
        sb.append("    private final com.codename1.impl.backend.Wiring.Scope ").append(field)
          .append(" = new com.codename1.impl.backend.Wiring.Scope() {\n")
          .append("        public Object get(int slot) {\n")
          .append("            return ").append(method).append("(slot);\n")
          .append("        }\n    };\n");
    }

    // ----------------------------------------------------------------- create

    private void create(StringBuilder sb, List<Router> routers) {
        sb.append("    public com.codename1.backend.HttpServer.Handler[] create(\n")
          .append("            com.codename1.impl.backend.WiringEnvironment environment) "
                  + "throws Exception {\n");
        // Every field back to null first. A builder started again reuses this
        // object, and a bean whose condition is off this time -- or a lazy one
        // already built -- would otherwise keep the stopped server's destroyed
        // instance and hand it to routes, injections, tools and jobs.
        sb.append("        synchronized (this) {\n");
        sb.append("            scheduler = null;\n            transactionSession = null;\n");
        sb.append("            active.clear();\n");
        for (BackendBeans.Bean b : model.beans) {
            if (BackendBeans.PROTOTYPE.equals(b.scope)) {
                continue;
            }
            sb.append("            ").append(b.var).append(" = null;\n");
            if (b.lazy) {
                sb.append("            ").append(b.var).append("Real = null;\n");
            }
        }
        sb.append("        }\n");
        sb.append("        config = environment.getConfig();\n");
        sb.append("        dataSource = environment.getDataSource();\n");
        sb.append("        entities = environment.getEntityManager();\n");
        if (model.needsSession) {
            sb.append("        transactionSession = new com.codename1.impl.backend."
                    + "TransactionSession(\n                com.codename1.backend.Backend."
                    + "requireEntities(entities, \"the injected Session\"));\n");
        }
        // Stand-ins first: they hold nothing but their scope, and anything built
        // below may be given one.
        for (BackendBeans.Bean b : model.beans) {
            if (b.isProxied()) {
                String scope = b.lazy ? "lazyScope" : BackendBeans.REQUEST.equals(b.scope)
                        ? "requestScope" : "sessionScope";
                sb.append("        ");
                openCondition(sb, b);
                sb.append(b.var).append(" = new ").append(b.proxyBinary).append('(').append(scope)
                  .append(");\n");
                closeCondition(sb, b);
            }
        }
        sb.append("        // Construction, each bean after what its constructor needs.\n");
        for (BackendBeans.Bean b : model.order) {
            if (!b.isEager()) {
                continue;
            }
            sb.append("        ");
            openCondition(sb, b);
            sb.append(b.var).append(" = ").append(construct(b)).append(";\n");
            closeCondition(sb, b);
        }
        sb.append("        // Members, once everything exists, so fields may form cycles.\n");
        for (BackendBeans.Bean b : model.order) {
            if (b.isEager()) {
                members(sb, b, b.var, "        ", true);
            }
        }
        sb.append("        // Initialization, in dependency order.\n");
        // By EVERY injection -- fields and setters too, not just constructors --
        // so a @PostConstruct never calls a dependency whose own has not run.
        // Construction order cannot be used: it only had to follow constructors,
        // and a field dependency found later was initialized after its user.
        List<BackendBeans.Bean> eager = new ArrayList<BackendBeans.Bean>();
        for (BackendBeans.Bean b : model.order) {
            if (b.isEager()) {
                eager.add(b);
            }
        }
        for (BackendBeans.Bean b : dependencyOrder(eager)) {
            initialize(sb, b, b.var, "        ", true);
        }
        for (BackendBeans.Bean b : model.beans) {
            if (BackendBeans.isSecurityChain(b)) {
                // The server asks them in @Order; a chain whose condition is off
                // is null and is not registered.
                sb.append("        environment.registerSecurityFilterChain(").append(b.var)
                  .append(", ").append(b.order).append(");\n");
            }
        }
        for (String name : model.namedBeans) {
            BackendBeans.Bean b = model.byName.get(name);
            if (b != null && b.isEager()) {
                // What an authorization expression calls as @name: reached
                // through the server the calling thread works for, since a
                // process may hold several.
                sb.append("        environment.registerNamedBean(")
                  .append(BackendSources.quote(name)).append(", ").append(b.var).append(");\n");
            }
        }
        for (BackendBeans.Bean b : model.beans) {
            if (b.managed == null && b.tools.isEmpty()) {
                continue;
            }
            // Evaluated once: a prototype's reference is a factory call, and
            // naming it in the null test and again below would build -- and
            // initialize -- a second instance only to drop the first.
            String ref = "exposed" + b.var;
            sb.append("        {\n        ").append(typeOf(b)).append(' ').append(ref)
              .append(" = ").append(reference(b)).append(";\n");
            sb.append("        if (").append(ref).append(" != null) {\n");
            if (b.managed != null) {
                sb.append("            ").append(b.managed.adapterBinary).append(" managed")
                  .append(b.var).append(" = new ").append(b.managed.adapterBinary).append('(')
                  .append(ref).append(");\n");
                sb.append("            environment.registerManaged(managed")
                  .append(b.var).append(");\n");
                sb.append("            managed").append(b.var)
                  .append(".registerGauges(environment);\n");
            }
            for (BackendBeans.Tool t : b.tools) {
                sb.append("            environment.registerTool(new ")
                  .append(t.adapterBinary).append('(').append(ref).append("));\n");
            }
            sb.append("        }\n        }\n");
        }
        sb.append("        java.util.List handlers = new java.util.ArrayList();\n");
        for (Router r : routers) {
            BackendBeans.Bean b = model.beanOfClass(r.controllerBinary);
            String ref = "routed" + b.var;
            sb.append("        {\n        ").append(typeOf(b)).append(' ').append(ref)
              .append(" = ").append(reference(b)).append(";\n");
            sb.append("        if (").append(ref).append(" != null) {\n");
            sb.append("            handlers.add(new ").append(r.routerBinary).append('(').append(ref)
              .append("));\n");
            sb.append("            synchronized (this) {\n                active.add(")
              .append(BackendSources.quote(r.controllerBinary))
              .append(");\n            }\n        }\n        }\n");
        }
        sb.append("        com.codename1.backend.HttpServer.Handler[] out =\n")
          .append("                new com.codename1.backend.HttpServer.Handler[handlers.size()];\n");
        sb.append("        for (int i = 0; i < out.length; i++) {\n")
          .append("            out[i] = (com.codename1.backend.HttpServer.Handler) handlers.get(i);\n")
          .append("        }\n");
        sb.append("        return out;\n    }\n\n");
    }

    /// `if (condition) {` before a conditional bean's statement, nothing otherwise.
    private void openCondition(StringBuilder sb, BackendBeans.Bean b) {
        if (!b.isConditional()) {
            return;
        }
        sb.append("if (").append(condition(b)).append(") {\n            ");
    }

    private void closeCondition(StringBuilder sb, BackendBeans.Bean b) {
        if (b.isConditional()) {
            sb.append("        }\n");
        }
    }

    private String condition(BackendBeans.Bean b) {
        List<String> parts = new ArrayList<String>();
        for (String[] group : b.profiles) {
            StringBuilder p = new StringBuilder("com.codename1.impl.backend.Wiring.profiles(config, "
                    + "new String[] {");
            for (int i = 0; i < group.length; i++) {
                if (i > 0) {
                    p.append(", ");
                }
                p.append(BackendSources.quote(group[i]));
            }
            parts.add(p.append("})").toString());
        }
        for (String[] c : b.propertyConditions) {
            parts.add("com.codename1.impl.backend.Wiring.propertyMatches(config, "
                    + BackendSources.quote(c[0]) + ", " + BackendSources.quote(c[1]) + ", "
                    + c[2] + ")");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(" && ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /// The expression that constructs a bean: `new`, the constructor's bridge, or
    /// the factory method.
    private String construct(BackendBeans.Bean b) {
        if (b.mockType != null) {
            // A test's @MockitoBean: the JVM test runtime asks Mockito for it.
            return "((" + typeOf(b) + ") com.codename1.impl.backend.test.Mocks.create("
                    + typeOf(b) + ".class))";
        }
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < b.constructorPoints.size(); i++) {
            if (i > 0) {
                args.append(", ");
            }
            args.append(point(b.constructorPoints.get(i)));
        }
        if (b.factory != null) {
            String name = BackendBeans.bridged(b.factory)
                    ? BackendWeaver.bridge(b.factory.getName()) : b.factory.getName();
            String target = b.owner != null ? reference(b.owner)
                    : b.factoryOwnerClass.getSourceName();
            // Checked where it is produced: an injection point given the bean
            // directly would take the null without a word, and the server would
            // report ready and fail on first use, far from the factory at fault.
            return "((" + typeOf(b) + ") com.codename1.impl.backend.Wiring.produced(" + target + "."
                    + name + "(" + args + "), "
                    + BackendSources.quote("@Bean " + b.factoryOwnerClass.getSourceName() + "."
                    + b.factory.getName()) + "))";
        }
        if (b.constructor.isPublic()) {
            return "new " + b.cls.getSourceName() + "(" + args + ")";
        }
        return b.cls.getSourceName() + "." + BackendWeaver.NEW_BRIDGE + "(" + args + ")";
    }

    /// Field and setter injection and configuration binding, for one instance.
    private void members(StringBuilder sb, BackendBeans.Bean b, String ref, String indent,
                         boolean guard) {
        boolean any = !b.fields.isEmpty() || !b.setters.isEmpty()
                || !b.propertySetters.isEmpty();
        if (!any) {
            return;
        }
        String inner = indent;
        if (guard) {
            sb.append(indent).append("if (").append(ref).append(" != null) {\n");
            inner = indent + "    ";
        }
        for (Map.Entry<FieldInfo, BackendBeans.Point> f : b.fields.entrySet()) {
            BackendBeans.Point p = f.getValue();
            if (skippable(p)) {
                continue;
            }
            if (maybeAbsent(p)) {
                // Its candidates are all conditional, so whether one is active is
                // known only at start-up: assigned only when one is, or the null
                // overwrote the field's initializer -- which Spring leaves intact.
                sb.append(inner).append("{\n").append(inner).append("    ")
                  .append(sourceType(p)).append(" v = ").append(point(p)).append(";\n")
                  .append(inner).append("    if (v != null) {\n").append(inner)
                  .append("        ").append(ref).append('.')
                  .append(BackendWeaver.injectSetter(f.getKey().getName()))
                  .append("(v);\n").append(inner).append("    }\n")
                  .append(inner).append("}\n");
                continue;
            }
            sb.append(inner).append(ref).append('.')
              .append(BackendWeaver.injectSetter(f.getKey().getName())).append('(')
              .append(point(p)).append(");\n");
        }
        for (BackendBeans.Call c : b.setters) {
            boolean skip = false;
            boolean deferred = false;
            for (BackendBeans.Point p : c.points) {
                skip |= skippable(p);
                deferred |= maybeAbsent(p);
            }
            if (skip) {
                continue;
            }
            if (deferred) {
                // As for a field, and as Spring does for an optional method: the
                // setter is not called unless every optional argument resolved.
                // Each is computed once, in order, so a prototype is built once.
                sb.append(inner).append("{\n");
                StringBuilder test = new StringBuilder();
                for (int i = 0; i < c.points.size(); i++) {
                    BackendBeans.Point p = c.points.get(i);
                    sb.append(inner).append("    ").append(sourceType(p)).append(" a").append(i)
                      .append(" = ").append(point(p)).append(";\n");
                    if (maybeAbsent(p)) {
                        test.append(test.length() == 0 ? "" : " && ").append('a').append(i)
                            .append(" != null");
                    }
                }
                sb.append(inner).append("    if (").append(test).append(") {\n")
                  .append(inner).append("        ").append(ref).append('.')
                  .append(callName(c.method)).append('(');
                for (int i = 0; i < c.points.size(); i++) {
                    sb.append(i > 0 ? ", a" : "a").append(i);
                }
                sb.append(");\n").append(inner).append("    }\n").append(inner).append("}\n");
                continue;
            }
            sb.append(inner).append(ref).append('.').append(callName(c.method)).append('(');
            for (int i = 0; i < c.points.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(point(c.points.get(i)));
            }
            sb.append(");\n");
        }
        for (MethodInfo setter : b.propertySetters) {
            String property = setter.getName().substring(3);
            String camel = BackendBeans.decapitalize(property);
            String key = b.propertiesPrefix.length() == 0 ? camel : b.propertiesPrefix + "." + camel;
            String kebab = kebab(camel);
            String relaxed = kebab.equals(camel) ? "null" : BackendSources.quote(
                    b.propertiesPrefix.length() == 0 ? kebab : b.propertiesPrefix + "." + kebab);
            Type t = Type.getArgumentTypes(setter.getDescriptor())[0];
            sb.append(inner).append("{\n").append(inner).append("    String value = ")
              .append("com.codename1.impl.backend.Wiring.property(config, ")
              .append(BackendSources.quote(key)).append(", ").append(relaxed).append(");\n");
            sb.append(inner).append("    if (value != null) {\n").append(inner).append("        ")
              .append(ref).append('.').append(setter.getName()).append('(')
              .append(convert(t, "value", key)).append(");\n");
            sb.append(inner).append("    }\n").append(inner).append("}\n");
        }
        if (guard) {
            sb.append(indent).append("}\n");
        }
    }

    /// An optional point with nothing to give: the field keeps its initializer and
    /// the setter is not called, as Spring does.
    private static boolean skippable(BackendBeans.Point p) {
        return !p.required && p.candidates.isEmpty() && p.builtin == null && p.value == null
                && !p.list;
    }

    /// An optional point whose value may be absent at start-up: every candidate
    /// it has is conditional (@Profile, @ConditionalOnProperty), so none may be
    /// active in the running configuration.
    private static boolean maybeAbsent(BackendBeans.Point p) {
        if (p.required || p.candidates.isEmpty() || p.builtin != null || p.value != null
                || p.list) {
            return false;
        }
        for (BackendBeans.Bean c : p.candidates) {
            if (!c.isConditional()) {
                return false;
            }
        }
        return true;
    }

    /// The Java source name of a point's declared type.
    private static String sourceType(BackendBeans.Point p) {
        return p.type.getClassName().replace('$', '.');
    }

    /// `@PostConstruct` methods and a factory's init method.
    private void initialize(StringBuilder sb, BackendBeans.Bean b, String ref, String indent,
                            boolean guard) {
        if (b.postConstruct.isEmpty() && b.initMethod == null) {
            return;
        }
        String inner = indent;
        if (guard) {
            sb.append(indent).append("if (").append(ref).append(" != null) {\n");
            inner = indent + "    ";
        }
        for (MethodInfo m : b.postConstruct) {
            sb.append(inner).append(ref).append('.').append(callName(m)).append("();\n");
        }
        if (b.initMethod != null) {
            sb.append(inner).append(ref).append('.').append(b.initMethod).append("();\n");
        }
        if (guard) {
            sb.append(indent).append("}\n");
        }
    }

    /// The expression one injection point receives.
    String point(BackendBeans.Point p) {
        if (p.value != null) {
            return convert(p.type, "com.codename1.impl.backend.Wiring.value(config, "
                    + BackendSources.quote(p.value) + ", " + BackendSources.quote(p.where) + ")",
                    p.where);
        }
        if (p.builtin != null) {
            if ("dataSource".equals(p.builtin)) {
                return "com.codename1.backend.Backend.requireDataSource(dataSource, "
                        + BackendSources.quote(p.where) + ")";
            }
            if ("entities".equals(p.builtin)) {
                return "com.codename1.backend.Backend.requireEntities(entities, "
                        + BackendSources.quote(p.where) + ")";
            }
            if ("session".equals(p.builtin)) {
                return "transactionSession";
            }
            if ("httpSession".equals(p.builtin)) {
                return "request.getSession(true)";
            }
            if ("httpSecurity".equals(p.builtin)) {
                StringBuilder sb = new StringBuilder("com.codename1.impl.backend.security."
                        + "SecuritySupport.http(config, new Object[] {");
                for (int i = 0; i < p.candidates.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(reference(p.candidates.get(i)));
                }
                return sb.append("})").toString();
            }
            return p.builtin;
        }
        String type = types.typeName(p.type);
        if (p.list) {
            StringBuilder sb = new StringBuilder("com.codename1.impl.backend.Wiring.list(new Object[] {");
            for (int i = 0; i < p.candidates.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(reference(p.candidates.get(i)));
            }
            return sb.append("})").toString();
        }
        if (p.candidates.isEmpty()) {
            return "null";
        }
        if (p.choice || (p.candidates.size() == 1 && p.candidates.get(0).isConditional())) {
            StringBuilder sb = new StringBuilder("(").append(type)
                    .append(") com.codename1.impl.backend.Wiring.")
                    .append(p.preferFirst ? "preferred" : "single").append("(new Object[] {");
            for (int i = 0; i < p.candidates.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(reference(p.candidates.get(i)));
            }
            return sb.append("}, ").append(p.required).append(", ")
                    .append(BackendSources.quote(p.where)).append(')').toString();
        }
        return reference(p.candidates.get(0));
    }

    /// How generated code refers to a bean: its field, or a fresh prototype.
    private String reference(BackendBeans.Bean b) {
        if (BackendBeans.PROTOTYPE.equals(b.scope)) {
            return "new" + b.var + "()";
        }
        return b.var;
    }

    /// Converts a configuration string to `t`.
    String convert(Type t, String expression, String where) {
        String w = BackendSources.quote(where);
        String wiring = "com.codename1.impl.backend.Wiring.";
        switch (t.getSort()) {
            case Type.BOOLEAN: return wiring + "toBoolean(" + expression + ", " + w + ")";
            case Type.CHAR: return wiring + "toChar(" + expression + ", " + w + ")";
            case Type.BYTE: return wiring + "toByte(" + expression + ", " + w + ")";
            case Type.SHORT: return wiring + "toShort(" + expression + ", " + w + ")";
            case Type.INT: return wiring + "toInt(" + expression + ", " + w + ")";
            case Type.LONG: return wiring + "toLong(" + expression + ", " + w + ")";
            case Type.FLOAT: return wiring + "toFloat(" + expression + ", " + w + ")";
            case Type.DOUBLE: return wiring + "toDouble(" + expression + ", " + w + ")";
            default:
                break;
        }
        String n = t.getInternalName();
        if ("java/lang/String".equals(n)) {
            return expression;
        }
        if ("java/lang/Integer".equals(n)) {
            return "Integer.valueOf(" + wiring + "toInt(" + expression + ", " + w + "))";
        }
        if ("java/lang/Long".equals(n)) {
            return "Long.valueOf(" + wiring + "toLong(" + expression + ", " + w + "))";
        }
        if ("java/lang/Boolean".equals(n)) {
            return "Boolean.valueOf(" + wiring + "toBoolean(" + expression + ", " + w + "))";
        }
        if ("java/lang/Double".equals(n)) {
            return "Double.valueOf(" + wiring + "toDouble(" + expression + ", " + w + "))";
        }
        if ("java/lang/Float".equals(n)) {
            return "Float.valueOf(" + wiring + "toFloat(" + expression + ", " + w + "))";
        }
        if ("java/lang/Short".equals(n)) {
            return "Short.valueOf(" + wiring + "toShort(" + expression + ", " + w + "))";
        }
        if ("java/lang/Byte".equals(n)) {
            return "Byte.valueOf(" + wiring + "toByte(" + expression + ", " + w + "))";
        }
        if ("java/lang/Character".equals(n)) {
            return "Character.valueOf(" + wiring + "toChar(" + expression + ", " + w + "))";
        }
        String type = types.sourceOf(n);
        return "(" + type + ") " + wiring + "toEnum(" + type + ".values(), " + expression + ", "
                + w + ")";
    }

    private static String kebab(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (i > 0) {
                    sb.append('-');
                }
                sb.append((char) (c + 32));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String callName(MethodInfo m) {
        return BackendBeans.bridged(m) ? BackendWeaver.bridge(m.getName()) : m.getName();
    }

    private String typeOf(BackendBeans.Bean b) {
        return types.sourceOf(b.type);
    }

    // ------------------------------------------------------------- websockets

    private void webSockets(StringBuilder sb, Map<String, String> sockets) {
        sb.append("    public void registerWebSockets(\n")
          .append("            com.codename1.backend.HttpServer.WebSocketRegistry registry) "
                  + "throws Exception {\n");
        for (Map.Entry<String, String> e : sockets.entrySet()) {
            BackendBeans.Bean b = model.beanOfClass(e.getValue());
            String ref = reference(b);
            sb.append("        if (").append(ref).append(" != null) {\n");
            sb.append("            registry.route(").append(BackendSources.quote(e.getKey()))
              .append(", ").append(ref).append(");\n");
            sb.append("            synchronized (this) {\n                active.add(")
              .append(BackendSources.quote(e.getValue())).append(");\n            }\n");
            sb.append("        }\n");
        }
        sb.append("    }\n\n");
    }

    // -------------------------------------------------------------- lifecycle

    private void started(StringBuilder sb) {
        sb.append("    public void started(com.codename1.backend.Backend backend) "
                + "throws Exception {\n");
        if (model.hasJobs()) {
            sb.append("        scheduler = new com.codename1.impl.backend.Scheduler(dataSource);\n");
            // Before it starts: whether its runs are measured, and whose tracer
            // their spans go to, are this server's.
            sb.append("        scheduler.bind(backend);\n");
            int index = 0;
            for (BackendBeans.Bean b : model.beans) {
                if (b.jobs.isEmpty()) {
                    continue;
                }
                String ref = reference(b);
                sb.append("        final ").append(typeOf(b)).append(" jobs").append(index)
                  .append(" = ").append(ref).append(";\n");
                sb.append("        if (jobs").append(index).append(" != null) {\n");
                for (BackendBeans.Job job : b.jobs) {
                    job(sb, job, "jobs" + index);
                }
                sb.append("        }\n");
                index++;
            }
            sb.append("        scheduler.start();\n");
        }
        sb.append("    }\n\n");
    }

    private void job(StringBuilder sb, BackendBeans.Job job, String target) {
        String where = BackendSources.quote("@Scheduled " + job.name);
        String executor = BackendSources.quote(job.executor);
        String thread = "com.codename1.impl.backend.BackendAccess." + job.thread;
        String lock = job.lock.length() == 0 ? "null" : BackendSources.quote(job.lock);
        String body = "new Runnable() {\n"
                + "                public void run() {\n"
                + "                    try {\n"
                + "                        " + target + "." + callName(job.method) + "();\n"
                + "                    } catch (RuntimeException err) {\n"
                + "                        throw err;\n"
                + "                    } catch (Exception err) {\n"
                + "                        throw new RuntimeException(err);\n"
                + "                    }\n"
                + "                }\n"
                + "            }";
        String name = BackendSources.quote(job.name);
        if (job.cron != null) {
            String schedule;
            if (job.masks != null) {
                CronCompiler c = job.masks;
                schedule = "new com.codename1.impl.backend.CronSchedule(" + c.seconds + "L, " + c.minutes
                        + "L, " + c.hours + "L, " + c.daysOfMonth + "L, " + c.months + "L, "
                        + c.daysOfWeek + "L, " + c.lastDayOfMonth + ", "
                        + BackendSources.quote(job.zone) + ", " + BackendSources.quote(job.cron) + ")";
            } else {
                schedule = "com.codename1.impl.backend.CronSchedule.parse("
                        + "com.codename1.impl.backend.Wiring.value(config, "
                        + BackendSources.quote(job.cron) + ", " + where + "), "
                        + BackendSources.quote(job.zone) + ")";
            }
            sb.append("            scheduler.cron(").append(name).append(", ").append(schedule)
              .append(", ").append(executor).append(", ").append(thread).append(", ").append(lock)
              .append(", ").append(job.lockAtMostFor).append("L, ").append(body).append(");\n");
            return;
        }
        boolean rate = job.fixedRate > 0 || job.fixedRateText != null;
        String period = rate ? duration(job.fixedRate, job.fixedRateText, where)
                : duration(job.fixedDelay, job.fixedDelayText, where);
        String initial = duration(job.initialDelay, job.initialDelayText, where);
        sb.append("            scheduler.").append(rate ? "fixedRate" : "fixedDelay").append('(')
          .append(name).append(", ").append(initial).append(", ").append(period).append(", ")
          .append(executor).append(", ").append(thread).append(", ").append(lock).append(", ")
          .append(job.lockAtMostFor).append("L, ").append(body).append(");\n");
    }

    private static String duration(long literal, String text, String where) {
        if (text != null) {
            return "com.codename1.impl.backend.Wiring.toLong(com.codename1.impl.backend.Wiring.value("
                    + "config, " + BackendSources.quote(text) + ", " + where + "), " + where + ")";
        }
        return literal + "L";
    }

    private void stopped(StringBuilder sb) {
        sb.append("    public void stopped() {\n");
        // Eager and lazy singletons in ONE reverse-dependency pass: a lazy bean
        // may use an eager one from its @PreDestroy, and an eager one may use a
        // lazy one through its stand-in -- destroying all the lazy ones last, or
        // first, broke one of the two.
        List<BackendBeans.Bean> singletons = new ArrayList<BackendBeans.Bean>();
        for (BackendBeans.Bean b : model.beans) {
            if (b.isEager() || b.lazy) {
                singletons.add(b);
            }
        }
        List<BackendBeans.Bean> reverse = dependencyOrder(singletons);
        java.util.Collections.reverse(reverse);
        for (BackendBeans.Bean b : reverse) {
            destroy(sb, b, b.lazy ? b.var + "Real" : b.var);
        }
        sb.append("    }\n\n");
    }

    private void destroy(StringBuilder sb, BackendBeans.Bean b, String ref) {
        if (b.preDestroy.isEmpty() && b.destroyMethod == null) {
            return;
        }
        sb.append("        if (").append(ref).append(" != null) {\n");
        for (MethodInfo m : destroyOrder(b)) {
            destroyCall(sb, b, ref + "." + callName(m) + "()");
        }
        if (b.destroyMethod != null) {
            destroyCall(sb, b, ref + "." + b.destroyMethod + "()");
        }
        sb.append("        }\n");
    }

    /// The @PreDestroy methods in the order they run: the reverse of the
    /// @PostConstruct order, so a subclass releases what it holds before the
    /// superclass tears down the state it was built on.
    private static List<MethodInfo> destroyOrder(BackendBeans.Bean b) {
        List<MethodInfo> out = new ArrayList<MethodInfo>(b.preDestroy);
        java.util.Collections.reverse(out);
        return out;
    }

    private static void destroyCall(StringBuilder sb, BackendBeans.Bean b, String call) {
        sb.append("            try {\n                ").append(call).append(";\n")
          .append("            } catch (Throwable err) {\n")
          .append("                com.codename1.impl.backend.Wiring.destroyFailed(")
          .append(BackendSources.quote(b.name)).append(", err);\n            }\n");
    }

    private void requestEnded(StringBuilder sb) {
        scopeEnded(sb, "requestEnded", BackendBeans.REQUEST);
        scopeEnded(sb, "sessionEnded", BackendBeans.SESSION);
    }

    /// The destroy callbacks of one scope's beans, which the server calls with
    /// the array the scope kept them in when the request or session ends.
    private void scopeEnded(StringBuilder sb, String method, String scope) {
        sb.append("    public void ").append(method).append("(Object[] beans) {\n");
        // Dependents first, as for the singletons: a bean's @PreDestroy may still
        // use a same-scope bean it depends on.
        List<BackendBeans.Bean> inScope = new ArrayList<BackendBeans.Bean>();
        for (BackendBeans.Bean b : model.beans) {
            if (scope.equals(b.scope)) {
                inScope.add(b);
            }
        }
        List<BackendBeans.Bean> reverse = dependencyOrder(inScope);
        java.util.Collections.reverse(reverse);
        for (BackendBeans.Bean b : reverse) {
            // A @Bean(destroyMethod = ...) counts as much as @PreDestroy: a
            // request-scoped factory bean with only a destroyMethod is exactly
            // the resource (a connection, a stream) that must be closed per request.
            if (!scope.equals(b.scope)
                    || (b.preDestroy.isEmpty() && b.destroyMethod == null)) {
                continue;
            }
            String type = typeOf(b);
            sb.append("        if (beans.length > ").append(b.slot).append(" && beans[")
              .append(b.slot).append("] instanceof ").append(type).append(") {\n");
            sb.append("            ").append(type).append(" bean = (").append(type)
              .append(") beans[").append(b.slot).append("];\n");
            for (MethodInfo m : destroyOrder(b)) {
                sb.append("    ");
                destroyCall(sb, b, "bean." + callName(m) + "()");
            }
            if (b.destroyMethod != null) {
                sb.append("    ");
                destroyCall(sb, b, "bean." + b.destroyMethod + "()");
            }
            sb.append("        }\n");
        }
        sb.append("    }\n\n");
    }

    // ------------------------------------------------------------- listings

    private void describeBeans(StringBuilder sb) {
        sb.append("    public java.util.List describeBeans() {\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        for (BackendBeans.Bean b : model.beans) {
            sb.append("        out.add(bean(").append(BackendSources.quote(b.name)).append(", ")
              .append(BackendSources.quote(b.type.replace('/', '.'))).append(", ")
              .append(BackendSources.quote(b.lazy ? "singleton (lazy)" : b.scope)).append(", ");
            if (b.isConditional()) {
                sb.append(BackendSources.quote(conditionText(b))).append(", ")
                  .append(BackendBeans.PROTOTYPE.equals(b.scope) ? "true" : reference(b) + " != null");
            } else {
                sb.append("null, true");
            }
            sb.append(", new String[] {");
            List<String> deps = new ArrayList<String>();
            collectDependencies(b, deps);
            for (int i = 0; i < deps.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(BackendSources.quote(deps.get(i)));
            }
            sb.append("}));\n");
        }
        sb.append("        return out;\n    }\n\n");
        sb.append("    private static java.util.Map bean(String name, String type, String scope,\n")
          .append("            String condition, boolean active, String[] dependencies) {\n")
          .append("        java.util.Map m = new java.util.LinkedHashMap();\n")
          .append("        m.put(\"name\", name);\n        m.put(\"type\", type);\n")
          .append("        m.put(\"scope\", scope);\n")
          .append("        if (condition != null) {\n")
          .append("            m.put(\"condition\", condition);\n")
          .append("            m.put(\"active\", Boolean.valueOf(active));\n        }\n")
          .append("        m.put(\"dependencies\", java.util.Arrays.asList(dependencies));\n")
          .append("        return m;\n    }\n\n");
    }

    private static String conditionText(BackendBeans.Bean b) {
        StringBuilder sb = new StringBuilder();
        for (String[] group : b.profiles) {
            if (sb.length() > 0) {
                sb.append(" and ");
            }
            sb.append("profile ").append(java.util.Arrays.toString(group));
        }
        for (String[] c : b.propertyConditions) {
            if (sb.length() > 0) {
                sb.append(" and ");
            }
            sb.append(c[0]).append(c[1].length() > 0 ? "=" + c[1] : " set");
        }
        return sb.toString();
    }

    /// `set` ordered so every bean comes after the beans of the set it is
    /// injected with -- construction order, which reversed is destruction
    /// order. A field or setter cycle, which is legal, is broken where it is met.
    private static List<BackendBeans.Bean> dependencyOrder(List<BackendBeans.Bean> set) {
        List<BackendBeans.Bean> out = new ArrayList<BackendBeans.Bean>();
        java.util.Set<BackendBeans.Bean> seen = new java.util.HashSet<BackendBeans.Bean>();
        for (BackendBeans.Bean b : set) {
            visitDependencies(b, set, seen, out);
        }
        return out;
    }

    private static void visitDependencies(BackendBeans.Bean b, List<BackendBeans.Bean> set,
                                          java.util.Set<BackendBeans.Bean> seen,
                                          List<BackendBeans.Bean> out) {
        if (!seen.add(b)) {
            return;
        }
        List<BackendBeans.Point> points = new ArrayList<BackendBeans.Point>(b.constructorPoints);
        points.addAll(b.fields.values());
        for (BackendBeans.Call c : b.setters) {
            points.addAll(c.points);
        }
        if (b.owner != null && set.contains(b.owner)) {
            visitDependencies(b.owner, set, seen, out);
        }
        for (BackendBeans.Point p : points) {
            for (BackendBeans.Bean d : p.candidates) {
                if (set.contains(d)) {
                    visitDependencies(d, set, seen, out);
                }
            }
        }
        out.add(b);
    }

    private static void collectDependencies(BackendBeans.Bean b, List<String> out) {
        List<BackendBeans.Point> points = new ArrayList<BackendBeans.Point>(b.constructorPoints);
        points.addAll(b.fields.values());
        for (BackendBeans.Call c : b.setters) {
            points.addAll(c.points);
        }
        if (b.owner != null && !out.contains(b.owner.name)) {
            out.add(b.owner.name);
        }
        for (BackendBeans.Point p : points) {
            for (BackendBeans.Bean d : p.candidates) {
                if (!out.contains(d.name)) {
                    out.add(d.name);
                }
            }
            if (p.builtin != null && !out.contains("(" + p.builtin + ")")) {
                out.add("(" + p.builtin + ")");
            }
            if (p.value != null) {
                out.add("@Value " + p.value);
            }
        }
    }

    private static void describeRoutes(StringBuilder sb, List<String[]> routes) {
        sb.append("    public synchronized java.util.List describeRoutes() {\n");
        sb.append("        java.util.List out = new java.util.ArrayList();\n");
        for (String[] r : routes) {
            // Only what this start registered: r[3] is the controller or
            // endpoint class the route belongs to.
            sb.append("        if (active.contains(").append(BackendSources.quote(r[3]))
              .append(")) {\n            out.add(route(").append(BackendSources.quote(r[0]))
              .append(", ").append(BackendSources.quote(r[1])).append(", ")
              .append(BackendSources.quote(r[2])).append("));\n        }\n");
        }
        sb.append("        return out;\n    }\n\n");
        sb.append("    private static java.util.Map route(String method, String path, "
                + "String handler) {\n")
          .append("        java.util.Map m = new java.util.LinkedHashMap();\n")
          .append("        m.put(\"method\", method);\n        m.put(\"path\", path);\n")
          .append("        m.put(\"handler\", handler);\n        return m;\n    }\n\n");
    }

    // ----------------------------------------------------------------- scopes

    private void scopes(StringBuilder sb) {
        if (model.requestSlots > 0) {
            sb.append("    private Object requestBean(int slot) {\n");
            sb.append("        com.codename1.backend.HttpServer.Request request =\n")
              .append("                com.codename1.backend.Backend.currentRequest();\n");
            sb.append("        if (request == null) {\n")
              .append("            throw new IllegalStateException(\"A request-scoped bean was "
                      + "used outside a request\");\n        }\n");
            sb.append("        Object[] beans = com.codename1.impl.backend.BackendAccess.get().requestBeans(request, ").append(model.requestSlots)
              .append(");\n");
            sb.append("        if (beans[slot] == null) {\n            beans[slot] = "
                    + "createScoped(slot, request, true);\n        }\n");
            sb.append("        return beans[slot];\n    }\n\n");
        }
        if (model.sessionSlots > 0) {
            sb.append("    private Object sessionBean(int slot) {\n");
            sb.append("        com.codename1.backend.HttpServer.Request request =\n")
              .append("                com.codename1.backend.Backend.currentRequest();\n");
            sb.append("        if (request == null) {\n")
              .append("            throw new IllegalStateException(\"A session-scoped bean was "
                      + "used outside a request\");\n        }\n");
            sb.append("        com.codename1.backend.HttpSession session = "
                    + "request.getSession(true);\n");
            // The session's shared lock, not the HttpSession: a database store
            // loads a separate copy per request, and locking one copy would let
            // two requests each build the bean.
            sb.append("        synchronized (com.codename1.impl.backend.BackendAccess.get().sessionBeanLock(session)) {\n");
            sb.append("            Object[] beans = com.codename1.impl.backend.BackendAccess.get().sessionBeans(session, ").append(model.sessionSlots)
              .append(");\n");
            sb.append("            if (beans[slot] == null) {\n                beans[slot] = "
                    + "createScoped(slot, request, false);\n            }\n");
            sb.append("            return beans[slot];\n        }\n    }\n\n");
        }
        if (model.lazySlots > 0) {
            sb.append("    private synchronized Object lazyBean(int slot) {\n");
            sb.append("        try {\n            switch (slot) {\n");
            for (BackendBeans.Bean b : model.beans) {
                if (!b.lazy) {
                    continue;
                }
                sb.append("                case ").append(b.slot).append(":\n");
                sb.append("                    if (").append(b.var).append("Real == null) {\n");
                sb.append("                        ").append(typeOf(b)).append(" bean = ")
                  .append(construct(b)).append(";\n");
                members(sb, b, "bean", "                        ", false);
                initialize(sb, b, "bean", "                        ", false);
                sb.append("                        ").append(b.var).append("Real = bean;\n");
                sb.append("                    }\n");
                sb.append("                    return ").append(b.var).append("Real;\n");
            }
            sb.append("                default:\n                    throw new "
                    + "IllegalStateException(\"No lazy bean \" + slot);\n            }\n");
            sb.append("        } catch (RuntimeException err) {\n            throw err;\n");
            sb.append("        } catch (Exception err) {\n")
              .append("            throw new IllegalStateException(\"Building a lazy bean "
                      + "failed: \" + err, err);\n        }\n    }\n\n");
        }
        if (model.requestSlots + model.sessionSlots > 0) {
            sb.append("    private Object createScoped(int slot, "
                    + "com.codename1.backend.HttpServer.Request request, boolean perRequest) {\n");
            sb.append("        try {\n");
            sb.append("            if (perRequest) {\n                switch (slot) {\n");
            scopedCases(sb, BackendBeans.REQUEST);
            sb.append("                    default:\n                        break;\n");
            sb.append("                }\n            } else {\n                switch (slot) {\n");
            scopedCases(sb, BackendBeans.SESSION);
            sb.append("                    default:\n                        break;\n");
            sb.append("                }\n            }\n");
            sb.append("        } catch (RuntimeException err) {\n            throw err;\n");
            sb.append("        } catch (Exception err) {\n")
              .append("            throw new IllegalStateException(\"Building a scoped bean "
                      + "failed: \" + err, err);\n        }\n");
            sb.append("        throw new IllegalStateException(\"No scoped bean \" + slot);\n");
            sb.append("    }\n\n");
        }
    }

    private void scopedCases(StringBuilder sb, String scope) {
        for (BackendBeans.Bean b : model.beans) {
            if (!scope.equals(b.scope)) {
                continue;
            }
            String indent = "                        ";
            sb.append("                    case ").append(b.slot).append(": {\n");
            sb.append(indent).append(typeOf(b)).append(" bean = ").append(construct(b))
              .append(";\n");
            members(sb, b, "bean", indent, false);
            initialize(sb, b, "bean", indent, false);
            sb.append(indent).append("return bean;\n");
            sb.append("                    }\n");
        }
    }

    /// A method per prototype bean, which builds a new one at each injection point.
    private void prototypes(StringBuilder sb) {
        for (BackendBeans.Bean b : model.beans) {
            if (!BackendBeans.PROTOTYPE.equals(b.scope)) {
                continue;
            }
            sb.append("    private ").append(typeOf(b)).append(" new").append(b.var)
              .append("() throws Exception {\n");
            if (b.isConditional()) {
                sb.append("        if (!(").append(condition(b)).append(")) {\n")
                  .append("            return null;\n        }\n");
            }
            sb.append("        ").append(typeOf(b)).append(" bean = ").append(construct(b))
              .append(";\n");
            members(sb, b, "bean", "        ", false);
            initialize(sb, b, "bean", "        ", false);
            sb.append("        return bean;\n    }\n\n");
        }
    }
}

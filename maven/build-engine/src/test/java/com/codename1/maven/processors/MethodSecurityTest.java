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

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.build.SystemStreamLog;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.BackendApplication;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessorContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Method security: the expression compiler form by form, the check the build
/// weaves in front of a method, and that check deciding real requests.
public class MethodSecurityTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String PKG = "package com.example;\n"
            + "import com.codename1.backend.*;\n"
            + "import com.codename1.backend.annotations.*;\n"
            + "import com.codename1.backend.security.*;\n"
            + "import com.codename1.backend.security.core.userdetails.*;\n"
            + "import java.util.*;\n";

    private static final String RT = "com.codename1.impl.backend.security.MethodSecurity";

    private static String config() {
        return PKG + "@Configuration public class SecurityConfig {\n"
                + "    @Bean public SecurityFilterChain chain(HttpSecurity http) {\n"
                + "        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())\n"
                + "            .httpBasic(Customizer.withDefaults())\n"
                + "            .csrf(csrf -> csrf.disable());\n"
                + "        return http.build();\n"
                + "    }\n"
                + "    @Bean public UserDetailsService users() {\n"
                + "        return new InMemoryUserDetailsManager(\n"
                + "            User.withUsername(\"ada\").password(\"{noop}pw\").roles(\"USER\").build(),\n"
                + "            User.withUsername(\"root\").password(\"{noop}pw\").roles(\"ADMIN\").build());\n"
                + "    }\n"
                + "}\n";
    }

    private static Map<String, String> sample() {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.SecurityConfig", config());
        s.put("com.example.Guard", PKG + "@Component(\"guard\") public class Guard {\n"
                + "    public boolean owns(Authentication who, String owner) {\n"
                + "        return who != null && who.getName().equals(owner);\n"
                + "    }\n"
                + "    public boolean atLeast(UserDetails user, long level, String tag, boolean on) {\n"
                + "        return on && level == 3 && \"x\".equals(tag) && user.getUsername().length() > 0;\n"
                + "    }\n"
                + "    public boolean any(Object principal) { return principal != null; }\n"
                + "    public String text() { return \"\"; }\n"
                + "    boolean hidden() { return true; }\n"
                + "    public boolean twice(String a) { return true; }\n"
                + "    public boolean twice(Object a) { return true; }\n"
                + "}\n");
        s.put("com.example.Reports", PKG + "@Component public class Reports {\n"
                + "    @PreAuthorize(\"hasRole('ADMIN')\") public String admin() { return \"admin\"; }\n"
                + "    @PreAuthorize(\"#owner == authentication.name or hasRole('ADMIN')\")\n"
                + "    public String read(@P(\"owner\") String o) { return \"read \" + o; }\n"
                + "    @PreAuthorize(\"@guard.owns(authentication, #owner)\")\n"
                + "    public String owned(@P(\"owner\") String o) { return \"owned \" + o; }\n"
                + "    @PreAuthorize(\"@guard.atLeast(principal, 3, 'x', true)\")\n"
                + "    public String details() { return \"details\"; }\n"
                + "    public String viaSelf() { return secret(); }\n"
                + "    @PreAuthorize(\"hasAuthority('ROLE_ADMIN')\")\n"
                + "    private String secret() { return \"secret\"; }\n"
                + "}\n");
        s.put("com.example.Vault", PKG + "@Component @Secured(\"ROLE_ADMIN\") public class Vault {\n"
                + "    public String open() { return \"open\"; }\n"
                + "    @PermitAll public String peek() { return \"peek\"; }\n"
                + "    @RolesAllowed(\"USER\") public String user() { return \"user\"; }\n"
                + "    @DenyAll public String never() { return \"never\"; }\n"
                + "    @PostConstruct public void init() { }\n"
                + "}\n");
        s.put("com.example.Api", PKG + "@RestController public class Api {\n"
                + "    private final Reports reports; private final Vault vault;\n"
                + "    public Api(Reports reports, Vault vault) { this.reports = reports; this.vault = vault; }\n"
                + "    @GetMapping(\"/admin\") public String admin() { return reports.admin(); }\n"
                + "    @GetMapping(\"/read\") public String read(@RequestParam(\"o\") String o) { return reports.read(o); }\n"
                + "    @GetMapping(\"/owned\") public String owned(@RequestParam(\"o\") String o) { return reports.owned(o); }\n"
                + "    @GetMapping(\"/details\") public String details() { return reports.details(); }\n"
                + "    @GetMapping(\"/self\") public String self() { return reports.viaSelf(); }\n"
                + "    @GetMapping(\"/open\") public String open() { return vault.open(); }\n"
                + "    @GetMapping(\"/peek\") public String peek() { return vault.peek(); }\n"
                + "    @GetMapping(\"/user\") public String user() { return vault.user(); }\n"
                + "    @GetMapping(\"/never\") public String never() { return vault.never(); }\n"
                + "    @PreAuthorize(\"isAuthenticated()\") @GetMapping(\"/direct\")\n"
                + "    public String direct() { return \"direct\"; }\n"
                + "}\n");
        return s;
    }

    // ------------------------------------------------------- the compiler alone

    private BackendBeans model;
    private AnnotatedClass reports;

    private String compile(String expression) throws Exception {
        return compile(expression, "read");
    }

    private String compile(String expression, String method) throws Exception {
        if (model == null) {
            ProcessorContext ctx = process(compile(sample()));
            assertNoErrors(ctx);
            model = BackendBeans.prepare(ctx);
            reports = ctx.lookup("com/example/Reports");
        }
        for (MethodInfo m : reports.getMethods()) {
            if (m.getName().equals(method)) {
                return new MethodSecurityCompiler(model, reports, m).compile(expression).java;
            }
        }
        throw new IllegalStateException(method);
    }

    private void refused(String expression, String... reason) throws Exception {
        try {
            String java = compile(expression);
            fail(expression + " compiled to " + java);
        } catch (MethodSecurityCompiler.Refused expected) {
            for (String part : reason) {
                assertTrue(expression + " -> " + expected.getMessage(),
                        expected.getMessage().contains(part));
            }
        }
    }

    private static String any(String... authorities) {
        return MethodSecurityCompiler.anyAuthority(Arrays.asList(authorities));
    }

    @Test
    public void everySupportedFormCompilesToPlainJava() throws Exception {
        assertEquals(RT + ".hasAnyAuthority(cn1Auth, new String[] {\"ROLE_ADMIN\"})",
                compile("hasRole('ADMIN')"));
        // A role that already has the prefix is left alone, as SpEL leaves it.
        assertEquals(any("ROLE_ADMIN"), compile("hasRole('ROLE_ADMIN')"));
        assertEquals(any("ROLE_A", "ROLE_B"), compile("hasAnyRole('A', \"B\")"));
        assertEquals(any("reports:read"), compile("hasAuthority('reports:read')"));
        assertEquals(any("a", "b", "it's"), compile("hasAnyAuthority('a','b','it''s')"));
        assertEquals(RT + ".isAuthenticated(cn1Auth)", compile("isAuthenticated()"));
        assertEquals(RT + ".isAnonymous(cn1Auth)", compile("isAnonymous()"));
        assertEquals(RT + ".isFullyAuthenticated(cn1Auth)", compile("isFullyAuthenticated()"));
        assertEquals(RT + ".isRememberMe(cn1Auth)", compile(" isRememberMe( ) "));
        assertEquals("true", compile("permitAll"));
        assertEquals("true", compile("permitAll()"));
        assertEquals("false", compile("denyAll"));
        assertEquals(RT + ".same(" + RT + ".name(cn1Auth), \"ada\")",
                compile("authentication.name == 'ada'"));
        assertEquals("!" + RT + ".same(a0, " + RT + ".username(cn1Auth))",
                compile("#owner != principal.username"));
        assertEquals(RT + ".same(" + RT + ".username(cn1Auth), a0)",
                compile("authentication.principal.username == #owner"));
    }

    @Test
    public void notBindsTighterThanAndWhichBindsTighterThanOr() throws Exception {
        String a = any("ROLE_A");
        String b = any("ROLE_B");
        String c = any("ROLE_C");
        assertEquals("(" + a + " || (" + b + " && " + c + "))",
                compile("hasRole('A') or hasRole('B') and hasRole('C')"));
        assertEquals("((" + a + " && " + b + ") || " + c + ")",
                compile("hasRole('A') && hasRole('B') || hasRole('C')"));
        assertEquals("((" + a + " || " + b + ") && " + c + ")",
                compile("(hasRole('A') or hasRole('B')) and hasRole('C')"));
        assertEquals("(!" + a + " && " + b + ")", compile("not hasRole('A') and hasRole('B')"));
        assertEquals("!(" + a + " && " + b + ")", compile("!(hasRole('A') and hasRole('B'))"));
        assertEquals("!!" + a, compile("! not hasRole('A')"));
        // A comparison is one condition, so not covers all of it.
        assertEquals("!" + RT + ".same(a0, \"x\")", compile("not #owner == 'x'"));
    }

    @Test
    public void aBeanCallIsADirectCallCheckedAgainstTheBean() throws Exception {
        assertEquals("cn1Bean_guard().owns(cn1Auth, (java.lang.String) a0)",
                compile("@guard.owns(authentication, #owner)"));
        // A principal that is not what the method takes is refused by an
        // instanceof, never handed over through a cast.
        assertEquals("(" + RT + ".principal(cn1Auth) instanceof "
                + "com.codename1.backend.security.core.userdetails.UserDetails && "
                + "cn1Bean_guard().atLeast((com.codename1.backend.security.core.userdetails."
                + "UserDetails) " + RT + ".principal(cn1Auth), 3L, \"x\", true))",
                compile("@guard.atLeast(principal, 3, 'x', true)"));
        assertEquals("cn1Bean_guard().any(" + RT + ".principal(cn1Auth))",
                compile("@guard.any(principal)"));
        assertEquals("cn1Bean_guard().any((java.lang.Object) a0)", compile("@guard.any(#owner)"));
        assertEquals("(" + any("ROLE_A") + " || cn1Bean_guard().any(cn1Auth))",
                compile("hasRole('A') or @guard.any(authentication)"));
    }

    @Test
    public void whatIsNotSupportedIsRefusedWithItsReason() throws Exception {
        refused("hasPermission(#owner, 'read')", "hasPermission(...) is not supported",
                "@permissions.canRead(authentication, #id)");
        refused("returnObject == 'x'", "returnObject is not supported", "before the method runs");
        refused("T(java.lang.Math).abs(1) == 1", "T(...) is not supported");
        refused("#owner.length == 3", "#owner.length is a property chain");
        refused("authentication.details == 'x'", "authentication.details cannot be read");
        refused("principal.id == 'x'", "principal.id cannot be read");
        refused("authentication.name.length == 'x'", "property chains are not supported");
        refused("hasRole('A') xor hasRole('B')", "'xor' at position 13 is not expected after "
                + "a complete expression");
        refused("hasRole('A') and", "the expression ends");
        refused("hasRole(ADMIN)", "needs a quoted role, as in hasRole('ADMIN')");
        refused("hasRole('A', 'B')", "hasRole takes one role; use hasAnyRole");
        refused("isAuthenticated", "the expression ends where '(' starts the arguments");
        refused("(hasRole('A')", "where ')' closes the parenthesis");
        refused("hasRole('A", "the string starting at position 8 is never closed");
        refused("#owner > 'a'", "'>' at position 7 is not part of the expressions");
        refused("#owner", "can only be compared with == or !=");
        refused("#nobody == 'x'", "#nobody names no parameter of read; its parameters are [owner]");
        refused("", "the expression is empty");
        refused("1 == 1", "'1' at position 0 is not expected where a condition should be");
    }

    @Test
    public void aBeanCallThatCannotBeMadeIsRefused() throws Exception {
        refused("@nobody.owns(authentication)", "@nobody names no bean of this application");
        refused("@guard.missing(authentication)", "com.example.Guard has no method named missing");
        refused("@guard.owns(authentication)", "does not fit com.example.Guard.owns",
                "takes 2 argument(s) and the expression passes 1");
        refused("@guard.owns(#owner, #owner)", "argument 1 is #owner, a java.lang.String, and the "
                + "method takes a com.codename1.backend.security.Authentication");
        refused("@guard.text()", "returns java.lang.String; a method an expression calls "
                + "returns boolean");
        refused("@guard.twice(#owner)", "fits 2 overloads");
        refused("@guard.any(authentication.name)", "authentication.name cannot be passed to "
                + "@guard.any: pass authentication itself");
        refused("@guard.any(1 + 2)", "'+' at position 13 is not part of the expressions");
        refused("@guard.atLeast(principal, 'three', 'x', true)", "argument 2 is a string");
        refused("@guard.owns(authentication, #owner).x", "cannot be read further");
        refused("@api.direct()", "returns java.lang.String");
    }

    @Test
    public void aParameterIsNamedByTheClassFileOrByP() throws Exception {
        // This test class is compiled with debug information; the sources the
        // tests compile are not, which is what @P is for.
        Map<String, AnnotatedClass> index = ClassScanner.scan(new File(
                MethodSecurityTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        AnnotatedClass self = index.get("com/codename1/maven/processors/MethodSecurityTest");
        for (MethodInfo m : self.getMethods()) {
            if (m.getName().equals("named")) {
                assertEquals("{first=0, wide=1, last=2}",
                        MethodSecurityCompiler.parameterNames(self, m).toString());
            }
        }
        try {
            compile("#o == 'x'", "read");
            fail("a name only the source has was found");
        } catch (MethodSecurityCompiler.Refused expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("#o names no "
                    + "parameter of read"));
        }
        try {
            compile("#x == 'x'", "admin");
            fail();
        } catch (MethodSecurityCompiler.Refused expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(
                    "Annotate the parameter with @P(\"x\"), or compile with -parameters"));
        }
    }

    static String named(String first, long wide, Object last) {
        String local = first + wide + last;
        return local;
    }

    @Test
    public void beanNamesAreFoundWithoutCompiling() {
        assertEquals("[guard, other_1]", MethodSecurityCompiler.beanNames(
                "@guard.x(#a) or '@not.a.bean' == #b and @other_1.y()").toString());
    }

    // ----------------------------------------------------- what the build emits

    @Test
    public void theCheckIsTheOutermostLayerOfTheGeneratedAspect() throws Exception {
        Map<String, String> s = sample();
        s.put("com.example.Later", PKG + "@Component public class Later {\n"
                + "    @Async @Transactional @PreAuthorize(\"hasRole('ADMIN')\")\n"
                + "    public void run() { }\n"
                + "}\n");
        ProcessorContext ctx = process(compile(s));
        assertNoErrors(ctx);
        Map<String, String> sources = BackendBeans.prepare(ctx).sources;
        String later = sources.get("com.example.LaterCn1Aspects");
        // The check, then the hand-off, then the transaction on the task's thread.
        int check = later.indexOf("static void run(com.example.Later self) throws Throwable {\n"
                + "        com.codename1.backend.security.Authentication cn1Auth = " + RT
                + ".authentication();\n"
                + "        if (!(" + any("ROLE_ADMIN") + ")) {\n"
                + "            throw " + RT + ".denied(cn1Auth, \"com.example.Later.run\");\n"
                + "        }\n"
                + "        LaterCn1Aspects.run$cn1async(self);\n");
        assertTrue(later, check > 0);
        assertTrue(later, later.contains("static void run$cn1async(com.example.Later self)"));
        assertTrue(later, later.contains("static void run$cn1tx(com.example.Later self)"));
        String reportsAspects = sources.get("com.example.ReportsCn1Aspects");
        assertTrue(reportsAspects, reportsAspects.contains(
                "    private static com.example.Guard cn1Bean_guard() {\n"
                + "        Object bean = " + RT + ".bean(\"guard\");\n"
                + "        if (bean instanceof com.example.Guard) {\n"
                + "            return (com.example.Guard) bean;\n"
                + "        }\n"
                + "        throw " + RT + ".noBean(\"guard\", \"com.example.Guard\", bean);\n"));
        // One accessor however many methods call the bean.
        assertEquals(reportsAspects, 2, reportsAspects.split("cn1Bean_guard\\(\\) \\{", -1).length);
        // A class default reaches the public methods it declares; @PermitAll
        // takes one out, and a lifecycle method was never in.
        String vault = sources.get("com.example.VaultCn1Aspects");
        assertTrue(vault, vault.contains("static java.lang.String open("));
        assertTrue(vault, vault.contains("if (!(false)) {"));
        assertTrue(vault, vault.contains(any("ROLE_USER")));
        assertFalse(vault, vault.contains(" peek("));
        assertFalse(vault, vault.contains(" init("));
    }

    @Test
    public void methodSecurityDecidesRealRequests() throws Exception {
        File classes = compile(sample());
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        String wiring = proc.generateWiring("com.example");
        assertTrue(wiring, wiring.contains("environment.registerNamedBean(\"guard\", b_guard);"));
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                getClass().getClassLoader());
        int port = freePort();
        // A server started and stopped first, with beans of its own: the bean a
        // call reaches is the one of the server serving, not whichever a static
        // was last given.
        Backend earlier = start(loader, port);
        try {
            assertEquals("owned ada", call(port, "/owned?o=ada", "ada"));
        } finally {
            earlier.stop();
        }
        port = freePort();
        Backend first = start(loader, port);
        try {
            // Nobody signed in: told to, which over HTTP Basic is a 401.
            assertEquals("HTTP 401: Unauthorized", call(port, "/admin", null));
            assertEquals("HTTP 401: Unauthorized", call(port, "/direct", null));
            // Signed in and refused: 403.
            assertTrue(call(port, "/admin", "ada"), call(port, "/admin", "ada").startsWith("HTTP 403"));
            assertEquals("admin", call(port, "/admin", "root"));
            assertEquals("direct", call(port, "/direct", "ada"));
            // #param against authentication.name, or a role.
            assertEquals("read ada", call(port, "/read?o=ada", "ada"));
            assertTrue(call(port, "/read?o=bob", "ada").startsWith("HTTP 403"));
            assertEquals("read bob", call(port, "/read?o=bob", "root"));
            // A bean decides, on either server.
            assertEquals("owned ada", call(port, "/owned?o=ada", "ada"));
            assertTrue(call(port, "/owned?o=ada", "root").startsWith("HTTP 403"));
            assertEquals("HTTP 401: Unauthorized", call(port, "/owned?o=ada", null));
            assertEquals("details", call(port, "/details", "ada"));
            // The anonymous principal is text, not a UserDetails: refused by
            // the instanceof, and as anonymous that is a 401.
            assertEquals("HTTP 401: Unauthorized", call(port, "/details", null));
            // A private method reached through this is checked too.
            assertTrue(call(port, "/self", "ada").startsWith("HTTP 403"));
            assertEquals("secret", call(port, "/self", "root"));
            // The class's @Secured, and the methods that replace it.
            assertTrue(call(port, "/open", "ada").startsWith("HTTP 403"));
            assertEquals("open", call(port, "/open", "root"));
            assertEquals("peek", call(port, "/peek", null));
            assertEquals("user", call(port, "/user", "ada"));
            assertTrue(call(port, "/user", "root").startsWith("HTTP 403"));
            assertTrue(call(port, "/never", "root").startsWith("HTTP 403"));
            assertEquals("HTTP 401: Unauthorized", call(port, "/never", null));
        } finally {
            first.stop();
        }
    }

    // -------------------------------------------------------------- build errors

    private String errors(Map<String, String> sources) throws Exception {
        return String.valueOf(process(compile(sources)).getErrors());
    }

    @Test
    public void methodSecurityWithoutAChainIsABuildError() throws Exception {
        Map<String, String> s = new LinkedHashMap<String, String>();
        s.put("com.example.Open", PKG + "@Component public class Open {\n"
                + "    @RolesAllowed(\"ADMIN\") public void run() { }\n"
                + "}\n");
        String errors = errors(s);
        assertTrue(errors, errors.contains("com.example.Open uses method security, and this "
                + "module has no SecurityFilterChain bean"));
    }

    @Test
    public void whatTheBuildCannotApplyIsABuildError() throws Exception {
        Map<String, String> s = sample();
        s.put("com.example.PostAuthorize", "package com.example;\n"
                + "public @interface PostAuthorize { String value(); }\n");
        s.put("com.example.PreFilter", "package com.example;\n"
                + "public @interface PreFilter { String value(); }\n");
        s.put("com.example.Late", PKG + "@Component public class Late {\n"
                + "    @PostAuthorize(\"returnObject == 'x'\") public String a() { return \"\"; }\n"
                + "    @PreFilter(\"filterObject == 'x'\") public void b(List<String> l) { }\n"
                + "    @PreAuthorize(\"hasPermission(#x, 'read')\") public void c(@P(\"x\") String x) { }\n"
                + "    @PreAuthorize(\"permitAll\") @DenyAll public void d() { }\n"
                + "    @PreAuthorize(\"isAuthenticated()\") @PostConstruct public void e() { }\n"
                + "    @Secured({}) public void f() { }\n"
                + "}\n");
        s.put("com.example.Shape", PKG + "public abstract class Shape {\n"
                + "    @PreAuthorize(\"isAuthenticated()\") public abstract void draw();\n"
                + "}\n");
        s.put("com.example.Contract", PKG + "public interface Contract {\n"
                + "    @PreAuthorize(\"isAuthenticated()\") void sign();\n"
                + "    @DenyAll default void tear() { }\n"
                + "}\n");
        s.put("com.example.Closed", PKG + "@RolesAllowed(\"ADMIN\") public interface Closed { }\n");
        String errors = errors(s);
        assertTrue(errors, errors.contains("@PostAuthorize on com.example.Late.a is not "
                + "supported: the check is compiled in before the method runs"));
        assertTrue(errors, errors.contains("@PreFilter on com.example.Late.b is not supported: "
                + "the build does not rewrite a method's collections"));
        assertTrue(errors, errors.contains("@PreAuthorize(\"hasPermission(#x, 'read')\") on "
                + "com.example.Late.c: hasPermission(...) is not supported"));
        assertTrue(errors, errors.contains("carries both @PreAuthorize and @DenyAll"));
        assertTrue(errors, errors.contains("com.example.Late.e is called by the server while "
                + "it builds and destroys its beans"));
        assertTrue(errors, errors.contains("@Secured on com.example.Late.f names nothing"));
        assertTrue(errors, errors.contains("com.example.Shape.draw is abstract, so there is no "
                + "body to wrap"));
        assertTrue(errors, errors.contains("@PreAuthorize on com.example.Contract.sign is not "
                + "applied"));
        assertTrue(errors, errors.contains("@DenyAll on com.example.Contract.tear is not applied"));
        assertTrue(errors, errors.contains("@RolesAllowed on interface com.example.Closed is "
                + "not applied"));
    }

    @Test
    public void aBeanAnExpressionCallsMustBeASingleton() throws Exception {
        Map<String, String> s = sample();
        s.put("com.example.PerRequest", PKG + "@Component(\"perRequest\") @Scope(\"request\")\n"
                + "public class PerRequest { public boolean ok() { return true; } }\n");
        s.put("com.example.Uses", PKG + "@Component public class Uses {\n"
                + "    @PreAuthorize(\"@perRequest.ok()\") public void run() { }\n"
                + "    @PreAuthorize(\"@guard.hidden()\") public void peek() { }\n"
                + "}\n");
        s.put("other.Far", "package other;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@Component public class Far {\n"
                + "    @PreAuthorize(\"@guard.hidden()\") public void run() { }\n"
                + "}\n");
        String errors = errors(s);
        assertTrue(errors, errors.contains("@perRequest is scoped request; an expression can "
                + "call a singleton bean only"));
        assertTrue(errors, errors.contains("@guard.hidden is not accessible from other.Far: "
                + "the method is not public"));
        // Package-private is enough from the bean's own package.
        assertFalse(errors, errors.contains("not accessible from com.example.Uses"));
    }

    @Test
    public void theSecuritySchemaIsRegisteredOnlyWhenTheBuildAsks() throws Exception {
        String register = "com.codename1.backend.Migrations.register("
                + "com.codename1.backend.security.SecuritySchema.migrations());";
        File classes = compile(sample());
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        String bootstrap = proc.generateBootstrap("com.example");
        // Not asked for: the entry point does not name the schema, so the
        // server does not carry it.
        assertFalse(bootstrap, bootstrap.contains("SecuritySchema"));
        assertFalse(bootstrap, bootstrap.contains(".requiresDataSource()"));

        classes = compile(sample());
        java.io.FileWriter w = new java.io.FileWriter(new File(classes, "application.properties"));
        w.write("cn1.security.schema.enabled=true\n");
        w.close();
        proc = new RestControllerAnnotationProcessor();
        proc.setDevTools(false);
        assertNoErrors(process(classes, proc));
        bootstrap = proc.generateBootstrap("com.example");
        assertTrue(bootstrap, bootstrap.contains("        " + register + "\n"));
        // Its tables need a database, whether or not a bean asked for one.
        assertTrue(bootstrap, bootstrap.contains(".requiresDataSource()"));
        // Before the server is built and started.
        assertTrue(bootstrap, bootstrap.indexOf(register) < bootstrap.indexOf("cn1Builder.run()"));
    }

    // ------------------------------------------------------------------ helpers

    private Backend start(URLClassLoader loader, int port) throws Exception {
        BackendApplication app = (BackendApplication) loader
                .loadClass("com.example.BackendWiring").newInstance();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend.Builder builder = Backend.builder(Config.of(settings, "dev")).quiet();
        BackendAccess.get().application(builder, app);
        BackendAccess.get().security(builder);
        return builder.start();
    }

    private static String call(int port, String path, String user) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path)
                .openConnection();
        if (user != null) {
            c.setRequestProperty("Authorization", "Basic " + com.codename1.backend.Base64.encode(
                    (user + ":pw").getBytes("UTF-8")));
        }
        int status = c.getResponseCode();
        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (in != null) {
            byte[] chunk = new byte[1024];
            for (int n = in.read(chunk); n >= 0; n = in.read(chunk)) {
                buffer.write(chunk, 0, n);
            }
            in.close();
        }
        String body = new String(buffer.toByteArray(), "UTF-8");
        return status >= 400 ? "HTTP " + status + ": " + body : body;
    }

    private File compile(Map<String, String> sources) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, backendClasspath());
        return classes;
    }

    private ProcessorContext process(File classes) throws Exception {
        return process(classes, new RestControllerAnnotationProcessor());
    }

    private ProcessorContext process(File classes, RestControllerAnnotationProcessor proc)
            throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        List<String> cp = new ArrayList<String>();
        for (File f : backendClasspath()) {
            cp.add(f.getAbsolutePath());
        }
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog(), tmp.newFolder(), new Properties(), null,
                Collections.<String>emptyList(), "UTF-8", cp);
        BackendBeanAnnotationProcessor beans = new BackendBeanAnnotationProcessor();
        beans.start(ctx);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) {
                proc.processClass(cls, ctx);
            }
        }
        beans.finish(ctx);
        proc.finish(ctx);
        return ctx;
    }

    private static void assertNoErrors(ProcessorContext ctx) {
        if (ctx.hasErrors()) {
            fail("the processors reported errors: " + ctx.getErrors());
        }
    }

    private static List<File> backendClasspath() throws Exception {
        URL url = HttpServer.class.getProtectionDomain().getCodeSource().getLocation();
        return Arrays.asList(new File(url.toURI()));
    }

    private static int freePort() throws Exception {
        ServerSocket s = new ServerSocket(0);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }
}

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

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.MethodInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// Compiles a `@PreAuthorize` expression into a Java boolean expression.
///
/// The backend has no expression language at run time: what Spring Security
/// interprets on every call is turned here, once, into the source of the check
/// the woven method runs -- calls on
/// `com.codename1.impl.backend.security.MethodSecurity`, `instanceof` tests and
/// direct calls on beans -- and javac compiles that. A mistake in an expression
/// is therefore a build error with the place it was found, not a failure on the
/// first request that reaches the method.
///
/// The grammar is the part of SpEL that authorization rules are written in:
///
/// ```
/// or      := and (('or' | '||') and)*
/// and     := not (('and' | '&&') not)*
/// not     := ('not' | '!') not | primary
/// primary := '(' or ')' | function | '@' bean '.' method '(' arguments ')'
///          | operand ('==' | '!=') operand
/// operand := 'authentication.name' | 'principal.username' | string | '#' parameter
/// ```
///
/// The generated expression reads the caller from a local named `cn1Auth` and
/// the method's arguments from `a0`, `a1`, ... -- the names
/// [BackendSources] gives them.
final class MethodSecurityCompiler {
    static final String RUNTIME = "com.codename1.impl.backend.security.MethodSecurity";
    static final String AUTH = "cn1Auth";
    private static final String AUTHENTICATION_TYPE = "com/codename1/backend/security/Authentication";

    /// An expression the build cannot compile, with why.
    static final class Refused extends Exception {
        private static final long serialVersionUID = 1L;

        Refused(String message) {
            super(message);
        }
    }

    /// What a compiled expression needs beside itself.
    static final class Result {
        /// The Java expression.
        String java;
        /// Bean name -> the Java source name of the type its calls are made on.
        final Map<String, String> beans = new LinkedHashMap<String, String>();
    }

    private final BackendBeans beans;
    private final BackendSources sources;
    private final AnnotatedClass caller;
    private final MethodInfo method;
    private final Type[] argumentTypes;
    private Map<String, Integer> parameters;
    private String source;
    private final List<String[]> tokens = new ArrayList<String[]>();
    private int at;
    private final Result result = new Result();

    MethodSecurityCompiler(BackendBeans beans, AnnotatedClass caller, MethodInfo method) {
        this.beans = beans;
        this.sources = new BackendSources(beans);
        this.caller = caller;
        this.method = method;
        this.argumentTypes = Type.getArgumentTypes(method.getDescriptor());
    }

    /// The name of the accessor [BackendSources] generates for a bean.
    static String accessor(String beanName) {
        return "cn1Bean_" + BackendBeans.identifier(beanName);
    }

    /// `hasAnyAuthority` over literal authorities, for `@Secured` and
    /// `@RolesAllowed`.
    static String anyAuthority(List<String> authorities) {
        StringBuilder sb = new StringBuilder(RUNTIME).append(".hasAnyAuthority(").append(AUTH)
                .append(", new String[] {");
        for (int i = 0; i < authorities.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(BackendSources.quote(authorities.get(i)));
        }
        return sb.append("})").toString();
    }

    /// The names of the beans `expression` calls, without judging the rest of
    /// it: what a test's wiring must register for the checks the main build
    /// already compiled.
    static Set<String> beanNames(String expression) {
        Set<String> out = new LinkedHashSet<String>();
        if (expression == null) {
            return out;
        }
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '\'' || c == '"') {
                int end = i + 1;
                while (end < expression.length() && expression.charAt(end) != c) {
                    end++;
                }
                i = end;
            } else if (c == '@') {
                int end = i + 1;
                while (end < expression.length() && isIdentifierPart(expression.charAt(end))) {
                    end++;
                }
                if (end > i + 1) {
                    out.add(expression.substring(i + 1, end));
                }
                i = end - 1;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- compile

    Result compile(String expression) throws Refused {
        if (expression == null || expression.trim().length() == 0) {
            throw new Refused("the expression is empty; write permitAll to let everyone in");
        }
        source = expression;
        tokenize();
        at = 0;
        result.java = or();
        if (at < tokens.size()) {
            throw unexpected("after a complete expression");
        }
        return result;
    }

    private static boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9');
    }

    /// Tokens are {kind, text, position}: "id", "str", "num" or the symbol itself.
    private void tokenize() throws Refused {
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                i++;
                continue;
            }
            String position = String.valueOf(i);
            if (isIdentifierStart(c)) {
                int end = i + 1;
                while (end < n && isIdentifierPart(source.charAt(end))) {
                    end++;
                }
                tokens.add(new String[] {"id", source.substring(i, end), position});
                i = end;
            } else if (c >= '0' && c <= '9') {
                int end = i + 1;
                while (end < n && source.charAt(end) >= '0' && source.charAt(end) <= '9') {
                    end++;
                }
                tokens.add(new String[] {"num", source.substring(i, end), position});
                i = end;
            } else if (c == '\'' || c == '"') {
                StringBuilder text = new StringBuilder();
                int end = i + 1;
                boolean closed = false;
                while (end < n) {
                    char d = source.charAt(end);
                    if (d == c) {
                        // SpEL writes the quote itself by doubling it.
                        if (end + 1 < n && source.charAt(end + 1) == c) {
                            text.append(c);
                            end += 2;
                            continue;
                        }
                        closed = true;
                        end++;
                        break;
                    }
                    text.append(d);
                    end++;
                }
                if (!closed) {
                    throw new Refused("the string starting at position " + i + " is never closed");
                }
                tokens.add(new String[] {"str", text.toString(), position});
                i = end;
            } else if (i + 1 < n && (source.startsWith("&&", i) || source.startsWith("||", i)
                    || source.startsWith("==", i) || source.startsWith("!=", i))) {
                tokens.add(new String[] {source.substring(i, i + 2), source.substring(i, i + 2),
                    position});
                i += 2;
            } else if ("()!,.#@".indexOf(c) >= 0) {
                tokens.add(new String[] {String.valueOf(c), String.valueOf(c), position});
                i++;
            } else {
                throw new Refused("'" + c + "' at position " + i + " is not part of the "
                        + "expressions the build compiles; see @PreAuthorize for what is");
            }
        }
    }

    private String kind() {
        return at < tokens.size() ? tokens.get(at)[0] : "end";
    }

    private String text() {
        return at < tokens.size() ? tokens.get(at)[1] : "";
    }

    private boolean isWord(String word) {
        return "id".equals(kind()) && word.equals(text());
    }

    private boolean take(String symbol) {
        if (symbol.equals(kind())) {
            at++;
            return true;
        }
        return false;
    }

    private void expect(String symbol, String why) throws Refused {
        if (!take(symbol)) {
            throw unexpected("where '" + symbol + "' " + why);
        }
    }

    private Refused unexpected(String where) {
        if (at >= tokens.size()) {
            return new Refused("the expression ends " + where);
        }
        return new Refused("'" + text() + "' at position " + tokens.get(at)[2] + " is not "
                + "expected " + where);
    }

    private String or() throws Refused {
        String left = and();
        while (take("||") || (isWord("or") && advance())) {
            left = "(" + left + " || " + and() + ")";
        }
        return left;
    }

    private String and() throws Refused {
        String left = not();
        while (take("&&") || (isWord("and") && advance())) {
            left = "(" + left + " && " + not() + ")";
        }
        return left;
    }

    private boolean advance() {
        at++;
        return true;
    }

    private String not() throws Refused {
        if (take("!") || (isWord("not") && advance())) {
            return "!" + not();
        }
        return primary();
    }

    private String primary() throws Refused {
        if (take("(")) {
            String inner = or();
            expect(")", "closes the parenthesis");
            return inner;
        }
        if (take("@")) {
            return beanCall();
        }
        if ("id".equals(kind())) {
            String word = text();
            if ("permitAll".equals(word) || "denyAll".equals(word)) {
                at++;
                if (take("(")) {
                    expect(")", "ends " + word + "()");
                }
                return "permitAll".equals(word) ? "true" : "false";
            }
            if ("true".equals(word) || "false".equals(word)) {
                at++;
                return word;
            }
            if ("hasRole".equals(word) || "hasAnyRole".equals(word)
                    || "hasAuthority".equals(word) || "hasAnyAuthority".equals(word)) {
                at++;
                return authorities(word);
            }
            if ("isAuthenticated".equals(word) || "isAnonymous".equals(word)
                    || "isFullyAuthenticated".equals(word) || "isRememberMe".equals(word)) {
                at++;
                expect("(", "starts the arguments of " + word);
                expect(")", "ends " + word + "(), which takes no arguments");
                return RUNTIME + "." + word + "(" + AUTH + ")";
            }
            if ("hasPermission".equals(word)) {
                throw new Refused("hasPermission(...) is not supported: there is no "
                        + "PermissionEvaluator. Write the decision as a bean method and call "
                        + "it: @permissions.canRead(authentication, #id)");
            }
            if ("returnObject".equals(word) || "filterObject".equals(word)) {
                throw new Refused(word + " is not supported: an expression is checked before "
                        + "the method runs and never sees what it returns or filters. Check "
                        + "the result in the method, or in a bean it calls");
            }
            if ("T".equals(word) && at + 1 < tokens.size() && "(".equals(tokens.get(at + 1)[0])) {
                throw new Refused("T(...) is not supported: an expression cannot name a "
                        + "class. Put the call in a bean method and write @bean.method(...)");
            }
        }
        return comparison();
    }

    private String authorities(String function) throws Refused {
        boolean role = function.endsWith("Role");
        boolean any = function.startsWith("hasAny");
        expect("(", "starts the arguments of " + function);
        List<String> names = new ArrayList<String>();
        do {
            if (!"str".equals(kind())) {
                throw unexpected("where " + function + " needs a quoted "
                        + (role ? "role" : "authority") + ", as in " + function + "('"
                        + (role ? "ADMIN" : "reports:read") + "')");
            }
            String name = text();
            at++;
            if (name.length() == 0) {
                throw new Refused(function + " was given an empty "
                        + (role ? "role" : "authority"));
            }
            // SpEL's hasRole leaves a role that already has the prefix alone.
            names.add(role && !name.startsWith("ROLE_") ? "ROLE_" + name : name);
        } while (take(","));
        expect(")", "ends the arguments of " + function);
        if (!any && names.size() != 1) {
            throw new Refused(function + " takes one " + (role ? "role" : "authority")
                    + "; use hasAny" + (role ? "Role" : "Authority") + " for several");
        }
        return anyAuthority(names);
    }

    private String comparison() throws Refused {
        String left = operand();
        boolean equal;
        if (take("==")) {
            equal = true;
        } else if (take("!=")) {
            equal = false;
        } else {
            throw unexpected("after a value, which can only be compared with == or !=");
        }
        String right = operand();
        return (equal ? "" : "!") + RUNTIME + ".same(" + left + ", " + right + ")";
    }

    private String operand() throws Refused {
        if ("str".equals(kind())) {
            String value = text();
            at++;
            return BackendSources.quote(value);
        }
        if (take("#")) {
            int index = parameter();
            if (!"java/lang/String".equals(internalName(argumentTypes[index]))) {
                throw new Refused("#" + tokens.get(at - 1)[1] + " is a "
                        + argumentTypes[index].getClassName() + ", and == compares text: only "
                        + "a String parameter can be compared. Pass it to a bean method "
                        + "instead: @bean.method(authentication, #" + tokens.get(at - 1)[1] + ")");
            }
            return "a" + index;
        }
        if (isWord("authentication")) {
            at++;
            expect(".", "reads a property of authentication; on its own it can only be "
                    + "passed to a bean method");
            if (isWord("name")) {
                at++;
                noChain("authentication.name");
                return RUNTIME + ".name(" + AUTH + ")";
            }
            if (isWord("principal")) {
                at++;
                expect(".", "reads a property of the principal");
                return username("authentication.principal");
            }
            throw new Refused("authentication." + text() + " cannot be read: an expression "
                    + "reads authentication.name and principal.username, and passes anything "
                    + "else to a bean method");
        }
        if (isWord("principal")) {
            at++;
            expect(".", "reads a property of principal; on its own it can only be passed "
                    + "to a bean method");
            return username("principal");
        }
        throw unexpected("where a condition should be: hasRole('X'), isAuthenticated(), "
                + "a comparison of authentication.name, or a call on a bean");
    }

    private String username(String of) throws Refused {
        if (!isWord("username")) {
            throw new Refused(of + "." + text() + " cannot be read: the build knows the "
                    + "principal only as an Object. " + of + ".username is supported; pass "
                    + "principal to a bean method for anything else");
        }
        at++;
        noChain(of + ".username");
        return RUNTIME + ".username(" + AUTH + ")";
    }

    private void noChain(String of) throws Refused {
        if (".".equals(kind())) {
            throw new Refused("a property or method of " + of + " cannot be read: property "
                    + "chains are not supported. Put the logic in a bean method and call it");
        }
    }

    /// The index of the parameter named after a `#`, which has been taken.
    private int parameter() throws Refused {
        if (!"id".equals(kind())) {
            throw unexpected("after #, which names a parameter");
        }
        String name = text();
        at++;
        if (".".equals(kind())) {
            throw new Refused("#" + name + "." + (at + 1 < tokens.size() ? tokens.get(at + 1)[1]
                    : "") + " is a property chain, which is not supported: the build compiles "
                    + "expressions and does not read properties by name. Pass the parameter "
                    + "to a bean method and read it there: @bean.method(authentication, #"
                    + name + ")");
        }
        if (parameters == null) {
            parameters = parameterNames(caller, method);
        }
        Integer index = parameters.get(name);
        if (index == null) {
            throw new Refused("#" + name + " names no parameter of " + method.getName()
                    + (parameters.isEmpty() ? ", whose parameter names the compiled class does "
                    + "not record. Annotate the parameter with @P(\"" + name + "\"), or compile "
                    + "with -parameters or debug information"
                    : "; its parameters are " + parameters.keySet()));
        }
        return index.intValue();
    }

    // -------------------------------------------------------------- bean calls

    /// {kind, source, descriptor or text}: one argument of a bean call.
    private String beanCall() throws Refused {
        if (!"id".equals(kind())) {
            throw unexpected("after @, which names a bean");
        }
        String beanName = text();
        at++;
        expect(".", "comes between a bean and the method called on it");
        if (!"id".equals(kind())) {
            throw unexpected("where the method to call on @" + beanName + " is named");
        }
        String methodName = text();
        at++;
        expect("(", "starts the arguments of @" + beanName + "." + methodName);
        List<String[]> arguments = new ArrayList<String[]>();
        if (!take(")")) {
            do {
                arguments.add(argument(beanName, methodName));
            } while (take(","));
            expect(")", "ends the arguments of @" + beanName + "." + methodName);
        }
        if (".".equals(kind())) {
            throw new Refused("the result of @" + beanName + "." + methodName + "(...) cannot "
                    + "be read further: the method itself must return the boolean");
        }
        BackendBeans.Bean bean = beans.byName.get(beanName);
        if (bean == null) {
            throw new Refused("@" + beanName + " names no bean of this application; its beans "
                    + "are " + beans.byName.keySet());
        }
        if (!bean.isEager() || bean.mockType != null) {
            throw new Refused("@" + beanName + " is " + (bean.lazy ? "lazy" : "scoped "
                    + bean.scope) + "; an expression can call a singleton bean only");
        }
        return call(bean, beanName, methodName, arguments);
    }

    private String[] argument(String beanName, String methodName) throws Refused {
        if ("str".equals(kind())) {
            String[] out = {"string", text()};
            at++;
            return out;
        }
        if ("num".equals(kind())) {
            String[] out = {"number", text()};
            at++;
            return out;
        }
        if (isWord("true") || isWord("false")) {
            String[] out = {"boolean", text()};
            at++;
            return out;
        }
        if (take("#")) {
            String name = text();
            int index = parameter();
            return new String[] {"parameter", String.valueOf(index), name};
        }
        if (isWord("authentication") || isWord("principal")) {
            String word = text();
            at++;
            if (".".equals(kind())) {
                throw new Refused(word + "." + (at + 1 < tokens.size() ? tokens.get(at + 1)[1]
                        : "") + " cannot be passed to @" + beanName + "." + methodName
                        + ": pass " + word + " itself and read it in the method");
            }
            return new String[] {word};
        }
        throw unexpected("as an argument of @" + beanName + "." + methodName + ": an argument "
                + "is authentication, principal, a parameter written #name, or a string, "
                + "whole number or boolean literal");
    }

    private String call(BackendBeans.Bean bean, String beanName, String methodName,
                        List<String[]> arguments) throws Refused {
        List<Object[]> named = new ArrayList<Object[]>();
        Set<String> seen = new LinkedHashSet<String>();
        List<String> queue = new ArrayList<String>();
        queue.add(bean.type);
        while (!queue.isEmpty()) {
            String next = queue.remove(0);
            if (next == null || !seen.add(next)) {
                continue;
            }
            AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(beans.ctx, next);
            if (c == null) {
                continue;
            }
            for (MethodInfo m : c.getMethods()) {
                if (m.getName().equals(methodName) && !m.isSynthetic()
                        && !BackendWeaver.isBody(m.getName())) {
                    boolean overridden = false;
                    for (Object[] earlier : named) {
                        if (((MethodInfo) earlier[1]).getDescriptor().equals(m.getDescriptor())) {
                            overridden = true;
                        }
                    }
                    if (!overridden) {
                        named.add(new Object[] {c, m});
                    }
                }
            }
            queue.add(c.getSuperInternalName());
            queue.addAll(c.getInterfaceInternalNames());
        }
        String shown = "@" + beanName + "." + methodName;
        String type = bean.type.replace('/', '.');
        if (named.isEmpty()) {
            throw new Refused(shown + ": " + type + " has no method named " + methodName);
        }
        List<Object[]> matching = new ArrayList<Object[]>();
        String lastMismatch = null;
        for (Object[] candidate : named) {
            MethodInfo m = (MethodInfo) candidate[1];
            Type[] wanted = Type.getArgumentTypes(m.getDescriptor());
            if (wanted.length != arguments.size()) {
                lastMismatch = "it takes " + wanted.length + " argument(s) and the expression "
                        + "passes " + arguments.size();
                continue;
            }
            String mismatch = null;
            for (int i = 0; i < wanted.length && mismatch == null; i++) {
                mismatch = mismatch(arguments.get(i), wanted[i], i);
            }
            if (mismatch == null) {
                matching.add(candidate);
            } else {
                lastMismatch = mismatch;
            }
        }
        if (matching.isEmpty()) {
            throw new Refused(shown + " does not fit " + type + "." + methodName
                    + (named.size() == 1 ? ((MethodInfo) named.get(0)[1]).getDescriptor() : "")
                    + ": " + lastMismatch);
        }
        if (matching.size() > 1) {
            throw new Refused(shown + " fits " + matching.size() + " overloads of " + type + "."
                    + methodName + "; give the one meant another name");
        }
        AnnotatedClass declaring = (AnnotatedClass) matching.get(0)[0];
        MethodInfo target = (MethodInfo) matching.get(0)[1];
        if (Type.getReturnType(target.getDescriptor()).getSort() != Type.BOOLEAN) {
            throw new Refused(shown + " returns " + Type.getReturnType(target.getDescriptor())
                    .getClassName() + "; a method an expression calls returns boolean");
        }
        AnnotatedClass receiver = RestControllerAnnotationProcessor.resolveClass(beans.ctx,
                bean.type);
        boolean samePackage = RestClientAnnotationProcessor.packageOf(caller.getBinaryName())
                .equals(RestClientAnnotationProcessor.packageOf(type));
        boolean visibleType = receiver != null && (receiver.isAccessibleFromAnywhere()
                || samePackage);
        boolean visibleMethod = target.isPublic() || (samePackage && !target.isPrivate()
                && RestClientAnnotationProcessor.packageOf(declaring.getBinaryName()).equals(
                        RestClientAnnotationProcessor.packageOf(type)));
        if (!visibleType || !visibleMethod) {
            throw new Refused(shown + " is not accessible from " + caller.getSourceName() + ": "
                    + (visibleType ? "the method is not public" : type + " is not public")
                    + ". The check is compiled into " + caller.getSourceName() + "'s package, "
                    + "so make the bean's class and the method public");
        }
        Type[] wanted = Type.getArgumentTypes(target.getDescriptor());
        StringBuilder guards = new StringBuilder();
        StringBuilder invocation = new StringBuilder();
        result.beans.put(beanName, sources.sourceOf(bean.type));
        invocation.append(accessor(beanName)).append("().").append(methodName).append('(');
        for (int i = 0; i < wanted.length; i++) {
            String[] argument = arguments.get(i);
            String kind = argument[0];
            String wantedSource = sources.typeName(wanted[i]);
            boolean object = "java/lang/Object".equals(internalName(wanted[i]));
            invocation.append(i == 0 ? "" : ", ");
            if ("authentication".equals(kind) || "principal".equals(kind)) {
                String value = "authentication".equals(kind) ? AUTH
                        : RUNTIME + ".principal(" + AUTH + ")";
                if (object || ("authentication".equals(kind)
                        && AUTHENTICATION_TYPE.equals(internalName(wanted[i])))) {
                    invocation.append(value);
                } else {
                    // Never a bare cast: on the translated runtime a failed one
                    // does not throw. Whoever is not of the type the method
                    // takes is refused.
                    guards.append(value).append(" instanceof ").append(wantedSource)
                          .append(" && ");
                    invocation.append('(').append(wantedSource).append(") ").append(value);
                }
            } else if ("parameter".equals(kind)) {
                invocation.append('(').append(wantedSource).append(") a").append(argument[1]);
            } else if ("string".equals(kind)) {
                invocation.append(BackendSources.quote(argument[1]));
            } else if ("number".equals(kind)) {
                invocation.append(argument[1]).append(wanted[i].getSort() == Type.LONG ? "L" : "");
            } else {
                invocation.append(argument[1]);
            }
        }
        invocation.append(')');
        return guards.length() == 0 ? invocation.toString()
                : "(" + guards + invocation + ")";
    }

    /// Why `argument` cannot be passed as a `wanted`, or null when it can.
    private String mismatch(String[] argument, Type wanted, int index) {
        String kind = argument[0];
        String wantedInternal = internalName(wanted);
        String position = "argument " + (index + 1);
        if ("authentication".equals(kind)) {
            if ("java/lang/Object".equals(wantedInternal)
                    || (wantedInternal != null && assignable(wantedInternal, AUTHENTICATION_TYPE))) {
                return null;
            }
            return position + " is authentication, and the method takes a "
                    + wanted.getClassName();
        }
        if ("principal".equals(kind)) {
            return wantedInternal != null ? null : position + " is principal, an object, and "
                    + "the method takes a " + wanted.getClassName();
        }
        if ("string".equals(kind)) {
            return "java/lang/String".equals(wantedInternal)
                    || "java/lang/Object".equals(wantedInternal)
                    || "java/lang/CharSequence".equals(wantedInternal) ? null
                    : position + " is a string, and the method takes a " + wanted.getClassName();
        }
        if ("number".equals(kind)) {
            if (wanted.getSort() == Type.LONG) {
                return fits(argument[1], true) ? null : position + " is too large for a long";
            }
            if (wanted.getSort() == Type.INT) {
                return fits(argument[1], false) ? null : position + " is too large for an int";
            }
            return position + " is a whole number, and the method takes a "
                    + wanted.getClassName() + "; a number literal fits an int or a long";
        }
        if ("boolean".equals(kind)) {
            return wanted.getSort() == Type.BOOLEAN ? null
                    : position + " is a boolean, and the method takes a " + wanted.getClassName();
        }
        Type given = argumentTypes[Integer.parseInt(argument[1])];
        if (given.equals(wanted)) {
            return null;
        }
        String givenInternal = internalName(given);
        if (givenInternal != null && wantedInternal != null
                && ("java/lang/Object".equals(wantedInternal)
                || assignable(givenInternal, wantedInternal))) {
            return null;
        }
        if (given.getSort() == Type.ARRAY && "java/lang/Object".equals(wantedInternal)) {
            return null;
        }
        return position + " is #" + argument[2] + ", a " + given.getClassName()
                + ", and the method takes a " + wanted.getClassName();
    }

    private static boolean fits(String digits, boolean asLong) {
        try {
            if (asLong) {
                Long.parseLong(digits);
            } else {
                Integer.parseInt(digits);
            }
            return true;
        } catch (NumberFormatException tooLarge) {
            return false;
        }
    }

    private static String internalName(Type type) {
        return type.getSort() == Type.OBJECT ? type.getInternalName() : null;
    }

    /// Whether a `from` is a `to`, for two class names.
    private boolean assignable(String from, String to) {
        if (from.equals(to) || beans.assignableTypes(from).contains(to)) {
            return true;
        }
        try {
            ClassLoader loader = MethodSecurityCompiler.class.getClassLoader();
            return Class.forName(to.replace('/', '.'), false, loader).isAssignableFrom(
                    Class.forName(from.replace('/', '.'), false, loader));
        } catch (ClassNotFoundException | LinkageError unknown) {
            // Not a class the build can see: javac decides when it compiles
            // the generated check.
            return false;
        }
    }

    // --------------------------------------------------------- parameter names

    /// The names an expression may call `method`'s parameters by, each with its
    /// position: `@P`, then what the class file records.
    static Map<String, Integer> parameterNames(AnnotatedClass cls, final MethodInfo method)
            throws Refused {
        final Type[] types = Type.getArgumentTypes(method.getDescriptor());
        final String[] recorded = new String[types.length];
        File file = cls.getClassFile();
        if (file != null && file.isFile()) {
            final int[] slots = new int[types.length];
            int slot = method.isStatic() ? 0 : 1;
            for (int i = 0; i < types.length; i++) {
                slots[i] = slot;
                slot += types[i].getSize();
            }
            try {
                new ClassReader(Files.readAllBytes(file.toPath())).accept(
                        new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                            String signature, String[] exceptions) {
                        if (!name.equals(method.getName())
                                || !descriptor.equals(method.getDescriptor())) {
                            return null;
                        }
                        return new MethodVisitor(Opcodes.ASM9) {
                            private int next;

                            @Override
                            public void visitParameter(String name, int access) {
                                if (next < recorded.length && name != null) {
                                    recorded[next] = name;
                                }
                                next++;
                            }

                            @Override
                            public void visitLocalVariable(String name, String descriptor,
                                    String signature, Label start, Label end, int index) {
                                for (int i = 0; i < slots.length; i++) {
                                    if (slots[i] == index && recorded[i] == null) {
                                        recorded[i] = name;
                                    }
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_FRAMES);
            } catch (IOException | RuntimeException unreadable) {
                throw new Refused("the class file of " + cls.getSourceName() + " could not be "
                        + "read for its parameter names: " + unreadable);
            }
        }
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        List<Map<String, AnnotationValues>> annotations = method.getParameterAnnotations();
        // @P first: where both name a parameter, the annotation is what was meant.
        for (int i = 0; i < types.length && annotations != null && i < annotations.size(); i++) {
            AnnotationValues p = annotations.get(i) == null ? null
                    : annotations.get(i).get(BackendBeans.P);
            if (p != null) {
                String name = p.getStringOrDefault("value", "").trim();
                if (name.length() == 0 || out.containsKey(name)) {
                    throw new Refused("@P on parameter " + (i + 1) + " of " + method.getName()
                            + (name.length() == 0 ? " has no name" : " repeats the name " + name));
                }
                out.put(name, Integer.valueOf(i));
                recorded[i] = null;
            }
        }
        for (int i = 0; i < types.length; i++) {
            if (recorded[i] != null && !out.containsKey(recorded[i])) {
                out.put(recorded[i], Integer.valueOf(i));
            }
        }
        return out;
    }
}

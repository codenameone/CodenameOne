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

import com.codename1.maven.annotations.ProcessorContext;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Parses every JPQL `@Query` of every repository at build time, with the
/// runtime's own parser over the entity models this build just generated.
///
/// The ORM runs no reflection and no query planning it could do ahead of time,
/// so a statement the parser rejects -- an unknown entity or property, a join
/// form it does not support, a misplaced keyword -- is a build error naming the
/// repository method, never a `PersistenceException` on the first request that
/// reaches it. Using the real parser rather than a second grammar is the point:
/// the build accepts exactly what the server will.
///
/// The parse runs over a `SqlAccess` that answers only the string-building calls
/// and has no database behind it. One rule depends on the dialect the server is
/// deployed with (MySQL's bulk subquery restriction) and stays a run-time check,
/// since the dialect is configuration, not source.
final class JpqlBuildCheck {
    private JpqlBuildCheck() {
    }

    /// @param statements `{method, JPQL, result class}` per statement, as
    ///     [RepositoryGenerator] records them
    static void check(ProcessorContext ctx, Iterable<String> modelClasses, List<String[]> statements) {
        if (statements.isEmpty()) return;
        List<URL> urls = new ArrayList<URL>();
        try {
            urls.add(ctx.getOutputClassDir().toURI().toURL());
            for (String element : ctx.getCompileClasspath()) urls.add(new File(element).toURI().toURL());
        } catch (java.net.MalformedURLException e) {
            ctx.error("Could not check @Query statements: " + e.getMessage());
            return;
        }
        // The platform loader as parent: the core classes must come from the
        // project's own classpath, the version the server will run, never from
        // whatever codenameone-core the build plugin itself was built with.
        // The build tool's own core is the fallback only for a classpath that
        // names none, as the generated sources' own compile allows.
        URL[] path = urls.toArray(new URL[0]);
        URLClassLoader loader = new URLClassLoader(path, ClassLoader.getSystemClassLoader().getParent());
        if (loader.findResource("com/codename1/impl/orm/SqlAccess.class") == null) {
            close(loader);
            loader = new URLClassLoader(path, JpqlBuildCheck.class.getClassLoader());
        }
        try {
            Class<?> sqlAccess = loader.loadClass("com.codename1.impl.orm.SqlAccess");
            Class<?> entityModel = loader.loadClass("com.codename1.impl.orm.EntityModel");
            Map<String, Object> models = new LinkedHashMap<String, Object>();
            for (String name : modelClasses) {
                Constructor<?> c = loader.loadClass(name).getDeclaredConstructor();
                c.setAccessible(true);
                Object model = c.newInstance();
                Class<?> type = (Class<?>) entityModel.getMethod("type").invoke(model);
                models.put(type.getName(), model);
            }
            Object access = Proxy.newProxyInstance(loader, new Class<?>[]{sqlAccess}, new BuildTimeSql());
            Class<?> sessionType = loader.loadClass("com.codename1.impl.orm.SessionImpl");
            Object session = sessionType.getConstructor(sqlAccess, Map.class).newInstance(access, models);
            Method create = sessionType.getMethod("createQuery", String.class, Class.class);
            // A project on an older runtime than this plugin has the parser but
            // not the result check; its statements are still parsed.
            Method result = null;
            try {
                result = loader.loadClass("com.codename1.impl.orm.JpqlQueryImpl").getMethod("checkResultType");
            } catch (NoSuchMethodException older) {
                ctx.getLog().debug("cn1: the runtime on the classpath has no query result check");
            }
            for (String[] s : statements) {
                try {
                    Class<?> type = "java.lang.Object[]".equals(s[2]) ? Object[].class
                            : Class.forName(s[2], false, loader);
                    Object query = create.invoke(session, s[1], type);
                    if (result != null) result.invoke(query);
                } catch (ClassNotFoundException e) {
                    ctx.error(s[0] + ": the result type " + s[2] + " is not on the compile classpath");
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    ctx.error(s[0] + ": JPQL rejected: " + cause.getMessage() + " -- in \"" + s[1] + "\"");
                }
            }
        } catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
            ctx.error("Could not check @Query statements against the generated models: " + cause);
        } catch (LinkageError e) {
            ctx.error("Could not check @Query statements against the generated models: " + e);
        } finally {
            close(loader);
        }
    }

    private static void close(URLClassLoader loader) {
        try {
            loader.close();
        } catch (java.io.IOException ignored) {
            // Nothing was written through it.
        }
    }

    /// The parser only builds SQL text; anything that would reach a database
    /// throws, so a parse that tried to execute is a build error too.
    private static final class BuildTimeSql implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("dialect".equals(name)) return "build-check";
            if ("toString".equals(name)) return "build-time SqlAccess";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name)) return proxy == args[0];
            if ("isTransactionActive".equals(name)) return Boolean.FALSE;
            for (Class<?> thrown : method.getExceptionTypes()) {
                if (thrown == java.io.IOException.class) {
                    throw new UnsupportedOperationException("A query parse reached the database (" + name + ")");
                }
            }
            Class<?> type = method.getReturnType();
            if (type == String.class) {
                if ("quote".equals(name)) return "\"" + args[0] + "\"";
                return args != null && args.length > 0 && args[0] instanceof String ? (String) args[0] : "";
            }
            // Capability questions answer the most permissive value: the
            // dialect is configuration, and its own rules stay run-time checks.
            if (type == boolean.class) return Boolean.TRUE;
            if (type == int.class) return Integer.valueOf(Integer.MAX_VALUE);
            if (type == long.class) return Long.valueOf(Long.MAX_VALUE);
            return null;
        }
    }
}

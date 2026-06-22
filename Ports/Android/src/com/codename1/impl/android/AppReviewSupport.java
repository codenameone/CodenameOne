/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Context;
import com.codename1.util.SuccessCallback;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridges {@link com.codename1.appreview.AppReview} to the Google Play In-App
 * Review API ({@code com.google.android.play:review}).
 *
 * <p>The Play review library is only added to the app's Gradle build when the
 * app references the app-review API (the {@code AndroidGradleBuilder} detects
 * this during its class scan), so this class talks to the library purely
 * through reflection. That keeps the Android port itself compilable without the
 * extra dependency, and the {@code Task}/{@code OnCompleteListener} interfaces
 * are resolved from the methods at runtime so the helper is agnostic to whether
 * the library is on the legacy {@code com.google.android.play.core.tasks} or the
 * newer {@code com.google.android.gms.tasks} package.</p>
 */
class AppReviewSupport {
    private static final String FACTORY = "com.google.android.play.core.review.ReviewManagerFactory";

    private AppReviewSupport() {
    }

    /**
     * @return true if the Play In-App Review library is present in the running
     * app (i.e. the app referenced the review API and the builder bundled it).
     */
    static boolean isSupported() {
        try {
            Class.forName(FACTORY);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Requests the native review flow. Must be called on the Android UI thread.
     * Any failure (library missing, Play services unavailable, quota reached)
     * completes {@code done} with {@code false} so the caller can fall back.
     */
    static void requestReview(final Activity activity, final SuccessCallback<Boolean> done) {
        try {
            Class<?> factory = Class.forName(FACTORY);
            Method create = factory.getMethod("create", Context.class);
            final Object manager = create.invoke(null, activity);

            Object requestFlow = manager.getClass().getMethod("requestReviewFlow").invoke(manager);
            addOnCompleteListener(requestFlow, new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (!"onComplete".equals(method.getName()) || args == null || args.length != 1) {
                        return defaultValue(method);
                    }
                    Object task = args[0];
                    if (isSuccessful(task)) {
                        Object reviewInfo = task.getClass().getMethod("getResult").invoke(task);
                        Object launchFlow = invokeNamed(manager, "launchReviewFlow", activity, reviewInfo);
                        addOnCompleteListener(launchFlow, new InvocationHandler() {
                            public Object invoke(Object p2, Method m2, Object[] a2) throws Throwable {
                                if ("onComplete".equals(m2.getName())) {
                                    complete(done, true);
                                }
                                return defaultValue(m2);
                            }
                        });
                    } else {
                        complete(done, false);
                    }
                    return defaultValue(method);
                }
            });
        } catch (Throwable t) {
            Logger.getLogger("Codename One").log(Level.WARNING, "Native in-app review failed", t);
            complete(done, false);
        }
    }

    private static boolean isSuccessful(Object task) throws Exception {
        Object result = task.getClass().getMethod("isSuccessful").invoke(task);
        return result instanceof Boolean && ((Boolean) result).booleanValue();
    }

    /**
     * Calls {@code task.addOnCompleteListener(listener)} for the single-argument
     * overload, building the listener as a dynamic proxy of whatever
     * {@code OnCompleteListener} interface that overload declares.
     */
    private static void addOnCompleteListener(Object task, InvocationHandler handler) throws Exception {
        Method add = null;
        Method[] methods = task.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().equals("addOnCompleteListener") && m.getParameterTypes().length == 1) {
                add = m;
                break;
            }
        }
        if (add == null) {
            throw new NoSuchMethodException("addOnCompleteListener");
        }
        Class<?> listenerType = add.getParameterTypes()[0];
        Object listener = Proxy.newProxyInstance(listenerType.getClassLoader(),
                new Class[]{listenerType}, handler);
        add.invoke(task, listener);
    }

    private static Object invokeNamed(Object target, String name, Object arg1, Object arg2) throws Exception {
        Method[] methods = target.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().equals(name) && m.getParameterTypes().length == 2) {
                return m.invoke(target, arg1, arg2);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static void complete(SuccessCallback<Boolean> done, boolean value) {
        if (done != null) {
            done.onSucess(value ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    private static Object defaultValue(Method method) {
        Class<?> ret = method.getReturnType();
        if (ret == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (ret == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        return null;
    }
}

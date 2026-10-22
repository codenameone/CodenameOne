/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.gorouter;

import com.codename1.flutter.BuildContext;

import dart.async.Future;

/**
 * go_router's {@code GoRouterHelper} extension on {@code BuildContext}:
 * {@code context.go('/pets/1')}, {@code context.push(...)}, {@code context.pop()}.
 */
public final class GoRouterHelper {
    private GoRouterHelper() {
    }

    private static GoRouter router(BuildContext context) {
        GoRouter r = GoRouter.of(context);
        if (r == null) {
            throw new dart.core.StateError("No GoRouter found in context");
        }
        return r;
    }

    public static void go(BuildContext context, String location, Object extra) {
        router(context).go(location, extra);
    }

    public static Future<Object> push(BuildContext context, String location, Object extra) {
        return router(context).push(location, extra);
    }

    public static void pushReplacement(BuildContext context, String location, Object extra) {
        router(context).pushReplacement(location, extra);
    }

    public static void replace(BuildContext context, String location, Object extra) {
        router(context).replace(location, extra);
    }

    public static void pop(BuildContext context, Object result) {
        router(context).pop(result);
    }

    public static boolean canPop(BuildContext context) {
        return router(context).canPop();
    }

    public static void goNamed(BuildContext context, String name, Object pathParameters,
            Object queryParameters, Object extra) {
        router(context).goNamed(name, pathParameters, queryParameters, extra);
    }

    public static Future<Object> pushNamed(BuildContext context, String name, Object pathParameters,
            Object queryParameters, Object extra) {
        return router(context).pushNamed(name, pathParameters, queryParameters, extra);
    }

    public static String namedLocation(BuildContext context, String name, Object pathParameters,
            Object queryParameters) {
        return router(context).namedLocation(name, pathParameters, queryParameters);
    }
}

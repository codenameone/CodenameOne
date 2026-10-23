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
package com.codename1.backend.web;

/// The controller method a request was routed to, as interceptors and exception
/// resolvers see it.
///
/// The build creates one per route, as a constant of the generated router, so
/// reading it costs nothing per request. It names the method rather than holding
/// a reflective handle: nothing in a translated backend can call a method by
/// name, and nothing here needs to.
public final class HandlerMethod {
    private final String controllerClass;
    private final String methodName;
    private final String httpMethod;
    private final String pattern;

    /// @param controllerClass the binary name of the controller class
    /// @param methodName the name of the handler method
    /// @param httpMethod the HTTP method the route is mapped to
    /// @param pattern the route template, such as `/notes/{id}`
    public HandlerMethod(String controllerClass, String methodName, String httpMethod,
                         String pattern) {
        this.controllerClass = controllerClass;
        this.methodName = methodName;
        this.httpMethod = httpMethod;
        this.pattern = pattern;
    }

    /// The binary name of the controller class, such as `com.example.Notes`.
    public String getControllerClass() {
        return controllerClass;
    }

    /// The name of the handler method.
    public String getMethodName() {
        return methodName;
    }

    /// The HTTP method the route is mapped to. A `GET` route also answers `HEAD`,
    /// and reports `GET` here when it does.
    public String getHttpMethod() {
        return httpMethod;
    }

    /// The route template the request matched, such as `/notes/{id}`; never the
    /// concrete path.
    public String getPattern() {
        return pattern;
    }

    /// Whether the controller class is in `pkg` or one of its subpackages.
    ///
    /// @param pkg a package name, such as `com.example.api`
    /// @return true when the controller is declared there
    public boolean isInPackage(String pkg) {
        if (pkg == null || pkg.length() == 0) {
            return true;
        }
        return controllerClass.startsWith(pkg)
                && (controllerClass.length() == pkg.length()
                    || controllerClass.charAt(pkg.length()) == '.');
    }

    @Override
    public String toString() {
        return httpMethod + " " + pattern + " -> " + controllerClass + "." + methodName;
    }
}

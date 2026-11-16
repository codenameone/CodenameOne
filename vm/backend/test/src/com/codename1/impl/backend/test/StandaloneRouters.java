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
package com.codename1.impl.backend.test;

import com.codename1.backend.HttpServer;
import java.util.Iterator;
import java.util.ServiceLoader;

/// The generated routers of an application's controllers, for
/// `MockMvcBuilders.standaloneSetup`. The test build writes the one
/// implementation, an `instanceof` test per controller class, and names it in
/// `META-INF/services`: a controller instance is matched to its router without
/// reflection.
public abstract class StandaloneRouters {
    private static StandaloneRouters found;
    private static boolean looked;

    /// The router for `controller`, or null when it is not a controller the
    /// build generated one for.
    protected abstract HttpServer.Handler router(Object controller);

    /// The router for `controller`; throws naming it when there is none.
    public static synchronized HttpServer.Handler of(Object controller) {
        if (controller == null) {
            throw new IllegalArgumentException("No controller");
        }
        if (!looked) {
            looked = true;
            Iterator<StandaloneRouters> it = ServiceLoader.load(StandaloneRouters.class).iterator();
            found = it.hasNext() ? it.next() : null;
        }
        HttpServer.Handler router = found == null ? null : found.router(controller);
        if (router == null) {
            throw new IllegalArgumentException(controller.getClass().getName()
                    + " is not a @RestController or @Controller of this application");
        }
        return router;
    }
}

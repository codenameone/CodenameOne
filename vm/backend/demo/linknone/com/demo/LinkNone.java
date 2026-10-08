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
package com.demo;

import com.codename1.backend.Backend;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.WiringEnvironment;

/// A server with no security at all: no chain, and no call that links the
/// layer. Its binary must hold none of `com.codename1.backend.security`.
public final class LinkNone {
    private LinkNone() {
    }

    public static void main(String[] args) throws Exception {
        Backend.Builder builder = Backend.builder().quiet().host("127.0.0.1").port(0);
        BackendAccess.get().application(builder, new LinkApp() {
            @Override
            void chains(WiringEnvironment environment) {
            }
        });
        Backend backend = builder.start();
        int open = LinkApp.status(backend, "GET", "/open");
        backend.stop();
        System.out.println("LINKCHECK none open=" + open);
        System.out.println(open == 200 ? "LINKCHECK OK" : "LINKCHECK FAILED");
    }
}

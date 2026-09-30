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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Builds the MCP endpoint into the server, serving the application's
/// [McpTool] methods over Streamable HTTP at `/mcp`.
///
/// A class with an [McpTool] method already does this, so the annotation is
/// needed only to move the endpoint or to name the origins it accepts. Without
/// either -- and without `cn1.mcp.enabled=true` in a properties file -- the entry
/// point of a packaged server never names the endpoint and the translator leaves
/// its code out of the binary. The endpoint serves only when it has a tool, and
/// `cn1.mcp.enabled=false` turns it off at start-up.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EnableMcpServer {
    /// Where the endpoint is served; `cn1.mcp.path`. Empty means `/mcp`.
    String path() default "";

    /// The browser origins allowed to call it; `cn1.mcp.allowedOrigins`.
    String[] allowedOrigins() default {};
}

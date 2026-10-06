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
package com.codename1.backend.test;

import java.util.List;

/// Handlers for `andDo(...)`, for static import.
public final class MockMvcResultHandlers {
    private MockMvcResultHandlers() {
    }

    /// Prints the request line, the status, the headers and the body.
    public static ResultHandler print() {
        return new ResultHandler() {
            @Override
            public void handle(MvcResult result) {
                MockResponse r = result.getResponse();
                StringBuilder sb = new StringBuilder();
                sb.append(result.getRequestMethod()).append(' ').append(result.getRequestTarget())
                        .append("\n  Status = ").append(r.getStatus());
                List names = r.getHeaderNames();
                for (Object entry : names) {
                    String name = (String) entry;
                    sb.append("\n  ").append(name).append(" = ").append(r.getHeaders(name));
                }
                sb.append("\n  Body = ").append(r.getContentAsString());
                System.out.println(sb.toString());
            }
        };
    }
}

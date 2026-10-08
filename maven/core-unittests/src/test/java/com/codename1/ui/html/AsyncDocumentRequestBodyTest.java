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
package com.codename1.ui.html;

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The body of a form an HTML page posts: its parameters, written through a writer that
/// has to be flushed before it is dropped. See `ConnectionRequestFormBodyTest`.
class AsyncDocumentRequestBodyTest extends UITestBase {

    @Test
    void aPostedFormsParametersReachTheStream() throws Exception {
        DocumentInfo form = new DocumentInfo("http://example.com/search", "q=codename+one&page=2", true);
        form.setEncoding(DocumentInfo.ENCODING_UTF8);
        ConnectionRequest request = new AsyncDocumentRequestHandlerImpl()
                .createConnectionRequest(form, null, new Object[1]);
        request.setPost(true);
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        Method build = ConnectionRequest.class.getDeclaredMethod("buildRequestBody", OutputStream.class);
        build.setAccessible(true);
        build.invoke(request, sent);
        assertEquals("q=codename+one&page=2", new String(sent.toByteArray(), "UTF-8"));
    }
}

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
package com.codename1.backend;

import com.codename1.backend.orm.EntityManager;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.BackendApplication;
import com.codename1.impl.backend.mcp.McpServer;
import com.codename1.impl.backend.mcp.McpTool;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The one implementation of [BackendAccess]: this package's internals, for the
/// generated code and the runtime's classes in other packages. Installed by
/// [Backend]'s static initializer; see BackendAccess for why it exists.
final class Access extends BackendAccess {
    @Override
    public void application(Backend.Builder builder, BackendApplication application) {
        builder.application(application);
    }

    @Override
    public void mcp(Backend.Builder builder, McpServer.Extension devTools) {
        builder.mcp(devTools);
    }

    @Override
    public void management(Backend.Builder builder) {
        builder.management();
    }

    @Override
    public void compiledSettings(Backend.Builder builder, String[] keysAndValues) {
        builder.compiledSettings(keysAndValues);
    }

    @Override
    public Config testConfig(java.util.Properties values, String profile) {
        return Config.overriding(values, profile);
    }

    @Override
    public void mcpTool(Backend.Builder builder, McpTool tool) {
        builder.mcpTool(tool);
    }

    @Override
    public void serviceName(Backend.Builder builder, String name) {
        builder.serviceName(name);
    }

    @Override
    public BackendApplication applicationOf(Backend backend) {
        return backend.getApplication();
    }

    @Override
    public List managedBeans(Backend backend) {
        return backend.getManagedBeans();
    }

    @Override
    public boolean isMeasured(Backend backend) {
        return backend.isMeasured();
    }

    @Override
    public void enableRequestLog(Backend backend, int capacity) {
        backend.getRequestLog().enable(capacity);
    }

    @Override
    public List recentRequests(Backend backend, int limit, boolean failuresOnly) {
        return backend.getRequestLog().recent(limit, failuresOnly);
    }

    @Override
    public HttpServer.Response dispatch(Backend backend, String method, String target,
                                        Map headers, byte[] body) throws Exception {
        Map lower = new LinkedHashMap();
        if (headers != null) {
            Iterator it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                lower.put(asciiLower(String.valueOf(e.getKey())), e.getValue());
            }
        }
        // The target is not validated as the HTTP/1 and HTTP/2 parsers validate a
        // wire target (bad escapes, fragments, control characters): MockMvc tests
        // the application, as Spring's does, and the testing guide sends protocol
        // tests to a real port.
        HttpServer.Request request = new HttpServer.Request(method, target, "HTTP/1.1", lower,
                null);
        if (body != null && body.length > 0) {
            Object[] content;
            try {
                content = HttpServer.bodyContent(request, body, 0, body.length);
            } catch (IOException refused) {
                return HttpServer.Response.text(HttpServer.refusalStatus(refused),
                        refused.getMessage());
            }
            request.setBody((String) content[0], (byte[]) content[1]);
        }
        return backend.dispatch(request);
    }

    /// A header name folded to lower case by hand: names are ASCII by
    /// specification, and toLowerCase() is locale sensitive.
    private static String asciiLower(String value) {
        char[] chars = value.toCharArray();
        for (int iter = 0 ; iter < chars.length ; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }

    @Override
    public int status(HttpServer.Response response) {
        return response.status;
    }

    @Override
    public List headers(HttpServer.Response response) {
        List out = new ArrayList();
        if (response.extraHeaders == null) {
            return out;
        }
        Iterator it = response.extraHeaders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            Object value = e.getValue();
            if (value instanceof List) {
                List values = (List) value;
                for (Object one : values) {
                    out.add(new String[] {String.valueOf(e.getKey()), String.valueOf(one)});
                }
            } else if (value != null) {
                out.add(new String[] {String.valueOf(e.getKey()), String.valueOf(value)});
            }
        }
        return out;
    }

    @Override
    public byte[] body(HttpServer.Response response) throws IOException {
        response.serializeDeferredJson();
        if (response.fileFd >= 0) {
            // The writer owns closing a file body, and there is no writer here.
            try {
                return StaticFiles.readAll(response.fileFd, response.fileOffset,
                        response.fileLength);
            } finally {
                response.discard();
            }
        }
        return response.body == null ? new byte[0] : response.body;
    }

    @Override
    public void discard(HttpServer.Response response) {
        response.discard();
    }

    @Override
    public String contentType(HttpServer.Response response) {
        return response.contentType;
    }

    @Override
    public Object[] requestBeans(HttpServer.Request request, int count) {
        return request.scopedBeans(count);
    }

    @Override
    public Object sessionBeanLock(HttpSession session) {
        return session.beanLock();
    }

    @Override
    public Object[] sessionBeans(HttpSession session, int count) {
        return session.scopedBeans(count);
    }

    @Override
    public void napUntil(long deadlineMillis) {
        HttpServer.napUntil(deadlineMillis);
    }

    @Override
    public Span captureParent() {
        return Tracing.captureParent();
    }

    @Override
    public Tracer captureOwner() {
        return Tracing.captureOwner();
    }

    @Override
    public Tracer untraced() {
        return Tracing.NONE;
    }

    @Override
    public Object inBackground(String name, Span parent, Tracer own, Tracing.Work work)
            throws Exception {
        return Tracing.inBackground(name, parent, own, work);
    }

    @Override
    public Object begin(int propagation, boolean readOnly, int timeoutSeconds) {
        return Transactions.begin(propagation, readOnly, timeoutSeconds);
    }

    @Override
    public void commit(Object transaction) {
        if (transaction instanceof Transactions.Transaction) {
            Transactions.commit((Transactions.Transaction) transaction);
        } else {
            throw new IllegalArgumentException("Not a transaction begin() returned");
        }
    }

    @Override
    public void afterThrow(Object transaction, boolean rollback) {
        if (transaction instanceof Transactions.Transaction) {
            Transactions.afterThrow((Transactions.Transaction) transaction, rollback);
        } else {
            throw new IllegalArgumentException("Not a transaction begin() returned");
        }
    }

    @Override
    public Database joined(DataSource pool) throws IOException {
        return Transactions.joined(pool);
    }

    @Override
    public boolean isJoined(DataSource pool, Database db) {
        return Transactions.isJoined(pool, db);
    }

    @Override
    public boolean isActiveOn(DataSource pool) {
        return Transactions.isActiveOn(pool);
    }

    @Override
    public com.codename1.orm.session.Session session(EntityManager entities) {
        return Transactions.session(entities);
    }

    @Override
    public void markRollbackOnly() {
        Transactions.markRollbackOnly();
    }

    @Override
    public void markRollbackOnly(DataSource pool) {
        Transactions.markRollbackOnly(pool);
    }

    @Override
    public TaskExecutor executor(String name, int kind) {
        return Tasks.executor(name, kind);
    }

    @Override
    public List executorsOf(Collection servers) {
        return Tasks.executorsOf(servers);
    }
}

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
package com.codename1.impl.backend;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.metrics.Gauge;
import com.codename1.backend.orm.EntityManager;
import com.codename1.impl.backend.mcp.McpTool;
import java.util.ArrayList;
import java.util.List;

/// What a [BackendApplication] is built from: the configuration, the database,
/// and the registrations the generated wiring makes while it builds the beans.
public final class WiringEnvironment {
    private final Config config;
    private final DataSource dataSource;
    private final EntityManager entities;

    private final List tools;
    private final List managed;
    /// {name, description, unit, Gauge.Source}, for the server to add and later remove.
    final List gauges = new ArrayList();

    public WiringEnvironment(Config config, DataSource dataSource, EntityManager entities,
                List tools, List managed) {
        this.config = config;
        this.dataSource = dataSource;
        this.entities = entities;
        this.tools = tools;
        this.managed = managed;
    }

    /// Publishes an `@McpTool` on this server's MCP endpoint. Generated
    /// code calls this while it builds the beans; the tool belongs to this
    /// server only, so a server started later in the same process does not
    /// serve a tool bound to a bean that has been destroyed.
    public void registerTool(McpTool tool) {
        for (Object element : tools) {
            if (((McpTool) element).name()
                    .equals(tool.name())) {
                // Two active beans publishing one name: one would be
                // unreachable, and which depends on construction order.
                throw new IllegalStateException("Two MCP tools are named \""
                        + tool.name() + "\"; give one a distinct name");
            }
        }
        tools.add(tool);
    }

    /// Publishes a managed attribute as a gauge of this server's: added when
    /// the server starts and removed when it stops. Generated code calls this.
    public void registerGauge(String name, String description, String unit,
                              Gauge.Source source) {
        gauges.add(new Object[] {name, description, unit, source});
    }

    /// Registers a managed bean with this server. Generated code calls this.
    public void registerManaged(ManagedBean bean) {
        String objectName = bean.getObjectName();
        if (objectName == null || objectName.length() == 0 || objectName.length() > 128) {
            throw new IllegalArgumentException("A managed bean needs a name of 1 to 128 "
                    + "characters");
        }
        for (int iter = 0 ; iter < objectName.length() ; iter++) {
            char c = objectName.charAt(iter);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == '-')) {
                // One URL segment, matched undecoded; see the build's check.
                throw new IllegalArgumentException("Managed bean \"" + objectName
                        + "\": a name is letters, digits, _, - and . only");
            }
        }
        for (Object element : managed) {
            if (((ManagedBean) element).getObjectName()
                    .equals(bean.getObjectName())) {
                throw new IllegalStateException("Two managed resources are named \""
                        + bean.getObjectName() + "\"; set objectName on one");
            }
        }
        managed.add(bean);
    }

    public Config getConfig() {
        return config;
    }

    /// The pool, or null when this server has no database.
    public DataSource getDataSource() {
        return dataSource;
    }

    /// The entity manager, or null when the build generated no entities.
    public EntityManager getEntityManager() {
        return entities;
    }

    /// The gauges registered so far, as {name, description, unit, Gauge.Source},
    /// for the server to add when it starts and remove when it stops.
    public List gauges() {
        return gauges;
    }
}

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
package com.codename1.impl.orm;

import com.codename1.orm.session.Identifier;
import com.codename1.orm.session.PersistenceException;

import java.util.LinkedHashMap;
import java.util.Map;

/// Startup registry populated by generated bootstraps.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class Models {
    private static final Map<String, EntityModel<?>> REGISTRY = new LinkedHashMap<String, EntityModel<?>>();
    private Models() {
    }
    public static synchronized void register(EntityModel<?> model) {
        if (model == null) {
            throw new IllegalArgumentException("model is null");
        }
        REGISTRY.put(model.type().getName(), model);
    }
    /// Reads a relationship key without initializing it. Used by generated mappings.
    public static Object foreignKey(Object owner, int index, Object target) {
        if (owner instanceof ManagedEntity) {
            EntityState state = ((ManagedEntity) owner).__cn1OrmState();
            if (state != null && !state.isLoaded(index)) {
                return state.key(index);
            }
        }
        if (target == null) {
            return null;
        }
        EntityModel model;
        EntityState state = owner instanceof ManagedEntity ? ((ManagedEntity) owner).__cn1OrmState() : null;
        if (state != null && state.session != null) {
            model = state.session.model(target.getClass());
        } else {
            synchronized (Models.class) {
                model = REGISTRY.get(target.getClass().getName());
            }
        }
        if (model == null) {
            throw new PersistenceException("No model for relationship target");
        }
        return model.identifier(target);
    }
    /// Component of a mapped foreign key, preserving unloaded association state.
    public static Object foreignKey(Object owner, int index, Object target, int component) {
        Object key = foreignKey(owner, index, target);
        if (key == null) {
            return null;
        }
        return key instanceof Identifier ? ((Identifier) key).values()[component] : key;
    }
    /// Builds a map using a target entity's unconverted basic attribute.
    public static Map mapBy(java.util.Collection values, Class type, String field) {
        if (values == null) {
            return null;
        }
        EntityModel model;
        synchronized (Models.class) {
            model = REGISTRY.get(type.getName());
        }
        if (model == null) {
            throw new PersistenceException("No model for map values");
        }
        int index = model.index(field);
        Map result = new LinkedHashMap();
        for (Object value : values) {
            Object key = model.domainValue(value, index);
            if (key == null || result.containsKey(key)) {
                throw new PersistenceException("Null or duplicate map key: " + field);
            }
            result.put(key, value);
        }
        return result;
    }
    /// New managed mappings must not silently fall through legacy scalar CRUD.
    public static synchronized boolean requiresSession(Class type) {
        EntityModel model = REGISTRY.get(type.getName());
        return model != null && model.requiresSession();
    }
    public static synchronized Map<String, EntityModel<?>> snapshot() {
        return new LinkedHashMap<String, EntityModel<?>>(REGISTRY);
    }
}

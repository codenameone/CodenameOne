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

import com.codename1.orm.Dao;
import java.util.HashMap;
import java.util.Map;

/// Internal registry shared by generated bootstrap code and entity managers.
/// @hidden
public final class DaoRegistry {
    private static final Map<String, Dao<?>> DAOS = new HashMap<String, Dao<?>>();
    private static final Map<String, DaoFactory<?>> FACTORIES = new HashMap<String, DaoFactory<?>>();

    private DaoRegistry() {
    }

    public static <T> void register(Dao<T> dao) {
        if (dao == null) {
            throw new IllegalArgumentException("dao is null");
        }
        synchronized (DAOS) {
            FACTORIES.remove(dao.type().getName());
            DAOS.put(dao.type().getName(), dao);
        }
    }

    public static <T> void registerFactory(Class<T> type, DaoFactory<T> factory) {
        if (type == null || factory == null) {
            throw new IllegalArgumentException("type/factory is null");
        }
        synchronized (DAOS) {
            DAOS.remove(type.getName());
            FACTORIES.put(type.getName(), factory);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Dao<T> create(Class<T> type) {
        synchronized (DAOS) {
            DaoFactory<T> factory = (DaoFactory<T>) FACTORIES.get(type.getName());
            return factory == null ? (Dao<T>) DAOS.get(type.getName()) : factory.create();
        }
    }
}

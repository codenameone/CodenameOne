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

import com.codename1.orm.session.Session;
import com.codename1.orm.session.PersistenceException;
import java.util.HashMap;
import java.util.Map;

/// Direct factories installed by the generated DAO bootstrap.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class RepositoryFactories {
    private static final Map<Class<?>, Factory<?>> FACTORIES = new HashMap<Class<?>, Factory<?>>();

    private RepositoryFactories() {
    }

    public interface Factory<T> {
        T create(Session session);
    }

    public static synchronized <T> void register(Class<T> type, Factory<T> factory) {
        FACTORIES.put(type, factory);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> T create(Class<T> type, Session session) {
        Factory<T> factory = (Factory<T>) FACTORIES.get(type);
        if (factory == null) {
            throw new PersistenceException("No generated repository registered for " + type.getName()
                    + "; run cn1:process-annotations and initialize the generated DAO bootstrap");
        }
        return factory.create(session);
    }
}

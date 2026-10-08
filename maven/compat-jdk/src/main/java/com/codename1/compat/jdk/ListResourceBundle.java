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
package com.codename1.compat.jdk;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// `java.util.ListResourceBundle` for the Codename One runtime: a resource
/// bundle whose content a subclass supplies as an array of key and value
/// pairs.
public abstract class ListResourceBundle extends ResourceBundle {

    private Map<String, Object> lookup;

    public ListResourceBundle() {
        super();
    }

    /// The bundle's content: each element is a pair, the key (a `String`)
    /// and its value.
    protected abstract Object[][] getContents();

    private Map<String, Object> lookup() {
        if (lookup == null) {
            Map<String, Object> built = new HashMap<String, Object>();
            Object[][] contents = getContents();
            for (int i = 0; i < contents.length; i++) {
                Object key = contents[i][0];
                Object value = contents[i][1];
                if (!(key instanceof String) || value == null) {
                    throw new NullPointerException();
                }
                built.put((String) key, value);
            }
            lookup = built;
        }
        return lookup;
    }

    @Override
    public final Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return lookup().get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return new ResourceBundle.KeyEnumeration(keySet().iterator());
    }

    @Override
    protected Set<String> handleKeySet() {
        return lookup().keySet();
    }
}

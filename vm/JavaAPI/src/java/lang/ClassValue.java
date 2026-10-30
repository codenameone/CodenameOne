/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package java.lang;

import java.util.HashMap;

/// Lazily computes one value per class and keeps it until [#remove(Class)].
public abstract class ClassValue<T> {
    private final HashMap<Class<?>, Object> values = new HashMap<Class<?>, Object>();
    private static final Object NULL = new Object();

    protected ClassValue() {
    }

    protected abstract T computeValue(Class<?> type);

    @SuppressWarnings("unchecked")
    public T get(Class<?> type) {
        Object v;
        synchronized (values) {
            v = values.get(type);
        }
        if (v == null) {
            T computed = computeValue(type);
            synchronized (values) {
                Object raced = values.get(type);
                if (raced != null) {
                    v = raced;
                } else {
                    v = computed == null ? NULL : computed;
                    values.put(type, v);
                }
            }
        }
        return v == NULL ? null : (T) v;
    }

    public void remove(Class<?> type) {
        synchronized (values) {
            values.remove(type);
        }
    }
}

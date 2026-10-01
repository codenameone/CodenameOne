/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * Implemented by widgets that publish a value to their subtree by type
 * (provider's {@code Provider}/{@code ChangeNotifierProvider} and scoped_model's
 * {@code ScopedModel}). {@link BuildContext#providerValueOfType(Class)} walks the
 * element tree and asks each ancestor provider whether it supplies the requested
 * type.
 */
public interface InheritedValueProvider {

    /**
     * The published value when it is assignable to {@code type}, otherwise null.
     */
    Object providedValueFor(Class<?> type);

    /**
     * Whether this provider answers a lookup for {@code type} -- decided separately from
     * the value, because a provider can match and publish null ({@code Provider<User?>}
     * before sign-in). Read from the value alone, such a provider was skipped: the lookup
     * went past it to an outer one, or to nothing, and never subscribed, so the reader
     * was not rebuilt when the value arrived. The default keeps the value-based answer
     * for providers with no type of their own.
     */
    default boolean providesType(Class<?> type) {
        return providedValueFor(type) != null;
    }
}

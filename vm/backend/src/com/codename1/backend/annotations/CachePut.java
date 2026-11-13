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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Always runs the method, then keeps what it returned under its key, so a later
/// [Cacheable] call of the same key is answered with it -- Spring's `@CachePut`.
///
/// Keys, names and lifetimes are as [Cacheable] describes. A null result removes
/// the entry, so the cache never answers with a value the method no longer gives.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CachePut {
    /// The cache's name; the first is used when several are given.
    String[] value() default {};
    /// The same as [#value], as Spring spells it.
    String[] cacheNames() default {};
    /// The key expression; see [Cacheable].
    String key() default "";
    /// Accepted when it is `#result == null`; see [Cacheable#unless()].
    String unless() default "";
}

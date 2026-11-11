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

/// Answers a call from the cache when the cache holds an answer for its key, and
/// otherwise runs the method and keeps what it returned -- Spring's `@Cacheable`.
///
/// ```java
/// @Cacheable(value = "products", key = "#id")
/// public Product product(long id) { ... }
/// ```
///
/// The build rewrites the method: there is no proxy, so a call from inside the
/// same class is cached too, which Spring's proxy would skip. The key expression
/// is compiled when the application is built, and one the build cannot compile
/// fails the build. It may name a parameter (`#id`, or `#p0`/`#a0` by
/// position), a property of one (`#user.id`, read through its getter or public
/// field), a quoted literal (`'all'`), `#root.methodName`, and join them with
/// `+`. Empty means every parameter, as Spring's `SimpleKeyGenerator` keys.
///
/// The value is kept in the server's key/value store -- Redis once `cn1.kv.url`
/// names one, so every process shares it -- written as JSON by a codec the build
/// generates for the return type. A null result is not kept, as Spring keeps
/// none with `unless = "#result == null"`. How long an entry lives is
/// `cn1.cache.<name>.ttl` in seconds, then `cn1.cache.ttl`, then
/// `spring.cache.redis.time-to-live`; unset, it lives until evicted.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Cacheable {
    /// The cache's name; the first is used when several are given.
    String[] value() default {};
    /// The same as [#value], as Spring spells it.
    String[] cacheNames() default {};
    /// The key expression; see the class description.
    String key() default "";
    /// Accepted when it is `#result == null`, which is what happens anyway;
    /// any other expression fails the build.
    String unless() default "";
}

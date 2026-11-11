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
/// A key/value store shared by every process of a deployment -- Redis, Valkey
/// or KeyDB, spoken directly over RESP2 with no client library -- and the
/// in-process store that stands in for one when none is configured.
///
/// Set `cn1.kv.url` (or Spring Boot's `spring.data.redis.*`) and the server's
/// sessions (`cn1.session.store=kv`), `@Cacheable` entries, scheduler locks and
/// [com.codename1.backend.security.ratelimit.KeyValueRateLimiter] counts all
/// live there, so replicas behind a load balancer agree. [KeyValueStores#current()]
/// is the store of the calling thread's server, and a [KeyValueStore] parameter
/// of a bean is given it.
package com.codename1.backend.kv;

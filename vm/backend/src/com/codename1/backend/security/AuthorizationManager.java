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
package com.codename1.backend.security;

import java.util.function.Supplier;

/// Decides whether an authentication may reach something: for a request rule,
/// a [RequestAuthorizationContext].
///
/// ```java
/// auth.requestMatchers("/accounts/{owner}/**").access((authentication, context) ->
///         new AuthorizationDecision(context.getVariables().get("owner")
///                 .equals(authentication.get().getName())));
/// ```
public interface AuthorizationManager<T> {
    /// The decision; null abstains, which a request rule reads as granted.
    ///
    /// @param authentication who is asking, looked up only when the rule needs it
    AuthorizationDecision check(Supplier<Authentication> authentication, T object);
}

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
package com.codename1.backend.security.oauth2.client;

/// Makes the user of a sign-in through an identity provider.
///
/// This is where an application decides who the provider's user is here: it
/// may look them up, create them, refuse them, and return a principal named
/// after its own account. [DefaultOAuth2UserService] and [OidcUserService]
/// take the provider's word; [LinkingOAuth2UserService] ties the provider's
/// user to a local one.
///
/// @param <R> what the service is asked with
/// @param <U> the user it makes
public interface OAuth2UserService<R extends OAuth2UserRequest, U extends OAuth2User> {
    /// The user.
    ///
    /// - `OAuth2AuthenticationException`: to refuse the sign-in
    U loadUser(R userRequest);
}

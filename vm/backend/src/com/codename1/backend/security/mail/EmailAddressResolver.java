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
package com.codename1.backend.security.mail;

import com.codename1.backend.mail.Mail;

/// Says where a user's mail goes. A user record here has a name and no
/// address, so the flows that send mail ask this.
///
/// ```java
/// @Bean
/// EmailAddressResolver addresses(AccountRepository accounts) {
///     return username -> accounts.findByUsername(username).getEmail();
/// }
/// ```
public interface EmailAddressResolver {
    /// The user name itself where it is an address, as it is in an
    /// application whose users sign in with one; nothing otherwise.
    EmailAddressResolver USERNAME = new EmailAddressResolver() {
        @Override
        public String emailFor(String username) {
            return Mail.isAddress(username) ? username : null;
        }
    };

    /// The address mail for `username` is sent to, or null when there is none
    /// and nothing should be sent.
    String emailFor(String username);
}

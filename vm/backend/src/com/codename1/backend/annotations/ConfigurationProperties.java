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

/// Binds a group of configuration keys to the setters of a bean.
///
/// ```java
/// @Component
/// @ConfigurationProperties("mail")
/// public class MailSettings {
///     private String host = "localhost";
///     private int port = 25;
///     public void setHost(String host) { this.host = host; }
///     public void setPort(int port) { this.port = port; }
/// }
/// ```
///
/// Reads `mail.host` and `mail.port`, and calls a setter only when its key is
/// set, so a field initializer is the default. The build lists the setters and
/// writes one typed read per setter into the entry point; nothing is discovered
/// at run time. Spring's relaxed binding applies to the setter name: `setMaxSize`
/// reads `mail.max-size` when `mail.maxSize` is not set.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ConfigurationProperties {
    /// The key prefix, without the trailing dot.
    String value() default "";
    /// Same as [#value]; Spring accepts either.
    String prefix() default "";
}

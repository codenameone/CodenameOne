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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a POJO or `PropertyBusinessObject` as a target for the build-time
/// JSON / XML mapping processor.
///
/// The Codename One Maven plugin scans every `@Mapped` class at build time and
/// emits a reflection-free `Mapper` next to it. Application code reaches the
/// generated mapper through `com.codename1.mapping.Mappers`:
///
/// ```java
/// @Mapped
/// public class User {
///     @JsonProperty("first_name")
///     public String firstName;
///     public int age;
///     @JsonIgnore
///     transient String passwordHash;
/// }
///
/// String json = Mappers.toJson(new User());
/// User u = Mappers.fromJson(json, User.class);
/// ```
///
/// Either public fields *or* JavaBeans-style accessors (`getX` / `setX`,
/// `isX` for booleans) are walked. `com.codename1.properties.Property` fields
/// are routed through `Property#get` / `Property#set` so the same class can
/// expose either programming model. Nothing on the runtime side uses
/// reflection or `Class.forName` -- every read and write is a direct symbol
/// reference that the iOS `Class.forName` ban and ParparVM rename pass leave
/// intact.
///
/// The `@XmlRoot` / `@XmlElement` / `@XmlAttribute` / `@XmlTransient`
/// annotations control the XML projection on top of the same fields.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Mapped {
}

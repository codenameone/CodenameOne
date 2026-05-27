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
package com.codename1.mapping;

import com.codename1.xml.Element;

import java.util.Map;

/// Runtime contract a build-time-generated mapper implements for one
/// `@Mapped` class. Application code rarely references `Mapper` directly --
/// it goes through the typed entry points on `Mappers`. The interface exists
/// so generated code has a single ServiceLoader-friendly shape and so
/// extensions (custom converters, plug-in serializers) can sit on the same
/// type.
public interface Mapper<T> {

    /// The class this mapper handles. The instance registry on `Mappers` is
    /// keyed on this value; generated mappers never call `Class.forName`.
    Class<T> type();

    /// Serializes `instance` to the JSON map representation that
    /// `com.codename1.io.JSONParser` produces in reverse. Sub-objects that
    /// have their own registered `Mapper` are emitted as nested maps; scalars
    /// (`String`, boxed primitives, `null`) go in as-is.
    Map<String, Object> toMap(T instance);

    /// Inverse of `#toMap`. Receives a `Map<String, Object>` as produced by
    /// `JSONParser` and populates a fresh `T`.
    T fromMap(Map<String, Object> map);

    /// XML root element name (`@XmlRoot.value`, falling back to the class
    /// simple name with a lowercase first character).
    String xmlRootName();

    /// Serializes `instance` into the given `Element`. Implementations append
    /// child elements / attributes; the root element is supplied by the caller
    /// so the same mapper can also be invoked from inside a parent element.
    void writeXml(T instance, Element root);

    /// Inverse of `#writeXml`. Receives an `Element` (the root that
    /// `Mappers.toXml` produced or that a parser delivered) and populates a
    /// fresh `T`.
    T readXml(Element root);
}

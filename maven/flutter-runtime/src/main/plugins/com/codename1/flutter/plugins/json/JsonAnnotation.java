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
package com.codename1.flutter.plugins.json;

import java.util.Map;

import dart.core.ArgumentError;
import dart.runtime.DartRuntime;

/// The helpers json_serializable's generated `*.g.dart` code calls from
/// json_annotation. The annotations themselves only drive code generation, which
/// has already run by the time an app is imported.
public final class JsonAnnotation {
    private JsonAnnotation() {
    }

    /// `$enumDecode(map, source, unknownValue: ...)`: the enum value whose JSON
    /// form is `source`.
    /// Typed by its result, not by the map: a generated `const` enum map is often
    /// inferred no further than `Map<Object, Object>` here, and its key type would
    /// then contradict the enum the caller assigns the result to.
    @SuppressWarnings("unchecked")
    public static <K> K $enumDecode(Map<?, ?> enumValues, Object source, Object unknownValue) {
        if (source == null) {
            throw new ArgumentError("A value must be provided. Supported values: " + supported(enumValues));
        }
        for (Map.Entry<?, ?> e : enumValues.entrySet()) {
            if (DartRuntime.eq(e.getValue(), source)) {
                return (K) e.getKey();
            }
        }
        if (unknownValue != null) {
            return (K) unknownValue;
        }
        throw new ArgumentError("`" + DartRuntime.str(source) + "` is not one of the supported values: "
                + supported(enumValues));
    }

    private static String supported(Map<?, ?> enumValues) {
        StringBuilder sb = new StringBuilder();
        for (Object v : enumValues.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(DartRuntime.str(v));
        }
        return sb.toString();
    }

    public static <K> K $enumDecodeNullable(Map<?, ?> enumValues, Object source, Object unknownValue) {
        return source == null ? null : JsonAnnotation.<K>$enumDecode(enumValues, source, unknownValue);
    }
}

/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.annotations;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Holds the literal element values parsed off a single annotation occurrence.
///
/// Values follow ASM's `AnnotationVisitor` conventions:
/// - Primitives are boxed (`Integer`, `Boolean`, ...).
/// - Strings stay as `String`.
/// - Class literals become `org.objectweb.asm.Type` instances.
/// - Enum constants become `String[] { internalName, valueName }` pairs.
/// - Arrays become `java.util.List<Object>`.
/// - Nested annotations become further `AnnotationValues` instances.
///
/// The wrapper exposes a small set of typed getters so processors don't have to
/// know which form a given element comes back as. Missing keys return `null` —
/// callers that require a value should check for null and emit a clear error
/// message rather than NPE'ing on missing input.
public final class AnnotationValues {

    private final String descriptor;
    private final Map<String, Object> values;

    AnnotationValues(String descriptor, Map<String, Object> values) {
        this.descriptor = descriptor;
        // Hold the live reference: the ClassScanner instantiates this object up
        // front and then populates the map as ASM dispatches `visit()` callbacks.
        // Copying here would freeze an empty snapshot before the values arrive.
        this.values = (values == null) ? new LinkedHashMap<String, Object>() : values;
    }

    /// The annotation's JVM internal descriptor, e.g. `Lcom/codename1/annotations/Route;`.
    public String getDescriptor() { return descriptor; }

    /// Returns the raw value for the named element, or null. The returned object
    /// follows the ASM conventions documented in this class.
    public Object get(String name) { return values.get(name); }

    /// Returns the value as a String, or null if absent or not a string. Use
    /// `#getStringOrDefault` when you want a typed fallback.
    public String getString(String name) {
        Object v = values.get(name);
        return (v instanceof String) ? (String) v : null;
    }

    /// Returns the string value, or `defaultValue` if absent.
    public String getStringOrDefault(String name, String defaultValue) {
        String s = getString(name);
        return s == null ? defaultValue : s;
    }

    /// Returns the int value, or `defaultValue` if absent. Accepts any boxed
    /// `Number`, narrowing via `intValue()`.
    public int getIntOrDefault(String name, int defaultValue) {
        Object v = values.get(name);
        if (v instanceof Number) return ((Number) v).intValue();
        return defaultValue;
    }

    /// Returns the boolean value, or `defaultValue` if absent.
    public boolean getBoolOrDefault(String name, boolean defaultValue) {
        Object v = values.get(name);
        return (v instanceof Boolean) ? ((Boolean) v).booleanValue() : defaultValue;
    }

    /// Unmodifiable view of all element values in declaration order.
    public Map<String, Object> all() { return Collections.unmodifiableMap(values); }

    @Override
    public String toString() {
        return "@" + descriptor + values;
    }
}

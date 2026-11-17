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
package com.codename1.backend.graphql;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/// What a [DataFetcher] is given: the parent value, the field's arguments and
/// the request's context.
///
/// Arguments are already coerced to the schema's types: an `Int` is an
/// `Integer`, a `Float` a `Double`, an `ID` a `String`, an enum the value's
/// name, an input object a `Map` and a list a `List`. Use
/// [com.codename1.backend.Json#convert(Object, Class)] to read a map into a
/// class of the application's own.
public final class DataFetchingEnvironment {
    private final Object source;
    private final Map<String, Object> arguments;
    private final Object context;
    private final String fieldName;
    private final String parentTypeName;
    private final List<Object> path;
    private final Map<String, Object> variables;

    DataFetchingEnvironment(Object source, Map<String, Object> arguments, Object context,
                            String fieldName, String parentTypeName, List<Object> path,
                            Map<String, Object> variables) {
        this.source = source;
        this.arguments = arguments;
        this.context = context;
        this.fieldName = fieldName;
        this.parentTypeName = parentTypeName;
        this.path = path;
        this.variables = variables;
    }

    /// The parent value, from which this field is resolved; null on a root
    /// field unless the execution was given a root value.
    public Object getSource() {
        return source;
    }

    /// One argument, or null when it was neither given nor defaulted.
    public Object getArgument(String name) {
        return arguments.get(name);
    }

    /// Whether the client gave the argument -- as null too -- or the schema
    /// defaulted it.
    public boolean containsArgument(String name) {
        return arguments.containsKey(name);
    }

    /// Every argument, by name.
    public Map<String, Object> getArguments() {
        return Collections.unmodifiableMap(arguments);
    }

    /// One argument as its JSON tree -- whole numbers as `Long`, objects as
    /// maps -- the form the generated JSON codecs read a class of the
    /// application's own from. Null when the argument is absent.
    public Object getArgumentAsJson(String name) {
        return asJson(arguments.get(name));
    }

    /// Every argument as one JSON object tree; see [#getArgumentAsJson].
    public Object getArgumentsAsJson() {
        return asJson(arguments);
    }

    private static Object asJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return com.codename1.backend.Json.parse(com.codename1.backend.Json.write(value));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException(e.getMessage()); // NOPMD PreserveStackTrace - a value Json just wrote cannot fail to parse
        }
    }

    /// The context the request was executed with: the `HttpServer.Request`
    /// when it came over HTTP or WebSocket, unless the program supplied its own.
    public Object getContext() {
        return context;
    }

    /// The field's name in the schema, not its alias.
    public String getFieldName() {
        return fieldName;
    }

    /// The name of the object type the field belongs to.
    public String getParentTypeName() {
        return parentTypeName;
    }

    /// Where the field is in the response, as names and list indexes.
    public List<Object> getExecutionPath() {
        return Collections.unmodifiableList(path);
    }

    /// The operation's coerced variables.
    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }
}

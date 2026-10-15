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
package com.codename1.impl.orm;

import com.codename1.orm.session.JpqlQuery;
import com.codename1.orm.session.PersistenceException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Scalar native SQL using the session's connection and failure handling.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class NativeQueryImpl<T> implements JpqlQuery<T> {
    private final SessionImpl session;
    private final String sql;
    private final boolean mutation;
    private final int kind;
    private final Class<T> type;
    private final List<String> names = new ArrayList<String>();
    private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    private int maximum = -1;
    private int skip;

    NativeQueryImpl(SessionImpl session, String statement, Class<T> type) {
        if (statement == null || type == null) {
            throw new IllegalArgumentException("statement/resultType is null");
        }
        this.session = session;
        this.type = type;
        String start = statement.trim();
        mutation = keyword(start, "UPDATE") || keyword(start, "DELETE");
        if (!mutation && !keyword(start, "SELECT")) {
            throw new IllegalArgumentException("Native query must start with SELECT, UPDATE or DELETE followed by whitespace");
        }
        kind = type == String.class ? Attribute.TEXT : type == Long.class || type == Object.class && mutation ? Attribute.BIGINT
                : type == Integer.class ? Attribute.INTEGER : type == Double.class ? Attribute.REAL
                : type == Boolean.class ? Attribute.BOOLEAN : type == byte[].class ? Attribute.BLOB : -1;
        if (kind < 0) {
            throw new IllegalArgumentException("Native selects support String, Long, Integer, Double, Boolean or byte[] scalars");
        }
        StringBuilder output = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (c == '\\') {
                throw new IllegalArgumentException("Bind backslashes as parameter values instead of SQL literals");
            }
            if (quote != 0) {
                output.append(c);
                if (c == quote) {
                    if (i + 1 < statement.length() && statement.charAt(i + 1) == quote) {
                        output.append(statement.charAt(++i));
                    } else {
                        quote = 0;
                    }
                }
            } else if (c == '\'' || c == '"') {
                quote = c; output.append(c);
            } else if (c == ':' && i + 1 < statement.length() && statement.charAt(i + 1) == ':') {
                output.append("::"); i++;
            } else if (c == ':') {
                int begin = ++i;
                if (i >= statement.length() || !Character.isJavaIdentifierStart(statement.charAt(i))) {
                    throw new IllegalArgumentException("Expected native parameter name after colon");
                }
                while (i < statement.length() && Character.isJavaIdentifierPart(statement.charAt(i))) {
                    i++;
                }
                names.add(statement.substring(begin, i--)); output.append('?');
            } else {
                if (c == ';' || c == '?' || c == '$' || c == '`'
                        || c == '-' && i + 1 < statement.length() && statement.charAt(i + 1) == '-'
                        || c == '/' && i + 1 < statement.length() && statement.charAt(i + 1) == '*') {
                    throw new IllegalArgumentException("Native queries require one statement, named parameters, standard quotes and no comments");
                }
                output.append(c);
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("Unterminated native SQL quote");
        }
        sql = output.toString();
    }
    private static boolean keyword(String sql, String word) {
        return sql.length() > word.length() && sql.substring(0, word.length()).equalsIgnoreCase(word)
                && Character.isWhitespace(sql.charAt(word.length()));
    }
    @Override
    public JpqlQuery<T> setParameter(String name, Object value) {
        if (!names.contains(name)) {
            throw new IllegalArgumentException("Unknown named parameter: " + name);
        }
        if (value instanceof Iterable || value != null && value.getClass().isArray() && !(value instanceof byte[])) {
            throw new IllegalArgumentException("Native parameters must be scalars or byte[] BLOBs");
        }
        parameters.put(name, value); return this;
    }
    @Override
    public JpqlQuery<T> limit(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative limit");
        }
        maximum = value; return this;
    }
    @Override
    public JpqlQuery<T> offset(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative offset");
        }
        skip = value; return this;
    }
    private Object[] arguments() {
        SessionImpl.checkParameterCount(names.size());
        Object[] result = new Object[names.size()];
        for (int i = 0; i < result.length; i++) {
            String name = names.get(i);
            if (!parameters.containsKey(name)) {
                throw new IllegalArgumentException("Unbound named parameter: " + name);
            }
            result[i] = Values.storage(parameters.get(name));
        }
        return result;
    }
    @Override
    public List<T> list() {
        return list(maximum);
    }
    @Override
    public T first() {
        List<T> rows = list(maximum == 0 ? 0 : 1);
        return rows.isEmpty() ? null : rows.get(0);
    }
    private List<T> list(int max) {
        if (mutation) {
            throw new IllegalStateException("Use executeUpdate for bulk mutations");
        }
        Object[] args = arguments();
        session.autoFlush();
        List<T> result = new ArrayList<T>();
        if (max == 0) {
            return result;
        }
        for (Object[] row : session.read(sql + session.limit(max, skip), args, new int[]{SqlAccess.NATIVE_SCALAR})) {
            Object value = row[0];
            try {
                if (kind == Attribute.REAL && value instanceof Number) {
                    value = Double.valueOf(((Number) value).doubleValue());
                } else if (kind == Attribute.BOOLEAN) {
                    value = Values.asBooleanObject(value);
                } else if (kind == Attribute.INTEGER) {
                    value = Values.asIntObject(value);
                } else if (kind == Attribute.BIGINT) {
                    value = Values.asLongObject(value);
                }
            } catch (java.io.IOException error) {
                throw new PersistenceException("Invalid native scalar projection", error);
            }
            if (value != null && !type.isInstance(value)) {
                throw new PersistenceException("Native scalar does not match " + type.getName());
            }
            result.add((T) value);
        }
        return result;
    }
    @Override
    public int executeUpdate() {
        if (!mutation) {
            throw new IllegalStateException("Not a bulk mutation");
        }
        if (maximum >= 0 || skip > 0) {
            throw new IllegalArgumentException("Bulk mutation pagination is not supported");
        }
        Object[] args = arguments();
        session.requireTransaction();
        session.flush();
        // Like JPQL, the session is left as it is; see JpqlQueryImpl.executeUpdate.
        return session.write(sql, args);
    }
}

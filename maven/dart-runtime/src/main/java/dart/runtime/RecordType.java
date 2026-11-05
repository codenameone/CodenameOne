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
package dart.runtime;

/// Dart's runtimeType of a record: the record type of its fields' own runtime types,
/// printed as Dart prints it, `(Null, int, {Null x})`, and equal to another of the same
/// shape and field types.
public final class RecordType {
    private final int positional;
    private final String[] named;
    private final Object[] fields;

    /// {@code fields} holds the positional fields' types, then the named ones' in the
    /// order of {@code named} (sorted, as the record class declares them).
    public RecordType(int positional, String[] named, Object... fields) {
        this.positional = positional;
        this.named = named;
        this.fields = fields;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (i == positional) {
                sb.append('{');
            }
            sb.append(DartRuntime.str(fields[i]));
            if (i >= positional) {
                sb.append(' ').append(named[i - positional]);
            }
        }
        if (fields.length > positional) {
            sb.append('}');
        }
        return sb.append(')').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RecordType)) {
            return false;
        }
        RecordType r = (RecordType) o;
        return positional == r.positional && java.util.Arrays.equals(named, r.named)
                && java.util.Arrays.equals(fields, r.fields);
    }

    @Override
    public int hashCode() {
        return (positional * 31 + java.util.Arrays.hashCode(named)) * 31 + java.util.Arrays.hashCode(fields);
    }
}

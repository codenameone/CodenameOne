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
package com.codename1.unitycompat.system;

/// `System.Exception`, and the root of everything translated C# can throw or
/// catch. It is a `RuntimeException` because C# has no checked exceptions.
///
/// Exceptions the VM raises on its own -- a null dereference, an array index
/// out of range -- are not of this type; [Interop#wrap] turns them into the
/// .NET exception a C# `catch` expects before any clause is tested.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Exception extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String message;
    private final Exception inner;

    public Exception() {
        this(null, null);
    }

    public Exception(String message) {
        this(message, null);
    }

    public Exception(String message, Exception inner) {
        super(message);
        this.message = message;
        this.inner = inner;
    }

    public String get_Message() {
        return message != null ? message : "Exception of type '" + typeName() + "' was thrown.";
    }

    public Exception get_InnerException() {
        return inner;
    }

    @Override
    public String getMessage() {
        return get_Message();
    }

    /// `Exception.ToString()`: the .NET type name, the message after a
    /// colon when there is one, and the inner exception chain. .NET also
    /// appends the stack trace of a thrown exception. That is left out on
    /// purpose: the frames here are Java methods of translated classes, and
    /// a trace in .NET's format would name lines the C# source never had.
    @Override
    public String toString() {
        String m = get_Message();
        String s = m.length() == 0 ? typeName() : typeName() + ": " + m;
        if (inner != null) {
            s = s + "\n ---> " + inner.toString() + "\n   --- End of inner exception stack trace ---";
        }
        return s;
    }

    /// The C# name of this exception's type. A runtime class lives under
    /// `com.codename1.unitycompat` with its namespace folded to lower case
    /// (`system.collections.generic`), and a translated one keeps its
    /// namespace as it is, or sits in `global` when it declared none. A
    /// nested class's `$` is .NET's `+`.
    String typeName() {
        String n = getClass().getName().replace('$', '+');
        String runtime = "com.codename1.unitycompat.";
        if (n.startsWith(runtime)) {
            StringBuilder sb = new StringBuilder();
            String rest = n.substring(runtime.length());
            int last = rest.lastIndexOf('.');
            int start = 0;
            while (start <= last) {
                int dot = rest.indexOf('.', start);
                String part = rest.substring(start, dot);
                if ("io".equals(part)) {
                    sb.append("IO");
                } else {
                    sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
                sb.append('.');
                start = dot + 1;
            }
            return sb.append(rest.substring(last + 1)).toString();
        }
        return n.startsWith("global.") ? n.substring("global.".length()) : n;
    }
}

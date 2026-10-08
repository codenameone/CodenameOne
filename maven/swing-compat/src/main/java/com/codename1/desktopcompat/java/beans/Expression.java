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
package com.codename1.desktopcompat.java.beans;

import com.codename1.compat.jdk.LinkOnly;

/// A [Statement] that yields a value.
///
/// Long-term persistence writes an object as the calls that rebuild it and
/// finds those calls by reflection, so nothing of it can run on a device. The
/// class is [LinkOnly]: it exists for the libraries that offer to save
/// themselves this way beside everything else they do, and an application's
/// own use of it is reported at build time.
///
/// A value given to the constructor or to [#setValue(Object)] is answered by
/// [#getValue()]; one that would have to be computed by making the call is
/// not, and `getValue` throws `UnsupportedOperationException`.
@LinkOnly
public class Expression extends Statement {

    private Object value;
    private boolean valueSet;

    public Expression(Object target, String methodName, Object[] arguments) {
        super(target, methodName, arguments);
    }

    public Expression(Object value, Object target, String methodName, Object[] arguments) {
        super(target, methodName, arguments);
        setValue(value);
    }

    public Object getValue() throws Exception {
        if (!valueSet) {
            execute();
        }
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
        this.valueSet = true;
    }
}

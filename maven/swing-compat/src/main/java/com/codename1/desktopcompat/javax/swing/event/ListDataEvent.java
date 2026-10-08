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
package com.codename1.desktopcompat.javax.swing.event;

import java.util.EventObject;

/// A range of a list model changed. The two indices are inclusive and in
/// ascending order whichever way round they were given.
public class ListDataEvent extends EventObject {

    public static final int CONTENTS_CHANGED = 0;

    public static final int INTERVAL_ADDED = 1;

    public static final int INTERVAL_REMOVED = 2;

    private final int type;
    private final int index0;
    private final int index1;

    public ListDataEvent(Object source, int type, int index0, int index1) {
        super(source);
        this.type = type;
        this.index0 = Math.min(index0, index1);
        this.index1 = Math.max(index0, index1);
    }

    public int getType() {
        return type;
    }

    public int getIndex0() {
        return index0;
    }

    public int getIndex1() {
        return index1;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[type=" + type + ",index0=" + index0 + ",index1=" + index1 + "]";
    }
}

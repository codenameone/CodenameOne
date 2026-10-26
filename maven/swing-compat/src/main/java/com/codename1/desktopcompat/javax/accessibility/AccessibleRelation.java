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
package com.codename1.desktopcompat.javax.accessibility;

/// A relation between the object that holds it and one or more targets,
/// such as "this label is the label for that field".
public class AccessibleRelation {

    public static final String LABEL_FOR = "labelFor";

    public static final String LABELED_BY = "labeledBy";

    public static final String MEMBER_OF = "memberOf";

    public static final String CONTROLLER_FOR = "controllerFor";

    public static final String CONTROLLED_BY = "controlledBy";

    public static final String FLOWS_TO = "flowsTo";

    public static final String FLOWS_FROM = "flowsFrom";

    public static final String SUBWINDOW_OF = "subwindowOf";

    public static final String PARENT_WINDOW_OF = "parentWindowOf";

    public static final String EMBEDS = "embeds";

    public static final String EMBEDDED_BY = "embeddedBy";

    public static final String CHILD_NODE_OF = "childNodeOf";

    public static final String LABEL_FOR_PROPERTY = "labelForProperty";

    public static final String LABELED_BY_PROPERTY = "labeledByProperty";

    public static final String MEMBER_OF_PROPERTY = "memberOfProperty";

    public static final String CONTROLLER_FOR_PROPERTY = "controllerForProperty";

    public static final String CONTROLLED_BY_PROPERTY = "controlledByProperty";

    private final String relationKey;

    private Object[] target = new Object[0];

    public AccessibleRelation(String key) {
        relationKey = key;
    }

    public AccessibleRelation(String key, Object target) {
        relationKey = key;
        this.target = new Object[] {target};
    }

    public AccessibleRelation(String key, Object[] target) {
        relationKey = key;
        setTarget(target);
    }

    public String getKey() {
        return relationKey;
    }

    /// A copy of the targets.
    public Object[] getTarget() {
        Object[] copy = new Object[target.length];
        System.arraycopy(target, 0, copy, 0, copy.length);
        return copy;
    }

    public void setTarget(Object target) {
        this.target = new Object[] {target};
    }

    public void setTarget(Object[] target) {
        Object[] from = target == null ? new Object[0] : target;
        Object[] copy = new Object[from.length];
        System.arraycopy(from, 0, copy, 0, copy.length);
        this.target = copy;
    }

    @Override
    public String toString() {
        return relationKey;
    }
}

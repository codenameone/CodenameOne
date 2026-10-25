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

import java.util.Vector;

/// The relations of one object, at most one of each kind: adding a
/// relation of a kind that is there already adds its targets to the one
/// that is.
public class AccessibleRelationSet {

    protected Vector<AccessibleRelation> relations;

    public AccessibleRelationSet() {
        relations = null;
    }

    public AccessibleRelationSet(AccessibleRelation[] relations) {
        if (relations.length != 0) {
            this.relations = new Vector<AccessibleRelation>(relations.length);
            for (int i = 0; i < relations.length; i++) {
                add(relations[i]);
            }
        }
    }

    public boolean add(AccessibleRelation relation) {
        if (relations == null) {
            relations = new Vector<AccessibleRelation>();
        }
        AccessibleRelation existing = get(relation.getKey());
        if (existing == null) {
            relations.addElement(relation);
            return true;
        }
        Object[] first = existing.getTarget();
        Object[] second = relation.getTarget();
        Object[] merged = new Object[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        existing.setTarget(merged);
        return true;
    }

    public void addAll(AccessibleRelation[] relations) {
        for (int i = 0; i < relations.length; i++) {
            add(relations[i]);
        }
    }

    public boolean remove(AccessibleRelation relation) {
        return relations != null && relations.removeElement(relation);
    }

    public void clear() {
        if (relations != null) {
            relations.removeAllElements();
        }
    }

    public int size() {
        return relations == null ? 0 : relations.size();
    }

    public boolean contains(String key) {
        return get(key) != null;
    }

    public AccessibleRelation get(String key) {
        if (relations == null || key == null) {
            return null;
        }
        for (int i = 0; i < relations.size(); i++) {
            AccessibleRelation r = relations.elementAt(i);
            if (key.equals(r.getKey())) {
                return r;
            }
        }
        return null;
    }

    public AccessibleRelation[] toArray() {
        AccessibleRelation[] all = new AccessibleRelation[size()];
        for (int i = 0; i < all.length; i++) {
            all[i] = relations.elementAt(i);
        }
        return all;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(relations.elementAt(i).getKey());
        }
        return sb.toString();
    }
}

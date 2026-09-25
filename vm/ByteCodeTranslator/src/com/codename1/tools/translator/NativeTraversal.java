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
package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.bytecodes.Instruction;
import java.util.ArrayList;
import java.util.List;

/**
 * A validated traversal's native representation: one rooted owner and primitive
 * cursor, last-returned slot, modification snapshot and optional layout selector.
 * Layout selection happens once. Unknown/subclass receivers keep their iterator.
 */
final class NativeTraversal {
    /** A stack consumer, deliberately not an AssignableExpression. */
    static final class StackBegin extends Instruction {
        private final String code;
        private final List<String> dependencies;
        StackBegin(String code, List<String> dependencies) {
            super(-1);
            this.code = code;
            this.dependencies = dependencies;
        }
        @Override public void appendInstruction(StringBuilder out) { out.append(code); }
        @Override public void addDependencies(List<String> out) { out.addAll(dependencies); }
    }
    private enum Layout {
        /* SET and HASH_SET are both java.util.HashSet and that is deliberate: they are
         * two different RECEIVERS, and the distinction is which object the emitted field
         * reads come from.
         *
         * SET is a map-backed view -- a HashMap's keySet() or values() -- where root is
         * assigned the MAP and every field read is a HashMap field. HASH_SET is a plain
         * HashSet iterated directly, where root is the SET and the fields are its own
         * keys+meta table. They cannot share a mode id for exactly that reason: next()
         * and remove() pick their field owner from the id at run time, and reading
         * HashMap fields off a HashSet would be a wrong-offset load, not a miss.
         *
         * A plain HashSet stopped renting a HashMap, which is why HASH_SET exists at
         * all. Until it did, the dispatch loop skipped SET for a direct receiver and
         * every for-each over a HashSet allocated a real iterator: 517,043 of them over
         * a translation of the 5,326-class corpus. */
        ARRAY(1, "java_util_ArrayList"), SET(2, "java_util_HashSet"), ORDERED_SET(3, "java_util_LinkedHashSet"),
        IDENTITY(4, "java_util_IdentityHashMap"), HASH_SET(5, "java_util_HashSet");
        final int id;
        final String type;
        Layout(int id, String type) { this.id = id; this.type = type; }
    }
    private final List<Layout> layouts = new ArrayList<Layout>(3);
    private final Layout exact;
    private final boolean keyView, valueView, identityKeys, identityValues, mapReceiver, mapValues;
    private final boolean unwrapSet;
    private final String root, index, last, expected, mode, buffer, nullObject, keptOwner;

    NativeTraversal(Invoke invoke, Invoke mapView, int id, int slot, boolean keepOwner) {
        keptOwner = keepOwner ? "__feOwner_" + id : null;
        Layout proven = null;
        mapReceiver = mapView != null;
        mapValues = mapReceiver && "values".equals(mapView.getName());
        String owner = invoke.getOwner().replace('/', '_');
        // Bytecode verification establishes this assignability even when the
        // concrete List implementation is unknown. None of our set/map views
        // implement List, so they cannot be alternatives at such a call site.
        boolean listOnly = !mapReceiver && ("java_util_List".equals(owner)
                || "java_util_ArrayList".equals(owner) || "java_util_AbstractList".equals(owner)
                || "java_util_AbstractSequentialList".equals(owner) || "java_util_LinkedList".equals(owner)
                || "java_util_Vector".equals(owner) || "java_util_Stack".equals(owner));
        unwrapSet = !listOnly && Parser.getClassObject("java_util_Collections_SetFromMap") != null;
        for (Layout layout : Layout.values()) {
            if (listOnly && layout != Layout.ARRAY) continue;
            if (Parser.getClassObject(layout.type) != null) layouts.add(layout);
            // Not a direct HashSet receiver: see the dispatch loop in setup().
            if (layout != Layout.SET && invoke.hasExactReceiver(layout.type)) proven = layout;
        }
        if (mapReceiver) proven = mapView.hasExactReceiver("java_util_IdentityHashMap") ? Layout.IDENTITY
                : mapView.hasExactReceiver("java_util_LinkedHashMap") ? Layout.ORDERED_SET : Layout.SET;
        // An exact fact itself establishes reachability even in isolated IR tests.
        if (proven != null && !layouts.contains(proven)) layouts.add(proven);
        exact = proven;
        keyView = !listOnly && Parser.getClassObject("java_util_HashMap_KeySet") != null;
        valueView = !listOnly && Parser.getClassObject("java_util_HashMap_Values") != null;
        if ((keyView || valueView) && !layouts.contains(Layout.SET)) layouts.add(Layout.SET);
        if ((keyView || valueView) && Parser.getClassObject("java_util_LinkedHashMap") != null
                && !layouts.contains(Layout.ORDERED_SET)) layouts.add(Layout.ORDERED_SET);
        identityKeys = !listOnly && Parser.getClassObject("java_util_IdentityHashMap_KeySet") != null;
        identityValues = !listOnly && Parser.getClassObject("java_util_IdentityHashMap_Values") != null;
        nullObject = "__feNull_" + id;
        root = "locals[" + slot + "].data.o";
        index = "__feIdx_" + id; last = "__feLast_" + id; expected = "__feMod_" + id;
        mode = "__feFast_" + id; buffer = "__feBuf_" + id;
    }
    boolean isDirect() { return exact != null; }

    List<String> dependencies() {
        List<String> result = new ArrayList<String>();
        result.add("java_util_ConcurrentModificationException");
        result.add("java_lang_IllegalStateException");
        result.add("java_lang_NullPointerException");
        result.add("java_util_Iterator");
        for (Layout layout : layouts) {
            if (Parser.getClassObject(layout.type) != null) result.add(layout.type);
            if (layout == Layout.ARRAY) result.add("java_util_AbstractList");
            else if (layout != Layout.IDENTITY) {
                if (Parser.getClassObject("java_util_HashSet") != null) result.add("java_util_HashSet");
                result.add("java_util_HashMap");
            }
            if (layout == Layout.ORDERED_SET) result.add("java_util_LinkedHashMap");
        }
        if (keyView) result.add("java_util_HashMap_KeySet");
        if (valueView) result.add("java_util_HashMap_Values");
        if (identityKeys) result.add("java_util_IdentityHashMap_KeySet");
        if (identityValues) result.add("java_util_IdentityHashMap_Values");
        if (unwrapSet) result.add("java_util_Collections_SetFromMap");
        return result;
    }
    private String field(String owner, String field, String object) {
        return "get_field_" + owner + "_" + field + "(" + object + ")";
    }
    private String map(String field) {
        // A map's values and metadata are parts of its keys table, not fields of their own.
        if (field.equals("cn1ValsBlock")) return part(1);
        if (field.equals("cn1MetaBlock")) return part(2);
        return field("java_util_HashMap", field, root);
    }
    /* Part `index` of the map's table: 1 values, 2 metadata, 3 and 4 the ordered links. */
    private String part(int index) {
        return "cn1TablePart(" + field("java_util_HashMap", "cn1KeysBlock", root) + ", " + index + ")";
    }
    /* The table fields for a layout. Identical names on both classes, different owner:
     * HASH_SET reads the HashSet's own table, everything else reads a map's. */
    private String table(Layout layout, String field) {
        if (layout == Layout.HASH_SET && field.equals("cn1MetaBlock")) {
            // A set's markers follow its elements in the same allocation.
            return "cn1SetTableMeta(" + field("java_util_HashSet", "cn1KeysBlock", root) + ")";
        }
        return layout == Layout.HASH_SET ? field("java_util_HashSet", field, root) : map(field);
    }
    private String modification(Layout layout) {
        return layout == Layout.ARRAY ? field("java_util_AbstractList", "modCount", root)
                : layout == Layout.IDENTITY ? field("java_util_IdentityHashMap", "modCount", root)
                : layout == Layout.HASH_SET ? field("java_util_HashSet", "cn1ModCount", root)
                : map("modCount");
    }
    private String check(Layout layout) {
        return "if(" + expected + " != " + modification(layout) + ") CN1_THROW_CME();\n";
    }
    private String start(Layout layout) {
        // The only receiver that reaches the field read is a LinkedHashSet, and the
        // field lives on LinkedHashSet now rather than on HashSet -- a plain HashSet
        // owns its table directly and carries no such field. Layout.SET is excluded
        // from `proven` above and skipped in the dispatch loop, and a map receiver
        // uses __c itself, so nothing else can land here.
        String owner = mapReceiver || layout == Layout.ARRAY || layout == Layout.HASH_SET ? "__c"
                : field("java_util_LinkedHashSet", "backingMap", "__c");
        String first = layout == Layout.ARRAY || layout == Layout.IDENTITY ? "0" : layout == Layout.ORDERED_SET
                ? field("java_util_LinkedHashMap", "cn1Head", root)
                : "cn1InlTableNext(" + table(layout, "cn1MetaBlock") + ", 0, " + table(layout, "cn1Cap") + ")";
        return root + " = " + owner + ";\n" + index + " = " + first + ";\n"
                + expected + " = " + modification(layout) + ";\n"
                + (layout == Layout.IDENTITY ? nullObject + " = get_static_java_util_IdentityHashMap_NULL_OBJECT();\n" : "");
    }
    String setup(String receiver, String iteratorCall, int slot) {
        StringBuilder code = new StringBuilder("    { JAVA_OBJECT __c = JAVA_NULL;\n");
        code.append(receiver).append("\nif(__c == JAVA_NULL) { cn1ThrowNullPointerHere(threadStateData); }\n");
        code.append(last).append(" = -1;\n");
        if (exact != null) code.append(start(exact));
        else {
            code.append(mode).append(" = 0;\nJAVA_OBJECT __original = __c;\n");
            if (unwrapSet) {
                code.append("if(CN1_OBJ_CLASS(__c) == &class__java_util_Collections_SetFromMap) __c = get_field_java_util_Collections_SetFromMap_backingSet(__c);\nif(__c == JAVA_NULL) { cn1ThrowNullPointerHere(threadStateData); }\n");
            }
            for (Layout layout : layouts) {
                if (layout == Layout.IDENTITY || Parser.getClassObject(layout.type) == null) continue;
                // A plain HashSet has no backingMap and start(Layout.SET) walks one.
                // It owns a keys+meta table with no values array, so this receiver takes
                // a real iterator. LinkedHashSet keeps its fast path (it does delegate to
                // a LinkedHashMap), as do keySet()/values() views via viewSetup().
                if (layout == Layout.SET) continue;
                code.append("if(CN1_OBJ_CLASS(__c) == &class__").append(layout.type)
                        .append(") {\n").append(mode).append(" = ").append(layout.id).append(";\n")
                        .append(start(layout)).append("}\n");
            }
            if (keyView) code.append(viewSetup("java_util_HashMap_KeySet", false));
            if (valueView) code.append(viewSetup("java_util_HashMap_Values", true));
            if (identityKeys) code.append(viewSetup("java_util_IdentityHashMap_KeySet", false));
            if (identityValues) code.append(viewSetup("java_util_IdentityHashMap_Values", true));
            code.append("if(").append(mode).append(" == 0) { cn1IterScopeBegin(threadStateData, ").append(buffer).append(");\n")
                    .append(root).append(" = ").append(iteratorCall).append("(threadStateData, __original);\n")
                    .append("cn1IterScopeEnd(threadStateData); }\n");
        }
        if (keptOwner != null) code.append(keptOwner).append(" = ").append(root).append(";\n");
        return code.append("locals[").append(slot).append("].type = CN1_TYPE_OBJECT; }\n").toString();
    }
    private String viewSetup(String viewClass, boolean values) {
        StringBuilder code = new StringBuilder("if(CN1_OBJ_CLASS(__c) == &class__");
        code.append(viewClass).append(") {\n").append(root).append(" = ")
                .append(field(viewClass, "map", "__c")).append(";\n");
        for (Layout layout : layouts) {
            // HASH_SET is a receiver, not a view backing: a keySet() or values() view is
            // always backed by a MAP, so a plain HashSet cannot be what stands behind one.
            // Letting it through here would emit a LinkedHashMap class test that sets the
            // HASH_SET mode, and next() would then read HashSet fields off a map.
            if (layout == Layout.ARRAY || layout == Layout.HASH_SET
                    || viewClass.startsWith("java_util_IdentityHashMap") != (layout == Layout.IDENTITY)) continue;
            String mapClass = layout == Layout.IDENTITY ? "java_util_IdentityHashMap"
                    : layout == Layout.SET ? "java_util_HashMap" : "java_util_LinkedHashMap";
            code.append("if(CN1_OBJ_CLASS(").append(root).append(") == &class__")
                    .append(mapClass).append(") {\n").append(mode).append(" = ").append(layout.id + (values ? 8 : 0)).append(";\n")
                    .append(index).append(" = ").append(layout == Layout.IDENTITY ? "0" : layout == Layout.SET
                            ? "cn1InlTableNext(" + map("cn1MetaBlock") + ", 0, " + map("cn1Cap") + ")"
                            : field("java_util_LinkedHashMap", "cn1Head", root)).append(";\n")
                    .append(expected).append(" = ").append(modification(layout)).append(";\n");
            if (layout == Layout.IDENTITY) code.append(nullObject).append(" = get_static_java_util_IdentityHashMap_NULL_OBJECT();\n");
            code.append("}\n");
        }
        return code.append("}\n").toString();
    }

    private String has(Layout layout) {
        if (layout == Layout.IDENTITY) return "({ JAVA_LONG __b = " + field("java_util_IdentityHashMap", "elementData", root)
                + "; " + index + " = cn1InlIdentityNext(__b, " + index + "); " + index + " < cn1RefBlockCount(__b); })";
        return layout == Layout.ARRAY ? "(" + index + " != " + field("java_util_ArrayList", "size", root) + ")"
                : "(" + index + " >= 0)";
    }
    String hasNext() {
        String expression = "virtual_java_util_Iterator_hasNext___R_boolean(threadStateData, " + root + ")";
        if (exact != null) expression = has(exact);
        else for (int i = layouts.size() - 1; i >= 0; i--) {
            Layout layout = layouts.get(i);
            expression = "(" + mode + " & 7) == " + layout.id + " ? " + has(layout) + " : " + expression;
        }
        return "    PUSH_INT(" + expression + ");\n";
    }
    private String next(Layout layout) {
        if (layout == Layout.IDENTITY) {
            String values = exact != null ? (mapValues ? "1" : "0") : "(" + mode + " & 8 ? 1 : 0)";
            return "({ " + check(layout) + last + " = " + index + "; " + index + " += 2; JAVA_OBJECT __v = cn1RefBlockGet("
                    + field("java_util_IdentityHashMap", "elementData", root) + ", " + last + " + " + values
                    + "); __v == " + nullObject + " ? JAVA_NULL : __v; })";
        }
        if (layout == Layout.ARRAY) return "(" + expected + " != " + modification(layout)
                + " ? (CN1_THROW_CME(), JAVA_NULL) : cn1RefBlockGet("
                + field("java_util_ArrayList", "cn1Storage", root) + ", " + last + " = " + index + "++))";
        String advance = layout == Layout.ORDERED_SET
                ? "cn1IntBlockGet(" + part(4) + ", " + index + ")"
                : "cn1InlTableNext(" + table(layout, "cn1MetaBlock") + ", " + index + " + 1, "
                        + table(layout, "cn1Cap") + ")";
        // A plain HashSet has keys only, so the values half of the mode never applies
        // to it -- it has no values part to name.
        String slots = layout == Layout.HASH_SET ? table(layout, "cn1KeysBlock")
                : exact != null ? map(mapValues ? "cn1ValsBlock" : "cn1KeysBlock")
                : "(" + mode + " & 8 ? " + map("cn1ValsBlock") + " : " + map("cn1KeysBlock") + ")";
        return "({ " + check(layout) + last + " = " + index + "; " + index + " = " + advance
                + "; cn1RefBlockGet(" + slots + ", " + last + "); })";
    }
    String next() {
        String expression = "virtual_java_util_Iterator_next___R_java_lang_Object(threadStateData, " + root + ")";
        if (exact != null) expression = next(exact);
        else for (int i = layouts.size() - 1; i >= 0; i--) {
            Layout layout = layouts.get(i);
            expression = "(" + mode + " & 7) == " + layout.id + " ? " + next(layout) + " : " + expression;
        }
        return "    PUSH_POINTER(" + expression + ");\n";
    }
    private String set(String owner, String field, String value) {
        return "set_field_" + owner + "_" + field + "(" + value + ", " + root + ");\n";
    }
    private String remove(Layout layout) {
        String invalid = "if(" + last + " < 0) CN1_THROW_ISE();\n";
        StringBuilder code = new StringBuilder(layout == Layout.ARRAY ? invalid + check(layout) : check(layout) + invalid);
        if (layout == Layout.IDENTITY) {
            code.append("java_util_IdentityHashMap_remove___java_lang_Object_R_java_lang_Object(threadStateData, ")
                    .append(root).append(", cn1RefBlockGet(").append(field("java_util_IdentityHashMap", "elementData", root))
                    .append(", ").append(last).append("));\n").append(index).append(" = ").append(last).append(";\n");
        } else if (layout == Layout.ARRAY) {
            String size = field("java_util_ArrayList", "size", root);
            code.append("JAVA_INT __p = --").append(index).append(";\nJAVA_LONG __a = ")
                    .append(field("java_util_ArrayList", "cn1Storage", root)).append(";\n")
                    .append("cn1RefBlockMove(threadStateData, __a, __p + 1, __p, ").append(size).append(" - __p - 1);\n")
                    .append("cn1RefBlockClear(threadStateData, __a, ").append(size).append(" - 1, 1);\n")
                    .append(set("java_util_ArrayList", "size", size + " - 1"))
                    .append(set("java_util_AbstractList", "modCount", modification(layout) + " + 1"));
        } else {
            if (layout == Layout.ORDERED_SET) {
                String prev = part(3);
                String next = part(4);
                code.append("JAVA_INT __p = cn1IntBlockGet(").append(prev).append(", ").append(last).append(");\n")
                        .append("JAVA_INT __n = cn1IntBlockGet(").append(next).append(", ").append(last).append(");\n")
                        .append("if(__p >= 0) cn1IntBlockSet(").append(next).append(", __p, __n); else ")
                        .append(set("java_util_LinkedHashMap", "cn1Head", "__n"))
                        .append("if(__n >= 0) cn1IntBlockSet(").append(prev).append(", __n, __p); else ")
                        .append(set("java_util_LinkedHashMap", "cn1Tail", "__p"));
            }
            // 1 is META_TOMB: the slot stays occupied for probing but holds nothing.
            code.append("cn1IntBlockSet(").append(table(layout, "cn1MetaBlock")).append(", ").append(last).append(", 1);\n")
                    .append("cn1RefBlockSet(threadStateData, ").append(table(layout, "cn1KeysBlock")).append(", ").append(last).append(", JAVA_NULL);\n");
            if (layout == Layout.HASH_SET) {
                // A set stores no values, and its count and modCount are its own fields.
                code.append(set("java_util_HashSet", "cn1Size", field("java_util_HashSet", "cn1Size", root) + " - 1"))
                        .append(set("java_util_HashSet", "cn1ModCount", modification(layout) + " + 1"));
            } else {
                // Through the ROOT: a part has no header for cn1RefBlockSet's barrier to use.
                code.append("cn1TableRefSet(threadStateData, ").append(map("cn1KeysBlock")).append(", 1, ").append(last).append(", JAVA_NULL);\n")
                        .append(set("java_util_HashMap", "elementCount", map("elementCount") + " - 1"))
                        .append(set("java_util_HashMap", "modCount", modification(layout) + " + 1"));
            }
        }
        return code.append(last).append(" = -1; ").append(expected).append(" = ").append(modification(layout)).append(";\n").toString();
    }
    String remove() {
        if (exact != null) return "    {\n" + remove(exact) + "}\n";
        StringBuilder code = new StringBuilder();
        for (Layout layout : layouts) code.append("if((").append(mode).append(" & 7) == ").append(layout.id).append(") {\n")
                .append(remove(layout)).append("} else ");
        return code.append("{ virtual_java_util_Iterator_remove__(threadStateData, ").append(root).append("); }\n").toString();
    }
}

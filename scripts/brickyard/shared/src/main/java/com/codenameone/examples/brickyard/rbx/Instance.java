/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Interp;
import com.codenameone.examples.brickyard.luau.JavaFunction;
import com.codenameone.examples.brickyard.luau.LuaError;
import com.codenameone.examples.brickyard.luau.LuaTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A node of the data model. Stored properties live in a map that holds
/// only what differs from the class default, so a fresh `Part` is a handful
/// of fields however many properties its class declares.
public class Instance extends RbxType {
    final Engine engine;
    final ClassDb.Desc desc;
    private String name;
    private Instance parent;
    private final List<Instance> children = new ArrayList<Instance>();
    private final Map<String, Object> props = new HashMap<String, Object>();
    private Map<String, Signal> signals;
    private Map<String, Signal> propSignals;
    private Map<String, Signal> attrSignals;
    private LinkedHashMap<String, Object> attributes;
    private List<String> tags;
    private boolean destroyed;
    private boolean parentLocked;
    /// Host-side state a behaviour keeps (a tween's progress, a body's
    /// physics state); never visible to scripts.
    public Object host;
    /// Stable id for replication and place files.
    private final int id;

    protected Instance(Engine engine, ClassDb.Desc desc) {
        this.engine = engine;
        this.desc = desc;
        this.name = desc.name;
        this.id = engine.nextId();
    }

    /// A number unique to this instance within its engine, stable for its life.
    public int getId() {
        return id;
    }

    @Override
    public String typeName() {
        return "Instance";
    }

    public String getClassName() {
        return desc.name;
    }

    public ClassDb.Desc getDesc() {
        return desc;
    }

    public Engine getEngine() {
        return engine;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        if (!n.equals(name)) {
            name = n;
            changed("Name");
        }
    }

    public Instance getParent() {
        return parent;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public boolean isA(String cls) {
        return desc.isA(cls);
    }

    public List<Instance> getChildren() {
        return children;
    }

    // ---------------------------------------------------------------- properties

    /// A stored property, or its class default.
    public Object prop(String key) {
        Object v = props.get(key);
        if (v != null || props.containsKey(key)) {
            return v;
        }
        Object d = desc.defaultOf(key);
        return d == ClassDb.REF || d == ClassDb.MISSING ? null : d; // NOPMD CompareObjectsWithEquals - sentinels
    }

    public boolean hasProp(String key) {
        return desc.defaultOf(key) != ClassDb.MISSING; // NOPMD CompareObjectsWithEquals - sentinel
    }

    public double num(String key) {
        Object v = prop(key);
        return v instanceof Double ? ((Double) v).doubleValue() : 0;
    }

    public boolean bool(String key) {
        return Boolean.TRUE.equals(prop(key));
    }

    public String str(String key) {
        Object v = prop(key);
        return v instanceof String ? (String) v : "";
    }

    public Instance ref(String key) {
        Object v = prop(key);
        return v instanceof Instance ? (Instance) v : null;
    }

    /// Stores a property without type checks and fires change events when it
    /// differs. Host code (physics, layout) writes through here.
    public void put(String key, Object v) {
        Object old = prop(key);
        if (same(old, v)) {
            return;
        }
        props.put(key, v);
        changed(key);
    }

    /// Stores silently: no events, no behaviour hooks. For bulk loads.
    public void putQuiet(String key, Object v) {
        props.put(key, v);
    }

    /// The properties that differ from their defaults, in no fixed order.
    public Map<String, Object> storedProps() {
        return props;
    }

    private static boolean same(Object a, Object b) {
        if (a == b) { // NOPMD CompareObjectsWithEquals - fast identity path
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof RbxType) {
            return ((RbxType) a).eq(b);
        }
        return a.equals(b);
    }

    void changed(String key) {
        for (ClassDb.Desc d = desc; d != null; d = d.parent) {
            if (d.behavior != null) {
                d.behavior.changed(this, key);
            }
        }
        engine.propertyChanged(this, key);
        if (isA("ValueBase") && "Value".equals(key)) {
            fireEvent("Changed", prop("Value"));
        } else {
            fireEvent("Changed", key);
        }
        if (propSignals != null) {
            Signal s = propSignals.get(key);
            if (s != null) {
                s.fire();
            }
        }
    }

    /// Converts a script value for property `key`, or throws the engine's
    /// error for a wrong type.
    Object coerce(String key, Object v) {
        Object d = desc.defaultOf(key);
        if (d == ClassDb.REF) { // NOPMD CompareObjectsWithEquals - sentinel
            if (v == null || v instanceof Instance || v instanceof com.codenameone.examples.brickyard.luau.LuaFunction
                    || v instanceof RbxType) {
                return v;
            }
        } else if (d instanceof Double) {
            Double n = Interp.toNumber(v);
            if (n != null) {
                return n;
            }
        } else if (d instanceof Boolean) {
            if (v instanceof Boolean) {
                return v;
            }
        } else if (d instanceof String) {
            if (v instanceof String) {
                return v;
            }
            if (v instanceof Double) {
                return Interp.numberToString(((Double) v).doubleValue());
            }
        } else if (d instanceof Enums.EnumItem) {
            Enums.EnumType t = ((Enums.EnumItem) d).type;
            if (v instanceof Enums.EnumItem && ((Enums.EnumItem) v).type == t) { // NOPMD CompareObjectsWithEquals - enum type identity
                return v;
            }
            if (v instanceof String && t.byName((String) v) != null) {
                return t.byName((String) v);
            }
            if (v instanceof Double && t.byValue(((Double) v).intValue()) != null) {
                return t.byValue(((Double) v).intValue());
            }
        } else if (d != null && v != null && d.getClass() == v.getClass()) {
            return v;
        }
        throw LuaError.rt("Unable to assign property " + key + ". " + expected(d) + " expected, got "
                + Interp.typeName(v));
    }

    private static String expected(Object d) {
        if (d == ClassDb.REF) { // NOPMD CompareObjectsWithEquals - sentinel
            return "Instance";
        }
        if (d instanceof Double) {
            return "number";
        }
        if (d instanceof Boolean) {
            return "bool";
        }
        if (d instanceof String) {
            return "string";
        }
        if (d instanceof Enums.EnumItem) {
            return "EnumItem";
        }
        return d instanceof RbxType ? ((RbxType) d).typeName() : "value";
    }

    // ---------------------------------------------------------------- tree

    public Instance findFirstChild(String n) {
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance c = children.get(i);
            if (c.name.equals(n)) {
                return c;
            }
        }
        return null;
    }

    public Instance findFirstDescendant(String n) {
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance c = children.get(i);
            if (c.name.equals(n)) {
                return c;
            }
            Instance d = c.findFirstDescendant(n);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    public Instance findFirstChildOfClass(String cls, boolean whichIsA) {
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance c = children.get(i);
            if (whichIsA ? c.isA(cls) : c.desc.name.equals(cls)) {
                return c;
            }
        }
        return null;
    }

    public Instance findFirstAncestorOfClass(String cls, boolean whichIsA) {
        for (Instance p = parent; p != null; p = p.parent) {
            if (whichIsA ? p.isA(cls) : p.desc.name.equals(cls)) {
                return p;
            }
        }
        return null;
    }

    public boolean isDescendantOf(Instance o) {
        for (Instance p = parent; p != null; p = p.parent) {
            if (p == o) { // NOPMD CompareObjectsWithEquals - tree identity
                return true;
            }
        }
        return false;
    }

    public void descendants(List<Instance> out) {
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance c = children.get(i);
            out.add(c);
            c.descendants(out);
        }
    }

    public List<Instance> descendants() {
        List<Instance> out = new ArrayList<Instance>();
        descendants(out);
        return out;
    }

    public String fullName() {
        if (parent == null || parent == engine.game) { // NOPMD CompareObjectsWithEquals - the root is unique
            return name;
        }
        return parent.fullName() + "." + name;
    }

    public void setParent(Instance np) {
        if (np == parent) { // NOPMD CompareObjectsWithEquals - tree identity
            return;
        }
        if (parentLocked || destroyed) {
            throw LuaError.rt("The Parent property of " + fullName() + " is locked, current parent: "
                    + (parent == null ? "NULL" : parent.name) + ", new parent " + (np == null ? "NULL" : np.name));
        }
        if (np == this || np != null && np.isDescendantOf(this)) { // NOPMD CompareObjectsWithEquals - tree identity
            throw LuaError.rt("Attempt to set " + fullName() + " as its own parent");
        }
        if (desc.service && parent != null) {
            throw LuaError.rt("The Parent property of " + name + " is locked");
        }
        Instance old = parent;
        if (old != null) {
            fireDescendantRemoving(old, this);
            old.children.remove(this);
            old.fireEvent("ChildRemoved", this);
        }
        parent = np;
        if (np != null) {
            np.children.add(this);
            np.fireEvent("ChildAdded", this);
            fireDescendantAdded(np, this);
        }
        ancestry(this);
        changed("Parent");
    }

    private static void fireDescendantAdded(Instance at, Instance added) {
        for (Instance p = at; p != null; p = p.parent) {
            p.fireEvent("DescendantAdded", added);
            List<Instance> sub = added.descendants();
            for (int i = 0; i < sub.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                p.fireEvent("DescendantAdded", sub.get(i));
            }
        }
    }

    private static void fireDescendantRemoving(Instance at, Instance removed) {
        for (Instance p = at; p != null; p = p.parent) {
            p.fireEvent("DescendantRemoving", removed);
        }
    }

    private void ancestry(Instance changedOne) {
        for (ClassDb.Desc d = desc; d != null; d = d.parent) {
            if (d.behavior != null) {
                d.behavior.ancestryChanged(this);
            }
        }
        engine.ancestryChanged(this);
        fireEvent("AncestryChanged", changedOne, changedOne.parent);
        Instance[] kids = children.toArray(new Instance[0]);
        for (Instance k : kids) {
            k.ancestry(changedOne);
        }
    }

    public void destroy() {
        if (destroyed) {
            return;
        }
        fireEvent("Destroying");
        Instance[] kids = children.toArray(new Instance[0]);
        for (Instance k : kids) {
            k.destroy();
        }
        if (parent != null) {
            setParent(null);
        }
        destroyed = true;
        parentLocked = true;
        engine.destroyed(this);
        if (signals != null) {
            for (Signal s : signals.values()) {
                s.disconnectAll();
            }
        }
        propSignals = null;
        attrSignals = null;
    }

    public void clearAllChildren() {
        Instance[] kids = children.toArray(new Instance[0]);
        for (Instance k : kids) {
            k.destroy();
        }
    }

    /// A deep copy of the archivable subtree; references inside it point at
    /// the copies.
    public Instance cloneTree() {
        if (!bool("Archivable")) {
            return null;
        }
        Map<Instance, Instance> map = new HashMap<Instance, Instance>();
        Instance c = copyInto(map);
        c.remapRefs(map);
        return c;
    }

    private Instance copyInto(Map<Instance, Instance> map) {
        Instance c = engine.create(desc.name);
        c.name = name;
        c.props.putAll(props);
        if (attributes != null) {
            c.attributes = new LinkedHashMap<String, Object>(attributes);
        }
        if (tags != null) {
            c.tags = new ArrayList<String>(tags);
        }
        map.put(this, c);
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance k = children.get(i);
            if (k.bool("Archivable")) {
                Instance kc = k.copyInto(map);
                kc.parent = c;
                c.children.add(kc);
            }
        }
        return c;
    }

    private void remapRefs(Map<Instance, Instance> map) {
        for (Map.Entry<String, Object> e : props.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Instance && map.containsKey(v)) {
                e.setValue(map.get(v));
            }
        }
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            children.get(i).remapRefs(map);
        }
    }

    // ---------------------------------------------------------------- events

    public Signal event(String n) {
        if (signals == null) {
            signals = new HashMap<String, Signal>();
        }
        Signal s = signals.get(n);
        if (s == null) {
            s = new Signal(n, engine.sched);
            signals.put(n, s);
        }
        return s;
    }

    public void fireEvent(String n, Object... args) {
        if (signals == null) {
            return;
        }
        Signal s = signals.get(n);
        if (s != null) {
            s.fire(args);
        }
    }

    public boolean hasListeners(String n) {
        Signal s = signals == null ? null : signals.get(n);
        return s != null && s.hasListeners();
    }

    public Signal propertySignal(String prop) {
        if (!hasProp(prop) && !"Name".equals(prop) && !"Parent".equals(prop)) {
            throw LuaError.rt(prop + " is not a valid property name.");
        }
        if (propSignals == null) {
            propSignals = new HashMap<String, Signal>();
        }
        Signal s = propSignals.get(prop);
        if (s == null) {
            s = new Signal(prop, engine.sched);
            propSignals.put(prop, s);
        }
        return s;
    }

    // ---------------------------------------------------------------- attributes and tags

    public Object getAttribute(String n) {
        return attributes == null ? null : attributes.get(n);
    }

    public Map<String, Object> attributes() {
        if (attributes == null) {
            attributes = new LinkedHashMap<String, Object>();
        }
        return attributes;
    }

    public void setAttribute(String n, Object v) {
        Object old = getAttribute(n);
        if (same(old, v)) {
            return;
        }
        if (v == null) {
            if (attributes != null) {
                attributes.remove(n);
            }
        } else {
            attributes().put(n, v);
        }
        fireEvent("AttributeChanged", n);
        if (attrSignals != null && attrSignals.get(n) != null) {
            attrSignals.get(n).fire();
        }
        engine.attributeChanged(this, n);
    }

    public List<String> tags() {
        if (tags == null) {
            tags = new ArrayList<String>();
        }
        return tags;
    }

    public boolean hasTag(String t) {
        return tags != null && tags.contains(t);
    }

    // ---------------------------------------------------------------- script access

    private static final Map<String, JavaFunction> METHODS = new HashMap<String, JavaFunction>();

    static JavaFunction method(final String n) {
        JavaFunction f = METHODS.get(n);
        if (f == null) {
            f = new JavaFunction(n) {
                @Override
                public Object[] call(Interp in, Object[] a) {
                    Object self = arg(a, 0);
                    if (!(self instanceof Instance)) {
                        throw LuaError.rt("Expected ':' not '.' calling member function " + n);
                    }
                    return ((Instance) self).callMethod(n, in, a);
                }
            };
            METHODS.put(n, f);
        }
        return f;
    }

    private static final String[] COMMON = {"FindFirstChild", "FindFirstChildOfClass", "FindFirstChildWhichIsA",
        "FindFirstAncestor", "FindFirstAncestorOfClass", "FindFirstAncestorWhichIsA", "FindFirstDescendant",
        "WaitForChild", "GetChildren", "GetDescendants", "IsA", "IsDescendantOf", "IsAncestorOf", "Destroy",
        "Clone", "ClearAllChildren", "GetFullName", "GetAttribute", "SetAttribute", "GetAttributes",
        "GetPropertyChangedSignal", "GetAttributeChangedSignal", "AddTag", "HasTag", "RemoveTag", "GetTags",
        "Remove", "remove", "destroy", "clone", "findFirstChild", "children", "getChildren", "isA", "GetActor",
        "IsPropertyModified", "ResetPropertyToDefault", "GetStyled"};

    private static boolean isCommon(String n) {
        for (String c : COMMON) {
            if (c.equals(n)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object get(Interp in, String key) {
        switch (key) {
            case "Name":
                return name;
            case "ClassName":
                return desc.name;
            case "Parent":
                return parent;
            default:
                break;
        }
        for (ClassDb.Desc d = desc; d != null; d = d.parent) {
            if (d.behavior != null) {
                Object v = d.behavior.get(this, in, key);
                if (v != Behavior.PASS) { // NOPMD CompareObjectsWithEquals - sentinel
                    return v;
                }
            }
        }
        if (hasProp(key)) {
            return prop(key);
        }
        if (isCommon(key) || desc.hasMethod(key)) {
            return method(key);
        }
        if (desc.hasEvent(key)) {
            return event(key);
        }
        Instance c = findFirstChild(key);
        if (c != null) {
            return c;
        }
        throw LuaError.rt(key + " is not a valid member of " + desc.name + " \"" + fullName() + "\"");
    }

    @Override
    public void set(Interp in, String key, Object v) {
        if ("Name".equals(key)) {
            if (v instanceof String) {
                setName((String) v);
            } else if (v instanceof Double) {
                setName(Interp.numberToString(((Double) v).doubleValue()));
            } else {
                throw LuaError.rt("Unable to assign property Name. string expected, got " + Interp.typeName(v));
            }
            return;
        }
        if ("Parent".equals(key)) {
            if (v != null && !(v instanceof Instance)) {
                throw LuaError.rt("Unable to assign property Parent. Instance expected, got " + Interp.typeName(v));
            }
            setParent((Instance) v);
            return;
        }
        for (ClassDb.Desc d = desc; d != null; d = d.parent) {
            if (d.behavior != null && d.behavior.set(this, in, key, v)) {
                return;
            }
        }
        if (!hasProp(key)) {
            throw LuaError.rt(key + " is not a valid member of " + desc.name + " \"" + fullName() + "\"");
        }
        put(key, coerce(key, v));
    }

    Object[] callMethod(String n, Interp in, Object[] a) {
        for (ClassDb.Desc d = desc; d != null; d = d.parent) {
            if (d.behavior != null) {
                Object[] r = d.behavior.call(this, n, in, a);
                if (r != Behavior.PASS_CALL) { // NOPMD CompareObjectsWithEquals - sentinel
                    return r;
                }
            }
        }
        switch (n) {
            case "FindFirstChild":
            case "findFirstChild": {
                String c = Args.str(a, 1, n);
                return ret(Args.bool(a, 2) ? findFirstDescendant(c) : findFirstChild(c));
            }
            case "FindFirstDescendant":
                return ret(findFirstDescendant(Args.str(a, 1, n)));
            case "FindFirstChildOfClass":
                return ret(findFirstChildOfClass(Args.str(a, 1, n), false));
            case "FindFirstChildWhichIsA": {
                String c = Args.str(a, 1, n);
                if (Args.bool(a, 2)) {
                    List<Instance> all = descendants();
                    for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        if (all.get(i).isA(c)) {
                            return ret(all.get(i));
                        }
                    }
                    return ret(null);
                }
                return ret(findFirstChildOfClass(c, true));
            }
            case "FindFirstAncestor": {
                String c = Args.str(a, 1, n);
                for (Instance p = parent; p != null; p = p.parent) {
                    if (p.name.equals(c)) {
                        return ret(p);
                    }
                }
                return ret(null);
            }
            case "FindFirstAncestorOfClass":
                return ret(findFirstAncestorOfClass(Args.str(a, 1, n), false));
            case "FindFirstAncestorWhichIsA":
                return ret(findFirstAncestorOfClass(Args.str(a, 1, n), true));
            case "WaitForChild":
                return waitForChild(in, Args.str(a, 1, n), Args.optNum(a, 2, -1));
            case "GetChildren":
            case "getChildren":
            case "children":
                return ret(list(children));
            case "GetDescendants":
                return ret(list(descendants()));
            case "IsA":
            case "isA":
                return ret(Boolean.valueOf(isA(Args.str(a, 1, n))));
            case "IsDescendantOf":
                return ret(Boolean.valueOf(Args.at(a, 1) instanceof Instance && isDescendantOf((Instance) a[1])));
            case "IsAncestorOf":
                return ret(Boolean.valueOf(Args.at(a, 1) instanceof Instance && ((Instance) a[1]).isDescendantOf(this)));
            case "Destroy":
            case "destroy":
                destroy();
                return null;
            case "Remove":
            case "remove":
                setParent(null);
                return null;
            case "Clone":
            case "clone":
                return ret(cloneTree());
            case "ClearAllChildren":
                clearAllChildren();
                return null;
            case "GetFullName":
                return ret(fullName());
            case "GetAttribute":
                return ret(getAttribute(Args.str(a, 1, n)));
            case "SetAttribute":
                setAttribute(Args.str(a, 1, n), Args.at(a, 2));
                return null;
            case "GetAttributes": {
                LuaTable t = new LuaTable();
                if (attributes != null) {
                    for (Map.Entry<String, Object> e : attributes.entrySet()) {
                        t.rawset(e.getKey(), e.getValue());
                    }
                }
                return ret(t);
            }
            case "GetPropertyChangedSignal":
                return ret(propertySignal(Args.str(a, 1, n)));
            case "GetAttributeChangedSignal": {
                String at = Args.str(a, 1, n);
                if (attrSignals == null) {
                    attrSignals = new HashMap<String, Signal>();
                }
                Signal s = attrSignals.get(at);
                if (s == null) {
                    s = new Signal(at, engine.sched);
                    attrSignals.put(at, s);
                }
                return ret(s);
            }
            case "AddTag":
                engine.addTag(this, Args.str(a, 1, n));
                return null;
            case "RemoveTag":
                engine.removeTag(this, Args.str(a, 1, n));
                return null;
            case "HasTag":
                return ret(Boolean.valueOf(hasTag(Args.str(a, 1, n))));
            case "GetTags": {
                LuaTable t = new LuaTable();
                List<String> tg = tags();
                for (int i = 0; i < tg.size(); i++) {
                    t.rawseti(i + 1, tg.get(i));
                }
                return ret(t);
            }
            case "GetActor":
                return ret(findFirstAncestorOfClass("Actor", false));
            case "IsPropertyModified":
                return ret(Boolean.valueOf(props.containsKey(Args.str(a, 1, n))));
            case "ResetPropertyToDefault":
                props.remove(Args.str(a, 1, n));
                changed(Args.str(a, 1, n));
                return null;
            default:
                engine.unsupported(desc.name, n);
                throw LuaError.rt(desc.name + ":" + n + " is not supported by this engine");
        }
    }

    private Object[] waitForChild(Interp in, final String n, double timeout) {
        Instance c = findFirstChild(n);
        if (c != null) {
            return ret(c);
        }
        final com.codenameone.examples.brickyard.luau.Coroutine co = in.currentThread();
        final Signal.Connection[] conn = new Signal.Connection[1];
        final boolean[] done = new boolean[1];
        conn[0] = event("ChildAdded").connect(new Signal.Listener() {
            @Override
            public void fired(Object[] args) {
                Instance k = (Instance) args[0];
                if (!done[0] && k.name.equals(n)) {
                    done[0] = true;
                    conn[0].disconnect();
                    engine.sched.resume(co, new Object[] {k});
                }
            }
        });
        double limit = timeout >= 0 ? timeout : 5;
        engine.later(limit, new Runnable() {
            @Override
            public void run() {
                if (!done[0]) {
                    if (isInfinite(timeout)) {
                        engine.sched.print("warn", "Infinite yield possible on '" + fullName() + ":WaitForChild(\"" + n + "\")'");
                        return;
                    }
                    done[0] = true;
                    conn[0].disconnect();
                    engine.sched.resume(co, new Object[] {null});
                }
            }
        });
        return in.yield(new Object[0]);
    }

    private static boolean isInfinite(double t) {
        return t < 0;
    }

    static LuaTable list(List<Instance> l) {
        LuaTable t = new LuaTable(l.size());
        for (int i = 0; i < l.size(); i++) {
            t.rawseti(i + 1, l.get(i));
        }
        return t;
    }

    @Override
    public String str() {
        return name;
    }
}

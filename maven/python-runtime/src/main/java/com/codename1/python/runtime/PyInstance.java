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
package com.codename1.python.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/// An instance of a class the application defines (and of the builtin
/// exception types, which are defined the same way).
///
/// Every operator is routed to the matching dunder method of the class, so a
/// class that defines `__add__` or `__getitem__` behaves under `+` and `[]`
/// exactly as in CPython.
public class PyInstance extends PyObject {
    public final PyType cls;
    /// Set once the instance holds an attribute outside its compiled fields,
    /// which might shadow a method the compiled dispatch would call.
    public boolean extra;
    /// The attribute layout and the values it describes; see `Shape`. An
    /// array shared by every instance of a class instead of a hash table
    /// per instance, which is both the lookup cost and most of the memory
    /// a small object would otherwise take.
    ///
    /// Null for the empty shape, and `slots` null with it: neither is
    /// stored by the constructor, which every object of every class runs.
    /// A ParparVM reference store goes through the collector's barrier, a
    /// call, so storing the two defaults cost more than the rest of a small
    /// object's construction.
    Shape shape;
    Object[] slots;


    /// Keys of `__dict__` that are not strings (`o.__dict__[1] = x`), which
    /// CPython keeps in the dict but no attribute can name. Null for nearly
    /// every instance.
    PyDict nonStrKeys;

    /// The `__dict__` view, made on first use and kept, so that
    /// `o.__dict__ is o.__dict__` as in CPython. Null until then.
    private InstanceDict dictView;

    /// The shape, the empty one for null.
    final Shape shape() {
        return shape == null ? Shape.EMPTY : shape;
    }
    /// For an instance of a subclass of a builtin type, the builtin value it
    /// extends (see `BuiltinSubclass`); null otherwise.
    public Object base;

    public PyInstance(PyType cls) {
        this.cls = cls;
    }

    @Override
    public PyType type() {
        return cls;
    }

    /// The compiled field holding attribute `name`, or -1.
    ///
    /// A class the transpiler can see whole gets a Java subclass whose
    /// attributes are fields (`PyType.layout`): generated code reads them
    /// straight after an `instanceof`, and these hooks let every generic path
    /// see them too. An unset field is null, as an absent attribute is.
    protected int fieldIndex(String name) {
        return -1;
    }

    /// Field `i` (see [#fieldIndex]).
    protected Object getField(int i) {
        return null;
    }

    /// Stores field `i`; null unsets it.
    protected void setField(int i, Object value) {
    }

    /// The names of the fields, in declaration order.
    protected String[] fieldNames() {
        return NO_NAMES;
    }

    private static final String[] NO_NAMES = new String[0];

    /// The instance attribute `name`, or null.
    Object own(String name) {
        int f = fieldIndex(name);
        if (f >= 0) {
            Object v = getField(f);
            if (v != null || !extra) {
                return v;
            }
        }
        int i = shape == null ? -1 : shape.indexOf(name);
        return i < 0 ? null : slots[i];
    }

    /// Stores `name`, adding it to the shape if the instance lacks it.
    void store(String name, Object value) {
        int f = fieldIndex(name);
        // A field first set after some other attribute would list before
        // it; such a late attribute is kept in the shape instead, which
        // remembers the order.
        if (f >= 0 && (!extra || getField(f) != null)) {
            setField(f, value);
            return;
        }
        int i = shape == null ? -1 : shape.indexOf(name);
        if (i >= 0) {
            slots[i] = value;
        } else {
            Set<String> allowed = cls.slotNames();
            if (allowed != null && !allowed.contains(name)) {
                throw Py.attributeError("'" + cls.name + "' object has no attribute '" + name
                        + "' and no __dict__ for setting new attributes");
            }
            append(shape().with(name), value);
        }
    }

    /// Moves to `next`, which is this instance's shape plus one name, and
    /// stores that name's value.
    void append(Shape next, Object value) {
        int n = shape().size();
        if (slots == null) {
            slots = new Object[4];
        } else if (n == slots.length) {
            Object[] grown = new Object[n < 4 ? 4 : n * 2];
            System.arraycopy(slots, 0, grown, 0, n);
            slots = grown;
        }
        slots[n] = value;
        shape = next;
        extra = true;
    }

    /// The attribute names: the set fields, then the rest in the order they
    /// were first assigned.
    String[] attributeNames() {
        String[] f = fieldNames();
        if (f.length == 0) {
            return shape().names;
        }
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < f.length; i++) {
            if (getField(i) != null) {
                out.add(f[i]);
            }
        }
        for (String n : shape().names) {
            out.add(n);
        }
        return out.toArray(new String[out.size()]);
    }

    @Override
    public Object getAttrOrNull(String name) {
        if (cls.hasCustomGetattribute()) {
            // Every explicit read goes through a class's own __getattribute__,
            // dunder names included (`obj.__dict__`, getattr(obj, '__x__')); the
            // implicit special-method lookup of len(obj) and friends reads the
            // type and never comes here. An AttributeError it raises falls back
            // to __getattr__, as CPython's slot does.
            try {
                return Py.obj(cls.lookup("__getattribute__")).call(new Object[] {this, name}, Py.NO_KW);
            } catch (PyException e) {
                if (!e.matches(Py.ATTRIBUTE_ERROR)) {
                    throw e;
                }
                Object ga = cls.lookup("__getattr__");
                if (ga == null) {
                    return null;
                }
                return Py.obj(ga).call(new Object[] {this, name}, Py.NO_KW);
            }
        }
        Object v = genericAttrOrNull(name);
        if (v != null) {
            return v;
        }
        Object ga = cls.lookup("__getattr__");
        if (ga != null) {
            return Py.obj(ga).call(new Object[] {this, name}, Py.NO_KW);
        }
        return null;
    }

    /// `object.__getattribute__(self, name)`: the lookup without the class's
    /// own `__getattribute__` or `__getattr__`; null when absent.
    Object genericAttrOrNull(String name) {
        Object clsAttr = cls.lookup(name);
        if (clsAttr instanceof PyProperty) {
            return ((PyProperty) clsAttr).get(this);
        }
        Object get = null;
        if (clsAttr instanceof PyInstance) {
            // A descriptor: one with __set__ or __delete__ (a data
            // descriptor) wins over the instance's own attribute.
            PyType dt = ((PyInstance) clsAttr).cls;
            get = dt.lookup("__get__");
            if (get != null && (dt.lookup("__set__") != null || dt.lookup("__delete__") != null)) {
                return Py.obj(get).call(new Object[] {clsAttr, this, cls}, Py.NO_KW);
            }
        }
        Object v = own(name);
        if (v != null) {
            return v;
        }
        if (get != null) {
            return Py.obj(get).call(new Object[] {clsAttr, this, cls}, Py.NO_KW);
        }
        if ("__dict__".equals(name)) {
            // A live view: its stores and deletes reach the instance, and it
            // refills itself from the instance before every read, so a write
            // made through the attribute shows in a view fetched earlier.
            if (cls.slotNames() != null) {
                // Every class declares __slots__ without __dict__: CPython
                // has no dict to answer either.
                throw Py.attributeError("'" + cls.name + "' object has no attribute '__dict__'");
            }
            // One view per instance, refilled from the instance before
            // every read, so a view fetched earlier sees later writes.
            if (dictView == null) {
                dictView = new InstanceDict(this);
            }
            return dictView;
        }
        if ("__class__".equals(name)) {
            return cls;
        }
        if (clsAttr != null) {
            return Py.bindAttr(clsAttr, this);
        }
        return null;
    }

    @Override
    public void setAttr(String name, Object value) {
        // A class's own __setattr__ runs first and decides, even for a name
        // a property or another data descriptor owns, as in CPython.
        Object sa = cls.lookup("__setattr__");
        if (sa != null && !(sa instanceof PyBuiltin)) {
            Py.obj(sa).call(new Object[] {this, name, value}, Py.NO_KW);
            return;
        }
        setAttrObject(name, value);
    }

    /// `object.__setattr__(self, name, value)`: data descriptors, then the
    /// plain store, never an overriding `__setattr__`.
    public void setAttrObject(String name, Object value) {
        Object clsAttr = cls.lookup(name);
        if (clsAttr instanceof PyProperty) {
            ((PyProperty) clsAttr).set(this, value);
            return;
        }
        if (clsAttr instanceof PyInstance) {
            Object set = ((PyInstance) clsAttr).cls.lookup("__set__");
            if (set != null) {
                Py.obj(set).call(new Object[] {clsAttr, this, value}, Py.NO_KW);
                return;
            }
        }
        store(name, value);
    }

    /// The plain store, bypassing descriptors and an overriding `__setattr__`.
    public void setAttrDirect(String name, Object value) {
        store(name, value);
    }

    @Override
    public void delAttr(String name) {
        Object da = cls.lookup("__delattr__");
        if (da != null && !(da instanceof PyBuiltin)) {
            Py.obj(da).call(new Object[] {this, name}, Py.NO_KW);
            return;
        }
        Object clsAttr = cls.lookup(name);
        if (clsAttr instanceof PyInstance) {
            Object del = ((PyInstance) clsAttr).cls.lookup("__delete__");
            if (del != null) {
                Py.obj(del).call(new Object[] {clsAttr, this}, Py.NO_KW);
                return;
            }
        }
        delAttrDirect(name);
    }

    /// Removes the instance attribute `name`, bypassing descriptors.
    void delAttrDirect(String name) {
        int f = fieldIndex(name);
        if (f >= 0) {
            if (getField(f) != null) {
                setField(f, null);
                // Set again, it is the newest attribute: see store.
                extra = true;
                return;
            }
        }
        int at = shape().indexOf(name);
        if (at < 0) {
            throw Py.attributeError("'" + cls.name + "' object has no attribute '" + name + "'");
        }
        // Rebuilt from the empty shape, so the instance meets every other
        // instance that holds the same names in the same order.
        String[] names = shape.names;
        Object[] old = slots;
        shape = null;
        slots = null;
        for (int i = 0; i < names.length; i++) {
            if (i != at) {
                append(shape().with(names[i]), old[i]);
            }
        }
    }

    /// Calls dunder `name` if the class defines it, else returns `null`.
    protected final Object dunder(String name, Object arg) {
        Object m = cls.lookup(name);
        if (m == null) {
            return null;
        }
        Object r = Py.obj(m).call(new Object[] {this, arg}, Py.NO_KW);
        return r == Py.NOT_IMPLEMENTED ? null : r; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    private Object dunder0(String name) {
        Object m = cls.lookup(name);
        return m == null ? null : Py.obj(m).call(new Object[] {this}, Py.NO_KW);
    }

    @Override
    public String repr() {
        Object r = dunder0("__repr__");
        if (r != null) {
            return Py.strValue(r, "__repr__");
        }
        return "<" + cls.name + " object at 0x" + Integer.toHexString(System.identityHashCode(this)) + ">";
    }

    @Override
    public String str() {
        Object r = dunder0("__str__");
        if (r != null) {
            return Py.strValue(r, "__str__");
        }
        return repr();
    }

    @Override
    public boolean isTrue() {
        Object m = cls.lookup("__bool__");
        if (m != null) {
            Object r = Py.obj(m).call(new Object[] {this}, Py.NO_KW);
            if (!(r instanceof PyBool)) {
                throw Py.typeError("__bool__ should return bool, returned " + Py.typeOf(r).name);
            }
            return r == Py.TRUE; //NOPMD CompareObjectsWithEquals - identity is meant
        }
        m = cls.lookup("__len__");
        if (m != null) {
            return len() != 0;
        }
        return true;
    }

    @Override
    public Object call(Object[] args, String[] kwNames) {
        Object m = cls.lookup("__call__");
        if (m == null) {
            return super.call(args, kwNames);
        }
        return Py.obj(m).call(Py.prepend(this, args), kwNames);
    }

    @Override
    public Object add(Object o) {
        return dunder("__add__", o);
    }

    @Override
    public Object radd(Object o) {
        return dunder("__radd__", o);
    }

    @Override
    public Object iadd(Object o) {
        return dunder("__iadd__", o);
    }

    @Override
    public Object inplace(String name, Object o) {
        return dunder(name, o);
    }

    @Override
    public Object sub(Object o) {
        return dunder("__sub__", o);
    }

    @Override
    public Object rsub(Object o) {
        return dunder("__rsub__", o);
    }

    @Override
    public Object mul(Object o) {
        return dunder("__mul__", o);
    }

    @Override
    public Object rmul(Object o) {
        return dunder("__rmul__", o);
    }

    @Override
    public Object truediv(Object o) {
        return dunder("__truediv__", o);
    }

    @Override
    public Object rtruediv(Object o) {
        return dunder("__rtruediv__", o);
    }

    @Override
    public Object floordiv(Object o) {
        return dunder("__floordiv__", o);
    }

    @Override
    public Object rfloordiv(Object o) {
        return dunder("__rfloordiv__", o);
    }

    @Override
    public Object mod(Object o) {
        return dunder("__mod__", o);
    }

    @Override
    public Object rmod(Object o) {
        return dunder("__rmod__", o);
    }

    @Override
    public Object pow(Object o) {
        return dunder("__pow__", o);
    }

    @Override
    public Object rpow(Object o) {
        return dunder("__rpow__", o);
    }

    @Override
    public Object and(Object o) {
        return dunder("__and__", o);
    }

    @Override
    public Object or(Object o) {
        return dunder("__or__", o);
    }

    @Override
    public Object xor(Object o) {
        return dunder("__xor__", o);
    }

    @Override
    public Object lshift(Object o) {
        return dunder("__lshift__", o);
    }

    @Override
    public Object rshift(Object o) {
        return dunder("__rshift__", o);
    }

    @Override
    public Object neg() {
        Object r = dunder0("__neg__");
        return r != null ? r : super.neg();
    }

    @Override
    public Object pos() {
        Object r = dunder0("__pos__");
        return r != null ? r : super.pos();
    }

    @Override
    public Object invert() {
        Object r = dunder0("__invert__");
        return r != null ? r : super.invert();
    }

    @Override
    public Object eq(Object o) {
        Object r = dunder("__eq__", o);
        return r != null ? r : super.eq(o);
    }

    @Override
    public Object lt(Object o) {
        return dunder("__lt__", o);
    }

    @Override
    public Object le(Object o) {
        return dunder("__le__", o);
    }

    @Override
    public Object gt(Object o) {
        return dunder("__gt__", o);
    }

    @Override
    public Object ge(Object o) {
        return dunder("__ge__", o);
    }

    @Override
    public long hash() {
        Object m = cls.lookup("__hash__");
        if (m == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.typeError("unhashable type: '" + cls.name + "'");
        }
        if (m == null) {
            if (cls.lookup("__eq__") != null) {
                throw Py.typeError("unhashable type: '" + cls.name + "'");
            }
            return System.identityHashCode(this);
        }
        return Py.asLong(Py.obj(m).call(new Object[] {this}, Py.NO_KW));
    }

    @Override
    public Object getItem(Object key) {
        if (base instanceof PyDict) {
            Object m = cls.lookup("__getitem__");
            if (m instanceof PyBuiltin) {
                Object v = ((PyDict) base).getOrNull(key);
                if (v != null) {
                    return v;
                }
                Object missing = cls.lookup("__missing__");
                if (missing != null) {
                    return Py.obj(missing).call(new Object[] {this, key}, Py.NO_KW);
                }
            }
        }
        Object r = dunder("__getitem__", key);
        return r != null ? r : super.getItem(key);
    }

    @Override
    public void setItem(Object key, Object value) {
        Object m = cls.lookup("__setitem__");
        if (m == null) {
            super.setItem(key, value);
            return;
        }
        Py.obj(m).call(new Object[] {this, key, value}, Py.NO_KW);
    }

    @Override
    public void delItem(Object key) {
        Object m = cls.lookup("__delitem__");
        if (m == null) {
            super.delItem(key);
            return;
        }
        Py.obj(m).call(new Object[] {this, key}, Py.NO_KW);
    }

    @Override
    public int len() {
        Object r = dunder0("__len__");
        if (r == null) {
            return super.len();
        }
        long n = Py.asLong(r);
        if (n < 0) {
            throw Py.valueError("__len__() should return >= 0");
        }
        return (int) n;
    }

    @Override
    public boolean contains(Object x) {
        Object m = cls.lookup("__contains__");
        if (m != null) {
            return Py.isTrue(Py.obj(m).call(new Object[] {this, x}, Py.NO_KW));
        }
        return super.contains(x);
    }

    @Override
    public Object iter() {
        Object r = dunder0("__iter__");
        if (r != null) {
            return r;
        }
        if (cls.lookup("__getitem__") != null) {
            return new PySeqIter(this);
        }
        return super.iter();
    }

    @Override
    public Object next() {
        Object m = cls.lookup("__next__");
        if (m == null) {
            return super.next();
        }
        try {
            return Py.obj(m).call(new Object[] {this}, Py.NO_KW);
        } catch (PyException e) {
            if (e.matches(Py.STOP_ITERATION)) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public long asLong() {
        Object r = dunder0("__index__");
        if (r == null) {
            r = dunder0("__int__");
        }
        return r != null ? Py.asLong(r) : super.asLong();
    }

    @Override
    public double asDouble() {
        Object r = dunder0("__float__");
        return r != null ? Py.asDouble(r) : super.asDouble();
    }

    /// `__dict__` of an instance: its stores and deletes reach the instance.
    private static final class InstanceDict extends PyDict {
        private final PyInstance self;

        /// Set while refilling, so the refill's own stores do not sync again.
        private boolean syncing;

        /// What the keys were last read from: the instance's shape and which
        /// of its fields were set. While both are unchanged the keys are, and
        /// sync only re-reads the values -- by index, with no name lookup.
        /// Values cannot be stamped: inline caches and generated code store
        /// into slots and fields directly.
        private Object seenShape;
        private long seenFields;
        private boolean seen;

        // No "dictionary keys changed during iteration" check: CPython 3.14
        // does not raise it for an instance's __dict__ either (deleting one
        // attribute and adding another while iterating runs on, as
        // object_protocol_ in the conformance tests shows); only a change of
        // size is reported, as there.

        InstanceDict(PyInstance self) {
            this.self = self;
        }

        @Override
        void sync() {
            if (syncing) {
                return;
            }
            syncing = true;
            try {
                String[] fields = self.fieldNames();
                long mask = 0;
                boolean small = fields.length <= 64;
                for (int i = 0; small && i < fields.length; i++) {
                    if (self.getField(i) != null) {
                        mask |= 1L << i;
                    }
                }
                // A net, not the mechanism: every write through this dict
                // clears `seen`, but a path that changes the instance without
                // moving its shape or set fields would otherwise go unseen.
                PyDict other0 = self.nonStrKeys;
                int expected = Long.bitCount(mask) + self.shape().size() + (other0 == null ? 0 : other0.size);
                if (small && seen && seenShape == self.shape && seenFields == mask //NOPMD CompareObjectsWithEquals - identity is meant
                        && size == expected && refreshValues(fields, mask)) {
                    return;
                }
                seenShape = self.shape;
                seenFields = mask;
                seen = small;
                String[] names = self.attributeNames();
                PyDict other = self.nonStrKeys;
                if (sameKeys(names, other)) {
                    // The same keys in the same order: the values are
                    // brought up to date in place, so an iteration in
                    // progress over this dict is not disturbed.
                    int n = 0;
                    for (int i = 0; i < used && n < names.length; i++) {
                        if (keys[i] != null) {
                            values[i] = self.own(names[n++]);
                        }
                    }
                    return;
                }
                super.clear();
                for (String e : names) {
                    super.setItem(e, self.own(e));
                }
                if (other != null) {
                    for (int i = 0; i < other.used; i++) {
                        if (other.keys[i] != null) {
                            super.setItem(other.keys[i], other.values[i]);
                        }
                    }
                }
            } finally {
                syncing = false;
            }
        }

        /// Re-reads the values in key order: the set fields, then the shape's
        /// slots (the order [PyInstance#attributeNames()] lists them in).
        /// Answers false, leaving the caller to rebuild, when a key is not
        /// the name expected at its place. The caller has already matched
        /// the shape, the set fields and the size, which fix the keys; the
        /// name check is a cheap safety net against a path that changes the
        /// keys without moving any of those, never the expected case.
        private boolean refreshValues(String[] fieldNames, long mask) {
            int fieldCount = fieldNames.length;
            String[] shapeNames = self.shape().names;
            int f = 0;
            int slot = 0;
            Object[] sl = self.slots;
            for (int i = 0; i < used; i++) {
                if (keys[i] == null) {
                    continue;
                }
                while (f < fieldCount && (mask & (1L << f)) == 0) {
                    f++;
                }
                if (f < fieldCount) {
                    if (!fieldNames[f].equals(keys[i])) {
                        return false;
                    }
                    values[i] = self.getField(f++);
                } else if (sl != null && slot < sl.length && slot < shapeNames.length) {
                    if (!shapeNames[slot].equals(keys[i])) {
                        return false;
                    }
                    values[i] = sl[slot++];
                } else {
                    // The non-str keys, after every attribute: they change
                    // only through this dict, which keeps them current.
                    break;
                }
            }
            return true;
        }

        /// Whether the keys already are `names` and then `other`'s, in that
        /// order.
        private boolean sameKeys(String[] names, PyDict other) {
            int extra = other == null ? 0 : other.size;
            if (size != names.length + extra) {
                return false;
            }
            int n = 0;
            for (int i = 0; i < used; i++) {
                if (keys[i] == null) {
                    continue;
                }
                if (n < names.length) {
                    if (!names[n].equals(keys[i])) {
                        return false;
                    }
                } else if (!other.contains(keys[i])) {
                    return false;
                }
                n++;
            }
            return true;
        }

        private static boolean isStr(Object key) {
            return key instanceof String || Py.isinstance(key, Py.STR_TYPE);
        }

        @Override
        public void setItem(Object key, Object value) {
            sync();
            // The attribute first: a store that fails (a slot it may not
            // hold) leaves the dict as it was.
            if (isStr(key)) {
                self.store(Py.toStr(key), value);
                super.setItem(key, value);
            } else {
                super.setItem(key, value);
                // Kept apart from the attributes, so the dict lists them
                // after every attribute rather than in insertion order as
                // CPython does: a known difference, for keys no program
                // reaches through an attribute anyway.
                if (self.nonStrKeys == null) {
                    self.nonStrKeys = new PyDict();
                }
                self.nonStrKeys.setItem(key, value);
            }
            // The write may restore a shape the stamp already holds (del
            // d['b'] then c.b = 9), so the next sync must not trust it.
            seen = false;
        }

        @Override
        void setLong(long k, Object value) {
            setItem(Long.valueOf(k), value);
        }

        @Override
        public void delItem(Object key) {
            sync();
            // Not super.delItem: it removes through remove(), which is overridden below and
            // would delete the attribute a second time.
            if (super.remove(key) == null) {
                throw Py.keyError(key);
            }
            forget(key);
        }

        /// Removes `key` from what the dict stands for.
        private void forget(Object key) {
            seen = false;
            if (isStr(key)) {
                self.delAttrDirect(Py.toStr(key));
            } else if (self.nonStrKeys != null) {
                self.nonStrKeys.remove(key);
            }
        }

        // pop() and popitem() remove through here, clear() through clear(): without these
        // overrides `obj.__dict__.pop(name)` answered the value and left the attribute set.
        @Override
        public Object remove(Object key) {
            sync();
            Object v = super.remove(key);
            if (v != null) {
                forget(key);
            }
            return v;
        }

        @Override
        public void clear() {
            sync();
            Object[] snapshot = new Object[used];
            int n = 0;
            for (int i = 0; i < used; i++) {
                if (keys[i] != null) {
                    snapshot[n++] = keys[i];
                }
            }
            super.clear();
            for (int i = 0; i < n; i++) {
                forget(snapshot[i]);
            }
        }
    }

}

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
        String[] names = shape().names;
        for (int i = 0; i < names.length; i++) {
            out.add(names[i]);
        }
        return out.toArray(new String[out.size()]);
    }

    @Override
    public Object getAttrOrNull(String name) {
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
            // A copy of the attributes whose stores and deletes reach the
            // instance too; a change made the other way after it was read
            // is not seen through it.
            PyDict d = new InstanceDict(this);
            String[] names = attributeNames();
            for (String e : names) {
                d.setItem(e, own(e));
            }
            return d;
        }
        if ("__class__".equals(name)) {
            return cls;
        }
        if (clsAttr != null) {
            return Py.bindAttr(clsAttr, this);
        }
        Object ga = cls.lookup("__getattr__");
        if (ga != null) {
            return Py.obj(ga).call(new Object[] {this, name}, Py.NO_KW);
        }
        return null;
    }

    @Override
    public void setAttr(String name, Object value) {
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
        Object sa = cls.lookup("__setattr__");
        if (sa != null && !(sa instanceof PyBuiltin)) {
            Py.obj(sa).call(new Object[] {this, name, value}, Py.NO_KW);
            return;
        }
        store(name, value);
    }

    /// `object.__setattr__(self, name, value)`: the plain store, bypassing an
    /// overriding `__setattr__`.
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

        InstanceDict(PyInstance self) {
            this.self = self;
        }

        @Override
        public void setItem(Object key, Object value) {
            super.setItem(key, value);
            self.store(Py.toStr(key), value);
        }

        @Override
        void setLong(long k, Object value) {
            setItem(Long.valueOf(k), value);
        }

        @Override
        public void delItem(Object key) {
            super.delItem(key);
            self.delAttrDirect(Py.toStr(key));
        }
    }

}

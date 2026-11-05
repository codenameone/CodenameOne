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

/// Root of the Python object model that transpiled code runs against.
///
/// Every Python value is a `PyObject`. The operations Python spells with
/// operators and dunder methods are virtual methods here; a type overrides the
/// ones it supports and the rest raise the `TypeError` CPython would. Binary
/// operators return `null` for "not implemented" so that [Py] can try the
/// reflected operation on the right operand, exactly as CPython's
/// `NotImplemented` protocol does.
public abstract class PyObject {

    /// The Python type of this value.
    public abstract PyType type();

    // ---- conversion ------------------------------------------------------

    /// `repr(self)`.
    public String repr() {
        return "<" + type().name + " object>";
    }

    /// `str(self)`; defaults to [#repr()] as in CPython.
    public String str() {
        return repr();
    }

    /// `bool(self)`.
    public boolean isTrue() {
        return true;
    }

    // ---- attributes ------------------------------------------------------

    /// `self.name`, or `null` when the attribute does not exist.
    public Object getAttrOrNull(String name) {
        Object attr = type().lookup(name);
        if (attr != null) {
            return Py.bindAttr(attr, this);
        }
        if ("__class__".equals(name)) {
            return type();
        }
        return null;
    }

    /// `self.name`, raising `AttributeError` when it does not exist.
    public final Object getAttr(String name) {
        Object v = getAttrOrNull(name);
        if (v == null) {
            if (this instanceof PyType) {
                throw Py.attributeError("type object '" + ((PyType) this).name + "' has no attribute '" + name + "'");
            }
            throw Py.attributeError("'" + type().name + "' object has no attribute '" + name + "'");
        }
        return v;
    }

    /// `self.name = value`.
    public void setAttr(String name, Object value) {
        throw Py.attributeError("'" + type().name + "' object attribute '" + name + "' is read-only");
    }

    /// `del self.name`.
    public void delAttr(String name) {
        throw Py.attributeError("'" + type().name + "' object attribute '" + name + "' is read-only");
    }

    /// Descriptor protocol: what an attribute found on a type becomes when read
    /// through an instance. Plain values bind to themselves.
    public Object bind(Object self) {
        return this;
    }

    // ---- calls -----------------------------------------------------------

    /// `self(*args, **kwargs)`. `kwNames` names the trailing `kwNames.length`
    /// entries of `args`; it is never `null`.
    public Object call(Object[] args, String[] kwNames) {
        throw Py.typeError("'" + type().name + "' object is not callable");
    }

    // Positional calls of a fixed arity. A function compiled with that many
    // parameters takes them directly; everything else gets the argument array
    // it would have been handed anyway.

    public Object call0() {
        return call(Py.NO_ARGS, Py.NO_KW);
    }

    public Object call1(Object a) {
        return call(new Object[] {a}, Py.NO_KW);
    }

    public Object call2(Object a, Object b) {
        return call(new Object[] {a, b}, Py.NO_KW);
    }

    public Object call3(Object a, Object b, Object c) {
        return call(new Object[] {a, b, c}, Py.NO_KW);
    }

    public Object call4(Object a, Object b, Object c, Object d) {
        return call(new Object[] {a, b, c, d}, Py.NO_KW);
    }

    public Object call5(Object a, Object b, Object c, Object d, Object e) {
        return call(new Object[] {a, b, c, d, e}, Py.NO_KW);
    }

    public Object call6(Object a, Object b, Object c, Object d, Object e, Object f) {
        return call(new Object[] {a, b, c, d, e, f}, Py.NO_KW);
    }

    // ---- numbers ---------------------------------------------------------

    public Object add(Object o) {
        return null;
    }

    public Object radd(Object o) {
        return null;
    }

    public Object sub(Object o) {
        return null;
    }

    public Object rsub(Object o) {
        return null;
    }

    public Object mul(Object o) {
        return null;
    }

    public Object rmul(Object o) {
        return null;
    }

    public Object truediv(Object o) {
        return null;
    }

    public Object rtruediv(Object o) {
        return null;
    }

    public Object floordiv(Object o) {
        return null;
    }

    public Object rfloordiv(Object o) {
        return null;
    }

    public Object mod(Object o) {
        return null;
    }

    public Object rmod(Object o) {
        return null;
    }

    public Object pow(Object o) {
        return null;
    }

    public Object rpow(Object o) {
        return null;
    }

    public Object lshift(Object o) {
        return null;
    }

    public Object rshift(Object o) {
        return null;
    }

    public Object and(Object o) {
        return null;
    }

    public Object rand(Object o) {
        return null;
    }

    public Object or(Object o) {
        return null;
    }

    public Object ror(Object o) {
        return null;
    }

    public Object xor(Object o) {
        return null;
    }

    public Object rxor(Object o) {
        return null;
    }

    public Object neg() {
        throw Py.typeError("bad operand type for unary -: '" + type().name + "'");
    }

    public Object pos() {
        throw Py.typeError("bad operand type for unary +: '" + type().name + "'");
    }

    public Object invert() {
        throw Py.typeError("bad operand type for unary ~: '" + type().name + "'");
    }

    /// In-place variants default to the plain operator; mutable types override.
    public Object iadd(Object o) {
        return null;
    }

    /// The other in-place operators: `dunder` names one (`__isub__`); null
    /// means "not supported in place", and the plain operator runs instead.
    public Object inplace(String dunder, Object o) {
        return null;
    }

    // ---- comparison ------------------------------------------------------

    /// `self == o`, or `null` for not implemented.
    public Object eq(Object o) {
        return this == o ? Py.TRUE : null; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    public Object lt(Object o) {
        return null;
    }

    public Object le(Object o) {
        return null;
    }

    public Object gt(Object o) {
        return null;
    }

    public Object ge(Object o) {
        return null;
    }

    /// `hash(self)`.
    public long hash() {
        return System.identityHashCode(this);
    }

    // ---- containers ------------------------------------------------------

    public Object getItem(Object key) {
        throw Py.typeError("'" + type().name + "' object is not subscriptable");
    }

    public void setItem(Object key, Object value) {
        throw Py.typeError("'" + type().name + "' object does not support item assignment");
    }

    public void delItem(Object key) {
        throw Py.typeError("'" + type().name + "' object does not support item deletion");
    }

    public int len() {
        throw Py.typeError("object of type '" + type().name + "' has no len()");
    }

    /// `x in self`.
    public boolean contains(Object x) {
        Object it = iter();
        for (Object v = Py.next(it); v != null; v = Py.next(it)) {
            if (Py.eqBool(v, x)) {
                return true;
            }
        }
        return false;
    }

    /// `iter(self)`.
    public Object iter() {
        throw Py.typeError("'" + type().name + "' object is not iterable");
    }

    /// `next(self)`, returning `null` at exhaustion instead of raising
    /// `StopIteration`, so loops need no exception.
    public Object next() {
        throw Py.typeError("'" + type().name + "' object is not an iterator");
    }

    // ---- Java interop ----------------------------------------------------

    /// The natural Java value: `Long`, `Double`, `String`, `Boolean`, `null`,
    /// or the object itself when there is none.
    public Object toJava() {
        return this;
    }

    /// `int(self)` as a Java index; raises `TypeError` for non-integers.
    public long asLong() {
        throw Py.typeError("'" + type().name + "' object cannot be interpreted as an integer");
    }

    /// `float(self)`.
    public double asDouble() {
        throw Py.typeError("must be real number, not " + type().name);
    }

    @Override
    public String toString() {
        return str();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PyObject && Py.eqBool(this, (PyObject) o);
    }

    @Override
    public int hashCode() {
        long h = hash();
        return (int) (h ^ (h >>> 32));
    }
}

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

/// A generator or coroutine object: the state of a suspended function body.
///
/// The transpiler lowers the body of a function that contains `yield` (or an
/// `async def`) into [#step], a `switch` over the suspension points, and the
/// function's locals into fields of an anonymous subclass. `pc` is the point
/// to resume at: 0 before the first resumption, a positive state while
/// suspended, -1 once finished. A step that returns with `pc` positive has
/// yielded the value it returns; one that returns with `pc == -1` has finished,
/// and the value is the function's return value.
///
/// Nothing here relies on a thread: suspension is a return from `step`, so a
/// generator costs one object and its fields, and an abandoned one is simply
/// garbage. Unlike CPython, an abandoned generator is not closed, so the
/// `finally` blocks of a generator never resumed to completion do not run.
public abstract class PyGenerator extends PyObject {
    /// The resumption point, see the class comment.
    protected int pc;
    /// The exception an `except` or `finally` state was entered with.
    protected PyException exc;
    /// The iterator a `yield from` or `await` is delegating to.
    protected PyObject delegate;
    /// What the finished delegate returned, the value of `yield from`.
    protected Object delegateResult;

    private final String name;
    private final boolean coroutine;
    private boolean running;
    private Object sent = Py.NONE;
    private PyException thrown;
    private Object returned = Py.NONE;

    protected PyGenerator(String name, boolean coroutine) {
        this.name = name;
        this.coroutine = coroutine;
    }

    /// Runs the body from `pc` to its next suspension or its end.
    protected abstract Object step();

    /// The value of the `yield` the body resumed at, or the exception thrown
    /// into it, raised.
    protected final Object resumed() {
        PyException t = thrown;
        Object s = sent;
        thrown = null;
        sent = Py.NONE;
        if (t != null) {
            throw t;
        }
        return s;
    }

    /// Resumes the body: `v` is sent in, or `t` raised at the suspension
    /// point. Returns the next value yielded, or null once the body finished
    /// (its return value is then [#returned]).
    final Object resume(Object v, PyException t) {
        if (running) {
            throw Py.valueError((coroutine ? "coroutine" : "generator") + " already executing");
        }
        if (pc == -1) {
            if (t != null) {
                throw t;
            }
            if (coroutine) {
                throw Py.runtimeError("cannot reuse already awaited coroutine");
            }
            return null;
        }
        if (pc == 0) {
            if (t != null) {
                pc = -1;
                throw t;
            }
            if (v != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                throw Py.typeError("can't send non-None value to a just-started "
                        + (coroutine ? "coroutine" : "generator"));
            }
        }
        sent = v;
        thrown = t;
        running = true;
        try {
            Object r = step();
            if (pc == -1) {
                returned = r;
                return null;
            }
            return r;
        } catch (PyException e) {
            pc = -1;
            if (e.matches(Py.STOP_ITERATION)) {
                // PEP 479: a StopIteration escaping the body would end the
                // caller's loop silently.
                Object err = Py.runtimeError((coroutine ? "coroutine" : "generator") + " raised StopIteration").value;
                PyException re = Py.raise(err, e.value);
                if (err instanceof PyInstance) {
                    ((PyInstance) err).setAttrDirect("__context__", e.value);
                }
                throw re; //NOPMD PreserveStackTrace - the Python cause is chained
            }
            throw e;
        } catch (RuntimeException e) {
            pc = -1;
            throw e;
        } catch (Error e) {
            pc = -1;
            throw e;
        } finally {
            running = false;
        }
    }

    /// The finished body's return value.
    final Object returned() {
        return returned;
    }

    /// One step of a `yield from` (or `await`) over [#delegate]: the value
    /// to pass out, or null when the delegate is exhausted, with its result
    /// in [#delegateResult]. A value sent in or an exception thrown in is
    /// forwarded to the delegate, as PEP 380 specifies.
    protected final Object delegateStep() {
        PyException t = thrown;
        Object s = sent;
        thrown = null;
        sent = Py.NONE;
        PyObject d = delegate;
        Object r;
        if (d instanceof PyGenerator) {
            PyGenerator g = (PyGenerator) d;
            if (t != null && t.matches(Py.GENERATOR_EXIT)) {
                g.close();
                throw t;
            }
            r = g.resume(s, t);
            if (r == null) {
                delegate = null;
                delegateResult = g.returned;
                return null;
            }
            return r;
        }
        try {
            if (t != null) {
                if (t.matches(Py.GENERATOR_EXIT)) {
                    Object close = d.getAttrOrNull("close");
                    if (close != null) {
                        Py.call(close);
                    }
                    throw t;
                }
                Object m = d.getAttrOrNull("throw");
                if (m == null) {
                    throw t;
                }
                r = Py.call(m, t.value);
            } else if (s == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                r = d.next();
                if (r == null) {
                    delegate = null;
                    delegateResult = Py.NONE;
                    return null;
                }
            } else {
                r = Py.callMethod(d, "send", s);
            }
            return r;
        } catch (PyException e) {
            if (e.matches(Py.STOP_ITERATION)) {
                delegate = null;
                delegateResult = Py.getattr(e.value, "value");
                return null;
            }
            throw e;
        }
    }

    // ---- the Python-visible protocol --------------------------------------

    /// `gen.send(v)`.
    public Object send(Object v) {
        Object r = resume(v, null);
        if (r == null) {
            throw stopIteration(returned);
        }
        return r;
    }

    /// `gen.throw(e)`.
    public Object throwIn(Object e) {
        Object r = resume(Py.NONE, Py.raise(e));
        if (r == null) {
            throw stopIteration(returned);
        }
        return r;
    }

    /// `gen.close()`.
    public void close() {
        if (pc <= 0) {
            pc = -1;
            return;
        }
        Object r;
        try {
            r = resume(Py.NONE, Py.raise(Py.GENERATOR_EXIT));
        } catch (PyException e) {
            if (e.matches(Py.GENERATOR_EXIT) || e.matches(Py.STOP_ITERATION)) {
                return;
            }
            throw e;
        }
        if (r != null) {
            throw Py.runtimeError((coroutine ? "coroutine" : "generator") + " ignored GeneratorExit");
        }
    }

    static PyException stopIteration(Object value) {
        Object[] args = value == Py.NONE ? Py.NO_ARGS : new Object[] {value}; //NOPMD CompareObjectsWithEquals - identity is meant
        return new PyException(Py.STOP_ITERATION.call(args, Py.NO_KW));
    }

    public boolean isCoroutine() {
        return coroutine;
    }

    @Override
    public PyType type() {
        return coroutine ? Py.COROUTINE_TYPE : Py.GENERATOR_TYPE;
    }

    @Override
    public String repr() {
        return "<" + (coroutine ? "coroutine" : "generator") + " object " + name + ">";
    }

    @Override
    public Object iter() {
        if (coroutine) {
            return super.iter();
        }
        return this;
    }

    @Override
    public Object next() {
        if (coroutine) {
            return super.next();
        }
        return resume(Py.NONE, null);
    }

    /// The iterator `await x` delegates to.
    public static PyObject awaitable(Object x) {
        if (x instanceof PyGenerator && ((PyGenerator) x).coroutine) {
            // delegateStep drives a coroutine directly.
            return (PyGenerator) x;
        }
        Object m = Py.obj(x).getAttrOrNull("__await__");
        if (m == null) {
            throw Py.typeError("object " + Py.typeOf(x).name + " can't be used in 'await' expression");
        }
        return Py.iter(Py.call(m));
    }

    /// `coro.__await__()`: the coroutine driven as an iterator.
    static final class Awaiter extends PyGenerator {
        private final PyGenerator coro;

        Awaiter(PyGenerator coro) {
            super("__await__", false);
            this.coro = coro;
        }

        @Override
        protected Object step() {
            if (pc == 0) {
                delegate = coro;
                pc = 1;
            }
            Object r = delegateStep();
            if (r == null) {
                pc = -1;
                return delegateResult;
            }
            return r;
        }
    }

    /// An async generator: `async def` with `yield`. Its body yields a
    /// [Yielded] for each `yield` and passes anything else -- what an
    /// `await` inside it suspends on -- out to the event loop unchanged.
    public abstract static class Async extends PyGenerator {
        protected Async(String name) {
            super(name, false);
        }

        @Override
        public PyType type() {
            return Py.ASYNC_GENERATOR_TYPE;
        }

        @Override
        public String repr() {
            return "<async_generator object " + super.repr().substring("<generator object ".length());
        }

        @Override
        public Object iter() {
            throw Py.typeError("'async_generator' object is not iterable");
        }

        @Override
        public Object next() {
            throw Py.typeError("'async_generator' object is not an iterator");
        }
    }

    /// The value of a `yield` in an async generator, as its step returns it.
    public static final class Yielded {
        final Object value;

        public Yielded(Object value) {
            this.value = value;
        }
    }

    private static final int ASEND = 0;
    private static final int ATHROW = 1;
    private static final int ACLOSE = 2;

    /// The awaitable `agen.__anext__()`, `asend(v)`, `athrow(e)` and
    /// `aclose()` return: it runs the async generator until its next
    /// `yield`, whose value is the result of the await.
    static final class ASend extends PyGenerator {
        private final PyGenerator agen;
        private final int mode;
        private final Object arg;

        ASend(PyGenerator agen, int mode, Object arg) {
            super("async_generator_asend", true);
            this.agen = agen;
            this.mode = mode;
            this.arg = arg;
        }

        @Override
        protected Object step() {
            Object v = Py.NONE;
            PyException t = null;
            if (pc == 0) {
                resumed();
                if (mode == ASEND) {
                    v = arg;
                } else if (mode == ATHROW) {
                    t = Py.raise(arg);
                } else {
                    if (agen.pc <= 0) {
                        agen.pc = -1;
                        pc = -1;
                        return Py.NONE;
                    }
                    t = Py.raise(Py.GENERATOR_EXIT);
                }
            } else {
                try {
                    v = resumed();
                } catch (PyException e) {
                    t = e;
                }
            }
            Object r;
            try {
                r = agen.resume(v, t);
            } catch (PyException e) {
                if (mode == ACLOSE && (e.matches(Py.GENERATOR_EXIT) || e.matches(Py.STOP_ASYNC_ITERATION))) {
                    pc = -1;
                    return Py.NONE;
                }
                throw e;
            }
            if (r == null) {
                if (mode == ACLOSE) {
                    pc = -1;
                    return Py.NONE;
                }
                throw new PyException(Py.STOP_ASYNC_ITERATION.call(Py.NO_ARGS, Py.NO_KW));
            }
            if (r instanceof Yielded) {
                if (mode == ACLOSE) {
                    throw Py.runtimeError("async generator ignored GeneratorExit");
                }
                pc = -1;
                return ((Yielded) r).value;
            }
            pc = 1;
            return r;
        }
    }

    static void install() {
        PyType ag = Py.ASYNC_GENERATOR_TYPE;
        ag.define("__aiter__", PyBuiltin.method(ag, "__aiter__", a -> a[0]));
        ag.define("__anext__", PyBuiltin.method(ag, "__anext__", a -> new ASend(gen(a[0]), ASEND, Py.NONE)));
        ag.define("asend", PyBuiltin.method(ag, "asend", a -> new ASend(gen(a[0]), ASEND, a[1])));
        ag.define("athrow", PyBuiltin.method(ag, "athrow", a -> new ASend(gen(a[0]), ATHROW,
                a.length > 2 && a[2] != Py.NONE ? a[2] : a[1])));
        ag.define("aclose", PyBuiltin.method(ag, "aclose", a -> new ASend(gen(a[0]), ACLOSE, Py.NONE)));
        for (int i = 0; i < 2; i++) {
            PyType t = i == 0 ? Py.GENERATOR_TYPE : Py.COROUTINE_TYPE;
            t.define("send", PyBuiltin.method(t, "send", a -> gen(a[0]).send(a.length > 1 ? a[1] : Py.NONE)));
            t.define("throw", PyBuiltin.method(t, "throw", a -> gen(a[0]).throwIn(a.length > 2 && a[2] != Py.NONE
                    ? a[2] : a[1])));
            t.define("close", PyBuiltin.method(t, "close", a -> {
                gen(a[0]).close();
                return Py.NONE;
            }));
        }
        Py.GENERATOR_TYPE.define("__iter__", PyBuiltin.method(Py.GENERATOR_TYPE, "__iter__", a -> a[0]));
        Py.GENERATOR_TYPE.define("__next__", PyBuiltin.method(Py.GENERATOR_TYPE, "__next__", a -> {
            Object r = gen(a[0]).resume(Py.NONE, null);
            if (r == null) {
                throw stopIteration(gen(a[0]).returned);
            }
            return r;
        }));
        Py.COROUTINE_TYPE.define("__await__", PyBuiltin.method(Py.COROUTINE_TYPE, "__await__",
                a -> new Awaiter(gen(a[0]))));
    }

    private static PyGenerator gen(Object o) {
        if (o instanceof PyGenerator) {
            return (PyGenerator) o;
        }
        throw Py.typeError("descriptor requires a generator but received a '" + Py.typeOf(o).name + "'");
    }
}

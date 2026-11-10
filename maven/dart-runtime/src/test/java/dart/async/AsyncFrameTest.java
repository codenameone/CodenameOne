/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package dart.async;

import dart.runtime.Funcs;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Hand-written state machines, in the shape the CPS lowering emits, driven headlessly
/// through the microtask queue.
public class AsyncFrameTest {

    @Test
    public void awaitOnACompletedFutureYields() {
        final List<String> log = new ArrayList<String>();
        // async { log a; await Future.value(1); log b }  then the caller logs c.
        Future<Object> f = new AsyncFrame<Object>() {
            @Override
            protected void step() throws Throwable {
                switch (state) {
                    case 0:
                        log.add("a");
                        await(Future.value((Object) 1L), 1);
                        return;
                    case 1:
                        log.add("b" + value());
                        complete(null);
                        return;
                    default:
                        throw new IllegalStateException();
                }
            }
        }.start();
        log.add("c");
        assertFalse(f.isDone());
        Async.runMicrotasks();
        assertEquals("[a, c, b1]", log.toString());
        assertTrue(f.isDone());
    }

    @Test
    public void awaitOnANonFutureYieldsWithTheValue() {
        final List<String> log = new ArrayList<String>();
        Future<Object> f = new AsyncFrame<Object>() {
            @Override
            protected void step() throws Throwable {
                if (state == 0) {
                    await("v", 1);
                    return;
                }
                log.add((String) value());
                complete("done");
            }
        }.start();
        log.add("sync");
        Async.runMicrotasks();
        assertEquals("[sync, v]", log.toString());
        assertEquals("done", f.valueOrThrow());
    }

    @Test
    public void thenOnACompletedFutureRunsLater() {
        final List<String> log = new ArrayList<String>();
        Future.value((Object) 1L).then(new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object v) {
                log.add("then");
                return null;
            }
        });
        log.add("sync");
        Async.runMicrotasks();
        assertEquals("[sync, then]", log.toString());
    }

    @Test
    public void throwBeforeTheFirstAwaitGivesAnErrorFuture() {
        Future<Object> f = new AsyncFrame<Object>() {
            @Override
            protected void step() throws Throwable {
                throw new dart.core.StateError("early");
            }
        }.start();
        assertTrue(f.isDone());
        Throwable e = f.completedError();
        assertNotNull(e);
        assertTrue(String.valueOf(e).contains("early"), String.valueOf(e));
    }

    @Test
    public void awaitedErrorIsRethrownAtTheResume() {
        final Completer<Object> c = new Completer<Object>();
        final List<String> log = new ArrayList<String>();
        Future<Object> f = new AsyncFrame<Object>() {
            @Override
            protected void step() throws Throwable {
                switch (state) {
                    case 0:
                        await(c.future(), 1);
                        return;
                    default:
                        try {
                            value();
                            log.add("no error");
                        } catch (Throwable t) {
                            log.add("caught");
                        }
                        complete("ok");
                }
            }
        }.start();
        c.completeError(new dart.core.StateError("late"));
        assertFalse(f.isDone(), "the resume is a microtask, not inside completeError");
        Async.runMicrotasks();
        assertEquals("[caught]", log.toString());
        assertEquals("ok", f.valueOrThrow());
    }

    @Test
    public void zoneIsRestoredAcrossAResume() {
        final Completer<Object> c = new Completer<Object>();
        final Zone[] seen = new Zone[2];
        final Zone[] inner = new Zone[1];
        Async.runZonedGuarded(new Funcs.Func0<Object>() {
            @Override
            public Object call() {
                inner[0] = Zone.current;
                new AsyncFrame<Object>() {
                    @Override
                    protected void step() throws Throwable {
                        if (state == 0) {
                            seen[0] = Zone.current;
                            await(c.future(), 1);
                            return;
                        }
                        seen[1] = Zone.current;
                        complete(null);
                    }
                }.start();
                return null;
            }
        }, new Funcs.VoidFunc2<Object, dart.core.StackTrace>() {
            @Override
            public void call(Object e, dart.core.StackTrace s) {
            }
        });
        assertSame(Zone.root, Zone.current);
        c.complete("x");
        Async.runMicrotasks();
        assertSame(inner[0], seen[0]);
        assertSame(inner[0], seen[1]);
        assertTrue(inner[0] != Zone.root);
        assertSame(Zone.root, Zone.current);
    }

    @Test
    public void voidFrameErrorReachesTheGuardedZoneHandler() {
        final List<Object> errors = new ArrayList<Object>();
        Async.runZonedGuarded(new Funcs.Func0<Object>() {
            @Override
            public Object call() {
                new AsyncVoidFrame() {
                    @Override
                    protected void step() throws Throwable {
                        if (state == 0) {
                            await(null, 1);
                            return;
                        }
                        throw new dart.core.StateError("boom");
                    }
                }.start();
                return null;
            }
        }, new Funcs.VoidFunc2<Object, dart.core.StackTrace>() {
            @Override
            public void call(Object e, dart.core.StackTrace s) {
                errors.add(e);
            }
        });
        assertTrue(errors.isEmpty());
        Async.runMicrotasks();
        assertEquals(1, errors.size());
        assertTrue(String.valueOf(errors.get(0)).contains("boom"), String.valueOf(errors.get(0)));
    }
}

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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A future completed WITH another future waits for it and adopts its outcome,
/// the way Dart's FutureOr completion does.
public class FutureAdoptionTest {

    @Test
    public void thenCallbackReturningAFutureAdoptsItsValue() {
        final Completer<Object> inner = new Completer<Object>();
        Future<Object> chained = Future.value((Object) "a").then(new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object v) {
                return inner.future();
            }
        });
        // Still waiting: the value is not the inner Future object.
        assertFalse(chained.isDone(), "the chain must wait for the returned future");
        inner.complete("fetched");
        assertTrue(chained.isDone());
        assertEquals("fetched", chained.valueOrThrow());
    }

    @Test
    public void thenCallbackReturningAFailingFutureAdoptsTheError() {
        final Completer<Object> inner = new Completer<Object>();
        Future<Object> chained = Future.value((Object) "a").then(new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object v) {
                return inner.future();
            }
        });
        inner.completeError(new IllegalStateException("fetch failed"));
        RuntimeException thrown = assertThrows(RuntimeException.class, chained::valueOrThrow);
        assertEquals("fetch failed", thrown.getMessage());
    }

    @Test
    public void catchErrorRecoveringWithAFutureAdoptsIt() {
        Future<Object> failed = new Future<Object>();
        failed.completeError(new IllegalStateException("boom"));
        Future<Object> recovered = failed.catchError(new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object e) {
                return Future.value((Object) "recovered");
            }
        });
        assertEquals("recovered", recovered.valueOrThrow());
    }

    @Test
    public void completingTwiceWhileAdoptingIsRefused() {
        Completer<Object> c = new Completer<Object>();
        c.complete(new Completer<Object>().future());
        assertThrows(dart.core.StateError.class, () -> c.complete("again"));
    }

    @Test
    public void aFutureCompletedWithItselfFailsInsteadOfHanging() {
        Future<Object> f = new Future<Object>();
        f.complete(f);
        assertTrue(f.isDone());
        assertThrows(RuntimeException.class, f::valueOrThrow);
    }

    @Test
    public void plainValuesAreUnchanged() {
        Future<Object> f = Future.value((Object) "v");
        assertSame("v", f.getNow());
    }

    @Test
    public void voidCatchErrorRecoversTheChain() {
        Future<Object> failed = new Future<Object>();
        failed.completeError(new IllegalStateException("boom"));
        final boolean[] ran = {false};
        Future<Object> recovered = failed.catchError(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                ran[0] = true;
            }
        });
        assertTrue(ran[0]);
        assertTrue(recovered.isDone());
        assertEquals(null, recovered.valueOrThrow(), "recovered with null, not rethrown");
    }

    @Test
    public void aThrowingVoidCatchErrorFailsTheChainInstead() {
        Future<Object> failed = new Future<Object>();
        failed.completeError(new IllegalStateException("boom"));
        Future<Object> chained = failed.catchError(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                throw new IllegalArgumentException("handler failed");
            }
        });
        RuntimeException thrown = assertThrows(RuntimeException.class, chained::valueOrThrow);
        assertEquals("handler failed", thrown.getMessage());
    }

    @Test
    public void whenCompleteCarriesTheOutcomeUnlessTheActionThrows() {
        Future<Object> ok = Future.value((Object) "v");
        assertEquals("v", ok.whenComplete(new Funcs.VoidFunc0() {
            @Override
            public void call() {
            }
        }).valueOrThrow());
        Future<Object> cleanupFailed = ok.whenComplete(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                throw new IllegalStateException("cleanup failed");
            }
        });
        RuntimeException thrown = assertThrows(RuntimeException.class, cleanupFailed::valueOrThrow);
        assertEquals("cleanup failed", thrown.getMessage());
    }

    @Test
    public void aNegativeDelayCompletesLikeZero() throws Exception {
        Future<Object> f = Future.delayed(dart.core.Duration.of(0, 0, 0, 0, -50, 0),
                new Funcs.Func0<Object>() {
                    @Override
                    public Object call() {
                        return "done";
                    }
                });
        long deadline = System.currentTimeMillis() + 2000;
        while (!f.isDone() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(f.isDone(), "a negative delay must not leave the future pending forever");
        assertEquals("done", f.valueOrThrow());
    }

    @Test
    public void aRejectingTestLetsTheErrorThrough() {
        Future<Object> failed = new Future<Object>();
        failed.completeError(new IllegalStateException("unrelated"));
        final boolean[] ran = {false};
        Future<Object> chained = failed.catchError(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                ran[0] = true;
            }
        }, new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object e) {
                return e instanceof IllegalArgumentException;
            }
        });
        assertFalse(ran[0], "the handler must not run for an error its test rejects");
        RuntimeException thrown = assertThrows(RuntimeException.class, chained::valueOrThrow);
        assertEquals("unrelated", thrown.getMessage());
    }

    @Test
    public void anAcceptingTestRecovers() {
        Future<Object> failed = new Future<Object>();
        failed.completeError(new IllegalArgumentException("mine"));
        Future<Object> chained = failed.catchError(new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object e) {
                return "recovered";
            }
        }, new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object e) {
                return e instanceof IllegalArgumentException;
            }
        });
        assertEquals("recovered", chained.valueOrThrow());
    }
}

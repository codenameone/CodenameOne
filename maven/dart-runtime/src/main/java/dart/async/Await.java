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

/**
 * The lowering target of Dart's {@code await} (M3 blocking model).
 *
 * <p>On the EDT the wait runs inside {@code Display.invokeAndBlock}, which
 * keeps dispatching events — other Dart callbacks, timers and UI input keep
 * running while this frame is parked, matching Dart's "other events run
 * during an await" semantics. Off the EDT (background threads, headless
 * tests) it is a plain monitor wait.</p>
 */
public final class Await {

    private Await() {
    }

    /**
     * {@code await v} where v is a FutureOr or dynamic: a Future is awaited, anything
     * else -- {@code await 42}, {@code await null} -- is the value itself, as in Dart.
     * Only the Future overload existed, so awaiting a non-Future did not compile.
     */
    @SuppressWarnings("unchecked")
    public static Object awaitAny(Object v) {
        return v instanceof Future ? await$((Future<Object>) v) : v;
    }

    public static <T> T await$(final Future<T> f) {
        if (f == null) {
            throw new dart.core.TypeError("await on null Future");
        }
        if (f.isDone()) {
            return f.valueOrThrow();
        }
        if (com.codename1.ui.Display.isInitialized()
                && com.codename1.ui.Display.getInstance().isEdt()) {
            com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    parkUntilDone(f);
                }
            });
        } else {
            parkUntilDone(f);
        }
        return f.valueOrThrow();
    }

    private static void parkUntilDone(Future<?> f) {
        synchronized (f.monitor()) {
            while (!f.isDone()) {
                try {
                    f.monitor().wait(500);
                } catch (InterruptedException ignore) {
                    // spurious wakeup handling via the loop condition
                }
            }
        }
    }
}

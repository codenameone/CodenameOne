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

/// The frame of an async body the transpiler lowered to a state machine. The emitter
/// declares the body's locals as fields of an anonymous subclass created where the body
/// was, so the function's type variables stay in scope, and the states as one lambda,
/// so {@code this} in the body is still the enclosing instance.
public class LambdaFrame<T> extends AsyncFrame<T> {
    /// One run of the state machine, from the current state to the next suspension.
    public interface Step {
        void step() throws Throwable;
    }

    public Step body;

    public LambdaFrame() {
    }

    @Override
    protected final void step() throws Throwable {
        body.step();
    }

    /// `return v;` in an async body: a Future (or any awaitable) is awaited first, as
    /// Dart's async return does, anything else completes the frame's future directly.
    @SuppressWarnings("unchecked")
    public final void resolve(Object v) {
        if (v instanceof Future) {
            final Future<Object> f = (Future<Object>) v;
            f.markHandled();
            f.onComplete(new Runnable() {
                @Override
                public void run() {
                    Throwable err = f.completedError();
                    if (err != null) {
                        fail(err);
                    } else {
                        complete((T) f.completedValue());
                    }
                }
            });
            return;
        }
        complete((T) v);
    }
}

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

/// The frame of an async body whose result nobody receives (a {@code void} async
/// function or closure). An error it does not catch goes to the uncaught-error handler of
/// the zone the frame was created in, as Dart reports an unhandled async error, rather
/// than into a future nobody listens to.
public abstract class AsyncVoidFrame extends AsyncFrame<Object> {
    protected AsyncVoidFrame() {
    }

    @Override
    public void fail(Throwable t) {
        if (isFinished()) {
            throw new IllegalStateException("async frame failed after it finished", t);
        }
        markFinished();
        zone.handleUncaughtError(dart.runtime.DartRuntime.caught(t), Zone.stackOf(t));
    }
}

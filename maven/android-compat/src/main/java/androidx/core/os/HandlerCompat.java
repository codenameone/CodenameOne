/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package androidx.core.os;

import android.os.Handler;
import android.os.Looper;

/// Static helpers over `Handler`.
public final class HandlerCompat {

    private HandlerCompat() {
    }

    public static Handler createAsync(Looper looper) {
        return Handler.createAsync(looper);
    }

    public static Handler createAsync(Looper looper, Handler.Callback callback) {
        return new Handler(looper, callback);
    }

    public static boolean postDelayed(Handler handler, Runnable r, Object token, long delayMillis) {
        return handler.postDelayed(r, token, delayMillis);
    }

    public static boolean hasCallbacks(Handler handler, Runnable r) {
        return handler.hasCallbacks(r);
    }
}

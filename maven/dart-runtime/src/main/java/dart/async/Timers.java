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
 * Schedules on the EDT after any delay a Dart Duration can express.
 *
 * <p>CN.setTimeout takes an int of milliseconds, so a delay past Integer.MAX_VALUE
 * -- about 24.8 days -- was narrowed to a negative timeout and threw from the
 * scheduler: {@code Future.delayed(Duration(days: 30))} failed at once instead of
 * completing in thirty days. Longer waits are chained in int-sized steps.</p>
 */
final class Timers {

    private Timers() {
    }

    static void schedule(final long ms, final Runnable r) {
        if (ms <= Integer.MAX_VALUE) {
            com.codename1.ui.CN.setTimeout((int) Math.max(0L, ms), r);
            return;
        }
        com.codename1.ui.CN.setTimeout(Integer.MAX_VALUE, new Runnable() {
            @Override
            public void run() {
                schedule(ms - Integer.MAX_VALUE, r);
            }
        });
    }
}

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
package java.util.concurrent;

/// Compile-time API for time units. Runtime implementations come from the host
/// JDK, Android, TeaVM, or vm/JavaAPI on ParparVM, as with the other CLDC stubs.
public enum TimeUnit {
    NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS;

    public long convert(long duration, TimeUnit sourceUnit) { return 0; }
    public long toNanos(long duration) { return 0; }
    public long toMicros(long duration) { return 0; }
    public long toMillis(long duration) { return 0; }
    public long toSeconds(long duration) { return 0; }
    public long toMinutes(long duration) { return 0; }
    public long toHours(long duration) { return 0; }
    public long toDays(long duration) { return 0; }
    public void timedWait(Object object, long timeout) throws InterruptedException { }
    public void timedJoin(Thread thread, long timeout) throws InterruptedException { }
    public void sleep(long timeout) throws InterruptedException { }
}

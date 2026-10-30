/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package java.lang.ref;

/// A queue the collector never enqueues into: this collector clears references but
/// does not post them. [#poll()] therefore always answers null and [#remove()] blocks
/// until the thread is interrupted, which is what a cleaning loop waiting on it expects
/// of a queue that stays empty.
public class ReferenceQueue<T> {
    public ReferenceQueue() {
    }

    public Reference poll() {
        return null;
    }

    public Reference remove() throws InterruptedException {
        synchronized (this) {
            while (true) {
                wait();
            }
        }
    }

    public Reference remove(long timeout) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("negative timeout");
        }
        synchronized (this) {
            if (timeout == 0) {
                while (true) {
                    wait();
                }
            }
            wait(timeout);
        }
        return null;
    }
}

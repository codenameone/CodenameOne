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
package android.os;

/// A thread with its own looper.
public class HandlerThread extends Thread {

    private Looper looper;
    private final Object lock = new Object();

    public HandlerThread(String name) {
        super(name);
    }

    public HandlerThread(String name, int priority) {
        super(name);
    }

    protected void onLooperPrepared() {
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (lock) {
            looper = Looper.myLooper();
            lock.notifyAll();
        }
        onLooperPrepared();
        Looper.loop();
    }

    public Looper getLooper() {
        synchronized (lock) {
            while (looper == null && isAlive()) {
                try {
                    lock.wait(50);
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return looper;
        }
    }

    public boolean quit() {
        Looper l = getLooper();
        if (l != null) {
            l.quit();
            return true;
        }
        return false;
    }

    public boolean quitSafely() {
        Looper l = getLooper();
        if (l != null) {
            l.quitSafely();
            return true;
        }
        return false;
    }

    public int getThreadId() {
        return 0;
    }
}

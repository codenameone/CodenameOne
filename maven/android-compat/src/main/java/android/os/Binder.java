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

/// A local binder. Subclass it and return the service from a method of your
/// own, as Android's bound-service guide does.
public class Binder implements IBinder {

    private String descriptor;

    public Binder() {
    }

    public Binder(String descriptor) {
        this.descriptor = descriptor;
    }

    public void attachInterface(Object owner, String descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String getInterfaceDescriptor() {
        return descriptor;
    }

    @Override
    public boolean pingBinder() {
        return true;
    }

    @Override
    public boolean isBinderAlive() {
        return true;
    }

    @Override
    public void linkToDeath(DeathRecipient recipient, int flags) {
    }

    @Override
    public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
        return true;
    }

    /// There is one process and no other caller; Android answers the
    /// caller's own ids for a local call, and these stand in for them.
    public static int getCallingPid() {
        return 1;
    }

    public static int getCallingUid() {
        return 10000;
    }

    public static long clearCallingIdentity() {
        return 0;
    }

    public static void restoreCallingIdentity(long token) {
    }
}

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

/// What `Service.onBind` answers. Within one process, as here, a client
/// casts it back to the service's own `Binder` subclass, which is the local
/// binder pattern Android documents; there is no cross-process transaction.
public interface IBinder {

    int FIRST_CALL_TRANSACTION = 0x00000001;
    int LAST_CALL_TRANSACTION = 0x00ffffff;
    int FLAG_ONEWAY = 0x00000001;

    /// Notified when the binder's process dies, which never happens apart
    /// from the application's own.
    interface DeathRecipient {
        void binderDied();
    }

    String getInterfaceDescriptor();

    boolean pingBinder();

    boolean isBinderAlive();

    void linkToDeath(DeathRecipient recipient, int flags);

    boolean unlinkToDeath(DeathRecipient recipient, int flags);
}

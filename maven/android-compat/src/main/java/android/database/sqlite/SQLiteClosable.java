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
package android.database.sqlite;

/// A reference-counted object: `close` releases the reference the
/// creator holds, and the object goes when the last one is released.
public abstract class SQLiteClosable implements AutoCloseable {

    private int mReferenceCount = 1;

    protected abstract void onAllReferencesReleased();

    @Deprecated
    protected void onAllReferencesReleasedFromContainer() {
        onAllReferencesReleased();
    }

    public void acquireReference() {
        if (mReferenceCount <= 0) {
            throw new IllegalStateException("attempt to re-open an already-closed object: " + this);
        }
        mReferenceCount++;
    }

    public void releaseReference() {
        if (mReferenceCount <= 0) {
            return;
        }
        if (--mReferenceCount == 0) {
            onAllReferencesReleased();
        }
    }

    @Deprecated
    public void releaseReferenceFromContainer() {
        if (mReferenceCount <= 0) {
            return;
        }
        if (--mReferenceCount == 0) {
            onAllReferencesReleasedFromContainer();
        }
    }

    @Override
    public void close() {
        releaseReference();
    }

    boolean hasReferences() {
        return mReferenceCount > 0;
    }
}

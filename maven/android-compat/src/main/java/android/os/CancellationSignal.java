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

/// Lets a caller cancel an operation in progress. Database work here runs
/// synchronously, so a signal is only checked before the work starts.
public final class CancellationSignal {

    /// Told when the signal is cancelled.
    public interface OnCancelListener {
        void onCancel();
    }

    private boolean mIsCanceled;
    private OnCancelListener mOnCancelListener;

    public boolean isCanceled() {
        return mIsCanceled;
    }

    public void throwIfCanceled() {
        if (mIsCanceled) {
            throw new OperationCanceledException();
        }
    }

    public void cancel() {
        if (mIsCanceled) {
            return;
        }
        mIsCanceled = true;
        if (mOnCancelListener != null) {
            mOnCancelListener.onCancel();
        }
    }

    public void setOnCancelListener(OnCancelListener listener) {
        if (mOnCancelListener == listener) {
            return;
        }
        mOnCancelListener = listener;
        if (mIsCanceled && listener != null) {
            listener.onCancel();
        }
    }
}

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
 * Dart's {@code StreamSubscription<T>}, returned by {@link Stream#listen}.
 *
 * <p>{@link #pause()} buffers events (data, errors and "done") raised while
 * paused and delivers them, in order, once {@link #resume()} runs; a
 * {@link #cancel()} issued while paused discards whatever was buffered
 * instead of delivering it.</p>
 *
 * <p>Not implemented: {@code pause(Future resumeSignal)} (use the no-arg
 * {@link #pause()} / {@link #resume()} pair) and {@code asFuture()} are not
 * provided.</p>
 */
public interface StreamSubscription<T> {

    /**
     * Cancels this subscription. Once the last live subscription on a
     * stream cancels, the stream's "no more listeners" hook runs (a
     * platform event channel stream calls the Flutter
     * {@code StreamHandler.onCancel} callback there; a
     * {@link StreamController}'s stream calls the controller's
     * {@code onCancel}). Returns an already-completed future, matching
     * Dart's {@code Future<void>} result shape without an asynchronous
     * teardown to wait for.
     */
    Future<Object> cancel();

    /** Replaces the data handler installed when this subscription was created. */
    void onData(Object handleData);

    /** Replaces the error handler installed when this subscription was created. */
    void onError(Object handleError);

    /** Replaces the done handler installed when this subscription was created. */
    void onDone(Object handleDone);

    /** Pauses delivery; events raised while paused are buffered, not dropped. */
    void pause();

    /** Resumes delivery after {@link #pause()}. */
    void resume();

    boolean isPaused();
}

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
package javafx.concurrent;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;

/// Work that runs on a background thread and reports on the JavaFX
/// application thread: its state, its progress and its result are
/// properties that change only there, so the user interface can bind
/// to them.
public interface Worker<V> {

    /// What a worker is doing.
    enum State {
        /// Created and not yet started.
        READY,
        /// About to run.
        SCHEDULED,
        /// Running.
        RUNNING,
        /// Finished with a result.
        SUCCEEDED,
        /// Cancelled before it finished.
        CANCELLED,
        /// Ended by an exception.
        FAILED
    }

    /// Returns what the worker is doing.
    State getState();

    /// What the worker is doing.
    ReadOnlyObjectProperty<State> stateProperty();

    /// Returns the result of the work, or the latest partial result.
    V getValue();

    /// The result of the work, or the latest partial result.
    ReadOnlyObjectProperty<V> valueProperty();

    /// Returns what made the work fail, or `null`.
    Throwable getException();

    /// What made the work fail, or `null`.
    ReadOnlyObjectProperty<Throwable> exceptionProperty();

    /// Returns how much of the work is done, -1 when that is unknown.
    double getWorkDone();

    /// How much of the work is done, -1 when that is unknown.
    ReadOnlyDoubleProperty workDoneProperty();

    /// Returns how much work there is, -1 when that is unknown.
    double getTotalWork();

    /// How much work there is, -1 when that is unknown.
    ReadOnlyDoubleProperty totalWorkProperty();

    /// Returns the share of the work that is done, 0 to 1, or -1 when that is unknown.
    double getProgress();

    /// The share of the work that is done, 0 to 1, or -1 when that is unknown.
    ReadOnlyDoubleProperty progressProperty();

    /// Returns whether the worker is scheduled or running.
    boolean isRunning();

    /// Whether the worker is scheduled or running.
    ReadOnlyBooleanProperty runningProperty();

    /// Returns a message describing what the worker is doing.
    String getMessage();

    /// A message describing what the worker is doing.
    ReadOnlyStringProperty messageProperty();

    /// Returns the title of the work.
    String getTitle();

    /// The title of the work.
    ReadOnlyStringProperty titleProperty();

    /// Asks the worker to stop. Returns whether that changed anything:
    /// a worker that already ended cannot be cancelled.
    boolean cancel();
}

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

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// The event of a worker entering a state.
public class WorkerStateEvent extends Event {

    private static final long serialVersionUID = 1L;

    /// Every worker state event.
    public static final EventType<WorkerStateEvent> ANY =
            new EventType<WorkerStateEvent>(Event.ANY, "WORKER_STATE");

    /// The worker became ready again.
    public static final EventType<WorkerStateEvent> WORKER_STATE_READY =
            new EventType<WorkerStateEvent>(ANY, "WORKER_STATE_READY");

    /// The worker was scheduled.
    public static final EventType<WorkerStateEvent> WORKER_STATE_SCHEDULED =
            new EventType<WorkerStateEvent>(ANY, "WORKER_STATE_SCHEDULED");

    /// The worker started running.
    public static final EventType<WorkerStateEvent> WORKER_STATE_RUNNING =
            new EventType<WorkerStateEvent>(ANY, "WORKER_STATE_RUNNING");

    /// The worker finished with a result.
    public static final EventType<WorkerStateEvent> WORKER_STATE_SUCCEEDED =
            new EventType<WorkerStateEvent>(ANY, "WORKER_STATE_SUCCEEDED");

    /// The worker was cancelled.
    public static final EventType<WorkerStateEvent> WORKER_STATE_CANCELLED =
            new EventType<WorkerStateEvent>(ANY, "WORKER_STATE_CANCELLED");

    /// The worker was ended by an exception.
    public static final EventType<WorkerStateEvent> WORKER_STATE_FAILED =
            new EventType<WorkerStateEvent>(ANY, "WORKER_STATE_FAILED");

    /// Creates the event of a worker entering a state.
    public WorkerStateEvent(Worker worker, EventType<? extends WorkerStateEvent> eventType) {
        super(worker, worker instanceof EventTarget ? (EventTarget) worker : null, eventType);
    }

    /// Returns the worker that entered the state.
    @Override
    public Worker getSource() {
        Object worker = super.getSource();
        return worker instanceof Worker ? (Worker) worker : null;
    }

    @Override
    public WorkerStateEvent copyFor(Object newSource, EventTarget newTarget) {
        super.copyFor(newSource, newTarget);
        return this;
    }
}

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

import java.util.ArrayList;

import com.codename1.fxcompat.runtime.EventHandlerManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.event.EventType;

/// The event handlers of a task or service: the ones added by type, and
/// one property per state for the `setOn...` handlers, which run after
/// them.
final class WorkerEvents {

    private static final String[] NAMES = {"onReady", "onScheduled", "onRunning", "onSucceeded", "onCancelled",
        "onFailed"};

    final EventHandlerManager manager;
    private final Object owner;
    private final ArrayList<ObjectProperty<EventHandler<WorkerStateEvent>>> slots =
            new ArrayList<ObjectProperty<EventHandler<WorkerStateEvent>>>();

    WorkerEvents(Object owner) {
        this.owner = owner;
        this.manager = new EventHandlerManager(owner);
        for (int i = 0; i < NAMES.length; i++) {
            slots.add(null);
        }
    }

    static EventType<WorkerStateEvent> type(Worker.State state) {
        switch (state) {
            case READY:
                return WorkerStateEvent.WORKER_STATE_READY;
            case SCHEDULED:
                return WorkerStateEvent.WORKER_STATE_SCHEDULED;
            case RUNNING:
                return WorkerStateEvent.WORKER_STATE_RUNNING;
            case SUCCEEDED:
                return WorkerStateEvent.WORKER_STATE_SUCCEEDED;
            case CANCELLED:
                return WorkerStateEvent.WORKER_STATE_CANCELLED;
            default:
                return WorkerStateEvent.WORKER_STATE_FAILED;
        }
    }

    ObjectProperty<EventHandler<WorkerStateEvent>> slot(Worker.State state) {
        int index = state.ordinal();
        ObjectProperty<EventHandler<WorkerStateEvent>> slot = slots.get(index);
        if (slot == null) {
            slot = new SimpleObjectProperty<EventHandler<WorkerStateEvent>>(owner, NAMES[index]);
            slots.set(index, slot);
        }
        return slot;
    }

    EventHandler<WorkerStateEvent> get(Worker.State state) {
        ObjectProperty<EventHandler<WorkerStateEvent>> slot = slots.get(state.ordinal());
        return slot == null ? null : slot.get();
    }

    /// Delivers the event of the owner entering a state: through the
    /// dispatch chain to the filters and handlers added by type, then to
    /// the handler set for the state.
    void fire(Worker worker, Worker.State state) {
        WorkerStateEvent event = new WorkerStateEvent(worker, type(state));
        if (worker instanceof javafx.event.EventTarget) {
            javafx.event.Event.fireEvent((javafx.event.EventTarget) worker, event);
        }
        EventHandler<WorkerStateEvent> handler = get(state);
        if (handler != null && !event.isConsumed()) {
            handler.handle(event);
        }
    }
}

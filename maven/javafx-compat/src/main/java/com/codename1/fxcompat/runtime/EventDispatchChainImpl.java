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
package com.codename1.fxcompat.runtime;

import java.util.ArrayList;

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;

/// The dispatch chain events travel along: an ordered list of dispatchers
/// and the position of the next one to call.
///
/// Dispatching hands the event to the dispatcher at the current position
/// together with the chain itself as the tail, moved one position on; a
/// dispatcher that passes the event to the tail thereby reaches the next
/// dispatcher. The position is restored when the dispatcher returns, so the
/// chain can be dispatched through again.
public final class EventDispatchChainImpl implements EventDispatchChain {

    private final ArrayList<EventDispatcher> dispatchers = new ArrayList<EventDispatcher>();
    private int next;

    /// Creates an empty chain.
    public EventDispatchChainImpl() {
    }

    @Override
    public EventDispatchChain append(EventDispatcher eventDispatcher) {
        if (eventDispatcher == null) {
            throw new NullPointerException();
        }
        dispatchers.add(eventDispatcher);
        return this;
    }

    @Override
    public EventDispatchChain prepend(EventDispatcher eventDispatcher) {
        if (eventDispatcher == null) {
            throw new NullPointerException();
        }
        dispatchers.add(next, eventDispatcher);
        return this;
    }

    @Override
    public Event dispatchEvent(Event event) {
        if (next >= dispatchers.size()) {
            return event;
        }
        int position = next;
        next = position + 1;
        try {
            return dispatchers.get(position).dispatchEvent(event, this);
        } finally {
            next = position;
        }
    }
}

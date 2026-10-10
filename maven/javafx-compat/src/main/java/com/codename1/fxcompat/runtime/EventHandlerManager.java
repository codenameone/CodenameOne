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
import java.util.HashMap;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;

/// The filters and handlers of one event target, and its stop on the
/// dispatch chain.
///
/// An event travels down the chain from the window to its target, the
/// capturing phase, in which each stop runs its filters; then back up, the
/// bubbling phase, in which each stop runs its handlers. Consuming the
/// event in either phase ends its journey: later stops never see it.
/// At one stop every filter (or handler) for the event's type runs, most
/// specific type first, the ones added with `addEventHandler` before the
/// one set through a convenience property such as `setOnAction`; a
/// handler consuming the event does not keep its neighbours at the same
/// stop from running.
///
/// Each stop sees the event with itself as the source.
public final class EventHandlerManager implements EventDispatcher {

    private static boolean consumedByFilter;

    private final Object owner;
    private HashMap<EventType<?>, ArrayList<EventHandler<?>>> filters;
    private HashMap<EventType<?>, ArrayList<EventHandler<?>>> handlers;
    private HashMap<EventType<?>, ObjectProperty<?>> slots;

    /// Creates the manager of an event target.
    public EventHandlerManager(Object owner) {
        this.owner = owner;
    }

    /// Clears the record of a filter having consumed an event.
    public static void resetConsumedByFilter() {
        consumedByFilter = false;
    }

    /// Returns whether an event was consumed by a filter, in the capturing
    /// phase, since the record was last cleared.
    public static boolean wasConsumedByFilter() {
        return consumedByFilter;
    }

    private static HashMap<EventType<?>, ArrayList<EventHandler<?>>> add(
            HashMap<EventType<?>, ArrayList<EventHandler<?>>> map, EventType<?> type, EventHandler<?> handler) {
        if (type == null) {
            throw new NullPointerException("Event type must not be null");
        }
        if (handler == null) {
            throw new NullPointerException("Event handler must not be null");
        }
        HashMap<EventType<?>, ArrayList<EventHandler<?>>> m = map;
        if (m == null) {
            m = new HashMap<EventType<?>, ArrayList<EventHandler<?>>>();
        }
        ArrayList<EventHandler<?>> list = m.get(type);
        if (list == null) {
            list = new ArrayList<EventHandler<?>>();
            m.put(type, list);
        }
        if (!list.contains(handler)) {
            list.add(handler);
        }
        return m;
    }

    private static void remove(HashMap<EventType<?>, ArrayList<EventHandler<?>>> map, EventType<?> type,
            EventHandler<?> handler) {
        if (type == null) {
            throw new NullPointerException("Event type must not be null");
        }
        if (handler == null) {
            throw new NullPointerException("Event handler must not be null");
        }
        if (map != null) {
            ArrayList<EventHandler<?>> list = map.get(type);
            if (list != null) {
                list.remove(handler);
            }
        }
    }

    /// Adds a handler for a type and its sub types.
    public <T extends Event> void addEventHandler(EventType<T> type, EventHandler<? super T> handler) {
        handlers = add(handlers, type, handler);
    }

    /// Removes a handler.
    public <T extends Event> void removeEventHandler(EventType<T> type, EventHandler<? super T> handler) {
        remove(handlers, type, handler);
    }

    /// Adds a filter for a type and its sub types.
    public <T extends Event> void addEventFilter(EventType<T> type, EventHandler<? super T> filter) {
        filters = add(filters, type, filter);
    }

    /// Removes a filter.
    public <T extends Event> void removeEventFilter(EventType<T> type, EventHandler<? super T> filter) {
        remove(filters, type, filter);
    }

    /// Returns the property behind a convenience handler such as
    /// `onAction`, creating it the first time.
    @SuppressWarnings("unchecked")
    public <T extends Event> ObjectProperty<EventHandler<? super T>> slot(EventType<T> type, String name) {
        if (slots == null) {
            slots = new HashMap<EventType<?>, ObjectProperty<?>>();
        }
        ObjectProperty<?> existing = slots.get(type);
        if (existing == null) {
            existing = new SimpleObjectProperty<EventHandler<? super T>>(owner, name);
            slots.put(type, existing);
        }
        return (ObjectProperty<EventHandler<? super T>>) existing;
    }

    /// Returns the convenience handler of a type, or `null`.
    @SuppressWarnings("unchecked")
    public <T extends Event> EventHandler<? super T> getSlot(EventType<T> type) {
        if (slots == null) {
            return null;
        }
        ObjectProperty<?> p = slots.get(type);
        Object value = p == null ? null : p.get();
        return value instanceof EventHandler ? (EventHandler<? super T>) value : null;
    }

    /// Sets the convenience handler of a type.
    public <T extends Event> void setSlot(EventType<T> type, String name, EventHandler<? super T> handler) {
        if (handler == null && (slots == null || !slots.containsKey(type))) {
            return;
        }
        slot(type, name).set(handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fire(HashMap<EventType<?>, ArrayList<EventHandler<?>>> map, Event event, boolean withSlots) {
        EventType<?> type = event.getEventType();
        while (type != null) {
            ArrayList<EventHandler<?>> list = map == null ? null : map.get(type);
            if (list != null && !list.isEmpty()) {
                EventHandler[] snapshot = list.toArray(new EventHandler[list.size()]);
                for (int i = 0; i < snapshot.length; i++) {
                    snapshot[i].handle(event);
                }
            }
            if (withSlots && slots != null) {
                ObjectProperty<?> p = slots.get(type);
                Object h = p == null ? null : p.get();
                if (h instanceof EventHandler) {
                    ((EventHandler) h).handle(event);
                }
            }
            type = type.getSuperType();
        }
    }

    /// Returns the event as this stop delivers it: addressed to the owner,
    /// and an entered or exited event that reached its own target typed as
    /// the kind only the target sees.
    private Event localize(Event event) {
        EventTarget target = event.getTarget();
        if (event instanceof MouseEvent && target == owner) {
            MouseEvent mouse = (MouseEvent) event;
            if (mouse.getEventType() == MouseEvent.MOUSE_ENTERED_TARGET) {
                return mouse.copyFor(owner, target, MouseEvent.MOUSE_ENTERED);
            } else if (mouse.getEventType() == MouseEvent.MOUSE_EXITED_TARGET) {
                return mouse.copyFor(owner, target, MouseEvent.MOUSE_EXITED);
            }
        }
        return event.getSource() == owner ? event : event.copyFor(owner, target);
    }

    private static void noteConsumedByFilter() {
        consumedByFilter = true;
    }

    @Override
    public Event dispatchEvent(Event event, EventDispatchChain tail) {
        if (filters != null) {
            Event capturing = localize(event);
            fire(filters, capturing, false);
            if (capturing.isConsumed()) {
                noteConsumedByFilter();
                return null;
            }
        }
        Event result = tail.dispatchEvent(event);
        if (result == null) {
            return null;
        }
        if (handlers != null || slots != null) {
            Event bubbling = localize(result);
            fire(handlers, bubbling, true);
            if (bubbling.isConsumed()) {
                return null;
            }
        }
        return result;
    }
}

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
package javafx.event;

import java.util.EventObject;

import com.codename1.fxcompat.runtime.EventDispatchChainImpl;

import javafx.beans.NamedArg;

/// Base class of every event: a type, the source the handler sees it coming
/// from, and the target it is being delivered to.
///
/// An event is copied for each object it is delivered through. A subclass
/// that adds state must override [#copyFor(Object, EventTarget)] to build
/// the copy with its own constructor; one that does not is re-addressed in
/// place instead, because this runtime cannot clone an object of a class it
/// does not know.
public class Event extends EventObject {

    private static final long serialVersionUID = 1L;

    /// The target of an event that has none.
    public static final EventTarget NULL_SOURCE_TARGET = new EventTarget() {
        @Override
        public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
            return tail;
        }
    };

    /// The type every event has.
    public static final EventType<Event> ANY = EventType.ROOT;

    /// The type of the event.
    protected EventType<? extends Event> eventType;

    /// The target the event is being delivered to.
    protected transient EventTarget target;

    /// Whether a handler consumed the event.
    protected boolean consumed;

    /// Creates an event of a type with no source and no target.
    public Event(final @NamedArg("eventType") EventType<? extends Event> eventType) {
        this(null, null, eventType);
    }

    /// Creates an event. A missing source or target is replaced by
    /// [#NULL_SOURCE_TARGET].
    public Event(final @NamedArg("source") Object source, final @NamedArg("target") EventTarget target,
            final @NamedArg("eventType") EventType<? extends Event> eventType) {
        super((source != null) ? source : NULL_SOURCE_TARGET);
        this.target = (target != null) ? target : NULL_SOURCE_TARGET;
        this.eventType = eventType;
    }

    /// Returns the target the event is being delivered to.
    public EventTarget getTarget() {
        return target;
    }

    /// Returns the type of the event.
    public EventType<? extends Event> getEventType() {
        return eventType;
    }

    /// Returns this event addressed to a new source and target. A missing
    /// source or target is replaced by [#NULL_SOURCE_TARGET].
    public Event copyFor(final Object newSource, final EventTarget newTarget) {
        Object resolvedSource = (newSource != null) ? newSource : NULL_SOURCE_TARGET;
        EventTarget resolvedTarget = (newTarget != null) ? newTarget : NULL_SOURCE_TARGET;
        if (getClass() == Event.class) {
            Event copy = new Event(resolvedSource, resolvedTarget, eventType);
            copy.consumed = consumed;
            return copy;
        }
        this.source = resolvedSource;
        this.target = resolvedTarget;
        return this;
    }

    /// Returns whether a handler consumed the event.
    public boolean isConsumed() {
        return consumed;
    }

    /// Marks the event consumed, which stops its delivery.
    public void consume() {
        consumed = true;
    }

    /// Delivers an event to a target, through the dispatch chain the
    /// target builds.
    public static void fireEvent(EventTarget eventTarget, Event event) {
        if (eventTarget == null) {
            throw new NullPointerException("Event target must not be null!");
        }
        if (event == null) {
            throw new NullPointerException("Event must not be null!");
        }
        Event addressed = event.getTarget() == eventTarget ? event : event.copyFor(eventTarget, eventTarget);
        EventDispatchChain chain = eventTarget.buildEventDispatchChain(new EventDispatchChainImpl());
        chain.dispatchEvent(addressed);
    }
}

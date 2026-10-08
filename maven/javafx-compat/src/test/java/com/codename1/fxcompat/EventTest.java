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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.codename1.fxcompat.runtime.EventDispatchChainImpl;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.event.WeakEventHandler;

public class EventTest {

    private final List<String> log = new ArrayList<String>();

    private EventDispatcher named(final String name, final boolean consume) {
        return new EventDispatcher() {
            @Override
            public Event dispatchEvent(Event event, EventDispatchChain tail) {
                log.add(name + ">");
                if (consume) {
                    event.consume();
                    return null;
                }
                Event result = tail.dispatchEvent(event);
                log.add("<" + name);
                return result;
            }
        };
    }

    @Test
    public void eventTypeHierarchy() {
        assertNull(EventType.ROOT.getSuperType());
        assertEquals("EVENT", EventType.ROOT.getName());
        assertSame(EventType.ROOT, Event.ANY);
        assertSame(Event.ANY, ActionEvent.ACTION.getSuperType());
        assertEquals("ACTION", ActionEvent.ACTION.getName());
        assertEquals("ACTION", ActionEvent.ACTION.toString());
        assertSame(ActionEvent.ACTION, ActionEvent.ANY);
        EventType<Event> custom = new EventType<Event>("CUSTOM");
        assertSame(EventType.ROOT, custom.getSuperType());
        EventType<Event> child = new EventType<Event>(custom, "CHILD");
        assertSame(custom, child.getSuperType());
        assertEquals("CHILD", child.getName());
        EventType<Event> unnamed = new EventType<Event>(custom);
        assertNull(unnamed.getName());
        assertTrue(unnamed.toString().length() > 0);
        try {
            new EventType<Event>((EventType<Event>) null, "X");
            fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void eventBasics() {
        Event bare = new Event(Event.ANY);
        assertSame(Event.NULL_SOURCE_TARGET, bare.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, bare.getTarget());
        assertSame(Event.ANY, bare.getEventType());
        assertFalse(bare.isConsumed());
        bare.consume();
        assertTrue(bare.isConsumed());
        EventTarget target = tail -> tail;
        Object source = new Object();
        Event event = new Event(source, target, ActionEvent.ACTION);
        assertSame(source, event.getSource());
        assertSame(target, event.getTarget());
        Event copy = event.copyFor("other", null);
        assertNotSame(event, copy);
        assertEquals("other", copy.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, copy.getTarget());
        assertSame(ActionEvent.ACTION, copy.getEventType());
        assertSame("the original is untouched", source, event.getSource());
    }

    @Test
    public void actionEvent() {
        ActionEvent bare = new ActionEvent();
        assertSame(ActionEvent.ACTION, bare.getEventType());
        assertSame(Event.NULL_SOURCE_TARGET, bare.getTarget());
        EventTarget target = tail -> tail;
        ActionEvent event = new ActionEvent("button", target);
        assertEquals("button", event.getSource());
        ActionEvent copy = event.copyFor("menu", target);
        assertNotSame(event, copy);
        assertEquals("menu", copy.getSource());
        assertSame(target, copy.getTarget());
        assertSame(ActionEvent.ACTION, copy.getEventType());
    }

    @Test
    public void subclassWithoutCopyForIsReaddressed() {
        class Custom extends Event {
            private static final long serialVersionUID = 1L;
            final String payload = "p";

            Custom() {
                super(Event.ANY);
            }
        }
        Custom custom = new Custom();
        Event copy = custom.copyFor("s", null);
        assertTrue(copy instanceof Custom);
        assertEquals("p", ((Custom) copy).payload);
        assertEquals("s", copy.getSource());
    }

    @Test
    public void dispatchChainRunsInOrderAndUnwinds() {
        EventDispatchChain chain = new EventDispatchChainImpl();
        chain = chain.append(named("b", false));
        chain = chain.append(named("c", false));
        chain = chain.prepend(named("a", false));
        Event event = new Event(Event.ANY);
        assertSame(event, chain.dispatchEvent(event));
        assertEquals("[a>, b>, c>, <c, <b, <a]", log.toString());
        log.clear();
        chain.dispatchEvent(event);
        assertEquals("a chain can be dispatched through again", 6, log.size());
    }

    @Test
    public void consumingDispatcherStopsTheChain() {
        EventDispatchChain chain = new EventDispatchChainImpl().append(named("a", false))
                .append(named("stop", true)).append(named("never", false));
        Event event = new Event(Event.ANY);
        assertNull(chain.dispatchEvent(event));
        assertTrue(event.isConsumed());
        assertEquals("[a>, stop>, <a]", log.toString());
        assertSame("an empty chain returns the event", event, new EventDispatchChainImpl().dispatchEvent(event));
    }

    @Test
    public void fireEventGoesThroughTheTargetsChain() {
        final List<Event> seen = new ArrayList<Event>();
        final EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                seen.add(event);
            }
        };
        EventTarget target = new EventTarget() {
            @Override
            public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
                return tail.prepend(new EventDispatcher() {
                    @Override
                    public Event dispatchEvent(Event event, EventDispatchChain rest) {
                        if (event instanceof ActionEvent) {
                            handler.handle((ActionEvent) event);
                        }
                        return rest.dispatchEvent(event);
                    }
                });
            }
        };
        ActionEvent original = new ActionEvent();
        Event.fireEvent(target, original);
        assertEquals(1, seen.size());
        assertSame("the event is addressed to the target", target, seen.get(0).getTarget());
        assertSame(target, seen.get(0).getSource());
        assertSame(Event.NULL_SOURCE_TARGET, original.getTarget());
        try {
            Event.fireEvent(null, original);
            fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            assertTrue(true);
        }
        try {
            Event.fireEvent(target, null);
            fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void weakEventHandlerForwards() {
        final int[] calls = new int[1];
        EventHandler<ActionEvent> strong = event -> calls[0]++;
        WeakEventHandler<ActionEvent> weak = new WeakEventHandler<ActionEvent>(strong);
        weak.handle(new ActionEvent());
        assertEquals(1, calls[0]);
        assertFalse(weak.wasGarbageCollected());
    }
}

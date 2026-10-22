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
package com.codename1.flutter.plugins.bloc;

/// bloc's `Transition<Event, State>`: a [Change] together with the event that caused it.
public class Transition<E, S> extends Change<S> {
    private final E event;

    public Transition(S currentState, E event, S nextState) {
        super(currentState, nextState);
        this.event = event;
    }

    public E event() {
        return event;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Transition && super.equals(o)
                && dart.runtime.DartRuntime.eq(event, ((Transition<?, ?>) o).event);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 + (event == null ? 0 : event.hashCode());
    }

    @Override
    public String toString() {
        return "Transition { currentState: " + currentState() + ", event: " + event
                + ", nextState: " + nextState() + " }";
    }
}

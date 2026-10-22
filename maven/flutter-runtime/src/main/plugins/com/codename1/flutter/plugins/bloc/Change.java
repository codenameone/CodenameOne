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

import dart.runtime.DartRuntime;

/// bloc's `Change<State>`: one state replacing another.
public class Change<S> {
    private final S currentState;
    private final S nextState;

    public Change(S currentState, S nextState) {
        this.currentState = currentState;
        this.nextState = nextState;
    }

    public S currentState() {
        return currentState;
    }

    public S nextState() {
        return nextState;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Change)) {
            return false;
        }
        Change<?> c = (Change<?>) o;
        return DartRuntime.eq(currentState, c.currentState) && DartRuntime.eq(nextState, c.nextState);
    }

    @Override
    public int hashCode() {
        return (currentState == null ? 0 : currentState.hashCode()) * 31
                + (nextState == null ? 0 : nextState.hashCode());
    }

    @Override
    public String toString() {
        return "Change { currentState: " + currentState + ", nextState: " + nextState + " }";
    }
}

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
package com.codename1.scheduling;

/// A link between two tasks: the successor waits for the predecessor.
///
/// `lag` is working time added to the wait, and a negative lag is a lead: a
/// finish-to-start link with a lag of minus one day lets the successor start
/// a day before the predecessor finishes.
public final class TaskDependency {
    /// Which end of the predecessor holds which end of the successor.
    public enum Type {
        /// The successor starts after the predecessor finishes. The usual one.
        FINISH_TO_START,
        /// The successor starts after the predecessor starts.
        START_TO_START,
        /// The successor finishes after the predecessor finishes.
        FINISH_TO_FINISH,
        /// The successor finishes after the predecessor starts.
        START_TO_FINISH
    }

    private final GanttTask predecessor;
    private final GanttTask successor;
    private Type type;
    private long lag;

    TaskDependency(GanttTask predecessor, GanttTask successor, Type type, long lag) {
        this.predecessor = predecessor;
        this.successor = successor;
        this.type = type == null ? Type.FINISH_TO_START : type;
        this.lag = lag;
    }

    public GanttTask getPredecessor() {
        return predecessor;
    }

    public GanttTask getSuccessor() {
        return successor;
    }

    public Type getType() {
        return type;
    }

    public TaskDependency setType(Type v) {
        type = v == null ? Type.FINISH_TO_START : v;
        return this;
    }

    /// Working time in milliseconds; negative for a lead.
    public long getLag() {
        return lag;
    }

    public TaskDependency setLag(long millis) {
        lag = millis;
        return this;
    }

    /// Whether the link holds the successor's start (and not its finish).
    public boolean constrainsStart() {
        return type == Type.FINISH_TO_START || type == Type.START_TO_START;
    }

    /// Whether the link is measured from the predecessor's finish.
    public boolean fromFinish() {
        return type == Type.FINISH_TO_START || type == Type.FINISH_TO_FINISH;
    }
}

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
package androidx.work;

import androidx.lifecycle.LiveData;

import java.util.concurrent.Future;

/// The outcome of an enqueue or cancel. Both are applied on the main thread,
/// so the state is already `SUCCESS` once the main thread has handled it.
public interface Operation {

    State.SUCCESS SUCCESS = new State.SUCCESS();
    State.IN_PROGRESS IN_PROGRESS = new State.IN_PROGRESS();

    LiveData<State> getState();

    Future<State.SUCCESS> getResult();

    abstract class State {
        State() {
        }

        public static final class SUCCESS extends State {
            SUCCESS() {
            }

            @Override
            public String toString() {
                return "SUCCESS";
            }
        }

        public static final class IN_PROGRESS extends State {
            IN_PROGRESS() {
            }

            @Override
            public String toString() {
                return "IN_PROGRESS";
            }
        }

        public static final class FAILURE extends State {
            private final Throwable exception;

            public FAILURE(Throwable exception) {
                this.exception = exception;
            }

            public Throwable getThrowable() {
                return exception;
            }

            @Override
            public String toString() {
                return "FAILURE (" + exception + ")";
            }
        }
    }
}

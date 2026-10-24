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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;

/// Listeners that record what they are told, shared by the tests.
public final class Probe {

    private Probe() {
    }

    /// Counts invalidations.
    public static final class Invalidations implements InvalidationListener {
        public int count;
        public Observable last;

        @Override
        public void invalidated(Observable observable) {
            count++;
            last = observable;
        }
    }

    /// Records every change as `old->new`.
    public static final class Values<T> implements ChangeListener<T> {
        public final List<String> log = new ArrayList<String>();

        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            log.add(oldValue + "->" + newValue);
        }
    }

    /// Records every list change, one entry per step.
    public static final class ListChanges<E> implements ListChangeListener<E> {
        public final List<String> log = new ArrayList<String>();
        public int events;

        @Override
        public void onChanged(Change<? extends E> change) {
            events++;
            while (change.next()) {
                StringBuilder step = new StringBuilder();
                if (change.wasPermutated()) {
                    step.append("perm[").append(change.getFrom()).append(',').append(change.getTo()).append("]{");
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        if (i > change.getFrom()) {
                            step.append(',');
                        }
                        step.append(change.getPermutation(i));
                    }
                    step.append('}');
                } else if (change.wasUpdated()) {
                    step.append("upd[").append(change.getFrom()).append(',').append(change.getTo()).append(']');
                } else if (change.wasReplaced()) {
                    step.append("rep[").append(change.getFrom()).append(',').append(change.getTo()).append("] -")
                            .append(change.getRemoved()).append(" +").append(change.getAddedSubList());
                } else if (change.wasAdded()) {
                    step.append("add[").append(change.getFrom()).append(',').append(change.getTo()).append("] +")
                            .append(change.getAddedSubList());
                } else if (change.wasRemoved()) {
                    step.append("rem[").append(change.getFrom()).append("] -").append(change.getRemoved());
                } else {
                    step.append("none");
                }
                log.add(step.toString());
            }
        }

        public String take() {
            String result = log.toString();
            log.clear();
            return result;
        }
    }

    /// Runs the collector until a weak reference clears; returns whether it
    /// did.
    public static boolean collected(WeakReference ref) {
        for (int i = 0; i < 200 && ref.get() != null; i++) {
            System.gc();
            byte[][] pressure = new byte[32][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException interrupted) {
                return false;
            }
        }
        return ref.get() == null;
    }
}

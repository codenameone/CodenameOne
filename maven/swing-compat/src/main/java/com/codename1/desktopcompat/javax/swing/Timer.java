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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.EventQueue;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/// Fires action events on the event dispatch thread after a delay, once
/// or repeatedly. Start and stop it from the event dispatch thread.
public class Timer {

    protected EventListenerList listenerList = new EventListenerList();

    private int delay;
    private int initialDelay;
    private boolean repeats = true;
    private boolean coalesce = true;
    private String actionCommand;
    private java.util.Timer timer;
    private int run;

    public Timer(int delay, ActionListener listener) {
        this.delay = delay;
        this.initialDelay = delay;
        if (listener != null) {
            addActionListener(listener);
        }
    }

    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    protected void fireActionPerformed(ActionEvent e) {
        ActionListener[] ls = getActionListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].actionPerformed(e);
        }
    }

    public int getDelay() {
        return delay;
    }

    /// Takes effect the next time the timer is started.
    public void setDelay(int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Invalid delay: " + delay);
        }
        this.delay = delay;
    }

    public int getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(int initialDelay) {
        if (initialDelay < 0) {
            throw new IllegalArgumentException("Invalid initial delay: " + initialDelay);
        }
        this.initialDelay = initialDelay;
    }

    public boolean isRepeats() {
        return repeats;
    }

    public void setRepeats(boolean flag) {
        repeats = flag;
    }

    public boolean isCoalesce() {
        return coalesce;
    }

    public void setCoalesce(boolean flag) {
        coalesce = flag;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public void setActionCommand(String command) {
        actionCommand = command;
    }

    public boolean isRunning() {
        return timer != null;
    }

    public void start() {
        if (timer != null) {
            return;
        }
        run++;
        final int mine = run;
        final AtomicInteger pending = new AtomicInteger();
        final Runnable tick = new Runnable() {
            @Override
            public void run() {
                tick(mine, pending.getAndSet(0));
            }
        };
        TimerTask task = new Hop(tick, pending);
        timer = new java.util.Timer();
        // Always periodic: whether the timer repeats is asked at each
        // firing, so setRepeats(true) on a running one-shot timer takes
        // effect, as it does in Swing. A timer that does not repeat stops
        // itself at its first tick.
        timer.schedule(task, initialDelay, Math.max(1, delay));
    }

    /// Runs on the event dispatch thread; a tick queued before the timer
    /// was stopped or restarted is dropped. `due` is how many periods
    /// elapsed since the last one ran: a busy event dispatch thread gets
    /// one event for all of them unless coalescing is off.
    private void tick(int ticket, int due) {
        int fire = coalesce ? 1 : due;
        for (int i = 0; i < fire; i++) {
            if (timer == null || ticket != run) {
                return;
            }
            if (!repeats) {
                stop();
            }
            fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand,
                    System.currentTimeMillis(), 0));
        }
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void restart() {
        stop();
        start();
    }

    /// Carries a tick from the timer's thread to the event dispatch thread.
    /// It counts the periods that elapsed and queues one runnable for all
    /// of those the event dispatch thread has not got to yet, so a slow
    /// listener cannot build a backlog. The count is the only state the
    /// two threads share.
    private static final class Hop extends TimerTask {
        private final Runnable tick;
        private final AtomicInteger pending;

        Hop(Runnable tick, AtomicInteger pending) {
            this.tick = tick;
            this.pending = pending;
        }

        @Override
        public void run() {
            if (pending.getAndIncrement() == 0) {
                EventQueue.invokeLater(tick);
            }
        }
    }
}

/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.rbx;

import com.codename1.luau.Coroutine;
import com.codename1.luau.Interp;
import com.codename1.luau.LuaError;
import com.codename1.luau.LuaFunction;
import com.codename1.luau.Utf8;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Runs every script thread: `task.spawn/defer/delay/wait`, signal handlers
/// and waits. There is one OS thread -- whoever calls [#step] -- and threads
/// are interpreter coroutines, so nothing here is concurrent.
///
/// Each resume runs under an instruction budget. A thread that exhausts it
/// is killed with the engine's timeout message and counted in
/// [#getBudgetKills], which the creator dashboard and the admin health page
/// report.
public final class Scheduler {
    /// Where script output and errors go: the studio console, a server log.
    public interface Output {
        void message(String type, String text, Object owner);
    }

    private static final class Sleeper {
        final Coroutine co;
        final double wake;
        final double start;
        final LuaFunction fn;
        final Object[] args;

        Sleeper(Coroutine co, double wake, double start, LuaFunction fn, Object[] args) {
            this.co = co;
            this.wake = wake;
            this.start = start;
            this.fn = fn;
            this.args = args;
        }
    }

    private final Interp in;
    private double now;
    private long budget = 5_000_000L;
    private int depth;
    private long budgetKills;
    private long errors;
    private Output out;
    private final List<Sleeper> sleepers = new ArrayList<Sleeper>();
    private final List<Object[]> deferred = new ArrayList<Object[]>();
    private final Set<Coroutine> cancelled = new HashSet<Coroutine>();

    public Scheduler(Interp in) {
        this.in = in;
        in.setBudget(budget);
        // The interpreter's own print falls back to System.out when no
        // printer is installed. Scripts here are untrusted (a creator's place
        // on a shared server), so every print path must end in this
        // scheduler's Output sink, which drops output when none is set,
        // never on the backend's stdout. Lines are capped so one call cannot
        // hand the sink an unbounded string.
        in.setPrinter(new Interp.Printer() {
            @Override
            public void print(String line) {
                String text = Utf8.toJava(line);
                if (text.length() > MAX_PRINT_CHARS) {
                    text = text.substring(0, MAX_PRINT_CHARS) + "...";
                }
                Scheduler.this.print("output", text);
            }
        });
    }

    /// Longest line the interpreter's fallback print forwards.
    static final int MAX_PRINT_CHARS = 8192;

    public Interp getInterp() {
        return in;
    }

    public void setOutput(Output out) {
        this.out = out;
    }

    /// The current output sink.
    public Output getOutput() {
        return out;
    }

    /// Instructions one resume may run before it is killed.
    public void setBudget(long steps) {
        budget = steps;
        in.setBudget(steps);
    }

    public long getBudget() {
        return budget;
    }

    public long getBudgetKills() {
        return budgetKills;
    }

    public long getErrorCount() {
        return errors;
    }

    /// Seconds since the scheduler started.
    public double now() {
        return now;
    }

    public int pendingThreads() {
        return sleepers.size() + deferred.size();
    }

    /// The owner (normally a Script) of the running thread.
    public Object currentOwner() {
        Coroutine c = in.currentThread();
        return c == null ? null : c.getOwner();
    }

    public void print(String type, String text) {
        if (out != null) {
            out.message(type, text, currentOwner());
        }
    }

    public Coroutine spawn(LuaFunction fn, Object[] args) {
        Coroutine co = in.newThread(fn);
        co.setOwner(currentOwner());
        resume(co, args);
        return co;
    }

    /// Spawns with an explicit owner, as a script starting does.
    public Coroutine spawnOwned(LuaFunction fn, Object owner, Object[] args) {
        Coroutine co = in.newThread(fn);
        co.setOwner(owner);
        resume(co, args);
        return co;
    }

    public void resume(Coroutine co, Object[] args) {
        if (cancelled.remove(co) || co.getStatus() != Coroutine.SUSPENDED) {
            return;
        }
        if (depth == 0) {
            in.resetSteps();
        }
        depth++;
        try {
            Object[] r = in.resume(co, args);
            if (r.length > 0 && Boolean.FALSE.equals(r[0])) {
                errors++;
                Object e = r.length > 1 ? r[1] : null;
                report(co, e instanceof String ? (String) e : in.tostring(e));
            }
        } catch (LuaError fatal) {
            budgetKills++;
            errors++;
            report(co, "Script timeout: exhausted allowed execution time (" + fatal.getMessage() + ")");
        } finally {
            depth--;
        }
    }

    /// Reports an error against `owner` (a script that failed to compile).
    public void error(Object owner, String msg) {
        errors++;
        if (out != null) {
            out.message("error", msg, owner);
        }
    }

    private void report(Coroutine co, String msg) {
        if (out != null) {
            out.message("error", msg, co.getOwner());
        }
    }

    public void defer(Coroutine co, Object[] args) {
        deferred.add(new Object[] {co, args});
    }

    public Coroutine deferFn(LuaFunction fn, Object[] args) {
        Coroutine co = in.newThread(fn);
        co.setOwner(currentOwner());
        deferred.add(new Object[] {co, args});
        return co;
    }

    /// Suspends the running thread for `secs`; resumed with the time slept.
    public Object[] sleep(double secs) {
        Coroutine co = in.currentThread();
        sleepers.add(new Sleeper(co, now + Math.max(0, secs), now, null, null));
        return in.yield(new Object[0]);
    }

    public Coroutine delay(double secs, LuaFunction fn, Object[] args) {
        Coroutine co = in.newThread(fn);
        co.setOwner(currentOwner());
        sleepers.add(new Sleeper(co, now + Math.max(0, secs), now, fn, args));
        return co;
    }

    public void cancel(Coroutine co) {
        boolean queued = false;
        for (int i = sleepers.size() - 1; i >= 0; i--) {
            if (sleepers.get(i).co == co) { // NOPMD CompareObjectsWithEquals - thread identity
                sleepers.remove(i);
                queued = true;
            }
        }
        for (int i = deferred.size() - 1; i >= 0; i--) {
            if (deferred.get(i)[0] == co) { // NOPMD CompareObjectsWithEquals - thread identity
                deferred.remove(i);
                queued = true;
            }
        }
        if (!queued && co.getStatus() == Coroutine.SUSPENDED) {
            // Waiting on a signal: the signal resumes it later, so remember.
            cancelled.add(co);
        }
    }

    /// Stops every thread owned by `owner`, as disabling a script does.
    public void cancelOwnedBy(Object owner) {
        for (int i = sleepers.size() - 1; i >= 0; i--) {
            if (sleepers.get(i).co.getOwner() == owner) { // NOPMD CompareObjectsWithEquals - owner identity
                sleepers.remove(i);
            }
        }
        for (int i = deferred.size() - 1; i >= 0; i--) {
            if (((Coroutine) deferred.get(i)[0]).getOwner() == owner) { // NOPMD CompareObjectsWithEquals - owner identity
                deferred.remove(i);
            }
        }
    }

    /// Advances time by `dt` seconds and resumes everything now due.
    public void step(double dt) {
        now += dt;
        runDeferred();
        List<Sleeper> due = new ArrayList<Sleeper>();
        for (int i = 0; i < sleepers.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Sleeper s = sleepers.get(i);
            if (s.wake <= now + 1e-9) {
                due.add(s);
            }
        }
        if (due.isEmpty()) {
            return;
        }
        sleepers.removeAll(due);
        // Earliest first, stable for equal wake times: deterministic traces.
        for (int i = 1; i < due.size(); i++) {
            Sleeper s = due.get(i);
            int j = i - 1;
            while (j >= 0 && due.get(j).wake > s.wake) {
                due.set(j + 1, due.get(j));
                j--;
            }
            due.set(j + 1, s);
        }
        for (Sleeper s : due) {
            if (s.fn != null) {
                resume(s.co, s.args);
            } else {
                resume(s.co, new Object[] {Args.n(now - s.start), Args.n(now)});
            }
        }
        runDeferred();
    }

    /// Runs deferred threads (and any they defer, up to a bound).
    public void runDeferred() {
        int rounds = 0;
        while (!deferred.isEmpty() && rounds++ < 10) {
            Object[][] d = deferred.toArray(new Object[0][]);
            deferred.clear();
            for (Object[] e : d) {
                resume((Coroutine) e[0], (Object[]) e[1]);
            }
        }
    }
}

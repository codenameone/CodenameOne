/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.fieldline.scheduler;

/// The soft-constraint weights. Every cost is an integer so two runtimes always
/// agree on which of two schedules is better.
public final class Weights {
    /// Per minute of driving.
    public int travel = 10;
    /// Per minute past a technician's regular hours.
    public int overtime = 15;
    /// Times priority squared, per job left unplaced.
    public int unassigned = 20000;
    /// Per job not given to its preferred technician.
    public int continuity = 300;
    /// Per job moved off the technician that held it before a re-plan.
    public int disruption = 800;
    /// Per minute of idle waiting before a window opens.
    public int waiting = 1;
    /// Times the squared deviation (in hours) from the mean workload.
    public int balance = 40;
    /// Per minute a job finishes past its SLA deadline, times its priority.
    public int lateness = 30;
    /// Per job done outside the technician's territories, when territories
    /// are a preference rather than a rule.
    public int outOfTerritory = 400;
    /// When true a technician is never sent outside their territories.
    public boolean strictTerritories;
}

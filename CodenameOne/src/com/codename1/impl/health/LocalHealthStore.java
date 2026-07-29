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
package com.codename1.impl.health;

import com.codename1.health.AggregateMetric;
import com.codename1.health.AggregateQuery;
import com.codename1.health.AggregateResult;
import com.codename1.health.HealthAggregationStyle;
import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthException;
import com.codename1.health.HealthError;
import com.codename1.health.HealthDeleteRequest;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthTimeRange;
import com.codename1.health.HealthWriteResult;
import com.codename1.health.BloodPressureSample;
import com.codename1.health.CategorySample;
import com.codename1.health.HealthQuantity;
import com.codename1.health.QuantitySample;
import com.codename1.health.SeriesSample;
import com.codename1.health.SessionSample;
import com.codename1.health.SleepSample;
import com.codename1.health.WorkoutSample;
import com.codename1.health.nutrition.Nutrient;
import com.codename1.health.nutrition.NutritionSample;
import com.codename1.health.SampleQuery;
import com.codename1.health.SamplePage;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/// A health store held in the app's own process, for ports with no
/// platform health provider -- the desktop ports, the JavaScript port, and
/// the simulator's starting state.
///
/// #### What this is and is not
///
/// It is a real store: reads, writes, deletes and aggregates all work, and
/// on ports that persist it the data survives a restart. That makes it
/// genuinely useful -- aggregation logic, chart code and unit handling can
/// all be developed and unit-tested on a laptop.
///
/// It is **not** a platform health store, and it reports
/// [com.codename1.health.HealthAvailability#LOCAL_ONLY] so apps can tell.
/// Nothing else writes into it: no watch, no scale, no other app. An app
/// whose whole purpose is reading what other apps recorded should tell the
/// user the feature needs a phone rather than showing an empty chart.
///
/// Subclasses supply persistence; this base keeps everything in memory,
/// which is the right behaviour for a simulator that should start clean.
public class LocalHealthStore extends HealthStore {

    /// Held across a mutation *and* the persist it triggers, so the two
    /// are one transaction.
    ///
    /// `samples` alone is not enough: it is released between changing the
    /// list and encoding it, so two concurrent mutations could each
    /// snapshot, then write in the opposite order, and the older snapshot
    /// landed last. Both callers were told they had succeeded and a
    /// deleted record came back on the next launch.
    ///
    /// Always taken *before* `samples`, never the other way round. Reads
    /// take `samples` on its own and do not come here, so they are not
    /// blocked by a save.
    private final Object mutationLock = new Object();

    private final List<HealthSample> samples = new ArrayList<HealthSample>();
    private long nextId = 1;

    @Override
    public boolean isSupported() {
        return true;
    }

    /// Every type is available locally: there is no platform to restrict
    /// what can be stored.
    @Override
    public boolean isTypeSupported(HealthDataType type) {
        return type != null;
    }

    @Override
    public List<HealthDataType> getSupportedTypes() {
        return HealthDataType.values();
    }

    @Override
    public boolean isWritable(HealthDataType type) {
        return type != null;
    }

    @Override
    public boolean isDeletable(HealthDataType type) {
        return type != null;
    }

    /// Every metric is computed here in shared code rather than delegated
    /// to a platform engine.
    @Override
    public List<AggregateMetric> getSupportedMetrics(HealthDataType type) {
        List<AggregateMetric> out = new ArrayList<AggregateMetric>();
        if (type == null) {
            return out;
        }
        out.add(AggregateMetric.COUNT);
        out.add(AggregateMetric.DURATION);
        if (type.getAggregationStyle() == HealthAggregationStyle.CUMULATIVE) {
            out.add(AggregateMetric.TOTAL);
        } else if (type.getAggregationStyle()
                == HealthAggregationStyle.DISCRETE) {
            out.add(AggregateMetric.AVERAGE);
            out.add(AggregateMetric.MINIMUM);
            out.add(AggregateMetric.MAXIMUM);
            out.add(AggregateMetric.LATEST);
        }
        return out;
    }

    /// There is no permission model on a local store, so both directions
    /// are honestly reported as authorized rather than as unknown.
    @Override
    public HealthAuthorizationStatus getReadAuthorizationStatus(
            HealthDataType type) {
        return HealthAuthorizationStatus.AUTHORIZED;
    }

    @Override
    public HealthAuthorizationStatus getWriteAuthorizationStatus(
            HealthDataType type) {
        return HealthAuthorizationStatus.AUTHORIZED;
    }

    @Override
    protected void doRequestAuthorization(
            List<com.codename1.health.HealthAccess> access,
            AsyncResource<Boolean> out) {
        completeInline(out, Boolean.TRUE);
    }

    // ------------------------------------------------------------------
    // reads
    // ------------------------------------------------------------------

    @Override
    protected void doReadSamples(SampleQuery query,
            AsyncResource<SamplePage> out) {
        HealthTimeRange range =
                query.getTimeRange().resolve(System.currentTimeMillis());
        List<HealthSample> matched = new ArrayList<HealthSample>();
        synchronized (samples) {
            for (HealthSample s : samples) {
                if (isVisible(s) && matches(s, query, range)) {
                    // A copy, not the stored object. Query results are
                    // snapshots: handing out the live record let caller
                    // code mutate the store through setId/setSource/
                    // putMetadata and see the change on later reads.
                    matched.add(snapshot(s));
                }
            }
        }
        sort(matched, query.isSortDescending());
        // Measurements, not records. A series record holds many readings
        // and the shared layer expands it when flattening is on, so
        // counting containers let one page of ten records mean half a
        // million samples -- the limit exists to bound exactly that, and
        // this store is the one backing desktop, the browser and the
        // simulator, where the heap is not generous.
        //
        // Whole records only, matching the trim the shared layer performs
        // after flattening: the measurements of one series share an
        // identifier, and this store hands back no continuation token, so
        // anything cut from the middle of a record is gone for good. The
        // last record is therefore kept entire and may overshoot.
        boolean flatten = query.isFlattenSeries();
        int limit = query.getLimit();
        boolean truncated = false;
        List<HealthSample> page = new ArrayList<HealthSample>();
        int counted = 0;
        for (HealthSample s : matched) {
            if (counted >= limit) {
                truncated = true;
                break;
            }
            page.add(s);
            counted += weigh(s, flatten);
        }
        matched = page;
        // Everything is in memory, so a single page always satisfies the
        // query; there is no continuation token to hand back.
        completeInline(out, new SamplePage(matched, null, truncated));
    }

    /// How many samples `s` becomes once the shared layer is done with
    /// it: one, or the size of the series when flattening is on.
    private static int weigh(HealthSample s, boolean flatten) {
        if (flatten && s instanceof SeriesSample) {
            return Math.max(1, ((SeriesSample) s).size());
        }
        return 1;
    }

    private boolean matches(HealthSample s, SampleQuery query,
            HealthTimeRange range) {
        if (!query.getTypes().contains(s.getType())) {
            return false;
        }
        // A sample belongs to the range when it overlaps it, so an
        // interval straddling the boundary is not silently dropped.
        //
        // Half-open at both ends: an interval ending exactly at the start
        // has zero overlap and belongs to the previous range, so steps
        // ending at midnight are yesterday's. An instantaneous sample at
        // the inclusive start is still inside.
        if (s.getStartMillis() >= range.getEndMillis()) {
            return false;
        }
        if (s.isInstantaneous() ? s.getEndMillis() < range.getStartMillis()
                : s.getEndMillis() <= range.getStartMillis()) {
            return false;
        }
        List<String> sources = query.getSources();
        if (!sources.isEmpty()) {
            if (s.getSource() == null
                    || !sources.contains(s.getSource().getBundleId())) {
                return false;
            }
        }
        return true;
    }

    /// Completes inline, on the calling thread.
    ///
    /// **This does not match the mobile ports, and that is a known gap.**
    /// `IOSHealth` marshals through `Display.callSerially` and
    /// `AndroidHealthStore` through `AndroidHealth.onEdt`, so on a phone
    /// every callback does arrive on the EDT as
    /// [com.codename1.health.Health] promises. Here it arrives on whichever
    /// thread called.
    ///
    /// Marshalling this store the same way is written and reverted: moving
    /// the completion onto the EDT makes the whole result chain --
    /// paging, unit normalization, series flattening -- run there too, and
    /// anything those throw escapes the EDT runnable instead of failing
    /// the resource, so the caller waits forever. That is a real defect in
    /// the completion path rather than in the hop, and it wants fixing
    /// first. Until then, inline delivery is the behaviour that cannot
    /// hang, and the developer guide says so rather than promising the
    /// EDT everywhere.
    private static void completeInline(AsyncResource out, Object value) {
        out.complete(value);
    }

    /// Copies a stored sample so callers cannot mutate the store.
    ///
    /// Quantity samples are the only shape this store holds; anything else
    /// is returned as-is, which is still an improvement on handing back
    /// the stored instance for the common case.
    private static HealthSample snapshot(HealthSample s) {
        HealthSample copy = copyShape(s);
        if (copy == null) {
            // A shape this build cannot copy. Sharing the caller's object
            // is worse than refusing the write: it would let a later edit
            // silently rewrite stored data, including the identifier a
            // delete matches on.
            throw new IllegalArgumentException(s.getClass().getName()
                    + " cannot be stored locally: this build has no copy"
                    + " for that sample shape");
        }
        copy.setId(s.getId());
        copy.setSource(s.getSource());
        copy.setRecordingMethod(s.getRecordingMethod());
        // Title and notes live on SessionSample, above the shapes that
        // carry them, so every per-shape branch missed them and a titled
        // workout or sleep session lost both the moment it was written.
        if (s instanceof SessionSample && copy instanceof SessionSample) {
            SessionSample from = (SessionSample) s;
            SessionSample to = (SessionSample) copy;
            to.setTitle(from.getTitle());
            to.setNotes(from.getNotes());
        }
        for (Map.Entry<String, String> e : s.getMetadata().entrySet()) {
            copy.putMetadata(e.getKey(), e.getValue());
        }
        return copy;
    }

    /// A copy of `s` carrying its shape-specific state, or null when the
    /// shape is unknown to this build.
    ///
    /// Every shape, not only [QuantitySample]. The common setters --
    /// identifier, source, metadata -- exist on all of them, and a series,
    /// a workout or a nutrition entry has mutable state of its own on top.
    /// Returning the caller's object left the store aliased to it, so
    /// editing a sample after writing it silently changed what was stored,
    /// and editing one that came back from a read did the same. Losing the
    /// identifier that way breaks deletion by id.
    private static HealthSample copyShape(HealthSample s) {
        if (s instanceof QuantitySample) {
            QuantitySample q = (QuantitySample) s;
            return q.isInstantaneous()
                    ? QuantitySample.create(q.getType(), q.getQuantity(),
                            q.getStartMillis())
                    : QuantitySample.create(q.getType(), q.getQuantity(),
                            q.getStartMillis(), q.getEndMillis());
        }
        if (s instanceof SeriesSample) {
            SeriesSample series = (SeriesSample) s;
            int n = series.size();
            long[] starts = new long[n];
            long[] ends = new long[n];
            double[] values = new double[n];
            for (int i = 0; i < n; i++) {
                starts[i] = series.getSampleStartMillis(i);
                ends[i] = series.getSampleEndMillis(i);
                values[i] = series.getSampleValue(i, series.getUnit());
            }
            return SeriesSample.create(series.getType(),
                    series.getStartMillis(), series.getEndMillis(), starts,
                    ends, values, series.getUnit());
        }
        if (s instanceof BloodPressureSample) {
            BloodPressureSample bp = (BloodPressureSample) s;
            BloodPressureSample copy = BloodPressureSample.create(
                    bp.getSystolic(), bp.getDiastolic(), bp.getStartMillis());
            copy.setPulse(bp.getPulse());
            copy.setBodyPosition(bp.getBodyPosition());
            copy.setMeasurementLocation(bp.getMeasurementLocation());
            return copy;
        }
        if (s instanceof CategorySample) {
            CategorySample c = (CategorySample) s;
            return c.isInstantaneous()
                    ? CategorySample.create(c.getType(), c.getValue(),
                            c.getStartMillis())
                    : CategorySample.create(c.getType(), c.getValue(),
                            c.getStartMillis(), c.getEndMillis());
        }
        if (s instanceof SleepSample) {
            SleepSample sleep = (SleepSample) s;
            return SleepSample.create(sleep.getStartMillis(),
                    sleep.getEndMillis(), sleep.getStages());
        }
        if (s instanceof WorkoutSample) {
            WorkoutSample w = (WorkoutSample) s;
            WorkoutSample copy = WorkoutSample.create(w.getActivityType(),
                    w.getStartMillis(), w.getEndMillis());
            copy.setPlatformCode(w.getPlatformCode());
            copy.setTotalEnergy(w.getTotalEnergy());
            copy.setTotalDistance(w.getTotalDistance());
            copy.setActiveDurationMillis(w.getActiveDurationMillis());
            return copy;
        }
        if (s instanceof NutritionSample) {
            NutritionSample n = (NutritionSample) s;
            NutritionSample copy = n.isInstantaneous()
                    ? NutritionSample.create(n.getStartMillis())
                    : NutritionSample.create(n.getStartMillis(),
                            n.getEndMillis());
            for (Nutrient nutrient : n.getNutrients()) {
                HealthQuantity amount = n.getNutrient(nutrient);
                if (amount != null) {
                    copy.setNutrient(nutrient, amount.getRawValue(),
                            amount.getUnit());
                }
            }
            copy.setMealType(n.getMealType());
            copy.setFoodName(n.getFoodName());
            return copy;
        }
        return null;
    }

    private static void sort(List<HealthSample> list,
            final boolean descending) {
        Collections.sort(list, new Comparator<HealthSample>() {
            @Override
            public int compare(HealthSample a, HealthSample b) {
                long d = a.getStartMillis() - b.getStartMillis();
                int r = d < 0 ? -1 : (d > 0 ? 1 : 0);
                return descending ? -r : r;
            }
        });
    }

    // ------------------------------------------------------------------
    // aggregates
    // ------------------------------------------------------------------

    @Override
    protected void doAggregate(AggregateQuery query, long[] boundaries,
            AsyncResource<List<AggregateResult>> out) {
        List<HealthSample> snapshot = new ArrayList<HealthSample>();
        synchronized (samples) {
            for (HealthSample s : samples) {
                if (isVisible(s)) {
                    snapshot.add(s);
                }
            }
        }
        completeInline(out, aggregateSamples(query, boundaries, snapshot));
    }


    // ------------------------------------------------------------------
    // writes
    // ------------------------------------------------------------------

    @Override
    protected void doWrite(List<HealthSample> toWrite,
            AsyncResource<HealthWriteResult> out) {
        HealthWriteResult result = new HealthWriteResult();
        List<HealthSample> added = new ArrayList<HealthSample>();
        boolean failed = false;
        synchronized (mutationLock) {
            synchronized (samples) {
                for (HealthSample s : toWrite) {
                    String id = "local-" + (nextId++);
                    // The identifier goes on the stored copy only. Stamping
                    // the caller's object made this store the one place a
                    // write mutated its input -- and HealthSample.hashCode()
                    // switches from identity to the id once it is set, so a
                    // sample already in a HashSet or used as a map key became
                    // unreachable the moment it was written. The id reaches
                    // the caller through HealthWriteResult, as it does on
                    // every other platform.
                    //
                    // Stored as a copy: keeping the caller's object meant a
                    // later setId/setSource/putMetadata on it silently rewrote
                    // the stored record, and could make deleting by the
                    // returned id fail.
                    HealthSample stored = snapshot(s);
                    stored.setId(id);
                    samples.add(stored);
                    added.add(stored);
                    result.addSampleId(id);
                }
            }
            if (!persist()) {
                // Rolled back rather than kept. `Storage.writeObject` reports
                // a full or unwritable store by returning false, and a caller
                // told the write succeeded would have a record that exists
                // only until the process exits -- on the very ports whose
                // whole claim is durability. Undoing it keeps memory and disk
                // saying the same thing.
                synchronized (samples) {
                    samples.removeAll(added);
                }
                failed = true;
            }
        }
        if (failed) {
            failStorage(out);
            return;
        }
        completeInline(out, result);
    }

    /// Fails an operation that could not be made durable.
    ///
    /// `DATABASE_INACCESSIBLE` because that is the retryable "the store
    /// would not take it" answer callers already handle from the phones;
    /// a local store that is full or read-only is the same situation.
    private static void failStorage(AsyncResource out) {
        out.error(new HealthException(HealthError.DATABASE_INACCESSIBLE,
                "the local health store could not be written; the change"
                        + " was rolled back"));
    }

    @Override
    protected void doDelete(HealthDeleteRequest request,
            AsyncResource<Integer> out) {
        int removed = 0;
        boolean failed = false;
        List<HealthSample> dropped = new ArrayList<HealthSample>();
        synchronized (mutationLock) {
            synchronized (samples) {
                if (request.isById()) {
                    List<String> ids = request.getSampleIds();
                    // The type is part of the request, and matching on the
                    // identifier alone ignored it. Health Connect deletes
                    // against a record class, so a stale or mispaired type and
                    // id removes nothing there while this store removed the
                    // record anyway -- the simulator being more destructive
                    // than the platform it stands in for.
                    List<HealthDataType> types = request.getTypes();
                    for (int i = samples.size() - 1; i >= 0; i--) {
                        HealthSample s = samples.get(i);
                        if (ids.contains(s.getId())
                                && (types.isEmpty()
                                        || types.contains(s.getType()))) {
                            dropped.add(samples.remove(i));
                            removed++;
                        }
                    }
                } else {
                    HealthTimeRange range = request.getTimeRange()
                            .resolve(System.currentTimeMillis());
                    for (int i = samples.size() - 1; i >= 0; i--) {
                        HealthSample s = samples.get(i);
                        if (request.getTypes().contains(s.getType())
                                && s.getStartMillis() >= range.getStartMillis()
                                && s.getStartMillis() < range.getEndMillis()) {
                            dropped.add(samples.remove(i));
                            removed++;
                        }
                    }
                }
            }
            if (removed > 0 && !persist()) {
                synchronized (samples) {
                    samples.addAll(dropped);
                }
                failed = true;
            }
        }
        if (failed) {
            failStorage(out);
            return;
        }
        completeInline(out, Integer.valueOf(removed));
    }

    // ------------------------------------------------------------------
    // extension points
    // ------------------------------------------------------------------

    /// Called after every mutation.
    ///
    /// #### Returns
    ///
    /// Whether the store is durable as a result. The in-memory base has
    /// nothing to write and nothing to promise, so it answers `true`; a
    /// persisting subclass answers `false` when the write did not land,
    /// and the mutation that triggered it is rolled back and reported as
    /// a failure rather than acknowledged as stored.
    protected boolean persist() {
        return true;
    }

    /// Whether `s` is visible to reads and aggregates at all.
    ///
    /// The simulator hides the records of a type scripted to yield
    /// nothing, and it has to happen *here* rather than by filtering the
    /// answer: a page is sorted and cut to the limit before it is
    /// returned, so a hidden record that sorts first would otherwise eat
    /// a slot and a limit-one query could come back empty while a visible
    /// record sat behind it. Records nobody can see must not compete for
    /// the budget.
    protected boolean isVisible(HealthSample s) {
        return true;
    }

    /// Inserts a sample without going through validation, for a subclass
    /// restoring persisted data or a simulator seeding a scripted dataset.
    protected final void addSampleDirect(HealthSample sample) {
        if (sample == null) {
            return;
        }
        synchronized (samples) {
            if (sample.getId() == null) {
                sample.setId("local-" + (nextId++));
            } else {
                // A restored sample brings its own identifier back, and
                // the counter has to clear it or the first write after a
                // restart hands out an id something already holds --
                // making a delete-by-id remove the wrong record.
                reserveId(sample.getId());
            }
            samples.add(sample);
        }
    }

    /// Moves the generator past an identifier that already exists.
    private void reserveId(String id) {
        if (!id.startsWith("local-")) {
            return;
        }
        try {
            long n = Long.parseLong(id.substring("local-".length()));
            if (n >= nextId) {
                nextId = n + 1;
            }
        } catch (NumberFormatException ex) {
            // Not one of ours; nothing to reserve.
        }
    }

    /// Every stored sample, for subclasses persisting the store.
    protected final List<HealthSample> getAllSamples() {
        synchronized (samples) {
            return new ArrayList<HealthSample>(samples);
        }
    }

    /// Discards everything.
    public final void clear() {
        List<HealthSample> before;
        synchronized (mutationLock) {
            synchronized (samples) {
                before = new ArrayList<HealthSample>(samples);
                samples.clear();
            }
            if (!persist()) {
                // No result to fail -- this returns void -- so the least
                // dishonest answer is to leave the store as it was rather
                // than empty in memory and full on disk.
                synchronized (samples) {
                    samples.addAll(before);
                }
                com.codename1.io.Log.p("CN1 Health: the local store could not"
                        + " be cleared on disk, so it was left as it was");
            }
        }
    }
}

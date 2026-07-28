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
package com.codename1.health;

import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/**
 * A scriptable {@link HealthStore} for testing the shared machinery.
 *
 * <p>The port SPI is what the base class drives, so a fake port is the only
 * way to test the parts that have historically broken: cursor advancement,
 * batch capping, paging and chunked writes. Everything here is
 * deterministic and synchronous -- no threads, no clock reads -- so a
 * failure points at the logic rather than at timing.</p>
 */
public class FakeHealthStore extends HealthStore {

    /** Pages returned by successive doReadSamples calls. */
    public final List<SamplePage> pages = new ArrayList<SamplePage>();
    /** Queries the base class actually issued, in order. */
    public final List<SampleQuery> queriesSeen = new ArrayList<SampleQuery>();
    /** The thread each of those queries arrived on, in order. */
    public final List<String> readThreads = new ArrayList<String>();
    /** Chunks the base class actually issued to doWrite, in order. */
    public final List<List<HealthSample>> writeChunks =
            new ArrayList<List<HealthSample>>();
    /** Index of the write chunk that should fail, or -1 for none. */
    public int failWriteChunk = -1;
    /** Batches the port hands to fireChanges when drainChanges runs. */
    public final List<HealthChangeBatch> batchesToFire =
            new ArrayList<HealthChangeBatch>();

    public int maxWriteBatch = 1000;
    public boolean supported = true;
    private int pageIndex;
    private int writeIndex;

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public boolean isTypeSupported(HealthDataType type) {
        return supported && type != null;
    }

    /// Types this fake refuses to write, so a caller can reproduce the
    /// Health Connect shape where a readable type has no write form.
    public final List<HealthDataType> unwritable =
            new ArrayList<HealthDataType>();

    @Override
    public boolean isWritable(HealthDataType type) {
        return isTypeSupported(type) && !unwritable.contains(type);
    }

    @Override
    public int getMaxWriteBatchSize() {
        return maxWriteBatch;
    }

    @Override
    protected void doReadSamples(SampleQuery query,
            AsyncResource<SamplePage> out) {
        queriesSeen.add(query);
        readThreads.add(Thread.currentThread().getName());
        if (pageIndex >= pages.size()) {
            out.complete(new SamplePage(new ArrayList<HealthSample>(), null,
                    false));
            return;
        }
        out.complete(pages.get(pageIndex++));
    }

    @Override
    protected void doWrite(List<HealthSample> samples,
            AsyncResource<HealthWriteResult> out) {
        int index = writeIndex++;
        writeChunks.add(new ArrayList<HealthSample>(samples));
        if (index == failWriteChunk) {
            out.error(new HealthException(HealthError.UNKNOWN,
                    "scripted failure on chunk " + index));
            return;
        }
        HealthWriteResult r = new HealthWriteResult();
        for (int i = 0; i < samples.size(); i++) {
            r.addSampleId("chunk" + index + "-" + i);
        }
        out.complete(r);
    }

    /// How many times the port's drain was actually entered.
    public int drainCount;
    /// Run on entry to the port drain, so a test can overlap two calls.
    public Runnable beforeDrain;

    /** Anchors the live handles carried into the last drain. */
    public final List<HealthAnchor> anchorsSeen =
            new ArrayList<HealthAnchor>();

    /** Seeds a cursor the way a port does at registration. */
    public boolean seedForTest(HealthSubscription sub, HealthAnchor anchor) {
        return seedAnchor(sub, anchor);
    }

    @Override
    protected void doDrainChanges(List<HealthSubscription> subscriptions,
            AsyncResource<Integer> out) {
        drainCount++;
        anchorsSeen.clear();
        for (HealthSubscription sub : subscriptions) {
            anchorsSeen.add(sub.getAnchor());
        }
        if (beforeDrain != null) {
            Runnable r = beforeDrain;
            beforeDrain = null;
            r.run();
        }
        int n = 0;
        for (HealthChangeBatch b : batchesToFire) {
            // Per delivery queued, not per batch handed in: the shared
            // layer splits a batch over the subscription's cap, and
            // drainChanges is documented as a count of batches the
            // listener received. Counting the input here made the fake
            // disagree with both real ports.
            n += fireChanges(b);
        }
        batchesToFire.clear();
        out.complete(Integer.valueOf(n));
    }

    /** A quantity sample of `type` over `[start,end)` with `value`. */
    public static QuantitySample sample(HealthDataType type, long start,
            long end, double value) {
        HealthQuantity q = new HealthQuantity(value,
                type.getCanonicalUnit());
        return start == end ? QuantitySample.create(type, q, start)
                : QuantitySample.create(type, q, start, end);
    }
}

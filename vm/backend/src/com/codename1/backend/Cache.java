/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import com.codename1.backend.metrics.Counter;
import com.codename1.backend.metrics.Gauge;
import com.codename1.backend.metrics.Histogram;
import com.codename1.backend.metrics.Metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A bounded, process-local application cache: entries limited by count or
/// by weight, expiring after a write, after an access or at a time each entry
/// chooses, loaded at most once per key however many callers ask together.
///
/// ```java
///   private static final Cache<String, Price> PRICES = Cache.<String, Price>builder("prices")
///           .maximumSize(10000)
///           .expireAfterWriteMillis(60000)
///           .build();
///   ...
///   Price p = PRICES.get(sku, new Cache.Loader<String, Price>() {
///       public Price load(String key) throws Exception {
///           return priceService.lookup(key);
///       }
///   });
/// ```
///
/// ## Loading
///
/// [#get(Object, Loader)] answers a live entry, or runs the loader on the
/// calling thread with no lock held. A second caller asking for the same key
/// while that load runs does not load again: it waits for the first one and
/// gets its result -- the value, or the same failure. A loader that fails, or
/// answers null, leaves the cache as it was; nothing is stored, so the next
/// caller loads again. A key invalidated or [#put] while its load is running
/// keeps what that call did: the loaded value is handed to the callers that
/// waited for it and is not stored over the newer state.
///
/// A loader may use the cache, but not for the key it is loading; that would
/// wait for itself, and is refused with `IllegalStateException`.
///
/// A waiting virtual thread naps rather than blocking its host, so a slow
/// load holds back only the requests that want its key.
///
/// ## Eviction and expiry
///
/// Above the size or weight bound the least recently used entries go first.
/// An expired entry is never answered; it is removed, with an `EXPIRED`
/// notification, when a read finds it, when a write's periodic sweep does,
/// or on [#cleanUp]. [#size] cleans up first, so it counts only live
/// entries.
///
/// ## Removal notifications
///
/// The [RemovalListener] runs on the thread whose call removed the entry,
/// after the cache's lock is released, so it may use the cache. One that
/// throws is reported on standard error and does not affect the cache or the
/// call that triggered it. Notifications from one call are delivered in the
/// order the entries were removed.
///
/// ## Metrics
///
/// Every cache records `cache.<name>.hits`, `.misses`, `.loads`,
/// `.load_failures` and `.evictions` (counters), `.size` and `.weight`
/// (gauges) and `.load.duration` (a histogram, milliseconds) in
/// [Metrics]; the same figures are on the cache's own accessors. Two caches
/// built with one name share the counters, and the gauges read the newer.
///
/// ## Scope and cost
///
/// A cache lives in this process only: two instances of a server each hold
/// their own, and nothing is shared or invalidated between them. Values are
/// held by reference, so a value a caller changes changes for every reader.
/// Each entry costs about a hundred bytes beyond its key and value -- a hash
/// map entry with its access-order links, and a node holding the value, its
/// weight and three timestamps -- on the JVM and in the native binary alike.
///
/// Server code: this class is not available in the app.
public final class Cache<K, V> {

    /// Computes the value for a key that is absent.
    public interface Loader<K, V> {
        /// The value, or null for "no value"; null is not stored.
        V load(K key) throws Exception;
    }

    /// The weight of an entry, for [Cache.Builder#maximumWeight].
    public interface Weigher<K, V> {
        /// Zero or more.
        long weigh(K key, V value);
    }

    /// A lifetime chosen per entry, from when it is written.
    public interface Expiry<K, V> {
        /// Milliseconds the entry lives after being stored, or `Long.MAX_VALUE`
        /// for no limit. Overrides [Cache.Builder#expireAfterWriteMillis] for this
        /// entry; [Cache.Builder#expireAfterAccessMillis] still applies. Zero or
        /// less means the value is not stored, and any old value for the key is
        /// removed; a [Cache#get] of such a value calls its loader every time.
        long millisToLive(K key, V value);
    }

    /// Told about every entry that leaves the cache.
    public interface RemovalListener<K, V> {
        /// `cause` is one of [#EXPLICIT], [#REPLACED], [#EXPIRED] or [#SIZE].
        void removed(K key, V value, int cause);
    }

    /// Where the cache reads the time: monotonic milliseconds. Replace it in a
    /// test to move time deterministically.
    public interface Clock {
        long millis();
    }

    /// Removed by [#invalidate] or [#invalidateAll].
    public static final int EXPLICIT = 1;
    /// Overwritten by [#put] or by a load.
    public static final int REPLACED = 2;
    /// Its lifetime ran out.
    public static final int EXPIRED = 3;
    /// Evicted to stay within the size or weight bound.
    public static final int SIZE = 4;

    /// Thrown by [#get(Object, Loader)] when the loader threw a checked
    /// exception, which is its cause. An unchecked one is rethrown as it is.
    public static final class LoadException extends RuntimeException {
        public LoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /// A cache's settings. Nothing is bounded or expires unless asked.
    public static final class Builder<K, V> {
        private final String name;
        private long maximumSize = -1;
        private long maximumWeight = -1;
        private Weigher<K, V> weigher;
        private long afterWrite = -1;
        private long afterAccess = -1;
        private Expiry<K, V> expiry;
        private RemovalListener<K, V> listener;
        private Clock clock;

        Builder(String name) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("A cache needs a name, for its metrics");
            }
            this.name = name;
        }

        /// At most `entries` entries. Excludes [#maximumWeight].
        public Builder<K, V> maximumSize(long entries) {
            if (entries < 0) {
                throw new IllegalArgumentException("maximumSize must not be negative");
            }
            maximumSize = entries;
            return this;
        }

        /// At most `weight` in total, as `weigher` weighs entries. Excludes
        /// [#maximumSize].
        public Builder<K, V> maximumWeight(long weight, Weigher<K, V> weigher) {
            if (weight < 0 || weigher == null) {
                throw new IllegalArgumentException("maximumWeight needs a weight and a weigher");
            }
            maximumWeight = weight;
            this.weigher = weigher;
            return this;
        }

        /// Entries expire this long after they are stored.
        public Builder<K, V> expireAfterWriteMillis(long millis) {
            afterWrite = positive(millis);
            return this;
        }

        /// Entries expire this long after they were last read or stored.
        public Builder<K, V> expireAfterAccessMillis(long millis) {
            afterAccess = positive(millis);
            return this;
        }

        /// Each entry's lifetime from its write, chosen by `expiry`.
        public Builder<K, V> expiry(Expiry<K, V> expiry) {
            this.expiry = expiry;
            return this;
        }

        /// Told about every removal; see the class description.
        public Builder<K, V> removalListener(RemovalListener<K, V> listener) {
            this.listener = listener;
            return this;
        }

        /// The time source; the default is the monotonic system clock.
        public Builder<K, V> clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Cache<K, V> build() {
            if (maximumSize >= 0 && maximumWeight >= 0) {
                throw new IllegalStateException("A cache is bounded by size or by weight, not both");
            }
            return new Cache<K, V>(this);
        }

        private static long positive(long millis) {
            if (millis <= 0) {
                throw new IllegalArgumentException("An expiry must be positive");
            }
            return millis;
        }
    }

    private static final Clock SYSTEM_CLOCK = new Clock() {
        @Override
        public long millis() {
            return System.nanoTime() / 1000000L;
        }
    };

    /// A cache named `name`; the name prefixes its metrics.
    public static <K, V> Builder<K, V> builder(String name) {
        return new Builder<K, V>(name);
    }

    private static final class Node<V> {
        V value;
        long weight;
        long accessed;
        /// When it expires by its write: Long.MAX_VALUE for never.
        long writeExpires;
    }

    /// One running load, shared by every caller of its key.
    private static final class Load<V> {
        final Object owner;
        boolean done;
        /// Set when the key was invalidated or put during the load.
        boolean superseded;
        V value;
        Throwable error;

        Load(Object owner) {
            this.owner = owner;
        }
    }

    private static final class Removal<K, V> {
        final K key;
        final V value;
        final int cause;

        Removal(K key, V value, int cause) {
            this.key = key;
            this.value = value;
            this.cause = cause;
        }
    }

    private final String name;
    private final long maximumSize;
    private final long maximumWeight;
    private final Weigher<K, V> weigher;
    private final long afterWrite;
    private final long afterAccess;
    private final Expiry<K, V> expiry;
    private final RemovalListener<K, V> listener;
    private final Clock clock;
    private final long sweepInterval;

    /// Access ordered, so iteration starts at the least recently used. Guarded
    /// by `this`, as is everything below.
    private final LinkedHashMap<K, Node<V>> entries = new LinkedHashMap<K, Node<V>>(16, 0.75f, true);
    private final Map<K, Load<V>> loading = new HashMap<K, Load<V>>();
    private long totalWeight;
    /// Starts at the minimum: nanoTime, and a test clock, may read negative.
    private long nextSweep = Long.MIN_VALUE;
    private long hits;
    private long misses;
    private long loads;
    private long loadFailures;
    private long evictions;

    private final Counter hitCounter;
    private final Counter missCounter;
    private final Counter loadCounter;
    private final Counter failureCounter;
    private final Counter evictionCounter;
    private final Histogram loadDuration;

    private Cache(Builder<K, V> b) {
        name = b.name;
        maximumSize = b.maximumSize;
        maximumWeight = b.maximumWeight;
        weigher = b.weigher;
        afterWrite = b.afterWrite;
        afterAccess = b.afterAccess;
        expiry = b.expiry;
        listener = b.listener;
        clock = b.clock == null ? SYSTEM_CLOCK : b.clock;
        long shortest = Long.MAX_VALUE;
        if (afterWrite > 0) {
            shortest = afterWrite;
        }
        if (afterAccess > 0) {
            shortest = Math.min(shortest, afterAccess);
        }
        // A per-entry expiry can be anything; sweep once a second for it. The
        // sweep is the only full walk, so it is kept to a fraction of the
        // shortest lifetime rather than run on every write.
        sweepInterval = shortest == Long.MAX_VALUE ? (expiry != null ? 1000 : Long.MAX_VALUE)
                : Math.max(1, Math.min(expiry != null ? 1000 : shortest, shortest / 4));
        String prefix = "cache." + name + ".";
        hitCounter = Metrics.counter(prefix + "hits", "Lookups answered from the cache", "{lookup}");
        missCounter = Metrics.counter(prefix + "misses", "Lookups the cache could not answer",
                "{lookup}");
        loadCounter = Metrics.counter(prefix + "loads", "Loads that completed", "{load}");
        failureCounter = Metrics.counter(prefix + "load_failures", "Loads that threw", "{load}");
        evictionCounter = Metrics.counter(prefix + "evictions",
                "Entries evicted for size or weight", "{entry}");
        loadDuration = Metrics.histogram(prefix + "load.duration", "Time spent loading", "ms");
        Metrics.gauge(prefix + "size", "Entries in the cache", "{entry}", new Gauge.Source() {
            @Override
            public double read() {
                // Not size(): a metrics read must not run removal listeners on
                // the exporter's thread. May include entries not yet swept.
                synchronized (Cache.this) {
                    return entries.size();
                }
            }
        });
        Metrics.gauge(prefix + "weight", "Total weight of the entries", "1", new Gauge.Source() {
            @Override
            public double read() {
                return weight();
            }
        });
    }

    public String getName() {
        return name;
    }

    /// The live value for `key`, or null. Counts a hit or a miss.
    public V getIfPresent(K key) {
        List<Removal<K, V>> removed = null;
        V answer;
        synchronized (this) {
            long now = clock.millis();
            Node<V> n = entries.get(key);
            if (n != null && expired(n, now)) {
                entries.remove(key);
                totalWeight -= n.weight;
                removed = add(null, key, n.value, EXPIRED);
                n = null;
            }
            if (n == null) {
                misses++;
                answer = null;
            } else {
                n.accessed = now;
                hits++;
                answer = n.value;
            }
        }
        (answer == null ? missCounter : hitCounter).increment();
        notifyAll(removed);
        return answer;
    }

    /// The live value for `key`, loading it with `loader` when absent. See the
    /// class description for concurrent callers, failures and null.
    public V get(K key, Loader<K, V> loader) {
        if (key == null || loader == null) {
            throw new NullPointerException("A cache lookup needs a key and a loader");
        }
        Object me = caller();
        Load<V> load;
        boolean mine;
        List<Removal<K, V>> removed = null;
        synchronized (this) {
            long now = clock.millis();
            Node<V> n = entries.get(key);
            if (n != null && expired(n, now)) {
                entries.remove(key);
                totalWeight -= n.weight;
                removed = add(null, key, n.value, EXPIRED);
                n = null;
            }
            if (n != null) {
                n.accessed = now;
                hits++;
                V v = n.value;
                hitCounter.increment();
                // Nothing was removed: an expired node would have left n null.
                return v;
            }
            misses++;
            load = loading.get(key);
            mine = load == null;
            if (mine) {
                load = new Load<V>(me);
                loading.put(key, load);
            } else if (load.owner.equals(me)) {
                throw new IllegalStateException("The loader for " + key
                        + " asked the cache for that same key");
            }
        }
        missCounter.increment();
        notifyAll(removed);
        if (mine) {
            return runLoad(key, loader, load);
        }
        return await(load);
    }

    private V runLoad(K key, Loader<K, V> loader, Load<V> load) {
        long started = System.currentTimeMillis();
        V value = null;
        Throwable error = null;
        try {
            value = loader.load(key);
        } catch (Throwable t) {
            error = t;
        }
        loadDuration.record(System.currentTimeMillis() - started);
        List<Removal<K, V>> removed = null;
        synchronized (this) {
            if (loading.get(key) == load) { //NOPMD CompareObjectsWithEquals - this load, by identity
                loading.remove(key);
            }
            if (error == null && value != null && !load.superseded) {
                try {
                    removed = store(key, value, null);
                } catch (Throwable t) {
                    // A weigher or an expiry that threw: store() changes nothing
                    // before both have answered, so the cache is as it was, and
                    // the waiters must still be released below.
                    error = t;
                }
            }
            if (error != null) {
                loadFailures++;
            } else {
                loads++;
            }
        }
        (error != null ? failureCounter : loadCounter).increment();
        countEvictions(removed);
        synchronized (load) {
            load.value = value;
            load.error = error;
            load.done = true;
            load.notifyAll();
        }
        notifyAll(removed);
        return answer(load);
    }

    /// Waits for another caller's load of the same key. A virtual thread naps,
    /// as Database's gate does, so its host keeps running the others.
    private V await(Load<V> load) {
        boolean virtual = VirtualThread.isVirtual();
        boolean interrupted = false;
        long nap = 1;
        while (true) {
            synchronized (load) {
                if (load.done) {
                    break;
                }
                if (!virtual) {
                    try {
                        load.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                    continue;
                }
            }
            HttpServer.napUntil(System.currentTimeMillis() + nap);
            nap = Math.min(20, nap * 2);
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return answer(load);
    }

    @SuppressWarnings("unchecked")
    private V answer(Load<V> load) {
        V value;
        Throwable error;
        synchronized (load) {
            value = load.value;
            error = load.error;
        }
        if (error == null) {
            return value;
        }
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        }
        if (error instanceof Error) {
            throw (Error) error;
        }
        throw new LoadException("Loading " + name + " failed: " + error, error);
    }

    /// Stores `value`, replacing what is there.
    public void put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("A cache stores neither null keys nor null values");
        }
        List<Removal<K, V>> removed;
        synchronized (this) {
            Load<V> load = loading.get(key);
            if (load != null) {
                load.superseded = true;
            }
            removed = store(key, value, null);
        }
        countEvictions(removed);
        notifyAll(removed);
    }

    /// Removes `key`, and keeps a load of it already running from storing its
    /// result.
    public void invalidate(K key) {
        List<Removal<K, V>> removed = null;
        synchronized (this) {
            // Out of `loading` too, so a get() arriving after this starts a
            // fresh load instead of joining one that may read the old value.
            Load<V> load = loading.remove(key);
            if (load != null) {
                load.superseded = true;
            }
            Node<V> n = entries.remove(key);
            if (n != null) {
                totalWeight -= n.weight;
                removed = add(null, key, n.value, EXPLICIT);
            }
        }
        notifyAll(removed);
    }

    /// Removes every entry, as [#invalidate] does each.
    public void invalidateAll() {
        List<Removal<K, V>> removed = null;
        synchronized (this) {
            for (Load<V> load : loading.values()) {
                load.superseded = true;
            }
            loading.clear();
            for (Map.Entry<K, Node<V>> e : entries.entrySet()) {
                removed = add(removed, e.getKey(), e.getValue().value, EXPLICIT);
            }
            entries.clear();
            totalWeight = 0;
        }
        notifyAll(removed);
    }

    /// Removes every expired entry now.
    public void cleanUp() {
        List<Removal<K, V>> removed;
        synchronized (this) {
            removed = sweep(clock.millis(), null);
        }
        notifyAll(removed);
    }

    /// Live entries, after a [#cleanUp].
    public long size() {
        cleanUp();
        synchronized (this) {
            return entries.size();
        }
    }

    /// The total weight of the entries: their count when no weigher is set.
    public synchronized long weight() {
        return weigher == null ? entries.size() : totalWeight;
    }

    public synchronized long hitCount() {
        return hits;
    }

    public synchronized long missCount() {
        return misses;
    }

    /// Loads that completed, with a value or with null.
    public synchronized long loadCount() {
        return loads;
    }

    public synchronized long loadFailureCount() {
        return loadFailures;
    }

    /// Entries evicted for size or weight; expiry and invalidation not counted.
    public synchronized long evictionCount() {
        return evictions;
    }

    // ------------------------------------------------------------ internals

    /// Called holding the lock.
    private List<Removal<K, V>> store(K key, V value, List<Removal<K, V>> removed) {
        long now = clock.millis();
        long w = 1;
        if (weigher != null) {
            w = weigher.weigh(key, value);
            if (w < 0) {
                throw new IllegalArgumentException("A weigher answered a negative weight for " + key);
            }
        }
        long life = expiry != null ? expiry.millisToLive(key, value) : afterWrite;
        boolean dead = expiry != null && life <= 0;
        boolean oversized = maximumWeight >= 0 && w > maximumWeight;
        if (dead || oversized) {
            // Not kept. Stored, a dead entry would hold size and weight until
            // the next full sweep, and an oversized one would evict every live
            // entry from the least recently used end before going itself.
            Node<V> old = entries.remove(key);
            if (old != null) {
                totalWeight -= old.weight;
                removed = add(removed, key, old.value, expired(old, now) ? EXPIRED : REPLACED);
            }
            if (oversized && !dead) {
                evictions++;
                removed = add(removed, key, value, SIZE);
            }
            return removed;
        }
        Node<V> n = new Node<V>();
        n.value = value;
        n.weight = w;
        n.accessed = now;
        if (expiry == null && life < 0 || life == Long.MAX_VALUE || now > Long.MAX_VALUE - life) {
            n.writeExpires = Long.MAX_VALUE;
        } else {
            n.writeExpires = now + life;
        }
        Node<V> old = entries.put(key, n);
        if (old != null) {
            totalWeight -= old.weight;
            removed = add(removed, key, old.value, expired(old, now) ? EXPIRED : REPLACED);
        }
        totalWeight += w;
        removed = sweep(now, removed);
        return evict(removed);
    }

    /// Removes expired entries; a full walk only every sweepInterval.
    private List<Removal<K, V>> sweep(long now, List<Removal<K, V>> removed) {
        boolean full = sweepInterval != Long.MAX_VALUE && now >= nextSweep;
        Iterator<Map.Entry<K, Node<V>>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, Node<V>> e = it.next();
            Node<V> n = e.getValue();
            if (expired(n, now)) {
                K key = e.getKey();
                it.remove();
                totalWeight -= n.weight;
                removed = add(removed, key, n.value, EXPIRED);
            } else if (!full) {
                // The head is the least recently accessed, so access expiry
                // never needs to look past the first live one.
                break;
            }
        }
        if (full) {
            nextSweep = now + sweepInterval;
        }
        return removed;
    }

    private List<Removal<K, V>> evict(List<Removal<K, V>> removed) {
        Iterator<Map.Entry<K, Node<V>>> it = entries.entrySet().iterator();
        while (it.hasNext() && over()) {
            Map.Entry<K, Node<V>> e = it.next();
            // Read before removing: ParparVM's compact map has no entry objects,
            // so an entry read after its removal answers null.
            K key = e.getKey();
            Node<V> n = e.getValue();
            it.remove();
            totalWeight -= n.weight;
            evictions++;
            removed = add(removed, key, n.value, SIZE);
        }
        return removed;
    }

    private boolean over() {
        if (maximumSize >= 0) {
            return entries.size() > maximumSize;
        }
        return maximumWeight >= 0 && totalWeight > maximumWeight;
    }

    private boolean expired(Node<V> n, long now) {
        return now >= n.writeExpires || afterAccess > 0 && now - n.accessed >= afterAccess;
    }

    private void countEvictions(List<Removal<K, V>> removed) {
        if (removed == null) {
            return;
        }
        long n = 0;
        for (Removal<K, V> r : removed) {
            if (r.cause == SIZE) {
                n++;
            }
        }
        if (n > 0) {
            evictionCounter.add(n);
        }
    }

    private static <K, V> List<Removal<K, V>> add(List<Removal<K, V>> list, K key, V value,
                                                  int cause) {
        if (list == null) {
            list = new ArrayList<Removal<K, V>>();
        }
        list.add(new Removal<K, V>(key, value, cause));
        return list;
    }

    /// Delivers removals with no lock held; a listener that throws is reported
    /// and skipped, so it can never leave the cache half updated.
    private void notifyAll(List<Removal<K, V>> removed) {
        if (removed == null || listener == null) {
            return;
        }
        for (Removal<K, V> r : removed) {
            try {
                listener.removed(r.key, r.value, r.cause);
            } catch (Throwable t) {
                System.err.println("Cache " + name + ": removal listener threw " + t);
            }
        }
    }

    private static Object caller() {
        long virtual = VirtualThread.current();
        return virtual != 0 ? (Object) Long.valueOf(virtual) : Thread.currentThread();
    }
}

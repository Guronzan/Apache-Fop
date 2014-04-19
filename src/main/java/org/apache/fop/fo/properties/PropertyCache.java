/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: PropertyCache.java 724444 2008-12-08 18:54:16Z adelmelle $ */

package org.apache.fop.fo.properties;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.apache.fop.fo.flow.Marker;

/**
 * Dedicated cache, meant for storing canonical instances of property-related
 * classes. The public access points are overloaded <code>fetch()</code> methods
 * that each correspond to a cached type. It is designed especially to be used
 * concurrently by multiple threads, drawing heavily upon the principles behind
 * Java 1.5's <code>ConcurrentHashMap</code>.
 */
public final class PropertyCache {

    private static final int SEGMENT_COUNT = 32; // 0x20
    private static final int INITIAL_BUCKET_COUNT = SEGMENT_COUNT;

    /**
     * bitmask to apply to the hash to get to the corresponding cache segment
     */
    private static final int SEGMENT_MASK = SEGMENT_COUNT - 1; // 0x1F
    /**
     * Indicates whether the cache should be used at all Can be controlled by
     * the system property: org.apache.fop.fo.properties.use-cache
     */
    private final boolean useCache;

    /** the segments array (length = 32) */
    private final CacheSegment[] segments = new CacheSegment[SEGMENT_COUNT];
    /** the table of hash-buckets */
    private CacheEntry[] table = new CacheEntry[INITIAL_BUCKET_COUNT];

    private final Class runtimeType;

    private final boolean[] votesForRehash = new boolean[SEGMENT_COUNT];

    /* same hash function as used by java.util.HashMap */
    private static int hash(final Object x) {
        return hash(x.hashCode());
    }

    private static int hash(final int hashCode) {
        int h = hashCode;
        h += ~(h << 9);
        h ^= h >>> 14;
        h += h << 4;
        h ^= h >>> 10;
        return h;
    }

    /* shortcut function */
    private static boolean eq(final Object p, final Object q) {
        return p == q || p != null && p.equals(q);
    }

    /* Class modeling a cached entry */
    private static class CacheEntry extends WeakReference {
        private volatile CacheEntry nextEntry;
        private final int hash;

        /* main constructor */
        public CacheEntry(final Object p, final CacheEntry nextEntry,
                final ReferenceQueue refQueue) {
            super(p, refQueue);
            this.nextEntry = nextEntry;
            this.hash = hash(p);
        }

        /* main constructor */
        public CacheEntry(final Object p, final CacheEntry nextEntry) {
            super(p);
            this.nextEntry = nextEntry;
            this.hash = hash(p);
        }

    }

    /* Wrapper objects to synchronize on */
    private static class CacheSegment {
        private int count = 0;
    }

    private void cleanSegment(final int segmentIndex) {
        final CacheSegment segment = this.segments[segmentIndex];

        final int oldCount = segment.count;

        /* clean all buckets in this segment */
        for (int bucketIndex = segmentIndex; bucketIndex < this.table.length; bucketIndex += SEGMENT_COUNT) {
            CacheEntry prev = null;
            CacheEntry entry = this.table[bucketIndex];
            if (entry == null) {
                continue;
            }
            do {
                if (entry.get() == null) {
                    if (prev == null) {
                        this.table[bucketIndex] = entry.nextEntry;
                    } else {
                        prev.nextEntry = entry.nextEntry;
                    }
                    segment.count--;
                    assert segment.count >= 0;
                } else {
                    prev = entry;
                }
                entry = entry.nextEntry;
            } while (entry != null);
        }

        synchronized (this.votesForRehash) {
            if (oldCount > segment.count) {
                this.votesForRehash[segmentIndex] = false;
                return;
            }
            /* cleanup had no effect */
            if (!this.votesForRehash[segmentIndex]) {
                /* first time for this segment */
                this.votesForRehash[segmentIndex] = true;
                int voteCount = 0;
                for (int i = SEGMENT_MASK + 1; --i >= 0;) {
                    if (this.votesForRehash[i]) {
                        voteCount++;
                    }
                }
                if (voteCount > SEGMENT_MASK / 4) {
                    rehash(SEGMENT_MASK);
                    /* reset votes */
                    for (int i = SEGMENT_MASK + 1; --i >= 0;) {
                        this.votesForRehash[i] = false;
                    }

                }
            }
        }
    }

    /*
     * Puts a new instance in the cache. If the total number of entries for the
     * corresponding segment exceeds twice the amount of hash-buckets, a cleanup
     * will be performed to try and remove obsolete entries.
     */
    private void put(final Object o) {

        final int hash = hash(o);
        final int segmentIndex = hash & SEGMENT_MASK;
        final CacheSegment segment = this.segments[segmentIndex];

        synchronized (segment) {
            final int index = hash & this.table.length - 1;
            CacheEntry entry = this.table[index];

            if (entry == null) {
                entry = new CacheEntry(o, null);
                this.table[index] = entry;
                segment.count++;
            } else {
                final Object p = entry.get();
                if (eq(p, o)) {
                    return;
                } else {
                    final CacheEntry newEntry = new CacheEntry(o, entry);
                    this.table[index] = newEntry;
                    segment.count++;
                }
            }

            if (segment.count > 2 * this.table.length) {
                cleanSegment(segmentIndex);
            }
        }
    }

    /* Gets a cached instance. Returns null if not found */
    private Object get(final Object o) {

        final int hash = hash(o);
        final int index = hash & this.table.length - 1;

        CacheEntry entry = this.table[index];
        Object q;

        /* try non-synched first */
        for (CacheEntry e = entry; e != null; e = e.nextEntry) {
            if (e.hash == hash && (q = e.get()) != null && eq(q, o)) {
                return q;
            }
        }

        /*
         * retry synched, only if the above attempt did not succeed, as another
         * thread may, in the meantime, have added a corresponding entry
         */
        final CacheSegment segment = this.segments[hash & SEGMENT_MASK];
        synchronized (segment) {
            entry = this.table[index];
            for (CacheEntry e = entry; e != null; e = e.nextEntry) {
                if (e.hash == hash && (q = e.get()) != null && eq(q, o)) {
                    return q;
                }
            }
        }
        return null;
    }

    /*
     * Recursively acquires locks on all 32 segments, extends the cache and
     * redistributes the entries.
     */
    private void rehash(final int index) {

        final CacheSegment seg = this.segments[index];
        synchronized (seg) {
            if (index > 0) {
                /* need to recursively acquire locks on all segments */
                rehash(index - 1);
            } else {
                /* double the amount of buckets */
                int newLength = this.table.length << 1;
                if (newLength > 0) { // no overflow?
                    /* reset segment counts */
                    for (int i = this.segments.length; --i >= 0;) {
                        this.segments[i].count = 0;
                    }

                    final CacheEntry[] newTable = new CacheEntry[newLength];

                    int hash, idx;
                    Object o;
                    newLength--;
                    for (int i = this.table.length; --i >= 0;) {
                        for (CacheEntry c = this.table[i]; c != null; c = c.nextEntry) {
                            if ((o = c.get()) != null) {
                                hash = c.hash;
                                idx = hash & newLength;
                                newTable[idx] = new CacheEntry(o, newTable[idx]);
                                this.segments[hash & SEGMENT_MASK].count++;
                            }
                        }
                    }
                    this.table = newTable;
                }
            }
        }
    }

    /**
     * Default constructor.
     *
     * @param c
     *            Runtime type of the objects that will be stored in the cache
     */
    public PropertyCache(final Class c) {
        this.useCache = Boolean.valueOf(
                System.getProperty("org.apache.fop.fo.properties.use-cache",
                        "true")).booleanValue();
        if (this.useCache) {
            for (int i = SEGMENT_MASK + 1; --i >= 0;) {
                this.segments[i] = new CacheSegment();
            }
        }
        this.runtimeType = c;
    }

    /**
     * Generic fetch() method. Checks if the given <code>Object</code> is
     * present in the cache - if so, returns a reference to the cached instance.
     * Otherwise the given object is added to the cache and returned.
     *
     * @param obj
     *            the Object to check for
     * @return the cached instance
     */
    private Object fetch(final Object obj) {
        if (!this.useCache) {
            return obj;
        }

        if (obj == null) {
            return null;
        }

        final Object cacheEntry = get(obj);
        if (cacheEntry != null) {
            return cacheEntry;
        }
        put(obj);
        return obj;
    }

    /**
     * Checks if the given {@link Property} is present in the cache - if so,
     * returns a reference to the cached instance. Otherwise the given object is
     * added to the cache and returned.
     *
     * @param prop
     *            the Property instance to check for
     * @return the cached instance
     */
    public Property fetch(final Property prop) {

        return (Property) fetch((Object) prop);
    }

    /**
     * Checks if the given {@link CommonHyphenation} is present in the cache -
     * if so, returns a reference to the cached instance. Otherwise the given
     * object is added to the cache and returned.
     *
     * @param chy
     *            the CommonHyphenation instance to check for
     * @return the cached instance
     */
    public CommonHyphenation fetch(final CommonHyphenation chy) {

        return (CommonHyphenation) fetch((Object) chy);
    }

    /**
     * Checks if the given {@link CommonFont} is present in the cache - if so,
     * returns a reference to the cached instance. Otherwise the given object is
     * added to the cache and returned.
     *
     * @param cf
     *            the CommonFont instance to check for
     * @return the cached instance
     */
    public CommonFont fetch(final CommonFont cf) {

        return (CommonFont) fetch((Object) cf);
    }

    /**
     * Checks if the given {@link CommonBorderPaddingBackground} is present in
     * the cache - if so, returns a reference to the cached instance. Otherwise
     * the given object is added to the cache and returned.
     *
     * @param cbpb
     *            the CommonBorderPaddingBackground instance to check for
     * @return the cached instance
     */
    public CommonBorderPaddingBackground fetch(
            final CommonBorderPaddingBackground cbpb) {

        return (CommonBorderPaddingBackground) fetch((Object) cbpb);
    }

    /**
     * Checks if the given {@link CommonBorderPaddingBackground.BorderInfo} is
     * present in the cache - if so, returns a reference to the cached instance.
     * Otherwise the given object is added to the cache and returned.
     *
     * @param bi
     *            the BorderInfo instance to check for
     * @return the cached instance
     */
    public CommonBorderPaddingBackground.BorderInfo fetch(
            final CommonBorderPaddingBackground.BorderInfo bi) {
        return (CommonBorderPaddingBackground.BorderInfo) fetch((Object) bi);
    }

    /**
     * Checks if the given {@link Marker.MarkerAttribute} is present in the
     * cache - if so, returns a reference to the cached instance. Otherwise the
     * given object is added to the cache and returned.
     *
     * @param ma
     *            the MarkerAttribute instance to check for
     * @return the cached instance
     */
    public Marker.MarkerAttribute fetch(final Marker.MarkerAttribute ma) {
        return (Marker.MarkerAttribute) fetch((Object) ma);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return super.toString() + "[runtimeType=" + this.runtimeType + "]";
    }

}

/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter;

/**
 * The Codename One equivalent of Flutter's {@code RestorationMixin}, applied to
 * a {@link State} subclass (Dart {@code class _FooState extends State<Foo> with
 * RestorationMixin}). The transpiler emits the applying class as a Java class
 * that {@code implements RestorationMixin}, so the mixin's members are reached
 * as inherited interface default methods.
 *
 * <p>Restoration is a no-op in Codename One: nothing is serialized or restored
 * across launches. {@link #registerForRestoration} simply notes that a property
 * is live (each {@link RestorableProperty} already holds its own value in a
 * field), and {@link #restoreState} is invoked once after {@code initState} so
 * subclasses can register their properties. The API is complete enough for the
 * gallery demos to build and behave correctly within a single session.</p>
 */
public interface RestorationMixin {

    /**
     * A stable identifier for this state's restoration scope. Overridden by the
     * applying State; the default (no scope) is null.
     */
    default String restorationId() {
        return null;
    }

    /** The restoration bucket for this scope; always null (no persistence). */
    default RestorationBucket bucket() {
        return null;
    }

    /**
     * Register the state's {@link RestorableProperty} instances. Called once
     * after the element is mounted (and again if the bucket changes, which never
     * happens here). Subclasses override this and call
     * {@link #registerForRestoration} for each property.
     */
    default void restoreState(RestorationBucket oldBucket, boolean initialRestore) {
    }

    /**
     * Wire a restorable property into this scope. With no persisted data the
     * property keeps the default value it was constructed with; we only mark it
     * registered so repeated registration is a no-op.
     */
    default void registerForRestoration(RestorableProperty<?> property, String restorationId) {
        if (property != null) {
            property.markRegistered();
        }
    }

    /** Detach a property from this scope. */
    default void unregisterFromRestoration(RestorableProperty<?> property) {
        if (property != null) {
            property.markUnregistered();
        }
    }

    /** Bucket-change hook; a no-op because the bucket is always null. */
    default void didToggleBucket(RestorationBucket oldBucket) {
    }
}

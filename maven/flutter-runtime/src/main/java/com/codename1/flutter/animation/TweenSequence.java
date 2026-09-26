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
package com.codename1.flutter.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Animatable} that pieces together a series of weighted
 * {@link TweenSequenceItem}s over the 0..1 interval — Flutter's
 * {@code TweenSequence<T>}. {@link #transform} locates the active segment by
 * cumulative weight and evaluates that item's tween over its local range.
 */
public class TweenSequence<T> extends Animatable<T> {

    private final List<TweenSequenceItem<T>> items = new ArrayList<TweenSequenceItem<T>>();
    private double totalWeight;

    @SuppressWarnings("unchecked")
    public TweenSequence(Object items) {
        if (items instanceof Iterable) {
            for (Object o : (Iterable<?>) items) {
                add((TweenSequenceItem<T>) o);
            }
        }
    }

    private void add(TweenSequenceItem<T> item) {
        if (item != null) {
            items.add(item);
            totalWeight += item.weight();
        }
    }

    @Override
    public T transform(double t) {
        if (items.isEmpty()) {
            return null;
        }
        if (t <= 0.0) {
            return items.get(0).tween().transform(0.0);
        }
        if (t >= 1.0) {
            TweenSequenceItem<T> last = items.get(items.size() - 1);
            return last.tween().transform(1.0);
        }
        double start = 0.0;
        for (TweenSequenceItem<T> item : items) {
            double span = item.weight() / totalWeight;
            double end = start + span;
            if (t < end || item == items.get(items.size() - 1)) {
                double local = span == 0.0 ? 0.0 : (t - start) / span;
                return item.tween().transform(local);
            }
            start = end;
        }
        return items.get(items.size() - 1).tween().transform(1.0);
    }
}

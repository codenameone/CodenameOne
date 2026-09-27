/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.samples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class SampleList implements Iterable<Sample> {
    private List<Sample> samples = new ArrayList<>();

    @Override
    public Iterator<Sample> iterator() {
        return samples.iterator();
    }
    
    public void add(Sample sample) {
        samples.add(sample);
    }
    
    public SampleList filter(SamplesContext context, String searchTerm) {
        SampleList out = new SampleList();
        for (Sample sample : this) {
            if (sample.matchesSearch(context, searchTerm)) {
                out.add(sample);
            }
        }
        return out;
        
    }
    
    public void sort() {
        Collections.sort(samples, new Comparator<Sample>() {
            @Override
            public int compare(Sample o1, Sample o2) {
                return o1.getName().compareTo(o2.getName());
            }
            
        });
    }
    
}

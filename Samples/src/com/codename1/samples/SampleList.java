/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author shannah
 */
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

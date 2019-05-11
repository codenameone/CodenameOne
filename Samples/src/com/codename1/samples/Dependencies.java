/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author shannah
 */
public class Dependencies implements Iterable<Dependency> {
    private List<Dependency> deps = new ArrayList<>();
    
    @Override
    public Iterator<Dependency> iterator() {
        return deps.iterator();
    }

    public void add(Dependency dep) {
        deps.add(dep);
    }
    
}

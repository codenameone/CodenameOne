/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author shannah
 */
public class StreamRegistry {
    
    private long nextId = 1;
    private static long MAX_ID=99999999l;
    private Map<String,StreamWrapper> streamRegistry = new HashMap<String,StreamWrapper>();
    
    public String registerStream(StreamWrapper wrapper) {
        String id;
        synchronized(streamRegistry) {
            id = ""+(nextId++);
            
            while (streamRegistry.containsKey(id)) {
                id = ""+(nextId++);
            }
            
            if (nextId > MAX_ID) {
                nextId = 1;
            }
            
        }
        streamRegistry.put(id, wrapper);
        return id;
    }
    
    public StreamWrapper getStream(String id) {
        return streamRegistry.get(id);
    }
    
    public boolean removeStream(StreamWrapper wrapper) {
        String key = null;
        for (String k : streamRegistry.keySet()) {
            StreamWrapper w = streamRegistry.get(k);
            if (w == wrapper) {
                key = k;
                break;
            }
        }
        if (key == null) {
            return false;
        }
        streamRegistry.remove(key);
        return true;
    }
}

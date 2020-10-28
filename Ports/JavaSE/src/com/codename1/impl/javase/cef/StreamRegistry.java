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

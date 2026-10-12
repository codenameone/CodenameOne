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
package com.codename1.designer.css;

import java.io.File;

public class PollingFileWatcher  {

    private File[] files;
    private long interval;
    private long[] lastMtime;
    private boolean stop;
    
    public PollingFileWatcher(File[] files, long interval) {
        this.files = files;
        this.interval = interval;
        this.lastMtime = lastModified();
    }
    
    // One time per file, 0 for a file that is not there. The newest of them
    // alone would not move when an older file is edited back, or deleted.
    private long[] lastModified() {
        long[] out = new long[files.length];
        for (int i = 0; i < files.length; i++) {
            out[i] = files[i].lastModified();
        }
        return out;
    }
    
    public synchronized void poll() throws InterruptedException {
        while (!stop && java.util.Arrays.equals(lastModified(), lastMtime)) {
            wait(interval);
        }
        long[] now = lastModified();
        java.util.List<File> moved = new java.util.ArrayList<File>();
        for (int i = 0; i < now.length; i++) {
            if (now[i] != lastMtime[i]) {
                moved.add(files[i]);
            }
        }
        changed = moved.toArray(new File[moved.size()]);
        lastMtime = now;
    }

    private File[] changed = new File[0];

    /// The files whose time moved, either way, at the poll that last
    /// returned; none before the first.
    public synchronized File[] changed() {
        return changed.clone();
    }
    
    public synchronized void stop() {
        stop = true;
        notifyAll();
    }
    
    
}

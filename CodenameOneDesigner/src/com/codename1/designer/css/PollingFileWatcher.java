/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.designer.css;

import java.io.File;

/**
 *
 * @author shannah
 */
public class PollingFileWatcher  {

    private File file;
    private long interval;
    private long lastMtime;
    private boolean stop;
    
    public PollingFileWatcher(File file, long interval) {
        this.file = file;
        this.interval = interval;
        this.lastMtime = file.lastModified();
    }
    
    public synchronized void poll() throws InterruptedException {
        while (!stop && file.lastModified() == lastMtime) {
            wait(interval);
        }
        lastMtime = file.lastModified();
    }
    
    public synchronized void stop() {
        stop = true;
        notifyAll();
    }
    
    
}

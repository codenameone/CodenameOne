/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.location;

/**
 *
 * @author Chen
 */
public class LocationRequest {
    
    /**
     * When you need gps location updates
     */
    public static int PRIORITY_HIGH_ACCUARCY = 0;

    /**
     * When accuracy is not highly important and you want to save battery
     */
    public static int PRIORITY_MEDIUM_ACCUARCY = 1;

    /**
     * When accuracy is not important and you want to save battery
     */
    public static int PRIORITY_LOW_ACCUARCY = 2;
    
    private int priority = PRIORITY_MEDIUM_ACCUARCY;
    
    private long interval = 5000;

    public LocationRequest() {
    }
    
    public LocationRequest(int priority, long interval) {
        this.priority = priority;
        this.interval = interval;
    }

    public int getPriority() {
        return priority;
    }

    public long getInterval() {
        return interval;
    }
    
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.simulator;

import java.util.EventObject;

/**
 *
 * @author shannah
 */
public class AppEvent {

    private EventObject sourceEvent;

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the type
     */
    public EventType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(EventType type) {
        this.type = type;
    }
    public static enum EventType {
        SwingEvent
    }
    
    private int width;
    private int height;
    private EventType type;
    
    public AppEvent(EventType type) {
        this.type = type;
    }
    public AppEvent(EventObject sourceEvent) {
        this.type = EventType.SwingEvent;
        this.sourceEvent = sourceEvent;
    }

    public EventObject getSourceEvent() {
        return sourceEvent;
    }

    public Object getSource() {
        if (sourceEvent == null) return null;
        return sourceEvent.getSource();
    }

    
    
}

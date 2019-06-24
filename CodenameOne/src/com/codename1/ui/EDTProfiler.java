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
package com.codename1.ui;

/**
 * An interface that can be implemented and registered to receive callbacks on the
 * event dispatch thread (EDT).  This allows for measuring the time taken to draw
 * frames in the UI.
 * @author shannah
 * @since 7.0
 */
public interface EDTProfiler {
    /**
     * Called at the start of rendering a frame.
     */
    public void startFrame();
    
    /**
     * Called when rendering a frame is complete.
     */
    public void endFrame();
    
    /**
     * Called before painting dirty components for a frame.
     */
    public void startPaintDirty();
    
    /**
     * Called after finishing painting dirty components for a frame.
     */
    public void endPaintDirty();
    
    /**
     * Called before repainting animations for a frame.
     */
    public void startRepaintAnimations();
    
    /**
     * Called after repainting animations for a frame.
     */
    public void endRepaintAnimations();
    
    /**
     * Called before processing serial calls for a frame.
     */
    public void startProcessSerialCalls();
    
    /**
     * Called after processing serial calls for a frame.
     */
    public void endProcessSerialCalls();
    
}

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

package com.codename1.payment;

/**
 * Callback interface that the main class must implement in order for the restore 
 * option of in-app-purchasing to work. Once the main class implements this 
 * interface the methods within it are invoked to indicate the various restore states.
 *
 * @author Steve Hannah
 */
public interface RestoreCallback {
    /**
     * Indicates a the given SKU was restored by a user. When restoring multiple 
     * SKU's at once multiple calls to this method will be performed.
     * @param sku the sku purchased
     */
    public void itemRestored(String sku);
    
    /**
     * Indicates that a {@link Purchase#restore()} request was completed without
     * errors.  It doesn't mean that any particular products were restored, only
     * that the request completed.  After a call to {@link Purchase#restore()}, 
     * either this or {@link #restoreRequestError} will be called at some point.
     */
    public void restoreRequestComplete();
    
    /**
     * Indicates that a {@link Purchase#restore()} request was completed with
     * errors.  It doesn't mean that any particular products were restored, only
     * that the request completed.  After a call to {@link Purchase#restore()}, 
     * either this or {@link #restoreRequestComplete} will be called at some point.
     * 
     * @param message The error message.
     */
    public void restoreRequestError(String message);
}

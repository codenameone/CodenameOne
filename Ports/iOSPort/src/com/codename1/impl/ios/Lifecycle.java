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
package com.codename1.impl.ios;

/**
 *
 * @author Shai Almog
 */
public class Lifecycle {
    /**
     * Sent when the application is about to move from active to inactive state. 
     * This can occur for certain types of temporary interruptions (such as an 
     * incoming phone call or SMS message) or when the user quits the application 
     * and it begins the transition to the background state.
     * Use this method to pause ongoing tasks, disable timers, and throttle down 
     * OpenGL ES frame rates. Games should use this method to pause the game.
     */
    public void applicationWillResignActive() {
    }

    /**
     * Use this method to release shared resources, save user data, invalidate 
     * timers, and store enough application state information to restore your 
     * application to its current state in case it is terminated later.
     * If your application supports background execution, this method is called 
     * instead of applicationWillTerminate: when the user quits.
     */
    public void applicationDidEnterBackground() {
    }

    /**
     * Use this method to release shared resources, save user data, invalidate 
     * timers, and store enough application state information to restore your 
     * application to its current state in case it is terminated later.
     * If your application supports background execution, this method is called 
     * instead of applicationWillTerminate: when the user quits.
     */
    public void applicationWillEnterForeground() {
    }
    
    /**
     * Called as part of the transition from the background to the inactive state; 
     * here you can undo many of the changes made on entering the background.
     */
    public void applicationDidBecomeActive() {
    }
    
    /**
     * Restart any tasks that were paused (or not yet started) while the 
     * application was inactive. If the application was previously in the background, 
     * optionally refresh the user interface.
     */
    public void applicationWillTerminate() {
    }

    /**
     * Indicates whether the application should handle the given URL, defaults to true
     * @param url the URL to handle
     * @param caller the invoking application
     * @return true to handle the URL, false otherwise
     */
    public boolean shouldApplicationHandleURL(String url, String caller) {
        return true;
    }
    
    /**
     * Headphones connected callback
     */
    public void headphonesConnected() {
    }

    /**
     * Headphones disconnected callback
     */
    public void headphonesDisconnected() {
    }
}

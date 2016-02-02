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
package com.codename1.notifications;

/**
 * <p>An interface that can be implemented by the application's main class (i.e.
 * the class with <code>start()</code>, <code>stop()</code> etc..) to respond
 * to local notifications.  This interface only works when it is implemented
 * by the main class.  It will never be called otherwise.</p>
 * 
 * <p><em>IMPORTANT:  THIS CALLBACK IS CALLED OFF THE EDT.  ANY UPDATES TO THE UI
     * WILL NEED TO OCCUR INSIDE A <code>callSerially()</code> block.</em></p>
 * 
 * @author shannah
 * @see LocalNotification
 */
public interface LocalNotificationCallback {
    
    /**
     * <p>Callback method that is called when a local notification is received AND
     * the application is active. This won't necessarily be called when the 
     * notification is received.  If the app is in the background, for example, 
     * the notification will manifest itself as a message to the user's task bar
     * (or equivalent).  If the user then clicks on the notification message, the
     * app will be activated, and this callback method will be called.</p>
     * 
     * <p><em>IMPORTANT:  THIS CALLBACK IS CALLED OFF THE EDT.  ANY UPDATES TO THE UI
     * WILL NEED TO OCCUR INSIDE A <code>callSerially()</code> block.</em></p>
     * 
     * @param notificationId The notification ID of the notification that was received.
     * @see LocalNotification
     */
    public void localNotificationReceived(String notificationId);
}

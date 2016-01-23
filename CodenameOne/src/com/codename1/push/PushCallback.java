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
package com.codename1.push;

/**
 * This callback interface is invoked when a push notification is received 
 * by the application. If the main class of the application implements 
 * push callback it will receive push notification calls from the system.
 * Notice that its very possible that a separate instance of the main class
 * will be created to perform the push!
 *
 * @author Shai Almog
 */
public interface PushCallback {
    /**
     * Error code returned when sending a push notification
     */
    public static final int REGISTRATION_ERROR_SERVICE_NOT_AVAILABLE = 1;

    /**
     * Error code returned when sending a push notification
     */
    public static final int REGISTRATION_ACCOUNT_MISSING = 2;

    /**
     * Error code returned when sending a push notification
     */
    public static final int REGISTRATION_AUTHENTICATION_FAILED = 3;

    /**
     * Error code returned when sending a push notification
     */
    public static final int REGISTRATION_TOO_MANY_REGISTRATIONS = 4;

    /**
     * Error code returned when sending a push notification
     */
    public static final int REGISTRATION_INVALID_SENDER = 5;

    /**
     * Error code returned when sending a push notification
     */
    public static final int REGISTRATION_PHONE_REGISTRATION_ERROR = 6;
    
    /**
     * Invoked when the push notification occurs
     * 
     * @param value the value of the push notification
     */
    public void push(String value);
    
    /**
     * Invoked when push registration is complete to pass the device ID to the application.
     * 
     * @param deviceId OS native push id you should not use this value and instead use <code>Push.getPushKey()</code>
     * @see Push#getPushKey() 
     */
    public void registeredForPush(String deviceId);
    
    /**
     * Invoked to indicate an error occurred during registration for push notification
     * @param error descriptive error string
     * @param errorCode an error code
     */
    public void pushRegistrationError(String error, int errorCode);
}

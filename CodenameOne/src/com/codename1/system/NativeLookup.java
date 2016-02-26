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
package com.codename1.system;

import java.util.HashMap;

/**
 * Creates an instance of the native interface which will call the underlying
 * platform using the convention documented in the package docs.
 * To understand more 
 * about native interfaces you can check out this 
 * <a href="https://www.codenameone.com/how-do-i---access-native-device-functionality-invoke-native-interfaces.html">
 * quick "How Do I?" tutorial</a>.<br>
 * Alternatively you can dig deeper into <a href="https://www.codenameone.com/blog/integrating-3rd-party-native-sdks-part-1.html">
 * this tutorial for integrating 3rd party native libraries</a>.
 * 
 * @author Shai Almog
 */
public class NativeLookup {
    /**
     * Indicates whether stack traces should be printed when lookup fails
     */
    private static boolean verbose = true;
    private static HashMap<Class, Class> interfaceToClassLookup; 

    /**
     * Indicates whether stack traces should be printed when lookup fails
     * @return the verbose
     */
    public static boolean isVerbose() {
        return verbose;
    }

    /**
     * Indicates whether stack traces should be printed when lookup fails
     * @param aVerbose the verbose to set
     */
    public static void setVerbose(boolean aVerbose) {
        verbose = aVerbose;
    }
    private NativeLookup() {}
    
    /**
     * Creates an instance of the given native interface and returns it for
     * user callbacks.
     * 
     * @param c the class of the NativeInterface sub interface
     * @return an instance of that interface that can be invoked or null if the native interface isn't
     * present on the underlying platform (e.g. simulator platform).
     */
    public static NativeInterface create(Class c) {
        try {
            if(interfaceToClassLookup != null) {
                Class cls = interfaceToClassLookup.get(c);
                if(cls == null) {
                    return null;
                }
                return (NativeInterface)cls.newInstance();
            }
            // special case for JavaSE native interfaces
            return (NativeInterface)Class.forName(c.getName() + "Impl").newInstance();
        } catch (Throwable ex) {
            if(verbose) {
                ex.printStackTrace();
            }
        } 
        return null;
    }
    
    /**
     * Do NOT invoke this method. This method is invoked internally by the stub to register the implementation class 
     * that matches a specific interface type. 
     * 
     * @param ni the native interface
     * @param cls the stub class matching said interface
     */
    public static void register(Class ni, Class cls) {
        if(interfaceToClassLookup == null) {
            interfaceToClassLookup = new HashMap<Class, Class>();
        }
        interfaceToClassLookup.put(ni, cls);
    }
}

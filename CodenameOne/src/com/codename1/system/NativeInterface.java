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

/**
 * This is a marker interface that should be extended by a user interface in order
 * to indicate that said interface is implemented in native code. To understand more 
 * about native interfaces you can check out this 
 * <a href="https://www.codenameone.com/how-do-i---access-native-device-functionality-invoke-native-interfaces.html">
 * quick "How Do I?" tutorial</a>.<br />
 * Alternatively you can dig deeper into <a href="https://www.codenameone.com/blog/integrating-3rd-party-native-sdks-part-1.html">
 * this tutorial for integrating 3rd party native libraries</a>.
 *
 * @author Shai Almog
 */
public interface NativeInterface {
    /**
     * Indicates whether this native interface is supported on the current platform
     * 
     * @return true if the native interface is supported on the given platform
     */
    public boolean isSupported();
}

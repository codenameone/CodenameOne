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

package com.codename1.social;

import com.codename1.ui.Display;

/**
 * Class implementing the facebook API
 * 
 * @author Shai Almog
 */
class FacebookImpl extends Facebook {
    @Override
    public boolean isFacebookSDKSupported() {
        return true;
    }
    
    @Override
    public void login() {
    }
    
    @Override
    public boolean isLoggedIn() {
        return getToken() != null;
    }
    
    @Override
    public String getToken() {
        return Display.getInstance().getProperty("facebook.token", null);
    }
    
    @Override
    public void logout() {
        Display.getInstance().setProperty("facebook.token", null);
    }

}

/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android.compat.app;

import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflection wrapper for <a href="https://developer.android.com/reference/android/support/v4/app/RemoteInput">RemoteInput</a>
 */
public class RemoteInputWrapper {
    public final Object internal;
    
    public RemoteInputWrapper(Object internal) {
        this.internal = internal;
    }
        
    public static Class cls() {
        try {
            return Class.forName("android.support.v4.app.RemoteInput");
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }
    
    public static boolean isSupported() {
        try {
            Class.forName("android.support.v4.app.RemoteInput");
            return true;
        } catch (ClassNotFoundException cnfe) {
            return false;
        }
    }


    public static Bundle getResultsFromIntent(Intent intent) {
        try {
            Method m = cls().getMethod("getResultsFromIntent", Intent.class);
            return (Bundle)m.invoke(null, intent);

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    public static class BuilderWrapper {
        public final Object internal;
        
        public BuilderWrapper(String resultKey) {
            try {
                Constructor c = cls().getConstructor(String.class);
                internal = c.newInstance(resultKey);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        
        public static Class cls() {
            try {
                return Class.forName("android.support.v4.app.RemoteInput$Builder");
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            }
        }
        
        public BuilderWrapper setLabel(String label) {
            try {
                Method m = cls().getMethod("setLabel", CharSequence.class);
                m.invoke(internal, label);
                
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            return this;
        }
        
        public RemoteInputWrapper build() {
            try {
                Method m = cls().getMethod("build");
                Object remoteInput = m.invoke(internal);
                return new RemoteInputWrapper(remoteInput);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            
        }
    }
    
}

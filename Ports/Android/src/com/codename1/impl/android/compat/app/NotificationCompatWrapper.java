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

import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A wrapper class for the {@link android.support.v4.app.NotificationCompat} class.  This provides 
 * runtime access to some of the methods and classes that were added in later APIs so that 
 * we can use these without breaking compilation against older APIs.
 * 
 * https://developer.android.com/reference/android/support/v4/app/NotificationCompat
 */
public class NotificationCompatWrapper {
    public final Object internal;
    public NotificationCompatWrapper(NotificationCompat internal) {
        this.internal = internal;
    }
    
    /**
     * Reflection wrapper for <a href="https://developer.android.com/reference/android/support/v4/app/NotificationCompat/Builder">NotificationCompat.Builder</a>.
     */
    public static class BuilderWrapper {
        public final Object internal;
        public static Class cls() {
            try {
                return Class.forName("android.support.v4.app.NotificationCompat$Builder");
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            }
        }
        public BuilderWrapper(Object internal) {
            this.internal = internal;
        }
        
        public BuilderWrapper addAction(ActionWrapper action) {
            try {
                Method m = cls().getMethod("addAction", ActionWrapper.cls());
                m.invoke(internal, action.internal);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            return this;
        }
    }
    
    /**
     * Reflection wrapper for <a href="https://developer.android.com/reference/android/support/v4/app/NotificationCompat/Action">NotificationCompat.Action</a>.
     */
    public static class ActionWrapper {
        public final Object internal;
        
        public ActionWrapper(Object internal) {
            this.internal = internal;
        }
        
        public static Class cls() {
            try {
                return Class.forName("android.support.v4.app.NotificationCompat$Action");
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            }
        }
        
        /**
         * Reflection wrapper for <a href="https://developer.android.com/reference/android/support/v4/app/NotificationCompat/Action/Builder">NotificationCompat.Action.Builder</a>.
         */
        public static class BuilderWrapper {
            public final Object internal;
            public static Class cls() {
                try {
                    return Class.forName("android.support.v4.app.NotificationCompat$Action$Builder");
                } catch (ClassNotFoundException cnfe) {
                    throw new RuntimeException(cnfe);
                }
            }

            public static boolean isSupported() {
                try {
                    Class.forName("android.support.v4.app.NotificationCompat$Action$Builder");
                    return true;
                } catch (ClassNotFoundException t) {
                    return false;
                }
            }

            public BuilderWrapper(int icon, CharSequence title, PendingIntent intent) {
                try {
                    Constructor constr = cls().getConstructor(int.class, CharSequence.class, PendingIntent.class);
                    internal = constr.newInstance(icon, title, intent);
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }

            public BuilderWrapper addRemoteInput(RemoteInputWrapper input) {
                try {
                    Method m = cls().getMethod("addRemoteInput", RemoteInputWrapper.cls());
                    m.invoke(internal, input.internal);

                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
                return this;
            }
            
            public ActionWrapper build() {
                try {
                    Method m = cls().getMethod("build");
                    Object action = m.invoke(internal);
                    return new ActionWrapper(action);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
        
    }
}

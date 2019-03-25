/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author shannah
 * 
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

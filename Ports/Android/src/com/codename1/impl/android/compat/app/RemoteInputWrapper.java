/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.android.compat.app;

import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflection wrapper for <a href="https://developer.android.com/reference/android/support/v4/app/RemoteInput">RemoteInput</a>
 * 
 * @author shannah
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

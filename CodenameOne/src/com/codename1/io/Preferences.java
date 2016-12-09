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
package com.codename1.io;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.ArrayList;

/**
 * <p>Simple map like class to store application and Codename One preference 
 * settings in the {@link com.codename1.io.Storage}. <br>
 * Simple usage of the class for storing a {@code String} token:</p>
 * 
 * <script src="https://gist.github.com/codenameone/fc7693ef69108e90057c.js"></script>
 * 
 * <p>
 * Notice that this class might get somewhat confusing with primitive numbers e.g. if you use 
 * {@code Preferences.set("primitiveLongValue", myLongNumber)} then invoke 
 * {@code Preferences.get("primitiveLongValue", 0)} you might get an exception!<br>
 * This would happen because the value is physically a {@code Long} object but you are trying to get an 
 * {@code Integer}. <br>
 * The workaround is to remain consistent and use code like this {@code Preferences.get("primitiveLongValue", (long)0)}.
 * </p>
 *
 * @author Shai Almog
 * @author Miguel Mu\u00f1oz
 */
public class Preferences {
    private static Hashtable<String, Object> p;
    private static final HashMap<String, ArrayList<PreferenceListener>> listenerMap
        = new HashMap<String, ArrayList<PreferenceListener>>();
    
    /**
     * Block instantiation of preferences 
     */
    Preferences() {}
    
    private static Hashtable<String, Object> get() {
        if(p == null) {
            if(Storage.getInstance().exists("CN1Preferences")) {
                p = (Hashtable<String, Object>)Storage.getInstance().readObject("CN1Preferences");
            }
            if(p == null) {
                p = new Hashtable<String, Object>();
            }
        }
        return p;
    }
    
    private static void save() {
        Storage.getInstance().writeObject("CN1Preferences", p);
    }
    
    /**
     * Sets a preference value, supported values are Strings, numbers and boolean
     * 
     * @param pref the key any unique none null value that doesn't start with cn1
     * @param o a String a number or boolean
     */
    private static void set(String pref, Object o) {
        Object prior = get(pref, null);
        if(o == null) {
            get().remove(pref);
        } else {
            get().put(pref, o);
        }
        save();
        fireChange(pref, prior, o);
    }

    /**
     * Sets a preference value
     * 
     * @param pref the key any unique none null value that doesn't start with cn1
     * @param s a String 
     */
    public static void set(String pref, String s) {
        set(pref, (Object)s);
    }

    /**
     * Sets a preference value
     * 
     * @param pref the key any unique none null value that doesn't start with cn1
     * @param i a number
     */
    public static void set(String pref, int i) {
        set(pref, new Integer(i));
    }

    /**
     * Sets a preference value
     * 
     * @param pref the key any unique none null value that doesn't start with cn1
     * @param l a number
     */
    public static void set(String pref, long l) {
        set(pref, new Long(l));
    }

    /**
     * Sets a preference value
     * 
     * @param pref the key any unique none null value that doesn't start with cn1
     * @param d a number
     */
    public static void set(String pref, double d) {
        set(pref, new Double(d));
    }

    /**
     * Sets a preference value
     * 
     * @param pref the key any unique none null value that doesn't start with cn1
     * @param f a number
     */
    public static void set(String pref, float f) {
        set(pref, new Float(f));
    }
    
    /**
     * Deletes a value for the given setting
     * 
     * @param pref the preference value
     */
    public static void delete(String pref) {
        Object prior = get(pref, null);
        get().remove(pref);
        save();        
        fireChange(pref, prior, null);
    }

    /**
     * Remove all preferences
     */
    public static void clearAll() {
        // We only need to save prior values for Preferences that actually have listeners.
        Hashtable<String, Object> priorValues = null;
        if (!listenerMap.keySet().isEmpty()) {

            // Save all the Preferences for which there are registered listeners.
            priorValues = new Hashtable<String, Object>();
            for (String key : listenerMap.keySet()) {
                final Object currentValue = get().get(key);
                // We can't put null values in the hashtable. But we don't need to, because if we could we'd just be calling 
                // fireChange(Pref, null, null) and fireChange would do nothing.
                if (currentValue != null) {
                    priorValues.put(key, currentValue);
                }
            }
        }
        get().clear();
        save();        
        if (priorValues != null) {
            for (String key : listenerMap.keySet()) {
                fireChange(key, priorValues.get(key), null);
            }
        }
    }

    /**
     * Sets a preference value
     * 
     * @param pref the key any unique none null value that doesn't start with cn1
     * @param b the value
     */
    public static void set(String pref, boolean b) {
        if(b) {
            set(pref, Boolean.TRUE);
        } else {
            set(pref, Boolean.FALSE);
        }
    }
    
    /**
     * Gets the value as a String
     * @param pref the preference key
     * @param def the default value
     * @return the default value or the value
     */
    public static String get(String pref, String def) {
        Object t = get().get(pref);
        if(t == null) {
            return def;
        }
        return t.toString();
    }

    /**
     * Gets the value as a number
     * @param pref the preference key
     * @param def the default value
     * @return the default value or the value
     */
    public static int get(String pref, int def) {
        Integer t = (Integer)get().get(pref);
        if(t == null) {
            return def;
        }
        return t.intValue();
    }

    /**
     * Gets the value as a number
     * @param pref the preference key
     * @param def the default value
     * @return the default value or the value
     */
    public static long get(String pref, long def) {
        Long t = (Long)get().get(pref);
        if(t == null) {
            return def;
        }
        return t.longValue();
    }

    /**
     * Gets the value as a number
     * @param pref the preference key
     * @param def the default value
     * @return the default value or the value
     */
    public static double get(String pref, double def) {
        Double t = (Double)get().get(pref);
        if(t == null) {
            return def;
        }
        return t.doubleValue();
    }

    /**
     * Gets the value as a number
     * @param pref the preference key
     * @param def the default value
     * @return the default value or the value
     */
    public static float get(String pref, float def) {
        Float t = (Float)get().get(pref);
        if(t == null) {
            return def;
        }
        return t.floatValue();
    }

    /**
     * Gets the value as a number
     * @param pref the preference key
     * @param def the default value
     * @return the default value or the value
     */
    public static boolean get(String pref, boolean def) {
        Boolean t = (Boolean)get().get(pref);
        if(t == null) {
            return def;
        }
        return t.booleanValue();
    }

    /**
     * Fires the PreferenceListeners if priorValue and value are not equal.
     *
     * @param pref       The preference name
     * @param priorValue The prior value, which may be null
     * @param value      The new value, which may be null
     */
    private static void fireChange(final String pref, final Object priorValue, final Object value) {
        //noinspection EqualsReplaceableByObjectsCall,ObjectEquality
        boolean valueChanged = (priorValue != value) && ((priorValue == null) || !priorValue.equals(value));
        if(valueChanged) {
            ArrayList<PreferenceListener> listenerList = listenerMap.get(pref);
            if(listenerList != null) {
                // Loop backwards, in case the listener removes itself.
                for(int i = listenerList.size() - 1; i >= 0; --i) {
                    // either value could be null
                    PreferenceListener listener = listenerList.get(i);
                    listener.preferenceChanged(pref, priorValue, value);
                }
            }
        }
    }

    /**
     * Adds a preference listener for the specified property to the list of listeners. When calling this method, it is
     * advisable to also read the current value and set it, since the value may have changed since the last time the
     * listener was removed. (Should this return the current value of the preference?)
     *
     * @param pref     The preference to listen to
     * @param listener The listener to add, which cannot be null.
     */
    public static void addPreferenceListener(String pref, PreferenceListener listener) {
        if(listener == null) {
            // fail fast. Without this, it will fail when the listener is fired, when it's harder to trace back.
            throw new NullPointerException("Null PreferenceListener not allowed");
        }
        ArrayList<PreferenceListener> listenerList = listenerMap.get(pref);
        if(listenerList == null) {
            listenerList = new ArrayList<PreferenceListener>();
            listenerMap.put(pref, listenerList);
        }
        listenerList.add(listener);
    }

    /**
     * Remove the listener for the specified preference.
     *
     * @param pref     The preference that the listener listens to
     * @param listener The listener to remove
     * @return true if the listener was removed, false if it was not found.
     */
    public static boolean removePreferenceListener(String pref, PreferenceListener listener) {
        ArrayList<PreferenceListener> listenerList = listenerMap.get(pref);
        if(listenerList != null) {
            return listenerList.remove(listener);
        }
        return false;
    }
}

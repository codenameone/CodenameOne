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
package com.codename1.media.audio;

/// Declares one [AudioParam] of an [AudioProcessorNode], the Web Audio
/// `AudioParamDescriptor`.
public final class AudioParamDescriptor {
    private final String name;
    private final float defaultValue;
    private final float minValue;
    private final float maxValue;
    private final AutomationRate rate;

    /// An audio rate parameter with no range.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter's name
    ///
    /// - `defaultValue`: its initial value
    public AudioParamDescriptor(String name, float defaultValue) {
        this(name, defaultValue, -Float.MAX_VALUE, Float.MAX_VALUE, AutomationRate.A_RATE);
    }

    /// A parameter with a range and a rate.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter's name
    ///
    /// - `defaultValue`: its initial value
    ///
    /// - `minValue`: the lowest value it takes
    ///
    /// - `maxValue`: the highest value it takes
    ///
    /// - `rate`: audio or control rate
    public AudioParamDescriptor(String name, float defaultValue, float minValue, float maxValue,
            AutomationRate rate) {
        if (name == null) {
            throw new IllegalArgumentException("name");
        }
        if (minValue > maxValue || defaultValue < minValue || defaultValue > maxValue) {
            throw new IllegalArgumentException("defaultValue must lie between minValue and maxValue");
        }
        this.name = name;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.rate = rate == null ? AutomationRate.A_RATE : rate;
    }

    /// #### Returns
    ///
    /// the name
    public String getName() {
        return name;
    }

    /// #### Returns
    ///
    /// the initial value
    public float getDefaultValue() {
        return defaultValue;
    }

    /// #### Returns
    ///
    /// the lowest value
    public float getMinValue() {
        return minValue;
    }

    /// #### Returns
    ///
    /// the highest value
    public float getMaxValue() {
        return maxValue;
    }

    /// #### Returns
    ///
    /// the automation rate
    public AutomationRate getAutomationRate() {
        return rate;
    }
}

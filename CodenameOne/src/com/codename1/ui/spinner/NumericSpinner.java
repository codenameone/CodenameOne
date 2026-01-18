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
package com.codename1.ui.spinner;

import com.codename1.ui.layouts.BorderLayout;

/**
 * A spinner class that allows picking a number
 *
 * @author Shai Almog
 * @deprecated use Picker instead
 */
public class NumericSpinner extends BaseSpinner {
    private Spinner spin;
    private double min = 0;
    private double max = 1000;
    private double value = 0;
    private double step = 1;

    /**
     * Default constructor
     */
    public NumericSpinner() {
        setLayout(new BorderLayout());
    }

    /**
     * Default constructor
     */
    void initSpinner() {
        if (spin == null) {
            spin = createSpinner();
            addComponent(BorderLayout.CENTER, spin);
        }
    }

    Spinner createSpinner() {
        return Spinner.create(min, max, value, step);
    }

    /**
     * The minimum value for the spinner
     *
     * @return the min
     */
    public double getMin() {
        return min;
    }

    /**
     * The minimum value for the spinner
     *
     * @param min the min to set
     */
    public void setMin(double min) {
        this.min = min;
        if (min > value) {
            value = min;
        }
        if (spin != null) {
            spin.setModel(new SpinnerNumberModel(min, max, value, step));
        }
    }

    /**
     * The maximum value for the spinner
     *
     * @return the max
     */
    public double getMax() {
        return max;
    }

    /**
     * The maximum value for the spinner
     *
     * @param max the max to set
     */
    public void setMax(double max) {
        this.max = max;
        if (max < value) {
            value = max;
        }
        if (spin != null) {
            spin.setModel(new SpinnerNumberModel(min, max, value, step));
        }
    }

    /**
     * The value for the spinner
     *
     * @return the value
     */
    public double getValue() {
        if (spin != null) {
            return ((Double) ((SpinnerNumberModel) spin.getModel()).getValue()).doubleValue();
        }
        return value;
    }

    /**
     * The value for the spinner
     *
     * @param value the value to set
     */
    public void setValue(double value) {
        this.value = value;
        if (spin != null) {
            ((SpinnerNumberModel) spin.getModel()).setValue(Double.valueOf(value));
        }
    }

    /**
     * Step for spinner gap
     *
     * @return the step
     */
    public double getStep() {
        return step;
    }

    /**
     * Step for spinner gap
     *
     * @param step the step to set
     */
    public void setStep(double step) {
        this.step = step;
        if (spin != null) {
            spin.setModel(new SpinnerNumberModel(min, max, value, step));
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[]{"min", "max", "value", "step"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
        return new Class[]{Double.class, Double.class, Double.class, Double.class};
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[]{"double", "double", "double", "double", "double"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if ("min".equals(name)) {
            return Double.valueOf(min);
        }
        if ("max".equals(name)) {
            return Double.valueOf(max);
        }
        if ("value".equals(name)) {
            return Double.valueOf(getValue());
        }
        if ("step".equals(name)) {
            return Double.valueOf(step);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if ("min".equals(name)) {
            setMin(Double.parseDouble(value.toString()));
            return null;
        }
        if ("max".equals(name)) {
            setMax(Double.parseDouble(value.toString()));
            return null;
        }
        if ("value".equals(name)) {
            setValue(Double.parseDouble(value.toString()));
            return null;
        }
        if ("step".equals(name)) {
            setStep(Double.parseDouble(value.toString()));
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}

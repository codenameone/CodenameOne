/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.desktopcompat.java.beans;

/// Describes a bean class as a whole: the class, and the customizer class a
/// GUI builder would configure it with.
public class BeanDescriptor extends FeatureDescriptor {

    private final Class<?> beanClass;
    private final Class<?> customizerClass;

    public BeanDescriptor(Class<?> beanClass) {
        this(beanClass, null);
    }

    public BeanDescriptor(Class<?> beanClass, Class<?> customizerClass) {
        this.beanClass = beanClass;
        this.customizerClass = customizerClass;
        String full = beanClass.getName();
        setName(full.substring(full.lastIndexOf('.') + 1));
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public Class<?> getCustomizerClass() {
        return customizerClass;
    }
}

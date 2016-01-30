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
package com.codename1.cloud;

/**
 * Allows adding a custom property to cloud objects, this effectively
 * allows to simulate missing properties or create properties that
 * don't represent a database entry (e.g. fullName which can be
 * comprised of joining the firstname and surname together).<br>
 * <b>Important:</b> The custom property only takes effect if there is no value
 * assigned to the given property!
 *
 * @author Shai Almog
 * @deprecated the cloud storage API is no longer supported, we recommend switching to a solution such as parse4cn1
 */
public interface CustomProperty {
    /**
     * Returns a property value for the given property name
     * @param obj the cloud object
     * @param propertyName the name of the property
     * @return the property value
     */
    public Object propertyValue(CloudObject obj, String propertyName);
}

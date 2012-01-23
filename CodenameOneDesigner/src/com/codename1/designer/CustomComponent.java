/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.designer;

/**
 * Represents a user custom component that can be integrated into the UI builder
 *
 * @author Shai Almog
 */
public class CustomComponent {
    private String type;
    private String className;
    private String codenameOneBaseClass;
    private Class cls;
    private boolean uiResource;

    public CustomComponent() {
    }

    public CustomComponent(boolean uiResource, String type) {
        this.type = type;
        this.uiResource = uiResource;
        cls = com.codename1.ui.Container.class;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the codenameOne
     */
    public String getCodenameOneBaseClass() {
        return codenameOneBaseClass;
    }

    /**
     * @param codenameOneBaseClass the codenameOneBaseClass to set
     */
    public void setCodenameOneBaseClass(String codenameOneBaseClass) {
        this.codenameOneBaseClass = codenameOneBaseClass;
    }

    /**
     * @return the cls
     */
    public Class getCls() {
        return cls;
    }

    /**
     * @param cls the cls to set
     */
    public void setCls(Class cls) {
        this.cls = cls;
    }

    /**
     * @return the uiResource
     */
    public boolean isUiResource() {
        return uiResource;
    }

    /**
     * @param uiResource the uiResource to set
     */
    public void setUiResource(boolean uiResource) {
        this.uiResource = uiResource;
    }
}

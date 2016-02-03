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
package com.codename1.facebook;

import java.util.Hashtable;

/**
 * This is a base class for all FaceBook Objects
 * 
 * @author Chen Fishbein
 */
public class FBObject {

    private String id;
    
    private String name;

    /**
     * Empty Contructor
     */
    public FBObject() {
    }

    /**
     * This contructor initialize it's attributes from the given Hashtable
     * @param props an Hashtable which contains the Object data
     */
    public FBObject(Hashtable props) {
        init(props);
    }



    /**
     * Simple setter
     * @param id the Object Id, each facebook element had an id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Simple getter
     * 
     * @return the facebook object id
     */    
    public String getId() {
        return id;
    }

    /**
     * Simple getter
     * 
     * @return the FB Object name
     */
    public String getName() {
        return name;
    }

    
    /**
     * copies the relevant values from the given hashtable
     * @param props an hashtable to copy from
     */
    public void copy(Hashtable props){
        init(props);
    }
  
    private void init(Hashtable props){
        id = (String) props.get("id");
        name = (String) props.get("name");
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        return id.equals(((FBObject)obj).id);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return id.hashCode();
    }



}

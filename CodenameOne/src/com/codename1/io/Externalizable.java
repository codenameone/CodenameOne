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

package com.codename1.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Similar to the Java SE externalizable interface this interface allows an object
 * to declare itself as externalizable for serialization. However, due to the lack
 * of reflection and use of obfuscation these objects must be registered with the
 * Util class.
 * Notice that all externalizable objects must have a default public constructor.
 *
 * @author Shai Almog
 */
public interface Externalizable {
    /**
     * Returns the version for the current persistance code, the version will be
     * pased to internalized thus allowing the internalize method to recognize
     * classes persisted in older revisions
     *
     * @return version number for the persistant code
     */
    public int getVersion();

    /**
     * Allows us to store an object state, this method must be implemented
     * in order to save the state of an object
     *
     * @param out the stream into which the object must be serialized
     * @throws java.io.IOException the method may throw an exception
     */
    public void externalize(DataOutputStream out) throws IOException;

    /**
     * Loads the object from the input stream and allows deserialization
     *
     * @param version the version the class returned during the externalization processs
     * @param in the input stream used to load the class
     * @throws java.io.IOException the method may throw an exception
     */
    public void internalize(int version, DataInputStream in) throws IOException;

    /**
     * The object id must be unique, it is used to identify the object when loaded
     * even when it is obfuscated.
     *
     * @return a unique id
     */
    public String getObjectId();
}

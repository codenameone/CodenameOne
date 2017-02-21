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
 * <p>{@code Externalizable} is similar to the Java SE {@code Externalizable} interface this interface. 
 * Notice that due to the lack of reflection and use of obfuscation these objects must be registered with the
 * Util class.<br>
 * Also notice that all externalizable objects must have a default public constructor.</p>
 * 
 * <h3>Built-In Object types</h3>
 * <p>
 * The externalization process supports serializing these types and considers them to be {@code Externalizable}.
 * E.g. you can just write {@code Storage.getInstance().writeObject(new Object[] {"Str1", "Str2"});} and it will work
 * as expected. <br>
 * Notice that while these objects can be written, the process doesn't guarantee they will be read with the same
 * object type. E.g. if you write a {@link java.util.LinkedList} you could get back a {@link java.util.ArrayList} as both
 * implement {@link java.util.Collection}:<br>
 *  {@link java.lang.String},  {@link java.util.Collection},  {@link java.util.Map},  {@link java.util.ArrayList}, 
 *  {@link java.util.HashMap}, {@link java.util.Vector},  {@link java.util.Hashtable},  {@link java.lang.Integer},
 * {@link java.lang.Double}, {@link java.lang.Float}, {@link java.lang.Byte}, {@link java.lang.Short}, 
 * {@link java.lang.Long}, {@link java.lang.Character}, {@link java.lang.Boolean}, {@code Object[]},
 * {@code byte[]}, {@code int[]}, {@code float[]}, {@code long[]}, {@code double[]}.
 * </p>
 * 
 * <p>
 * The sample below demonstrates the usage and registration of the {@code Externalizable} interface:
 * </p>
 * <script src="https://gist.github.com/codenameone/858d8634e3cf1a82a1eb.js"></script>
 *
 * <p><strong>WARNING:</strong> The externalization process caches objects so the app will seem to work and only fail on restart!</p>
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

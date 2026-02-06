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

/// `Externalizable` is similar to the Java SE `Externalizable` interface this interface.
/// Notice that due to the lack of reflection and use of obfuscation these objects must be registered with the
/// Util class.
///
/// Also notice that all externalizable objects must have a default public constructor.
///
/// Built-In Object types
///
/// The externalization process supports serializing these types and considers them to be `Externalizable`.
/// E.g. you can just write `Storage.getInstance().writeObject(new Object[] {"Str1", "Str2"`);} and it will work
/// as expected.
///
/// Notice that while these objects can be written, the process doesn't guarantee they will be read with the same
/// object type. E.g. if you write a `java.util.LinkedList` you could get back a `java.util.ArrayList` as both
/// implement `java.util.Collection`:
///
/// `java.lang.String`,  `java.util.Collection`,  `java.util.Map`,  `java.util.ArrayList`,
/// `java.util.HashMap`, `java.util.Vector`,  `java.util.Hashtable`,  `java.lang.Integer`,
/// `java.lang.Double`, `java.lang.Float`, `java.lang.Byte`, `java.lang.Short`,
/// `java.lang.Long`, `java.lang.Character`, `java.lang.Boolean`, `Object[]`,
/// `byte[]`, `int[]`, `float[]`, `long[]`, `double[]`.
///
/// The sample below demonstrates the usage and registration of the `Externalizable` interface:
///
/// ```java
/// // File: Main.java
/// public Main {
///   public void init(Object o) {
///     theme = UIManager.initFirstTheme("/theme");
///
///     // IMPORTANT: Notice we don't use MyClass.class.getName()! This won't work due to obfuscation!
///     Util.register("MyClass", MyClass.class);
///   }
///
///   public void start() {
///     //...
///   }
///
///   public void stop() {
///     //...
///   }
///
///   public void destroy() {
///     //...
///   }
/// }
/// ```
///
/// ```java
/// // File: MyClass.java
/// public MyClass implements Externalizable {
///   // allows us to manipulate the version, in this case we are demonstrating a data change between the initial release
///   // and the current state of object data
///   private static final int VERSION = 2;
///
///   private String name;
///   private Map data;
///
///   // this field was added after version 1
///   private Date startedAt;
///
///   public int getVersion() {
///     return VERSION;
///   }
///
///   public void externalize(DataOutputStream out) throws IOException {
///     Util.writeUTF(name, out);
///     Util.writeObject(data, out);
///     if(startedAt != null) {
///         out.writeBoolean(true);
///         out.writeLong(startedAt.getTime());
///     } else {
///         out.writeBoolean(false);
///     }
///   }
///   public void internalize(int version, DataInputStream in) throws IOException {
///     name = Util.readUTF(in);
///     data = (Map)Util.readObject(in);
///     if(version > 1) {
///         boolean hasDate = in.readBoolean();
///         if(hasDate) {
///             startedAt = new Date(in.readLong());
///         }
///     }
///   }
///   public String getObjectId() {
///     // IMPORTANT: Notice we don't use getClass().getName()! This won't work due to obfuscation!
///     return "MyClass";
///   }
/// }
/// ```
///
/// ```java
/// // File: ReadAndWrite.java
/// // will read the file or return null if failed
/// MyClass object = (MyClass)Storage.getInstance().readObject("NameOfFile");
///
/// // write the object back to storage
/// Storage.getInstance().writeObject("NameOfFile", object);
/// ```
///
/// **WARNING:** The externalization process caches objects so the app will seem to work and only fail on restart!
///
/// @author Shai Almog
///
/// #### See also
///
/// - [Object Persistence in Codename One](https://sjhannah.com/blog/2013/02/08/object-persistence-in-codename-one/)
public interface Externalizable {
    /// Returns the version for the current persistance code, the version will be
    /// pased to internalized thus allowing the internalize method to recognize
    /// classes persisted in older revisions
    ///
    /// #### Returns
    ///
    /// version number for the persistant code
    int getVersion();

    /// Allows us to store an object state, this method must be implemented
    /// in order to save the state of an object
    ///
    /// #### Parameters
    ///
    /// - `out`: the stream into which the object must be serialized
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: the method may throw an exception
    void externalize(DataOutputStream out) throws IOException;

    /// Loads the object from the input stream and allows deserialization
    ///
    /// #### Parameters
    ///
    /// - `version`: the version the class returned during the externalization processs
    ///
    /// - `in`: the input stream used to load the class
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: the method may throw an exception
    void internalize(int version, DataInputStream in) throws IOException;

    /// The object id must be unique, it is used to identify the object when loaded
    /// even when it is obfuscated.
    ///
    /// #### Returns
    ///
    /// a unique id
    String getObjectId();
}

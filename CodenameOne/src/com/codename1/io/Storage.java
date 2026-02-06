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

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.util.StringUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Abstracts the underlying application specific storage system, unlike the `com.codename1.io.FileSystemStorage`
/// this class is a higher level abstraction. The `Storage` class is designed to be very portable and as
/// such it has no support for staple file system capabilities such as hierarchies.
///
/// Check out a more thorough discussion of this API `here`.
///
/// The sample code below shows a simple storage browser tool in action:
///
/// ```java
/// public void showForm() {
///     Toolbar.setGlobalToolbar(true);
///     Form hi = new Form("Storage", new BoxLayout(BoxLayout.Y_AXIS));
///     hi.getToolbar().addCommandToRightBar("+", null, (e) -> {
///         TextField tf = new TextField("", "File Name", 20, TextField.ANY);
///         TextArea body = new TextArea(5, 20);
///         body.setHint("File Body");
///         Command ok = new Command("OK");
///         Command cancel = new Command("Cancel");
///         Command result = Dialog.show("File Name", BorderLayout.north(tf).add(BorderLayout.CENTER, body), ok, cancel);
///         if(ok == result) {
///             try(OutputStream os = Storage.getInstance().createOutputStream(tf.getText())) {
///                 os.write(body.getText().getBytes("UTF-8"));
///                 createFileEntry(hi, tf.getText());
///                 hi.getContentPane().animateLayout(250);
///             } catch(IOException err) {
///                 Log.e(err);
///             }
///         }
///     });
///
///     for(String file : Storage.getInstance().listEntries()) {
///         createFileEntry(hi, file);
///     }
///     hi.show();
/// }
///
/// private void createFileEntry(Form hi, String file) {
///    Label fileField = new Label(file);
///    Button delete = new Button();
///    Button view = new Button();
///    FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE);
///    FontImage.setMaterialIcon(view, FontImage.MATERIAL_OPEN_IN_NEW);
///    Container content = BorderLayout.center(fileField);
///    int size = Storage.getInstance().entrySize(file);
///    content.add(BorderLayout.EAST, BoxLayout.encloseX(new Label(size + "bytes"), delete, view));
///    delete.addActionListener((e) -> {
///        Storage.getInstance().deleteStorageFile(file);
///        content.setY(hi.getWidth());
///        hi.getContentPane().animateUnlayoutAndWait(150, 255);
///        hi.removeComponent(content);
///        hi.getContentPane().animateLayout(150);
///    });
///    view.addActionListener((e) -> {
///        try(InputStream is = Storage.getInstance().createInputStream(file)) {
///            String s = Util.readToString(is, "UTF-8");
///            Dialog.show(file, s, "OK", null);
///        } catch(IOException err) {
///            Log.e(err);
///        }
///    });
///    hi.add(content);
/// }
/// ```
///
/// @author Shai Almog
public class Storage {
    private static Storage INSTANCE;
    private final CacheMap cache = new CacheMap();
    private boolean normalizeNames = true;

    /// This method must be invoked before using the storage otherwise some platforms
    /// might fail without the application data.
    ///
    /// #### Parameters
    ///
    /// - `data`: @param data either the name of the application e.g. on CDC platforms or
    ///             a context object on other platforms
    private static synchronized void init(Object data) {
        if (Util.getImplementation() != null) {
            Util.getImplementation().setStorageData(data);
        }
        if (INSTANCE == null) {
            INSTANCE = new Storage();
        }
    }

    /// Returns true if the storage is initialized
    ///
    /// #### Returns
    ///
    /// true if the storage is initialized
    public static boolean isInitialized() {
        return INSTANCE != null;
    }

    /// Returns the storage instance or null if the storage wasn't initialized using
    /// a call to init(String) first.
    ///
    /// #### Returns
    ///
    /// storage instance
    public static Storage getInstance() {
        if (INSTANCE == null) {
            init("cn1");
        }
        return INSTANCE;
    }

    /// Allows installing a custom storage instance to provide functionality such as seamless encryption
    ///
    /// #### Parameters
    ///
    /// - `s`: the storage instance
    public static void setStorageInstance(Storage s) {
        INSTANCE = s;
    }

    /// Indicates the caching size, storage can be pretty slow
    ///
    /// #### Parameters
    ///
    /// - `size`: size in elements (not kb!)
    public void setHardCacheSize(int size) {
        cache.setCacheSize(size);
    }

    /// If a file name contains slashes replace them with underscores, same goes for *, %, ? etc.
    ///
    /// #### Parameters
    ///
    /// - `name`: the file name
    ///
    /// #### Returns
    ///
    /// the fixed filename
    private String fixFileName(String name) {
        if (normalizeNames) {
            name = StringUtil.replaceAll(name, "/", "_");
            name = StringUtil.replaceAll(name, "\\", "_");
            name = StringUtil.replaceAll(name, "%", "_");
            name = StringUtil.replaceAll(name, "?", "_");
            name = StringUtil.replaceAll(name, "*", "_");
            name = StringUtil.replaceAll(name, ":", "_");
            name = StringUtil.replaceAll(name, "=", "_");
        }
        return name;
    }

    /// Storage is cached for faster access, however this might cause a problem with refreshing
    /// objects since they are not cloned. Clearing the cache allows to actually reload from the
    /// storage file.
    public void clearCache() {
        cache.clearAllCache();
    }

    /// Flush the storage cache allowing implementations that cache storage objects
    /// to store
    public void flushStorageCache() {
        Util.getImplementation().flushStorageCache();
    }

    /// Deletes the given file name from the storage
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the storage file
    public void deleteStorageFile(String name) {
        name = fixFileName(name);
        Util.getImplementation().deleteStorageFile(name);
        cache.delete(name);
    }

    /// Deletes all the files in the application storage
    public void clearStorage() {
        Util.getImplementation().clearStorage();
        cache.clearAllCache();
    }

    /// Creates an output stream to the storage with the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the storage file name
    ///
    /// #### Returns
    ///
    /// an output stream of limited capacity
    public OutputStream createOutputStream(String name) throws IOException {
        name = fixFileName(name);
        return Util.getImplementation().createStorageOutputStream(name);
    }

    /// Creates an input stream to the given storage source file
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the source file
    ///
    /// #### Returns
    ///
    /// the input stream
    public InputStream createInputStream(String name) throws IOException {
        if (!exists(name)) {
            throw new IOException("Storage key " + name + " does not exist");
        }
        name = fixFileName(name);
        return Util.getImplementation().createStorageInputStream(name);
    }

    /// Returns true if the given storage file exists
    ///
    /// #### Parameters
    ///
    /// - `name`: the storage file name
    ///
    /// #### Returns
    ///
    /// true if it exists
    public boolean exists(String name) {
        name = fixFileName(name);
        CodenameOneImplementation implementation = Util.getImplementation();
        return implementation != null && implementation.storageFileExists(name);
    }

    /// Lists the names of the storage files
    ///
    /// #### Returns
    ///
    /// the names of all the storage files
    public String[] listEntries() {
        return Util.getImplementation().listStorageEntries();
    }

    /// Returns the size in bytes of the given entry
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the entry
    ///
    /// #### Returns
    ///
    /// the size in bytes
    public int entrySize(String name) {
        name = fixFileName(name);
        return Util.getImplementation().getStorageEntrySize(name);
    }

    /// Writes the given object to storage assuming it is an externalizable type
    /// or one of the supported types.
    ///
    /// The sample below demonstrates the usage and registration of the `com.codename1.io.Externalizable` interface:
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
    /// #### Parameters
    ///
    /// - `name`: store name
    ///
    /// - `o`: object to store
    ///
    /// #### Returns
    ///
    /// true for success, false for failure
    public boolean writeObject(String name, Object o) {
        return writeObject(name, o, true);
    }

    /// Writes the given object to storage assuming it is an externalizable type
    /// or one of the supported types.
    ///
    /// The sample below demonstrates the usage and registration of the `com.codename1.io.Externalizable` interface:
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
    /// #### Parameters
    ///
    /// - `name`: store name
    ///
    /// - `o`: object to store
    ///
    /// - `includeLogging`: During app initialization, the logging on error might impact the apps stability
    ///
    /// #### Returns
    ///
    /// true for success, false for failure
    public boolean writeObject(String name, Object o, boolean includeLogging) {
        name = fixFileName(name);
        cache.put(name, o);
        DataOutputStream d = null; //NOPMD CloseResource
        try {
            d = new DataOutputStream(createOutputStream(name));
            Util.writeObject(o, d);
            return true;
        } catch (Exception err) {
            if (includeLogging) {
                Log.e(err);
                if (Log.isCrashBound()) {
                    Log.sendLog();
                }
            }
            Util.getImplementation().deleteStorageFile(name);
            return false;
        } finally {
            Util.getImplementation().cleanup(d);
        }
    }

    /// Reads the object from the storage, returns null if the object isn't there
    ///
    /// The sample below demonstrates the usage and registration of the `com.codename1.io.Externalizable` interface:
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
    /// #### Parameters
    ///
    /// - `name`: name of the store
    ///
    /// #### Returns
    ///
    /// object stored under that name
    public Object readObject(String name) {
        return readObject(name, true);
    }

    /// Reads the object from the storage, returns null if the object isn't there
    ///
    /// The sample below demonstrates the usage and registration of the `com.codename1.io.Externalizable` interface:
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
    /// #### Parameters
    ///
    /// - `name`: name of the store
    ///
    /// - `includeLogging`: During app initialization, the logging on error might impact the apps stability
    ///
    /// #### Returns
    ///
    /// object stored under that name
    public Object readObject(String name, boolean includeLogging) {
        name = fixFileName(name);
        Object o = cache.get(name);
        if (o != null) {
            return o;
        }
        DataInputStream d = null; //NOPMD CloseResource
        try {
            if (!exists(name)) {
                return null;
            }
            d = new DataInputStream(createInputStream(name));
            o = Util.readObject(d);
            cache.put(name, o);
            return o;
        } catch (Throwable err) {
            if (includeLogging) {
                Log.p("Error while reading: " + name);
                Log.e(err);
                if (Log.isCrashBound()) {
                    Log.sendLog();
                }
            }
            return null;
        } finally {
            Util.getImplementation().cleanup(d);
        }
    }

    /// Indicates whether characters that are typically illegal in filesystems should
    /// be sanitized and replaced with underscore
    ///
    /// #### Returns
    ///
    /// the normalizeNames
    public boolean isNormalizeNames() {
        return normalizeNames;
    }

    /// Indicates whether characters that are typically illegal in filesystems should
    /// be sanitized and replaced with underscore
    ///
    /// #### Parameters
    ///
    /// - `normalizeNames`: the normalizeNames to set
    public void setNormalizeNames(boolean normalizeNames) {
        this.normalizeNames = normalizeNames;
    }
}

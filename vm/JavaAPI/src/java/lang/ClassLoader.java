/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package java.lang;

import java.io.InputStream;


public abstract class ClassLoader extends Object {
    private ClassLoader parent;
    private static ClassLoader systemClassLoader = new ClassLoader() {
        
    };
    

    protected ClassLoader() {
        this(null);
    }

    protected ClassLoader(ClassLoader parent) {
        this.parent = parent;
    }

    public ClassLoader getParent() {
        return parent;
    }

    public static ClassLoader getSystemClassLoader() {
        return systemClassLoader;
    }

    public InputStream getResourceAsStream(String name) {
        throw new UnsupportedOperationException();
    }

    public static InputStream getSystemResourceAsStream(String name) {
        throw new UnsupportedOperationException();
    }

    /// Always empty: nothing on a device can be listed by name the way a class path's
    /// entries can. Code that discovers services this way (kotlinx.coroutines'
    /// fast loader) then falls back to [java.util.ServiceLoader], which the translator
    /// fills from the `META-INF/services` files it was given.
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    /// There is no class path to search. Fails rather than answering an empty
    /// enumeration, so that a service lookup scanning `META-INF/services` falls back to
    /// [java.util.ServiceLoader], which the translator fills in at build time.
    public java.util.Enumeration<java.net.URL> getResources(String name) throws java.io.IOException {
        throw new java.io.IOException("no class path on this platform: " + name);
    }

}
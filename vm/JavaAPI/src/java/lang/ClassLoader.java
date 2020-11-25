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

}
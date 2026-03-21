/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/
package bsh.classpath;

import java.util.HashMap;

import bsh.BshClassManager;
import bsh.classpath.BshClassPath.ClassSource;

/**
    A classloader which can load one or more classes from specified sources.
    Because the classes are loaded via a single classloader they change as a
    group and any versioning cross dependencies can be managed.
*/
public class DiscreteFilesClassLoader extends BshClassLoader
{
    /**
        Map of class sources which also implies our coverage space.
    */
    ClassSourceMap map;

    public static class ClassSourceMap extends HashMap<String, ClassSource>
    {
        private static final long serialVersionUID = 1L;
        @Override
        public ClassSource put( String name, ClassSource source ) {
            return super.put( name, source );
        }
        @Override
        public ClassSource get( Object name ) {
            return super.get( name );
        }
    }

    private static DiscreteFilesClassLoader instance;
    public static DiscreteFilesClassLoader instance() {
        return instance;
    }
    public static void newInstance(
            BshClassManager classManager, ClassSourceMap map ) {
        instance = new DiscreteFilesClassLoader(classManager, map);
    }

    public DiscreteFilesClassLoader(
        BshClassManager classManager, ClassSourceMap map )
    {
        super( classManager );
        this.map = map;
    }

    /**
    */
    public Class findClass( String name ) throws ClassNotFoundException
    {
        // Load it if it's one of our classes
        ClassSource source = map.get( name );

        if ( source != null )
        {
            byte [] code = source.getCode( name );
            return defineClass( name, code, 0,
                    null == code ? 0 : code.length );
        } else
            // Let superclass BshClassLoader (URLClassLoader) findClass try
            // to find the class...
            return super.findClass( name );
    }

    public String toString() {
        return super.toString() + "for files: "+map;
    }

}

package com.codename1.mojo.exec;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collection;

abstract class AbstractPath
{

    /**
     * @parameter dependency
     */
    private Collection<String> dependencies;

    public void setDependencies( Collection<String> deps )
    {
        this.dependencies = deps;
    }

    public void setDependency( String dependency )
    {
        // Is the the correct thing to do? See MOJO-348
        if ( dependencies == null )
        {
            setDependencies( new java.util.ArrayList<String>() );
        }
        dependencies.add( dependency );
    }

    public Collection<String> getDependencies()
    {
        return dependencies;
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder( "Classpath {" );

        if ( dependencies != null )
        {
            for ( String dep : dependencies )
            {
                buffer.append( " " ).append( dep );
            }
        }

        buffer.append( "}" );

        return buffer.toString();
    }

}

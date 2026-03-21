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
package bsh;

/**
    This interface supports name completion, which is used primarily for
    command line tools, etc.  It provides a flat source of "names" in a
    space.  For example all of the classes in the classpath or all of the
    variables in a namespace (or all of those).
    <p>
    NameSource is the lightest weight mechanism for sources which wish to
    support name completion.  In the future it might be better for NameSpace
    to implement NameCompletion directly in a more native and efficient
    fasion.  However in general name competion is used for human interaction
    and therefore does not require high performance.
    <p>
    @see bsh.util.NameCompletion
    @see bsh.util.NameCompletionTable
*/
public interface NameSource
{
    String [] getAllNames();
    void addNameSourceListener( NameSource.Listener listener );

    public static interface Listener {
        void nameSourceChanged( NameSource src );
        /**
            Provide feedback on the progress of mapping a namespace
            @param msg is an update about what's happening
            @perc is an integer in the range 0-100 indicating percentage done
        public void nameSourceMapping(
            NameSource src, String msg, int perc );
        */
    }
}

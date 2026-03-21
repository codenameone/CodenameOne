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


package bsh.util;

import bsh.ConsoleInterface;
import java.awt.Color;

/**
    Additional capabilities of an interactive console for BeanShell.
    Although this is called "GUIConsoleInterface" it might just as well be
    used by a more sophisticated text-only command line.
    <p>
    Note: we may want to express the command line history, editing,
    and cut & paste functionality here as well at some point.
*/
public interface GUIConsoleInterface extends ConsoleInterface
{
    void print( Object o, Color color );
    void setNameCompletion( NameCompletion nc );

    /** e.g. the wait cursor */
    void setWaitFeedback( boolean on );
}


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
    Represents a Return, Break, or Continue statement
*/
class ReturnControl implements ParserConstants {
    public int kind;
    public String label = null;
    public Object value = Primitive.VOID;
    /** The node where we returned... for printing error messages correctly */
    public Node returnPoint;

    public ReturnControl( int kind, Object value, Node returnPoint ) {
        this.kind = kind;
        this.value = value;
        this.returnPoint = returnPoint;
    }

    public ReturnControl( int kind, String label, Node returnPoint ) {
        this.kind = kind;
        this.label = label;
        this.returnPoint = returnPoint;
    }

    public String toString() {
        return "ReturnControl: " + (kind == BREAK ? "BREAK " : kind == RETURN ? "RETURN " : kind == CONTINUE ? "CONTINUE " : "DUNNO?? "+kind)
                + label + ": from: " + returnPoint;
    }
}


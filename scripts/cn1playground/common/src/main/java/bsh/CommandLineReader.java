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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
    This is a quick hack to turn empty lines entered interactively on the
    command line into ';\n' empty lines for the interpreter.  It's just more
    pleasant to be able to hit return on an empty line and see the prompt
    reappear.

    This is *not* used when text is sourced from a file non-interactively.
*/
public class CommandLineReader extends FilterReader {

    public CommandLineReader( Reader in ) {
        super(in);
    }

    static final int
        normal = 0,
        lastCharNL = 1,
        sentSemi = 2;

    int state = lastCharNL;

    public int read() throws IOException {
        int b;

        if ( state == sentSemi ) {
            state = lastCharNL;
            return '\n';
        }

        // skip CR
        while ( (b = in.read()) == '\r' );

        if ( b == '\n' )
            if ( state == lastCharNL ) {
                b = ';';
                state = sentSemi;
            } else
                state = lastCharNL;
        else
            state = normal;

        return b;
    }

    /**
        This is a degenerate implementation.
        I don't know how to keep this from blocking if we try to read more
        than one char...  There is no available() for Readers ??
    */
    public int read(char buff[], int off, int len) throws IOException
    {
        int b = read();
        if ( b == -1 )
            return -1;  // EOF, not zero read apparently
        else {
            buff[off]=(char)b;
            return 1;
        }
    }

    // Test it
    public static void main( String [] args ) throws Exception {
        @SuppressWarnings("resource")
        Reader in = new CommandLineReader( new FileReader(System.in) );
        while ( true )
            System.out.println( in.read() );
    }
}


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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;

import bsh.FileReader;
import bsh.Interpreter;
import bsh.NameSpace;

/**
    BeanShell remote session server.
    Starts instances of bsh for client connections.
    Note: the sessiond effectively maps all connections to the same interpreter
    (shared namespace).
*/
public class Sessiond extends Thread
{
    private ServerSocket ss;
    NameSpace globalNameSpace;

    /*
    public static void main(String argv[]) throws IOException
    {
        new Sessiond( Integer.parseInt(argv[0])).start();
    }
    */

    public Sessiond(NameSpace globalNameSpace, int port) throws IOException
    {
        ss = new ServerSocket(port);
        this.globalNameSpace = globalNameSpace;
    }

    public void run()
    {
        try
        {
            while(true)
                new SessiondConnection(globalNameSpace, ss.accept()).start();
        }
        catch(IOException e) { System.out.println(e); }
    }
}

class SessiondConnection extends Thread
{
    NameSpace globalNameSpace;
    Socket client;
    Interpreter i;

    SessiondConnection(NameSpace globalNameSpace, Socket client)
    {
        this.client = client;
        this.globalNameSpace = globalNameSpace;
    }

    public void run()
    {
        try (Reader in = new FileReader(client.getInputStream())) {
            PrintStream out = new PrintStream(
                    client.getOutputStream(), true, "UTF-8");
            i = new Interpreter(
                in, out, out, true, globalNameSpace);
            i.setExitOnEOF( false ); // don't exit interpreter
            i.run();
        }
        catch(IOException e) { System.out.println(e); }
    }
}


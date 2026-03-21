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
/**
    Display the contents of the current working directory.
    The format is similar to the Unix ls -l
    <em>This is an example of a bsh command written in Java for speed.</em>

    @method void dir( [ String dirname ] )
*/
package bsh.commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import bsh.CallStack;
import bsh.Interpreter;

public class dir
{
    static final String [] months = { "Jan", "Feb", "Mar", "Apr",
        "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    public static String usage() {
        return "usage: dir( String dir )\n       dir()";
    }

    /**
        Implement dir() command.
    */
    public static void invoke( Interpreter env, CallStack callstack )
    {
        String dir = ".";
        invoke( env, callstack, dir );
    }

    /**
        Implement dir( String directory ) command.
    */
    public static void invoke(
        Interpreter env, CallStack callstack, String dir )
    {
        File file;
        String path;
        try {
            path = env.pathToFile( dir ).getAbsolutePath();
            file =  env.pathToFile( dir );
        } catch (IOException e ) {
            env.println("error reading path: "+e);
            return;
        }

        if ( !file.exists() || !file.canRead() ) {
            env.println( "Can't read " + file );
            return;
        }
        if ( !file.isDirectory() )  {
            env.println("'"+dir+"' is not a directory");
        }

        String [] files = file.list();
        if ( null == files )
            files = new String[0];
        Arrays.sort(files);

        for( int i=0; i< files.length; i++ ) {
            File f = new File( path + File.separator + files[i] );
            StringBuilder sb = new StringBuilder();
            sb.append( f.canRead() ? "r": "-" );
            sb.append( f.canWrite() ? "w": "-" );
            sb.append( "_" );
            sb.append( " ");

            Date d = new Date(f.lastModified());
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(d);
            int day = c.get(Calendar.DAY_OF_MONTH);
            sb.append( months[ c.get(Calendar.MONTH) ] + " " + day );
            if ( day < 10 )
                sb.append(" ");

            sb.append(" ");

            // hack to get fixed length 'length' field
            int fieldlen = 8;
            StringBuilder len = new StringBuilder();
            for(int j=0; j<fieldlen; j++)
                len.append(" ");
            len.insert(0, f.length());
            len.setLength(fieldlen);
            // hack to move the spaces to the front
            int si = len.toString().indexOf(" ");
            if ( si != -1 ) {
                String pad = len.toString().substring(si);
                len.setLength(si);
                len.insert(0, pad);
            }

            sb.append( len.toString() );

            sb.append( " " + f.getName() );
            if ( f.isDirectory() )
                sb.append("/");

            env.println( sb.toString() );
        }
    }
}


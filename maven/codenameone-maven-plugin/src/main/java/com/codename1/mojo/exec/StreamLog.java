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

import java.io.PrintStream;

import org.apache.maven.plugin.logging.Log;

/**
 * a Simple Maven Log that outputs to a Stream
 */
class StreamLog
    implements Log
{
    static final int DEBUG = 0;

    static final int INFO = 1;

    static final int WARN = 2;

    static final int ERROR = 3;

    private int level = INFO;

    public void setLevel( int level )
    {
        if ( level < DEBUG || level > ERROR )
        {
            throw new IllegalStateException( "invalid level: " + level );
        }
        this.level = level;
    }

    private final PrintStream s;

    StreamLog( PrintStream s )
    {
        this.s = s;
    }

    public void debug( CharSequence content )
    {
        if ( isDebugEnabled() )
        {
            s.println( content );
        }
    }

    public void debug( CharSequence content, Throwable error )
    {
        if ( isDebugEnabled() )
        {
            s.println( content );
        }
    }

    public void debug( Throwable error )
    {
        if ( isDebugEnabled() )
        {
            error.printStackTrace( s );
        }
    }

    public void error( CharSequence content )
    {
        if ( isErrorEnabled() )
        {
            s.println( content );
        }
    }

    public void error( CharSequence content, Throwable error )
    {
        error( content );
        error( error );
    }

    public void error( Throwable error )
    {
        if ( isErrorEnabled() )
        {
            error.printStackTrace( s );
        }
    }

    public void info( CharSequence content )
    {
        if ( isInfoEnabled() )
        {
            s.println( content );
        }
    }

    public void info( CharSequence content, Throwable error )
    {
        info( content );
        info( error );
    }

    public void info( Throwable error )
    {
        if ( isInfoEnabled() )
        {
            error.printStackTrace( s );
        }
    }

    public boolean isDebugEnabled()
    {
        return level >= DEBUG;
    }

    public boolean isErrorEnabled()
    {
        return level >= ERROR;
    }

    public boolean isInfoEnabled()
    {
        return level >= INFO;
    }

    public boolean isWarnEnabled()
    {
        return level >= WARN;
    }

    public void warn( CharSequence content )
    {
        if ( isWarnEnabled() )
        {
            s.println( content );
        }
    }

    public void warn( CharSequence content, Throwable error )
    {
        warn( content );
        warn( error );
    }

    public void warn( Throwable error )
    {
        if ( isWarnEnabled() )
        {
            error.printStackTrace( s );
        }
    }
}

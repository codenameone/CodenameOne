/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.io.gzip;

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple version of filter input stream
 *
 * @author Shai Almog
 */
public class FilterInputStream extends InputStream
{
    protected InputStream in;

    protected FilterInputStream(InputStream underlying)
    {
        in = underlying;
    }

    public int read() throws IOException
    {
        return in.read();
    }

    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int offset, int length) throws IOException
    {
        return in.read(b, offset, length);
    }

    public long skip(long n) throws IOException
    {
        return in.skip(n);
    }

    public int available() throws IOException
    {
        return in.available();
    }

    public void close() throws IOException
    {
        in.close();
    }

    public void mark(int readlimit)
    {
        in.mark(readlimit);
    }

    public void reset() throws IOException
    {
        in.reset();
    }

    public boolean markSupported()
    {
        return in.markSupported();
    }
}

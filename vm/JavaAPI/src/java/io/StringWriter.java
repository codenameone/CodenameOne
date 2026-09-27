/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package java.io;

public class StringWriter extends Writer implements Appendable {
    
    private StringBuffer buf;
    
    public StringWriter() {
        buf = new StringBuffer();
    }
    
    public StringWriter(int initialSize) {
        buf = new StringBuffer(initialSize);
    }

    @Override
    public void close() throws IOException {
        
    }

    @Override
    public void flush() throws IOException {
        
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        buf.append(cbuf, off, len);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        buf.append(str, off, off + len);
    }

    @Override
    public void write(int c) throws IOException {
        buf.append((char)c);
    }

    @Override
    public void write(String str) throws IOException {
        buf.append(str);
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        buf.append(cbuf);
    }

    @Override
    public String toString() {
        return buf.toString();
    }
    
    public StringBuffer getBuffer() {
        return buf;
    }
    
    
    public StringWriter append(char c) {
        buf.append(c);
        return this;
    }
    
    public StringWriter append(CharSequence csq) {
        buf.append(csq);
        return this;
    }
    
    public StringWriter append(CharSequence csq, int start, int end) {
        buf.append(csq, start, end);
        return this;
    }
    
    
    
    
    
    
    
}

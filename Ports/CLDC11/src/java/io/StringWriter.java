/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package java.io;

/**
 *
 * @author shannah
 */
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codename1.util.regex;

import java.io.Reader;

/**
 *
 * @author Nikolay Neizvesny
 */
public class StringReader extends Reader {
    
    private static final char NEW_LINE = '\n';

    private String str;
    private int pointer = 0;

    public StringReader(String str) {
        this.str = str;
    }
    
    public int read() {
        if (pointer >= str.length()) {
            return -1;
        }
        return str.charAt(pointer++);
    }

    public int read(char[] cbuf, int off, int len) {
        if (pointer >= str.length()) {
            return -1;
        }
        int read = 0;
        for (int i = 0; i < len && pointer < str.length(); i++, read++) {
            cbuf[off + i] = str.charAt(pointer++);
        }
        return read;
    }

    public void close() {
    }

    public String readLine() {
        if (pointer >= str.length()) {
            return null;
        }
        int nextLine = str.indexOf(NEW_LINE, pointer);
        if (nextLine == -1) {
            nextLine = str.length();
        }
        String result = str.substring(pointer, nextLine);
        pointer = nextLine + 1;
        return result;
    }

    public boolean ready() {
        return pointer < str.length();
    }
}

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

/**
 * Encapsulates different types of character sources - String, InputStream, ...
 * Defines a set of common methods
 *
 * @author <a href="mailto:ales.novak@netbeans.com">Ales Novak</a>
 * @version CVS $Id: CharacterIterator.java 518156 2007-03-14 14:31:26Z vgritsenko $
 */
public interface CharacterIterator {
    /**
     * @return a substring
     */
    String substring(int beginIndex, int endIndex);

    /**
     * @return a substring
     */
    String substring(int beginIndex);

    /**
     * @return a character at the specified position.
     */
    char charAt(int pos);

    /**
     * @return <tt>true</tt> iff if the specified index is after the end of the character stream
     */
    boolean isEnd(int pos);
}

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
 * This is a class that contains utility helper methods for this package.
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 * @version $Id: REUtil.java 518156 2007-03-14 14:31:26Z vgritsenko $
 */
public class REUtil {
    /**
     * complex:
     */
    private static final String complexPrefix = "complex:";

    /**
     * Creates a regular expression, permitting simple or complex syntax
     *
     * @param expression The expression, beginning with a prefix if it's complex or
     *                   having no prefix if it's simple
     * @param matchFlags Matching style flags
     * @return The regular expression object
     * @throws RESyntaxException thrown in case of error
     */
    public static RE createRE(String expression, int matchFlags) throws RESyntaxException {
        if (expression.startsWith(complexPrefix)) {
            return new RE(expression.substring(complexPrefix.length()), matchFlags);
        }
        return new RE(RE.simplePatternToFullRegularExpression(expression), matchFlags);
    }

    /**
     * Creates a regular expression, permitting simple or complex syntax
     *
     * @param expression The expression, beginning with a prefix if it's complex or
     *                   having no prefix if it's simple
     * @return The regular expression object
     * @throws RESyntaxException thrown in case of error
     */
    public static RE createRE(String expression) throws RESyntaxException {
        return createRE(expression, RE.MATCH_NORMAL);
    }
}

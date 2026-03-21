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

public final class BSHLiteral extends SimpleNode
{
    public static volatile boolean internStrings = true;

    public Object value;

    BSHLiteral(int id) { super(id); }

    public Object eval( CallStack callstack, Interpreter interpreter )
        throws EvalError
    {
        return value;
    }

    private char getEscapeChar(char ch)
    {
        switch(ch)
        {
            case 'b':
                ch = '\b';
                break;

            case 't':
                ch = '\t';
                break;

            case 'n':
                ch = '\n';
                break;

            case 'f':
                ch = '\f';
                break;

            case 'r':
                ch = '\r';
                break;

            // do nothing - ch already contains correct character
            case '"':
            case '\'':
            case '\\':
                break;
        }

        return ch;
    }

    public void charSetup(String str)
    {
        int len = str.toCharArray().length;

        if ( len == 0 || len > 4 || len > 1 && str.charAt(0) != '\\' ) {
            stringSetup(str);
            return;
        }
        try {
            char ch = str.charAt(0);
            if(ch == '\\')
            {
                // get next character
                ch = str.charAt(1);

                if(Character.isDigit(ch)) {
                    if (255 < (ch = (char)Integer.parseInt(str.substring(1), 8))) {
                        stringSetup(str);
                        return;
                    }
                }
                else
                    ch = getEscapeChar(ch);
            }

            value = new Primitive(Character.valueOf(ch).charValue());
        } catch (Exception e) {
            stringSetup(str);
        }
    }

    void stringSetup(String str)
    {
        StringBuilder buffer = new StringBuilder();
        int len = str.length();
        for(int i = 0; i < len; i++)
        {
            char ch = str.charAt(i);
            if(ch == '\\')
            {
                // get next character
                ch = str.charAt(++i);

                if(Character.isDigit(ch) && Integer.parseInt(String.valueOf(ch)) < 8)
                {
                    int endPos = i;

                    // check the next two characters
                    int max = Math.min( i + 2, len - 1 );
                    while(endPos < max)
                    {
                        final char t = str.charAt(endPos + 1);
                        if(Character.isDigit(t) && Integer.parseInt(String.valueOf(t)) < 8)
                            endPos++;
                        else
                            break;
                    }
                    String num = str.substring(i, endPos + 1);
                    if (num.length() == 3 && Integer.parseInt(String.valueOf(ch)) > 3)
                        ch = (char)Integer.parseInt(str.substring(i, endPos--), 8);
                    else
                        ch = (char)Integer.parseInt(num, 8);
                    i = endPos;
                }
                else
                    ch = getEscapeChar(ch);
            }

            buffer.append(ch);
        }

        String s = buffer.toString();
        if( internStrings )
            s = s.intern();
        value = s;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + value;
    }
}

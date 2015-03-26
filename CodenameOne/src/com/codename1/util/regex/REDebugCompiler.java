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

import java.io.PrintStream;
import java.util.HashMap;

/**
 * A subclass of RECompiler which can dump a regular expression program
 * for debugging purposes.
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 */
public class REDebugCompiler extends RECompiler
{
    /**
     * Mapping from opcodes to descriptive strings
     */
    static HashMap hashOpcode = new HashMap();
    static
    {
        hashOpcode.put(new Integer(RE.OP_RELUCTANTSTAR),    "OP_RELUCTANTSTAR");
        hashOpcode.put(new Integer(RE.OP_RELUCTANTPLUS),    "OP_RELUCTANTPLUS");
        hashOpcode.put(new Integer(RE.OP_RELUCTANTMAYBE),   "OP_RELUCTANTMAYBE");
        hashOpcode.put(new Integer(RE.OP_END),              "OP_END");
        hashOpcode.put(new Integer(RE.OP_BOL),              "OP_BOL");
        hashOpcode.put(new Integer(RE.OP_EOL),              "OP_EOL");
        hashOpcode.put(new Integer(RE.OP_ANY),              "OP_ANY");
        hashOpcode.put(new Integer(RE.OP_ANYOF),            "OP_ANYOF");
        hashOpcode.put(new Integer(RE.OP_BRANCH),           "OP_BRANCH");
        hashOpcode.put(new Integer(RE.OP_ATOM),             "OP_ATOM");
        hashOpcode.put(new Integer(RE.OP_STAR),             "OP_STAR");
        hashOpcode.put(new Integer(RE.OP_PLUS),             "OP_PLUS");
        hashOpcode.put(new Integer(RE.OP_MAYBE),            "OP_MAYBE");
        hashOpcode.put(new Integer(RE.OP_NOTHING),          "OP_NOTHING");
        hashOpcode.put(new Integer(RE.OP_GOTO),             "OP_GOTO");
        hashOpcode.put(new Integer(RE.OP_CONTINUE),         "OP_CONTINUE");
        hashOpcode.put(new Integer(RE.OP_ESCAPE),           "OP_ESCAPE");
        hashOpcode.put(new Integer(RE.OP_OPEN),             "OP_OPEN");
        hashOpcode.put(new Integer(RE.OP_CLOSE),            "OP_CLOSE");
        hashOpcode.put(new Integer(RE.OP_BACKREF),          "OP_BACKREF");
        hashOpcode.put(new Integer(RE.OP_POSIXCLASS),       "OP_POSIXCLASS");
        hashOpcode.put(new Integer(RE.OP_OPEN_CLUSTER),     "OP_OPEN_CLUSTER");
        hashOpcode.put(new Integer(RE.OP_CLOSE_CLUSTER),    "OP_CLOSE_CLUSTER");
    }

    /**
     * Returns a descriptive string for an opcode.
     *
     * @param opcode Opcode to convert to a string
     * @return Description of opcode
     */
    String opcodeToString(char opcode)
    {
        // Get string for opcode
        String ret = (String) hashOpcode.get(new Integer(opcode));

        // Just in case we have a corrupt program
        if (ret == null)
        {
            ret = "OP_????";
        }
        return ret;
    }

    /**
     * Return a string describing a (possibly unprintable) character.
     *
     * @param c Character to convert to a printable representation
     * @return String representation of character
     */
    String charToString(char c)
    {
        // If it's unprintable, convert to '\###'
        if (c < ' ' || c > 127)
        {
            return "\\" + (int) c;
        }

        // Return the character as a string
        return String.valueOf(c);
    }

    /**
     * Returns a descriptive string for a node in a regular expression program.
     * @param node Node to describe
     * @return Description of node
     */
    String nodeToString(int node)
    {
        // Get opcode and opdata for node
        char opcode =       instruction[node /* + RE.offsetOpcode */];
        int opdata  = (int) instruction[node    + RE.offsetOpdata   ];

        // Return opcode as a string and opdata value
        return opcodeToString(opcode) + ", opdata = " + opdata;
    }


    /**
     * Dumps the current program to a {@link PrintStream}.
     *
     * @param p PrintStream for program dump output
     */
    public void dumpProgram(PrintStream p)
    {
        // Loop through the whole program
        for (int i = 0; i < lenInstruction; )
        {
            // Get opcode, opdata and next fields of current program node
            char opcode =         instruction[i /* + RE.offsetOpcode */];
            char opdata =         instruction[i    + RE.offsetOpdata   ];
            int  next   = (short) instruction[i    + RE.offsetNext     ];

            // Display the current program node
            p.print(i + ". " + nodeToString(i) + ", next = ");

            // If there's no next, say 'none', otherwise give absolute index of next node
            if (next == 0)
            {
                p.print("none");
            }
            else
            {
                p.print(i + next);
            }

            // Move past node
            i += RE.nodeSize;

            // If character class
            if (opcode == RE.OP_ANYOF)
            {
                // Opening bracket for start of char class
                p.print(", [");

                // Show each range in the char class
                // int rangeCount = opdata;
                for (int r = 0; r < opdata; r++)
                {
                    // Get first and last chars in range
                    char charFirst = instruction[i++];
                    char charLast  = instruction[i++];

                    // Print range as X-Y, unless range encompasses only one char
                    if (charFirst == charLast)
                    {
                        p.print(charToString(charFirst));
                    }
                    else
                    {
                        p.print(charToString(charFirst) + "-" + charToString(charLast));
                    }
                }

                // Annotate the end of the char class
                p.print("]");
            }

            // If atom
            if (opcode == RE.OP_ATOM)
            {
                // Open quote
                p.print(", \"");

                // Print each character in the atom
                for (int len = opdata; len-- != 0; )
                {
                    p.print(charToString(instruction[i++]));
                }

                // Close quote
                p.print("\"");
            }

            // Print a newline
            p.println("");
        }
    }

    /**
     * Dumps the current program to a <code>System.out</code>.
     */
    public void dumpProgram()
    {
        PrintStream w = new PrintStream(System.out);
        dumpProgram(w);
        w.flush();
    }
}

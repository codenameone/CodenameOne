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

import java.util.HashMap;

/**
 * A subclass of RECompiler which can dump a regular expression program
 * for debugging purposes.
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 */
@SuppressWarnings({"PMD.AssignmentInOperand", "PMD.CloseResource"})
public class REDebugCompiler extends RECompiler {
    /**
     * Mapping from opcodes to descriptive strings
     */
    static HashMap hashOpcode = new HashMap();

    static {
        hashOpcode.put(Integer.valueOf(RE.OP_RELUCTANTSTAR), "OP_RELUCTANTSTAR");
        hashOpcode.put(Integer.valueOf(RE.OP_RELUCTANTPLUS), "OP_RELUCTANTPLUS");
        hashOpcode.put(Integer.valueOf(RE.OP_RELUCTANTMAYBE), "OP_RELUCTANTMAYBE");
        hashOpcode.put(Integer.valueOf(RE.OP_END), "OP_END");
        hashOpcode.put(Integer.valueOf(RE.OP_BOL), "OP_BOL");
        hashOpcode.put(Integer.valueOf(RE.OP_EOL), "OP_EOL");
        hashOpcode.put(Integer.valueOf(RE.OP_ANY), "OP_ANY");
        hashOpcode.put(Integer.valueOf(RE.OP_ANYOF), "OP_ANYOF");
        hashOpcode.put(Integer.valueOf(RE.OP_BRANCH), "OP_BRANCH");
        hashOpcode.put(Integer.valueOf(RE.OP_ATOM), "OP_ATOM");
        hashOpcode.put(Integer.valueOf(RE.OP_STAR), "OP_STAR");
        hashOpcode.put(Integer.valueOf(RE.OP_PLUS), "OP_PLUS");
        hashOpcode.put(Integer.valueOf(RE.OP_MAYBE), "OP_MAYBE");
        hashOpcode.put(Integer.valueOf(RE.OP_NOTHING), "OP_NOTHING");
        hashOpcode.put(Integer.valueOf(RE.OP_GOTO), "OP_GOTO");
        hashOpcode.put(Integer.valueOf(RE.OP_CONTINUE), "OP_CONTINUE");
        hashOpcode.put(Integer.valueOf(RE.OP_ESCAPE), "OP_ESCAPE");
        hashOpcode.put(Integer.valueOf(RE.OP_OPEN), "OP_OPEN");
        hashOpcode.put(Integer.valueOf(RE.OP_CLOSE), "OP_CLOSE");
        hashOpcode.put(Integer.valueOf(RE.OP_BACKREF), "OP_BACKREF");
        hashOpcode.put(Integer.valueOf(RE.OP_POSIXCLASS), "OP_POSIXCLASS");
        hashOpcode.put(Integer.valueOf(RE.OP_OPEN_CLUSTER), "OP_OPEN_CLUSTER");
        hashOpcode.put(Integer.valueOf(RE.OP_CLOSE_CLUSTER), "OP_CLOSE_CLUSTER");
    }

    /**
     * Returns a descriptive string for an opcode.
     *
     * @param opcode Opcode to convert to a string
     * @return Description of opcode
     */
    String opcodeToString(char opcode) {
        // Get string for opcode
        String ret = (String) hashOpcode.get(Integer.valueOf(opcode));

        // Just in case we have a corrupt program
        if (ret == null) {
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
    String charToString(char c) {
        // If it's unprintable, convert to '\###'
        if (c < ' ' || c > 127) {
            return "\\" + (int) c;
        }

        // Return the character as a string
        return String.valueOf(c);
    }

    /**
     * Returns a descriptive string for a node in a regular expression program.
     *
     * @param node Node to describe
     * @return Description of node
     */
    String nodeToString(int node) {
        // Get opcode and opdata for node
        char opcode = instruction[node /* + RE.offsetOpcode */];
        int opdata = instruction[node + RE.offsetOpdata];

        // Return opcode as a string and opdata value
        return opcodeToString(opcode) + ", opdata = " + opdata;
    }


    /**
     * Dumps the current program to a {@link java.io.Writer}.
     *
     * @param p Writer for program dump output
     */
    public void dumpProgram(java.io.Writer p) {
        try {
            // Loop through the whole program
            for (int i = 0; i < lenInstruction; ) {
                // Get opcode, opdata and next fields of current program node
                char opcode = instruction[i /* + RE.offsetOpcode */];
                char opdata = instruction[i + RE.offsetOpdata];
                int next = (short) instruction[i + RE.offsetNext];

                // Display the current program node
                p.write(i + ". " + nodeToString(i) + ", next = ");

                // If there's no next, say 'none', otherwise give absolute index of next node
                if (next == 0) {
                    p.write("none");
                } else {
                    p.write(String.valueOf(i + next));
                }

                // Move past node
                i += RE.nodeSize;

                // If character class
                if (opcode == RE.OP_ANYOF) {
                    // Opening bracket for start of char class
                    p.write(", [");

                    // Show each range in the char class
                    // int rangeCount = opdata;
                    for (int r = 0; r < opdata; r++) {
                        // Get first and last chars in range
                        char charFirst = instruction[i++];
                        char charLast = instruction[i++];

                        // Print range as X-Y, unless range encompasses only one char
                        if (charFirst == charLast) {
                            p.write(charToString(charFirst));
                        } else {
                            p.write(charToString(charFirst) + "-" + charToString(charLast));
                        }
                    }

                    // Annotate the end of the char class
                    p.write("]");
                }

                // If atom
                if (opcode == RE.OP_ATOM) {
                    // Open quote
                    p.write(", \"");

                    // Print each character in the atom
                    for (int len = opdata; len-- != 0; ) {
                        p.write(charToString(instruction[i++]));
                    }

                    // Close quote
                    p.write("\"");
                }

                // Print a newline
                p.write('\n');
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Dumps the current program to a <code>System.out</code>.
     */
    public void dumpProgram() {
        java.io.OutputStreamWriter w = com.codename1.io.Util.getWriter(System.out);
        dumpProgram(w);
        try {
            w.flush();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}

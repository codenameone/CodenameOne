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
 * A class that holds compiled regular expressions.  You should not need to
 * work directly with this class.
 *
 * @see RE
 * @see RECompiler
 *
 * @author <a href="mailto:jonl@muppetlabs.com">Jonathan Locke</a>
 * @version $Id: REProgram.java 518156 2007-03-14 14:31:26Z vgritsenko $
 */
public class REProgram
{
    static final int OPT_HASBACKREFS = 1;
    static final int OPT_HASBOL      = 2;

    char[] instruction;         // The compiled regular expression 'program'
    int lenInstruction;         // The amount of the instruction buffer in use
    char[] prefix;              // Prefix string optimization
    int flags;                  // Optimization flags (REProgram.OPT_*)
    int maxParens = -1;

    /**
     * Constructs a program object from a character array
     * @param instruction Character array with RE opcode instructions in it
     */
    public REProgram(char[] instruction)
    {
        this(instruction, instruction.length);
    }

    /**
     * Constructs a program object from a character array
     * @param parens Count of parens in the program
     * @param instruction Character array with RE opcode instructions in it
     */
    public REProgram(int parens, char[] instruction)
    {
        this(instruction, instruction.length);
        this.maxParens = parens;
    }

    /**
     * Constructs a program object from a character array
     * @param instruction Character array with RE opcode instructions in it
     * @param lenInstruction Amount of instruction array in use
     */
    public REProgram(char[] instruction, int lenInstruction)
    {
        setInstructions(instruction, lenInstruction);
    }

    /**
     * Returns a copy of the current regular expression program in a character
     * array that is exactly the right length to hold the program.  If there is
     * no program compiled yet, getInstructions() will return null.
     * @return A copy of the current compiled RE program
     */
    public char[] getInstructions()
    {
        // Ensure program has been compiled!
        if (lenInstruction != 0)
        {
            // Return copy of program
            char[] ret = new char[lenInstruction];
            System.arraycopy(instruction, 0, ret, 0, lenInstruction);
            return ret;
        }
        return null;
    }

    /**
     * Sets a new regular expression program to run.  It is this method which
     * performs any special compile-time search optimizations.  Currently only
     * two optimizations are in place - one which checks for backreferences
     * (so that they can be lazily allocated) and another which attempts to
     * find a prefix anchor string so that substantial amounts of input can
     * potentially be skipped without running the actual program.
     * @param instruction Program instruction buffer
     * @param lenInstruction Length of instruction buffer in use
     */
    public void setInstructions(char[] instruction, int lenInstruction)
    {
        // Save reference to instruction array
        this.instruction = instruction;
        this.lenInstruction = lenInstruction;

        // Initialize other program-related variables
        this.flags = 0;
        this.prefix = null;

        // Try various compile-time optimizations if there's a program
        if (instruction != null && lenInstruction != 0)
        {
            // If the first node is a branch
            if (lenInstruction >= RE.nodeSize && instruction[0 + RE.offsetOpcode] == RE.OP_BRANCH)
            {
                // to the end node
                int next = (short) instruction[0 + RE.offsetNext];
                if (instruction[next + RE.offsetOpcode] == RE.OP_END && lenInstruction >= (RE.nodeSize * 2))
                {
                    final char nextOp = instruction[RE.nodeSize + RE.offsetOpcode];
                    // the branch starts with an atom
                    if (nextOp == RE.OP_ATOM)
                    {
                        // then get that atom as a prefix because there's no other choice
                        int lenAtom = instruction[RE.nodeSize + RE.offsetOpdata];
                        this.prefix = new char[lenAtom];
                        System.arraycopy(instruction, RE.nodeSize * 2, prefix, 0, lenAtom);
                    }
                    // the branch starts with a BOL
                    else if (nextOp == RE.OP_BOL)
                    {
                        // then set the flag indicating that BOL is present
                        this.flags |= OPT_HASBOL;
                    }
                }
            }

            BackrefScanLoop:

            // Check for backreferences
            for (int i = 0; i < lenInstruction; i += RE.nodeSize)
            {
                switch (instruction[i + RE.offsetOpcode])
                {
                    case RE.OP_ANYOF:
                        i += (instruction[i + RE.offsetOpdata] * 2);
                        break;

                    case RE.OP_ATOM:
                        i += instruction[i + RE.offsetOpdata];
                        break;

                    case RE.OP_BACKREF:
                        flags |= OPT_HASBACKREFS;
                        break BackrefScanLoop;
                }
            }
        }
    }

    /**
     * Returns a copy of the prefix of current regular expression program
     * in a character array.  If there is no prefix, or there is no program
     * compiled yet, <code>getPrefix</code> will return null.
     * @return A copy of the prefix of current compiled RE program
     */
    public char[] getPrefix()
    {
        if (prefix != null)
        {
            // Return copy of prefix
            char[] ret = new char[prefix.length];
            System.arraycopy(prefix, 0, ret, 0, prefix.length);
            return ret;
        }
        return null;
    }
}

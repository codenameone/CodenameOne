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

import java.util.ArrayList;
import java.util.List;

/** A node reprresenting a code block */
class BSHBlock extends SimpleNode {
    /** Unique block id for this instance */
    final int blockId;

    /** Whether the block needs to be synchronized */
    public boolean isSynchronized = false;

    /** This block has a static modifier. To be used as a static
     * initialization block within a class. */
    public boolean isStatic = false;

    /** A flag for skipping class declarations when there are none. */
    private boolean hasClassDeclaration = false;

    /** Only check for class declarations the first time through. */
    private boolean isFirst = true;

    BSHBlock(int id) {
        super(id);
        blockId = ++BlockNameSpace.blockCount;
    }

    public Object eval( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        return eval( callstack, interpreter, false );
    }

    /** Evaluate this block and resolve names in the given interpreter context.
    * <p>
    * In the normal course of events each eval will allocate a namespace object
    * swapped with and using call stack top as the parent.During evaluation this
    * new namespace will be used for local variables. When the block is finished
    * the new namespace is released and the old namespace restored.
    * <p>
    * There are some situations where a new name space is not desired, for
    * example:
    * <ul>
    * <li>BshMethod.invokeImpl()
    * <br>The method namespace has already been set up containing
    * the formal parameters.
    * <li>BSHAllocationExpression.constructWithInterfaceBody()
    * <br>The caller sets up a namespace the same as if it would be
    * done using overrideChild=false.
    * <li>BSHEnumConstant.eval()
    * <br>The enum constants should be set within the current namespace.
    * <li>BSHTryStatement.eval()
    * <br>Holds the catch parameter and swaps it on the stack after initializing
    * </ul>
    * In these situations where the overrideChild flag has been set to true *no*
    * new BlockNamespace will be swapped onto the stack and the eval will happen
    * in the current top namespace. If override namespace is null a cached block
    * will be swapped instead of a new instance.
    * @param callstack the stack of namespace chains used to resolve names
    * @param interpreter the interpreter object used for evaluation
    * @param overrideNamespace whether a new namespace is required
    * @return the result from evaluating the block
    * @throws EvalError rolls exceptions back up to the user */
    public Object eval( CallStack callstack, Interpreter interpreter,
            Boolean overrideNamespace ) throws EvalError {

        if ( isSynchronized ) {
            // First node is the expression on which to sync
            Node exp = jjtGetChild(0);
            Object syncValue = exp.eval(callstack, interpreter);
            synchronized( syncValue ) { // Do the actual synchronization
                return evalBlock(
                    callstack, interpreter, overrideNamespace, null/*filter*/);
            }
        }
        return evalBlock(
                callstack, interpreter, overrideNamespace, null/*filter*/);
    }

    Object evalBlock( CallStack callstack, Interpreter interpreter,
            Boolean overrideNamespace, NodeFilter nodeFilter ) throws EvalError {

        Object ret = Primitive.VOID;
        final NameSpace enclosingNameSpace;
        if ( null == overrideNamespace )
            enclosingNameSpace = callstack.swap(
                BlockNameSpace.getInstance(callstack.top(), blockId));
        else if ( !overrideNamespace )
            enclosingNameSpace = callstack.swap(
                new BlockNameSpace(callstack.top(), blockId));
        else enclosingNameSpace = null;

        int startChild = isSynchronized ? 1 : 0;
        int numChildren = jjtGetNumChildren();

        try {
            // Evaluate block in two passes:
            // First do class declarations then do everything else.
            if (isFirst || hasClassDeclaration)
                for (int i = startChild; i < numChildren; i++) {
                    Node node = jjtGetChild(i);

                    if ( nodeFilter != null && !nodeFilter.isVisible( node ) )
                        continue;

                    if ( node instanceof BSHClassDeclaration ) {
                        hasClassDeclaration = true;
                        node.eval( callstack, interpreter );
                    }
                }

            List<Node> enumBlocks = null;
            for(int i = startChild; i < numChildren; i++) {
                Node node = jjtGetChild(i);

                if ( node instanceof BSHClassDeclaration )
                    continue;

                // filter nodes
                if ( nodeFilter != null && !nodeFilter.isVisible( node ) )
                    continue;

                // enum blocks need to override enum class members
                // let the class finish initializing first
                if (node instanceof BSHEnumConstant) {
                    if (enumBlocks == null)
                        enumBlocks = new ArrayList<>();
                    enumBlocks.add(node);
                    continue;
                }

                ret = node.eval( callstack, interpreter );

                // statement or embedded block evaluated a return statement
                if ( ret instanceof ReturnControl )
                    break;
            }

            // evaluate the enum constants blocks if any.
            if (enumBlocks != null)
                while (!enumBlocks.isEmpty())
                    enumBlocks.remove(0).eval( callstack, interpreter );

            return ret;
        } finally {
            isFirst = false;
            // Make sure we put the namespace back when we leave.
            if (null != enclosingNameSpace) callstack.swap(enclosingNameSpace);
        }
    }

    public interface NodeFilter {
        public boolean isVisible( Node node );
    }

    @Override
    public String toString() {
        return super.toString() + ": static=" + isStatic + ", synchronized=" + isSynchronized;
    }
}

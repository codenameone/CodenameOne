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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.Map;

import bsh.Types.MapEntry;

class BSHArrayInitializer extends SimpleNode {
    private static final long serialVersionUID = 1L;
    boolean isMapInArray = false;
    Deque<BSHPrimaryExpression> expressionQueue = new ArrayDeque<>();
    BSHArrayInitializer(int id) { super(id); }

    /** Hook into node creation to apply additional configurations.
     * Inform expression children that they are array expressions.
     * @see BSHPrimaryExpression.setArrayExpression
     * {@inheritDoc} */
    @Override
    public void jjtSetParent(Node n) {
        parent = n;
        if ( null != children ) for ( Node c : children )
            if ( c.jjtGetNumChildren() > 0
                    && c.jjtGetChild(0) instanceof BSHPrimaryExpression ) {
                expressionQueue.push((BSHPrimaryExpression) c.jjtGetChild(0));
                expressionQueue.peek().setArrayExpression(this);
            }
    }

    /** Default node eval is disabled for this node type.
     * {@inheritDoc} */
    @Override
    public Object eval( CallStack callstack, Interpreter interpreter )
        throws EvalError {
        throw new EvalError( "Array initializer has no base type.",
            this, callstack );
    }

    /** Construct the array from the initializer syntax.
     * @param baseType the base class type of the array (no dimensionality)
     * @param dimensions the top number of dimensions of the array
     *      e.g. 2 for a String [][];
     * @param callstack default eval call stack
     * @param interpreter default eval interpreter
     * @return array initializer
     * @throws EvalError produced by thrown type errors */
    public Object eval( Class<?> baseType, int dimensions, CallStack callstack,
            Interpreter interpreter ) throws EvalError {
        if ( 0 == jjtGetNumChildren() )
            dimensions = 0;

        // we may infer the baseType, assume they are the same
        Class<?> inferType = baseType;

        // if dimensions are 0 then our work here is done
        if ( 0 == dimensions ) {
            if ( baseType == Void.TYPE || Types.isCollectionType(baseType) )
                inferType = Object.class;
            Object emptyArray = new Object[0];
            return toCollection(emptyArray, baseType, callstack);
        }

        // loose typed arrays ex. a = {1, 2, 3}
        if ( -1 == dimensions ) {
            // apply strict java when loose type arrays or maps are invalid
            if ( interpreter.getStrictJava() )
                throw new EvalException("No declared array type or dimensions.",
                        this, callstack );
            // beans don't have dimensions, check if we can get out of here
            if (isBeanType(baseType))
                return buildBean(baseType, callstack, interpreter);

            // infer dimensions starting with 1 dimension, this initializer node
            dimensions = this.inferDimensions(1, 0, this, callstack, interpreter);

            // ensure type inference for List and Map types
            if ( Types.isCollectionType(inferType) )
                inferType = Void.TYPE;
        }

        // infer the element type
        if ( inferType == Void.TYPE )
            inferType = inferCommonType(null, this, callstack, interpreter);

        // force MapEntry to Map output
        if ( MapEntry.class == inferType && Void.TYPE == baseType
                || MapEntry.class == baseType )
            baseType = Map.class;

        // no common type was inferred
        if ( null == inferType ) {
            inferType = Object.class;
            // assume null value indicates undefined dimension
            // example: {null} makes Object[][]
            dimensions++;
        }

        // evaluate the child nodes and build the array
        Object array = buildArray(dimensions, inferType, callstack, interpreter);

        // clear evaluation cache
        clearEvalCache();

        return toCollection(array, baseType, callstack);
    }

    /** Evaluate child nodes and build the array.
     * @param dimensions array dimensions
     * @param baseType array base type
     * @param callstack default eval call stack
     * @param interpreter default eval interpreter
     * @return an evaluated array
     * @throws EvalError produced by thrown type errors */
    private Object buildArray(int dimensions, Class<?> baseType,
            CallStack callstack, Interpreter interpreter) throws EvalError {
        // allocate the array to store the initializers
        Object[] array = new Object[jjtGetNumChildren()];

        // Evaluate the child nodes
        for ( int i = 0; i < jjtGetNumChildren(); i++ ) {
            final Node node = jjtGetChild(i);
            final Object entry;
            if ( node instanceof BSHArrayInitializer )
                // nested arrays needs at least 2 dimensions to be valid
                if ( dimensions < 2 )
                    // maps in arrays are not arrays, they are typed java.util.Map
                    // identified during node creation as map in array, ensure type
                    // assignable by java.util.Map and evaluate the initializer as
                    // a 1 dimensional array of type LHS.MapEntry
                    if ( isMapInArray((BSHArrayInitializer) node) )
                        entry = ((BSHArrayInitializer) node).eval(
                            MapEntry.class, 1, callstack, interpreter);
                    else
                        // this is an invalid dimension, raise error
                        throw new EvalException(
                            "Invalid Intializer for "+baseType+", at position: "+i,
                            this, callstack );
                else
                    // multidimensional array is supported by dimension size
                    entry = ((BSHArrayInitializer) node).eval(
                        baseType, dimensions-1, callstack, interpreter);
            else
                // evaluate array element node for value
                entry = node.eval( callstack, interpreter);

            if ( entry == Primitive.VOID )
                throw new EvalException(
                    "Void in array initializer, position "+i, this, callstack );

            try {
                // store the value in the array
                array[i] = normalizeEntry(entry, baseType, dimensions, callstack);
            } catch( IllegalArgumentException e ) {
                Interpreter.debug("illegal arg", e);
                throwTypeError( baseType, entry, i, callstack );
            }
        }
        return array;
    }

    /** Evaluate child nodes as a bean type instance.
     * @param baseType bean base type
     * @param callstack default eval call stack
     * @param interpreter default eval interpreter
     * @return an evaluated bean with properties set
     * @throws EvalError produced by thrown type errors */
    private Object buildBean(Class<?> baseType,
            CallStack callstack, Interpreter interpreter) throws EvalError {
        callstack.push(new NameSpace(callstack.top(), baseType.getName()));
        callstack.top().setClassStatic(baseType);
        callstack.top().getThis(interpreter);
        try {
            throw new EvalException("Bean-style array initializers are unsupported in the reduced CN1 runtime.",
                    this, callstack);
        } catch (Throwable t) {
            throw new EvalException(t.getMessage(), this, callstack, t);
        } finally {
            callstack.pop();
        }
    }

    /** Cast the array entry value to type and unwrap an primitives.
     * If the dimensionality of the array is 1 the value element can be
     * Primitive or Object types otherwise we expect an array.
     * @param value the array entry
     * @param baseType base type of the array and expected type of value
     * @param dimensions array dimensions
     * @param callstack the evaluation call stack
     * @return a normalized value for the entry
     * @throws EvalError thrown on cast exceptions */
    private Object normalizeEntry(Object value, Class<?> baseType, int dimensions,
            CallStack callstack) throws EvalError {
        // Null elements indicate undefined dimensions, we do not want to
        // cast those values unless they are value elements.
        if ( dimensions == 1 || value != Primitive.NULL ) try {
            return Primitive.unwrap(
                    Types.castObject(value, baseType, Types.CAST));
        } catch ( UtilEvalError e ) {
            throw e.toEvalException(
                "Error in array initializer", this, callstack );
        }
        // unwrap any primitive, map voids to null, etc.
        return Primitive.unwrap(value);
    }

    /** Cast the produced array if collection type or return array.
     * @param value the resulting array
     * @param type inferred array base type
     * @param callstack the evaluation call stack
     * @return array cast to Map, List or compatible collection types.
     * @throws EvalError thrown on cast exceptions */
    private Object toCollection(Object value, Class<?> type, CallStack callstack)
            throws EvalError {
        if ( Types.isCollectionType(type) ) try {
            return Types.castObject(value, type, Types.CAST);
        } catch ( UtilEvalError e ) {
            e.toEvalError(this, callstack);
        }
        return value;
    }

    /** Maps are not array dimensions they are type java.util.Map.
     * Configures this initializer as a map expression within an
     * array, not to be treated as an array dimension.
     * @param is for isMapInArray */
    void setMapInArray(boolean is) {
        isMapInArray = is;
    }

    /** Determine whether type or current node can be expressed as a bean.
     * @return if this is a bean type */
    private boolean isBeanType(Class<?> type) {
        return Void.TYPE != type && !Types.isCollectionType(type)
            && jjtGetChild(0) instanceof BSHAssignment
            && jjtGetChild(0).jjtGetChild(0) instanceof BSHPrimaryExpression
            && ((BSHPrimaryExpression)jjtGetChild(0).jjtGetChild(0)).isMapExpression
            && jjtGetChild(0).jjtGetChild(0).jjtGetChild(0) instanceof BSHAmbiguousName;
    }

    /** Convenience method to query the provided node's map in array flag.
     * @param init the BSHArrayInitializer to query
     * @return the given initializer's isMapInArray state  */
    private boolean isMapInArray(BSHArrayInitializer init) {
        return init.isMapInArray;
    }

    /** Clear evaluation cache on primary expression references.
     * Nodes are evaluated when it is required to infer unknown base types and
     * dimensions which can be cached to avoid redundancies but needs to clear
     * for block repetitions.*/
    private void clearEvalCache() {
        for ( final BSHPrimaryExpression expression : expressionQueue )
            expression.clearCache();
    }

    /** Infer array dimensions for loose typed array expressions.
     * We traverse down the hierarchy looking only at the first entry unless
     * we find a null or empty value then one up to inspect the next index.
     * @param dimensions the current dimension count
     * @param idx the child node index
     * @param node the node to query
     * @param callstack the evaluation call stack
     * @param interpreter the evaluation interpreter
     * @return the number of dimensions defined
     * @throws EvalError thrown at node evaluation  */
    private int inferDimensions(int dimensions, int idx, Node node,
            CallStack callstack, Interpreter interpreter) throws EvalError {
        // count ArrayInitializer nodes in this hierarchy
        while ( node.jjtGetNumChildren() > idx
                && (node = node.jjtGetChild(idx)) instanceof BSHArrayInitializer
                && !isMapInArray((BSHArrayInitializer) node)
                && node.jjtGetNumChildren() > 0 ) {
            dimensions++;
            idx = 0;
        }
        // certain value elements may require more inference
        if ( !(node instanceof BSHArrayInitializer) ) {
            Object ot = node.eval(callstack, interpreter);
            // if the value element is null look for more dimensions
            // example: {null, {1, 2}} makes int[][]
            if ( ot == Primitive.NULL )
                return inferDimensions(dimensions, ++idx, node.jjtGetParent(),
                        callstack, interpreter);
            // if the value element is an array we can append dimensions
            // example: new {new {1, 2}} makes int[][]
            dimensions += Types.arrayDimensions(Types.getType(ot));
        }
        // if we found an empty array element look for more dimensions
        // example: {{}, {1, 2}} makes int[][] but {{}} makes Object[][]
        else if ( node.jjtGetNumChildren() == 0 )
            return inferDimensions(dimensions, ++idx, node.jjtGetParent(),
                    callstack, interpreter);
        return dimensions;
    }

    /** Helper function to traverse array elements to find the common base type.
     * Recursive calling for each element in the array across all dimensions.
     * Abort if we already inferred Object type or found a MapEntry.
     * @param common the current common type
     * @param node the node to query
     * @param callstack the evaluation call stack
     * @param interpreter the evaluation interpreter
     * @return the common type for all cells
     * @throws EvalError thrown at node evaluation  */
    private Class<?> inferCommonType(Class<?> common, Node node,
            CallStack callstack, Interpreter interpreter ) throws EvalError {
        // Object is already the most common type and maps are typed MapEntry
        if ( Object.class == common || MapEntry.class == common )
            return common;
        // inspect value elements for common type
        if ( node instanceof BSHAssignment ) {
            Object value = node.eval(callstack, interpreter);
            Class<?> type = Types.getType(value, Primitive.isWrapperType(common));
            return Types.getCommonType(common, Types.arrayElementType(type));
        }
        // avoid traversing maps as arrays when nested in array
        if ( node instanceof BSHArrayInitializer
                && isMapInArray((BSHArrayInitializer) node) )
            return Types.getCommonType(common, Map.class);
        // recurse through nested array initializer nodes
        for ( Node child : node.jjtGetChildren() )
            common = this.inferCommonType(common, child, callstack, interpreter);
        return common;
    }

    /** Helper function to build appropriate EvalError on type exceptions.
     * @param baseType the array's component type
     * @param initializer current array dimension
     * @param argNum current cell index
     * @param callstack call stack from eval
     * @throws EvalError the produced type exception */
    private void throwTypeError(
        Class<?> baseType, Object initializer, int argNum, CallStack callstack )
        throws EvalError {
        String rhsType = StringUtil.typeString(initializer);

        throw new EvalException ( "Incompatible type: " + rhsType
            +" in initializer of array type: "+ baseType.getSimpleName()
            +" at position: "+argNum, this, callstack );
    }

    @Override
    public String toString() {
        return super.toString() + ": " + isMapInArray;
    }
}

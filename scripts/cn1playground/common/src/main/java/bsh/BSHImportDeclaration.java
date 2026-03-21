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

class BSHImportDeclaration extends SimpleNode
{
    private static final long serialVersionUID = 1L;
    public boolean importPackage;
    public boolean staticImport;
    public boolean superImport;

    BSHImportDeclaration(int id) { super(id); }

    public Object eval(CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        NameSpace namespace = callstack.top();
        if ( superImport ) try {
            namespace.doSuperImport();
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( this, callstack  );
        }
        else {
            BSHAmbiguousName ambigName = (BSHAmbiguousName) jjtGetChild(0);
            if ( staticImport ) {
                if ( importPackage ) {
                    // import all (*) static members
                    Class<?> clas = ambigName.toClass( callstack, interpreter );
                    namespace.importStatic( clas );
                } else {
                    Object obj = null;
                    Class<?> clas = null;
                    String name = Name.suffix(ambigName.text, 1);
                    try { // import static method from class
                        clas = namespace.getClass(Name.prefix(ambigName.text));
                        obj = Reflect.staticMethodImport(clas, name);
                    } catch (Exception e) { /* ignore try field instead */ }
                    try { // import static field from class
                        if (null != clas && null == obj)
                            obj = Reflect.getLHSStaticField(clas, name);
                    } catch (Exception e) { /* ignore try method instead */ }
                    try { // import static method from Name
                        if (null == obj)
                            obj = ambigName.toObject( callstack, interpreter );
                    } catch (Exception e) { /* ignore try field instead */ }
                    // do we have a method
                    if ( obj instanceof BshMethod ) {
                        namespace.setMethod( (BshMethod) obj );
                        return Primitive.VOID;
                    }
                    if ( !(obj instanceof LHS) )
                        // import static field from Name
                        obj = ambigName.toLHS( callstack, interpreter );
                    // do we have a field
                    if ( obj instanceof LHS && ((LHS) obj).isStatic() ) {
                        namespace.setVariableImpl( ((LHS) obj).getVariable() );
                        return Primitive.VOID;
                    }
                    // no static member found
                    throw new EvalException(ambigName.text
                                        + " is not a static member of a class",
                                        this, callstack );
                }
            } else { // import package
                String name = ambigName.text;
                if ( importPackage )
                    namespace.importPackage(name);
                else
                    namespace.importClass(name);
            }
        }
        return Primitive.VOID;
    }

    @Override
    public String toString() {
        return super.toString() + ": static=" + staticImport + ", *=" + importPackage + ", super import=" + superImport;
    }
}


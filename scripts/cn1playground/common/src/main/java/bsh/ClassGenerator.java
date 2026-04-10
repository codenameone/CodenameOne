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
 *****************************************************************************/

package bsh;

/**
 * Scripted class generation is not supported in the CN1 playground runtime.
 * This stub preserves parser/runtime references while failing explicitly if a
 * script attempts to declare or synthesize classes.
 */
public final class ClassGenerator {
    public enum Type {
        CLASS, INTERFACE, ENUM
    }

    public static final class ClassNodeFilter implements BSHBlock.NodeFilter {
        public static final ClassNodeFilter CLASSSTATICFIELDS = new ClassNodeFilter();
        public static final ClassNodeFilter CLASSSTATICMETHODS = new ClassNodeFilter();
        public static final ClassNodeFilter CLASSINSTANCEFIELDS = new ClassNodeFilter();
        public static final ClassNodeFilter CLASSINSTANCEMETHODS = new ClassNodeFilter();
        public static final ClassNodeFilter CLASSCLASSES = new ClassNodeFilter();

        private ClassNodeFilter() {
        }

        public boolean isVisible(Node node) {
            return false;
        }
    }

    private static final ClassGenerator INSTANCE = new ClassGenerator();

    private ClassGenerator() {
    }

    public static ClassGenerator getClassGenerator() {
        return INSTANCE;
    }

    public Class<?> generateClass(String name, Modifiers modifiers, Class<?>[] interfaces,
            Class<?> superClass, BSHBlock block, Type type, CallStack callstack,
            Interpreter interpreter) throws EvalError {
        throw new EvalError("Scripted class generation is not supported in the Codename One BeanShell runtime.",
                null, callstack);
    }

    public Object invokeSuperclassMethod(BshClassManager bcm, Object instance,
            Class<?> superClass, String methodName, Object[] args) throws EvalError {
        throw new EvalError("Superclass dispatch for generated classes is not supported in the Codename One BeanShell runtime.",
                null, null);
    }
}

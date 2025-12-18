/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package com.codename1.tools.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintains a hierarchical dependency index between classes and methods so we
 * can cheaply look up callers and prune eliminated elements without rescanning
 * bytecode.
 */
public class MethodDependencyGraph {
    private final Map<String, Set<BytecodeMethod>> callersByLookupSignature = new HashMap<String, Set<BytecodeMethod>>();
    private final Map<BytecodeMethod, Set<String>> methodToCalls = new HashMap<BytecodeMethod, Set<String>>();
    private final Map<String, Set<BytecodeMethod>> methodsByClass = new HashMap<String, Set<BytecodeMethod>>();

    public void clear() {
        callersByLookupSignature.clear();
        methodToCalls.clear();
        methodsByClass.clear();
    }

    public void registerMethod(BytecodeMethod method) {
        Set<BytecodeMethod> byClass = methodsByClass.get(method.getClsName());
        if (byClass == null) {
            byClass = new HashSet<BytecodeMethod>();
            methodsByClass.put(method.getClsName(), byClass);
        }
        byClass.add(method);
    }

    public void recordMethodCall(BytecodeMethod caller, String calleeSignature) {
        Set<BytecodeMethod> callers = callersByLookupSignature.get(calleeSignature);
        if (callers == null) {
            callers = new HashSet<BytecodeMethod>();
            callersByLookupSignature.put(calleeSignature, callers);
        }
        callers.add(caller);

        Set<String> calls = methodToCalls.get(caller);
        if (calls == null) {
            calls = new HashSet<String>();
            methodToCalls.put(caller, calls);
        }
        calls.add(calleeSignature);
    }

    public List<BytecodeMethod> getCallers(String calleeSignature) {
        Set<BytecodeMethod> callers = callersByLookupSignature.get(calleeSignature);
        if (callers == null) {
            return new ArrayList<BytecodeMethod>();
        }
        return new ArrayList<BytecodeMethod>(callers);
    }

    public void removeMethod(BytecodeMethod method) {
        Set<String> calls = methodToCalls.remove(method);
        if (calls != null) {
            for (String call : calls) {
                Set<BytecodeMethod> callers = callersByLookupSignature.get(call);
                if (callers != null) {
                    callers.remove(method);
                    if (callers.isEmpty()) {
                        callersByLookupSignature.remove(call);
                    }
                }
            }
        }

        Set<BytecodeMethod> byClass = methodsByClass.get(method.getClsName());
        if (byClass != null) {
            byClass.remove(method);
            if (byClass.isEmpty()) {
                methodsByClass.remove(method.getClsName());
            }
        }
    }

    public void removeClass(String clsName) {
        Set<BytecodeMethod> methods = methodsByClass.remove(clsName);
        if (methods != null) {
            for (BytecodeMethod method : new ArrayList<BytecodeMethod>(methods)) {
                removeMethod(method);
            }
        }
    }
}

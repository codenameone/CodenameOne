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
package com.codename1.backend;

import java.util.Map;

/**
 * A {@code @ManagedResource} bean as the build describes it: its attributes and
 * operations by index, read and invoked through direct calls the build
 * generated. The JMX model -- named attributes you can watch, operations you can
 * call -- with nothing looked up reflectively.
 */
public interface ManagedBean {
    /** The name its metrics are prefixed with. */
    String getObjectName();

    String getDescription();

    String[] attributeNames();

    String[] attributeDescriptions();

    /** The attribute's current value. */
    Object readAttribute(int index) throws Exception;

    String[] operationNames();

    String[] operationDescriptions();

    /** Each operation's parameter names, in order. */
    String[][] operationParameters();

    /**
     * Calls an operation. Arguments arrive by parameter name, as strings,
     * numbers or booleans, and are converted by the generated code.
     */
    Object invoke(int index, Map arguments) throws Exception;
}

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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.util.Set;

/// Runs the backend's bean pass -- see [BackendBeans] -- in every module that
/// uses its annotations, including one with no controller: a library of services
/// still needs its `@Transactional` methods rewritten.
///
/// Registered before [RestControllerAnnotationProcessor], which reads the result
/// to write the entry point. The pass itself runs once per build whichever of the
/// two asks first.
public final class BackendBeanAnnotationProcessor extends AbstractAnnotationProcessor {
    @Override
    public Set<String> getAnnotationDescriptors() {
        return BackendBeans.DESCRIPTORS;
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) {
        // Nothing per class: the pass needs every class at once, and reads them
        // from the index in finish().
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) {
            return;
        }
        BackendBeans.prepare(ctx);
    }
}

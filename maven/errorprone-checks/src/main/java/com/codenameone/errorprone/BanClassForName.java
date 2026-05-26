/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codenameone.errorprone;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Rejects any call to {@code java.lang.Class.forName(...)} inside framework
 * sources. The framework's iOS port translates bytecode to C ahead of time
 * via ParparVM, which cannot resolve classes named only by string at runtime.
 * Reflective Class lookups silently work on Android and JavaSE but fail (or
 * dead-strip) on iOS, leaving cross-platform bugs that only surface in app
 * store builds. Use {@code com.codename1.system.NativeLookup} or an explicit
 * Class literal instead.
 */
@AutoService(BugChecker.class)
@BugPattern(
    summary = "Class.forName is forbidden in Codename One framework code; "
        + "ParparVM cannot resolve classes by string name at runtime. "
        + "Use NativeLookup or a Class literal instead.",
    severity = BugPattern.SeverityLevel.ERROR,
    linkType = BugPattern.LinkType.NONE)
public final class BanClassForName extends BugChecker
    implements MethodInvocationTreeMatcher {

    private static final MethodNameMatcher CLASS_FOR_NAME =
        MethodMatchers.staticMethod()
            .onClass("java.lang.Class")
            .named("forName");

    @Override
    public Description matchMethodInvocation(
            MethodInvocationTree tree, VisitorState state) {
        if (CLASS_FOR_NAME.matches(tree, state)) {
            return describeMatch(tree);
        }
        return Description.NO_MATCH;
    }
}

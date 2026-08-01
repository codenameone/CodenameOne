/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which listeners the generated factory can actually construct.
 *
 * <p>Every case here is a way the naive answer -- bind whatever declared
 * the interface -- produces source that does not compile, or drops a
 * listener that would have worked.</p>
 */
class HealthListenerScanTest {

    private static HealthListenerScan scan() {
        return new HealthListenerScan();
    }

    /** The ordinary case: a public top-level class implementing it. */
    @Test
    void aDirectPublicImplementorIsBound() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Watcher", HealthListenerScan.LISTENER);
        s.declaresPublicType("app/Watcher");
        s.declaresType("app/Watcher", "java/lang/Object", true);

        Map<String, String> out = s.resolve();
        assertEquals(1, out.size());
        assertEquals("app.Watcher", out.get("app.Watcher"));
    }

    /**
     * A concrete listener nothing can build is reported even when a
     * subclass can be.
     *
     * <p>The subclass covers the interface for its own registration, not
     * for the declarer's. The factory has no entry for a class it cannot
     * build, so an app that registers the declarer -- a perfectly
     * ordinary thing to do with a concrete class -- loses its background
     * delivery, and the build said nothing because something else in the
     * hierarchy was buildable.</p>
     */
    @Test
    void aConcreteDeclarerWithNoUsableConstructorIsStillReported() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Needy", HealthListenerScan.LISTENER);
        s.declaresPublicType("app/Needy");
        s.declaresConcreteType("app/Needy");
        // Concrete, but its only constructor takes an argument.
        s.declaresType("app/Needy", "java/lang/Object", false);
        s.declaresPublicType("app/Handy");
        s.declaresConcreteType("app/Handy");
        s.declaresType("app/Handy", "app/Needy", true);

        assertEquals(1, s.resolve().size(),
                "only the buildable one gets a binding");
        List<String> warnings = s.warnings();
        assertEquals(1, warnings.size(),
                "and the one that cannot be built must be named: "
                        + warnings);
        assertTrue(warnings.get(0).contains("app.Needy"), warnings.get(0));
    }

    /**
     * An unbuildable concrete *descendant* is reported too.
     *
     * <p>An abstract base with one good subclass and one that has no
     * usable constructor: the good one covers the base, so the bad one
     * was silent -- and it is just as registerable and just as absent
     * from the factory.</p>
     */
    @Test
    void anUnbuildableConcreteSubclassIsReported() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Base", HealthListenerScan.LISTENER);
        s.declaresPublicType("app/Base");
        s.declaresType("app/Base", "java/lang/Object", false);

        s.declaresPublicType("app/Good");
        s.declaresConcreteType("app/Good");
        s.declaresType("app/Good", "app/Base", true);

        // Concrete, public, and its only constructor takes an argument.
        s.declaresPublicType("app/Bad");
        s.declaresConcreteType("app/Bad");
        s.declaresType("app/Bad", "app/Base", false);

        assertEquals(1, s.resolve().size(),
                "only the buildable subclass gets a binding");
        List<String> warnings = s.warnings();
        assertEquals(1, warnings.size(),
                "the subclass nothing can build must be named: "
                        + warnings);
        assertTrue(warnings.get(0).contains("app.Bad"), warnings.get(0));
    }

    /**
     * A package-private concrete listener is reported as well.
     *
     * <p>Concreteness is about the class, accessibility is about whether
     * the binding can name it: folding them together let a non-public
     * declarer look like an abstract base and be treated as covered by a
     * subclass.</p>
     */
    @Test
    void aNonPublicConcreteListenerIsReported() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Hidden", HealthListenerScan.LISTENER);
        // Concrete but not public, so never constructible.
        s.declaresConcreteType("app/Hidden");
        s.declaresType("app/Hidden", "java/lang/Object", false);
        s.declaresPublicType("app/Visible");
        s.declaresConcreteType("app/Visible");
        s.declaresType("app/Visible", "app/Hidden", true);

        List<String> warnings = s.warnings();
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("app.Hidden"), warnings.get(0));
    }

    /**
     * The interface declared on an abstract base: the base cannot be
     * built, and the concrete subclass never names the interface.
     */
    @Test
    void aSubclassOfAnAbstractDeclarerIsBoundInstead() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Base", HealthListenerScan.LISTENER);
        s.declaresPublicType("app/Base");
        s.declaresType("app/Base", "java/lang/Object", false);
        s.declaresPublicType("app/Concrete");
        s.declaresType("app/Concrete", "app/Base", true);

        Map<String, String> out = s.resolve();
        assertEquals(1, out.size(), "the abstract base must not be bound");
        assertEquals("app.Concrete", out.get("app.Concrete"));
    }

    /** The interface reached through the app's own intermediate one. */
    @Test
    void anIntermediateInterfaceIsFollowed() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/AppListener",
                HealthListenerScan.LISTENER);
        s.implementsInterface("app/Watcher", "app/AppListener");
        s.declaresPublicType("app/Watcher");
        s.declaresType("app/Watcher", "java/lang/Object", true);

        assertEquals("app.Watcher", s.resolve().get("app.Watcher"));
    }

    /** Nothing that cannot be built with new X() is bound. */
    @Test
    void aClassWithoutANoArgConstructorIsNotBound() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Watcher", HealthListenerScan.LISTENER);
        s.declaresPublicType("app/Watcher");
        s.declaresType("app/Watcher", "java/lang/Object", false);

        assertTrue(s.resolve().isEmpty());
        assertFalse(s.warnings().isEmpty(),
                "a listener nothing can construct must be reported");
    }

    /**
     * A public nested class inside a package-private outer one cannot be
     * named from the generated package, however public it is itself.
     */
    @Test
    void aListenerInsideANonPublicOuterClassIsNotBound() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Outer$Watcher",
                HealthListenerScan.LISTENER);
        s.declaresPublicType("app/Outer$Watcher");
        s.declaresType("app/Outer$Watcher", "java/lang/Object", true);
        s.declaresEnclosedBy("app/Outer$Watcher", "app/Outer");
        // app/Outer is deliberately not declared public.

        assertTrue(s.resolve().isEmpty());
        assertFalse(s.warnings().isEmpty());
    }

    /** A nested listener whose whole chain is public is named with dots. */
    @Test
    void aNestedListenerIsNamedThroughItsOuterClass() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Outer$Watcher",
                HealthListenerScan.LISTENER);
        s.declaresPublicType("app/Outer$Watcher");
        s.declaresPublicType("app/Outer");
        s.declaresType("app/Outer$Watcher", "java/lang/Object", true);
        s.declaresEnclosedBy("app/Outer$Watcher", "app/Outer");

        assertEquals("app.Outer.Watcher", s.resolve().get("app.Outer$Watcher"));
    }

    /**
     * A dollar that separates nothing survives: it is a legal Java
     * identifier character and this class is top-level.
     */
    @Test
    void aDollarInATopLevelNameIsNotASeparator() {
        HealthListenerScan s = scan();
        s.implementsInterface("app/Step$Watcher",
                HealthListenerScan.LISTENER);
        s.declaresPublicType("app/Step$Watcher");
        s.declaresType("app/Step$Watcher", "java/lang/Object", true);

        assertEquals("app.Step$Watcher",
                s.resolve().get("app.Step$Watcher"));
    }

    /** An app with no listeners produces nothing and complains about it. */
    @Test
    void anAppWithoutListenersResolvesToNothing() {
        HealthListenerScan s = scan();
        s.declaresType("app/Something", "java/lang/Object", true);

        assertFalse(s.sawAnyListener());
        assertTrue(s.resolve().isEmpty());
        assertTrue(s.warnings().isEmpty());
    }

    /** Two listeners come back in a stable order. */
    @Test
    void severalListenersAreOrdered() {
        HealthListenerScan s = scan();
        for (String cls : new String[] {"app/B", "app/A"}) {
            s.implementsInterface(cls, HealthListenerScan.LISTENER);
            s.declaresPublicType(cls);
            s.declaresType(cls, "java/lang/Object", true);
        }
        assertEquals("[app.A, app.B]",
                s.resolve().keySet().toString());
    }
}

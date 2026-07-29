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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out which classes the generated health listener factory can
 * actually construct, from what the class scanner saw.
 *
 * <p>It exists because the naive answer is wrong in three ways at once,
 * and each was found the hard way. The class that <em>declares</em>
 * {@code HealthBackgroundListener} may be an abstract base or an
 * intermediate interface the app wrote, so binding the direct implementor
 * emits a {@code new Base()} that does not compile while the usable
 * subclass is never bound. A listener with no public no-argument
 * constructor cannot be built at all. And a public nested class inside a
 * package-private outer one cannot be named from the generated package,
 * however public it is itself.</p>
 *
 * <p>Shared rather than copied because it had been copied: the Android
 * builder resolved properly and the iOS one bound direct implementors, so
 * the same app produced different bindings on the two platforms.</p>
 *
 * <p><b>Keep this file in sync with the BuildDaemon copy.</b></p>
 */
class HealthListenerScan {

    /** The interface a background listener implements. */
    static final String LISTENER =
            "com/codename1/health/HealthBackgroundListener";

    /** Types that declare the listener interface themselves. */
    private final Set<String> declarers = new HashSet<>();
    /** Superclass edges, so an inherited declaration is reachable. */
    private final Map<String, String> supers = new HashMap<>();
    /** Interface edges, including the app's own intermediate interfaces. */
    private final Map<String, Set<String>> interfaces = new HashMap<>();
    /** Types that are public. */
    private final Set<String> publicTypes = new HashSet<>();
    /** Types that can be built with {@code new X()}. */
    private final Set<String> constructible = new HashSet<>();

    /// Classes that are concrete -- neither abstract nor an interface --
    /// whatever their constructors look like.
    ///
    /// A concrete listener with no public no-argument constructor is a
    /// class an app can register and nothing can restore, which is a
    /// silent loss of background delivery. An abstract one is not: it
    /// exists to be subclassed, and the subclass is what gets registered.
    /// Both are "not constructible", so the two are tracked apart.
    private final Set<String> concrete = new HashSet<>();
    /** What encloses each nested type, from its InnerClasses attribute. */
    private final Map<String, String> enclosing = new HashMap<>();

    /** Records an implemented interface, framework or otherwise. */
    void implementsInterface(String cls, String iface) {
        if (cls == null || iface == null) {
            return;
        }
        if (LISTENER.equals(iface)) {
            declarers.add(cls);
        }
        // Every edge is kept, not only the framework one: an app may
        // declare `interface AppListener extends HealthBackgroundListener`
        // and implement that, in which case the concrete class never names
        // the framework interface at all.
        Set<String> known = interfaces.get(cls);
        if (known == null) {
            known = new HashSet<>();
            interfaces.put(cls, known);
        }
        known.add(iface);
    }

    /** Records a type, its superclass, and whether it can be built. */
    void declaresType(String cls, String superName, boolean isConstructible) {
        if (cls == null) {
            return;
        }
        if (superName != null) {
            supers.put(cls, superName);
        }
        if (isConstructible) {
            constructible.add(cls);
        }
    }

    /** Records that a type is public. */
    void declaresConcreteType(String cls) {
        concrete.add(cls);
    }

    void declaresPublicType(String cls) {
        if (cls != null) {
            publicTypes.add(cls);
        }
    }

    /** Records what a nested type is a member of. */
    void declaresEnclosedBy(String cls, String outer) {
        if (cls != null && outer != null) {
            enclosing.put(cls, outer);
        }
    }

    /** Whether anything at all declared the listener interface. */
    boolean sawAnyListener() {
        return !declarers.isEmpty();
    }

    /**
     * The bindable listeners, binary name to the name a generated
     * {@code new} expression must use.
     *
     * <p>Ordered by binary name so the generated source is stable build to
     * build.</p>
     */
    Map<String, String> resolve() {
        List<String> ordered = new ArrayList<>(constructible);
        java.util.Collections.sort(ordered);
        Map<String, String> out = new LinkedHashMap<>();
        for (String cls : ordered) {
            if (findListenerAncestor(cls) == null) {
                continue;
            }
            if (!enclosingChainIsPublic(cls)) {
                continue;
            }
            out.put(cls.replace('/', '.'), sourceNameOf(cls));
        }
        return out;
    }

    /**
     * Listeners the app declared that nothing can construct, and the
     * public-nesting failures, as messages worth putting in the build log.
     *
     * <p>Said out loud because the alternative is a listener that never
     * fires and a build that looked fine -- which is the failure this
     * whole generator exists to avoid.</p>
     */
    List<String> warnings() {
        List<String> out = new ArrayList<>();
        for (String cls : constructible) {
            if (findListenerAncestor(cls) != null
                    && !enclosingChainIsPublic(cls)) {
                out.add(cls.replace('/', '.') + " is nested in a non-public"
                        + " class, so the generated binding could not name"
                        + " it; make every enclosing class public or move"
                        + " the listener to its own file");
            }
        }
        Set<String> covered = new HashSet<>();
        for (String cls : constructible) {
            String hit = findListenerAncestor(cls);
            if (hit != null && enclosingChainIsPublic(cls)) {
                covered.add(hit);
            }
        }
        for (String declarer : declarers) {
            // A concrete declarer speaks for itself. A subclass covers the
            // interface for its own registration, not for the declarer's:
            // the factory has no entry for a class it cannot build, so an
            // app registering the declarer loses its background delivery
            // with nothing said. An abstract declarer is a different
            // matter and is covered by any constructible descendant.
            boolean unbuildableConcrete = concrete.contains(declarer)
                    && !constructible.contains(declarer);
            if (unbuildableConcrete || !covered.contains(declarer)) {
                out.add(declarer.replace('/', '.') + " implements"
                        + " HealthBackgroundListener but cannot be"
                        + " constructed by the generated bindings. A"
                        + " background listener must be a top-level or"
                        + " static nested class with a public no-argument"
                        + " constructor; an abstract base needs a concrete"
                        + " subclass, which this app does not appear to"
                        + " have.");
            }
        }
        return out;
    }

    /**
     * The type through which {@code cls} is a listener, or null when it is
     * not one.
     *
     * <p>Both edges matter: the interface can be inherited through an
     * abstract base, and it can equally be reached through an intermediate
     * interface the app declared itself.</p>
     */
    private String findListenerAncestor(String cls) {
        List<String> queue = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        queue.add(cls);
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            if (current == null || !seen.add(current) || seen.size() > 512) {
                continue;
            }
            if (declarers.contains(current)) {
                return current;
            }
            String parent = supers.get(current);
            if (parent != null) {
                queue.add(parent);
            }
            Set<String> known = interfaces.get(current);
            if (known != null) {
                queue.addAll(known);
            }
        }
        return null;
    }

    /**
     * Whether every class enclosing {@code cls} is public.
     *
     * <p>A class the generated bindings construct has to be nameable from
     * the generated package, and that means the whole dotted path, not
     * only the last segment.</p>
     */
    private boolean enclosingChainIsPublic(String cls) {
        // Walked through the InnerClasses metadata, not by splitting the
        // binary name on '$': a dollar is a legal Java identifier
        // character, so a top-level `app.Step$Listener` is a class
        // somebody may really have written.
        Set<String> seen = new HashSet<>();
        String outer = enclosing.get(cls);
        while (outer != null && seen.add(outer)) {
            if (!publicTypes.contains(outer)) {
                return false;
            }
            outer = enclosing.get(outer);
        }
        return true;
    }

    /**
     * The name a generated constructor call has to use, built from the
     * InnerClasses metadata for the same reason.
     */
    private String sourceNameOf(String cls) {
        Set<String> seen = new HashSet<>();
        String simpleNames = "";
        String current = cls;
        while (seen.add(current)) {
            String outer = enclosing.get(current);
            if (outer == null || !current.startsWith(outer + "$")) {
                return (current + simpleNames).replace('/', '.');
            }
            simpleNames = "." + current.substring(outer.length() + 1)
                    + simpleNames;
            current = outer;
        }
        return cls.replace('/', '.');
    }
}

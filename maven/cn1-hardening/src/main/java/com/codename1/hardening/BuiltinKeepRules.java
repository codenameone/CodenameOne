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
package com.codename1.hardening;

import java.util.ArrayList;
import java.util.List;

/**
 * Tier 1 keep rules: the fixed set that must survive on every app, independent of
 * what the input jar contains. These exist because the builders generate stub
 * source <em>after</em> hardening that names classes literally and then compiles
 * it against the hardened classes -- the main class and its {@code Stub}, the
 * generated router and annotation bootstraps, native-interface peers, and the
 * usual reflective seams (enums, serialization, {@code native} members).
 */
public final class BuiltinKeepRules {

    /** The seven generated bootstrap classes the builders splice into the app stub. */
    private static final String[] BOOTSTRAPS = {
            "MapperBootstrap", "BinderBootstrap", "DaoBootstrap", "RestClientBootstrap",
            "ProtoBootstrap", "GrpcClientBootstrap", "GraphQLClientBootstrap"
    };

    private BuiltinKeepRules() {
    }

    /**
     * The complete Tier-1 rule block for the main app class. Shared verbatim with R8 on Android
     * via {@link #forR8(String)} so the same app-level seams are described once for both renamers.
     */
    public static List<String> rules(String mainClass) {
        List<String> r = new ArrayList<String>();
        if (mainClass != null && !mainClass.isEmpty()) {
            r.add("-keep class " + mainClass + " { *; }");
            r.add("-keep class " + mainClass + "Stub { *; }");
        }
        // Generated registries the stub instantiates by literal name.
        r.add("-keep class com.codename1.router.generated.Routes { *; }");
        for (String b : BOOTSTRAPS) {
            r.add("-keep class cn1app." + b + " { *; }");
        }
        // Native interfaces are matched to their implementation by name.
        r.add("-keep class * implements com.codename1.system.NativeInterface { *; }");
        r.add("-keep class **Impl { *; }");
        r.add("-keep class **Stub { *; }");
        // JNI/native method names must not move.
        r.add("-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }");
        // Reflective seams the JDK itself relies on.
        r.add("-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }");
        r.add("-keepclassmembers class * implements java.io.Serializable { "
                + "static final long serialVersionUID; "
                + "private void writeObject(java.io.ObjectOutputStream); "
                + "private void readObject(java.io.ObjectInputStream); "
                + "java.lang.Object writeReplace(); java.lang.Object readResolve(); }");
        r.add("-keep class * implements java.io.Externalizable { *; }");
        // PropertyBusinessObject property/field names ARE the JSON/ORM column names;
        // renaming them silently changes the on-disk schema and the wire format, which
        // corrupts data on the next app upgrade rather than throwing. Keep the member
        // names (the class itself may still be renamed).
        r.add("-keepclassmembernames class * implements com.codename1.properties.PropertyBusinessObject { *; }");
        return r;
    }

    /** The global ProGuard flags the engine always sets. Kept here so the Android R8 export can share them. */
    public static List<String> flags() {
        List<String> r = new ArrayList<String>();
        // ParparVM culls and R8 shrinks; shrinking/optimizing here only risks
        // "works in debug, NPEs in release". Rename and encrypt, nothing else.
        r.add("-dontshrink");
        r.add("-dontoptimize");
        r.add("-dontpreverify");
        // Class files are written to a directory and builds run on a case-insensitive
        // filesystem, so mixed-case names would collide.
        r.add("-dontusemixedcaseclassnames");
        r.add("-dontnote");
        r.add("-dontwarn");
        r.add("-keepattributes Exceptions,InnerClasses,Signature,EnclosingMethod,*Annotation*");
        return r;
    }

    /**
     * The app-level keep rules only, in R8/ProGuard syntax, so Android's generated {@code proguard.cfg}
     * can append them. The flags are not included -- Android manages its own R8 flags.
     */
    public static List<String> forR8(String mainClass) {
        return rules(mainClass);
    }
}

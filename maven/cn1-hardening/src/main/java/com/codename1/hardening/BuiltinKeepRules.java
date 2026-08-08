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
 * generated router and annotation bootstraps, native-interface types, {@code native}
 * members, and {@code enum} {@code values()}/{@code valueOf()}. Codename One has no
 * reflection and does not support serialization, so no {@code Class.forName},
 * {@code Serializable}/{@code Externalizable} or property-name keeps are needed.
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
        // The transcoded-SVG registry: the platform builders probe for this class by its exact name to
        // decide whether to emit its installGlobal() call, so renaming it would make them conclude no
        // SVGs were generated and silently drop SVG rendering from the hardened app.
        r.add("-keep class com.codename1.generated.svg.SVGRegistry { *; }");
        for (String b : BOOTSTRAPS) {
            r.add("-keep class cn1app." + b + " { *; }");
        }
        // Native interfaces are bound to their platform implementation by name. Keep the interface
        // itself here; the specific <Interface>Impl / <Interface>Stub are found by scanning the input
        // (InputJarKeepScanner) and kept individually, rather than the over-broad **Impl / **Stub.
        r.add("-keep class * implements com.codename1.system.NativeInterface { *; }");
        // Background callbacks the OS restarts by the app's PERSISTED class name resolve the listener
        // via Class.forName + newInstance after a process restart (GeofenceManager persists it to
        // Storage; background location and background fetch register a class the platform reconstructs).
        // These are genuine reflective, name-bound seams -- unlike CN1's string-keyed property
        // persistence -- so the class name must stay stable across an app update, or the default
        // per-build mapping renames it and the background callback silently stops. Keep the implementors.
        r.add("-keep class * implements com.codename1.location.GeofenceListener { *; }");
        r.add("-keep class * implements com.codename1.location.LocationListener { *; }");
        r.add("-keep class * implements com.codename1.background.BackgroundFetch { *; }");
        r.add("-keep class * implements com.codename1.background.BackgroundWorker { *; }");
        // A com.codename1.social.Login subclass persists its OAuth access/refresh tokens under keys
        // derived from getClass().getName() (Login.getAccessToken/setAccessToken/validateToken). Renaming
        // an app's Login subclass would change the key after an app update, so the stored session becomes
        // unreadable and the user is silently logged out. Keep the subclasses so the name stays stable.
        r.add("-keep class * extends com.codename1.social.Login { *; }");
        // JNI/native method names must not move.
        r.add("-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }");
        // enum values()/valueOf(String) resolve constants by name, so they are kept -- this is
        // ordinary language behaviour, not reflection.
        r.add("-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }");
        return r;
    }

    /** The global ProGuard flags the engine always sets. Kept here so the Android R8 export can share them. */
    public static List<String> flags() {
        return flags(null);
    }

    /**
     * The global ProGuard flags, tuned for {@code platform}. On the real-JVM targets (JavaSE /
     * desktop) {@code -dontpreverify} is omitted so ProGuard regenerates {@code StackMapTable}
     * frames: without them a class ProGuard emitted unchanged (not rewritten by the string or
     * control-flow transforms) throws {@code VerifyError} on a Java 7+ JVM. The ParparVM ports
     * translate to C and the JavaScript port to JS, so their frames are never JVM-verified and the
     * flag stays (preverification there only costs time).
     */
    public static List<String> flags(String platform) {
        List<String> r = new ArrayList<String>();
        // ParparVM culls and R8 shrinks; shrinking/optimizing here only risks
        // "works in debug, NPEs in release". Rename and encrypt, nothing else.
        r.add("-dontshrink");
        r.add("-dontoptimize");
        if (!isRealJvmTarget(platform)) {
            r.add("-dontpreverify");
        }
        // Class files are written to a directory and builds run on a case-insensitive
        // filesystem, so mixed-case names would collide.
        r.add("-dontusemixedcaseclassnames");
        r.add("-dontnote");
        r.add("-dontwarn");
        // Keep LineNumberTable so a hardened crash still reports its true line (the crash retrace
        // passes device line numbers through, and ParparVM turns the table into on-device debug-line
        // info). SourceFile is NOT kept -- the retrace synthesizes the file name from the class name,
        // so the original .java name is stripped, matching DexGuard.
        r.add("-keepattributes Exceptions,InnerClasses,Signature,EnclosingMethod,*Annotation*,"
                + "LineNumberTable");
        return r;
    }

    /** True for the ports whose hardened classes are executed on a real JVM (so frames are verified). */
    static boolean isRealJvmTarget(String platform) {
        return "javase".equals(platform) || "desktop".equals(platform);
    }

    /**
     * The app-level keep rules only, in R8/ProGuard syntax, so Android's generated {@code proguard.cfg}
     * can append them. The flags are not included -- Android manages its own R8 flags.
     */
    public static List<String> forR8(String mainClass) {
        return rules(mainClass);
    }
}

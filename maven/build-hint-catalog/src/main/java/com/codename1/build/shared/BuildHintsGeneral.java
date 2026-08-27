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
package com.codename1.build.shared;

import com.codename1.build.shared.BuildHints.Hint;

import java.util.List;

/**
 * Hints with no platform prefix, plus hardening and on-device debugging.
 *
 * <p>Seeded by mining every {@code getArg} call site in the builders, so the
 * name and the default match what the build actually reads. Curated entries
 * carry an annotation attribute and, where the domain is provably closed, an
 * enum; the rest are described but set through
 * {@code codenameone_settings.properties}.</p>
 *
 * <p>Split out of {@link BuildHints} because a single class initializer
 * holding every entry would exceed the JVM's 64KB per-method limit.</p>
 */
final class BuildHintsGeneral {

    private BuildHintsGeneral() {
    }

    static void register(List<Hint> h) {
        h.add(new Hint("call.video")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general")
                .doc("Whether video calls are offered, on both platforms. `ios.call.video` and "
                        + "`android.call.video` override it per platform."));

        h.add(new Hint("KeepScreenOn")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general"));

        h.add(new Hint("androidx.appcompat.version")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("build.incSources")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("build.testReporter")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("build.unitTest")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("cn1.androidTheme")
                .aliasOf("and.themeMode")
                .deprecated("Use and.themeMode, or @Android(themeMode = ...).")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .doc("Deprecated alias for and.themeMode (AndroidGradleBuilder.java:4097). "
                        + "Both names configure one setting, so declaring this alongside "
                        + "@Android(themeMode) is a conflict."));

        h.add(new Hint("cn1.buildKey")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("cn1.entitled")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general"));

        h.add(new Hint("cn1.harden.forceOff")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("cn1.hardenLevel")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .def("off")
                .platform("general"));

        h.add(new Hint("cn1.hardened")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general"));

        h.add(new Hint("cn1.hardening.libraryJars")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("cn1.mappingId")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("cn1.nativeTheme")
                .aliasOf("nativeTheme")
                .deprecated("Use nativeTheme, or @Build(nativeTheme = ...).")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .doc("Deprecated alias for nativeTheme (AndroidGradleBuilder.java:4099, "
                        + "IPhoneBuilder.java:947). Both names configure one setting, so "
                        + "declaring this alongside @Build(nativeTheme) is a conflict."));

        h.add(new Hint("db.legacy")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("delayPushCompletion")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general"));

        // NO default, whatever the literal at the call site says. Both builders
        // decide whether Facebook support is in the app at all by asking whether
        // this hint is null, so the 706695982682332 further down is a fallback
        // reached only once the feature is already on -- never a value the build
        // uses by default. Recording it made Add enable Facebook integration
        // against an unrelated shared app ID the moment the row was clicked.

        h.add(new Hint("facebook.clientToken")
                .group(HintGroup.GENERAL)
                .type(HintType.SECRET)
                .platform("general")
                .doc("The client token for an app that requires native Facebook login integration, this is "
                        + "required if the facebook.appId is set."));

        h.add(new Hint("google.adUnitId")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .doc("Allows integrating Admob/Google Play ads into the application see "
                        + "link:https://www.codenameone.com/blog/adding-google-play-ads.html[this]"));

        h.add(new Hint("gradleDependencies")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING_LIST)
                .separator("\n")
                .platform("general"));

        h.add(new Hint("harden.ios.enabled")
                .group(HintGroup.HARDENING)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general"));

        h.add(new Hint("harden.mac.enabled")
                .group(HintGroup.HARDENING)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general"));

        h.add(new Hint("harden.tv.enabled")
                .group(HintGroup.HARDENING)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general"));

        h.add(new Hint("harden.watch.enabled")
                .group(HintGroup.HARDENING)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general"));

        h.add(new Hint("java.version")
                .group(HintGroup.GENERAL)
                .type(HintType.INT)
                .def("8")
                .platform("general")
                .doc("Valid values include 5 or 8. Indicates the JVM version that should be used for server "
                        + "compilation, this is defined by default for newly created apps based on the Java 8 mode "
                        + "selection"));

        h.add(new Hint("maps.provider")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .doc("Selects the native map provider. `android.maps.provider` and "
                        + "`ios.maps.provider` override it for one platform."));

        h.add(new Hint("nativeVerify")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general")
                .doc("`strict` or `warn` turns on ParparVM's native signature check for this build; "
                        + "anything else leaves it off, which is the default. ParparVM encodes the whole "
                        + "Java signature in the C function name, so a native spelled even slightly "
                        + "differently never reaches the linker as an error: the correctly named symbol "
                        + "is simply absent, "
                        + "the dead-code pass reads that as unused, and the feature ships inert. "
                        + "`ios.nativeVerify`, `linux.nativeVerify` and `windows.nativeVerify` override "
                        + "it for one platform."));

        h.add(new Hint("requireKotlinStdlib")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("tvMain")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("vserv.allowSkipping")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("general"));

        h.add(new Hint("vserv.category")
                .group(HintGroup.GENERAL)
                .type(HintType.INT)
                .def("29")
                .platform("general"));

        h.add(new Hint("vserv.countryCode")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .def("null")
                .platform("general"));

        h.add(new Hint("vserv.locale")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .def("en_US")
                .platform("general"));

        h.add(new Hint("vserv.networkCode")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .def("null")
                .platform("general"));

        h.add(new Hint("vserv.scaleMode")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general"));

        h.add(new Hint("vserv.transition")
                .group(HintGroup.GENERAL)
                .type(HintType.INT)
                .def("300000")
                .platform("general"));

        h.add(new Hint("vserv.zone")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("watchMain")
                .group(HintGroup.GENERAL)
                .type(HintType.STRING)
                .platform("general"));

        h.add(new Hint("watchStandalone")
                .group(HintGroup.GENERAL)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("general"));
    }
}

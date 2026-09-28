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

// Build-script snippets for the Gradle Project Workflow chapter of the developer
// guide. Each tagged region is a separate file in a real project; they are kept
// together here so the guide has one checked source to include from.

// tag::gradle-settings[]
pluginManagement {
    repositories {
        maven("https://repo.codenameone.com/maven2")
        gradlePluginPortal()
    }
}

plugins {
    id("com.codenameone") version "VERSION"
}

rootProject.name = "MyApp"
// end::gradle-settings[]

// tag::gradle-build-script[]
dependencies {
    // A Codename One library published to a Maven repository:
    cn1lib("com.codenameone:googlemaps-lib:1.0")

    // A plain Java library that only uses APIs Codename One supports:
    implementation("org.example:library:1.0")
}
// end::gradle-build-script[]

// tag::gradle-codenameone-block[]
codenameone {
    buildHints.put("ios.newStorageLocation", "true")
    buildHints.put("android.targetSDKVersion", "35")
}
// end::gradle-codenameone-block[]

// tag::gradle-kotlin-plugin[]
plugins {
    kotlin("jvm") version "2.2.10"
}

dependencies {
    // the application's own dependencies, as before
}
// end::gradle-kotlin-plugin[]

// tag::gradle-cn1lib-build[]
group = "com.example"
version = "1.0"

publishing {
    repositories {
        maven {
            name = "company"
            url = uri("https://maven.example.com/releases")
        }
    }
}
// end::gradle-cn1lib-build[]

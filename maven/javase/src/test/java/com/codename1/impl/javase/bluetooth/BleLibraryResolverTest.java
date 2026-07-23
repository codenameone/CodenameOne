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
package com.codename1.impl.javase.bluetooth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The OS/arch-keyed classpath layout {@link BleLibraryResolver} expects the
 * bundled {@code libcn1ble} to ship in: a universal dylib on macOS, per-arch
 * ELF/PE elsewhere. Pure logic, no filesystem or native load.
 */
public class BleLibraryResolverTest {

    @Test
    public void macIsUniversalRegardlessOfArch() {
        assertEquals(BleLibraryResolver.LIBRARY_RESOURCE_DIR
                        + "macos/libcn1ble.dylib",
                BleLibraryResolver.libraryResourcePath("Mac OS X", "aarch64"));
        assertEquals(BleLibraryResolver.LIBRARY_RESOURCE_DIR
                        + "macos/libcn1ble.dylib",
                BleLibraryResolver.libraryResourcePath("Mac OS X", "x86_64"));
    }

    @Test
    public void linuxAndWindowsArePerArch() {
        assertEquals(BleLibraryResolver.LIBRARY_RESOURCE_DIR
                        + "linux/x64/libcn1ble.so",
                BleLibraryResolver.libraryResourcePath("Linux", "amd64"));
        assertEquals(BleLibraryResolver.LIBRARY_RESOURCE_DIR
                        + "linux/arm64/libcn1ble.so",
                BleLibraryResolver.libraryResourcePath("Linux", "aarch64"));
        assertEquals(BleLibraryResolver.LIBRARY_RESOURCE_DIR
                        + "windows/x64/cn1ble.dll",
                BleLibraryResolver.libraryResourcePath("Windows 11", "amd64"));
    }

    @Test
    public void unsupportedArchOrOsResolvesToNothing() {
        assertNull(BleLibraryResolver.libraryResourcePath("Linux", "x86"));
        assertNull(BleLibraryResolver.libraryResourcePath("SunOS", "sparc"));
    }

    @Test
    public void archAliasesNormalize() {
        assertEquals("x64", BleLibraryResolver.normalizeArch("x86_64"));
        assertEquals("x64", BleLibraryResolver.normalizeArch("amd64"));
        assertEquals("arm64", BleLibraryResolver.normalizeArch("aarch64"));
        assertEquals("arm64", BleLibraryResolver.normalizeArch("arm64"));
        assertNull(BleLibraryResolver.normalizeArch("i386"));
    }
}

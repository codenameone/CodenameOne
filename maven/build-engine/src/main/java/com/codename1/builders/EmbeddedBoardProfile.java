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

import java.util.LinkedHashMap;
import java.util.Map;

/// What the embedded Linux target needs to know about a board, keyed by the
/// `linux.embedded.board` build hint. A profile only sets defaults: every value
/// can still be overridden by its own hint, and at run time by the matching
/// `CN1_*` environment variable (see `cn1_linux_embedded.c`).
///
/// The display default is `auto` for every board, which tries KMS and then
/// fbdev, so an unlisted board is never worse off than a listed one -- the
/// profile exists for the architecture, the expected panel density and the
/// boards whose usual panel is mounted rotated.
final class EmbeddedBoardProfile {
    static final String DISPLAY_AUTO = "auto";
    static final String RENDERER_GPU = "gpu";
    static final String RENDERER_SOFTWARE = "software";

    final String name;
    final String arch;
    final String display;
    final int rotation;
    final int dpi;
    /// `gpu` where the board's Mesa GLES 2 driver is upstream and dependable
    /// (VC4/V3D, Etnaviv, Panfrost/Panthor, Intel and AMD on x86-64, and
    /// NVIDIA's own GBM-capable driver on Jetson); `software` where the GPU is
    /// absent or its driver is out of tree, so a default build always renders.
    final String renderer;
    final String description;

    private static final Map<String, EmbeddedBoardProfile> PROFILES = new LinkedHashMap<String, EmbeddedBoardProfile>();

    static {
        add("rpi5", LinuxNativeBuilder.ARCH_ARM64, 0, 0, RENDERER_GPU, "Raspberry Pi 5 (vc4/v3d KMS)");
        add("rpi4", LinuxNativeBuilder.ARCH_ARM64, 0, 0, RENDERER_GPU, "Raspberry Pi 4 / 400 / CM4 (vc4/v3d KMS)");
        add("rpi3", LinuxNativeBuilder.ARCH_ARM64, 0, 0, RENDERER_GPU, "Raspberry Pi 3 / CM3, 64-bit OS");
        add("rpi-zero2", LinuxNativeBuilder.ARCH_ARM64, 0, 0, RENDERER_GPU, "Raspberry Pi Zero 2 W, 64-bit OS");
        add("rpi-armv7", LinuxNativeBuilder.ARCH_ARMV7, 0, 0, RENDERER_GPU, "Any Raspberry Pi on a 32-bit OS");
        // The official 7" touch display reports no physical size over DSI.
        add("rpi-touch7", LinuxNativeBuilder.ARCH_ARM64, 0, 133, RENDERER_GPU, "Raspberry Pi with the official 7\" DSI display");
        add("imx8", LinuxNativeBuilder.ARCH_ARM64, 0, 0, RENDERER_GPU, "NXP i.MX 8M family (LCDIF/DCSS KMS)");
        add("imx6", LinuxNativeBuilder.ARCH_ARMV7, 0, 0, RENDERER_GPU, "NXP i.MX 6 family (IPUv3 KMS)");
        add("rk3588", LinuxNativeBuilder.ARCH_ARM64, 0, 0, RENDERER_GPU, "Rockchip RK3588 / RK3566 boards (VOP2 KMS)");
        add("jetson", LinuxNativeBuilder.ARCH_ARM64, 0, 0, RENDERER_GPU, "NVIDIA Jetson Orin / Nano");
        add("beagleplay", LinuxNativeBuilder.ARCH_ARM64, 0, 0, "BeaglePlay / AM62x (tidss KMS)");
        add("visionfive2", LinuxNativeBuilder.ARCH_RISCV64, 0, 0, "StarFive VisionFive 2 / JH7110");
        add("generic-arm64", LinuxNativeBuilder.ARCH_ARM64, 0, 0, "Any 64-bit ARM board");
        add("generic-armv7", LinuxNativeBuilder.ARCH_ARMV7, 0, 0, "Any 32-bit ARM hard-float board");
        add("generic-x64", LinuxNativeBuilder.ARCH_X64, 0, 0, RENDERER_GPU, "x86-64 panel PCs and kiosks");
        add("generic-riscv64", LinuxNativeBuilder.ARCH_RISCV64, 0, 0, "Any 64-bit RISC-V board");
    }

    private EmbeddedBoardProfile(String name, String arch, String display, int rotation, int dpi,
            String renderer, String description) {
        this.name = name;
        this.arch = arch;
        this.display = display;
        this.rotation = rotation;
        this.dpi = dpi;
        this.renderer = renderer;
        this.description = description;
    }

    private static void add(String name, String arch, int rotation, int dpi, String description) {
        add(name, arch, rotation, dpi, RENDERER_SOFTWARE, description);
    }

    private static void add(String name, String arch, int rotation, int dpi, String renderer, String description) {
        PROFILES.put(name, new EmbeddedBoardProfile(name, arch, DISPLAY_AUTO, rotation, dpi, renderer, description));
    }

    /// The profile for a board name, or `generic-arm64` for an unknown or absent one.
    static EmbeddedBoardProfile forBoard(String board) {
        EmbeddedBoardProfile p = board == null ? null : PROFILES.get(board.trim());
        return p != null ? p : PROFILES.get("generic-arm64");
    }

    static Map<String, EmbeddedBoardProfile> all() {
        return PROFILES;
    }

    /// Validates a display hint; anything unknown falls back to auto.
    static String normalizeDisplay(String display) {
        if ("kms".equals(display) || "fbdev".equals(display) || "memory".equals(display)
                || "headless".equals(display)) {
            return display;
        }
        return DISPLAY_AUTO;
    }

    /// Validates a renderer hint; anything but `gpu` or `software` keeps the default.
    static String normalizeRenderer(String renderer, String def) {
        if (RENDERER_GPU.equals(renderer) || RENDERER_SOFTWARE.equals(renderer)) {
            return renderer;
        }
        return def;
    }

    /// Validates a rotation hint; anything but 90, 180 or 270 is 0.
    static int normalizeRotation(int rotation) {
        return rotation == 90 || rotation == 180 || rotation == 270 ? rotation : 0;
    }
}

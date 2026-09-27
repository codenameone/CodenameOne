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
 * Desktop, native Windows, native Linux and JavaScript build hints.
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
final class BuildHintsDesktop {

    private BuildHintsDesktop() {
    }

    static void register(List<Hint> h) {

        h.add(new Hint("desktop.title")
                .group(HintGroup.DESKTOP)
                .type(HintType.STRING)
                .platform("desktop"));

        h.add(new Hint("javascript.includeVideoJS")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("javascript"));

        h.add(new Hint("javascript.inject_proxy")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("javascript")
                .doc("true/false (defaults to `true`). The ParparVM builder generates a same-origin proxy "
                        + "bundle and configures the app to use it. Setting this to `false` disables both proxy "
                        + "generation and proxy URL injection."));

        h.add(new Hint("javascript.allowBrowserTranslation")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("javascript")
                .doc("true/false (defaults to `false`). By default the page opts out of browser "
                        + "machine translation (`<meta name=\"google\" content=\"notranslate\">` and "
                        + "`translate=\"no\"`), because a translator rewriting the page under the running "
                        + "app, or loading it through a translation proxy, breaks it. Set to `true` to let "
                        + "the browser offer translation."));

        h.add(new Hint("javascript.darkreaderLock")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("javascript")
                .doc("true/false (defaults to `true`). Emits `<meta name=\"darkreader-lock\">` so the "
                        + "Dark Reader extension leaves the app's own colors alone. Dark Reader honours the "
                        + "lock in its default Dynamic mode only; its Filter, Filter+ and Static modes "
                        + "ignore it. Set to `false` to omit the tag."));

        h.add(new Hint("javascript.desktopTheme")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .def("auto")
                .valuePattern("auto|fluent|aqua|adwaita|none")
                .platform("javascript")
                .doc("The native theme a desktop browser gets. `auto` (default) picks Windows Fluent, "
                        + "macOS Aqua or GNOME Adwaita from the browser's operating system when "
                        + "`nativeTheme=native`; phones and tablets keep the iOS and Android themes. "
                        + "`fluent`, `aqua` or `adwaita` pins one theme for every desktop browser, so "
                        + "the bundle carries only that one. `none` keeps the mobile themes on the "
                        + "desktop too."));

        h.add(new Hint("javascript.native.theme")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .platform("javascript")
                .doc("An explicit native theme resource, such as `/WindowsFluentTheme.res`, used in "
                        + "every browser instead of the one the theme hints and the browser would pick."));

        h.add(new Hint("javascript.portSources")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .platform("javascript"));

        h.add(new Hint("javascript.proxy.allowedTargets")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .platform("javascript")
                .doc("Comma-separated target origins, host names, or wildcard subdomains that a generated "
                        + "proxy may access, for example `https://api.example.com,*.services.example.org`. If "
                        + "omitted, the proxy accepts any HTTP or HTTPS target and the build emits a warning."));

        h.add(new Hint("javascript.proxy.target")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .def("jakarta-servlet")
                .platform("javascript")
                .doc("The generated ParparVM proxy deployment platform. Supported values are `jakarta-servlet` "
                        + "(default), `javax-servlet`, `node`, `php`, `aws-lambda`, `google-cloud-functions`, "
                        + "`cloudflare-workers`, and `none`."));

        h.add(new Hint("javascript.proxy.url")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .platform("javascript")
                .doc("The URL of an existing proxy to use for network requests. Setting it suppresses "
                        + "generated proxy packaging unless `javascript.proxy.target` is also set. If "
                        + "`javascript.inject_proxy` is `false`, this build hint is ignored."));

        h.add(new Hint("javascript.textSelection")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("javascript")
                .doc("true/false (defaults to `false`). Makes read-only text -- labels, span labels and "
                        + "non-editable text areas -- selectable and copyable in every form, using the "
                        + "framework's own text selection. A mouse selects by dragging; a touch screen "
                        + "selects with a long press, so a swipe still scrolls."));

        h.add(new Hint("javascript.titleBar")
                .group(HintGroup.JAVASCRIPT)
                .type(HintType.STRING)
                .def("toolbar")
                .valuePattern("toolbar|html")
                .platform("javascript")
                .doc("How a desktop native theme presents the form title and commands in a browser. "
                        + "`toolbar` (default) keeps the Toolbar in the app, styled by the desktop theme. "
                        + "`html` shows the title in an HTML title bar and the commands in an HTML menu "
                        + "bar above the app, the way the native desktop ports use the window title and "
                        + "the system menu bar."));

        h.add(new Hint("linux.arch")
                .group(HintGroup.LINUX)
                .type(HintType.STRING)
                .platform("linux"));

        h.add(new Hint("linux.nativeVerify")
                .group(HintGroup.LINUX)
                .type(HintType.STRING)
                .platform("linux")
                .doc("`nativeVerify` for the native Linux translation alone."));

        h.add(new Hint("linux.cc")
                .group(HintGroup.LINUX)
                .type(HintType.STRING)
                .platform("linux"));

        h.add(new Hint("linux.debug")
                .group(HintGroup.LINUX)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("linux"));

        h.add(new Hint("linux.libc")
                .group(HintGroup.LINUX)
                .type(HintType.STRING)
                .def("glibc")
                .platform("linux"));

        h.add(new Hint("linux.musl")
                .group(HintGroup.LINUX)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("linux"));

        h.add(new Hint("linux.muslNativeCc")
                .group(HintGroup.LINUX)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("linux"));

        h.add(new Hint("linux.toolchain")
                .group(HintGroup.LINUX)
                .type(HintType.STRING)
                .platform("linux"));

        h.add(new Hint("windows.arch")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows")
                .doc("Native Windows port only (the `windows-native` build target -- not the JVM `win.*` "
                        + "desktop hints above). Target CPU architecture for the standalone `.exe`: `x64` (the "
                        + "default) or `arm64`. Accepts the usual synonyms (`x86_64`/`amd64`, `aarch64`). clang-cl "
                        + "cross-compiles to the chosen architecture from either host. See the "
                        + "link:#_working_with_the_native_windows_port[Working with the native Windows port "
                        + "chapter]."));

        h.add(new Hint("windows.calendar.restrictedCapability")
                .group(HintGroup.WINDOWS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("windows"));

        h.add(new Hint("windows.nativeVerify")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows")
                .doc("`nativeVerify` for the native Windows translation alone."));

        h.add(new Hint("windows.debug")
                .group(HintGroup.WINDOWS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("windows")
                .doc("Native Windows port only. true/false (defaults to false). When `false` the `.exe` is "
                        + "built optimized and *stripped* -- no PDB, dead-stripped unreferenced code (`/OPT:REF`) "
                        + "and folded identical functions (`/OPT:ICF`) -- which is the shipping default. Set `true` "
                        + "to keep debug symbols (a `.pdb` next to the exe, via `RelWithDebInfo` / clang-cl `/Zi` + "
                        + "linker `/DEBUG`) so a native crash address can be symbolized during development. "
                        + "Optimizations stay on in both cases."));

        h.add(new Hint("windows.msix")
                .group(HintGroup.WINDOWS)
                .type(HintType.BOOLEAN)
                .def("false")
                .platform("windows"));

        h.add(new Hint("windows.msix.identityName")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows"));

        h.add(new Hint("windows.msix.password")
                .group(HintGroup.WINDOWS)
                .type(HintType.SECRET)
                .platform("windows"));

        h.add(new Hint("windows.msix.pfx")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows"));

        h.add(new Hint("windows.msix.publisher")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows"));

        h.add(new Hint("windows.msix.version")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows"));

        h.add(new Hint("windows.sdkRoot")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows")
                .doc("Native Windows port only; used when building on a *non-Windows* host (for example a "
                        + "Linux build server). Path to a Windows SDK laid out by "
                        + "https://github.com/Jake-Shadle/xwin[`xwin splat`] (a directory containing `crt/include` "
                        + "and `sdk/include/um`), used to cross-compile the `.exe` with clang-cl + lld-link instead "
                        + "of a Visual Studio environment. If unset, the `CN1_XWIN_SYSROOT` environment variable is "
                        + "used. Ignored on Windows hosts, which build through Visual Studio. The same SDK serves "
                        + "both `windows.arch` targets (its `x86_64` / `aarch64` lib subdirs)."));

        h.add(new Hint("windows.signing")
                .group(HintGroup.WINDOWS)
                .type(HintType.BOOLEAN)
                .def("true")
                .platform("windows")
                .doc("Native Windows port only. `true`/`false` (default `true`). Set `false` to force an "
                        + "unsigned build even when a certificate is available."));

        h.add(new Hint("windows.signing.digest")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .def("sha256")
                .platform("windows")
                .doc("Native Windows port only. Signature digest algorithm. Default `sha256`."));

        h.add(new Hint("windows.signing.name")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows"));

        h.add(new Hint("windows.signing.password")
                .group(HintGroup.WINDOWS)
                .type(HintType.SECRET)
                .platform("windows"));

        h.add(new Hint("windows.signing.pkcs12")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows"));

        h.add(new Hint("windows.signing.timestampUrl")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .def("http://timestamp.digicert.com")
                .platform("windows")
                .doc("Native Windows port only. RFC&#160;3161 timestamp server used when signing, so the "
                        + "signature stays valid after the certificate expires. Default "
                        + "`http://timestamp.digicert.com`; set empty to disable timestamping."));

        h.add(new Hint("windows.signing.url")
                .group(HintGroup.WINDOWS)
                .type(HintType.STRING)
                .platform("windows"));
    }
}

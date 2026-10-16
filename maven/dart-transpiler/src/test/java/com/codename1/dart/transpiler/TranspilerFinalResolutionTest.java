/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regression coverage for the resolution-level gaps closed on the final new_gallery
 * transpile drive: single-package class-name collisions resolved with same-library
 * preference ({@code widget.<field>}), enhanced-enum instance methods, {@code is!}/{@code ||}
 * flow promotion, dynamic member access, function-typedef call result types, primitive
 * numeric {@code parse}/{@code tryParse}, and {@code Object.runtimeType}.
 */
public class TranspilerFinalResolutionTest {

    /**
     * Two libraries each declare a {@code Backdrop} StatefulWidget with disjoint fields; a
     * State's {@code widget.<field>} must resolve against the same-library widget, not the
     * last one registered globally.
     */
    @Test
    public void widgetFieldResolvesAgainstSameLibraryWidget() {
        String pages =
                "import 'package:flutter/material.dart';\n"
              + "class Backdrop extends StatefulWidget {\n"
              + "  const Backdrop({super.key, required this.isDesktop, this.settingsPage});\n"
              + "  final bool isDesktop;\n"
              + "  final Widget? settingsPage;\n"
              + "  @override\n"
              + "  State<Backdrop> createState() => _BackdropState();\n"
              + "}\n"
              + "class _BackdropState extends State<Backdrop> {\n"
              + "  @override\n"
              + "  Widget build(BuildContext context) =>\n"
              + "      widget.isDesktop ? (widget.settingsPage ?? const Text('x')) : const Text('y');\n"
              + "}\n";
        String crane =
                "import 'package:flutter/material.dart';\n"
              + "class Backdrop extends StatefulWidget {\n"
              + "  const Backdrop({super.key, required this.backLayerItems});\n"
              + "  final List<int> backLayerItems;\n"
              + "  @override\n"
              + "  State<Backdrop> createState() => _CraneState();\n"
              + "}\n"
              + "class _CraneState extends State<Backdrop> {\n"
              + "  @override\n"
              + "  Widget build(BuildContext context) => Text(widget.backLayerItems.length.toString());\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"pages/backdrop.dart", pages}, {"studies/crane/backdrop.dart", crane}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }

    @Test
    public void enhancedEnumInstanceMethod() {
        String src =
                "enum GalleryDemoCategory {\n"
              + "  study, material, cupertino, other;\n"
              + "  @override\n"
              + "  String toString() => name.toUpperCase();\n"
              + "  String? displayTitle(String fallback) => switch (this) {\n"
              + "    study => null,\n"
              + "    material || cupertino => toString(),\n"
              + "    other => fallback,\n"
              + "  };\n"
              + "}\n"
              + "class User {\n"
              + "  String? label(GalleryDemoCategory c) => c.displayTitle('ref');\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }

    /** `x is! T || x.member` flow-promotes x to T in the right operand. */
    @Test
    public void isNotOrPromotesRightOperand() {
        String src =
                "import 'package:flutter/material.dart';\n"
              + "class _Painter extends CustomPainter {\n"
              + "  _Painter({required this.time});\n"
              + "  final double time;\n"
              + "  @override\n"
              + "  void paint(Canvas canvas, Size size) {}\n"
              + "  @override\n"
              + "  bool shouldRepaint(CustomPainter oldDelegate) =>\n"
              + "      oldDelegate is! _Painter || oldDelegate.time != time;\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }

    @Test
    public void dynamicMemberAccessAndTypedefCallAndParseAndRuntimeType() {
        String src =
                "typedef LibraryLoader = Future<void> Function();\n"
              + "class Demos {\n"
              + "  static Future<void> preload(LibraryLoader loader) =>\n"
              + "      loader().then((dynamic _) { print('done'); });\n"
              + "  static String? slugOf(dynamic demo) => demo.slug as String?;\n"
              + "  static double? parse(String v) => double.tryParse(v);\n"
              + "  static int? parseI(String v) => int.parse(v);\n"
              + "}\n"
              + "class BoardPoint {\n"
              + "  final int q;\n"
              + "  BoardPoint(this.q);\n"
              + "  @override\n"
              + "  bool operator ==(Object other) =>\n"
              + "      other.runtimeType == runtimeType && other is BoardPoint && other.q == q;\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
    }
}

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

import fs from "node:fs";
import path from "node:path";

const input = path.resolve(process.argv[2] || "docs/website/public");
const compareFile = fs.existsSync(input) && fs.statSync(input).isDirectory()
    ? path.join(input, "compare", "index.html")
    : input;

function fail(message) {
    throw new Error(`Compare page regression: ${message}`);
}

if (!fs.existsSync(compareFile) || !fs.statSync(compareFile).isFile()) {
    fail(`missing generated file ${compareFile}`);
}

const html = fs.readFileSync(compareFile, "utf8");
const text = html
    .replace(/<script\b[\s\S]*?<\/script>/gi, " ")
    .replace(/<style\b[\s\S]*?<\/style>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ");

const forbidden = [
    [/\bDart engine\b/i, 'calls the Flutter engine a "Dart engine"'],
    [/separately shipped/i, 'describes an app-bundled runtime as "separately shipped"'],
    [/Runtime and RAM/i, "makes an unsubstantiated RAM comparison"],
];

for (const [pattern, message] of forbidden) {
    if (pattern.test(text)) {
        fail(message);
    }
}

const required = [
    [/Runtime packaging/i, "the architecture table must describe runtime packaging"],
    [/AOT-compiled Dart code/i, "Flutter release code must be identified as AOT-compiled Dart"],
    [/Flutter engine/i, "the bundled engine must be identified as the Flutter engine"],
    [/Dart runtime/i, "Flutter runtime support must be named accurately"],
    [/Android[^.]{0,160}\bART\b/i, "the Android comparison must identify ART"],
];

for (const [pattern, message] of required) {
    if (!pattern.test(text)) {
        fail(message);
    }
}

console.log(`Validated comparison terminology in ${compareFile}`);

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
import vm from "node:vm";

const publicDirectory = path.resolve(process.argv[2] || "docs/website/public");

function fail(message) {
    throw new Error(`Port Status regression: ${message}`);
}

function read(relativePath) {
    const file = path.join(publicDirectory, relativePath);
    if (!fs.existsSync(file) || !fs.statSync(file).isFile()) {
        fail(`missing generated file ${relativePath}`);
    }
    return fs.readFileSync(file, "utf8");
}

function countMatches(value, pattern) {
    return Array.from(value.matchAll(pattern)).length;
}

function validate() {
    const home = read("index.html");
    const page = read(path.join("port-status", "index.html"));

    if (!/<a\b[^>]*href=(?:["']?\/port-status\/["']?)[^>]*>[\s\S]*?PORT STATUS[\s\S]*?<\/a>/i.test(home)) {
        fail("the main site navigation does not link to /port-status/");
    }

    const pageText = page.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ");
    if (!/Codename One compliance test suite/i.test(pageText)) {
        fail("the page does not identify the compliance test suite");
    }
    if (/HelloCodenameOne/i.test(pageText)) {
        fail("the public page exposes the internal HelloCodenameOne fixture name");
    }
    if (/port_status\.py/i.test(page)) {
        fail("the public page links to the result-normalizer implementation");
    }

    if (/data-port-status-contract|raw\.githubusercontent\.com|port-status-data\/ports/i.test(page)) {
        fail("the generated page still depends on runtime report data");
    }
    if (/\bdata-port=(?:["']?javase["']?)(?:\s|>)/i.test(page)) {
        fail("JavaSE must not appear as a portability target");
    }

    const portCards = countMatches(page, /\bdata-port-card(?:=|\s|>)/g);
    const featureRows = countMatches(page, /\bdata-feature-row(?:=|\s|>)/g);
    const featureCells = countMatches(page, /\bdata-feature-cell(?:=|\s|>)/g);
    const mappedTests = countMatches(page, /<li><code>[^<]+<\/code><\/li>/g);
    if (portCards !== 10) {
        fail(`generated ${portCards} port cards instead of 10`);
    }
    if (featureRows < 51) {
        fail(`the generated table has only ${featureRows} feature rows`);
    }
    if (featureCells !== portCards * featureRows) {
        fail("the generated compliance matrix is incomplete");
    }
    if (mappedTests < 164) {
        fail(`the generated table exposes only ${mappedTests} mapped tests`);
    }
    for (const feature of ["ar-motion-sensors", "camera-access", "video-decoding", "video-round-trip"]) {
        if (!new RegExp(`data-feature-id=["']?${feature}(?:["'\\s>])`).test(page)) {
            fail(`the generated table is missing the split ${feature} feature row`);
        }
    }

    const renderedCells = countMatches(
        page,
        /<td\b(?=[^>]*\bdata-feature-cell\b)(?=[^>]*\bclass=(?:["']?is-(?:pass|fail|partial|stale|unknown)))[^>]*>/g
    );
    if (renderedCells !== featureCells) {
        fail("not every compliance cell has a build-time status");
    }
    if (/Waiting for report|Loading the latest compliance reports/i.test(page)) {
        fail("the generated page still waits for client-side report rendering");
    }
    const truthTime = page.match(
        /<time\b(?=[^>]*\bdata-port-status-truth\b)(?=[^>]*\bdatetime=(?:["']([^"']+)["']|([^\s>]+)))[^>]*>/i
    );
    if (!truthTime || Number.isNaN(Date.parse(truthTime[1] || truthTime[2])) ||
        countMatches(page, /<time\b/gi) !== 1) {
        fail("the static table must have exactly one valid time-of-truth timestamp");
    }
    if (fs.existsSync(path.join(publicDirectory, "port-status-data"))) {
        fail("report JSON was copied into the public website output");
    }

    const assetMatch = page.match(
        /<script\b[^>]*src=(?:["']([^"']*cn1-port-status[^"']*\.js)["']|([^\s>]*cn1-port-status[^\s>]*\.js))[^>]*><\/script>/i
    );
    if (!assetMatch) {
        fail("the generated page does not load the Port Status JavaScript");
    }
    const assetPath = (assetMatch[1] || assetMatch[2]).replace(/^\//, "");
    const script = read(assetPath);
    try {
        new vm.Script(script, {filename: assetPath});
    } catch (error) {
        fail(`the generated Port Status JavaScript is invalid: ${error.message}`);
    }
    if (/\bfetch\s*\(/.test(script) || /raw\.githubusercontent\.com/.test(script)) {
        fail("the generated JavaScript fetches report data at runtime");
    }
    if (!script.includes("data-category-filter") || !script.includes("data-feature-search")) {
        fail("the generated JavaScript lost table filtering");
    }

    const stylesheetMatch = page.match(
        /<link\b[^>]*href=(?:["']([^"']*stylesheet[^"']*\.css)["']|([^\s>]*stylesheet[^\s>]*\.css))[^>]*>/i
    );
    if (!stylesheetMatch) {
        fail("the generated page has no website stylesheet");
    }
    const stylesheetPath = (stylesheetMatch[1] || stylesheetMatch[2]).replace(/^\//, "");
    const stylesheet = read(stylesheetPath);
    if (!stylesheet.includes(".cn1-port-status__matrix{") ||
        !stylesheet.includes("max-height:min(78vh,920px)") ||
        !/\.cn1-port-status__matrix thead th\{[^}]*position:sticky[^}]*top:0/.test(stylesheet)) {
        fail("the generated table header is not frozen inside its scroll area");
    }

    console.log(
        `Port Status page valid: ${portCards} ports, ` +
        `${featureRows} features, ${mappedTests} mapped tests, static snapshot.`
    );
}

try {
    validate();
} catch (error) {
    console.error(error.message);
    process.exit(1);
}

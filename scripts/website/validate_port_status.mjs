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

function attribute(tag, name) {
    const match = tag.match(new RegExp(`\\b${name}=(?:["']([^"']*)["']|([^\\s>]+))`, "i"));
    return match ? (match[1] || match[2]) : "";
}

function validate() {
    const home = read("index.html");
    const page = read(path.join("port-status", "index.html"));

    const resourcesMenu = home.match(
        /<button\b[^>]*>[\s\S]*?<span>RESOURCES<\/span>[\s\S]*?<\/button>\s*<ul\b[^>]*class=(?:["']?sub-menu["']?)[^>]*>([\s\S]*?)<\/ul>/i
    );
    if (!resourcesMenu || !/<a\b[^>]*href=(?:["']?\/port-status\/["']?)[^>]*>[\s\S]*?Port Status[\s\S]*?<\/a>/i.test(resourcesMenu[1]) ||
        countMatches(home, /href=(?:["']?\/port-status\/["']?)/gi) !== 1) {
        fail("the Resources menu must contain the only /port-status/ navigation item");
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
    if (!/11 CI targets/i.test(pageText) || /11 port targets/i.test(pageText)) {
        fail("the compliance columns must be described as CI targets, not distinct platforms");
    }

    const deploymentRows = countMatches(page, /\bdata-deployment-row(?:=|\s|>)/g);
    const browserResults = countMatches(page, /\bdata-browser-result(?:=|\s|>)/g);
    const performanceRows = countMatches(page, /\bdata-performance-row(?:=|\s|>)/g);
    const performanceCells = countMatches(page, /\bdata-performance-cell(?:=|\s|>)/g);
    if (deploymentRows !== 8 || browserResults !== 3 || performanceRows !== 13 || performanceCells !== 13 * 11) {
        fail("deployment, browser, or performance evidence is incomplete");
    }
    for (const required of [
        "runtime CI evidence", "Mac Catalyst", "Chromium", "Firefox", "WebKit",
        "glibc 2.28", "Ubuntu 20.04", "Debian 10", "RHEL/Rocky/AlmaLinux 8",
        "Binary size (complete application)", "Minimum managed memory", "Peak managed memory",
        "Integer arithmetic", "Transcendental math", "Object allocation", "Quicksort"
    ]) {
        if (!pageText.toLowerCase().includes(required.toLowerCase())) {
            fail(`deployment and performance evidence is missing ${required}`);
        }
    }
    if (/flutter/i.test(pageText)) {
        fail("the Port Status page must not contain unrelated framework-comparison language");
    }

    const portCards = countMatches(page, /\bdata-port-card(?:=|\s|>)/g);
    const featureRows = countMatches(page, /\bdata-feature-row(?:=|\s|>)/g);
    const featureCells = countMatches(page, /\bdata-feature-cell(?:=|\s|>)/g);
    const mappedTests = countMatches(page, /<li><code>[^<]+<\/code><\/li>/g);
    if (portCards !== 11 || !/data-port-card=(?:["']?windows-arm64["']?)(?:\s|>)/i.test(page)) {
        fail(`generated ${portCards} port cards or omitted Windows ARM64; expected 11 targets`);
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
    const platformNames = {
        "android": "Android",
        "ios-gl": "iOS (OpenGL)",
        "ios-metal": "iOS (Metal)",
        "mac-native": "macOS native",
        "javascript": "Web",
        "linux-x64": "Linux x64",
        "linux-arm64": "Linux ARM64",
        "windows-x64": "Windows x64",
        "windows-arm64": "Windows ARM64",
        "watchos": "watchOS",
        "tvos": "tvOS"
    };
    const performanceTags = page.match(/<td\b[^>]*data-performance-cell[^>]*>/g) || [];
    for (const [port, name] of Object.entries(platformNames)) {
        if (!performanceTags.some((tag) => attribute(tag, "data-port") === port &&
            attribute(tag, "title").startsWith(`${name}:`))) {
            fail(`performance cells for ${name} must identify the platform in their tooltip`);
        }
    }
    const primaryCellTags = Array.from(page.matchAll(/<td\b(?=[^>]*\bdata-feature-cell\b)[^>]*>/gi), match => match[0]);
    for (const cell of primaryCellTags) {
        const port = attribute(cell, "data-port");
        if (!platformNames[port] || !attribute(cell, "title").startsWith(`${platformNames[port]}:`)) {
            fail(`compliance cell for ${port || "an unknown port"} has no platform-named tooltip`);
        }
    }

    const errata = Array.from(page.matchAll(/\bdata-skip-erratum=(?:["']([^"']+)["']|([^\s>]+))/gi),
        match => match[1] || match[2]);
    if (errata.length < 2 || !errata.includes("CameraApiTest") || !errata.includes("VideoIORoundTripTest") ||
        !/Every skipped result in the table is accounted for/i.test(pageText) ||
        countMatches(page, /<strong>Port support:<\/strong>/gi) !== errata.length) {
        fail("the generated page does not contain exhaustive skipped-test errata");
    }

    const manualRows = countMatches(page, /\bdata-manual-feature-row(?:=|\s|>)/g);
    const manualCells = countMatches(page, /\bdata-manual-feature-cell(?:=|\s|>)/g);
    if (manualRows < 20 || manualCells !== manualRows * portCards) {
        fail("the environment-dependent feature matrix is incomplete");
    }
    const manualCellTags = Array.from(page.matchAll(/<td\b(?=[^>]*\bdata-manual-feature-cell\b)[^>]*>/gi), match => match[0]);
    for (const cell of manualCellTags) {
        const port = attribute(cell, "data-port");
        const state = attribute(cell, "class");
        if (!platformNames[port] || !attribute(cell, "title").startsWith(`${platformNames[port]}:`) ||
            !/^is-(?:supported|conditional|fallback|unavailable)$/.test(state)) {
            fail(`environment-dependent cell for ${port || "an unknown port"} is incomplete`);
        }
    }
    if (countMatches(page, /<b>Verification:<\/b>/gi) !== manualRows ||
        countMatches(page, /<b>Why it is outside the suite:<\/b>/gi) !== manualRows) {
        fail("each environment-dependent feature must explain its test logic and suite exclusion");
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
    const matrixRule = stylesheet.match(/\.cn1-port-status__matrix\{([^}]*)\}/);
    if (!matrixRule || /max-height|overflow:(?:auto|scroll)/.test(matrixRule[1]) ||
        !/\.cn1-port-status__matrix thead th\{[^}]*position:sticky[^}]*top:0/.test(stylesheet) ||
        !stylesheet.includes(".cn1-port-status__errata-list{") ||
        !stylesheet.includes(".cn1-port-status__matrix--manual")) {
        fail("the table header must stick during normal page scrolling without a nested table scroller");
    }

    console.log(
        `Port Status page valid: ${portCards} CI targets, ${deploymentRows} deployment platforms, ` +
        `${featureRows} automated features, ${manualRows} environment-dependent features, ` +
        `${mappedTests} mapped tests, static snapshot.`
    );
}

try {
    validate();
} catch (error) {
    console.error(error.message);
    process.exit(1);
}

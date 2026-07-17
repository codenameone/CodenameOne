#!/usr/bin/env node
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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

const inputDirectory = path.resolve(process.argv[2] || "artifacts/browser-evidence");
const outputFile = path.resolve(process.argv[3] || "artifacts/port-status-environment.json");
const expectedBrowsers = ["chromium", "firefox", "webkit"];
const displayNames = {chromium: "Chromium", firefox: "Firefox", webkit: "WebKit"};
const coverage = {
    chromium: "Full compliance suite plus nightly lifecycle validation",
    firefox: "Nightly lifecycle validation",
    webkit: "Nightly lifecycle validation"
};

function reportsUnder(directory) {
    const reports = [];
    for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
        const candidate = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            reports.push(...reportsUnder(candidate));
        } else if (entry.isFile() && entry.name === "report.json") {
            reports.push(candidate);
        }
    }
    return reports;
}

if (!fs.existsSync(inputDirectory)) {
    throw new Error(`Browser evidence directory does not exist: ${inputDirectory}`);
}

const byBrowser = new Map();
for (const reportFile of reportsUnder(inputDirectory)) {
    const results = JSON.parse(fs.readFileSync(reportFile, "utf8"));
    if (!Array.isArray(results) || results.length === 0) {
        throw new Error(`Empty lifecycle report: ${reportFile}`);
    }
    const browser = results[0].browser;
    if (!expectedBrowsers.includes(browser) || results.some(result => result.browser !== browser)) {
        throw new Error(`Invalid browser identity in ${reportFile}`);
    }
    if (byBrowser.has(browser)) {
        throw new Error(`More than one lifecycle report was supplied for ${browser}`);
    }
    const versions = [...new Set(results.map(result => result.engineVersion).filter(Boolean))];
    byBrowser.set(browser, {
        id: browser,
        name: displayNames[browser],
        engine_version: versions.length === 1 ? versions[0] : "Browser did not launch",
        status: results.every(result => result.ok) ? "pass" : "fail",
        coverage: coverage[browser]
    });
}

for (const browser of expectedBrowsers) {
    if (!byBrowser.has(browser)) {
        throw new Error(`Missing lifecycle report for ${browser}`);
    }
}

const output = {
    schema_version: 1,
    generated_at: new Date().toISOString().replace(/\.\d{3}Z$/, "Z"),
    commit: process.env.GITHUB_SHA || null,
    browsers: expectedBrowsers.map(browser => byBrowser.get(browser))
};

fs.mkdirSync(path.dirname(outputFile), {recursive: true});
fs.writeFileSync(outputFile, `${JSON.stringify(output, null, 2)}\n`);
console.log(`Collected browser evidence for ${expectedBrowsers.join(", ")} in ${outputFile}`);

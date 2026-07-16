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

    const contractMatch = page.match(
        /<script\b(?=[^>]*\bdata-port-status-contract\b)[^>]*>([\s\S]*?)<\/script>/i
    );
    if (!contractMatch) {
        fail("the generated page does not embed its compliance contract");
    }

    let contract;
    try {
        contract = JSON.parse(contractMatch[1]);
    } catch (error) {
        fail(`the embedded compliance contract is invalid JSON: ${error.message}`);
    }
    if (!contract || typeof contract !== "object" || Array.isArray(contract)) {
        fail("the embedded compliance contract was serialized as a string");
    }
    if (!Array.isArray(contract.ports) || contract.ports.length !== 10) {
        fail("the contract must contain the 10 portability targets");
    }
    if (contract.ports.some((port) => port.id === "javase")) {
        fail("JavaSE must not appear as a portability target");
    }
    if (!Array.isArray(contract.features) || contract.features.length < 49) {
        fail("the compliance feature map lost required rows");
    }

    const mappedTests = new Set();
    for (const feature of contract.features) {
        if (!Array.isArray(feature.tests) || feature.tests.length === 0) {
            fail(`feature ${feature.id || "<unknown>"} has no mapped tests`);
        }
        for (const test of feature.tests) {
            if (mappedTests.has(test)) {
                fail(`test ${test} is mapped more than once`);
            }
            mappedTests.add(test);
        }
    }
    if (mappedTests.size < 164) {
        fail("the compliance contract lost mapped tests");
    }

    const portCards = countMatches(page, /\bdata-port-card(?:=|\s|>)/g);
    const featureRows = countMatches(page, /\bdata-feature-row(?:=|\s|>)/g);
    const featureCells = countMatches(page, /\bdata-feature-cell(?:=|\s|>)/g);
    if (portCards !== contract.ports.length) {
        fail(`generated ${portCards} port cards for ${contract.ports.length} ports`);
    }
    if (featureRows !== contract.features.length) {
        fail(`generated ${featureRows} feature rows for ${contract.features.length} features`);
    }
    if (featureCells !== contract.ports.length * contract.features.length) {
        fail("the generated compliance matrix is incomplete");
    }

    if (contract.seed_report_base !== "/port-status-data/ports/") {
        fail("the initial master-result fallback is not configured");
    }
    for (const port of contract.ports) {
        const report = JSON.parse(read(path.join("port-status-data", "ports", `${port.id}.json`)));
        if (report.port !== port.id || report.schema_version !== contract.schema_version) {
            fail(`seed report ${port.id} does not match the compliance contract`);
        }
        if (report.workflow_conclusion !== "success" || report.summary?.fail !== 0) {
            fail(`seed report ${port.id} is not based on a successful master workflow`);
        }
        const reportTests = Object.keys(report.tests || {});
        if (reportTests.length !== mappedTests.size || reportTests.some((test) => !mappedTests.has(test))) {
            fail(`seed report ${port.id} does not cover every mapped test`);
        }
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
    if (!script.includes("seed_report_base") || !script.includes("successful-master-workflow")) {
        fail("the generated JavaScript lost master-result fallback handling");
    }

    console.log(
        `Port Status page valid: ${contract.ports.length} ports, ` +
        `${contract.features.length} features, ${mappedTests.size} mapped tests.`
    );
}

try {
    validate();
} catch (error) {
    console.error(error.message);
    process.exit(1);
}

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

import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const publicDir = path.resolve(process.argv[2] || "docs/website/public");
const initializrUrl = "/initializr/";
const registerUrl = "https://cloud.codenameone.com/register";

/*
 * Build-first: every primary CTA sends a visitor to Initializr, and the account
 * is created by the first cloud build (the build tool's sign-in offers "Create
 * an account"). That is how first-time builders converted before the
 * 2026-08-29 switch to signup-first (#5625), which grew signups ~5x while new
 * builders fell to 5-8 a month: people signed up with no project, landed in
 * the console, and 69% of them left without downloading anything. The
 * Initializr download beacon (#5935) and BuildCloud's first-build telemetry
 * make this path measurable, which is what it lacked before.
 */
function page(name) {
  const file = name === "home"
    ? path.join(publicDir, "index.html")
    : path.join(publicDir, name, "index.html");
  assert.ok(fs.existsSync(file), `missing generated ${name} page: ${file}`);
  return fs.readFileSync(file, "utf8");
}

function anchorElements(html) {
  return Array.from(html.matchAll(/<a\b[^>]*>[\s\S]*?<\/a>/gi), ([element]) => {
    const openingTag = element.match(/^<a\b[^>]*>/i)?.[0] || "";
    const attributes = {};
    const attributePattern = /([^\s=/>]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+)))?/g;
    const source = openingTag.replace(/^<a\b/i, "").replace(/>$/, "");
    let match;
    while ((match = attributePattern.exec(source)) !== null) {
      attributes[match[1].toLowerCase()] = match[2] ?? match[3] ?? match[4] ?? "";
    }
    return {
      attributes,
      text: element.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim(),
    };
  });
}

function assertProjectCta(html, event) {
  const found = anchorElements(html).some(({ attributes }) =>
    attributes.href === initializrUrl && attributes["data-cn1-conversion"] === event
  );
  assert.ok(found, `${event} must send to Initializr`);
}

assertProjectCta(
  `<a href="${initializrUrl}" data-cn1-conversion="quoted-project">Create</a>`,
  "quoted-project"
);
assertProjectCta(
  `<a href=${initializrUrl} data-cn1-conversion=minified-project>Create</a>`,
  "minified-project"
);

const home = page("home");
const pricing = page("pricing");
const compare = page("compare");

assertProjectCta(home, "home-primary-project");
assertProjectCta(home, "home-final-project");
assertProjectCta(pricing, "pricing-free-project");
assertProjectCta(compare, "compare-project");
assertProjectCta(compare, "compare-final-project");

assert.ok(anchorElements(home).some(({ attributes, text }) =>
  attributes.href === initializrUrl && text === "Create Project"
), "the global header must lead with Create Project");

for (const html of [home, pricing, compare]) {
  assert.doesNotMatch(html, /(?:home-(?:primary|final)|pricing-free|compare(?:-final)?)-signup/i,
    "primary funnel CTAs must not regress to signup-first");
  assert.ok(!anchorElements(html).some(({ attributes }) =>
    attributes.href === registerUrl && attributes["data-cn1-conversion"]
  ), "no primary CTA may send straight to registration");
}

console.log(`Validated build-first routing in ${publicDir}`);

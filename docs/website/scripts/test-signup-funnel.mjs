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
const registerUrl = "https://cloud.codenameone.com/register";

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

function assertSignupCta(html, event) {
  const found = anchorElements(html).some(({ attributes }) =>
    attributes.href === registerUrl && attributes["data-cn1-conversion"] === event
  );
  assert.ok(found, `${event} must send directly to registration`);
}

assertSignupCta(
  `<a href="${registerUrl}" data-cn1-conversion="quoted-signup">Create account</a>`,
  "quoted-signup"
);
assertSignupCta(
  `<a href=${registerUrl} data-cn1-conversion=minified-signup>Create account</a>`,
  "minified-signup"
);

const home = page("home");
const pricing = page("pricing");
const compare = page("compare");

assertSignupCta(home, "home-primary-signup");
assertSignupCta(home, "home-final-signup");
assertSignupCta(pricing, "pricing-free-signup");
assertSignupCta(compare, "compare-signup");
assertSignupCta(compare, "compare-final-signup");

assert.ok(anchorElements(home).some(({ attributes, text }) =>
  attributes.href === registerUrl && text === "Sign Up"
), "the global header must expose registration");
assert.ok(anchorElements(home).some(({ attributes }) => attributes.href === "/initializr/"),
  "Initializr must remain available as an explicit project-generation tool");
assert.doesNotMatch(home, /cn1-exp-004/i,
  "the retired download experiment must not enroll new homepage visitors");

for (const html of [home, pricing, compare]) {
  assert.doesNotMatch(html, /(?:home-(?:primary|final)|pricing-free|compare(?:-final)?)-project/i,
    "primary funnel CTAs must not regress to the project-first route");
}

console.log(`Validated signup-first routing in ${publicDir}`);

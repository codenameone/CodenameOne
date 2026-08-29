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

function escape(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

// Hugo minifies the generated HTML, and minification DROPS the quotes around an
// attribute value that has no whitespace in it -- the pages ship
// `href=https://cloud.codenameone.com/register`, not `href="..."`. A pattern
// that insists on quotes therefore cannot match the very output it validates,
// which is how a correct signup funnel failed this test.
function attr(name, value) {
  const v = escape(value);
  return `${name}=(?:"${v}"|'${v}'|${v}(?=[\\s>]))`;
}

function assertSignupCta(html, event) {
  const link = new RegExp(
    `<a[^>]+${attr("href", registerUrl)}[^>]+${attr("data-cn1-conversion", event)}|` +
    `<a[^>]+${attr("data-cn1-conversion", event)}[^>]+${attr("href", registerUrl)}`,
    "i"
  );
  assert.match(html, link, `${event} must send directly to registration`);
}

const home = page("home");
const pricing = page("pricing");
const compare = page("compare");

assertSignupCta(home, "home-primary-signup");
assertSignupCta(home, "home-final-signup");
assertSignupCta(pricing, "pricing-free-signup");
assertSignupCta(compare, "compare-signup");
assertSignupCta(compare, "compare-final-signup");

assert.match(home,
  new RegExp(`<a[^>]+${attr("href", registerUrl)}[^>]*>[\\s\\S]*?Sign Up`, "i"),
  "the global header must expose registration");
assert.match(home, new RegExp(attr("href", "/initializr/"), "i"),
  "Initializr must remain available as an explicit project-generation tool");
assert.doesNotMatch(home, /cn1-exp-004/i,
  "the retired download experiment must not enroll new homepage visitors");

for (const html of [home, pricing, compare]) {
  assert.doesNotMatch(html, /(?:home-(?:primary|final)|pricing-free|compare(?:-final)?)-project/i,
    "primary funnel CTAs must not regress to the project-first route");
}

console.log(`Validated signup-first routing in ${publicDir}`);

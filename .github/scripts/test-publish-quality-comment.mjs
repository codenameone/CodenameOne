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

// Publishing the quality report is NOT the gate. generate-quality-report.py
// enforces the analysis in an earlier step and this one runs under
// `if: always()` so a human can read the result -- so a GitHub API error here
// must never fail the build. It did: run 33263893857 died on a 504 from
// PATCH /issues/comments while posting a report whose own first line read
// "Continuous Quality Report" with 7416 tests and 0 failures. A red build
// that says nothing is wrong is worse than a missing comment, because it
// teaches people to re-run reds without reading them.

import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { publishQualityComment } =
  require("./publish-quality-comment.js");

const report = path.join(os.tmpdir(), "cn1-quality-report-test.md");
fs.writeFileSync(report, "## Continuous Quality Report\n- Tests: 1, 0 failed\n");

const context = { repo: { owner: "o", repo: "r" }, issue: { number: 1 } };

function newCore() {
  const warnings = [];
  return { warnings, warning: (message) => warnings.push(message) };
}

function gatewayTimeout() {
  const error = new Error("We couldn't respond to your request in time.");
  error.status = 504;
  throw error;
}

const existingComment = {
  id: 5,
  user: { type: "Bot" },
  body: "<!-- quality-report -->stale",
};

// The listing fails.
let core = newCore();
await publishQualityComment({
  github: { rest: { issues: {
    listComments: gatewayTimeout,
    updateComment: gatewayTimeout,
    createComment: gatewayTimeout,
  } } },
  context,
  core,
  reportPath: report,
});
assert.equal(core.warnings.length, 1,
  "a failed listing has to warn rather than throw");

// The update fails, which is the call the real run died on.
core = newCore();
await publishQualityComment({
  github: { rest: { issues: {
    listComments: async () => ({ data: [existingComment] }),
    updateComment: gatewayTimeout,
    createComment: gatewayTimeout,
  } } },
  context,
  core,
  reportPath: report,
});
assert.equal(core.warnings.length, 1,
  "a failed update has to warn rather than throw");
assert.match(core.warnings[0], /Could not publish the quality report comment/,
  "and the warning has to say what could not be done");

// The creation fails, for a pull request that has no comment yet.
core = newCore();
await publishQualityComment({
  github: { rest: { issues: {
    listComments: async () => ({ data: [] }),
    updateComment: gatewayTimeout,
    createComment: gatewayTimeout,
  } } },
  context,
  core,
  reportPath: report,
});
assert.equal(core.warnings.length, 1,
  "a failed creation has to warn rather than throw");

// And it still publishes when the API is healthy, so the swallow above did
// not simply turn the step off.
let updated = 0;
let created = 0;
core = newCore();
await publishQualityComment({
  github: { rest: { issues: {
    listComments: async () => ({ data: [existingComment] }),
    updateComment: async () => { updated++; },
    createComment: async () => { created++; },
  } } },
  context,
  core,
  reportPath: report,
});
assert.equal(updated, 1, "an existing comment is updated");
assert.equal(created, 0, "and not duplicated");
assert.equal(core.warnings.length, 0, "a healthy publish warns about nothing");

core = newCore();
await publishQualityComment({
  github: { rest: { issues: {
    listComments: async () => ({ data: [] }),
    updateComment: async () => { updated++; },
    createComment: async () => { created++; },
  } } },
  context,
  core,
  reportPath: report,
});
assert.equal(created, 1, "a pull request with no comment yet gets one");

// A missing report is not an error either; it is the case the step's own
// hashFiles() guard already covers.
core = newCore();
await publishQualityComment({
  github: { rest: { issues: {
    listComments: gatewayTimeout,
    updateComment: gatewayTimeout,
    createComment: gatewayTimeout,
  } } },
  context,
  core,
  reportPath: path.join(os.tmpdir(), "cn1-quality-report-absent.md"),
});
assert.equal(core.warnings.length, 1, "a missing report warns and returns");

console.log("Validated that publishing the quality report cannot fail a build");

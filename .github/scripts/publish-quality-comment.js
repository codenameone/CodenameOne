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

const fs = require('fs');

/**
 * Publish or update a pull request comment containing a quality report.
 *
 * @param {{github: import('@actions/github').GitHub, context: any, core: any, marker?: string, reportPath?: string}} options
 */
async function publishQualityComment({ github, context, core, marker, reportPath }) {
  const effectiveMarker = marker || '<!-- quality-report -->';
  const report = reportPath || 'quality-report.md';

  if (!fs.existsSync(report)) {
    core.warning(`${report} was not generated.`);
    return;
  }

  const body = `${effectiveMarker}\n${fs.readFileSync(report, 'utf8')}`;
  const { owner, repo } = context.repo;
  const issue_number = context.issue.number;

  // WARN, never throw. Publishing is not the gate: the workflow enforces the
  // analysis with generate-quality-report.py in an earlier step and this one
  // runs under `if: always()` purely so a human can read the result. Letting
  // it throw meant a transient GitHub API error failed a build whose gate had
  // already passed -- run 33263893857 died on a 504 from PATCH
  // /issues/comments while posting a report whose own first line was
  // "Continuous Quality Report" with 7416 tests and 0 failures. A red build
  // that says nothing is wrong is worse than a missing comment, and it
  // teaches people to re-run reds without reading them.
  try {
    const { data: comments } = await github.rest.issues.listComments({
      owner,
      repo,
      issue_number,
      per_page: 100,
    });

    const existing = comments.find(
      (comment) => comment.user?.type === 'Bot' && comment.body?.includes(effectiveMarker),
    );

    if (existing) {
      await github.rest.issues.updateComment({
        owner,
        repo,
        comment_id: existing.id,
        body,
      });
    } else {
      await github.rest.issues.createComment({
        owner,
        repo,
        issue_number,
        body,
      });
    }
  } catch (error) {
    // Surfaced as an annotation on the run, so a comment that stopped being
    // published is visible rather than silently absent.
    core.warning(`Could not publish the quality report comment: ${error.message}`);
  }
}

module.exports = { publishQualityComment };

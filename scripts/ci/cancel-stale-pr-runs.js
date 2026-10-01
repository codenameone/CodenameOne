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

// Only PR validation runs are eligible. No checkout of PR code is needed by
// the caller, and push/release/dispatch runs are never cancellation targets.
const ACTIVE = new Set(['queued', 'in_progress', 'waiting', 'pending', 'requested']);

function belongsTo(run, pr) {
  if (run.event !== 'pull_request') return false;
  const refs = run.pull_requests || [];
  if (refs.length) return refs.some(ref => ref.number === pr.number);
  // GitHub can return an empty association list for fork PR runs. Match BOTH
  // repository identity and branch, never the branch alone (e.g. patch-1).
  return !!pr.head.repo && run.head_repository?.id === pr.head.repo.id &&
    run.head_branch === pr.head.ref;
}

function stale(run, pr) {
  if (!ACTIVE.has(run.status) || !belongsTo(run, pr)) return false;
  if (pr.state === 'closed') return true;
  // Preserve a run testing the head or current synthetic merge commit.
  return !!pr.head.sha && !!run.head_sha && run.head_sha !== pr.head.sha &&
    run.head_sha !== pr.merge_commit_sha;
}

async function cancelStale({github, context, core, dryRun = false}) {
  const repo = context.repo;
  const number = context.payload.pull_request.number;
  let pr = (await github.rest.pulls.get({...repo, pull_number: number})).data;
  let cancelled = 0;
  // Gather before cancelling: mutating while paging can skip runs as the list
  // shrinks. Broad active statuses include queued dependencies and approvals.
  const runs = new Map();
  for (const status of ACTIVE) {
    const page = await github.paginate(github.rest.actions.listWorkflowRunsForRepo,
      {...repo, event: 'pull_request', status, per_page: 100});
    for (const run of page) if (belongsTo(run, pr)) runs.set(run.id, run);
  }
  for (const candidate of runs.values()) {
    // Re-read both immediately before mutation: a delayed synchronize/closed
    // event must not kill a new head or a PR that has since been reopened.
    pr = (await github.rest.pulls.get({...repo, pull_number: number})).data;
    const run = (await github.rest.actions.getWorkflowRun({...repo, run_id: candidate.id})).data;
    if (!stale(run, pr)) continue;
    core.info(`${dryRun ? 'Would cancel' : 'Cancelling'} obsolete PR #${number} run ${run.id}: ${run.name}`);
    if (!dryRun) {
      try {
        await github.rest.actions.cancelWorkflowRun({...repo, run_id: run.id});
      } catch (error) {
        // A run can finish between GET and POST; other errors remain failures.
        if (error.status !== 409) throw error;
      }
    }
    cancelled++;
  }
  core.info(`${cancelled} obsolete run(s) ${dryRun ? 'identified' : 'cancelled'}`);
  return cancelled;
}
module.exports = {belongsTo, stale, cancelStale};

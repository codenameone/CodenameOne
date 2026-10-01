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

const test = require('node:test');
const assert = require('node:assert/strict');
const {stale, cancelStale} = require('./cancel-stale-pr-runs');
const pr = {number: 7, state: 'open', head: {sha: 'new', ref: 'patch-1', repo: {id: 9}}, merge_commit_sha: 'merge'};
const run = {id: 11, name: 'CI', event: 'pull_request', status: 'queued', head_sha: 'old', pull_requests: [{number: 7}]};

test('superseded queued and running runs are stale; current head and merge are kept', () => {
  for (const status of ['queued', 'in_progress', 'waiting', 'pending', 'requested']) {
    assert.equal(stale({...run, status}, pr), true);
    for (const head_sha of ['new', 'merge']) assert.equal(stale({...run, status, head_sha}, pr), false);
  }
  assert.equal(stale({...run, status: 'completed'}, pr), false);
});
test('closed PR cancels even its final head; another PR and non-PR runs survive', () => {
  assert.equal(stale({...run, head_sha: 'new'}, {...pr, state: 'closed'}), true);
  assert.equal(stale({...run, pull_requests: [{number: 8}]}, pr), false);
  for (const event of ['push', 'workflow_dispatch', 'schedule', 'pull_request_target']) {
    assert.equal(stale({...run, event}, {...pr, state: 'closed'}), false);
  }
});
test('empty fork associations require both repository identity and branch', () => {
  const forkRun = {...run, pull_requests: [], head_branch: 'patch-1', head_repository: {id: 9}};
  assert.equal(stale(forkRun, pr), true);
  assert.equal(stale({...forkRun, head_repository: {id: 10}}, pr), false);
  assert.equal(stale({...forkRun, head_branch: 'other'}, pr), false);
  assert.equal(stale(forkRun, {...pr, head: {...pr.head, repo: null}}), false);
});
function api(livePr = pr, liveRun = run) {
  const cancelled = [];
  const github = {
    paginate: async (_, params) => params.status === 'queued' ? [run] : [],
    rest: {
      pulls: {get: async () => ({data: livePr})},
      actions: {
        listWorkflowRunsForRepo: () => {},
        getWorkflowRun: async () => ({data: liveRun}),
        cancelWorkflowRun: async ({run_id}) => cancelled.push(run_id)
      }
    }
  };
  return {github, cancelled, context: {repo: {owner: 'o', repo: 'r'}, payload: {pull_request: pr}}, core: {info: () => {}}};
}
test('requests cancellation only after checking live state', async () => {
  const client = api();
  assert.equal(await cancelStale(client), 1);
  assert.deepEqual(client.cancelled, [11]);
});
test('delayed events do not cancel a newer run or a reopened current head', async () => {
  for (const client of [api(pr, {...run, head_sha: 'new'}), api({...pr, head: {...pr.head, sha: 'old'}}), api(pr, {...run, status: 'completed'})]) {
    await cancelStale(client);
    assert.deepEqual(client.cancelled, []);
  }
});
test('dry-run is read-only', async () => {
  const client = api();
  assert.equal(await cancelStale({...client, dryRun: true}), 1);
  assert.deepEqual(client.cancelled, []);
});
test('409 finish race is tolerated, authorization failures are not hidden', async () => {
  for (const status of [409, 403]) {
    const client = api();
    client.github.rest.actions.cancelWorkflowRun = async () => { throw Object.assign(new Error('API'), {status}); };
    if (status === 409) await cancelStale(client);
    else await assert.rejects(cancelStale(client), /API/);
  }
});
